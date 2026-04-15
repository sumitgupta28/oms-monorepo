# OMS — Low-Level Design Document (Consolidated)

| Field       | Value                     |
|-------------|---------------------------|
| **Type**    | LLD — Full System         |
| **Version** | 1.0                       |
| **Date**    | 2025-04-06                |
| **Status**  | Draft                     |
| **Author**  | OMS Engineering Team      |

> **Navigation:** This document consolidates the root-level LLD and all service-specific LLDs.
> Individual service docs live in `<service>/docs/<SERVICE>-LLD.md`.

---

## Table of Contents

1. [Monorepo & Directory Structure](#1-monorepo--directory-structure)
2. [Shared Conventions](#2-shared-conventions)
3. [JWT Token Structure](#3-jwt-token-structure)
4. [Gradle 8.14.3 + Java 21 Setup](#4-gradle-8143--java-21-setup)
5. [Docker Compose Network](#5-docker-compose-network)
6. [Environment Variables](#6-environment-variables)
7. [Service LLDs](#7-service-llds)
   - [7.1 Gateway](#71-gateway)
   - [7.2 Agent Service](#72-agent-service)
   - [7.3 Order Service](#73-order-service)
   - [7.4 Payment Service](#74-payment-service)
   - [7.5 Inventory Service](#75-inventory-service)
   - [7.6 Product Service](#76-product-service)
   - [7.7 Notification Service](#77-notification-service)
   - [7.8 React UI](#78-react-ui)

---

## 1. Monorepo & Directory Structure

The repository is structured as a **Gradle multi-module project**. Each Spring Boot service is a submodule. The root `build.gradle` defines shared plugins, Java 21 toolchain, and common dependencies.

```
oms-monorepo/
├── build.gradle                  ← shared plugin + dep config
├── settings.gradle               ← submodule declarations
├── gradle/wrapper/               ← Gradle 8.14.3
├── docker-compose.yml            ← all-in-one compose
├── docker-compose.infra.yml      ← infra only (PostgreSQL, Kafka, etc.)
├── docker-compose.apps.yml       ← services only
├── .env.example
├── BRD.md                        ← consolidated BRD (this repo)
├── HLD.md                        ← consolidated HLD
├── LLD.md                        ← consolidated LLD
├── root-docs/                    ← original root BRD/HLD/LLD as .md
├── shared-events/                ← shared Kafka event records
│   └── src/main/java/com/oms/events/
├── gateway/
│   ├── build.gradle
│   ├── src/
│   └── docs/                     ← GATEWAY-BRD.md, HLD.md, LLD.md
├── agent-service/
├── order-service/
├── payment-service/
├── inventory-service/
├── product-service/
├── notification-service/
├── react-ui/                     ← React + TypeScript (npm)
│   └── docs/
├── postgres/
│   └── init.sql                  ← DB creation + pgvector extension
└── keycloak/realms/
    └── oms-realm.json            ← Keycloak realm auto-import
```

---

## 2. Shared Conventions

### 2.1 Package Structure (per service)

```
com.oms.<service>/
  controller/  — REST endpoints (@RestController)
  service/     — business logic (@Service)
  repository/  — Spring Data interfaces
  domain/      — JPA entities / MongoDB documents
  event/       — Kafka event records (local consumers/producers)
  config/      — Spring configuration beans
  exception/   — custom exceptions + @RestControllerAdvice
```

### 2.2 Error Handling

Every service has a `@RestControllerAdvice` that maps domain exceptions to **RFC 7807 `ProblemDetail`** responses:

| Status | Scenario          |
|--------|-------------------|
| 400    | Validation error  |
| 401    | Unauthorized      |
| 403    | Forbidden         |
| 404    | Not found         |
| 409    | Conflict / invalid state transition |
| 422    | Unprocessable entity |
| 500    | Internal error    |

### 2.3 Shared Kafka Event Schema

All Kafka events are Java records in the `shared-events` submodule, serialised as JSON:

```java
// sealed interface — all domain events implement this
public sealed interface DomainEvent
    permits OrderPlacedEvent, PaymentConfirmedEvent, PaymentFailedEvent,
            InventoryReservedEvent, InventoryInsufficientEvent,
            OrderShippedEvent, OrderCancelledEvent, ProductUpdatedEvent {

    UUID    correlationId();  // traces full order journey
    String  eventType();      // e.g. "ORDER_PLACED"
    Instant timestamp();
}
```

### 2.4 Flyway Migration Convention

All PostgreSQL-backed services use Flyway. Migration files follow:

```
src/main/resources/db/migration/
  V1__init_schema.sql    ← initial table creation
  V2__add_indexes.sql    ← subsequent changes
```

`ddl-auto` is set to `validate` for services with JPA entities, `none` for product-service (pgvector only).

---

## 3. JWT Token Structure

Keycloak issues access tokens containing:

| Claim          | Type           | Example                    |
|----------------|----------------|----------------------------|
| `sub`          | UUID string    | `a1b2c3d4-...`             |
| `email`        | String         | `user@example.com`         |
| `given_name`   | String         | `Alex`                     |
| `family_name`  | String         | `Chen`                     |
| `realm_access` | Object         | `{"roles":["CUSTOMER"]}`   |
| `exp`          | Unix timestamp | `1700000000`               |

Services extract `sub` as `userId` and `realm_access.roles` for `@PreAuthorize` checks.

---

## 4. Gradle 8.14.3 + Java 21 Setup

### 4.1 Root `build.gradle` Key Config

```groovy
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

// BOM imports — all submodules inherit these versions
dependencyManagement {
    imports {
        mavenBom "org.springframework.boot:spring-boot-dependencies:3.3.0"
        mavenBom "org.springframework.cloud:spring-cloud-dependencies:2023.0.1"
        mavenBom "org.springframework.ai:spring-ai-bom:1.1.4"
    }
}
```

### 4.2 Gradle Wrapper

```properties
# gradle/wrapper/gradle-wrapper.properties
distributionUrl=https://services.gradle.org/distributions/gradle-8.14.3-bin.zip
```

```bash
./gradlew wrapper --gradle-version 8.14.3   # regenerate if needed
```

### 4.3 Common Build Commands

```bash
./gradlew build                                           # build all
./gradlew :order-service:build                            # single service
./gradlew test                                            # test all
./gradlew :order-service:test --tests "com.oms.order.*"  # targeted test
```

---

## 5. Docker Compose Network

All containers share a single bridge network: **`oms-network`**. Services address each other by container name (e.g. `http://order-service:8081`). The Anthropic API is the only external call that leaves the Docker network.

```bash
# Recommended startup order:
docker compose -f docker-compose.infra.yml up -d   # start infra first
docker compose -f docker-compose.apps.yml  up -d   # then services
```

---

## 6. Environment Variables

| Variable                   | Used By                       | Example Value  |
|----------------------------|-------------------------------|----------------|
| `ANTHROPIC_API_KEY`        | agent-service, product-service | `sk-ant-...`  |
| `KEYCLOAK_ADMIN`           | keycloak                      | `admin`        |
| `KEYCLOAK_ADMIN_PASSWORD`  | keycloak                      | `admin`        |
| `POSTGRES_PASSWORD`        | postgres                      | `postgres`     |
| `SPRING_PROFILES_ACTIVE`   | all services                  | `local`        |
| `SPRING_DATASOURCE_URL`    | order/payment/inventory/product | `jdbc:postgresql://postgres:5432/<db>` |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | all services with Kafka | `kafka:9092`  |
| `KEYCLOAK_ISSUER`          | all services                  | `http://keycloak:8180/realms/oms` |

Copy `.env.example` to `.env` and fill in `ANTHROPIC_API_KEY` before running `docker compose up`.

---

## 7. Service LLDs

---

### 7.1 Gateway

> Full doc: [`gateway/docs/GATEWAY-LLD.md`](gateway/docs/GATEWAY-LLD.md)

**Package structure:**
```
com.oms.gateway/
  config/  — SecurityConfig, RouteConfig
  filter/  — JwtClaimsForwardFilter
```

**Key classes:**

| Class                  | Responsibility                                                    |
|------------------------|-------------------------------------------------------------------|
| `SecurityConfig`       | `SecurityWebFilterChain` — JWT resource server, public routes     |
| `KeycloakJwtConverter` | Extracts `realm_access.roles` → Spring `GrantedAuthority` list    |
| `JwtClaimsForwardFilter` | Forwards userId/email/roles as request headers downstream      |

**Error codes:**

| Code   | Status | Description                       |
|--------|--------|-----------------------------------|
| GW-001 | 401    | Missing or invalid JWT            |
| GW-002 | 403    | Insufficient role                 |
| GW-003 | 503    | Downstream service unavailable    |

---

### 7.2 Agent Service

> Full doc: [`agent-service/docs/AGENT-SERVICE-LLD.md`](agent-service/docs/AGENT-SERVICE-LLD.md)

**Package structure:**
```
com.oms.agent/
  config/      — AgentConfig (ChatClient bean with @Tool beans + Redis ChatMemory)
  tools/       — OrderTools, PaymentTools, ProductTools, ValidationTools
  controller/  — ChatController (SSE endpoint)
  client/      — OrderClient, PaymentClient, ProductClient
```

**Key classes:**

| Class            | Responsibility                                                      |
|------------------|---------------------------------------------------------------------|
| `AgentConfig`    | Builds `ChatClient` with all `@Tool` beans and Redis `ChatMemory`   |
| `OrderTools`     | `@Tool` methods: `placeOrder`, `trackOrder`, `cancelOrder`          |
| `ChatController` | SSE endpoint — `chatClient.stream()` → `Flux<ServerSentEvent>`      |
| `OrderClient`    | `RestTemplate` wrapper for order-service REST API                   |

**Configuration:**

| Property                              | Default             |
|---------------------------------------|---------------------|
| `spring.ai.anthropic.chat.model`      | `claude-sonnet-4-6` |
| `spring.ai.anthropic.chat.max-tokens` | `4096`              |
| `spring.data.redis.host`              | `redis`             |

**Error codes:**

| Code   | Status | Description            |
|--------|--------|------------------------|
| AS-001 | 503    | Claude API unreachable |
| AS-002 | 400    | Empty message          |
| AS-003 | 401    | Missing JWT            |

---

### 7.3 Order Service

> Full doc: [`order-service/docs/ORDER-SERVICE-LLD.md`](order-service/docs/ORDER-SERVICE-LLD.md)

**Package structure:**
```
com.oms.order/
  domain/       — Order, OrderItem, OrderStatus, OutboxEvent
  statemachine/ — OrderStateMachineConfig
  repository/   — OrderRepository, OutboxEventRepository
  service/      — OrderService, OutboxPublisher (@Scheduled)
  kafka/        — OrderEventProducer, PaymentEventConsumer
  controller/   — OrderController
```

**Key classes:**

| Class                    | Responsibility                                                   |
|--------------------------|------------------------------------------------------------------|
| `OrderStateMachineConfig`| All states, transitions, and guards for the order FSM           |
| `OrderService`           | Business logic, delegates transitions to state machine           |
| `OutboxPublisher`        | `@Scheduled` poller — publishes unpublished outbox events        |
| `OrderEventProducer`     | Wraps `KafkaTemplate` for domain event publishing                |

**Database schema:** `orders`, `order_items`, `outbox_events` (see Flyway `V1__init_schema.sql`)

**Error codes:**

| Code   | Status | Description                 |
|--------|--------|-----------------------------|
| OS-001 | 404    | Order not found             |
| OS-002 | 409    | Invalid state transition    |
| OS-003 | 403    | Not the order owner         |

---

### 7.4 Payment Service

> Full doc: [`payment-service/docs/PAYMENT-SERVICE-LLD.md`](payment-service/docs/PAYMENT-SERVICE-LLD.md)

**Package structure:**
```
com.oms.payment/
  domain/      — Payment, PaymentLedger, PaymentStatus
  service/     — PaymentService, MockGateway
  kafka/       — PaymentEventProducer, OrderCancelledConsumer
  controller/  — PaymentController
```

**Key classes:**

| Class                   | Responsibility                                                |
|-------------------------|---------------------------------------------------------------|
| `MockGateway`           | Configurable delay + failure rate simulation                  |
| `PaymentService`        | Idempotency check, ledger write, Kafka publish                |
| `PaymentEventProducer`  | Publishes `PaymentConfirmedEvent` / `PaymentFailedEvent`      |

**Configuration:**

| Property                    | Default | Description                          |
|-----------------------------|---------|--------------------------------------|
| `mock.payment.failure-rate` | `0.1`   | Fraction of payments that fail       |
| `mock.payment.delay-ms`     | `1000`  | Simulated processing delay (ms)      |

**Error codes:**

| Code   | Status | Description                                    |
|--------|--------|------------------------------------------------|
| PS-001 | 409    | Duplicate idempotency key with different amount|
| PS-002 | 404    | Payment not found                              |
| PS-003 | 422    | Payment already refunded                       |

---

### 7.5 Inventory Service

> Full doc: [`inventory-service/docs/INVENTORY-SERVICE-LLD.md`](inventory-service/docs/INVENTORY-SERVICE-LLD.md)

**Package structure:**
```
com.oms.inventory/
  domain/      — Inventory, StockMovement, MovementType
  repository/  — InventoryRepository (with @Lock queries)
  service/     — InventoryService, ReservationService
  kafka/       — OrderEventConsumer, InventoryEventProducer
  controller/  — InventoryController
```

**Key classes:**

| Class                    | Responsibility                                                           |
|--------------------------|--------------------------------------------------------------------------|
| `ReservationService`     | Atomic reserve/release with `@Transactional` + `@Lock(PESSIMISTIC_WRITE)` |
| `OrderEventConsumer`     | Consumes `OrderPlacedEvent`, `OrderCancelledEvent`, `OrderShippedEvent`  |
| `InventoryEventProducer` | Publishes `InventoryReservedEvent` / `InventoryInsufficientEvent`        |

**Configuration:**

| Property                        | Default | Description                       |
|---------------------------------|---------|-----------------------------------|
| `inventory.low-stock-threshold` | `5`     | Publish `LowStockEvent` below qty |

**Error codes:**

| Code    | Status | Description                               |
|---------|--------|-------------------------------------------|
| INV-001 | 404    | Product not found in inventory            |
| INV-002 | 422    | Insufficient stock for requested quantity |
| INV-003 | 409    | Duplicate reservation for same order      |

---

### 7.6 Product Service

> Full doc: [`product-service/docs/PRODUCT-SERVICE-LLD.md`](product-service/docs/PRODUCT-SERVICE-LLD.md)

**Package structure:**
```
com.oms.product/
  domain/      — Product (@Document — MongoDB)
  repository/  — ProductRepository (MongoRepository)
  service/     — ProductService, EmbeddingService
  controller/  — ProductController
  config/      — VectorStoreConfig, SecurityConfig
```

**Key classes:**

| Class             | Responsibility                                                   |
|-------------------|------------------------------------------------------------------|
| `EmbeddingService`| Calls Spring AI `EmbeddingClient`, writes to `VectorStore`       |
| `ProductService`  | CRUD + triggers embedding on save/update                         |
| `VectorStoreConfig`| Wires `PgVectorStore` with `JdbcTemplate` + `EmbeddingModel`   |

**Data stores:**
- MongoDB: schema-less `products` collection
- PostgreSQL: `vector_store` table managed by Spring AI (`initialize-schema: true`)

**Error codes:**

| Code    | Status | Description          |
|---------|--------|----------------------|
| PRS-001 | 404    | Product not found    |
| PRS-002 | 400    | Empty search query   |

---

### 7.7 Notification Service

> Full doc: [`notification-service/docs/NOTIFICATION-SERVICE-LLD.md`](notification-service/docs/NOTIFICATION-SERVICE-LLD.md)

**Package structure:**
```
com.oms.notification/
  consumer/  — OrderEventConsumer, PaymentEventConsumer
  sender/    — EmailSender (interface), SmtpEmailSender, ConsoleEmailSender
  template/  — EmailTemplateService (Thymeleaf)
  config/    — MailConfig, KafkaConsumerConfig
```

**Key classes:**

| Class                  | Responsibility                                                         |
|------------------------|------------------------------------------------------------------------|
| `OrderEventConsumer`   | `@KafkaListener` on placed/shipped/cancelled topics                    |
| `ConsoleEmailSender`   | `@Profile("local")` — logs email to SLF4J, no SMTP needed             |
| `SmtpEmailSender`      | `@Profile("prod")` — sends via `JavaMailSender`                        |
| `EmailTemplateService` | Renders Thymeleaf HTML email templates                                 |

**Configuration:**

| Property                         | Default                  |
|----------------------------------|--------------------------|
| `spring.kafka.consumer.group-id` | `oms-notification-group` |
| `notification.from-email`        | `noreply@oms.com`        |

---

### 7.8 React UI

> Full doc: [`react-ui/docs/REACT-UI-LLD.md`](react-ui/docs/REACT-UI-LLD.md)

**Directory structure:**
```
react-ui/src/
├── auth/        — AuthContext, useAuth, authApi
├── api/         — axiosClient, orderApi, productApi, paymentApi
├── layouts/     — PublicLayout, ProtectedLayout
├── pages/       — auth/, products/, chat/, admin/
├── components/  — chat/, products/, common/
└── hooks/       — useAgentStream, useOrderHistory
```

**Key components:**

| Component          | Responsibility                                                   |
|--------------------|------------------------------------------------------------------|
| `AuthContext`      | `user`, `tokens`, `login()`, `logout()`, `hasRole()`            |
| `ProtectedLayout`  | Redirect to `/login` if unauthenticated; 403 if wrong role       |
| `useAgentStream`   | `EventSource` → token accumulation → tool-call events           |
| `ToolCallChip`     | Teal badge showing invoked `@Tool` method name                   |
| `axiosClient`      | Axios instance with JWT interceptor and auto-refresh             |

**State management:** Auth in `AuthContext`, server data in React Query, chat in `useAgentStream` local state. No Redux.

**Environment variables:**

| Variable                       | Example                 |
|--------------------------------|-------------------------|
| `REACT_APP_KEYCLOAK_URL`       | `http://localhost:8180` |
| `REACT_APP_KEYCLOAK_REALM`     | `oms`                   |
| `REACT_APP_KEYCLOAK_CLIENT_ID` | `react-ui`              |
| `REACT_APP_GATEWAY_URL`        | `http://localhost:8080` |
