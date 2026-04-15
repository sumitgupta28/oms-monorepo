# Gateway — Business Requirements Document

| Field       | Value                              |
|-------------|------------------------------------|
| **Type**    | BRD                                |
| **Scope**   | gateway — Spring Cloud Gateway     |
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

The Gateway is the **single public entry point** for all OMS API traffic. It validates JWT tokens issued by Keycloak and routes requests to downstream microservices.

---

## 2. Business Goals

- Centralise authentication — no downstream service needs to handle raw login
- Enable CORS for the React UI
- Route traffic by path prefix to appropriate services
- Forward JWT via `TokenRelay` to downstream services

---

## 3. Functional Requirements

### FR-GW-01 — JWT Validation

- Validate every incoming JWT against Keycloak public key
- Reject expired or tampered tokens with `401`
- Extract roles and forward as headers

### FR-GW-02 — Routing

| Path Prefix         | Downstream Service     |
|---------------------|------------------------|
| `/api/chat/**`      | `agent-service:8085`   |
| `/api/orders/**`    | `order-service:8081`   |
| `/api/products/**`  | `product-service:8084` (public) |
| `/api/payments/**`  | `payment-service:8082` |
| `/api/inventory/**` | `inventory-service:8083` |

### FR-GW-03 — CORS

- Allow origin `http://localhost:3000`
- Allow all HTTP methods and headers

---

## 4. Non-Functional Requirements

| Attribute       | Requirement                  |
|-----------------|------------------------------|
| **Latency**     | < 5ms added per request      |
| **Availability**| Auto-restart on crash        |

---

## 5. Acceptance Criteria

- All `/api/chat` calls without JWT return `401`
- All `/api/products` calls without JWT return `200`
- `TokenRelay` header present on all forwarded requests
