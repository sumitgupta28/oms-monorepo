-- Inventory Service schema initialisation

CREATE TABLE IF NOT EXISTS inventory (
    product_id    VARCHAR(255) PRIMARY KEY,
    available_qty INTEGER      NOT NULL DEFAULT 0,
    reserved_qty  INTEGER      NOT NULL DEFAULT 0,
    updated_at    TIMESTAMPTZ
);

-- ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS stock_movements (
    id            UUID         PRIMARY KEY,
    product_id    VARCHAR(255) NOT NULL,
    movement_type VARCHAR(50)  NOT NULL,
    delta         INTEGER      NOT NULL,
    order_id      UUID,
    created_at    TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_stock_movements_product_id ON stock_movements (product_id);
CREATE INDEX IF NOT EXISTS idx_stock_movements_order_id   ON stock_movements (order_id);
CREATE INDEX IF NOT EXISTS idx_stock_movements_created_at ON stock_movements (created_at);
