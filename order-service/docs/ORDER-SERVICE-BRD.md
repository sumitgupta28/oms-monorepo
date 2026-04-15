# Order Service — Business Requirements Document

| Field       | Value                         |
|-------------|-------------------------------|
| **Type**    | BRD                           |
| **Scope**   | order-service                 |
| **Version** | 1.0                           |
| **Date**    | 2025-04-06                    |
| **Status**  | Draft                         |
| **Author**  | OMS Engineering Team          |

---

## Table of Contents

1. [Purpose](#1-purpose)
2. [Business Goals](#2-business-goals)
3. [Functional Requirements](#3-functional-requirements)
4. [Non-Functional Requirements](#4-non-functional-requirements)
5. [Acceptance Criteria](#5-acceptance-criteria)

---

## 1. Purpose

The Order Service is the **core domain service** managing the full order lifecycle from creation through delivery. It implements a state machine and publishes Kafka events to drive the saga.

---

## 2. Business Goals

- Provide reliable order CRUD with state machine enforcement
- Publish domain events to drive the distributed saga
- Allow customers to view their own orders and admins to view all

---

## 3. Functional Requirements

### FR-OS-01 — Order Lifecycle

- Create order with line items and `userId`
- Transition states: `PENDING → VALIDATED → PAYMENT_INITIATED → PAID → SHIPPED → DELIVERED`
- Cancel from `PENDING` or `VALIDATED` only
- Reject invalid transitions with `409`

### FR-OS-02 — Kafka Events

| Direction | Event                  | Trigger                            |
|-----------|------------------------|------------------------------------|
| Publishes | `OrderPlacedEvent`     | On order creation                  |
| Consumes  | `PaymentConfirmedEvent`| Advances order to `PAID`           |
| Consumes  | `InventoryInsufficientEvent` | Cancels the order             |
| Publishes | `OrderShippedEvent`    | When admin marks order as shipped  |
| Publishes | `OrderCancelledEvent`  | On cancellation                    |

---

## 4. Non-Functional Requirements

| Attribute       | Requirement                                            |
|-----------------|--------------------------------------------------------|
| **Consistency** | Outbox pattern for reliable event publishing           |
| **Idempotency** | Duplicate event consumption is safe (no-op)            |

---

## 5. Acceptance Criteria

- Order creation returns `201` with `orderId`
- Invalid state transition returns `409`
- `OrderPlacedEvent` appears in Kafka within 500ms
