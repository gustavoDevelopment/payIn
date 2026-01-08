# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

PayIn Transaction Service - A Spring Boot 3.2 payment transaction system implementing Hexagonal Architecture (Ports & Adapters pattern).

## Technology Stack

- Java 17
- Spring Boot 3.2.0 with Spring Data JPA
- H2 in-memory database
- Lombok and MapStruct for code generation
- SpringDoc OpenAPI for API documentation
- Maven build tool

## Development Commands

### Build and Run
```bash
# Compile the project
mvn clean compile

# Run the application (starts on port 8080)
mvn spring-boot:run

# Build executable JAR
mvn clean package

# Run tests
mvn test

# Run specific test class
mvn test -Dtest=ClassName

# Run specific test method
mvn test -Dtest=ClassName#methodName
```

### Access Points
- Application: http://localhost:8080
- API Documentation (Swagger UI): http://localhost:8080/swagger-ui.html
- H2 Console: http://localhost:8080/h2-console
  - JDBC URL: `jdbc:h2:mem:payindb`
  - Username: `sa`
  - Password: `password`

## Architecture

The codebase follows **Hexagonal Architecture** with clear separation of concerns across three main layers:

### 1. Domain Layer (`com.payin.domain`)
Contains pure business logic with no external dependencies.

- **`model/`**: Domain entities with embedded business rules
  - `PayIn`: Core aggregate root with state machine (CREATED → VALIDATED → PROCESSED/FAILED)
  - `Customer`, `Account`, `PaymentMethod`: Supporting domain models

- **`ports/input/`**: Use case interfaces (application entry points)
  - `PayInUseCase`: Defines operations like createPayIn, processPayIn

- **`ports/output/`**: Repository interfaces (persistence contracts)
  - `PayInRepositoryPort`: Data access contract

- **`service/`**: Domain services implementing use cases
  - `PayInService`: Orchestrates domain logic and persistence

- **`exceptions/`**: Domain-specific exceptions
  - `DomainException`: Business rule violations with error codes (PAYIN-001, etc.)

### 2. Infrastructure Layer (`com.payin.infrastructure`)
Implements technical concerns and external integrations.

- **`adapter/`**: Implements output ports
  - `PayInRepositoryAdapter`: Bridges domain and JPA repository

- **`entity/`**: JPA entities (separate from domain models)
  - `PayInEntity`: Database representation with `fromDomain()` and `toDomain()` converters

- **`repository/`**: Spring Data JPA repositories
  - `JpaPayInRepository`: Extends JpaRepository for database operations

### 3. Interfaces Layer (`com.payin.interfaces`)
Exposes application functionality to external consumers.

- **`rest/`**: REST API controllers
  - `PayInController`: Exposes endpoints at `/api/v1/payins`

- **`rest/dto/`**: Data Transfer Objects
  - `CreatePayInRequest`, `PayInResponse`: API request/response models

## Key Architectural Patterns

### Domain-Driven Design
- Domain models contain business logic (e.g., `PayIn.validate()`, `PayIn.process()`)
- Rich domain model with behavior, not anemic data containers
- Business rules validated within domain entities

### Dependency Inversion
- Domain layer defines ports (interfaces), infrastructure implements them
- Domain has zero dependencies on infrastructure or frameworks
- Adapters translate between domain models and infrastructure concerns

### Entity Separation
- Domain models (e.g., `PayIn`) separate from JPA entities (`PayInEntity`)
- Explicit conversion via `fromDomain()` and `toDomain()` methods
- Prevents persistence concerns from leaking into business logic

### PayIn State Machine
Strict state transitions enforced by domain methods:
```
CREATED → validate() → VALIDATED → process() → PROCESSED
                                 ↘ fail() → FAILED
```

State validation occurs in domain methods, throwing exceptions for invalid transitions.

### Domain Exception Handling
Business rule violations throw `DomainException` with structured error codes:
- Format: `PAYIN-XXX` (e.g., PAYIN-001 for null amount)
- Enables client-side error handling and i18n
- Controller layer handles exceptions and maps to HTTP status codes

## Annotation Processing

The project uses both Lombok and MapStruct annotation processors configured in pom.xml:97-108. When modifying Lombok-annotated classes or adding MapStruct mappers, Maven will automatically regenerate code during compilation.
