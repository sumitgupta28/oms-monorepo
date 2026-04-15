# React UI — Low-Level Design Document

| Field       | Value                 |
|-------------|-----------------------|
| **Type**    | LLD                   |
| **Scope**   | react-ui              |
| **Version** | 1.0                   |
| **Date**    | 2025-04-06            |
| **Status**  | Draft                 |
| **Author**  | OMS Engineering Team  |

---

## Table of Contents

1. [Directory Structure](#1-directory-structure)
2. [Key Components](#2-key-components)
3. [State Management](#3-state-management)
4. [Environment Variables](#4-environment-variables)
5. [Build and Run](#5-build-and-run)

---

## 1. Directory Structure

```
react-ui/src/
├── auth/
│   ├── authApi.ts
│   ├── AuthContext.tsx
│   ├── useAuth.ts
│   └── usePostLoginRedirect.ts
├── api/
│   ├── axiosClient.ts
│   ├── orderApi.ts
│   ├── productApi.ts
│   └── paymentApi.ts
├── layouts/
│   ├── PublicLayout.tsx
│   └── ProtectedLayout.tsx
├── pages/
│   ├── auth/
│   │   ├── LoginPage.tsx
│   │   └── RegisterPage.tsx
│   ├── products/
│   │   ├── ProductCatalogPage.tsx
│   │   └── ProductDetailPage.tsx
│   ├── chat/
│   │   └── ChatPage.tsx
│   └── admin/
│       ├── AdminDashboard.tsx
│       └── AgentLogsPage.tsx
├── components/
│   ├── chat/
│   │   ├── MessageBubble.tsx
│   │   ├── ToolCallChip.tsx
│   │   └── OrderConfirmCard.tsx
│   ├── products/
│   │   ├── ProductCard.tsx
│   │   ├── ProductGrid.tsx
│   │   └── SearchBar.tsx
│   └── common/
│       ├── Navbar.tsx
│       ├── LoadingSpinner.tsx
│       └── ErrorBanner.tsx
└── hooks/
    ├── useAgentStream.ts
    └── useOrderHistory.ts
```

---

## 2. Key Components

| Component          | Type      | Responsibility                                                          |
|--------------------|-----------|-------------------------------------------------------------------------|
| `AuthContext`      | Context   | Holds `user`, `tokens`, `login()`, `logout()`, `hasRole()`             |
| `ProtectedLayout`  | Layout    | Redirects to `/login` if not authenticated; 403 if wrong role           |
| `useAgentStream`   | Hook      | `EventSource` connection, token accumulation, tool-call events          |
| `MessageBubble`    | Component | Renders user or agent message bubble with streaming text                |
| `ToolCallChip`     | Component | Small teal badge showing `@Tool` method name                            |
| `OrderConfirmCard` | Component | Inline order summary card rendered in agent response                    |
| `axiosClient`      | Utility   | Axios instance with JWT interceptor and auto-refresh                    |

---

## 3. State Management

| Concern             | Solution                              |
|---------------------|---------------------------------------|
| Auth state          | `AuthContext` (React Context)         |
| Server data         | React Query (`@tanstack/react-query`) |
| Chat conversation   | `useAgentStream` local state          |
| Cross-component     | No Redux — props + context only       |

---

## 4. Environment Variables

| Variable                       | Example                        | Purpose                     |
|--------------------------------|--------------------------------|-----------------------------|
| `REACT_APP_KEYCLOAK_URL`       | `http://localhost:8180`        | Keycloak base URL           |
| `REACT_APP_KEYCLOAK_REALM`     | `oms`                          | Realm name                  |
| `REACT_APP_KEYCLOAK_CLIENT_ID` | `react-ui`                     | OAuth2 client ID            |
| `REACT_APP_GATEWAY_URL`        | `http://localhost:8080`        | API gateway base URL        |

---

## 5. Build and Run

```bash
npm install        # install dependencies
npm start          # development server on localhost:3000
npm run build      # production build to dist/
npm test           # Jest + React Testing Library
```
