# Patrones de Diseño Aplicados

## 1. Hexagonal Architecture (Ports & Adapters)

**Ubicación**: Estructura completa del proyecto

**Descripción**: La arquitectura principal del sistema separa la lógica de negocio del dominio de las preocupaciones técnicas.

**Implementación**:
- **Domain Layer** (`com.payin.domain`): Núcleo de negocio sin dependencias externas
- **Ports** (`domain.ports.input` y `domain.ports.output`): Interfaces que definen contratos
  - `PayInUseCase`: Puerto de entrada para casos de uso
  - `PayInRepositoryPort`: Puerto de salida para persistencia
- **Adapters** (`infrastructure.adapter`): Implementaciones de puertos
  - `PayInRepositoryAdapter`: Adapta el repositorio JPA al puerto de dominio
- **Interfaces** (`interfaces.rest`): Adaptadores de entrada (API REST)

**Beneficios**:
- Lógica de negocio independiente de frameworks
- Fácil testing con mocks
- Componentes intercambiables

---

## 2. Domain-Driven Design (DDD)

**Ubicación**: `com.payin.domain.model.PayIn`

**Descripción**: El modelo de dominio contiene la lógica de negocio, no es un simple contenedor de datos.

**Implementación**:
- Entidad `PayIn` con métodos de negocio: `validate()`, `process()`, `fail()`
- Reglas de negocio encapsuladas en el modelo
- Validaciones de dominio con excepciones específicas (`DomainException`)
- Enum `PayInStatus` dentro del agregado

**Ejemplo**:
```java
public void validate() {
    validateAmount();
    validateCurrency();
    validateCustomer();
    validatePaymentMethod();
    this.status = PayInStatus.VALIDATED;
}
```

---

## 3. Repository Pattern

**Ubicación**: `domain.ports.output.PayInRepositoryPort` y `infrastructure`

**Descripción**: Abstrae el acceso a datos mediante interfaces.

**Implementación**:
- **Puerto de dominio**: `PayInRepositoryPort` define el contrato
- **Adaptador**: `PayInRepositoryAdapter` implementa el puerto
- **JPA Repository**: `JpaPayInRepository` maneja la persistencia real
- **Entity Mapper**: `PayInEntity` con métodos `fromDomain()` y `toDomain()`

**Beneficios**:
- Dominio desacoplado de la tecnología de persistencia
- Fácil cambio de base de datos
- Testing simplificado con mocks

---

## 4. State Pattern

**Ubicación**: `com.payin.domain.model.PayIn`

**Descripción**: Gestiona las transiciones de estado del PayIn mediante una máquina de estados.

**Implementación**:
```
CREATED → validate() → VALIDATED → process() → PROCESSED
                                 ↘ fail() → FAILED
```

**Validaciones**:
- `process()` solo puede ejecutarse si el estado es `VALIDATED`
- Transiciones controladas mediante métodos del dominio
- Estados representados por enum `PayInStatus`

**Ejemplo**:
```java
public void process() {
    if (this.status != PayInStatus.VALIDATED) {
        throw new IllegalStateException("PayIn must be validated before processing");
    }
    this.status = PayInStatus.PROCESSED;
}
```

---

## 5. Builder Pattern

**Ubicación**: Todos los modelos de dominio y DTOs

**Descripción**: Facilita la creación de objetos complejos mediante Lombok.

**Implementación**:
```java
@Data
@Builder
public class PayIn {
    // fields...
}
```

**Uso**:
```java
PayIn payIn = PayIn.builder()
    .amount(request.getAmount())
    .currency(request.getCurrency())
    .customerId(request.getCustomerId())
    .build();
```

---

## 6. Dependency Injection (IoC)

**Ubicación**: Toda la aplicación

**Descripción**: Spring gestiona las dependencias mediante inyección.

**Implementación**:
- `@Service`, `@Component`, `@Repository` para beans
- `@RequiredArgsConstructor` (Lombok) para inyección por constructor
- Interfaces inyectadas, no implementaciones concretas

**Ejemplo**:
```java
@Service
@RequiredArgsConstructor
public class PayInService implements PayInUseCase {
    private final PayInRepositoryPort payInRepository;
}
```

---

## 7. Data Transfer Object (DTO) Pattern

**Ubicación**: `com.payin.interfaces.rest.dto`

**Descripción**: Objetos específicos para transferencia de datos en la capa de presentación.

**Implementación**:
- `CreatePayInRequest`: DTO de entrada con validaciones Jakarta
- `PayInResponse`: DTO de salida con formato JSON específico
- Conversión explícita entre DTOs y modelos de dominio

**Beneficios**:
- API desacoplada del modelo de dominio
- Validaciones en capa de presentación
- Control de formato de respuesta

---

## 8. Strategy Pattern (Implícito)

**Ubicación**: Puertos de dominio

**Descripción**: Los puertos permiten múltiples implementaciones intercambiables.

**Implementación**:
- `PayInRepositoryPort` puede tener múltiples implementaciones:
  - `PayInRepositoryAdapter` (JPA/H2)
  - Podría agregarse: `PayInMongoRepositoryAdapter`, `PayInInMemoryAdapter`, etc.

**Extensibilidad**:
```java
// Se puede agregar fácilmente:
@Component
public class PayInMongoAdapter implements PayInRepositoryPort {
    // implementación con MongoDB
}
```

---

## 9. Exception Handling Pattern

**Ubicación**: `domain.exceptions.DomainException` y `PayInController`

**Descripción**: Manejo estructurado de errores con códigos identificadores.

**Implementación**:
- `DomainException` con código de error (`PAYIN-001`, `PAYIN-002`, etc.)
- Excepciones de dominio para violaciones de reglas de negocio
- `@ExceptionHandler` en controller para mapeo HTTP

**Ejemplo**:
```java
throw new DomainException("El monto debe ser mayor a cero", "PAYIN-002");
```

---

## 10. Service Layer Pattern

**Ubicación**: `com.payin.domain.service.PayInService`

**Descripción**: Orquesta la lógica de negocio y coordina entre dominio y persistencia.

**Implementación**:
- Implementa casos de uso (`PayInUseCase`)
- Coordina transacciones con `@Transactional`
- Orquesta llamadas al dominio y repositorio

---

## 11. Entity Separation Pattern

**Ubicación**: `domain.model.PayIn` vs `infrastructure.entity.PayInEntity`

**Descripción**: Separa el modelo de dominio de la representación de persistencia.

**Implementación**:
- **PayIn**: Modelo de dominio puro con lógica de negocio
- **PayInEntity**: Entidad JPA con anotaciones de persistencia
- Métodos de conversión: `fromDomain()` y `toDomain()`

**Beneficios**:
- Dominio libre de anotaciones de persistencia
- Flexibilidad para cambiar ORM
- Separación de responsabilidades

---

## Resumen de Aplicación de Principios SOLID

- **Single Responsibility**: Cada clase tiene una responsabilidad única
- **Open/Closed**: Extensible mediante puertos sin modificar código existente
- **Liskov Substitution**: Implementaciones de puertos son intercambiables
- **Interface Segregation**: Interfaces pequeñas y específicas (puertos)
- **Dependency Inversion**: Dependencias sobre abstracciones (puertos), no implementaciones
