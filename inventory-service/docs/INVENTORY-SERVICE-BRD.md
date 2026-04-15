# Inventory Service — Business Requirements Document

| Field       | Value                         |
|-------------|-------------------------------|
| **Type**    | BRD                           |
| **Scope**   | inventory-service             |
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

The Inventory Service manages stock levels for all products. It reserves stock when an order is placed, releases it on cancellation, and deducts it on shipment. It enforces stock constraints to prevent overselling.

---

## 2. Business Goals

- Prevent overselling by atomically reserving stock on order placement
- Release reservations on order cancellation or payment failure
- Deduct confirmed stock on order shipment
- Expose stock levels to the Product Catalog and Validation Agent

---

## 3. Functional Requirements

### FR-INV-01 — Stock Reservation

- Consume `OrderPlacedEvent` — reserve requested quantity
- Publish `InventoryReservedEvent` on success
- Publish `InventoryInsufficientEvent` if stock unavailable
- Reservation held until `PaymentConfirmed` or `OrderCancelled`

### FR-INV-02 — Stock Management

- REST API for admins to add, adjust, and view stock levels
- Stock level visible on product detail (public read)
- Low stock threshold alert published to Kafka

---

## 4. Non-Functional Requirements

| Attribute       | Requirement                                                        |
|-----------------|--------------------------------------------------------------------|
| **Consistency** | Pessimistic lock on stock reservation to prevent race conditions   |
| **Idempotency** | Duplicate `OrderPlacedEvent` consumption is a no-op               |
| **Auditability**| Every stock movement recorded in `stock_movements` table           |

---

## 5. Acceptance Criteria

- `OrderPlacedEvent` with qty > available stock results in `InventoryInsufficientEvent`
- Concurrent orders for last unit — only one succeeds, other gets `InsufficientEvent`
- `OrderCancelledEvent` restores reserved quantity to available
