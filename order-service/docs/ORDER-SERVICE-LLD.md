# Order Service — Low-Level Design Document

| Field       | Value                         |
|-------------|-------------------------------|
| **Type**    | LLD                           |
| **Scope**   | order-service                 |
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
com.oms.order/
  domain/       — Order, OrderItem, OrderStatus (enum), OutboxEvent
  statemachine/ — OrderStateMachineConfig
  repository/   — OrderRepository, OutboxEventRepository
  service/      — OrderService, OutboxPublisher
  kafka/        — OrderEventProducer, PaymentEventConsumer
  controller/   — OrderController
  config/       — SecurityConfig, KafkaConfig
  exception/    — OrderNotFoundException, InvalidStateTransitionException
```

---

## 2. Key Classes

| Class                    | Type        | Responsibility                                                        |
|--------------------------|-------------|-----------------------------------------------------------------------|
| `OrderStateMachineConfig`| Config      | Defines all states, transitions, and guards for the order FSM         |
| `OrderService`           | Service     | Business logic — delegates transitions to state machine               |
| `OutboxPublisher`        | Service     | `@Scheduled` job — polls and publishes unpublished outbox events to Kafka |
| `OrderEventProducer`     | Kafka       | Wraps `KafkaTemplate` for domain event publishing                     |
| `PaymentEventConsumer`   | Kafka       | Consumes `PaymentConfirmedEvent` and `InventoryInsufficientEvent`      |

---

## 3. Database Schema

```sql
-- Managed by Flyway migration V1__init_schema.sql
CREATE TABLE orders (
    id                  UUID PRIMARY KEY,
    user_id             VARCHAR(255) NOT NULL,
    user_email          VARCHAR(255) NOT NULL,
    status              VARCHAR(50)  NOT NULL,
    total_amount        DECIMAL(12,2) NOT NULL,
    cancellation_reason VARCHAR(255),
    tracking_number     VARCHAR(255),
    created_at          TIMESTAMPTZ,
    updated_at          TIMESTAMPTZ
);

CREATE TABLE order_items (
    id           UUID PRIMARY KEY,
    order_id     UUID NOT NULL REFERENCES orders(id),
    product_id   VARCHAR(255) NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    quantity     INTEGER NOT NULL,
    unit_price   DECIMAL(10,2) NOT NULL
);

CREATE TABLE outbox_events (
    id           UUID PRIMARY KEY,
    event_type   VARCHAR(255) NOT NULL,
    topic        VARCHAR(255) NOT NULL,
    payload      JSONB NOT NULL,
    published    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ,
    published_at TIMESTAMPTZ
);
```

---

## 4. Configuration Properties

| Property                           | Default                                   | Description              |
|------------------------------------|-------------------------------------------|--------------------------|
| `spring.datasource.url`            | `jdbc:postgresql://localhost:5432/orders` | DB URL                   |
| `spring.kafka.bootstrap-servers`   | `localhost:9092`                          | Kafka broker             |
| `spring.security.oauth2.resourceserver.jwt.issuer-uri` | `http://localhost:8180/realms/oms` | JWT issuer   |

---

## 5. Error Codes

| Code   | HTTP Status | Description                     |
|--------|-------------|---------------------------------|
| OS-001 | 404         | Order not found                 |
| OS-002 | 409         | Invalid state transition        |
| OS-003 | 403         | Not the order owner             |
