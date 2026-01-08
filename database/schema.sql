-- ============================================
-- PayIn Service - Database Schema
-- Modelo Normalizado - PostgreSQL / H2
-- ============================================

-- ============================================
-- 1. CUSTOMERS TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS customers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_type VARCHAR(20) NOT NULL,
    document_number VARCHAR(50) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    active BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_customer_document UNIQUE (document_type, document_number),
    CONSTRAINT uk_customer_email UNIQUE (email),
    CONSTRAINT chk_email_format CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}$')
);

-- Index for customer lookups
CREATE INDEX idx_customers_email ON customers(email);
CREATE INDEX idx_customers_document ON customers(document_type, document_number);
CREATE INDEX idx_customers_active ON customers(active);

-- ============================================
-- 2. PAYMENT_PROVIDERS TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS payment_providers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    base_url VARCHAR(255),
    api_key VARCHAR(500),
    webhook_secret VARCHAR(500),
    active BOOLEAN DEFAULT TRUE NOT NULL,
    priority INTEGER DEFAULT 0 NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_provider_code UNIQUE (code)
);

-- Index for provider selection
CREATE INDEX idx_payment_providers_active ON payment_providers(active, priority);

-- ============================================
-- 3. PAYMENT_METHODS TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS payment_methods (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL,
    type VARCHAR(50) NOT NULL,
    token VARCHAR(500) NOT NULL,
    is_default BOOLEAN DEFAULT FALSE NOT NULL,
    last_four_digits VARCHAR(4),
    provider VARCHAR(50),
    active BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_payment_method_customer FOREIGN KEY (customer_id)
        REFERENCES customers(id) ON DELETE CASCADE,
    CONSTRAINT chk_payment_type CHECK (type IN ('CREDIT_CARD', 'DEBIT_CARD', 'BANK_ACCOUNT', 'E_WALLET', 'CRYPTO'))
);

-- Indexes for payment method lookups
CREATE INDEX idx_payment_methods_customer ON payment_methods(customer_id);
CREATE INDEX idx_payment_methods_active ON payment_methods(active);
CREATE INDEX idx_payment_methods_default ON payment_methods(customer_id, is_default)
    WHERE is_default = TRUE;

-- ============================================
-- 4. ACCOUNTS TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL,
    account_number VARCHAR(50) NOT NULL UNIQUE,
    account_type VARCHAR(20) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    balance NUMERIC(19, 4) DEFAULT 0.0000 NOT NULL,
    available_balance NUMERIC(19, 4) DEFAULT 0.0000 NOT NULL,
    active BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_account_customer FOREIGN KEY (customer_id)
        REFERENCES customers(id) ON DELETE CASCADE,
    CONSTRAINT chk_account_type CHECK (account_type IN ('CHECKING', 'SAVINGS', 'INVESTMENT')),
    CONSTRAINT chk_currency_format CHECK (currency ~* '^[A-Z]{3}$'),
    CONSTRAINT chk_balance_positive CHECK (balance >= 0),
    CONSTRAINT chk_available_balance CHECK (available_balance >= 0 AND available_balance <= balance)
);

-- Indexes for account operations
CREATE INDEX idx_accounts_customer ON accounts(customer_id);
CREATE INDEX idx_accounts_number ON accounts(account_number);
CREATE INDEX idx_accounts_active ON accounts(active);

-- ============================================
-- 5. PAYINS TABLE (Main Transaction Table)
-- ============================================
CREATE TABLE IF NOT EXISTS payins (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id VARCHAR(100) NOT NULL UNIQUE,
    amount NUMERIC(19, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    customer_id VARCHAR(255) NOT NULL,
    payment_method_id VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL,
    description VARCHAR(500),
    error_message VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Note: customer_id and payment_method_id are stored as VARCHAR for flexibility
    -- In a real production system, these would be UUIDs with foreign keys

    CONSTRAINT chk_payin_amount CHECK (amount > 0 AND amount <= 1000000),
    CONSTRAINT chk_payin_currency CHECK (currency ~* '^[A-Z]{3}$'),
    CONSTRAINT chk_payin_status CHECK (status IN ('CREATED', 'VALIDATED', 'PROCESSED', 'FAILED'))
);

-- Indexes for PayIn queries
CREATE INDEX idx_payins_transaction_id ON payins(transaction_id);
CREATE INDEX idx_payins_customer ON payins(customer_id);
CREATE INDEX idx_payins_status ON payins(status);
CREATE INDEX idx_payins_created_at ON payins(created_at DESC);
CREATE INDEX idx_payins_customer_status ON payins(customer_id, status);

-- ============================================
-- 6. PAYIN_AUDIT TABLE (Audit Trail)
-- ============================================
CREATE TABLE IF NOT EXISTS payin_audit (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payin_id UUID NOT NULL,
    previous_status VARCHAR(20),
    new_status VARCHAR(20) NOT NULL,
    changed_by VARCHAR(100),
    change_reason VARCHAR(500),
    metadata JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_payin_audit_payin FOREIGN KEY (payin_id)
        REFERENCES payins(id) ON DELETE CASCADE
);

-- Index for audit queries
CREATE INDEX idx_payin_audit_payin ON payin_audit(payin_id);
CREATE INDEX idx_payin_audit_created_at ON payin_audit(created_at DESC);

-- ============================================
-- 7. PAYIN_EVENTS TABLE (Event Sourcing)
-- ============================================
CREATE TABLE IF NOT EXISTS payin_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payin_id UUID NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    event_data JSONB NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_payin_event_payin FOREIGN KEY (payin_id)
        REFERENCES payins(id) ON DELETE CASCADE,
    CONSTRAINT chk_event_type CHECK (event_type IN (
        'PAYIN_CREATED',
        'PAYIN_VALIDATED',
        'PAYIN_PROCESSED',
        'PAYIN_FAILED',
        'PAYMENT_GATEWAY_REQUEST',
        'PAYMENT_GATEWAY_RESPONSE'
    ))
);

-- Index for event queries
CREATE INDEX idx_payin_events_payin ON payin_events(payin_id);
CREATE INDEX idx_payin_events_type ON payin_events(event_type);
CREATE INDEX idx_payin_events_created_at ON payin_events(created_at DESC);

-- ============================================
-- 8. COMMENTS AND DOCUMENTATION
-- ============================================

COMMENT ON TABLE customers IS 'Almacena información de clientes del sistema';
COMMENT ON TABLE payment_providers IS 'Proveedores de pago disponibles (Stripe, PayPal, etc.)';
COMMENT ON TABLE payment_methods IS 'Métodos de pago tokenizados de clientes';
COMMENT ON TABLE accounts IS 'Cuentas bancarias de clientes';
COMMENT ON TABLE payins IS 'Transacciones de ingreso de dinero';
COMMENT ON TABLE payin_audit IS 'Auditoría de cambios de estado de PayIns';
COMMENT ON TABLE payin_events IS 'Eventos de dominio para Event Sourcing';

COMMENT ON COLUMN payins.transaction_id IS 'ID único de transacción generado por el sistema';
COMMENT ON COLUMN payins.status IS 'Estado actual: CREATED, VALIDATED, PROCESSED, FAILED';
COMMENT ON COLUMN payins.error_message IS 'Mensaje de error en caso de fallo';

-- ============================================
-- 9. INDEXES FOR PERFORMANCE
-- ============================================

-- Composite index for common query patterns
CREATE INDEX idx_payins_customer_date_status
    ON payins(customer_id, created_at DESC, status);

-- Index for reporting queries
CREATE INDEX idx_payins_date_range
    ON payins(created_at)
    WHERE status = 'PROCESSED';

-- ============================================
-- 10. DATABASE STATISTICS
-- ============================================

-- Enable statistics collection (PostgreSQL specific)
-- ANALYZE customers;
-- ANALYZE payment_methods;
-- ANALYZE accounts;
-- ANALYZE payins;
