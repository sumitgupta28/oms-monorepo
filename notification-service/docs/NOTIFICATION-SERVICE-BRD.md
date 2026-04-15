# Notification Service — Business Requirements Document

| Field       | Value                         |
|-------------|-------------------------------|
| **Type**    | BRD                           |
| **Scope**   | notification-service          |
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

The Notification Service consumes Kafka events from other services and sends outbound notifications to customers via email. It is a **pure consumer** — it has no REST API and exposes no data. Locally it logs notifications to console instead of sending real emails.

---

## 2. Business Goals

- Notify customers of key order milestones without coupling producer services to notification logic
- Support email channel locally via console logging (configurable SMTP for production)
- Decouple notification logic — adding a new channel (SMS, push) requires no changes to other services

---

## 3. Functional Requirements

### FR-NOT-01 — Email Notifications

| Trigger Event           | Notification                          |
|-------------------------|---------------------------------------|
| `OrderPlacedEvent`      | Order confirmation email              |
| `PaymentConfirmedEvent` | Payment confirmation email            |
| `OrderShippedEvent`     | Shipment notification email           |
| `OrderCancelledEvent`   | Cancellation confirmation email       |

### FR-NOT-02 — Local Development Mode

- When `SPRING_PROFILES_ACTIVE=local`, log notification content to console instead of sending
- No SMTP configuration required for local development

---

## 4. Non-Functional Requirements

| Attribute          | Requirement                                                            |
|--------------------|------------------------------------------------------------------------|
| **Reliability**    | At-least-once delivery via Kafka consumer group                        |
| **Idempotency**    | Duplicate events produce duplicate emails — acceptable for MVP         |
| **Configurability**| Email sender, subject templates configurable via properties            |

---

## 5. Acceptance Criteria

- `OrderShippedEvent` causes shipment email log entry within 2 seconds locally
- Service restarts cleanly and replays missed events from Kafka offset
