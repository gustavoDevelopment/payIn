# Arquitectura Implementada - PayIn Service

## Resumen Ejecutivo

PayIn Service implementa una **Arquitectura Hexagonal (Ports & Adapters)** con principios de **Domain-Driven Design (DDD)**, diseñada para ser un componente de plataforma reusable, desacoplado y extensible para el procesamiento de transacciones financieras.

### Características Clave

- ✅ **Independencia de frameworks**: Lógica de negocio sin dependencias de Spring o JPA
- ✅ **Testabilidad**: 100% testeable con mocks, sin necesidad de base de datos
- ✅ **Extensibilidad**: Nuevos adaptadores sin modificar dominio
- ✅ **Consistencia transaccional**: ACID garantizado con Spring @Transactional
- ✅ **Trazabilidad**: Modelo de estados estricto con validaciones

---

## Arquitectura Hexagonal: Visión General

La arquitectura hexagonal separa el sistema en tres capas principales:

### 1. Domain Layer (Núcleo)
El corazón del sistema, contiene toda la lógica de negocio y está completamente aislado de detalles técnicos.

**Responsabilidades**:
- Definir reglas de negocio
- Gestionar estados y transiciones
- Validar invariantes del dominio
- Exponer contratos (puertos)

**Principio**: "El dominio no sabe nada del mundo exterior"

### 2. Interfaces Layer (Adaptadores de Entrada)
Expone el dominio al mundo exterior mediante diferentes protocolos.

**Implementación actual**: REST API con Spring Web
**Futuras extensiones**: GraphQL, gRPC, Message Queues

### 3. Infrastructure Layer (Adaptadores de Salida)
Implementa las dependencias del dominio (persistencia, integraciones externas).

**Implementación actual**: JPA con H2/PostgreSQL
**Futuras extensiones**: Redis cache, Payment gateways, Event publishers

---

## Domain Layer: Diseño Detallado

### Estructura de Paquetes

```
com.payin.domain
├── model                    # Entidades y agregados
│   ├── PayIn.java          # Aggregate Root
│   ├── Customer.java       # Value Object / Entity (futuro)
│   ├── Account.java        # Value Object / Entity (futuro)
│   ├── PaymentMethod.java  # Value Object / Entity (futuro)
│   └── PaymentProvider.java
├── ports
│   ├── input               # Casos de uso (entry points)
│   │   └── PayInUseCase.java
│   └── output              # Contratos de persistencia
│       └── PayInRepositoryPort.java
├── service                 # Servicios de dominio
│   └── PayInService.java
└── exceptions              # Excepciones de negocio
    └── DomainException.java
```

### PayIn: Aggregate Root

`PayIn` es el **agregado raíz** del dominio, encapsula toda la lógica relacionada con una transacción de ingreso.

**Características**:
- Contiene su propio estado (`PayInStatus`)
- Protege sus invariantes mediante validaciones
- Expone métodos de negocio, no getters/setters puros
- Inmutable en partes críticas (id, transactionId)

**Código clave**:
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayIn {
    private UUID id;
    private String transactionId;
    private BigDecimal amount;
    private String currency;
    private String customerId;
    private String paymentMethodId;
    private PayInStatus status;
    private String description;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum PayInStatus {
        CREATED, VALIDATED, PROCESSED, FAILED
    }

    // Métodos de negocio
    public void validate() { ... }
    public void process() { ... }
    public void fail(String errorMessage) { ... }
}
```

**Invariantes protegidas**:
1. `amount` debe ser > 0 y <= 1,000,000
2. `currency` debe ser código ISO de 3 letras (e.g., USD, EUR)
3. `customerId` y `paymentMethodId` son obligatorios
4. Solo se puede procesar un PayIn en estado `VALIDATED`
5. Estados `PROCESSED` y `FAILED` son finales

### Validaciones de Dominio

Las validaciones están encapsuladas en métodos privados dentro del agregado:

```java
public void validate() {
    validateAmount();      // PAYIN-001, PAYIN-002, PAYIN-003
    validateCurrency();    // PAYIN-004, PAYIN-005
    validateCustomer();    // PAYIN-006
    validatePaymentMethod(); // PAYIN-007

    this.status = PayInStatus.VALIDATED;
    this.updatedAt = LocalDateTime.now();
}
```

**Ventajas de este diseño**:
- Lógica centralizada (single source of truth)
- Fácil de testear unitariamente
- Cambios de reglas de negocio en un solo lugar
- Excepciones tipadas con códigos de error

### State Machine: Máquina de Estados

El PayIn sigue un flujo de estados estricto:

```
CREATED → validate() → VALIDATED → process() → PROCESSED
                                 ↘ fail() → FAILED
```

**Implementación**:
```java
public void process() {
    if (this.status != PayInStatus.VALIDATED) {
        throw new IllegalStateException(
            "PayIn must be validated before processing"
        );
    }
    this.status = PayInStatus.PROCESSED;
    this.updatedAt = LocalDateTime.now();
}
```

**Beneficios**:
- Transiciones explícitas y controladas
- Imposible tener estados inconsistentes
- Trazabilidad de cambios de estado
- Fácil auditoría

### Puertos: Inversión de Dependencias

Los puertos son **interfaces** que definen contratos entre el dominio y el mundo exterior.

#### Puerto de Entrada: PayInUseCase

```java
public interface PayInUseCase {
    PayIn createPayIn(PayIn payIn);
    PayIn processPayIn(UUID payInId);
    PayIn getPayIn(UUID payInId);
    PayIn getPayInByTransactionId(String transactionId);
}
```

**Propósito**: Define qué puede hacer el sistema (casos de uso).

**Implementación**: `PayInService` (capa de dominio)

#### Puerto de Salida: PayInRepositoryPort

```java
public interface PayInRepositoryPort {
    PayIn save(PayIn payIn);
    Optional<PayIn> findById(UUID id);
    Optional<PayIn> findByTransactionId(String transactionId);
}
```

**Propósito**: Define cómo el dominio persiste datos, sin saber cómo.

**Implementación**: `PayInRepositoryAdapter` (capa de infraestructura)

**Ventaja clave**: El dominio depende de abstracciones, no de implementaciones concretas. Puedo cambiar de H2 a PostgreSQL sin tocar el dominio.

### Servicio de Dominio: PayInService

`PayInService` orquesta la lógica de negocio y coordina entre el dominio y la persistencia.

```java
@Service
@RequiredArgsConstructor
public class PayInService implements PayInUseCase {
    private final PayInRepositoryPort payInRepository;

    @Override
    @Transactional
    public PayIn createPayIn(PayIn payIn) {
        // 1. Inicializar PayIn
        payIn.setId(UUID.randomUUID());
        payIn.setStatus(PayIn.PayInStatus.CREATED);
        payIn.setTransactionId(generateTransactionId());
        payIn.setCreatedAt(LocalDateTime.now());
        payIn.setUpdatedAt(LocalDateTime.now());

        // 2. Validar reglas de negocio
        payIn.validate();

        // 3. Persistir
        return payInRepository.save(payIn);
    }

    @Override
    @Transactional
    public PayIn processPayIn(UUID payInId) {
        // 1. Obtener PayIn
        PayIn payIn = payInRepository.findById(payInId)
            .orElseThrow(() -> new IllegalArgumentException(
                "PayIn not found with id: " + payInId
            ));

        // 2. Procesar (aquí iría integración con gateway)
        try {
            payIn.process();
            return payInRepository.save(payIn);
        } catch (Exception e) {
            payIn.fail(e.getMessage());
            return payInRepository.save(payIn);
        }
    }
}
```

**Responsabilidades**:
- Coordinar llamadas entre dominio y persistencia
- Gestionar transacciones (`@Transactional`)
- Generar IDs y timestamps
- Manejo de excepciones de alto nivel

**NO es responsable de**:
- Lógica de negocio (está en `PayIn`)
- Detalles de persistencia (está en adapter)
- Validaciones de dominio (están en `PayIn.validate()`)

### DomainException: Excepciones Tipadas

```java
public class DomainException extends RuntimeException {
    private final String errorCode;

    public DomainException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
```

**Códigos de error definidos**:
- `PAYIN-001`: Monto nulo
- `PAYIN-002`: Monto <= 0
- `PAYIN-003`: Monto excede límite
- `PAYIN-004`: Moneda requerida
- `PAYIN-005`: Formato de moneda inválido
- `PAYIN-006`: Customer ID requerido
- `PAYIN-007`: Payment method ID requerido
- `PAYIN-009`: PayIn no encontrado por transactionId

**Beneficios**:
- Internacionalización fácil (mapear códigos a mensajes)
- Identificación rápida en logs
- Contratos de error claros para clientes de API

---

## Infrastructure Layer: Diseño Detallado

### Estructura de Paquetes

```
com.payin.infrastructure
├── adapter                 # Implementa puertos de salida
│   └── PayInRepositoryAdapter.java
├── entity                  # Entidades JPA
│   └── PayInEntity.java
└── repository              # Repositorios Spring Data
    └── JpaPayInRepository.java
```

### Separación: Domain Model vs JPA Entity

**Decisión arquitectónica crítica**: Los modelos de dominio están separados de las entidades JPA.

#### PayIn (Domain Model)

```java
// Sin anotaciones JPA
public class PayIn {
    private UUID id;
    private String transactionId;
    private BigDecimal amount;
    // ...
    public void validate() { ... } // Lógica de negocio
}
```

#### PayInEntity (JPA Entity)

```java
@Entity
@Table(name = "payins")
public class PayInEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String transactionId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    // Métodos de conversión
    public static PayInEntity fromDomain(PayIn payIn) {
        return PayInEntity.builder()
            .id(payIn.getId())
            .transactionId(payIn.getTransactionId())
            // ... mapeo de todos los campos
            .build();
    }

    public PayIn toDomain() {
        return PayIn.builder()
            .id(this.id)
            .transactionId(this.transactionId)
            // ... mapeo de todos los campos
            .build();
    }
}
```

**Ventajas de la separación**:
1. **Dominio puro**: Sin dependencias de JPA (`@Entity`, `@Column`, etc.)
2. **Flexibilidad**: Cambiar ORM sin tocar dominio
3. **Testing**: Testear dominio sin base de datos
4. **Evolución independiente**: Modelo de dominio y schema DB evolucionan independientemente

**Costos**:
- Código de mapeo adicional (mitigado con MapStruct si crece)
- Overhead de conversión (mínimo, solo en boundaries)

### PayInRepositoryAdapter: Implementación del Puerto

```java
@Component
@RequiredArgsConstructor
public class PayInRepositoryAdapter implements PayInRepositoryPort {
    private final JpaPayInRepository jpaPayInRepository;

    @Override
    public PayIn save(PayIn payIn) {
        PayInEntity entity = PayInEntity.fromDomain(payIn);
        PayInEntity savedEntity = jpaPayInRepository.save(entity);
        return savedEntity.toDomain();
    }

    @Override
    public Optional<PayIn> findById(UUID id) {
        return jpaPayInRepository.findById(id)
            .map(PayInEntity::toDomain);
    }

    @Override
    public Optional<PayIn> findByTransactionId(String transactionId) {
        return jpaPayInRepository.findByTransactionId(transactionId)
            .map(PayInEntity::toDomain);
    }
}
```

**Responsabilidades**:
- Implementar contrato `PayInRepositoryPort`
- Traducir entre dominio y JPA
- Delegar operaciones a `JpaPayInRepository`

**Patrón aplicado**: Adapter Pattern

### JpaPayInRepository: Spring Data Repository

```java
@Repository
public interface JpaPayInRepository extends JpaRepository<PayInEntity, UUID> {
    Optional<PayInEntity> findByTransactionId(String transactionId);
}
```

**Características**:
- Extiende `JpaRepository` para operaciones CRUD automáticas
- Métodos derivados de nombres (Spring Data Magic)
- Sin necesidad de implementación manual

**Queries generadas automáticamente**:
- `findById(UUID)`: SELECT * FROM payins WHERE id = ?
- `save(PayInEntity)`: INSERT/UPDATE
- `findByTransactionId(String)`: SELECT * FROM payins WHERE transaction_id = ?

---

## Interfaces Layer: Diseño Detallado

### Estructura de Paquetes

```
com.payin.interfaces.rest
├── PayInController.java
└── dto
    ├── CreatePayInRequest.java
    └── PayInResponse.java
```

### PayInController: Adaptador REST

```java
@RestController
@RequestMapping("/api/v1/payins")
@RequiredArgsConstructor
public class PayInController {
    private final PayInUseCase payInUseCase;

    @PostMapping
    public ResponseEntity<PayInResponse> createPayIn(
        @Valid @RequestBody CreatePayInRequest request
    ) {
        PayIn payIn = PayIn.builder()
            .amount(request.getAmount())
            .currency(request.getCurrency())
            .customerId(request.getCustomerId())
            .paymentMethodId(request.getPaymentMethodId())
            .description(request.getDescription())
            .build();

        PayIn createdPayIn = payInUseCase.createPayIn(payIn);
        return new ResponseEntity<>(
            PayInResponse.fromDomain(createdPayIn),
            HttpStatus.CREATED
        );
    }

    @PostMapping("/{id}/process")
    public ResponseEntity<PayInResponse> processPayIn(@PathVariable UUID id) {
        PayIn processedPayIn = payInUseCase.processPayIn(id);
        return ResponseEntity.ok(PayInResponse.fromDomain(processedPayIn));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PayInResponse> getPayIn(@PathVariable UUID id) {
        PayIn payIn = payInUseCase.getPayIn(id);
        return ResponseEntity.ok(PayInResponse.fromDomain(payIn));
    }

    @GetMapping("/transaction/{transactionId}")
    public ResponseEntity<PayInResponse> getPayInByTransactionId(
        @PathVariable String transactionId
    ) {
        PayIn payIn = payInUseCase.getPayInByTransactionId(transactionId);
        return ResponseEntity.ok(PayInResponse.fromDomain(payIn));
    }

    // Exception handlers
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<String> handleNotFoundException(
        IllegalArgumentException ex
    ) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<String> handleIllegalStateException(
        IllegalStateException ex
    ) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }
}
```

**Responsabilidades**:
- Exponer endpoints HTTP
- Validar entrada (`@Valid`)
- Convertir DTOs ↔ Domain Models
- Mapear excepciones a códigos HTTP
- Retornar respuestas JSON

**Principio aplicado**: El controller NO contiene lógica de negocio, solo orquestación HTTP.

### DTOs: Data Transfer Objects

#### CreatePayInRequest

```java
@Data
public class CreatePayInRequest {
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    private String currency;

    @NotBlank(message = "Customer ID is required")
    private String customerId;

    @NotBlank(message = "Payment method ID is required")
    private String paymentMethodId;

    private String description;
}
```

**Validaciones Bean Validation**:
- `@NotNull`, `@NotBlank`: Campos obligatorios
- `@DecimalMin`: Validación de rango
- Mensajes de error personalizados

**Capa de defensa**: Validación en DTO es la primera línea, validaciones de dominio son la segunda.

#### PayInResponse

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayInResponse {
    private UUID id;
    private String transactionId;
    private BigDecimal amount;
    private String currency;
    private String customerId;
    private String paymentMethodId;
    private String status; // Enum como String para JSON
    private String description;
    private String errorMessage;

    @JsonFormat(shape = JsonFormat.Shape.STRING,
                pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING,
                pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime updatedAt;

    public static PayInResponse fromDomain(PayIn payIn) {
        return PayInResponse.builder()
            .id(payIn.getId())
            .transactionId(payIn.getTransactionId())
            .amount(payIn.getAmount())
            .currency(payIn.getCurrency())
            .customerId(payIn.getCustomerId())
            .paymentMethodId(payIn.getPaymentMethodId())
            .status(payIn.getStatus().name())
            .description(payIn.getDescription())
            .errorMessage(payIn.getErrorMessage())
            .createdAt(payIn.getCreatedAt())
            .updatedAt(payIn.getUpdatedAt())
            .build();
    }
}
```

**Características**:
- Conversión de enum a String para JSON
- Formato de fechas ISO 8601
- Conversión explícita desde dominio

---

## Transacciones y Consistencia

### Estrategia de Transacciones

El servicio utiliza `@Transactional` de Spring para garantizar consistencia ACID:

```java
@Service
@RequiredArgsConstructor
public class PayInService implements PayInUseCase {
    @Override
    @Transactional
    public PayIn createPayIn(PayIn payIn) {
        // Toda esta operación es atómica
        payIn.setId(UUID.randomUUID());
        payIn.validate(); // Puede lanzar DomainException
        return payInRepository.save(payIn);
        // Si hay exception, rollback automático
    }
}
```

**Comportamiento**:
- Todas las operaciones en una transacción DB
- Si cualquier operación falla, rollback completo
- Aislamiento: READ_COMMITTED (default)
- Propagación: REQUIRED (default)

### Idempotencia y Concurrencia

**Problema**: Dos requests simultáneos para procesar el mismo PayIn.

**Solución futura**: Locking optimista con versioning:

```java
@Entity
public class PayInEntity {
    @Version
    private Long version;
    // ...
}
```

Con versioning, si dos transacciones intentan actualizar el mismo PayIn, la segunda falla con `OptimisticLockException`.

---

## Configuración y Propiedades

### application.yml

```yaml
spring:
  application:
    name: payin-service

  datasource:
    url: jdbc:h2:mem:payindb
    driverClassName: org.h2.Driver
    username: sa
    password: password

  h2:
    console:
      enabled: true
      path: /h2-console

  jpa:
    database-platform: org.hibernate.dialect.H2Dialect
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        format_sql: true

  jackson:
    serialization:
      fail-on-empty-beans: false
    deserialization:
      fail-on-unknown-properties: false

logging:
  level:
    root: INFO
    com.payin: DEBUG
    org.hibernate.SQL: DEBUG

server:
  port: 8080
  error:
    include-message: always
    include-binding-errors: always
```

**Configuraciones clave**:
- **H2 in-memory**: Para desarrollo rápido
- **ddl-auto: update**: Hibernate genera/actualiza schema automáticamente
- **show-sql: true**: Logs de queries para debugging
- **error.include-message: always**: Mensajes de error en respuestas HTTP

---

## Principios SOLID Aplicados

### Single Responsibility Principle (SRP)
- `PayIn`: Solo gestiona estado y validaciones de PayIn
- `PayInService`: Solo orquesta lógica de negocio
- `PayInController`: Solo maneja HTTP
- `PayInRepositoryAdapter`: Solo traduce dominio ↔ JPA

### Open/Closed Principle (OCP)
- Abierto a extensión: Nuevos adaptadores (MongoDB, Redis) sin modificar dominio
- Cerrado a modificación: Dominio estable, cambios en infra no lo afectan

### Liskov Substitution Principle (LSP)
- Cualquier implementación de `PayInRepositoryPort` puede reemplazar otra
- `PayInService` no sabe si usa H2, PostgreSQL o MongoDB

### Interface Segregation Principle (ISP)
- Puertos pequeños y específicos (`PayInUseCase`, `PayInRepositoryPort`)
- No interfaces "god" con demasiados métodos

### Dependency Inversion Principle (DIP)
- Dependencias sobre abstracciones (`PayInRepositoryPort`), no implementaciones
- Inversión de control: Dominio define contratos, infraestructura implementa

---

## Escalabilidad y Performance

### Estrategias de Escalamiento

#### Escalamiento Horizontal
- Múltiples instancias del servicio (stateless)
- Load balancer distribuyendo tráfico
- Sin sesiones, completamente stateless

#### Escalamiento Vertical
- Aumentar recursos (CPU, RAM) por instancia
- JVM tuning (-Xms, -Xmx)

### Optimizaciones Futuras

1. **Caching con Redis**
   - Cache de configuraciones (límites, providers)
   - Cache de PayIns recientes (1 minuto TTL)

2. **Connection Pooling**
   - HikariCP (incluido en Spring Boot)
   - Configurar pool size según carga

3. **Índices de Base de Datos**
   - Ya implementados en schema.sql
   - `idx_payins_customer_status` para queries comunes

4. **Async Processing**
   - Procesar PayIns de forma asíncrona con `@Async`
   - Message queue (Kafka, RabbitMQ) para procesamiento diferido

---

## Seguridad

### Consideraciones Implementadas

1. **Validaciones en Múltiples Capas**
   - DTO: Bean Validation
   - Dominio: Validaciones de negocio

2. **Prepared Statements**
   - JPA usa prepared statements (protección contra SQL injection)

3. **Error Messages Controlados**
   - No exponer stack traces a clientes
   - Mensajes genéricos en producción

### Pendientes de Implementación

1. **Autenticación y Autorización**
   - Spring Security con JWT
   - Roles: ADMIN, USER, SYSTEM

2. **Rate Limiting**
   - Bucket4j: 100 requests/minuto por IP

3. **Encriptación**
   - Datos sensibles encriptados en DB
   - TLS para comunicación

4. **Auditoría**
   - Tabla `payin_audit` para cambios de estado
   - Logs de acceso con Spring AOP

---

## Observabilidad y Monitoring

### Logs

**Niveles configurados**:
- `com.payin: DEBUG`: Logs detallados de la aplicación
- `org.hibernate.SQL: DEBUG`: Queries SQL

**Best practices**:
- Usar SLF4J con Logback
- Logs estructurados (JSON) en producción
- Correlation IDs para tracing distribuido

### Métricas (Futuro)

**Prometheus + Grafana**:
- Tasa de transacciones por minuto
- Success rate (PROCESSED vs FAILED)
- Latencia p50, p95, p99
- Uso de JVM (heap, threads, GC)

### Tracing (Futuro)

**Jaeger o Zipkin**:
- Tracing distribuido de requests
- Identificar cuellos de botella
- Correlación de logs entre servicios

---

## Testing Strategy

### Pirámide de Testing

```
        ┌─────────────┐
        │  E2E Tests  │ (Pocos)
        ├─────────────┤
        │Integration │ (Algunos)
        │   Tests    │
        ├─────────────┤
        │   Unit     │ (Muchos)
        │   Tests    │
        └─────────────┘
```

### Unit Tests (Dominio)

```java
@Test
void validate_shouldThrowException_whenAmountIsNegative() {
    PayIn payIn = PayIn.builder()
        .amount(new BigDecimal("-10"))
        .currency("USD")
        .customerId("CUST001")
        .paymentMethodId("PM001")
        .build();

    DomainException exception = assertThrows(
        DomainException.class,
        () -> payIn.validate()
    );

    assertEquals("PAYIN-002", exception.getErrorCode());
}
```

### Integration Tests (Repository)

```java
@SpringBootTest
@AutoConfigureTestDatabase(replace = Replace.ANY)
class PayInRepositoryAdapterIntegrationTest {
    @Autowired
    private PayInRepositoryPort payInRepository;

    @Test
    void save_shouldPersistPayIn() {
        PayIn payIn = createValidPayIn();
        PayIn saved = payInRepository.save(payIn);
        assertNotNull(saved.getId());
        assertEquals(payIn.getAmount(), saved.getAmount());
    }
}
```

### E2E Tests (API)

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PayInControllerE2ETest {
    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void createPayIn_shouldReturn201() {
        CreatePayInRequest request = new CreatePayInRequest();
        request.setAmount(new BigDecimal("100.00"));
        request.setCurrency("USD");
        request.setCustomerId("CUST001");
        request.setPaymentMethodId("PM001");

        ResponseEntity<PayInResponse> response = restTemplate
            .postForEntity("/api/v1/payins", request, PayInResponse.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("VALIDATED", response.getBody().getStatus());
    }
}
```

---

## Evolución Futura: Roadmap

### Fase 1: Production Hardening (Q1 2024)
- [ ] Tests completos (coverage > 80%)
- [ ] Spring Security con JWT
- [ ] Rate limiting con Bucket4j
- [ ] Monitoreo con Prometheus/Grafana
- [ ] CI/CD completo con GitHub Actions
- [ ] Migración a PostgreSQL

### Fase 2: Advanced Features (Q2 2024)
- [ ] Integración real con payment gateways (Stripe, PayPal)
- [ ] Circuit breaker con Resilience4j
- [ ] Webhooks para notificaciones
- [ ] Cache con Redis
- [ ] Event sourcing con Kafka

### Fase 3: Platform Features (Q3 2024)
- [ ] Multi-tenancy
- [ ] API Gateway (Kong/AWS API Gateway)
- [ ] Service mesh (Istio)
- [ ] Chaos engineering (Chaos Monkey)
- [ ] Distributed tracing (Jaeger)

---

## Conclusión

La arquitectura de PayIn Service está diseñada con principios sólidos de ingeniería de software:

- ✅ **Desacoplamiento**: Dominio independiente de frameworks
- ✅ **Extensibilidad**: Nuevos adaptadores sin modificar core
- ✅ **Mantenibilidad**: Código limpio, organizado y documentado
- ✅ **Escalabilidad**: Diseño stateless, listo para horizontal scaling
- ✅ **Testabilidad**: Arquitectura que facilita testing en todos los niveles

Esta arquitectura es un **foundation sólido** para un sistema de pagos enterprise-grade, listo para evolucionar con los requerimientos del negocio.
