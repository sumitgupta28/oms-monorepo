# Agent Service — Business Requirements Document

| Field       | Value                              |
|-------------|------------------------------------|
| **Type**    | BRD                                |
| **Scope**   | agent-service — Spring AI + MCP    |
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

The Agent Service hosts all AI agents powered by **Claude** via Spring AI. It exposes `@Tool`-annotated MCP tools for order, payment, validation, and product operations. It manages multi-turn conversation memory in Redis.

---

## 2. Business Goals

- Enable natural language order placement and management
- Expose MCP tools callable by Claude
- Maintain conversation context across turns
- Surface tool call activity to the UI for transparency

---

## 3. Functional Requirements

### FR-AS-01 — Chat Streaming

- Stream Claude responses token-by-token via **SSE**
- Emit tool-call events alongside text tokens
- Support session-based conversation history

### FR-AS-02 — MCP Tools

| Tool Method             | Description                                  |
|-------------------------|----------------------------------------------|
| `placeOrder(userId, items)` | Calls order-service to create an order    |
| `trackOrder(orderId)`   | Returns current order status                 |
| `cancelOrder(orderId)`  | Cancels order if it is in an eligible state  |
| `initiatePayment(orderId)` | Calls payment-service                    |
| `searchProducts(query)` | RAG semantic product search                  |
| `checkInventory(productId, qty)` | Checks available stock              |

---

## 4. Non-Functional Requirements

| Attribute            | Requirement                         |
|----------------------|-------------------------------------|
| **LLM latency**      | < 10s p95                           |
| **Memory retention** | Last 20 turns per session           |
| **Concurrent sessions** | 50+ locally                      |

---

## 5. Acceptance Criteria

- User sends "order laptop" → agent calls `placeOrder` tool automatically
- Tool call names visible in SSE stream
- Session context persists across page reload via Redis
