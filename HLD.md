# OMS — High-Level Design Document (Consolidated)

| Field       | Value                     |
|-------------|---------------------------|
| **Type**    | HLD — Full System         |
| **Version** | 1.0                       |
| **Date**    | 2025-04-06                |
| **Status**  | Draft                     |
| **Author**  | OMS Engineering Team      |

> **Navigation:** This document consolidates the root-level HLD and all service-specific HLDs.
> Individual service docs live in `<service>/docs/<SERVICE>-HLD.md`.

---

## Table of Contents

1. [Architecture Overview](#1-architecture-overview)
2. [System Layers](#2-system-layers)
3. [Service Inventory](#3-service-inventory)
4. [Key Design Decisions](#4-key-design-decisions)
5. [Kafka Topic Map](#5-kafka-topic-map)
6. [Security Architecture](#6-security-architecture)
7. [Observability Stack](#7-observability-stack)
8. [Service HLDs](#8-service-hlds)
   - [8.1 Gateway](#81-gateway)
   - [8.2 Agent Service](#82-agent-service)
   - [8.3 Order Service](#83-order-service)
   - [8.4 Payment Service](#84-payment-service)
   - [8.5 Inventory Service](#85-inventory-service)
   - [8.6 Product Service](#86-product-service)
   - [8.7 Notification Service](#87-notification-service)
   - [8.8 React UI](#88-react-ui)

---

## 1. Architecture Overview

The OMS uses a **microservices architecture** with an event-driven backbone. All services are deployed as Docker containers, communicating internally via REST and Kafka. Spring Cloud Gateway is the single public entry point. Keycloak provides identity and access management.

```
React UI (3000)
     │
     ▼
Spring Cloud Gateway (8080)  ←── Keycloak JWT validation
     │
     ├──► agent-service (8085) ─── Claude API (external)
     ├──► order-service  (8081)
     ├──► payment-service (8082)
     ├──► inventory-service (8083)
     └──► product-service (8084) ─── Anthropic Embeddings (external)

         All services ◄──► Kafka (9092)
         order/payment/inventory ◄──► PostgreSQL (5432)
         product ◄──► MongoDB (27017) + pgvector (PostgreSQL)
         agent ◄──► Redis (6379)
```

---

## 2. System Layers

| Layer            | Components                                          | Technology                                   |
|------------------|-----------------------------------------------------|----------------------------------------------|
| **Presentation** | React UI — Chat, Dashboard, Admin                   | React 18, TypeScript, Tailwind CSS           |
| **Gateway**      | Spring Cloud Gateway + JWT validation               | Spring Cloud Gateway 2023.x, WebFlux         |
| **AI / Agent**   | Spring AI agents with `@Tool` MCP tools             | Spring AI 1.x, Claude API                   |
| **Application**  | Order, Payment, Inventory, Product services         | Spring Boot 3.3, Java 21                     |
| **Messaging**    | Async event bus                                     | Apache Kafka 3.x                             |
| **Data**         | Relational, Document, Vector, Cache                 | PostgreSQL 16, MongoDB 7, pgvector, Redis 7  |
| **Identity**     | Auth server, user store, audit log                  | Keycloak 24                                  |

---

## 3. Service Inventory

| Service                | Port | Responsibility                           | Primary Store              |
|------------------------|------|------------------------------------------|----------------------------|
| `gateway`              | 8080 | Routing, JWT validation, CORS            | —                          |
| `agent-service`        | 8085 | LLM orchestration, MCP tools            | Redis (ChatMemory)         |
| `order-service`        | 8081 | Order CRUD, state machine, outbox        | PostgreSQL (`orders` DB)   |
| `payment-service`      | 8082 | Mock payment, ledger, refunds            | PostgreSQL (`payments` DB) |
| `inventory-service`    | 8083 | Stock management, reservations           | PostgreSQL (`inventory` DB)|
| `product-service`      | 8084 | Product catalog, RAG embeddings          | MongoDB + pgvector         |
| `notification-service` | 8086 | Email / webhook notifications            | — (event consumer only)    |
| `keycloak`             | 8180 | Identity, auth, login audit              | PostgreSQL (`keycloak` DB) |
| `react-ui`             | 3000 | Customer and admin frontend              | —                          |

**Infrastructure ports:**

| Service    | Port | Purpose                       |
|------------|------|-------------------------------|
| Prometheus | 9090 | Metrics scraping              |
| Grafana    | 3001 | Dashboards                    |
| Kafdrop    | 9000 | Kafka UI                      |
| MongoDB    | 27017| Product documents             |
| Redis      | 6379 | ChatMemory cache              |

---

## 4. Key Design Decisions

### 4.1 Single Git Repo — Multi-Module Gradle

All services live in one IntelliJ project with Gradle submodules. This simplifies dependency management, enables shared libraries (`shared-events`), and is ideal for learning.

### 4.2 Event-Driven Saga

Order placement triggers a Kafka saga:

```
OrderPlaced ──► InventoryReserved ──► PaymentConfirmed ──► OrderConfirmed
     │                │                      │
     │           InsufficientEvent       PaymentFailed
     │                │                      │
     └──────────── Cancel saga ◄─────────────┘
```

Each step is a separate Kafka topic. Failure at any step publishes a compensating event.

### 4.3 Transactional Outbox Pattern

`order-service` writes events to an `outbox_events` table in the same transaction as the order. A `@Scheduled` poller reads unpublished events and sends them to Kafka — guaranteeing exactly-once delivery even if the service crashes after the DB write.

### 4.4 MCP via Spring AI `@Tool`

MCP tool definitions are Java methods annotated with `@Tool` inside `agent-service`. Spring AI registers them with Claude automatically. No separate MCP server process is needed.

### 4.5 RAG with pgvector

Product embeddings are stored in PostgreSQL with the `pgvector` extension. The Product Agent embeds the user query and runs cosine similarity search to retrieve relevant products before calling Claude.

### 4.6 Public Product Browsing

The `/api/products/**` route in the Gateway is `permitAll()`. Authenticated users get personalised results. Guests see the full catalog but cannot order.

### 4.7 Flyway Database Migrations

All PostgreSQL-backed services use **Flyway** for schema management. `ddl-auto` is set to `validate` (order/payment/inventory) or `none` (product). Migrations live in `src/main/resources/db/migration/`.

---

## 5. Kafka Topic Map

| Topic                        | Producer              | Consumer(s)                                      | Purpose                          |
|------------------------------|-----------------------|--------------------------------------------------|----------------------------------|
| `oms.orders.placed`          | order-service         | inventory-service, notification-service          | New order created                |
| `oms.inventory.reserved`     | inventory-service     | payment-service                                  | Stock confirmed, initiate payment|
| `oms.inventory.insufficient` | inventory-service     | order-service                                    | Cancel saga — no stock           |
| `oms.payment.confirmed`      | payment-service       | order-service, notification-service              | Payment success                  |
| `oms.payment.failed`         | payment-service       | order-service, inventory-service                 | Compensate saga                  |
| `oms.orders.shipped`         | order-service         | notification-service, inventory-service          | Trigger email + deduct stock     |
| `oms.orders.cancelled`       | order-service         | inventory-service, notification-service, payment-service | Release stock + refund  |
| `oms.products.updated`       | product-service       | product-service (re-embed)                       | Embedding refresh                |
| `oms.inventory.low-stock`    | inventory-service     | (alert consumer — future)                        | Low stock threshold alert        |

---

## 6. Security Architecture

- **Keycloak realm:** `oms` — issues JWTs for all users
- **`react-ui` client:** public, PKCE, `directAccessGrants` enabled
- **Gateway** validates JWT on every request using Keycloak JWKS (auto-fetched)
- **`TokenRelay` filter** forwards the validated JWT to every downstream service
- Each microservice re-validates the forwarded JWT independently
- `@PreAuthorize` annotations enforce role-based access at method level

**Roles:**

| Role              | Access                                |
|-------------------|---------------------------------------|
| `CUSTOMER`        | Chat, own orders, product browsing    |
| `ADMIN`           | All orders, product CRUD, stock mgmt  |
| `AGENT_MANAGER`   | Agent logs and activity               |

---

## 7. Observability Stack

| Tool              | Port             | Purpose                                          |
|-------------------|------------------|--------------------------------------------------|
| Spring Actuator   | per-service `/actuator` | Health, metrics, httptrace                |
| Prometheus        | 9090             | Metrics scraping from all `/actuator/prometheus` |
| Grafana           | 3001             | Dashboards — latency, throughput, errors         |
| Kafdrop           | 9000             | Kafka topic and consumer group viewer            |
| Keycloak Console  | 8180/admin       | User management, login event log                 |

---

## 8. Service HLDs

---

### 8.1 Gateway

> Full doc: [`gateway/docs/GATEWAY-HLD.md`](gateway/docs/GATEWAY-HLD.md)

**Technology:** Spring Cloud Gateway 2023.x, WebFlux, `spring-security-oauth2-resource-server`

**API Endpoints:**

| Method | Path               | Auth                          | Description                    |
|--------|--------------------|-------------------------------|--------------------------------|
| ANY    | `/api/**`          | JWT (except `/api/products`)  | Routes to downstream service   |
| GET    | `/actuator/health` | None                          | Health probe                   |

**Kafka:** None (stateless).

**Data store:** None. Routes configured in `application.yml`.

---

### 8.2 Agent Service

> Full doc: [`agent-service/docs/AGENT-SERVICE-HLD.md`](agent-service/docs/AGENT-SERVICE-HLD.md)

**Technology:** Spring Boot 3.3, Spring AI 1.x, Redis

**API Endpoints:**

| Method | Path                  | Auth         | Description                      |
|--------|-----------------------|--------------|----------------------------------|
| GET    | `/api/chat/stream`    | CUSTOMER JWT | SSE stream — chat with agents    |
| DELETE | `/api/chat/session`   | CUSTOMER JWT | Clear conversation history       |
| GET    | `/actuator/health`    | None         | Health probe                     |

**Dependencies:** Claude API (external), order/payment/product services (REST), Redis

**Data store:** Redis — `chat:session:{sessionId}` → `List<ChatMessage>`, TTL 24h

---

### 8.3 Order Service

> Full doc: [`order-service/docs/ORDER-SERVICE-HLD.md`](order-service/docs/ORDER-SERVICE-HLD.md)

**Technology:** Spring Boot 3.3, Spring Data JPA, PostgreSQL, Spring Kafka, Flyway

**API Endpoints:**

| Method | Path                   | Auth         | Description           |
|--------|------------------------|--------------|-----------------------|
| POST   | `/orders`              | CUSTOMER JWT | Create new order      |
| GET    | `/orders/my`           | CUSTOMER JWT | Get caller's orders   |
| GET    | `/orders/{id}`         | CUSTOMER JWT | Get order by ID       |
| GET    | `/orders`              | ADMIN JWT    | Get all orders        |
| PATCH  | `/orders/{id}/cancel`  | CUSTOMER JWT | Cancel order          |
| PATCH  | `/orders/{id}/ship`    | ADMIN JWT    | Mark as shipped       |

**Kafka:** Produces `OrderPlacedEvent`, `OrderShippedEvent`, `OrderCancelledEvent`. Consumes `PaymentConfirmedEvent`, `InventoryInsufficientEvent`.

**Data store:** PostgreSQL `orders` DB — tables: `orders`, `order_items`, `outbox_events`

---

### 8.4 Payment Service

> Full doc: [`payment-service/docs/PAYMENT-SERVICE-HLD.md`](payment-service/docs/PAYMENT-SERVICE-HLD.md)

**Technology:** Spring Boot 3.3, Spring Data JPA, PostgreSQL, Spring Kafka, Flyway

**API Endpoints:**

| Method | Path                    | Auth         | Description            |
|--------|-------------------------|--------------|------------------------|
| POST   | `/payments`             | CUSTOMER JWT | Initiate payment       |
| GET    | `/payments/{id}`        | CUSTOMER JWT | Get payment status     |
| POST   | `/payments/{id}/refund` | ADMIN JWT    | Refund a payment       |

**Kafka:** Produces `PaymentConfirmedEvent`, `PaymentFailedEvent`. Consumes `OrderCancelledEvent`.

**Data store:** PostgreSQL `payments` DB — tables: `payments`, `payment_ledger`

---

### 8.5 Inventory Service

> Full doc: [`inventory-service/docs/INVENTORY-SERVICE-HLD.md`](inventory-service/docs/INVENTORY-SERVICE-HLD.md)

**Technology:** Spring Boot 3.3, Spring Data JPA, PostgreSQL, Spring Kafka, Flyway, pessimistic locking

**API Endpoints:**

| Method | Path                             | Auth      | Description                    |
|--------|----------------------------------|-----------|--------------------------------|
| GET    | `/inventory/{productId}`         | None      | Get stock level                |
| GET    | `/inventory`                     | ADMIN JWT | List all stock levels          |
| POST   | `/inventory`                     | ADMIN JWT | Set initial stock              |
| PATCH  | `/inventory/{productId}/adjust`  | ADMIN JWT | Add or subtract units          |

**Kafka:** Produces `InventoryReservedEvent`, `InventoryInsufficientEvent`, `LowStockEvent`. Consumes `OrderPlacedEvent`, `OrderCancelledEvent`, `OrderShippedEvent`.

**Data store:** PostgreSQL `inventory` DB — tables: `inventory`, `stock_movements`

---

### 8.6 Product Service

> Full doc: [`product-service/docs/PRODUCT-SERVICE-HLD.md`](product-service/docs/PRODUCT-SERVICE-HLD.md)

**Technology:** Spring Boot 3.3, Spring Data MongoDB, Spring AI PgVectorStore, Anthropic Embeddings

**API Endpoints:**

| Method | Path               | Auth      | Description                         |
|--------|--------------------|-----------|-------------------------------------|
| GET    | `/products`        | None      | List all products (paginated)       |
| GET    | `/products/{id}`   | None      | Get product detail                  |
| GET    | `/products/search` | None      | Semantic search by query string     |
| POST   | `/products`        | ADMIN JWT | Create product + generate embedding |
| PUT    | `/products/{id}`   | ADMIN JWT | Update product + re-embed           |
| DELETE | `/products/{id}`   | ADMIN JWT | Delete product                      |

**Kafka:** Produces `ProductUpdatedEvent`.

**Data stores:** MongoDB `products` collection + PostgreSQL `products` DB (pgvector `vector_store` table)

---

### 8.7 Notification Service

> Full doc: [`notification-service/docs/NOTIFICATION-SERVICE-HLD.md`](notification-service/docs/NOTIFICATION-SERVICE-HLD.md)

**Technology:** Spring Boot 3.3, Spring Kafka, Spring Mail, Thymeleaf, profile-based email switching

**API Endpoints:** `GET /actuator/health` only. No business endpoints.

**Kafka:** Consumes `OrderPlacedEvent`, `PaymentConfirmedEvent`, `OrderShippedEvent`, `OrderCancelledEvent`.

**Data store:** None. Stateless event consumer.

---

### 8.8 React UI

> Full doc: [`react-ui/docs/REACT-UI-HLD.md`](react-ui/docs/REACT-UI-HLD.md)

**Technology:** React 18, TypeScript, Tailwind CSS, React Router v6, React Query v5

**Page Map:**

| Route               | Auth | Role                   | Component          |
|---------------------|------|------------------------|--------------------|
| `/products`         | No   | Any                    | ProductCatalogPage |
| `/products/:id`     | No   | Any                    | ProductDetailPage  |
| `/login`            | No   | Any                    | LoginPage          |
| `/register`         | No   | Any                    | RegisterPage       |
| `/chat`             | Yes  | CUSTOMER               | ChatPage           |
| `/orders`           | Yes  | CUSTOMER               | MyOrdersPage       |
| `/admin`            | Yes  | ADMIN                  | AdminDashboard     |
| `/admin/agent-logs` | Yes  | ADMIN / AGENT_MANAGER  | AgentLogsPage      |

**Streaming:** Native `EventSource` API → token accumulation → tool call chips → `done` event closes stream.

**Auth:** Keycloak Direct Grant → `localStorage` tokens → axios interceptor → auto-refresh 30s before expiry.
