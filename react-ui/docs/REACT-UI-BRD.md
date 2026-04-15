# React UI — Business Requirements Document

| Field       | Value                 |
|-------------|-----------------------|
| **Type**    | BRD                   |
| **Scope**   | react-ui              |
| **Version** | 1.0                   |
| **Date**    | 2025-04-06            |
| **Status**  | Draft                 |
| **Author**  | OMS Engineering Team  |

---

## Table of Contents

1. [Purpose](#1-purpose)
2. [Business Goals](#2-business-goals)
3. [Functional Requirements](#3-functional-requirements)
4. [Non-Functional Requirements](#4-non-functional-requirements)
5. [Acceptance Criteria](#5-acceptance-criteria)

---

## 1. Purpose

The React UI is the **primary interface** for customers and administrators. It provides a public product catalog, a custom login and registration flow backed by Keycloak, an AI-powered chat interface for ordering, and an admin dashboard for order and agent management.

---

## 2. Business Goals

- Allow product browsing without authentication to reduce conversion friction
- Provide a branded custom login and registration page for all roles
- Enable natural language ordering via the agent chat interface
- Give admins visibility into orders, products, and agent tool call activity

---

## 3. Functional Requirements

### FR-UI-01 — Public Product Catalog

- Product grid visible without login
- Search by keyword and semantic query
- Product detail page with price, description, and stock status
- Guest users see "Sign in to order" CTA on product pages

### FR-UI-02 — Authentication

- Custom login page — single page for all roles (`CUSTOMER`, `ADMIN`, `AGENT_MANAGER`)
- Custom registration page — auto-assigns `CUSTOMER` role
- Post-login redirect by role: `CUSTOMER → /chat`, `ADMIN → /admin`
- Persistent session via refresh token stored in `localStorage`
- Auto token refresh 30 seconds before expiry

### FR-UI-03 — Agent Chat

- Streaming chat interface with token-by-token response display
- Tool call chips showing which `@Tool` method Claude invoked
- Order confirmation cards rendered inline in conversation
- Quick action buttons for common tasks

### FR-UI-04 — Admin Dashboard

- Order list with status filter and date range
- Agent activity panel showing tool call counts and success rate
- Product manager for CRUD operations

---

## 4. Non-Functional Requirements

| Attribute          | Requirement                                          |
|--------------------|------------------------------------------------------|
| **Performance**    | First contentful paint < 1.5s on localhost           |
| **Accessibility**  | WCAG 2.1 AA compliant form elements                  |
| **Responsiveness** | Usable on screens 1024px and wider                   |
| **Auth security**  | PKCE flow, no client secret in browser               |

---

## 5. Acceptance Criteria

- Unauthenticated user can browse `/products` without redirect
- Login with `admin@oms.com` redirects to `/admin`, not `/chat`
- Registration creates account and auto-logs in without page reload
- Agent chat shows tool call chip within 500ms of tool execution
