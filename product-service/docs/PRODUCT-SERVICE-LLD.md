# Product Service — Low-Level Design Document

| Field       | Value                              |
|-------------|------------------------------------|
| **Type**    | LLD                                |
| **Scope**   | product-service (Catalog + RAG)    |
| **Version** | 1.0                                |
| **Date**    | 2025-04-06                         |
| **Status**  | Draft                              |
| **Author**  | OMS Engineering Team               |

---

## Table of Contents

1. [Package Structure](#1-package-structure)
2. [Key Classes](#2-key-classes)
3. [Database Schema](#3-database-schema)
4. [Configuration Properties](#4-configuration-properties)
5. [Error Codes](#5-error-codes)

---

## 1. Package Structure

```
com.oms.product/
  domain/      — Product (@Document — MongoDB)
  repository/  — ProductRepository (MongoRepository)
  service/     — ProductService, EmbeddingService
  controller/  — ProductController
  config/      — VectorStoreConfig, SecurityConfig
```

---

## 2. Key Classes

| Class                    | Type       | Responsibility                                                  |
|--------------------------|------------|-----------------------------------------------------------------|
| `EmbeddingService`       | Service    | Calls Spring AI `EmbeddingClient`, writes to `VectorStore`      |
| `ProductService`         | Service    | CRUD + triggers embedding on save/update                        |
| `VectorStoreConfig`      | Config     | Wires `PgVectorStore` bean with `JdbcTemplate` + `EmbeddingModel` |

---

## 3. Database Schema

**MongoDB** — schema-less document store managed by Spring Data MongoDB.

**PostgreSQL** — pgvector schema managed by Spring AI (`initialize-schema: true`):

```sql
-- Ensured by Flyway V1__init_pgvector.sql
CREATE EXTENSION IF NOT EXISTS vector;

-- Table managed by Spring AI PgVectorStore
CREATE TABLE vector_store (
    id        UUID PRIMARY KEY,
    content   TEXT,
    metadata  JSONB,
    embedding vector(1536)
);
CREATE INDEX ON vector_store USING ivfflat (embedding vector_cosine_ops);
```

---

## 4. Configuration Properties

| Property                                     | Default                              | Description                   |
|----------------------------------------------|--------------------------------------|-------------------------------|
| `spring.data.mongodb.uri`                    | `mongodb://localhost:27017/products` | MongoDB URI                   |
| `spring.ai.vectorstore.pgvector.dimensions`  | `1536`                               | Embedding dimensions          |
| `spring.ai.vectorstore.pgvector.initialize-schema` | `true`                         | Spring AI manages vector_store table |
| `spring.ai.anthropic.embedding.model`        | `text-embedding-3-small`             | Embedding model               |

---

## 5. Error Codes

| Code    | HTTP Status | Description          |
|---------|-------------|----------------------|
| PRS-001 | 404         | Product not found    |
| PRS-002 | 400         | Empty search query   |
