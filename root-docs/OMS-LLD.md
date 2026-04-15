# Order Management System — Low-Level Design Document

| Field       | Value                           |
|-------------|---------------------------------|
| **Type**    | LLD                             |
| **Scope**   | Root — Cross-Cutting Concerns   |
| **Version** | 1.0                             |
| **Date**    | 2025-04-06                      |
| **Status**  | Draft                           |
| **Author**  | OMS Engineering Team            |

---

## Table of Contents

1. [Monorepo Structure](#1-monorepo-structure)
2. [Shared Conventions](#2-shared-conventions)
3. [JWT Token Structure](#3-jwt-token-structure)
4. [Gradle 8.14.3 + Java 21 Setup](#4-gradle-8143--java-21-setup)
5. [Docker Compose Network](#5-docker-compose-network)
6. [Environment Variables](#6-environment-variables)

---

## 1. Monorepo Structure

The repository is structured as a Gradle multi-module project. Each Spring Boot service is a submodule. The root `build.gradle` defines shared plugins, Java 21 toolchain, and common dependencies.

### 1.1 Directory Layout

```
oms-monorepo/                     ← root IntelliJ project
├── build.gradle                  ← shared plugin + dep config
├── settings.gradle               ← submodule declarations
├── gradle/wrapper/               ← Gradle 8.14.3
├── docker-compose.yml
├── docker-compose.infra.yml
├── docker-compose.apps.yml
├── .env.example
├── root-docs/                    ← BRD, HLD, LLD for full system
├── gateway/                      ← Spring Cloud Gateway submodule
│   ├── build.gradle
│   ├── src/
│   └── docs/                     ← BRD, HLD, LLD, HOW-TO-RUN.md
├── agent-service/
├── order-service/
├── payment-service/
├── inventory-service/
├── product-service/
├── notification-service/
├── react-ui/                     ← React + TypeScript (npm)
│   └── docs/
└── keycloak/realms/              ← oms-realm.json auto-import
```

---

## 2. Shared Conventions

### 2.1 Package Structure (per service)

```
com.oms.<service>/
  controller  — REST endpoints
  service     — business logic
  repository  — Spring Data interfaces
  domain      — JPA entities / MongoDB documents
  event       — Kafka event records (shared via root)
  config      — Spring configuration beans
  exception   — custom exceptions + global handler
```

### 2.2 Error Handling

Every service has a `@RestControllerAdvice` that maps domain exceptions to **RFC 7807 `ProblemDetail`** responses. HTTP status codes follow REST conventions:

| Status | Meaning       |
|--------|---------------|
| 400    | Validation error |
| 401    | Unauthorized  |
| 403    | Forbidden     |
| 404    | Not found     |
| 409    | Conflict      |
| 500    | Internal error|

### 2.3 Kafka Event Schema

All Kafka events are Java records in the shared events package, serialised as JSON:

```java
record DomainEvent(
    UUID   correlationId,  // traces a full order journey across services
    String eventType,      // e.g. ORDER_PLACED
    Instant timestamp,
    Object payload         // service-specific nested record
) {}
```

---

## 3. JWT Token Structure

Keycloak issues access tokens containing:

| Claim          | Type           | Example                |
|----------------|----------------|------------------------|
| `sub`          | UUID string    | `a1b2c3d4-...`         |
| `email`        | String         | `user@example.com`     |
| `given_name`   | String         | `Alex`                 |
| `family_name`  | String         | `Chen`                 |
| `realm_access` | Object         | `{"roles":["CUSTOMER"]}` |
| `exp`          | Unix timestamp | `1700000000`           |

---

## 4. Gradle 8.14.3 + Java 21 Setup

### 4.1 Root `build.gradle` Key Config

```groovy
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}
```

This ensures all submodules compile with Java 21 regardless of the developer's local JDK.

The Spring Boot BOM and Spring Cloud BOM are imported in `dependencyManagement` at root level, so no submodule needs to declare Spring versions.

### 4.2 Wrapper Configuration

`gradle/wrapper/gradle-wrapper.properties` pins `distributionUrl` to `gradle-8.14.3-bin.zip`.

```bash
./gradlew wrapper --gradle-version 8.14.3   # regenerate if needed
```

---

## 5. Docker Compose Network

All containers share a single bridge network called **`oms-network`**. Services address each other by container name, e.g. `http://order-service:8081`. The Anthropic API is the only external call that leaves the Docker network.

Two compose files allow independent lifecycle management:

```bash
# Infrastructure only (PostgreSQL, MongoDB, Redis, Kafka, Keycloak, Prometheus, Grafana)
docker compose -f docker-compose.infra.yml up -d

# Applications only (all Spring Boot services + React UI)
docker compose -f docker-compose.apps.yml up -d
```

> Always start infra before apps. Apps join `oms-network` as an external network.

---

## 6. Environment Variables

| Variable                   | Used By                       | Example Value   |
|----------------------------|-------------------------------|-----------------|
| `ANTHROPIC_API_KEY`        | agent-service, product-service | `sk-ant-...`   |
| `KEYCLOAK_ADMIN`           | keycloak                      | `admin`         |
| `KEYCLOAK_ADMIN_PASSWORD`  | keycloak                      | `admin`         |
| `POSTGRES_PASSWORD`        | postgres                      | `postgres`      |
| `SPRING_PROFILES_ACTIVE`   | all services                  | `local`         |

Copy `.env.example` to `.env` and fill in `ANTHROPIC_API_KEY` before running `docker compose up`.
