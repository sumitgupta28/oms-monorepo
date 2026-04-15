-- Payment Service schema initialisation

CREATE TABLE IF NOT EXISTS payments (
    id               UUID            PRIMARY KEY,
    order_id         UUID            NOT NULL,
    user_id          VARCHAR(255)    NOT NULL,
    amount           DECIMAL(12, 2)  NOT NULL,
    status           VARCHAR(50)     NOT NULL,
    idempotency_key  VARCHAR(255)    NOT NULL UNIQUE,
    failure_reason   VARCHAR(255),
    created_at       TIMESTAMPTZ,
    processed_at     TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_payments_order_id  ON payments (order_id);
CREATE INDEX IF NOT EXISTS idx_payments_user_id   ON payments (user_id);
CREATE INDEX IF NOT EXISTS idx_payments_status    ON payments (status);

-- ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS payment_ledger (
    id          UUID           PRIMARY KEY,
    payment_id  UUID           NOT NULL,
    order_id    UUID           NOT NULL,
    entry_type  VARCHAR(50)    NOT NULL,
    amount      DECIMAL(12, 2) NOT NULL,
    created_at  TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_payment_ledger_payment_id ON payment_ledger (payment_id);
CREATE INDEX IF NOT EXISTS idx_payment_ledger_order_id   ON payment_ledger (order_id);
