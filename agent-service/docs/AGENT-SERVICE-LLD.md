# Agent Service — Low-Level Design Document

| Field       | Value                              |
|-------------|------------------------------------|
| **Type**    | LLD                                |
| **Scope**   | agent-service — Spring AI + MCP    |
| **Version** | 1.0                                |
| **Date**    | 2025-04-06                         |
| **Status**  | Draft                              |
| **Author**  | OMS Engineering Team               |

---

## Table of Contents

1. [Package Structure](#1-package-structure)
2. [Key Classes](#2-key-classes)
3. [Data Model](#3-data-model)
4. [Configuration Properties](#4-configuration-properties)
5. [Error Codes](#5-error-codes)

---

## 1. Package Structure

```
com.oms.agent/
  config/      — AgentConfig (ChatClient bean)
  tools/       — OrderTools, PaymentTools, ProductTools, ValidationTools
  controller/  — ChatController (SSE endpoint)
  client/      — OrderClient, PaymentClient, ProductClient (RestTemplate wrappers)
```

---

## 2. Key Classes

| Class            | Type              | Responsibility                                                    |
|------------------|-------------------|-------------------------------------------------------------------|
| `AgentConfig`    | Config            | Builds `ChatClient` with all `@Tool` beans and Redis `ChatMemory` |
| `OrderTools`     | Service + Tools   | Four `@Tool` methods for order operations                         |
| `ChatController` | Controller        | SSE endpoint — calls `chatClient.stream()`                        |
| `OrderClient`    | Client            | `RestTemplate` wrapper for order-service REST API                 |

---

## 3. Data Model

Redis session storage:

| Property  | Value                              |
|-----------|------------------------------------|
| Key       | `chat:session:{sessionId}`         |
| Structure | `List<ChatMessage>` (JSON)         |
| TTL       | 24 hours                           |

---

## 4. Configuration Properties

| Property                               | Default              | Description                    |
|----------------------------------------|----------------------|--------------------------------|
| `spring.ai.anthropic.api-key`          | `${ANTHROPIC_API_KEY}` | Claude API key               |
| `spring.ai.anthropic.chat.model`       | `claude-sonnet-4-6`  | Model selection                |
| `spring.ai.anthropic.chat.max-tokens`  | `4096`               | Max response tokens            |
| `spring.data.redis.host`               | `redis`              | Redis host inside Docker       |

---

## 5. Error Codes

| Code   | HTTP Status | Description              |
|--------|-------------|--------------------------|
| AS-001 | 503         | Claude API unreachable   |
| AS-002 | 400         | Empty message            |
| AS-003 | 401         | Missing JWT              |
