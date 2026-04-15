# Order Management System — High-Level Design Document

| Field       | Value                      |
|-------------|----------------------------|
| **Type**    | HLD                        |
| **Scope**   | Root — System Architecture |
| **Version** | 1.0                        |
| **Date**    | 2025-04-06                 |
| **Status**  | Draft                      |
| **Author**  | OMS Engineering Team       |

---

## Table of Contents

1. [Architecture Overview](#1-architecture-overview)
2. [System Layers](#2-system-layers)
3. [Service Inventory](#3-service-inventory)
4. [Key Design Decisions](#4-key-design-decisions)
5. [Kafka Topic Map](#5-kafka-topic-map)
6. [Security Architecture](#6-security-architecture)
7. [Observability Stack](#7-observability-stack)

---

## 1. Architecture Overview

The OMS uses a **microservices architecture** with an event-driven backbone. All services are deployed as Docker containers, communicating internally via REST and Kafka. Spring Cloud Gateway is the single public entry point. Keycloak provides identity and access management.

---

## 2. System Layers

| Layer           | Components                                         | Technology                              |
|-----------------|----------------------------------------------------|-----------------------------------------|
| **Presentation**| React UI — Chat, Dashboard, Admin                  | React 18, TypeScript, Tailwind          |
| **Gateway**     | Spring Cloud Gateway + JWT validation              | Spring Cloud Gateway 2023.x             |
| **AI / Agent**  | Spring AI agents with `@Tool` MCP tools            | Spring AI 1.x, Claude API               |
| **Application** | Order, Payment, Inventory, Product services        | Spring Boot 3.3, Java 21                |
| **Messaging**   | Async event bus                                    | Apache Kafka 3.x                        |
| **Data**        | Relational, Document, Vector, Cache                | PostgreSQL 16, MongoDB 7, pgvector, Redis 7 |
| **Identity**    | Auth server, user store, audit log                 | Keycloak 24                             |

---

## 3. Service Inventory

| Service                | Port | Responsibility                          | Primary Store              |
|------------------------|------|-----------------------------------------|----------------------------|
| `gateway`              | 8080 | Routing, JWT validation, CORS           | —                          |
| `agent-service`        | 8085 | LLM orchestration, MCP tools            | Redis (ChatMemory)         |
| `order-service`        | 8081 | Order CRUD, state machine               | PostgreSQL                 |
| `payment-service`      | 8082 | Mock payment, ledger, refunds           | PostgreSQL                 |
| `inventory-service`    | 8083 | Stock management, reservations          | PostgreSQL                 |
| `product-service`      | 8084 | Product catalog, RAG embeddings         | MongoDB + pgvector         |
| `notification-service` | 8086 | Email / webhook notifications           | — (event consumer)         |
| `keycloak`             | 8180 | Identity, auth, login audit             | PostgreSQL                 |
| `react-ui`             | 3000 | Customer and admin frontend             | —                          |

---

## 4. Key Design Decisions

### 4.1 Single Git Repo — Multi-Module Gradle

All services live in one IntelliJ project with Gradle submodules. This simplifies dependency management, enables shared libraries, and is ideal for learning.

### 4.2 Event-Driven Saga

Order placement triggers a Kafka saga:

```
OrderPlaced → InventoryReserved → PaymentInitiated → PaymentConfirmed → OrderConfirmed
```

Each step is a separate Kafka topic. Failure at any step publishes a compensating event.

### 4.3 MCP via Spring AI `@Tool`

MCP tool definitions are Java methods annotated with `@Tool` inside `agent-service`. Spring AI registers them with Claude automatically. No separate MCP server process is needed.

### 4.4 RAG with pgvector

Product embeddings are stored in PostgreSQL with the `pgvector` extension. The Product Agent embeds the user query and runs cosine similarity search to retrieve relevant products before calling Claude.

### 4.5 Public Product Browsing

The `/api/products/**` route in the Gateway is `permitAll()`. Authenticated users get personalised results. Guests see the full catalog but cannot order.

---

## 5. Kafka Topic Map

| Topic                      | Producer             | Consumer(s)                                 | Purpose                        |
|----------------------------|----------------------|---------------------------------------------|--------------------------------|
| `oms.orders.placed`        | order-service        | inventory-service, payment-service          | New order created              |
| `oms.inventory.reserved`   | inventory-service    | payment-service                             | Stock confirmed                |
| `oms.payment.initiated`    | payment-service      | payment-service (async)                     | Payment in progress            |
| `oms.payment.confirmed`    | payment-service      | order-service                               | Payment success                |
| `oms.payment.failed`       | payment-service      | order-service, inventory-service            | Compensate saga                |
| `oms.orders.shipped`       | order-service        | notification-service                        | Trigger email                  |
| `oms.orders.cancelled`     | order-service        | inventory-service, notification-service     | Release stock                  |
| `oms.inventory.insufficient`| inventory-service   | order-service                               | Cancel saga — no stock         |
| `oms.products.updated`     | product-service      | product-service (re-embed)                  | Embedding refresh              |

---

## 6. Security Architecture

- **Keycloak realm:** `oms` — issues JWTs for all users
- **`react-ui` client:** public, PKCE, `directAccessGrants` enabled
- **Gateway** validates JWT on every request using Keycloak public key (auto-fetched)
- **`TokenRelay` filter** forwards JWT to downstream services
- Each microservice validates the forwarded JWT independently
- `@PreAuthorize` annotations enforce role-based access at method level

---

## 7. Observability Stack

| Tool               | Port            | Purpose                                             |
|--------------------|-----------------|-----------------------------------------------------|
| Spring Actuator    | per-service `/actuator` | Health, metrics, httptrace                  |
| Prometheus         | 9090            | Metrics scraping                                    |
| Grafana            | 3001            | Dashboards — latency, throughput, errors            |
| Kafdrop            | 9000            | Kafka topic and consumer group viewer               |
| Keycloak Console   | 8180/admin      | User management, login event log                    |
