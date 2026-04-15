# Agent Service — High-Level Design Document

| Field       | Value                              |
|-------------|------------------------------------|
| **Type**    | HLD                                |
| **Scope**   | agent-service — Spring AI + MCP    |
| **Version** | 1.0                                |
| **Date**    | 2025-04-06                         |
| **Status**  | Draft                              |
| **Author**  | OMS Engineering Team               |

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

Spring Boot 3.3 + Spring AI 1.x. `ChatClient` with registered `@Tool` beans. Redis `ChatMemory` for multi-turn history. SSE endpoint for token streaming. Calls downstream services via `RestTemplate`.

---

## 2. API Endpoints

| Method | Path                  | Auth         | Description                          |
|--------|-----------------------|--------------|--------------------------------------|
| GET    | `/api/chat/stream`    | CUSTOMER JWT | SSE stream — chat with agents        |
| DELETE | `/api/chat/session`   | CUSTOMER JWT | Clear conversation history           |
| GET    | `/actuator/health`    | None         | Health probe                         |

---

## 3. Dependencies

| Dependency       | Type          | Purpose                                  |
|------------------|---------------|------------------------------------------|
| Claude API       | External HTTPS| LLM inference and tool calling           |
| order-service    | Internal REST | Order CRUD tools                         |
| payment-service  | Internal REST | Payment tools                            |
| product-service  | Internal REST | Product search + RAG                     |
| Redis            | Internal      | `ChatMemory` session store               |

---

## 4. Kafka Events

Agent Service does not produce or consume Kafka events directly. Tool calls are synchronous REST calls to downstream services.

---

## 5. Data Model

No SQL database. Conversation history stored in **Redis** as serialised message lists, keyed by `sessionId`.

- Key pattern: `chat:session:{sessionId}`
- Structure: `List<ChatMessage>` serialised as JSON
- TTL: 24 hours

---

## 6. Security

- Requires `CUSTOMER` JWT on `/api/chat/stream`
- JWT subject (`userId`) passed to downstream tool calls
- `ANTHROPIC_API_KEY` loaded from environment variable — never hardcoded
