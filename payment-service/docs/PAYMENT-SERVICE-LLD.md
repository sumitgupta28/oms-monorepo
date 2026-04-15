# Payment Service — Low-Level Design Document

| Field       | Value                              |
|-------------|------------------------------------|
| **Type**    | LLD                                |
| **Scope**   | payment-service (Mock)             |
| **Version** | 1.0                                |
| **Date**    | 2025-04-06                         |
| **Status**  | Draft                              |
| **Author**  | OMS Engineering Team               |

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
com.oms.payment/
  domain/      — Payment, PaymentLedger, PaymentStatus (enum)
  service/     — PaymentService, MockGateway
  kafka/       — PaymentEventProducer, OrderCancelledConsumer
  controller/  — PaymentController
  config/      — SecurityConfig, KafkaConfig
  exception/   — DuplicateIdempotencyKeyException, PaymentNotFoundException
```

---

## 2. Key Classes

| Class                  | Type    | Responsibility                                                        |
|------------------------|---------|-----------------------------------------------------------------------|
| `MockGateway`          | Service | Simulates payment with configurable delay and failure rate            |
| `PaymentService`       | Service | Idempotency check, ledger write, Kafka publish                        |
| `PaymentEventProducer` | Kafka   | Publishes `PaymentConfirmedEvent` and `PaymentFailedEvent`            |
| `OrderCancelledConsumer`| Kafka  | Consumes `OrderCancelledEvent` and triggers refund flow               |

---

## 3. Database Schema

```sql
-- Managed by Flyway migration V1__init_schema.sql
CREATE TABLE payments (
    id               UUID PRIMARY KEY,
    order_id         UUID NOT NULL,
    user_id          VARCHAR(255) NOT NULL,
    amount           DECIMAL(12,2) NOT NULL,
    status           VARCHAR(50) NOT NULL,
    idempotency_key  VARCHAR(255) NOT NULL UNIQUE,
    failure_reason   VARCHAR(255),
    created_at       TIMESTAMPTZ,
    processed_at     TIMESTAMPTZ
);

CREATE TABLE payment_ledger (
    id          UUID PRIMARY KEY,
    payment_id  UUID NOT NULL,
    order_id    UUID NOT NULL,
    entry_type  VARCHAR(50) NOT NULL,
    amount      DECIMAL(12,2) NOT NULL,
    created_at  TIMESTAMPTZ
);
```

---

## 4. Configuration Properties

| Property                         | Default | Description                                      |
|----------------------------------|---------|--------------------------------------------------|
| `mock.payment.failure-rate`      | `0.1`   | Fraction of payments that fail (0.0–1.0)         |
| `mock.payment.delay-ms`          | `1000`  | Simulated processing delay in milliseconds       |

---

## 5. Error Codes

| Code   | HTTP Status | Description                                  |
|--------|-------------|----------------------------------------------|
| PS-001 | 409         | Duplicate idempotency key with different amount |
| PS-002 | 404         | Payment not found                            |
| PS-003 | 422         | Payment already refunded                     |
