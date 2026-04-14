# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

### Build & Test (Gradle)
```bash
./gradlew build                          # Build all modules
./gradlew :order-service:build           # Build a single service
./gradlew test                           # Test all modules
./gradlew :order-service:test            # Test a single service
./gradlew :order-service:test --tests "com.oms.order.OrderServiceTest"  # Single test
```

### Frontend (React)
```bash
cd react-ui && npm install
npm start          # Dev server at localhost:3000
npm run build      # Production build
npm test           # Run tests
```

### Infrastructure
```bash
docker compose up -d          # Start all infrastructure + services
docker compose ps             # Check status
docker compose down -v        # Full teardown including volumes
```

### Environment Setup
Copy `.env.example` to `.env` and set `ANTHROPIC_API_KEY` before starting Docker Compose.

## Architecture Overview

This is a **Gradle multi-project Spring Boot 3.3 monorepo** implementing an AI-powered Order Management System. Services communicate via both REST (synchronous) and Kafka (asynchronous).

### Services & Ports

| Service | Port | Responsibility |
|---|---|---|
| `gateway` | 8080 | Spring Cloud Gateway — all external traffic enters here |
| `agent-service` | 8085 | Spring AI (Claude) chat agent with tool calling |
| `order-service` | 8081 | Order lifecycle management + transactional outbox |
| `payment-service` | 8082 | Mock payment processor |
| `inventory-service` | 8083 | Stock reservation/deallocation |
| `product-service` | 8084 | Product catalog (MongoDB) + vector search (pgvector) |
| `notification-service` | 8086 | Email dispatch via Kafka events |
| `react-ui` | 3000 | React 18 + TypeScript frontend |

Infrastructure: Keycloak (8180), Grafana (3001), Kafdrop (9000), Prometheus (9090), PostgreSQL (5432), MongoDB (27017), Redis (6379), Kafka (9092).

### Event-Driven Saga (Order Flow)

Order placement uses a saga pattern with Kafka:
1. `order-service` creates order → publishes `OrderPlacedEvent` → uses **Transactional Outbox** (`outbox_events` table) to guarantee delivery
2. `inventory-service` consumes → publishes `InventoryReservedEvent` or `InventoryInsufficientEvent`
3. `payment-service` consumes reserved event → publishes `PaymentConfirmedEvent` or `PaymentFailedEvent`
4. `order-service` consumes payment result → updates order status
5. `notification-service` consumes all events → dispatches emails

**Kafka topics:** `order.placed`, `payment.confirmed`, `payment.failed`, `inventory.reserved`, `inventory.insufficient`, `order.cancelled`, `order.shipped`, `product.updated`

**Shared Events** (`shared-events/` module): Sealed interface `DomainEvent` — all event types live here and are shared across services via Gradle dependency.

### AI Agent Service

`agent-service` uses Spring AI with Claude Sonnet 4.6 via `@Tool`-annotated methods:
- **OrderTools**: placeOrder, trackOrder, cancelOrder, getMyOrders
- **ProductTools**: searchProducts, getProductDetails (semantic via pgvector)
- **PaymentTools**: initiatePayment, confirmPayment
- **ValidationTools**: validate order/payment data

Chat memory is Redis-backed (10-message sliding window). Responses stream to the frontend via SSE using `useAgentStream` hook. The agent calls other services via REST clients (`ProductClient`, `OrderClient`, etc.) resolved through environment variables (e.g., `ORDER_SERVICE_URL`).

### Product Service — Dual Storage

Products are stored in two systems simultaneously:
- **MongoDB** (`products` DB): Full product documents
- **PostgreSQL + pgvector** (`products` DB): Vector embeddings for semantic search (generated using Anthropic embeddings)

Search via `GET /products/search?query=...` uses vector similarity, not keyword matching.

### Authentication

**Keycloak 24** (realm: `oms`, client: `react-ui`) issues JWTs. Every Spring Boot service validates tokens via `spring.security.oauth2.resourceserver.jwt.issuer-uri`. The Gateway enforces auth; downstream services re-validate independently.

Test credentials: `customer@oms.com / customer123` (CUSTOMER role), `admin@oms.com / admin123` (ADMIN role).

### Data Storage per Service

- `order-service` → PostgreSQL `orders` DB (JPA, auto-schema)
- `payment-service` → PostgreSQL `payments` DB
- `inventory-service` → PostgreSQL `inventory` DB
- `product-service` → MongoDB `products` + PostgreSQL `products` (pgvector)
- `agent-service` → Redis (ChatMemory)
- All services share the same PostgreSQL instance on port 5432 with separate databases.

### Observability

All services expose `/actuator/prometheus`. Prometheus scrapes them; Grafana (admin/admin) visualizes. Each service also exposes `/actuator/health`.

### Gradle Build Structure

Root `build.gradle` defines a shared BOM:
- Spring Boot 3.3.0, Spring Cloud 2023.0.1, Spring AI 1.1.4
- Java 21 toolchain
- All subprojects inherit common dependencies (Lombok, Testcontainers)

Each service has its own `build.gradle` adding only what it needs. `settings.gradle` lists all included subprojects.

### Frontend Structure

`react-ui/src/`:
- `pages/`: ProductCatalogPage, ChatPage, LoginPage, RegisterPage, AdminDashboard
- `hooks/useAgentStream.ts`: SSE streaming for AI chat
- `api/productApi.ts`, `orderApi.ts`: REST clients pointing to Gateway (port 8080)
- Auth via Keycloak OIDC; protected routes use `ProtectedLayout`
