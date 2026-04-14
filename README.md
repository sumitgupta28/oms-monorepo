# OMS Monorepo — AI-Powered Order Management System

A learning-oriented, enterprise-grade order management system with Spring AI agents,
MCP tool calling, RAG, Kafka event streaming, and Keycloak auth.

## Quick start

```bash
cp .env.example .env          # add your ANTHROPIC_API_KEY
docker compose up -d          # start everything
open http://localhost:3000    # browse products (no login needed)
```

## Modules

| Module | Technology | Docs |
|--------|-----------|------|
| [gateway](./gateway) | Spring Cloud Gateway | [docs](./gateway/docs) |
| [agent-service](./agent-service) | Spring AI + Claude | [docs](./agent-service/docs) |
| [order-service](./order-service) | Spring Boot + PostgreSQL | [docs](./order-service/docs) |
| [payment-service](./payment-service) | Spring Boot (Mock) | [docs](./payment-service/docs) |
| [inventory-service](./inventory-service) | Spring Boot + PostgreSQL | [docs](./inventory-service/docs) |
| [product-service](./product-service) | Spring Boot + MongoDB + pgvector | [docs](./product-service/docs) |
| [notification-service](./notification-service) | Spring Boot + Kafka | [docs](./notification-service/docs) |
| [react-ui](./react-ui) | React 18 + TypeScript | [docs](./react-ui/docs) |

## Root documentation

| Document | Description |
|----------|-------------|
| [root-docs/OMS-BRD.docx](./root-docs/OMS-BRD.docx) | Business Requirements — full system |
| [root-docs/OMS-HLD.docx](./root-docs/OMS-HLD.docx) | High-Level Design — architecture |
| [root-docs/OMS-LLD.docx](./root-docs/OMS-LLD.docx) | Low-Level Design — cross-cutting concerns |
| [HOW-TO-RUN.md](./HOW-TO-RUN.md) | Setup and run instructions |

## Tech stack

- **Java 21** + **Spring Boot 3.3** + **Gradle 8.14.3**
- **Spring AI 1.x** — Claude (claude-sonnet-4-6) + @Tool MCP
- **Keycloak 24** — Auth, roles, login audit
- **Apache Kafka** — Event-driven saga
- **PostgreSQL 16** + **pgvector** — Orders, payments, embeddings
- **MongoDB 7** — Product catalog
- **Redis 7** — Session cache, ChatMemory
- **React 18** + TypeScript + Tailwind CSS
- **Docker Compose** — Full local stack
