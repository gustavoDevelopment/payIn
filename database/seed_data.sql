-- ============================================
-- PayIn Service - Seed Data for Testing
-- ============================================

-- ============================================
-- 1. CUSTOMERS
-- ============================================
INSERT INTO customers (id, document_type, document_number, first_name, last_name, email, phone, active)
VALUES
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'DNI', '12345678', 'Juan', 'Pérez', 'juan.perez@example.com', '+57300123456', TRUE),
    ('b1ffcd88-8b1a-4df7-aa5c-5aa8ac270a22', 'DNI', '87654321', 'María', 'García', 'maria.garcia@example.com', '+57310654321', TRUE),
    ('c2ggde77-7a09-4ce6-9949-4996bd150a33', 'CE', '98765432', 'Carlos', 'Rodríguez', 'carlos.rodriguez@example.com', '+57320789012', TRUE),
    ('d3hhef66-6908-4bd5-8838-3885ac040a44', 'PASSPORT', 'AB123456', 'Ana', 'Martínez', 'ana.martinez@example.com', '+57330345678', FALSE);

-- ============================================
-- 2. PAYMENT_PROVIDERS
-- ============================================
INSERT INTO payment_providers (id, name, code, base_url, api_key, active, priority)
VALUES
    ('e4iifg55-5807-4ac4-7727-2774ab030a55', 'Stripe', 'STRIPE', 'https://api.stripe.com', 'sk_test_xxxxx', TRUE, 1),
    ('f5jjgh44-4706-4923-6616-1663ab020a66', 'PayPal', 'PAYPAL', 'https://api.paypal.com', 'paypal_secret_xxx', TRUE, 2),
    ('g6kkhi33-3605-4812-5505-0552ab010a77', 'MercadoPago', 'MERCADOPAGO', 'https://api.mercadopago.com', 'mp_access_token_xxx', FALSE, 3);

-- ============================================
-- 3. PAYMENT_METHODS
-- ============================================
INSERT INTO payment_methods (id, customer_id, type, token, is_default, last_four_digits, provider, active)
VALUES
    ('h7llij22-2504-4701-4404-9441ab009a88',
     'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
     'CREDIT_CARD', 'tok_visa_4242424242424242', TRUE, '4242', 'VISA', TRUE),

    ('i8mmjk11-1403-4590-3303-8330ab998a99',
     'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
     'DEBIT_CARD', 'tok_mastercard_5555555555554444', FALSE, '4444', 'MASTERCARD', TRUE),

    ('j9nnkl00-0302-4489-2202-7229ab887aaa',
     'b1ffcd88-8b1a-4df7-aa5c-5aa8ac270a22',
     'E_WALLET', 'tok_paypal_maria@example.com', TRUE, NULL, 'PAYPAL', TRUE),

    ('k0omlm99-9201-4378-1101-6118ab776abb',
     'c2ggde77-7a09-4ce6-9949-4996bd150a33',
     'BANK_ACCOUNT', 'tok_bank_acc_carlos', TRUE, '7890', 'BANCOLOMBIA', TRUE);

-- ============================================
-- 4. ACCOUNTS
-- ============================================
INSERT INTO accounts (id, customer_id, account_number, account_type, currency, balance, available_balance, active)
VALUES
    ('l1pnmn88-8100-4267-0000-5007ab665acc',
     'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
     'ACC-001-12345678', 'CHECKING', 'USD', 5000.0000, 4500.0000, TRUE),

    ('m2qono77-7099-4156-9989-4996ab554add',
     'b1ffcd88-8b1a-4df7-aa5c-5aa8ac270a22',
     'ACC-002-87654321', 'SAVINGS', 'USD', 10000.0000, 10000.0000, TRUE),

    ('n3rpop66-6988-4045-8878-3885ab443aee',
     'c2ggde77-7a09-4ce6-9949-4996bd150a33',
     'ACC-003-98765432', 'CHECKING', 'COP', 15000000.0000, 12000000.0000, TRUE);

-- ============================================
-- 5. PAYINS - Sample Transactions
-- ============================================

-- Successful transactions
INSERT INTO payins (id, transaction_id, amount, currency, customer_id, payment_method_id, status, description, created_at, updated_at)
VALUES
    ('o4sqpq55-5877-3934-7767-2774ab332aff',
     'TXN1704067200001', 150.5000, 'USD', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
     'h7llij22-2504-4701-4404-9441ab009a88', 'PROCESSED',
     'Subscription payment', '2024-01-01 10:00:00', '2024-01-01 10:00:15'),

    ('p5trqr44-4766-2823-6656-1663ab221b00',
     'TXN1704153600002', 500.0000, 'USD', 'b1ffcd88-8b1a-4df7-aa5c-5aa8ac270a22',
     'j9nnkl00-0302-4489-2202-7229ab887aaa', 'PROCESSED',
     'Product purchase', '2024-01-02 11:30:00', '2024-01-02 11:30:20'),

    ('q6usrs33-3655-1712-5545-0552ab110c11',
     'TXN1704240000003', 75.2500, 'USD', 'c2ggde77-7a09-4ce6-9949-4996bd150a33',
     'k0omlm99-9201-4378-1101-6118ab776abb', 'PROCESSED',
     'Service payment', '2024-01-03 09:15:00', '2024-01-03 09:15:10');

-- Pending/Validated transactions
INSERT INTO payins (id, transaction_id, amount, currency, customer_id, payment_method_id, status, description, created_at, updated_at)
VALUES
    ('r7vtst22-2544-0601-4434-9441ab009d22',
     'TXN1704326400004', 200.0000, 'USD', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
     'h7llij22-2504-4701-4404-9441ab009a88', 'VALIDATED',
     'Pending authorization', '2024-01-04 14:00:00', '2024-01-04 14:00:05');

-- Failed transaction
INSERT INTO payins (id, transaction_id, amount, currency, customer_id, payment_method_id, status, description, error_message, created_at, updated_at)
VALUES
    ('s8wutu11-1433-9490-3323-8330ab998e33',
     'TXN1704412800005', 1000.0000, 'USD', 'b1ffcd88-8b1a-4df7-aa5c-5aa8ac270a22',
     'j9nnkl00-0302-4489-2202-7229ab887aaa', 'FAILED',
     'Large purchase attempt', 'Insufficient funds in payment method',
     '2024-01-05 16:20:00', '2024-01-05 16:20:08');

-- ============================================
-- 6. PAYIN_AUDIT - Sample Audit Records
-- ============================================
INSERT INTO payin_audit (id, payin_id, previous_status, new_status, changed_by, change_reason)
VALUES
    ('t9xvuv00-0322-8389-2212-7229ab887f44',
     'o4sqpq55-5877-3934-7767-2774ab332aff',
     'VALIDATED', 'PROCESSED', 'system', 'Payment gateway confirmed'),

    ('u0ywvw99-9211-7278-1101-6118ab776g55',
     's8wutu11-1433-9490-3323-8330ab998e33',
     'VALIDATED', 'FAILED', 'system', 'Payment gateway rejected - insufficient funds');

-- ============================================
-- Verification Queries
-- ============================================

-- Count records
-- SELECT 'customers' AS table_name, COUNT(*) AS count FROM customers
-- UNION ALL
-- SELECT 'payment_providers', COUNT(*) FROM payment_providers
-- UNION ALL
-- SELECT 'payment_methods', COUNT(*) FROM payment_methods
-- UNION ALL
-- SELECT 'accounts', COUNT(*) FROM accounts
-- UNION ALL
-- SELECT 'payins', COUNT(*) FROM payins
-- UNION ALL
-- SELECT 'payin_audit', COUNT(*) FROM payin_audit;
