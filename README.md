# OMS Monorepo — AI-Powered Order Management System

A learning-oriented, enterprise-grade order management system with Spring AI agents,
MCP tool calling, RAG, Kafka event streaming, and Keycloak auth.

## Quick start

```bash
cp .env.example .env          # configure AI provider (see below)
docker compose up -d          # start everything
open http://localhost:3000    # browse products (no login needed)
```

## AI Provider — Anthropic vs Ollama

The `agent-service` supports two AI backends, switchable via the `AI_PROVIDER` env var.
No code changes or rebuilds are needed to switch.

### Option A — Anthropic (Claude) `AI_PROVIDER=anthropic`

Requires an API key from [console.anthropic.com](https://console.anthropic.com).

```bash
# .env
AI_PROVIDER=anthropic
ANTHROPIC_API_KEY=sk-ant-...

# Optional overrides
AI_MODEL=claude-sonnet-4-6    # default
AI_MAX_TOKENS=4096             # default
```

Start normally:

```bash
docker compose -f docker-compose.infra.yml up -d
docker compose -f docker-compose.apps.yml up -d
```

### Option B — Ollama (local, no API key) `AI_PROVIDER=ollama`

Runs a local LLM. Free, offline, no external account needed.

**Step 1 — Install Ollama**

Download from [ollama.com](https://ollama.com) and pull a model:

```bash
ollama pull llama3.2           # ~2 GB, good default
# or
ollama pull mistral            # ~4 GB, stronger reasoning
```

**Step 2 — Configure `.env`**

```bash
AI_PROVIDER=ollama
# ANTHROPIC_API_KEY not needed

# Optional overrides
AI_MODEL=llama3.2              # default for Ollama
AI_MAX_TOKENS=4096             # default
OLLAMA_BASE_URL=http://localhost:11434   # if running Ollama locally (not in Docker)
```

**Step 3 — Start with the dedicated Ollama compose file**

```bash
docker compose -f docker-compose.infra.yml up -d
docker compose -f docker-compose.apps.yml -f docker-compose.ollama.yml up -d

# First time only — pull the model inside the container:
docker exec ollama ollama pull llama3.2
```

Or run Ollama on your host machine (already started via `ollama serve`) and point to it:

```bash
OLLAMA_BASE_URL=http://host.docker.internal:11434
docker compose -f docker-compose.apps.yml up -d agent-service
```

### AI env var reference

| Variable | Default | Description |
|---|---|---|
| `AI_PROVIDER` | `anthropic` | `anthropic` or `ollama` |
| `AI_MODEL` | provider default | Model name override |
| `AI_MAX_TOKENS` | `4096` | Token limit per response |
| `ANTHROPIC_API_KEY` | — | Required for Anthropic only |
| `OLLAMA_BASE_URL` | `http://localhost:11434` | Ollama server URL |

### Model recommendations

| Use case | Anthropic | Ollama |
|---|---|---|
| Best quality | `claude-opus-4-7` | `mistral` / `llama3.1` |
| Balanced (default) | `claude-sonnet-4-6` | `llama3.2` |
| Fastest / smallest | `claude-haiku-4-5-20251001` | `phi3` |

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
- **Spring AI 1.x** — Claude (Anthropic) or Ollama (local LLM) + @Tool MCP
- **Keycloak 24** — Auth, roles, login audit
- **Apache Kafka** — Event-driven saga
- **PostgreSQL 16** + **pgvector** — Orders, payments, embeddings
- **MongoDB 7** — Product catalog
- **Redis 7** — Session cache, ChatMemory
- **React 18** + TypeScript + Tailwind CSS
- **Docker Compose** — Full local stack
