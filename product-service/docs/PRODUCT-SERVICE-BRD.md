# Product Service — Business Requirements Document

| Field       | Value                              |
|-------------|------------------------------------|
| **Type**    | BRD                                |
| **Scope**   | product-service (Catalog + RAG)    |
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

The Product Service manages the product catalog stored in **MongoDB** and enables semantic search via **pgvector** embeddings. It is publicly accessible for browsing without authentication.

---

## 2. Business Goals

- Serve product catalog to authenticated and guest users
- Enable semantic search for the Product Agent using RAG
- Generate and store vector embeddings for all products
- Expose product data to the React UI and agent tools

---

## 3. Functional Requirements

### FR-PRS-01 — Catalog

- CRUD for products (`ADMIN` role)
- Public read — no JWT required
- Filter by category, price range, stock availability

### FR-PRS-02 — RAG Search

- Embedding job generates vector for each product on save
- Similarity search endpoint accepts natural language query
- Returns top-5 semantically similar products
- pgvector cosine distance operator `<=>`

---

## 4. Non-Functional Requirements

| Attribute              | Requirement                                    |
|------------------------|------------------------------------------------|
| **Search latency**     | < 200ms for vector similarity search           |
| **Embedding freshness**| Re-embed on product update                     |

---

## 5. Acceptance Criteria

- Search "wireless headset for gaming" returns relevant products not matching exact keywords
- New product embedding appears in pgvector within 5 seconds of save
