# Diagramas de Arquitectura - PayIn Service

## 1. Arquitectura Hexagonal (High-Level)

```
┌─────────────────────────────────────────────────────────────────┐
│                        ADAPTADORES DE ENTRADA                   │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐         │
│  │ REST API     │  │ GraphQL      │  │ Message      │         │
│  │ Controller   │  │ (future)     │  │ Queue        │         │
│  │              │  │              │  │ (future)     │         │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘         │
│         │                 │                 │                  │
└─────────┼─────────────────┼─────────────────┼──────────────────┘
          │                 │                 │
          v                 v                 v
┌─────────────────────────────────────────────────────────────────┐
│                     PUERTOS DE ENTRADA                          │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │              PayInUseCase (interface)                    │  │
│  │  - createPayIn(PayIn): PayIn                            │  │
│  │  - processPayIn(UUID): PayIn                            │  │
│  │  - getPayIn(UUID): PayIn                                │  │
│  │  - getPayInByTransactionId(String): PayIn              │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────┬───────────────────────────────────┘
                              │
                              v
┌─────────────────────────────────────────────────────────────────┐
│                       NÚCLEO DE DOMINIO                         │
│  ┌────────────────────────────────────────────────────────┐    │
│  │               PayInService (implementa PayInUseCase)   │    │
│  │  - Orquesta lógica de negocio                         │    │
│  │  - Coordina dominio y persistencia                    │    │
│  │  - Gestiona transacciones                             │    │
│  └──────────────────┬─────────────────────────────────────┘    │
│                     │                                           │
│  ┌──────────────────v─────────────────────────────────────┐    │
│  │           PayIn (Aggregate Root)                       │    │
│  │  - id, transactionId, amount, currency, status        │    │
│  │  - validate(): void                                   │    │
│  │  - process(): void                                    │    │
│  │  - fail(String): void                                 │    │
│  └────────────────────────────────────────────────────────┘    │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │         DomainException (excepciones de negocio)       │   │
│  │  - errorCode (PAYIN-001, PAYIN-002, etc.)             │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────┬───────────────────────────────────┘
                              │
                              v
┌─────────────────────────────────────────────────────────────────┐
│                     PUERTOS DE SALIDA                           │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │         PayInRepositoryPort (interface)                  │  │
│  │  - save(PayIn): PayIn                                   │  │
│  │  - findById(UUID): Optional<PayIn>                      │  │
│  │  - findByTransactionId(String): Optional<PayIn>        │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────┬───────────────────────────────────┘
                              │
                              v
┌─────────────────────────────────────────────────────────────────┐
│                    ADAPTADORES DE SALIDA                        │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐         │
│  │ JPA/H2       │  │ Payment      │  │ Event        │         │
│  │ Repository   │  │ Gateway      │  │ Publisher    │         │
│  │ Adapter      │  │ (future)     │  │ (future)     │         │
│  └──────┬───────┘  └──────────────┘  └──────────────┘         │
│         │                                                       │
└─────────┼───────────────────────────────────────────────────────┘
          │
          v
    ┌──────────┐
    │ Database │
    │ (H2/PG)  │
    └──────────┘
```

---

## 2. Flujo de Procesamiento de PayIn

```mermaid
graph TD
    A[Cliente HTTP] -->|POST /api/v1/payins| B[PayInController]
    B -->|Validar DTO| C{DTO válido?}
    C -->|No| D[400 Bad Request]
    C -->|Sí| E[PayInService.createPayIn]

    E -->|1. Crear PayIn| F[PayIn.builder]
    F -->|2. Generar transaction_id| G[TXN + timestamp + random]
    G -->|3. Set status=CREATED| H[PayIn domain object]

    H -->|4. PayIn.validate| I{Validaciones de negocio}
    I -->|Fallo| J[DomainException]
    J --> K[PayInController @ExceptionHandler]
    K --> L[400 Bad Request con error code]

    I -->|Éxito| M[status=VALIDATED]
    M -->|5. PayInRepositoryPort.save| N[PayInRepositoryAdapter]
    N -->|6. PayInEntity.fromDomain| O[Convertir a JPA Entity]
    O -->|7. JpaPayInRepository.save| P[Persistir en H2]
    P -->|8. PayInEntity.toDomain| Q[Convertir a Domain]
    Q -->|9. PayInResponse.fromDomain| R[Convertir a DTO]
    R --> S[201 Created + JSON response]

    style A fill:#e1f5ff
    style B fill:#b3e5fc
    style E fill:#81d4fa
    style I fill:#ffe082
    style J fill:#ffccbc
    style M fill:#c5e1a5
    style P fill:#ce93d8
    style S fill:#a5d6a7
```

---

## 3. Diagrama de Secuencia: Crear y Procesar PayIn

```mermaid
sequenceDiagram
    participant Client
    participant Controller as PayInController
    participant Service as PayInService
    participant Domain as PayIn (Domain)
    participant Adapter as PayInRepositoryAdapter
    participant JPA as JpaPayInRepository
    participant DB as Database

    %% Crear PayIn
    rect rgb(200, 230, 255)
        Note over Client,DB: 1. Crear PayIn
        Client->>Controller: POST /api/v1/payins
        Controller->>Controller: Validar DTO (Bean Validation)
        Controller->>Service: createPayIn(payIn)
        Service->>Domain: new PayIn() + set fields
        Service->>Domain: validate()
        Domain->>Domain: validateAmount()
        Domain->>Domain: validateCurrency()
        Domain->>Domain: validateCustomer()
        Domain->>Domain: validatePaymentMethod()
        Domain-->>Service: status = VALIDATED
        Service->>Adapter: save(payIn)
        Adapter->>Adapter: PayInEntity.fromDomain(payIn)
        Adapter->>JPA: save(entity)
        JPA->>DB: INSERT INTO payins...
        DB-->>JPA: Saved entity
        JPA-->>Adapter: PayInEntity
        Adapter->>Adapter: entity.toDomain()
        Adapter-->>Service: PayIn (domain)
        Service-->>Controller: PayIn
        Controller->>Controller: PayInResponse.fromDomain()
        Controller-->>Client: 201 Created + JSON
    end

    %% Procesar PayIn
    rect rgb(200, 255, 200)
        Note over Client,DB: 2. Procesar PayIn
        Client->>Controller: POST /api/v1/payins/{id}/process
        Controller->>Service: processPayIn(id)
        Service->>Adapter: findById(id)
        Adapter->>JPA: findById(id)
        JPA->>DB: SELECT * FROM payins WHERE id=?
        DB-->>JPA: PayInEntity
        JPA-->>Adapter: Optional<PayInEntity>
        Adapter->>Adapter: entity.toDomain()
        Adapter-->>Service: PayIn (domain)
        Service->>Domain: process()
        alt Estado válido (VALIDATED)
            Domain->>Domain: status = PROCESSED
            Domain-->>Service: Success
            Service->>Adapter: save(payIn)
            Adapter->>JPA: save(entity)
            JPA->>DB: UPDATE payins SET status='PROCESSED'...
            DB-->>JPA: Updated entity
            JPA-->>Adapter: PayInEntity
            Adapter-->>Service: PayIn
            Service-->>Controller: PayIn
            Controller-->>Client: 200 OK + JSON
        else Estado inválido
            Domain-->>Service: IllegalStateException
            Service-->>Controller: Exception
            Controller-->>Client: 400 Bad Request
        end
    end

    %% Caso de fallo
    rect rgb(255, 200, 200)
        Note over Client,DB: 3. PayIn Falla
        Service->>Domain: process()
        Domain->>Domain: Call external service (future)
        Domain-->>Service: Exception
        Service->>Domain: fail(errorMessage)
        Domain->>Domain: status = FAILED
        Service->>Adapter: save(payIn)
        Adapter->>JPA: save(entity)
        JPA->>DB: UPDATE payins SET status='FAILED'...
        Service-->>Controller: PayIn (FAILED)
        Controller-->>Client: 200 OK + JSON (status=FAILED)
    end
```

---

## 4. Diagrama de Componentes

```
┌─────────────────────────────────────────────────────────────────────┐
│                           PayIn Service                             │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌───────────────────────────────────────────────────────────────┐ │
│  │              Interfaces Layer (com.payin.interfaces)          │ │
│  │  ┌──────────────┐  ┌─────────────────────────────────────┐   │ │
│  │  │ PayIn        │  │ DTOs                                │   │ │
│  │  │ Controller   │──┤ - CreatePayInRequest                │   │ │
│  │  │              │  │ - PayInResponse                     │   │ │
│  │  │ @RestController  │ - Error responses                  │   │ │
│  │  └──────┬───────┘  └─────────────────────────────────────┘   │ │
│  │         │                                                     │ │
│  └─────────┼─────────────────────────────────────────────────────┘ │
│            │                                                       │
│            │ depends on                                            │
│            v                                                       │
│  ┌─────────────────────────────────────────────────────────────┐  │
│  │              Domain Layer (com.payin.domain)                │  │
│  │                                                             │  │
│  │  ┌─────────────────────────────────────────────────────┐   │  │
│  │  │ Ports (Interfaces)                                  │   │  │
│  │  │  ┌───────────────┐    ┌────────────────────────┐   │   │  │
│  │  │  │ Input Ports   │    │ Output Ports           │   │   │  │
│  │  │  │ PayInUseCase  │    │ PayInRepositoryPort    │   │   │  │
│  │  │  └───────┬───────┘    └────────────┬───────────┘   │   │  │
│  │  └──────────┼────────────────────────┼───────────────┘   │  │
│  │             │                        │                   │  │
│  │             │ implemented by         │ used by           │  │
│  │             v                        │                   │  │
│  │  ┌──────────────────────────────────┼────────────────┐  │  │
│  │  │ Services                         │                │  │  │
│  │  │ PayInService ────────────────────┘                │  │  │
│  │  │ @Service                                          │  │  │
│  │  │ @Transactional                                    │  │  │
│  │  └──────────────────────┬────────────────────────────┘  │  │
│  │                         │                               │  │
│  │                         │ uses                          │  │
│  │                         v                               │  │
│  │  ┌─────────────────────────────────────────────────┐   │  │
│  │  │ Domain Models                                   │   │  │
│  │  │ ┌─────────────┐  ┌─────────────┐              │   │  │
│  │  │ │ PayIn       │  │ Customer    │              │   │  │
│  │  │ │ (Aggregate) │  │ Account     │              │   │  │
│  │  │ │             │  │ PaymentMethod              │   │  │
│  │  │ │ - validate()│  │ PaymentProvider             │   │  │
│  │  │ │ - process() │  │                             │   │  │
│  │  │ │ - fail()    │  │                             │   │  │
│  │  │ └─────────────┘  └─────────────┘              │   │  │
│  │  └─────────────────────────────────────────────────┘   │  │
│  │                                                         │  │
│  │  ┌─────────────────────────────────────────────────┐   │  │
│  │  │ Domain Exceptions                               │   │  │
│  │  │ DomainException (errorCode + message)          │   │  │
│  │  └─────────────────────────────────────────────────┘   │  │
│  └─────────────────────────────────────────────────────────┘  │
│                         │                                     │
│                         │ depends on                          │
│                         v                                     │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │       Infrastructure Layer (com.payin.infrastructure)   │  │
│  │                                                         │  │
│  │  ┌──────────────────────────────────────────────────┐  │  │
│  │  │ Adapters (implements output ports)              │  │  │
│  │  │ ┌────────────────────────────────────────────┐  │  │  │
│  │  │ │ PayInRepositoryAdapter                     │  │  │  │
│  │  │ │ implements PayInRepositoryPort             │  │  │  │
│  │  │ │ @Component                                 │  │  │  │
│  │  │ └────────────┬───────────────────────────────┘  │  │  │
│  │  └──────────────┼──────────────────────────────────┘  │  │
│  │                 │                                     │  │
│  │                 │ uses                                │  │
│  │                 v                                     │  │
│  │  ┌──────────────────────────────────────────────────┐  │  │
│  │  │ JPA Repositories                                 │  │  │
│  │  │ JpaPayInRepository extends JpaRepository         │  │  │
│  │  │ @Repository                                      │  │  │
│  │  └────────────┬─────────────────────────────────────┘  │  │
│  │               │                                        │  │
│  │               │ manages                                │  │
│  │               v                                        │  │
│  │  ┌──────────────────────────────────────────────────┐  │  │
│  │  │ JPA Entities                                     │  │  │
│  │  │ ┌────────────────────────────────────────────┐  │  │  │
│  │  │ │ PayInEntity                                │  │  │  │
│  │  │ │ @Entity @Table(name="payins")             │  │  │  │
│  │  │ │                                            │  │  │  │
│  │  │ │ + fromDomain(PayIn): PayInEntity          │  │  │  │
│  │  │ │ + toDomain(): PayIn                       │  │  │  │
│  │  │ └────────────────────────────────────────────┘  │  │  │
│  │  └──────────────────────────────────────────────────┘  │  │
│  └─────────────────────────────────────────────────────────┘  │
│                         │                                     │
│                         │ persists to                         │
│                         v                                     │
│                  ┌─────────────┐                              │
│                  │   Database  │                              │
│                  │   H2/PG     │                              │
│                  └─────────────┘                              │
└─────────────────────────────────────────────────────────────────┘

External Dependencies:
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│ Spring Boot  │    │   Lombok     │    │  MapStruct   │
└──────────────┘    └──────────────┘    └──────────────┘
```

---

## 5. Diagrama de Estados del PayIn

```
                    ┌─────────────────┐
                    │    CREATED      │
                    │  (Estado inicial│
                    │   al crear)     │
                    └────────┬────────┘
                             │
                             │ validate()
                             │ - validateAmount()
                             │ - validateCurrency()
                             │ - validateCustomer()
                             │ - validatePaymentMethod()
                             │
                             v
                    ┌─────────────────┐
                    │   VALIDATED     │
                    │ (Listo para     │
                    │  procesar)      │
                    └────────┬────────┘
                             │
                             │ process()
                ┌────────────┼────────────┐
                │                         │
                │ Success                 │ Failure
                │                         │
                v                         v
    ┌─────────────────┐        ┌─────────────────┐
    │   PROCESSED     │        │     FAILED      │
    │ (Transacción    │        │ (Transacción    │
    │  completada)    │        │  fallida)       │
    └─────────────────┘        └─────────────────┘
                                        │
                                        │ Contiene
                                        │ errorMessage
                                        v
                              ┌──────────────────────┐
                              │ "Insufficient funds" │
                              │ "Gateway timeout"    │
                              │ "Invalid card"       │
                              └──────────────────────┘

Invariantes:
- Solo se puede llamar process() si status == VALIDATED
- Una vez PROCESSED o FAILED, el estado es final (no hay transiciones)
- Cada transición actualiza updatedAt timestamp
- fail() puede ser llamado desde cualquier estado

Validaciones por Estado:
┌──────────┬────────────────────────────────────────────┐
│ CREATED  │ - ID generado (UUID)                      │
│          │ - transactionId generado                  │
│          │ - createdAt = now()                       │
└──────────┴────────────────────────────────────────────┘
┌──────────┬────────────────────────────────────────────┐
│VALIDATED │ - amount > 0 AND <= 1,000,000             │
│          │ - currency es código ISO 3 letras         │
│          │ - customerId no vacío                     │
│          │ - paymentMethodId no vacío                │
└──────────┴────────────────────────────────────────────┘
┌──────────┬────────────────────────────────────────────┐
│PROCESSED │ - Payment gateway confirmó                │
│          │ - Fondos debitados                        │
│          │ - updatedAt actualizado                   │
└──────────┴────────────────────────────────────────────┘
┌──────────┬────────────────────────────────────────────┐
│ FAILED   │ - errorMessage poblado                    │
│          │ - updatedAt actualizado                   │
│          │ - Gateway rechazó o timeout               │
└──────────┴────────────────────────────────────────────┘
```

---

## 6. Diagrama de Despliegue

```
┌─────────────────────────────────────────────────────────────────┐
│                         AWS / GCP / Azure                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌───────────────────────────────────────────────────────────┐ │
│  │                   API Gateway / Load Balancer             │ │
│  │  - Rate limiting                                          │ │
│  │  - SSL termination                                        │ │
│  │  - Request routing                                        │ │
│  └──────────────────────────┬────────────────────────────────┘ │
│                             │                                  │
│                             v                                  │
│  ┌───────────────────────────────────────────────────────────┐ │
│  │              Kubernetes Cluster (EKS/GKE/AKS)             │ │
│  │                                                           │ │
│  │  ┌────────────┐  ┌────────────┐  ┌────────────┐        │ │
│  │  │ PayIn Pod 1│  │ PayIn Pod 2│  │ PayIn Pod N│        │ │
│  │  │            │  │            │  │            │        │ │
│  │  │ Java 17    │  │ Java 17    │  │ Java 17    │        │ │
│  │  │ Spring Boot│  │ Spring Boot│  │ Spring Boot│        │ │
│  │  │ Port: 8080 │  │ Port: 8080 │  │ Port: 8080 │        │ │
│  │  └─────┬──────┘  └─────┬──────┘  └─────┬──────┘        │ │
│  │        │               │               │                │ │
│  │        └───────────────┴───────────────┘                │ │
│  │                        │                                │ │
│  │                        v                                │ │
│  │        ┌───────────────────────────────┐               │ │
│  │        │    Service (ClusterIP)        │               │ │
│  │        └───────────────┬───────────────┘               │ │
│  └────────────────────────┼───────────────────────────────┘ │
│                           │                                  │
│       ┌───────────────────┴───────────────────┐             │
│       │                                       │             │
│       v                                       v             │
│  ┌─────────────────┐              ┌─────────────────┐      │
│  │   PostgreSQL    │              │  Redis Cache    │      │
│  │   (RDS/Cloud    │              │  (ElastiCache)  │      │
│  │    SQL)         │              │                 │      │
│  │  - Primary      │              │ - TTL: 5 min    │      │
│  │  - Replica(s)   │              │ - Max memory:   │      │
│  │  - Auto-backup  │              │   2GB           │      │
│  └─────────────────┘              └─────────────────┘      │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │           Observability Stack                       │   │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────┐ │   │
│  │  │ Prometheus   │  │  Grafana     │  │  ELK     │ │   │
│  │  │ (Metrics)    │  │ (Dashboards) │  │ (Logs)   │ │   │
│  │  └──────────────┘  └──────────────┘  └──────────┘ │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
└─────────────────────────────────────────────────────────────┘

External Services:
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│   Stripe     │    │   PayPal     │    │ Notification │
│   Gateway    │    │   Gateway    │    │   Service    │
└──────────────┘    └──────────────┘    └──────────────┘

Resource Specifications:
- PayIn Pods: 2 CPU, 4GB RAM, 3 replicas
- PostgreSQL: db.t3.medium (2 vCPU, 4GB RAM)
- Redis: cache.t3.micro (2 vCPU, 1GB RAM)
- Auto-scaling: Min 3, Max 10 pods (CPU > 70%)
```

---

## 7. Mapa de Dependencias

```
┌─────────────────────────────────────────────────────────────┐
│                      PayIn Service                          │
│                                                             │
│  Dependencies:                                              │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ Spring Boot Ecosystem                               │   │
│  │ ┌─────────────────┐  ┌────────────────────────┐    │   │
│  │ │ spring-boot-    │  │ spring-boot-starter-   │    │   │
│  │ │ starter-web     │  │ data-jpa               │    │   │
│  │ │ (Tomcat, REST)  │  │ (Hibernate, JPA)       │    │   │
│  │ └─────────────────┘  └────────────────────────┘    │   │
│  │ ┌─────────────────┐  ┌────────────────────────┐    │   │
│  │ │ spring-boot-    │  │ springdoc-openapi-     │    │   │
│  │ │ starter-        │  │ starter-webmvc-ui      │    │   │
│  │ │ validation      │  │ (Swagger docs)         │    │   │
│  │ └─────────────────┘  └────────────────────────┘    │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ Database                                            │   │
│  │ ┌─────────────────┐  ┌────────────────────────┐    │   │
│  │ │ H2 Database     │  │ PostgreSQL Driver      │    │   │
│  │ │ (Development)   │  │ (Production)           │    │   │
│  │ └─────────────────┘  └────────────────────────┘    │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ Code Generation                                     │   │
│  │ ┌─────────────────┐  ┌────────────────────────┐    │   │
│  │ │ Lombok          │  │ MapStruct              │    │   │
│  │ │ (Boilerplate)   │  │ (Object mapping)       │    │   │
│  │ └─────────────────┘  └────────────────────────┘    │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ Testing (future)                                    │   │
│  │ ┌─────────────────┐  ┌────────────────────────┐    │   │
│  │ │ JUnit 5         │  │ Mockito                │    │   │
│  │ └─────────────────┘  └────────────────────────┘    │   │
│  │ ┌─────────────────┐  ┌────────────────────────┐    │   │
│  │ │ TestContainers  │  │ REST Assured           │    │   │
│  │ └─────────────────┘  └────────────────────────┘    │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘

Dependency Version Management:
- Spring Boot BOM: 3.2.0
- Java Version: 17
- Maven: 3.8+
```

---

## Leyenda de Notación

- **Rectángulos**: Componentes o módulos
- **Flechas sólidas**: Dependencias (depends on)
- **Flechas punteadas**: Interfaces/contratos
- **Diamantes**: Composición
- **Círculos**: Puertos (interfaces)
- **Color azul**: Capa de interfaces
- **Color verde**: Capa de dominio
- **Color morado**: Capa de infraestructura
