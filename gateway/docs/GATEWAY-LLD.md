# Gateway — Low-Level Design Document

| Field       | Value                              |
|-------------|------------------------------------|
| **Type**    | LLD                                |
| **Scope**   | gateway — Spring Cloud Gateway     |
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
com.oms.gateway/
  config/   — SecurityConfig, RouteConfig
  filter/   — JwtClaimsForwardFilter
```

---

## 2. Key Classes

| Class                  | Type      | Responsibility                                                              |
|------------------------|-----------|-----------------------------------------------------------------------------|
| `SecurityConfig`       | Config    | Defines `SecurityWebFilterChain` with JWT resource server                   |
| `KeycloakJwtConverter` | Converter | Extracts `realm_access.roles` into Spring `GrantedAuthority` list           |
| `JwtClaimsForwardFilter` | Filter  | Forwards parsed JWT claims (userId, email, roles) as request headers        |

---

## 3. Database Schema

No database. Routes are defined in `application.yml` under `spring.cloud.gateway.routes`.

---

## 4. Configuration Properties

| Property                              | Default                    | Description              |
|---------------------------------------|----------------------------|--------------------------|
| `spring.security.oauth2.resourceserver.jwt.issuer-uri` | `http://keycloak:8180/realms/oms` | Keycloak JWKS auto-discovery |
| `spring.cloud.gateway.globalcors.*`   | `http://localhost:3000`    | CORS origin              |

---

## 5. Error Codes

| Code   | HTTP Status | Description                         |
|--------|-------------|-------------------------------------|
| GW-001 | 401         | Missing or invalid JWT              |
| GW-002 | 403         | Insufficient role                   |
| GW-003 | 503         | Downstream service unavailable      |
