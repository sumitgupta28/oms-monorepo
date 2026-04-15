# Product Service — High-Level Design Document

| Field       | Value                              |
|-------------|------------------------------------|
| **Type**    | HLD                                |
| **Scope**   | product-service (Catalog + RAG)    |
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

Spring Boot 3.3 + **Spring Data MongoDB** (catalog) + **Spring AI EmbeddingClient** (Anthropic) + **pgvector** via Spring AI `VectorStore`. Products saved in MongoDB; embeddings stored in PostgreSQL pgvector.

---

## 2. API Endpoints

| Method | Path                 | Auth      | Description                         |
|--------|----------------------|-----------|-------------------------------------|
| GET    | `/products`          | None      | List all products (paginated)       |
| GET    | `/products/{id}`     | None      | Get product detail                  |
| GET    | `/products/search`   | None      | Semantic search by query string     |
| POST   | `/products`          | ADMIN JWT | Create product + generate embedding |
| PUT    | `/products/{id}`     | ADMIN JWT | Update product + re-embed           |
| DELETE | `/products/{id}`     | ADMIN JWT | Delete product                      |

---

## 3. Dependencies

| Dependency                | Type          | Purpose                                      |
|---------------------------|---------------|----------------------------------------------|
| MongoDB                   | Internal      | Product document store                       |
| PostgreSQL + pgvector     | Internal      | Embedding vector store                       |
| Anthropic Embeddings API  | External      | Generate `text-embedding-3-small` vectors    |
| Kafka                     | Internal      | `ProductUpdatedEvent` for embedding refresh  |

---

## 4. Kafka Events

| Topic                  | Direction | Event Type            | Notes                    |
|------------------------|-----------|-----------------------|--------------------------|
| `oms.products.updated` | Produces  | `ProductUpdatedEvent` | Triggers re-embedding    |

---

## 5. Data Model

**MongoDB** (`products` collection):

```json
{
  "id": "string",
  "name": "string",
  "description": "string",
  "category": "string",
  "price": 0.00,
  "stockQty": 0,
  "imageUrl": "string",
  "active": true,
  "createdAt": "instant",
  "updatedAt": "instant"
}
```

**PostgreSQL** (`vector_store` table — managed by Spring AI):

```sql
CREATE EXTENSION IF NOT EXISTS vector;
CREATE TABLE vector_store (
    id        UUID PRIMARY KEY,
    content   TEXT,
    metadata  JSONB,
    embedding vector(1536)
);
CREATE INDEX ON vector_store USING ivfflat (embedding vector_cosine_ops);
```

---

## 6. Security

- `GET` endpoints are public (`permitAll` in Gateway)
- `POST` / `PUT` / `DELETE` require `ADMIN` JWT
