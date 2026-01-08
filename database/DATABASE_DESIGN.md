# Diseño de Base de Datos - PayIn Service

## Modelo Entidad-Relación (ERD)

```
┌─────────────────────┐
│     CUSTOMERS       │
├─────────────────────┤
│ PK id (UUID)        │
│    document_type    │
│    document_number  │
│    first_name       │
│    last_name        │
│ UK email            │
│    phone            │
│    active           │
│    created_at       │
│    updated_at       │
└──────────┬──────────┘
           │ 1
           │
           │ N
┌──────────┴──────────┐         ┌─────────────────────┐
│  PAYMENT_METHODS    │         │  PAYMENT_PROVIDERS  │
├─────────────────────┤         ├─────────────────────┤
│ PK id (UUID)        │         │ PK id (UUID)        │
│ FK customer_id      │         │ UK code             │
│    type             │         │    name             │
│    token            │         │    base_url         │
│    is_default       │         │    api_key          │
│    last_four_digits │         │    webhook_secret   │
│    provider         │         │    active           │
│    active           │         │    priority         │
│    created_at       │         │    created_at       │
│    updated_at       │         │    updated_at       │
└─────────────────────┘         └─────────────────────┘


┌──────────┐ 1
│CUSTOMERS │───────────────┐
└──────────┘               │ N
                    ┌──────┴────────┐
                    │   ACCOUNTS    │
                    ├───────────────┤
                    │ PK id (UUID)  │
                    │ FK customer_id│
                    │ UK acc_number │
                    │    acc_type   │
                    │    currency   │
                    │    balance    │
                    │    available  │
                    │    active     │
                    │    created_at │
                    │    updated_at │
                    └───────────────┘


┌─────────────────────┐
│       PAYINS        │ ◄─────────┐
├─────────────────────┤           │
│ PK id (UUID)        │           │
│ UK transaction_id   │           │
│    amount           │           │
│    currency         │           │
│    customer_id      │           │
│    payment_method_id│           │
│    status           │           │
│    description      │           │
│    error_message    │           │
│    created_at       │           │
│    updated_at       │           │
└──────────┬──────────┘           │
           │ 1                    │
           │                      │
           │ N                    │ N
┌──────────┴──────────┐  ┌────────┴────────┐
│   PAYIN_AUDIT       │  │  PAYIN_EVENTS   │
├─────────────────────┤  ├─────────────────┤
│ PK id (UUID)        │  │ PK id (UUID)    │
│ FK payin_id         │  │ FK payin_id     │
│    previous_status  │  │    event_type   │
│    new_status       │  │    event_data   │
│    changed_by       │  │    created_at   │
│    change_reason    │  │                 │
│    metadata (JSONB) │  │                 │
│    created_at       │  │                 │
└─────────────────────┘  └─────────────────┘
```

## Normalización

El modelo de base de datos está normalizado en **Tercera Forma Normal (3NF)**:

### Primera Forma Normal (1NF)
- Todos los atributos contienen valores atómicos
- No hay grupos repetitivos
- Cada tabla tiene una clave primaria

### Segunda Forma Normal (2NF)
- Cumple con 1NF
- Todos los atributos no clave dependen completamente de la clave primaria
- No hay dependencias parciales

### Tercera Forma Normal (3NF)
- Cumple con 2NF
- No hay dependencias transitivas
- Cada atributo no clave depende únicamente de la clave primaria

## Descripción de Tablas

### 1. **customers**
Almacena información de clientes del sistema.

**Campos clave**:
- `document_type`, `document_number`: Identificación única del cliente
- `email`: Único, usado para login y notificaciones
- `active`: Flag para soft delete

**Índices**:
- `idx_customers_email`: Búsqueda rápida por email
- `idx_customers_document`: Búsqueda por documento
- `idx_customers_active`: Filtrado de clientes activos

### 2. **payment_providers**
Catálogo de proveedores de pago externos (Stripe, PayPal, etc.).

**Campos clave**:
- `code`: Identificador único del proveedor (e.g., 'STRIPE')
- `priority`: Orden de selección cuando múltiples providers están activos
- `api_key`, `webhook_secret`: Credenciales encriptadas

**Uso**: Configuración centralizada de integraciones con gateways

### 3. **payment_methods**
Métodos de pago tokenizados de los clientes.

**Campos clave**:
- `token`: Token generado por el payment gateway
- `is_default`: Solo un método puede ser default por cliente
- `last_four_digits`: Para identificación visual (solo últimos 4 dígitos)

**Relación**: N:1 con customers

### 4. **accounts**
Cuentas bancarias/financieras de clientes.

**Campos clave**:
- `balance`: Saldo total de la cuenta
- `available_balance`: Saldo disponible (balance - fondos retenidos)
- `currency`: Moneda de la cuenta (USD, COP, EUR, etc.)

**Constraint importante**: `available_balance <= balance`

### 5. **payins** (Tabla Principal)
Transacciones de ingreso de dinero al sistema.

**Campos clave**:
- `transaction_id`: ID único de transacción (TXN + timestamp + random)
- `status`: Estado actual (CREATED, VALIDATED, PROCESSED, FAILED)
- `amount`: Monto con precisión de 4 decimales
- `error_message`: Mensaje de error en caso de FAILED

**Constraints**:
- `amount > 0 AND amount <= 1000000`
- `currency` debe ser código ISO de 3 letras
- `status` debe ser uno de los valores permitidos

**Índices importantes**:
- `idx_payins_transaction_id`: Búsqueda por transaction ID (único)
- `idx_payins_customer_status`: Consultas por cliente y estado
- `idx_payins_created_at`: Ordenamiento por fecha

### 6. **payin_audit**
Auditoría de cambios de estado de PayIns.

**Propósito**: Trazabilidad y compliance

**Campos clave**:
- `previous_status`, `new_status`: Transición de estado
- `changed_by`: Usuario o sistema que realizó el cambio
- `metadata`: JSONB para información adicional flexible

### 7. **payin_events**
Eventos de dominio para Event Sourcing.

**Propósito**: Reconstrucción del estado y debugging

**Tipos de eventos**:
- `PAYIN_CREATED`: PayIn fue creado
- `PAYIN_VALIDATED`: Pasó validaciones
- `PAYIN_PROCESSED`: Procesado exitosamente
- `PAYIN_FAILED`: Falló el procesamiento
- `PAYMENT_GATEWAY_REQUEST`: Request a gateway externo
- `PAYMENT_GATEWAY_RESPONSE`: Response del gateway

## Estrategia de Índices

### Índices Primarios (Performance)
- `payins.transaction_id`: Búsqueda exacta (99% de queries)
- `payins.customer_id`: Listar PayIns de un cliente
- `payins.status`: Filtrar por estado

### Índices Compuestos
- `(customer_id, created_at DESC, status)`: Queries comunes de dashboard
- `(customer_id, is_default)` en payment_methods: Método de pago default

### Índices Parciales
```sql
CREATE INDEX idx_payins_date_range
    ON payins(created_at)
    WHERE status = 'PROCESSED';
```
Solo indexa transacciones procesadas para reportes

## Estrategia de Particionamiento (Futuro)

Para escalar a millones de transacciones, considerar:

### Particionamiento por Fecha
```sql
-- Particionamiento mensual
CREATE TABLE payins_2024_01 PARTITION OF payins
    FOR VALUES FROM ('2024-01-01') TO ('2024-02-01');

CREATE TABLE payins_2024_02 PARTITION OF payins
    FOR VALUES FROM ('2024-02-01') TO ('2024-03-01');
```

**Beneficios**:
- Queries más rápidas en rangos de fechas
- Archivado eficiente de datos antiguos
- Mantenimiento por partición

## Consideraciones de Seguridad

### 1. Encriptación en Reposo
- `payment_providers.api_key`: Encriptado con AWS KMS o Vault
- `payment_methods.token`: Encriptado a nivel de aplicación

### 2. Encriptación en Tránsito
- Conexiones SSL/TLS a base de datos
- Certificados rotados cada 90 días

### 3. Acceso Controlado
- Usuario de aplicación con permisos mínimos
- Solo SELECT, INSERT, UPDATE en tablas necesarias
- Sin DROP, TRUNCATE, ALTER en producción

### 4. Auditoría
- Todos los cambios en `payins` generan registro en `payin_audit`
- Logs de base de datos habilitados para operaciones DDL

## Performance Tuning

### Configuraciones Recomendadas (PostgreSQL)

```sql
-- Aumentar cache de consultas
shared_buffers = 256MB
effective_cache_size = 1GB

-- Optimizar writes
wal_buffers = 16MB
checkpoint_completion_target = 0.9

-- Conexiones
max_connections = 100
```

### Queries Optimizadas

```sql
-- Query 1: PayIns recientes de un cliente
SELECT * FROM payins
WHERE customer_id = ?
  AND created_at > NOW() - INTERVAL '30 days'
ORDER BY created_at DESC
LIMIT 20;
-- Usa: idx_payins_customer_status

-- Query 2: Dashboard de transacciones
SELECT
    DATE(created_at) as date,
    status,
    COUNT(*) as count,
    SUM(amount) as total_amount
FROM payins
WHERE created_at > NOW() - INTERVAL '7 days'
GROUP BY DATE(created_at), status;
-- Usa: idx_payins_created_at
```

## Migración y Versionamiento

### Herramienta: Flyway

**Convención de nombres**:
- `V1__create_initial_schema.sql`
- `V2__add_payin_audit_table.sql`
- `V3__add_indexes_for_performance.sql`

**Proceso**:
1. Desarrollador crea script de migración
2. CI/CD valida sintaxis
3. Staging: Migración automática
4. Producción: Migración con aprobación manual

## Backup y Disaster Recovery

### Estrategia de Backup
- **Full backup**: Diario a las 2 AM
- **Incremental backup**: Cada hora
- **Point-in-time recovery**: Habilitado (WAL archiving)

### RPO y RTO
- **RPO (Recovery Point Objective)**: < 1 hora
- **RTO (Recovery Time Objective)**: < 30 minutos

### Retención
- Backups diarios: 30 días
- Backups semanales: 90 días
- Backups mensuales: 1 año
