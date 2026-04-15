# Inventory Service — Low-Level Design Document

| Field       | Value                         |
|-------------|-------------------------------|
| **Type**    | LLD                           |
| **Scope**   | inventory-service             |
| **Version** | 1.0                           |
| **Date**    | 2025-04-06                    |
| **Status**  | Draft                         |
| **Author**  | OMS Engineering Team          |

---

## Table of Contents

1. [Package Structure](#1-package-structure)
2. [Key Classes](#2-key-classes)
3. [Database Schema](#3-database-schema)
4. [Configuration Properties](#4-configuration-properties)
5. [Error Codes](#5-error-codes)

---

## 1. Package Structure

```
com.oms.inventory/
  domain/      — Inventory, StockMovement, MovementType (enum)
  repository/  — InventoryRepository (@Lock queries)
  service/     — InventoryService, ReservationService
  kafka/       — OrderEventConsumer, InventoryEventProducer
  controller/  — InventoryController
  config/      — SecurityConfig, KafkaConfig
```

---

## 2. Key Classes

| Class                    | Type       | Responsibility                                                          |
|--------------------------|------------|-------------------------------------------------------------------------|
| `ReservationService`     | Service    | Atomic reserve/release with `@Transactional` + `@Lock(PESSIMISTIC_WRITE)` |
| `OrderEventConsumer`     | Kafka      | Consumes `OrderPlacedEvent`, `OrderCancelledEvent`, `OrderShippedEvent` |
| `InventoryEventProducer` | Kafka      | Publishes `InventoryReservedEvent` and `InventoryInsufficientEvent`     |
| `InventoryRepository`    | Repository | `findByProductIdWithLock()` uses `@Lock(PESSIMISTIC_WRITE)`             |

---

## 3. Database Schema

```sql
-- Managed by Flyway migration V1__init_schema.sql
CREATE TABLE inventory (
    product_id    VARCHAR(255) PRIMARY KEY,
    available_qty INTEGER NOT NULL DEFAULT 0,
    reserved_qty  INTEGER NOT NULL DEFAULT 0,
    updated_at    TIMESTAMPTZ
);

CREATE TABLE stock_movements (
    id            UUID PRIMARY KEY,
    product_id    VARCHAR(255) NOT NULL,
    movement_type VARCHAR(50) NOT NULL,
    delta         INTEGER NOT NULL,
    order_id      UUID,
    created_at    TIMESTAMPTZ
);
```

---

## 4. Configuration Properties

| Property                         | Default                                      | Description              |
|----------------------------------|----------------------------------------------|--------------------------|
| `spring.datasource.url`          | `jdbc:postgresql://localhost:5432/inventory` | DB connection            |
| `spring.kafka.bootstrap-servers` | `localhost:9092`                             | Kafka broker address     |
| `inventory.low-stock-threshold`  | `5`                                          | Publish `LowStockEvent` below this qty |

---

## 5. Error Codes

| Code    | HTTP Status | Description                                   |
|---------|-------------|-----------------------------------------------|
| INV-001 | 404         | Product not found in inventory                |
| INV-002 | 422         | Insufficient stock for requested quantity     |
| INV-003 | 409         | Duplicate reservation for same order          |
