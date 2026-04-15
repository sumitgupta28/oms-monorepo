# Gateway — High-Level Design Document

| Field       | Value                              |
|-------------|------------------------------------|
| **Type**    | HLD                                |
| **Scope**   | gateway — Spring Cloud Gateway     |
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

Spring Cloud Gateway 2023.x reactive gateway using **WebFlux**. JWT validation via `spring-security-oauth2-resource-server`. Keycloak JWKS endpoint polled automatically on startup and cached.

---

## 2. API Endpoints

| Method | Path             | Auth | Description                   |
|--------|------------------|------|-------------------------------|
| ANY    | `/api/**`        | JWT (except `/api/products`) | Routes to downstream service |
| GET    | `/actuator/health` | None | Health probe                |

---

## 3. Dependencies

| Dependency        | Type            | Purpose                                        |
|-------------------|-----------------|------------------------------------------------|
| Keycloak          | External        | JWKS key fetch for JWT validation              |
| All microservices | Internal Docker | Route targets                                  |

---

## 4. Kafka Events

Gateway does not produce or consume Kafka events. It is stateless and synchronous.

---

## 5. Data Model

Gateway is **stateless** — no database. Route configuration is defined in `application.yml` under `spring.cloud.gateway.routes`.

---

## 6. Security

- JWT validation on all routes except `/api/products/**` and `/actuator/health`
- PKCE enforcement for `react-ui` Keycloak client
- Rate limiting via Redis (optional — Phase 6 enhancement)
- `TokenRelay` filter forwards the validated JWT to every downstream service
