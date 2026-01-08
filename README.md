# PayIn Transaction Service

Componente de dominio reusable para el procesamiento de transacciones PayIn, implementando principios de arquitectura hexagonal y diseño orientado a dominio.

## Tabla de Contenidos

- [Descripción General](#descripción-general)
- [Decisiones Arquitectónicas](#decisiones-arquitectónicas)
- [Suposiciones](#suposiciones)
- [Riesgos Identificados](#riesgos-identificados)
- [Instalación y Ejecución](#instalación-y-ejecución)
- [API Endpoints](#api-endpoints)
- [Testing](#testing)
- [Documentación Adicional](#documentación-adicional)

---

## Descripción General

PayIn Service es un componente transaccional que gestiona operaciones de ingreso de dinero al sistema, garantizando:

- ✅ **Consistencia**: Transacciones ACID
- ✅ **Atomicidad**: Estados controlados mediante máquina de estados
- ✅ **Trazabilidad**: Auditoría completa de cambios
- ✅ **Extensibilidad**: Arquitectura desacoplada basada en puertos

### Estados del PayIn

```
CREATED → VALIDATED → PROCESSED
                    ↘ FAILED
```

- **CREATED**: PayIn inicializado en el sistema
- **VALIDATED**: Validaciones de negocio aprobadas
- **PROCESSED**: Transacción completada exitosamente
- **FAILED**: Transacción fallida (con mensaje de error)

---

## Decisiones Arquitectónicas

### 1. Arquitectura Hexagonal (Ports & Adapters)

**Decisión**: Implementar hexagonal architecture separando dominio, infraestructura e interfaces.

**Justificación**:
- **Independencia de frameworks**: La lógica de negocio no depende de Spring, JPA o HTTP
- **Testabilidad**: Fácil testing con mocks de puertos
- **Mantenibilidad**: Cambios en infraestructura no afectan al dominio
- **Evolución**: Componente reusable en diferentes contextos

**Estructura**:
```
com.payin
├── domain                    # Capa de dominio (lógica de negocio)
│   ├── model                # Entidades y agregados
│   ├── ports
│   │   ├── input           # Casos de uso (entry points)
│   │   └── output          # Contratos de persistencia
│   ├── service             # Servicios de dominio
│   └── exceptions          # Excepciones de negocio
├── infrastructure           # Capa de infraestructura
│   ├── adapter             # Implementación de puertos output
│   ├── entity              # Entidades JPA
│   └── repository          # Repositorios Spring Data
└── interfaces               # Capa de interfaces
    └── rest                 # Controladores REST
        └── dto              # DTOs de entrada/salida
```

**Alternativas consideradas**:
- **Arquitectura en capas tradicional**: Descartada por alto acoplamiento
- **Microkernel**: Menos apropiada para un componente transaccional

---

### 2. Domain-Driven Design (DDD)

**Decisión**: Modelo de dominio rico con lógica de negocio encapsulada.

**Justificación**:
- **Reglas de negocio en el dominio**: `PayIn.validate()`, `PayIn.process()`
- **Lenguaje ubicuo**: Términos de negocio en el código
- **Agregados cohesivos**: PayIn es un agregado con invariantes protegidas
- **Excepciones de dominio**: `DomainException` con códigos estructurados

**Ejemplo**:
```java
public void validate() {
    validateAmount();      // Monto > 0 y <= 1,000,000
    validateCurrency();    // Código ISO de 3 letras
    validateCustomer();    // Customer ID requerido
    validatePaymentMethod();
    this.status = PayInStatus.VALIDATED;
}
```

**Alternativas consideradas**:
- **Modelo anémico**: Descartado por falta de cohesión y lógica dispersa
- **Transaction Script**: No escalable para lógica compleja

---

### 3. Separación de Entidades: Domain vs Persistence

**Decisión**: Modelos de dominio separados de entidades JPA.

**Justificación**:
- **Pureza del dominio**: Sin anotaciones JPA en modelos de negocio
- **Flexibilidad**: Cambiar ORM sin modificar dominio
- **Mapeo explícito**: Control total de conversiones

**Implementación**:
- `PayIn` (dominio) ↔️ `PayInEntity` (JPA)
- Conversión: `PayInEntity.fromDomain()` y `PayInEntity.toDomain()`

**Costo**:
- Código adicional de mapeo (mitigado con MapStruct)
- Conversiones en runtime (overhead mínimo)

**Beneficio**:
- Dominio 100% libre de infraestructura
- Testing sin necesidad de base de datos

---

### 4. State Machine para Estados

**Decisión**: Transiciones de estado controladas mediante métodos del dominio.

**Justificación**:
- **Invariantes protegidas**: Solo transiciones válidas son posibles
- **Trazabilidad**: Cada transición es explícita
- **Validaciones centralizadas**: Lógica de negocio en el modelo

**Implementación**:
```java
public void process() {
    if (this.status != PayInStatus.VALIDATED) {
        throw new IllegalStateException("PayIn must be validated before processing");
    }
    this.status = PayInStatus.PROCESSED;
    this.updatedAt = LocalDateTime.now();
}
```

**Alternativa considerada**:
- **State Pattern con clases**: Más complejo para este caso de uso

---

### 5. Exception Handling con Códigos de Error

**Decisión**: `DomainException` con códigos estructurados (PAYIN-XXX).

**Justificación**:
- **Internacionalización**: Códigos mapeables a mensajes en cualquier idioma
- **Identificación rápida**: Debugging y soporte facilitado
- **Contratos de API**: Clientes pueden manejar errores específicos

**Ejemplo**:
```java
throw new DomainException("El monto excede el límite permitido", "PAYIN-003");
```

**Códigos definidos**:
- `PAYIN-001`: Monto nulo
- `PAYIN-002`: Monto <= 0
- `PAYIN-003`: Monto excede límite
- `PAYIN-004`: Moneda requerida
- `PAYIN-005`: Formato de moneda inválido
- Y más...

---

### 6. H2 In-Memory Database

**Decisión**: H2 para desarrollo y demos.

**Justificación**:
- **Simplicidad**: Sin dependencias externas
- **Rapidez**: Desarrollo ágil sin configuración
- **H2 Console**: UI para inspeccionar datos

**Producción**: PostgreSQL o MySQL recomendado

**Configuración**:
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:payindb
  h2:
    console:
      enabled: true
      path: /h2-console
```

---

### 7. OpenAPI/Swagger para Documentación

**Decisión**: SpringDoc OpenAPI para documentación automática.

**Justificación**:
- **Auto-generación**: Docs sincronizadas con código
- **Swagger UI**: Interfaz interactiva para testing
- **Estándar de industria**: OpenAPI 3.0

**Acceso**: http://localhost:8080/swagger-ui.html

---

### 8. Maven como Build Tool

**Decisión**: Maven sobre Gradle.

**Justificación**:
- **Convención sobre configuración**: Estructura estándar
- **Plugins maduros**: Spring Boot plugin, compiler plugin
- **Amplia adopción**: Mayor soporte en herramientas CI/CD

**Alternativa**: Gradle es más flexible pero menos convencional

---

## Suposiciones

### Funcionales

1. **IDs como Strings en PayIn**
   - `customer_id` y `payment_method_id` se almacenan como VARCHAR
   - **Supuesto**: Estos IDs vienen de sistemas externos
   - **Futuro**: Si se integran entidades Customer y PaymentMethod locales, convertir a UUID con FK

2. **Sin Integración Real con Payment Gateways**
   - El método `processPayIn()` no llama a Stripe, PayPal, etc.
   - **Supuesto**: Componente de dominio independiente
   - **Futuro**: Crear puerto `PaymentGatewayPort` con implementaciones por proveedor

3. **Validación en Múltiples Capas**
   - DTO: Bean Validation (`@NotNull`, `@DecimalMin`)
   - Dominio: Validaciones de negocio complejas
   - **Supuesto**: Defense in depth, cada capa valida lo que le corresponde

4. **Montos con 4 Decimales**
   - `BigDecimal(19, 4)` para soportar monedas con alta precisión
   - **Supuesto**: Compatibilidad con criptomonedas y forex

5. **Límite de Monto: $1,000,000**
   - Constraint en dominio y base de datos
   - **Supuesto**: Límite de negocio (configurable en el futuro)

### Técnicas

6. **Java 17 como Versión Mínima**
   - Spring Boot 3.2 requiere Java 17+
   - **Supuesto**: Entornos de producción tienen Java 17 disponible

7. **Transacciones Gestionadas por Spring**
   - `@Transactional` para consistencia ACID
   - **Supuesto**: Base de datos soporta transacciones (PostgreSQL, MySQL)

8. **Sin Autenticación/Autorización**
   - API expuesta sin seguridad
   - **Supuesto**: Gateway o API Manager maneja autenticación
   - **Futuro**: Integrar Spring Security con JWT/OAuth2

9. **Entorno de Ejecución: Kubernetes**
   - Diseñado para contenedores stateless
   - **Supuesto**: Escalamiento horizontal con múltiples pods

10. **Logs en STDOUT**
    - Sin log files locales
    - **Supuesto**: Logs centralizados con ELK o CloudWatch

---

## Riesgos Identificados

### Riesgos Técnicos

#### 1. Concurrencia en Procesamiento de PayIns
**Riesgo**: Dos procesos intentan procesar el mismo PayIn simultáneamente.

**Probabilidad**: Media
**Impacto**: Alto (doble cargo al cliente)

**Mitigación**:
- Implementar **locking optimista** con versioning:
  ```java
  @Version
  private Long version;
  ```
- O **locking pesimista** en queries críticas:
  ```java
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<PayInEntity> findById(UUID id);
  ```
- Idempotencia en payment gateways usando `transaction_id`

---

#### 2. Falta de Circuit Breaker para Dependencias Externas
**Riesgo**: Si un payment gateway está caído, el sistema colapsa.

**Probabilidad**: Media
**Impacto**: Alto (toda la aplicación inaccesible)

**Mitigación**:
- Implementar **Resilience4j Circuit Breaker**:
  ```java
  @CircuitBreaker(name = "paymentGateway", fallbackMethod = "fallbackProcess")
  public PayIn processWithGateway(PayIn payIn) { ... }
  ```
- Configurar timeouts (3s) y thresholds (50% failure rate)
- Fallback: marcar PayIn como `PENDING_RETRY`

---

#### 3. Sin Rate Limiting
**Riesgo**: Ataques de fuerza bruta o abuse de API.

**Probabilidad**: Alta
**Impacto**: Medio (costo de infraestructura, DoS)

**Mitigación**:
- Implementar rate limiting con **Bucket4j**:
  ```java
  @RateLimiter(name = "api", fallbackMethod = "rateLimitFallback")
  ```
- Configurar: 100 requests/minuto por IP
- Usar API Gateway (AWS API Gateway, Kong) para rate limiting global

---

#### 4. Datos Sensibles en Logs
**Riesgo**: Información de pago en logs puede ser expuesta.

**Probabilidad**: Media
**Impacto**: Crítico (violación PCI-DSS, GDPR)

**Mitigación**:
- **Nunca** loguear tokens, API keys, tarjetas completas
- Usar máscara de datos:
  ```java
  log.info("Processing PayIn for customer: {}, card: ****{}",
           customerId, lastFourDigits);
  ```
- Revisar logs regularmente con herramientas SAST

---

#### 5. Sin Backup Automático de Base de Datos
**Riesgo**: Pérdida de datos en caso de fallo de infraestructura.

**Probabilidad**: Baja
**Impacto**: Crítico (pérdida de transacciones)

**Mitigación**:
- Configurar backups automáticos diarios (RDS, CloudSQL)
- Point-in-time recovery habilitado
- Probar proceso de restore mensualmente
- RPO: < 1 hora, RTO: < 30 minutos

---

### Riesgos de Negocio

#### 6. Límite de Monto Hardcoded
**Riesgo**: Cambio de límite requiere redeployment.

**Probabilidad**: Alta
**Impacto**: Bajo (inconveniencia operacional)

**Mitigación**:
- Externalizar límites a configuración:
  ```yaml
  payin:
    limits:
      min-amount: 0.01
      max-amount: 1000000
  ```
- O gestionar límites en base de datos por cliente/cuenta

---

#### 7. Sin Soporte Multi-Tenancy
**Riesgo**: Difícil escalar a múltiples merchants o tenants.

**Probabilidad**: Media
**Impacto**: Alto (refactoring significativo)

**Mitigación**:
- Diseñar desde ahora con `tenant_id` en tablas críticas
- Filtrado automático por tenant en queries
- Segregación de datos por schema o base de datos

---

#### 8. Falta de Webhooks para Notificaciones
**Riesgo**: Clientes deben hacer polling para verificar estados.

**Probabilidad**: Alta
**Impacto**: Medio (experiencia de usuario degradada)

**Mitigación**:
- Implementar sistema de webhooks:
  ```java
  @Async
  void notifyPayInStatusChange(PayIn payIn, String webhookUrl) {
      restTemplate.postForObject(webhookUrl, event, Void.class);
  }
  ```
- Retry logic con exponential backoff
- Dead letter queue para webhooks fallidos

---

#### 9. Sin Monitoring y Alertas
**Riesgo**: Incidentes no detectados a tiempo.

**Probabilidad**: Media
**Impacto**: Alto (SLA degradado)

**Mitigación**:
- Integrar **Prometheus + Grafana** para métricas
- Alertas en Slack/PagerDuty para:
  - Tasa de PayIns fallidos > 5%
  - Latencia p99 > 1s
  - Tasa de error 5xx > 1%
- Dashboards: transacciones/minuto, success rate, latencia

---

#### 10. Escalabilidad: Sin Caching
**Riesgo**: Queries repetitivas a base de datos.

**Probabilidad**: Alta (en producción con tráfico alto)
**Impacto**: Medio (latencia aumentada)

**Mitigación**:
- Implementar **Redis** para cache de:
  - Información de payment providers
  - Límites de transacciones por cliente
  - Configuraciones de sistema
- Cache invalidation con TTL de 5 minutos
- Fallback a base de datos si Redis está caído

---

## Instalación y Ejecución

### Requisitos Previos

- **Java 17** o superior
- **Maven 3.8+**
- (Opcional) Docker para ejecución en contenedor

### Configuración de Java 17

```bash
# Verificar versión actual
java -version

# Cambiar a Java 17 (si está instalado)
sudo update-alternatives --config java
sudo update-alternatives --config javac

# O configurar JAVA_HOME
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH
```

### Compilar y Ejecutar

```bash
# Clonar repositorio
git clone https://github.com/your-org/payin-service.git
cd payin-service

# Compilar
mvn clean compile

# Ejecutar aplicación
mvn spring-boot:run

# O con Java directamente
mvn package
java -jar target/payin-service-0.0.1-SNAPSHOT.jar
```

La aplicación estará disponible en: **http://localhost:8080**

### Ejecutar con Docker

```bash
# Construir imagen
docker build -t payin-service:latest .

# Ejecutar contenedor
docker run -p 8080:8080 payin-service:latest
```

### Acceso a H2 Console

1. Navegar a: http://localhost:8080/h2-console
2. Configurar conexión:
   - JDBC URL: `jdbc:h2:mem:payindb`
   - Username: `sa`
   - Password: `password`

---

## API Endpoints

### 1. Crear PayIn

```http
POST /api/v1/payins
Content-Type: application/json

{
  "amount": 100.50,
  "currency": "USD",
  "customerId": "CUST001",
  "paymentMethodId": "PM001",
  "description": "Subscription payment"
}
```

**Response** (201 Created):
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "transactionId": "TXN1704067200001",
  "amount": 100.50,
  "currency": "USD",
  "customerId": "CUST001",
  "paymentMethodId": "PM001",
  "status": "VALIDATED",
  "description": "Subscription payment",
  "errorMessage": null,
  "createdAt": "2024-01-01T10:00:00.000Z",
  "updatedAt": "2024-01-01T10:00:00.000Z"
}
```

### 2. Procesar PayIn

```http
POST /api/v1/payins/{id}/process
```

**Response** (200 OK):
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "transactionId": "TXN1704067200001",
  "status": "PROCESSED",
  ...
}
```

### 3. Consultar PayIn por ID

```http
GET /api/v1/payins/{id}
```

### 4. Consultar PayIn por Transaction ID

```http
GET /api/v1/payins/transaction/{transactionId}
```

### Documentación Completa

Swagger UI: **http://localhost:8080/swagger-ui.html**

---

## Testing

### Ejecutar Tests

```bash
# Todos los tests
mvn test

# Test específico
mvn test -Dtest=PayInServiceTest

# Con cobertura (JaCoCo)
mvn test jacoco:report
# Reporte en: target/site/jacoco/index.html
```

### Testing Manual con cURL

```bash
# Crear PayIn
curl -X POST http://localhost:8080/api/v1/payins \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 100.50,
    "currency": "USD",
    "customerId": "CUST001",
    "paymentMethodId": "PM001",
    "description": "Test payment"
  }'

# Procesar PayIn (reemplazar {id})
curl -X POST http://localhost:8080/api/v1/payins/{id}/process

# Consultar PayIn
curl http://localhost:8080/api/v1/payins/{id}
```

---

## Documentación Adicional

- [DESIGN_PATTERNS.md](DESIGN_PATTERNS.md) - Patrones de diseño aplicados
- [CI_CD_STRATEGY.md](CI_CD_STRATEGY.md) - Modelo de integración continua
- [database/DATABASE_DESIGN.md](database/DATABASE_DESIGN.md) - Diseño de base de datos
- [database/schema.sql](database/schema.sql) - Scripts SQL del modelo normalizado
- [CLAUDE.md](CLAUDE.md) - Guía para Claude Code

---

## Roadmap

### Fase 1: MVP (Completado)
- ✅ Arquitectura hexagonal
- ✅ Modelo de dominio con validaciones
- ✅ Persistencia con JPA/H2
- ✅ API REST con OpenAPI
- ✅ Documentación completa

### Fase 2: Production-Ready (Q1 2024)
- ⏳ Tests unitarios y de integración (80% coverage)
- ⏳ Autenticación con Spring Security + JWT
- ⏳ Rate limiting con Bucket4j
- ⏳ Monitoring con Prometheus/Grafana
- ⏳ CI/CD con GitHub Actions

### Fase 3: Advanced Features (Q2 2024)
- 📋 Integración real con payment gateways (Stripe, PayPal)
- 📋 Webhooks para notificaciones
- 📋 Event sourcing completo con Kafka
- 📋 Cache con Redis
- 📋 Multi-tenancy

---

## Contribución

### Branching Strategy

- `main`: Código en producción
- `develop`: Integración continua
- `feature/*`: Nuevas funcionalidades
- `bugfix/*`: Correcciones de bugs
- `hotfix/*`: Correcciones urgentes

### Code Review Checklist

- ✅ Tests pasando (coverage > 80%)
- ✅ Sin warnings de SonarQube
- ✅ Documentación actualizada
- ✅ CHANGELOG.md actualizado

---

## Licencia

Este proyecto es propietario. Todos los derechos reservados.

---

## Contacto

- **Autor**: Tu Nombre
- **Email**: tu.email@example.com
- **GitHub**: https://github.com/your-org/payin-service

---

## Agradecimientos

Desarrollado como parte de una prueba técnica para demostrar:
- Diseño de componentes desacoplados y extensibles
- Modelado correcto de dominio transaccional
- Decisiones arquitectónicas fundamentadas
- Pensamiento en términos de plataforma
