-- Order Service schema initialisation

CREATE TABLE IF NOT EXISTS orders (
    id                  UUID            PRIMARY KEY,
    user_id             VARCHAR(255)    NOT NULL,
    user_email          VARCHAR(255)    NOT NULL,
    status              VARCHAR(50)     NOT NULL,
    total_amount        DECIMAL(12, 2)  NOT NULL,
    cancellation_reason VARCHAR(255),
    tracking_number     VARCHAR(255),
    created_at          TIMESTAMPTZ,
    updated_at          TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_orders_user_id    ON orders (user_id);
CREATE INDEX IF NOT EXISTS idx_orders_status     ON orders (status);
CREATE INDEX IF NOT EXISTS idx_orders_created_at ON orders (created_at);

-- ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS order_items (
    id           UUID           PRIMARY KEY,
    order_id     UUID           NOT NULL,
    product_id   VARCHAR(255)   NOT NULL,
    product_name VARCHAR(255)   NOT NULL,
    quantity     INTEGER        NOT NULL,
    unit_price   DECIMAL(10, 2) NOT NULL,
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders (id)
);

CREATE INDEX IF NOT EXISTS idx_order_items_order_id ON order_items (order_id);

-- ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS outbox_events (
    id           UUID        PRIMARY KEY,
    event_type   VARCHAR(255) NOT NULL,
    topic        VARCHAR(255) NOT NULL,
    payload      JSONB        NOT NULL,
    published    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ,
    published_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_outbox_events_published   ON outbox_events (published);
CREATE INDEX IF NOT EXISTS idx_outbox_events_created_at  ON outbox_events (created_at);
