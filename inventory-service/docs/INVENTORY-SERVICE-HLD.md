# Inventory Service — High-Level Design Document

| Field       | Value                         |
|-------------|-------------------------------|
| **Type**    | HLD                           |
| **Scope**   | inventory-service             |
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

Spring Boot 3.3 + Spring Data JPA on **PostgreSQL**. Kafka consumer for order events. **Pessimistic locking** (`@Lock(PESSIMISTIC_WRITE)`) prevents race conditions on stock reservation. REST API for admin stock management.

---

## 2. API Endpoints

| Method | Path                             | Auth      | Description                      |
|--------|----------------------------------|-----------|----------------------------------|
| GET    | `/inventory/{productId}`         | None      | Get stock level for a product    |
| GET    | `/inventory`                     | ADMIN JWT | List all stock levels            |
| POST   | `/inventory`                     | ADMIN JWT | Set initial stock for a product  |
| PATCH  | `/inventory/{productId}/adjust`  | ADMIN JWT | Add or subtract stock units      |

---

## 3. Dependencies

| Dependency | Type     | Purpose                                            |
|------------|----------|----------------------------------------------------|
| PostgreSQL | Internal | Stock and movement tables                          |
| Kafka      | Internal | Consume order events, publish inventory events     |
| Keycloak   | Internal | JWT validation                                     |

---

## 4. Kafka Events

| Topic                        | Direction | Event Type                   | Notes                        |
|------------------------------|-----------|------------------------------|------------------------------|
| `oms.orders.placed`          | Consumes  | `OrderPlacedEvent`           | Triggers stock reservation   |
| `oms.inventory.reserved`     | Produces  | `InventoryReservedEvent`     | Stock confirmed              |
| `oms.inventory.insufficient` | Produces  | `InventoryInsufficientEvent` | Saga compensation            |
| `oms.orders.cancelled`       | Consumes  | `OrderCancelledEvent`        | Releases reservation         |
| `oms.orders.shipped`         | Consumes  | `OrderShippedEvent`          | Deducts stock permanently    |
| `oms.inventory.low-stock`    | Produces  | `LowStockEvent`              | Threshold alert              |

---

## 5. Data Model

```sql
inventory (
    product_id    VARCHAR(255) PRIMARY KEY,
    available_qty INTEGER NOT NULL DEFAULT 0,
    reserved_qty  INTEGER NOT NULL DEFAULT 0,
    updated_at    TIMESTAMPTZ
)

stock_movements (
    id            UUID PRIMARY KEY,
    product_id    VARCHAR(255) NOT NULL,
    movement_type VARCHAR(50) NOT NULL,   -- RESERVE | RELEASE | DEDUCT | RESTOCK
    delta         INTEGER NOT NULL,
    order_id      UUID,
    created_at    TIMESTAMPTZ
)
```

---

## 6. Security

- `GET /inventory/{productId}` is public — no JWT required
- All write operations require `ADMIN` JWT
- Pessimistic DB lock prevents concurrent reservation race conditions
