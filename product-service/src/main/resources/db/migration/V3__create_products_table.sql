-- Product catalog table — managed by Flyway
CREATE TABLE IF NOT EXISTS products (
    id          VARCHAR(36)   PRIMARY KEY,
    name        VARCHAR(255)  NOT NULL,
    description TEXT,
    category    VARCHAR(100)  NOT NULL,
    price       NUMERIC(19,2) NOT NULL,
    stock_qty   INT           NOT NULL DEFAULT 0,
    image_url   VARCHAR(500),
    active      BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_products_name     ON products (name);
CREATE INDEX IF NOT EXISTS idx_products_category ON products (category);
