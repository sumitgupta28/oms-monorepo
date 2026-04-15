# React UI — High-Level Design Document

| Field       | Value                 |
|-------------|-----------------------|
| **Type**    | HLD                   |
| **Scope**   | react-ui              |
| **Version** | 1.0                   |
| **Date**    | 2025-04-06            |
| **Status**  | Draft                 |
| **Author**  | OMS Engineering Team  |

---

## Table of Contents

1. [Overview](#1-overview)
2. [Page Map](#2-page-map)
3. [Key Dependencies](#3-key-dependencies)
4. [Auth Flow](#4-auth-flow)
5. [Streaming Architecture](#5-streaming-architecture)
6. [Security](#6-security)

---

## 1. Overview

**React 18 + TypeScript + Tailwind CSS + React Router v6.** Auth via `keycloak-js` calling Keycloak Direct Grant API. Server-Sent Events for streaming agent responses. React Query for order and product data fetching with caching.

---

## 2. Page Map

| Route               | Auth Required | Role                      | Component             |
|---------------------|---------------|---------------------------|-----------------------|
| `/`                 | No            | Any                       | Redirects to `/products` |
| `/products`         | No            | Any                       | ProductCatalogPage    |
| `/products/:id`     | No            | Any                       | ProductDetailPage     |
| `/login`            | No            | Any                       | LoginPage             |
| `/register`         | No            | Any                       | RegisterPage          |
| `/chat`             | Yes           | CUSTOMER                  | ChatPage              |
| `/orders`           | Yes           | CUSTOMER                  | MyOrdersPage          |
| `/admin`            | Yes           | ADMIN                     | AdminDashboard        |
| `/admin/agent-logs` | Yes           | ADMIN or AGENT_MANAGER    | AgentLogsPage         |

---

## 3. Key Dependencies

| Library                 | Version | Purpose                                    |
|-------------------------|---------|--------------------------------------------|
| `react`                 | 18.x    | UI rendering                               |
| `react-router-dom`      | 6.x     | Client-side routing                        |
| `@tanstack/react-query` | 5.x     | Server state, caching, refetch             |
| `keycloak-js`           | 24.x    | Keycloak Direct Grant auth                 |
| `axios`                 | 1.x     | HTTP client with JWT interceptor           |
| `tailwindcss`           | 3.x     | Utility CSS                                |
| `jwt-decode`            | 4.x     | Parse JWT claims without validation        |

---

## 4. Auth Flow

```
User → /login → Custom React form → POST to Keycloak token endpoint
             → Access token + refresh token stored in localStorage
             → axios interceptor attaches Bearer token to every API call
             → Auto-refresh fires 30s before access token expiry
             → On 401 → attempt refresh → if fails → redirect to /login
```

---

## 5. Streaming Architecture

Chat uses native `EventSource` API connecting to `agent-service` SSE endpoint:

1. Tokens append to the last message in state as they arrive
2. Tool call events appear as teal chips above the message bubble
3. `EventSource` closes on `done` event

---

## 6. Security

- **PKCE** code challenge used with `react-ui` Keycloak client
- No client secret in browser code
- `ProtectedRoute` component redirects unauthenticated users to `/login`
- Role check in `ProtectedLayout` renders 403 page for insufficient role
