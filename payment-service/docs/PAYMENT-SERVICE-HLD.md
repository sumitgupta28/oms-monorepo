# Payment Service — High-Level Design Document

| Field       | Value                              |
|-------------|------------------------------------|
| **Type**    | HLD                                |
| **Scope**   | payment-service (Mock)             |
| **Version** | 1.0                                |
| **Date**    | 2025-04-06                         |
| **Status**  | Draft                              |
| **Author**  | OMS Engineering Team               |

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

Spring Boot 3.3 + Spring Data JPA on **PostgreSQL**. In-memory mock gateway bean with configurable delay and failure rate. Full ledger in PostgreSQL. Kafka for async confirmation events.

---

## 2. API Endpoints

| Method | Path                    | Auth         | Description             |
|--------|-------------------------|--------------|-------------------------|
| POST   | `/payments`             | CUSTOMER JWT | Initiate payment        |
| GET    | `/payments/{id}`        | CUSTOMER JWT | Get payment status      |
| POST   | `/payments/{id}/refund` | ADMIN JWT    | Refund a payment        |

---

## 3. Dependencies

| Dependency | Type     | Purpose                          |
|------------|----------|----------------------------------|
| PostgreSQL | Internal | `payments` and `payment_ledger` tables |
| Kafka      | Internal | Publish confirmation/failure events |

---

## 4. Kafka Events

| Topic                    | Direction | Event Type               | Notes                       |
|--------------------------|-----------|--------------------------|-----------------------------|
| `oms.payment.confirmed`  | Produces  | `PaymentConfirmedEvent`  | After mock processing       |
| `oms.payment.failed`     | Produces  | `PaymentFailedEvent`     | On mock failure             |
| `oms.orders.cancelled`   | Consumes  | `OrderCancelledEvent`    | Triggers refund flow        |

---

## 5. Data Model

```sql
payments (
    id               UUID PRIMARY KEY,
    order_id         UUID NOT NULL,
    user_id          VARCHAR(255) NOT NULL,
    amount           DECIMAL(12,2) NOT NULL,
    status           VARCHAR(50) NOT NULL,      -- PaymentStatus enum
    idempotency_key  VARCHAR(255) NOT NULL UNIQUE,
    failure_reason   VARCHAR(255),
    created_at       TIMESTAMPTZ,
    processed_at     TIMESTAMPTZ
)

payment_ledger (
    id          UUID PRIMARY KEY,
    payment_id  UUID NOT NULL,
    order_id    UUID NOT NULL,
    entry_type  VARCHAR(50) NOT NULL,           -- CHARGE | REFUND
    amount      DECIMAL(12,2) NOT NULL,
    created_at  TIMESTAMPTZ
)
```

---

## 6. Security

- JWT required on all endpoints
- `Idempotency-Key` header required on `POST /payments`
