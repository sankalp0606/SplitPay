-- ====================================================================
-- SPLITPAY DATABASE INITIALIZATION SCHEMA (V1)
-- ====================================================================

-- Users & Merchants
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'USER',
    business_name VARCHAR(255),
    default_upi_id VARCHAR(255),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Payment Orders
CREATE TABLE IF NOT EXISTS payment_orders (
    id UUID PRIMARY KEY,
    order_reference VARCHAR(64) NOT NULL UNIQUE,
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    recipient_name VARCHAR(255) NOT NULL,
    upi_id VARCHAR(255) NOT NULL,
    total_amount NUMERIC(15, 2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'INR',
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    splitting_strategy VARCHAR(50) NOT NULL DEFAULT 'MAX_PART_AMOUNT',
    max_part_amount NUMERIC(15, 2),
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_order_amount_pos CHECK (total_amount > 0)
);

-- Payment Parts (individual split segments of an order)
CREATE TABLE IF NOT EXISTS payment_parts (
    id UUID PRIMARY KEY,
    payment_order_id UUID NOT NULL REFERENCES payment_orders(id) ON DELETE CASCADE,
    part_number INTEGER NOT NULL,
    payment_reference VARCHAR(64) NOT NULL UNIQUE,
    amount NUMERIC(15, 2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'INR',
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    upi_uri TEXT NOT NULL,
    qr_payload TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_part_amount_pos CHECK (amount > 0)
);

-- Payment Transactions (authoritative records from payment provider / webhook)
CREATE TABLE IF NOT EXISTS payment_transactions (
    id UUID PRIMARY KEY,
    payment_part_id UUID REFERENCES payment_parts(id) ON DELETE SET NULL,
    payment_reference VARCHAR(64) NOT NULL,
    provider_transaction_id VARCHAR(128),
    provider_name VARCHAR(64) NOT NULL,
    amount NUMERIC(15, 2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'INR',
    status VARCHAR(50) NOT NULL,
    raw_payload TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Inbound Webhook / Provider Events for Deduplication and Idempotency
CREATE TABLE IF NOT EXISTS payment_events (
    id UUID PRIMARY KEY,
    event_id VARCHAR(128) NOT NULL UNIQUE,
    event_type VARCHAR(64) NOT NULL,
    provider_name VARCHAR(64) NOT NULL,
    payload TEXT NOT NULL,
    processed BOOLEAN NOT NULL DEFAULT FALSE,
    processed_at TIMESTAMP WITH TIME ZONE,
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Security & Audit Logs
CREATE TABLE IF NOT EXISTS audit_logs (
    id UUID PRIMARY KEY,
    user_id UUID,
    action VARCHAR(128) NOT NULL,
    resource_type VARCHAR(64) NOT NULL,
    resource_id VARCHAR(64),
    ip_address VARCHAR(64),
    details TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Performance & Query Optimization Indexes
CREATE INDEX IF NOT EXISTS idx_payment_orders_ref ON payment_orders(order_reference);
CREATE INDEX IF NOT EXISTS idx_payment_orders_user ON payment_orders(user_id);
CREATE INDEX IF NOT EXISTS idx_payment_orders_status ON payment_orders(status);
CREATE INDEX IF NOT EXISTS idx_payment_parts_order ON payment_parts(payment_order_id);
CREATE INDEX IF NOT EXISTS idx_payment_parts_ref ON payment_parts(payment_reference);
CREATE INDEX IF NOT EXISTS idx_payment_parts_status ON payment_parts(status);
CREATE INDEX IF NOT EXISTS idx_payment_tx_ref ON payment_transactions(payment_reference);
CREATE INDEX IF NOT EXISTS idx_payment_tx_provider_id ON payment_transactions(provider_transaction_id);
CREATE INDEX IF NOT EXISTS idx_payment_events_id ON payment_events(event_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_user ON audit_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_created ON audit_logs(created_at);
