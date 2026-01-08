-- Flyway Migration V1: Initial Schema
-- This migration creates the initial database schema for PayIn service

-- Enable UUID extension (PostgreSQL)
-- CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create payins table
CREATE TABLE IF NOT EXISTS payins (
    id UUID PRIMARY KEY,
    transaction_id VARCHAR(100) NOT NULL UNIQUE,
    amount DECIMAL(19, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    customer_id VARCHAR(255) NOT NULL,
    payment_method_id VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL,
    description VARCHAR(500),
    error_message VARCHAR(1000),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT chk_payin_amount CHECK (amount > 0 AND amount <= 1000000),
    CONSTRAINT chk_payin_currency CHECK (LENGTH(currency) = 3),
    CONSTRAINT chk_payin_status CHECK (status IN ('CREATED', 'VALIDATED', 'PROCESSED', 'FAILED'))
);

-- Create indexes
CREATE INDEX idx_payins_transaction_id ON payins(transaction_id);
CREATE INDEX idx_payins_customer ON payins(customer_id);
CREATE INDEX idx_payins_status ON payins(status);
CREATE INDEX idx_payins_created_at ON payins(created_at DESC);
CREATE INDEX idx_payins_customer_status ON payins(customer_id, status);
