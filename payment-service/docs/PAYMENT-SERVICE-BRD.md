# Payment Service — Business Requirements Document

| Field       | Value                              |
|-------------|------------------------------------|
| **Type**    | BRD                                |
| **Scope**   | payment-service (Mock)             |
| **Version** | 1.0                                |
| **Date**    | 2025-04-06                         |
| **Status**  | Draft                              |
| **Author**  | OMS Engineering Team               |

---

## Table of Contents

1. [Purpose](#1-purpose)
2. [Business Goals](#2-business-goals)
3. [Functional Requirements](#3-functional-requirements)
4. [Non-Functional Requirements](#4-non-functional-requirements)
5. [Acceptance Criteria](#5-acceptance-criteria)

---

## 1. Purpose

The Payment Service simulates a production-grade payment gateway for learning purposes. It implements **idempotency**, a transaction ledger, refunds, and a configurable failure rate.

---

## 2. Business Goals

- Teach the idempotency pattern without a real payment gateway dependency
- Maintain an accurate financial ledger per order
- Support refunds for cancelled orders
- Simulate async payment confirmation via Kafka

---

## 3. Functional Requirements

### FR-PS-01 — Payment Flow

- Initiate payment with `orderId` and `amount`
- Idempotency key prevents duplicate charges
- Async confirmation published to Kafka after configurable delay
- Configurable failure rate (default 10%) for testing error paths

### FR-PS-02 — Refunds

- Refund endpoint reverses a confirmed payment
- Ledger updated with `REFUND` entry
- `PaymentRefundedEvent` published to Kafka

---

## 4. Non-Functional Requirements

| Attribute       | Requirement                                              |
|-----------------|----------------------------------------------------------|
| **Idempotency** | Same idempotency key always returns same result          |
| **Auditability**| Every transaction recorded in the payment ledger         |

---

## 5. Acceptance Criteria

- Two calls with same idempotency key return identical response
- Failure rate produces ~10% failed payments in test runs
