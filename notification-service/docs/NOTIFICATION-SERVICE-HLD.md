# Notification Service — High-Level Design Document

| Field       | Value                         |
|-------------|-------------------------------|
| **Type**    | HLD                           |
| **Scope**   | notification-service          |
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

Spring Boot 3.3 + **Spring Kafka** consumer + **Spring Mail**. No database, no REST API. Thin event consumer that maps domain events to email templates and dispatches them. Profile-based switching between real SMTP and console logging.

---

## 2. API Endpoints

| Method | Path               | Auth | Description                                |
|--------|--------------------|------|--------------------------------------------|
| GET    | `/actuator/health` | None | Health probe only — no business endpoints  |

---

## 3. Dependencies

| Dependency                    | Type                    | Purpose                  |
|-------------------------------|-------------------------|--------------------------|
| Kafka                         | Internal                | Consume domain events    |
| Spring Mail / JavaMailSender  | External (prod) / Console (local) | Email dispatch |

---

## 4. Kafka Events

| Topic                    | Direction | Event Type               | Notification Sent              |
|--------------------------|-----------|--------------------------|--------------------------------|
| `oms.orders.placed`      | Consumes  | `OrderPlacedEvent`       | Order confirmation email       |
| `oms.payment.confirmed`  | Consumes  | `PaymentConfirmedEvent`  | Payment receipt email          |
| `oms.orders.shipped`     | Consumes  | `OrderShippedEvent`      | Shipment notification email    |
| `oms.orders.cancelled`   | Consumes  | `OrderCancelledEvent`    | Cancellation notice email      |

---

## 5. Data Model

**No database.** Stateless event consumer. Each consumed event is mapped to a Thymeleaf email template and dispatched. Consumer group ID: `oms-notification-group`.

---

## 6. Security

- No inbound REST API — no JWT required
- Kafka consumer group authentication if Kafka is secured (not needed locally)
- SMTP credentials loaded from environment variables — never hardcoded
