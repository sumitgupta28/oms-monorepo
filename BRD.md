# OMS — Business Requirements Document (Consolidated)

| Field       | Value                     |
|-------------|---------------------------|
| **Type**    | BRD — Full System         |
| **Version** | 1.0                       |
| **Date**    | 2025-04-06                |
| **Status**  | Draft                     |
| **Author**  | OMS Engineering Team      |

> **Navigation:** This document consolidates the root-level BRD and all service-specific BRDs into a single reference.
> Individual service docs live in `<service>/docs/<SERVICE>-BRD.md`.

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Business Objectives](#2-business-objectives)
3. [Scope](#3-scope)
4. [Stakeholders](#4-stakeholders)
5. [System-Level Functional Requirements](#5-system-level-functional-requirements)
6. [Non-Functional Requirements](#6-non-functional-requirements)
7. [Constraints](#7-constraints)
8. [Glossary](#8-glossary)
9. [Service BRDs](#9-service-brds)
   - [9.1 Gateway](#91-gateway)
   - [9.2 Agent Service](#92-agent-service)
   - [9.3 Order Service](#93-order-service)
   - [9.4 Payment Service](#94-payment-service)
   - [9.5 Inventory Service](#95-inventory-service)
   - [9.6 Product Service](#96-product-service)
   - [9.7 Notification Service](#97-notification-service)
   - [9.8 React UI](#98-react-ui)

---

## 1. Executive Summary

The **Order Management System (OMS)** is an enterprise-grade, AI-powered platform that enables customers to browse products, place orders, make payments, and track fulfilment through a conversational AI interface. Administrators manage the product catalog, monitor orders, and observe AI agent activity in real time.

The system is built as a learning-oriented monorepo using **Java 21, Spring Boot 3.x, Spring AI, React, Kafka, PostgreSQL, MongoDB, Redis, and Keycloak**, deployed locally via Docker Compose.

---

## 2. Business Objectives

- Provide a natural-language ordering experience powered by **Claude** (Anthropic)
- Allow product browsing without authentication to reduce conversion friction
- Support a unified login for all roles: Customer, Admin, Agent Manager
- Build enterprise patterns: event-driven architecture, saga, outbox, RAG, MCP tool calling
- Serve as a reference implementation for learning agentic AI systems

---

## 3. Scope

### 3.1 In Scope

- Customer self-registration and login via custom React pages backed by Keycloak
- Public product catalog browsable without authentication
- AI-powered order placement, tracking, and cancellation via agent chat
- Mock payment processing with idempotent ledger
- Inventory reservation and validation
- Kafka-based event-driven saga across services
- RAG-powered product search using pgvector
- Admin dashboard for order management and agent activity monitoring
- Role-based access control: `CUSTOMER`, `ADMIN`, `AGENT_MANAGER`

### 3.2 Out of Scope

- Real payment gateway integration (Stripe / Razorpay) — mock only
- Email verification on registration (disabled locally)
- Mobile native apps
- Multi-region deployment

---

## 4. Stakeholders

| Role              | Responsibility                      | System Access                   |
|-------------------|-------------------------------------|---------------------------------|
| **Customer**      | Browse, order, pay, track           | React UI — Chat + Orders        |
| **Admin**         | Manage orders, products, users      | React UI — Admin Dashboard      |
| **Agent Manager** | Monitor agent calls and logs        | React UI — Agent Logs           |
| **Developer**     | Build, maintain, extend the system  | Full stack + Docker Compose     |

---

## 5. System-Level Functional Requirements

### FR-01 — Authentication & Registration

- Custom React login page used by all roles
- Customer self-registration with first name, last name, email, password
- `CUSTOMER` role assigned automatically on registration
- JWT tokens issued by Keycloak, validated at Spring Cloud Gateway
- Password strength validation client-side and server-side
- Forgot-password flow via Keycloak

### FR-02 — Product Catalog

- Public browsing — no authentication required
- Full-text and semantic (RAG) search powered by pgvector
- Product details: name, description, price, stock level, category
- Guest users prompted to sign in when attempting to order

### FR-03 — Order Management

- Order placement through conversational AI agent
- Order state machine: `PENDING → VALIDATED → PAYMENT_INITIATED → PAID → SHIPPED → DELIVERED`
- Order cancellation allowed from `PENDING` or `VALIDATED` states
- Customers view their own orders; Admins view all orders

### FR-04 — Payment

- Mock payment gateway simulates real Stripe-style flow
- Idempotency keys prevent double processing
- Configurable failure rate for testing error paths
- Refund workflow for cancelled orders

### FR-05 — AI Agent

- Spring AI `ChatClient` with `@Tool`-annotated MCP tools
- Claude (`claude-sonnet-4-6`) as the LLM provider
- Multi-turn memory via Redis-backed `ChatMemory`
- Four tool groups: Order, Payment, Validation, Product
- Tool calls visible in React UI for learning/debugging

---

## 6. Non-Functional Requirements

| Attribute        | Requirement                          | Notes                           |
|------------------|--------------------------------------|---------------------------------|
| **Performance**  | API response < 500ms p95 (ex-LLM)   | LLM latency ~2–5s expected      |
| **Availability** | 99% local uptime                     | Docker Compose restart policies |
| **Security**     | JWT on all protected endpoints       | Keycloak PKCE flow              |
| **Observability**| Prometheus + Grafana metrics         | Spring Actuator on all services |
| **Scalability**  | Stateless services, Kafka for async  | Redis session store             |
| **Auditability** | Full login event log                 | Keycloak Events API             |

---

## 7. Constraints

- Java 21 with Gradle 8.14.3 for all Spring Boot modules
- Docker Compose only — no Kubernetes for local development
- Anthropic API key required for Claude LLM calls
- Single Git repository — IntelliJ multi-module project structure

---

## 8. Glossary

| Term       | Definition                                                                      |
|------------|---------------------------------------------------------------------------------|
| **MCP**    | Model Context Protocol — Spring AI `@Tool` methods exposed to Claude           |
| **RAG**    | Retrieval-Augmented Generation — pgvector similarity search + LLM              |
| **Saga**   | Distributed transaction pattern using Kafka events                             |
| **Outbox** | Transactional pattern ensuring event publishing reliability                    |
| **JWT**    | JSON Web Token — signed bearer token issued by Keycloak                        |

---

## 9. Service BRDs

---

### 9.1 Gateway

> Full doc: [`gateway/docs/GATEWAY-BRD.md`](gateway/docs/GATEWAY-BRD.md)

**Purpose:** Single public entry point for all OMS API traffic. Validates JWT tokens and routes requests to downstream microservices.

**Key Functional Requirements:**

| Req ID     | Requirement                                                        |
|------------|--------------------------------------------------------------------|
| FR-GW-01   | Validate every JWT against Keycloak public key; reject with `401` if invalid |
| FR-GW-02   | Route `/api/chat/**` → agent-service, `/api/orders/**` → order-service, `/api/products/**` → product-service (public), `/api/payments/**` → payment-service |
| FR-GW-03   | Allow CORS from `http://localhost:3000`                            |

**Acceptance Criteria:**
- All `/api/chat` calls without JWT return `401`
- All `/api/products` calls without JWT return `200`
- `TokenRelay` header present on all forwarded requests

---

### 9.2 Agent Service

> Full doc: [`agent-service/docs/AGENT-SERVICE-BRD.md`](agent-service/docs/AGENT-SERVICE-BRD.md)

**Purpose:** Hosts all AI agents powered by Claude via Spring AI. Exposes `@Tool`-annotated MCP tools for order, payment, validation, and product operations. Manages multi-turn conversation memory in Redis.

**Key Functional Requirements:**

| Req ID     | Requirement                                                                |
|------------|----------------------------------------------------------------------------|
| FR-AS-01   | Stream Claude responses token-by-token via SSE; emit tool-call events      |
| FR-AS-02   | Expose MCP tools: `placeOrder`, `trackOrder`, `cancelOrder`, `initiatePayment`, `searchProducts`, `checkInventory` |

**Acceptance Criteria:**
- User sends "order laptop" → agent calls `placeOrder` tool automatically
- Tool call names visible in SSE stream
- Session context persists across page reload via Redis

---

### 9.3 Order Service

> Full doc: [`order-service/docs/ORDER-SERVICE-BRD.md`](order-service/docs/ORDER-SERVICE-BRD.md)

**Purpose:** Core domain service managing the full order lifecycle. Implements a state machine and publishes Kafka events to drive the saga.

**Key Functional Requirements:**

| Req ID     | Requirement                                                               |
|------------|---------------------------------------------------------------------------|
| FR-OS-01   | Create order; transition states `PENDING → … → DELIVERED`; cancel from `PENDING`/`VALIDATED` |
| FR-OS-02   | Publish `OrderPlacedEvent`; consume `PaymentConfirmedEvent` to advance to `PAID` |

**Acceptance Criteria:**
- Order creation returns `201` with `orderId`
- Invalid state transition returns `409`
- `OrderPlacedEvent` appears in Kafka within 500ms

---

### 9.4 Payment Service

> Full doc: [`payment-service/docs/PAYMENT-SERVICE-BRD.md`](payment-service/docs/PAYMENT-SERVICE-BRD.md)

**Purpose:** Simulates a production-grade payment gateway. Implements idempotency, a transaction ledger, refunds, and a configurable failure rate.

**Key Functional Requirements:**

| Req ID     | Requirement                                                              |
|------------|--------------------------------------------------------------------------|
| FR-PS-01   | Initiate payment with orderId + amount; idempotency key prevents duplicate charges; configurable 10% failure rate |
| FR-PS-02   | Refund endpoint reverses a confirmed payment; ledger updated with `REFUND` entry |

**Acceptance Criteria:**
- Two calls with same idempotency key return identical response
- Failure rate produces ~10% failed payments in test runs

---

### 9.5 Inventory Service

> Full doc: [`inventory-service/docs/INVENTORY-SERVICE-BRD.md`](inventory-service/docs/INVENTORY-SERVICE-BRD.md)

**Purpose:** Manages stock levels. Reserves on order placement, releases on cancellation, deducts on shipment. Prevents overselling.

**Key Functional Requirements:**

| Req ID     | Requirement                                                              |
|------------|--------------------------------------------------------------------------|
| FR-INV-01  | Consume `OrderPlacedEvent`; reserve stock; publish `InventoryReservedEvent` or `InventoryInsufficientEvent` |
| FR-INV-02  | REST API for admins to manage stock; low-stock threshold alert           |

**Acceptance Criteria:**
- `OrderPlacedEvent` with qty > available stock → `InventoryInsufficientEvent`
- Concurrent orders for last unit — only one succeeds
- `OrderCancelledEvent` restores reserved quantity to available

---

### 9.6 Product Service

> Full doc: [`product-service/docs/PRODUCT-SERVICE-BRD.md`](product-service/docs/PRODUCT-SERVICE-BRD.md)

**Purpose:** Manages the product catalog in MongoDB and enables semantic search via pgvector embeddings. Publicly accessible without authentication.

**Key Functional Requirements:**

| Req ID     | Requirement                                                              |
|------------|--------------------------------------------------------------------------|
| FR-PRS-01  | CRUD for products (ADMIN); public read; filter by category, price range  |
| FR-PRS-02  | Generate vector embedding on save; similarity search returns top-5 results |

**Acceptance Criteria:**
- Search "wireless headset for gaming" returns relevant products not matching exact keywords
- New product embedding appears in pgvector within 5 seconds of save

---

### 9.7 Notification Service

> Full doc: [`notification-service/docs/NOTIFICATION-SERVICE-BRD.md`](notification-service/docs/NOTIFICATION-SERVICE-BRD.md)

**Purpose:** Pure Kafka event consumer that dispatches email notifications. No REST API. Console-only in local profile.

**Key Functional Requirements:**

| Req ID     | Requirement                                                              |
|------------|--------------------------------------------------------------------------|
| FR-NOT-01  | Email on `OrderPlacedEvent`, `PaymentConfirmedEvent`, `OrderShippedEvent`, `OrderCancelledEvent` |
| FR-NOT-02  | Local profile logs to console — no SMTP required                         |

**Acceptance Criteria:**
- `OrderShippedEvent` causes shipment email log entry within 2 seconds locally
- Service restarts cleanly and replays missed events from Kafka offset

---

### 9.8 React UI

> Full doc: [`react-ui/docs/REACT-UI-BRD.md`](react-ui/docs/REACT-UI-BRD.md)

**Purpose:** Primary interface for customers and administrators. Public product catalog, custom auth flow, AI chat for ordering, admin dashboard.

**Key Functional Requirements:**

| Req ID     | Requirement                                                              |
|------------|--------------------------------------------------------------------------|
| FR-UI-01   | Public product grid; semantic search; guest CTA "Sign in to order"       |
| FR-UI-02   | Custom login + registration pages; post-login redirect by role           |
| FR-UI-03   | Streaming chat with tool call chips and inline order confirmation cards  |
| FR-UI-04   | Admin dashboard: order list, agent activity panel, product manager       |

**Acceptance Criteria:**
- Unauthenticated user can browse `/products` without redirect
- Login with `admin@oms.com` redirects to `/admin`, not `/chat`
- Registration creates account and auto-logs in without page reload
- Agent chat shows tool call chip within 500ms of tool execution
