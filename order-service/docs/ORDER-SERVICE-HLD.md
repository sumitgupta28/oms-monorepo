# Order Service — High-Level Design Document

| Field       | Value                         |
|-------------|-------------------------------|
| **Type**    | HLD                           |
| **Scope**   | order-service                 |
| **Version** | 1.0                           |
| **Date**    | 2025-04-06                    |
| **Status**  | Draft                         |
| **Author**  | OMS Engineering Team          |

---

## Table of Contents

1. [Service Overview](#1-service-overview)
2. [API Endpoints](#2-api-endpoints)
3. [Dependencies](#3-dependencies)
4. [Kafka Events](#4-kafka-events)
5. [Data Model](#5-data-model)
6. [Security](#6-security)

---

## 1. Service Overview

Spring Boot 3.3 + Spring Data JPA on **PostgreSQL**. Spring State Machine for state transitions. Spring Kafka for event publishing and consumption. **Outbox pattern** for guaranteed event delivery.

---

## 2. API Endpoints

| Method | Path                    | Auth         | Description              |
|--------|-------------------------|--------------|--------------------------|
| POST   | `/orders`               | CUSTOMER JWT | Create new order         |
| GET    | `/orders/my`            | CUSTOMER JWT | Get caller's orders      |
| GET    | `/orders/{id}`          | CUSTOMER JWT | Get order by ID          |
| GET    | `/orders`               | ADMIN JWT    | Get all orders           |
| PATCH  | `/orders/{id}/cancel`   | CUSTOMER JWT | Cancel order             |
| PATCH  | `/orders/{id}/ship`     | ADMIN JWT    | Mark order as shipped    |

---

## 3. Dependencies

| Dependency | Type     | Purpose                               |
|------------|----------|---------------------------------------|
| PostgreSQL | Internal | Order and outbox tables               |
| Kafka      | Internal | Event publishing and consumption      |
| Keycloak   | Internal | JWT validation via issuer URI         |

---

## 4. Kafka Events

| Topic                    | Direction | Event Type               | Notes                            |
|--------------------------|-----------|--------------------------|----------------------------------|
| `oms.orders.placed`      | Produces  | `OrderPlacedEvent`       | Published via outbox             |
| `oms.payment.confirmed`  | Consumes  | `PaymentConfirmedEvent`  | Advances order to `PAID`         |
| `oms.inventory.insufficient` | Consumes | `InventoryInsufficientEvent` | Cancels order             |
| `oms.orders.shipped`     | Produces  | `OrderShippedEvent`      | Admin action                     |
| `oms.orders.cancelled`   | Produces  | `OrderCancelledEvent`    | Customer or system cancellation  |

---

## 5. Data Model

```sql
orders (
    id            UUID PRIMARY KEY,
    user_id       VARCHAR NOT NULL,
    user_email    VARCHAR NOT NULL,
    status        VARCHAR NOT NULL,       -- OrderStatus enum
    total_amount  DECIMAL(12,2) NOT NULL,
    cancellation_reason VARCHAR,
    tracking_number     VARCHAR,
    created_at    TIMESTAMPTZ,
    updated_at    TIMESTAMPTZ
)

order_items (
    id           UUID PRIMARY KEY,
    order_id     UUID NOT NULL REFERENCES orders(id),
    product_id   VARCHAR NOT NULL,
    product_name VARCHAR NOT NULL,
    quantity     INTEGER NOT NULL,
    unit_price   DECIMAL(10,2) NOT NULL
)

outbox_events (
    id          UUID PRIMARY KEY,
    event_type  VARCHAR NOT NULL,
    topic       VARCHAR NOT NULL,
    payload     JSONB NOT NULL,
    published   BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ,
    published_at TIMESTAMPTZ
)
```

---

## 6. Security

- JWT required on all endpoints
- `@PreAuthorize("hasRole('CUSTOMER')")` on `/orders/my`
- `@PreAuthorize("hasRole('ADMIN')")` on `GET /orders` (all orders)
- Customers can only cancel their own orders (ownership check in service layer)
