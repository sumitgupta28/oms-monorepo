---
name: Inventory service test patterns
description: Testing patterns, gotchas, and infrastructure details discovered while building inventory-service tests
type: project
---

Key findings from building the inventory-service test suite:

- **MockBean import**: Spring Boot 3.3.0 uses `org.springframework.boot.test.mock.mockito.MockBean`, NOT the deprecated `org.springframework.boot.test.mock.bean.MockBean`.
- **GlobalExceptionHandler vs @PreAuthorize**: The `GlobalExceptionHandler` catches `Exception.class` which swallows Spring Security's `AuthorizationDeniedException` from `@PreAuthorize`, turning expected 403 responses into 500. For controller security tests, exclude GlobalExceptionHandler using `@WebMvcTest(excludeFilters = @ComponentScan.Filter(...))`. Test exception handling (validation, InventoryException) in a separate test class that imports GlobalExceptionHandler.
- **Testcontainers Ryuk**: Docker Hub pull for `testcontainers/ryuk:0.7.0` fails with 403 in this environment. Disable Ryuk via `TESTCONTAINERS_RYUK_DISABLED=true` env var or `~/.testcontainers.properties`. A `testcontainers.properties` in `src/test/resources` was also added.
- **Testcontainers JDBC driver conflict**: When using `@DynamicPropertySource` with an explicit `PostgreSQLContainer`, must also override `spring.datasource.driver-class-name` to `org.postgresql.Driver`, because the test `application.yml` may set it to `org.testcontainers.jdbc.ContainerDatabaseDriver` which rejects standard JDBC URLs.
- **Available Docker image**: Only `postgres:16-alpine` is available locally; `postgres:15-alpine` fails to pull.
- **SecurityConfig pattern**: `/inventory/*` (single wildcard) permits unauthenticated access to single-segment paths like `/inventory/prod-1`, but NOT to `/inventory` (no segment) or `/inventory/prod-1/adjust` (multi-segment). The latter two require authentication.
- **InventoryService requires ReflectionTestUtils**: `lowStockThreshold` is injected via `@Value` and `objectMapper` is a constructor-injected final field. Both need to be set via `ReflectionTestUtils.setField` in `@BeforeEach` when using `@InjectMocks`.

**Why:** These patterns apply to all services in the OMS monorepo that share similar SecurityConfig, GlobalExceptionHandler, and Testcontainers setups.
**How to apply:** When generating tests for other services (order-service, payment-service, etc.), apply the same MockBean import, SecurityConfig exclusion pattern, and Testcontainers configuration.
