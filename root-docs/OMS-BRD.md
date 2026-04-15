# Order Management System — Business Requirements Document

| Field       | Value              |
|-------------|--------------------|
| **Type**    | BRD                |
| **Scope**   | Root — All Modules |
| **Version** | 1.0                |
| **Date**    | 2025-04-06         |
| **Status**  | Draft              |
| **Author**  | OMS Engineering Team |

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Business Objectives](#2-business-objectives)
3. [Scope](#3-scope)
4. [Stakeholders](#4-stakeholders)
5. [Functional Requirements](#5-functional-requirements)
6. [Non-Functional Requirements](#6-non-functional-requirements)
7. [Constraints](#7-constraints)
8. [Glossary](#8-glossary)

---

## 1. Executive Summary

The Order Management System (OMS) is an enterprise-grade, AI-powered platform that enables customers to browse products, place orders, make payments, and track fulfilment through a conversational AI interface. Administrators manage the product catalog, monitor orders, and observe AI agent activity in real time.

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

| Role              | Responsibility                              | System Access                         |
|-------------------|---------------------------------------------|---------------------------------------|
| **Customer**      | Browse, order, pay, track                   | React UI — Chat + Orders              |
| **Admin**         | Manage orders, products, users              | React UI — Admin Dashboard            |
| **Agent Manager** | Monitor agent calls and logs                | React UI — Agent Logs                 |
| **Developer**     | Build, maintain, extend the system          | Full stack + Docker Compose           |

---

## 5. Functional Requirements

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
- Four agents: Order, Payment, Validation, Product
- Tool calls visible in React UI for learning/debugging

---

## 6. Non-Functional Requirements

| Attribute       | Requirement                                | Notes                        |
|-----------------|--------------------------------------------|------------------------------|
| **Performance** | API response < 500ms p95 (ex-LLM)          | LLM latency ~2–5s expected   |
| **Availability**| 99% local uptime                           | Docker Compose restart policies |
| **Security**    | JWT on all protected endpoints             | Keycloak PKCE flow           |
| **Observability**| Prometheus + Grafana metrics              | Spring Actuator on all services |
| **Scalability** | Stateless services, Kafka for async        | Redis session store          |
| **Auditability**| Full login event log                       | Keycloak Events API          |

---

## 7. Constraints

- Java 21 with Gradle 8.14.3 for all Spring Boot modules
- Docker Compose only — no Kubernetes for local development
- Anthropic API key required for Claude LLM calls
- Single Git repository — IntelliJ multi-module project structure

---

## 8. Glossary

| Term        | Definition                                                                             |
|-------------|----------------------------------------------------------------------------------------|
| **MCP**     | Model Context Protocol — Spring AI `@Tool` methods exposed to Claude                 |
| **RAG**     | Retrieval-Augmented Generation — pgvector similarity search + LLM                    |
| **Saga**    | Distributed transaction pattern using Kafka events                                    |
| **Outbox**  | Transactional pattern ensuring event publishing reliability                           |
| **JWT**     | JSON Web Token — signed bearer token issued by Keycloak                               |
