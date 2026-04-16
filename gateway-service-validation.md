# Gateway Service Standalone Validation

This document provides step-by-step instructions to validate the gateway service independently after the Keycloak infrastructure is running.

## Prerequisites

### 1. Start Infrastructure Services

Start the infrastructure stack (Keycloak, PostgreSQL, Redis, Kafka, etc.):

```bash
# From project root
docker compose -f docker-compose.infra.yml up -d
```

Verify all infrastructure services are running:

```bash
docker compose -f docker-compose.infra.yml ps
```

Expected services should include:
- `keycloak` (port 8180)
- `postgres` (port 5432)
- `redis` (port 6379)
- `kafka` (port 9092)
- `mongodb` (port 27017)
- `prometheus` (port 9090)
- `grafana` (port 3001)

### 2. Verify Keycloak is Running

```bash
# Check Keycloak health
curl -s http://localhost:8180/health | jq

# Or open in browser: http://localhost:8180
# Admin Console: http://localhost:8180/admin (admin/admin)
```

Verify the `oms` realm exists:
```bash
curl -s http://localhost:8180/realms/oms/.well-known/openid-configuration | jq
```

### 3. Configure Environment Variables

Ensure environment variables are set for the gateway:

```bash
export KEYCLOAK_URL=http://localhost:8180
export KEYCLOAK_REALM=oms
export AGENT_SERVICE_URI=http://localhost:8085
export ORDER_SERVICE_URI=http://localhost:8081
export PRODUCT_SERVICE_URI=http://localhost:8084
export PAYMENT_SERVICE_URI=http://localhost:8082
export INVENTORY_SERVICE_URI=http://localhost:8083
```

## Build Gateway Service

```bash
# Build the gateway module
./gradlew :gateway:build -x test

# Or with tests
./gradlew :gateway:build
```

## Run Gateway Service

### Option A: Run via Gradle

```bash
./gradlew :gateway:bootRun
```

### Option B: Run the JAR directly

```bash
java -jar gateway/build/libs/gateway-*.jar
```

### Option C: Run via Docker

```bash
docker compose -f docker-compose.apps.yml up -d gateway
docker compose -f docker-compose.apps.yml logs -f gateway
```

## Validation Steps

### 1. Health Check

```bash
# Actuator health endpoint
curl -s http://localhost:8080/actuator/health | jq

# Expected response:
# {
#   "status": "UP"
# }
```

### 2. Prometheus Metrics

```bash
curl -s http://localhost:8080/actuator/prometheus | head -20
```

### 3. Obtain JWT Token from Keycloak

First, get an access token using the test credentials:

```bash
# Get token for customer user
TOKEN_RESPONSE=$(curl -s -X POST "http://localhost:8180/realms/oms/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=react-ui" \
  -d "username=customer@oms.com" \
  -d "password=customer123")

# Extract access token
ACCESS_TOKEN=$(echo $TOKEN_RESPONSE | jq -r .access_token)

echo "Access Token: $ACCESS_TOKEN"
```

For admin user:
```bash
TOKEN_RESPONSE=$(curl -s -X POST "http://localhost:8180/realms/oms/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=react-ui" \
  -d "username=admin@oms.com" \
  -d "password=admin123")

ACCESS_TOKEN=$(echo $TOKEN_RESPONSE | jq -r .access_token)
```

### 4. Test Authentication Enforcement

```bash
# Without token - should return 401 Unauthorized
curl -s -w "\nHTTP Status: %{http_code}\n" http://localhost:8080/api/orders

# Expected: HTTP Status: 401
```

```bash
# With invalid token - should return 401 Unauthorized
curl -s -w "\nHTTP Status: %{http_code}\n" \
  -H "Authorization: Bearer invalid_token" \
  http://localhost:8080/api/orders

# Expected: HTTP Status: 401
```

### 5. Test Token Validation

```bash
# With valid token - should attempt to route (backend may not be available)
curl -s -w "\nHTTP Status: %{http_code}\n" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  http://localhost:8080/api/orders

# Expected: May return 503 (service unavailable) or 502 (bad gateway)
# if downstream services aren't running, but auth should pass
```

### 6. Test Route Configuration

Check if routes are properly configured:

```bash
# Test each route with valid token (downstream service may be unavailable)

# Agent Service route
curl -s -w "\nHTTP Status: %{http_code}\n" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  http://localhost:8080/api/chat/messages

# Product Service route
curl -s -w "\nHTTP Status: %{http_code}\n" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  http://localhost:8080/api/products

# Payment Service route
curl -s -w "\nHTTP Status: %{http_code}\n" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  http://localhost:8080/api/payments

# Inventory Service route
curl -s -w "\nHTTP Status: %{http_code}\n" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  http://localhost:8080/api/inventory
```

### 7. Verify TokenRelay Filter

To verify that TokenRelay is forwarding the JWT to downstream services:

1. Start at least one downstream service (e.g., order-service):
   ```bash
   ./gradlew :order-service:bootRun
   ```

2. Make a request through the gateway:
   ```bash
   curl -v -H "Authorization: Bearer $ACCESS_TOKEN" \
     http://localhost:8080/api/orders 2>&1 | grep -i "authorization"
   ```

3. Check order-service logs to verify the Authorization header was received.

### 8. Test CORS Configuration

```bash
# Preflight request
curl -v -X OPTIONS http://localhost:8080/api/orders \
  -H "Origin: http://localhost:3000" \
  -H "Access-Control-Request-Method: GET" \
  -H "Access-Control-Request-Headers: Authorization, Content-Type"

# Should return headers:
# Access-Control-Allow-Origin: http://localhost:3000
# Access-Control-Allow-Methods: ...
# Access-Control-Allow-Credentials: true
```

```bash
# Test with disallowed origin
curl -v -X OPTIONS http://localhost:8080/api/orders \
  -H "Origin: http://malicious-site.com" \
  -H "Access-Control-Request-Method: GET"

# Should not return CORS headers
```

## Validation with All Services Running

For complete end-to-end validation, start all application services:

```bash
docker compose -f docker-compose.infra.yml up -d
docker compose -f docker-compose.apps.yml up -d
```

Then run the full test suite:

```bash
# Get fresh token
TOKEN_RESPONSE=$(curl -s -X POST "http://localhost:8180/realms/oms/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=react-ui" \
  -d "username=customer@oms.com" \
  -d "password=customer123")

ACCESS_TOKEN=$(echo $TOKEN_RESPONSE | jq -r .access_token)

# Test complete flow through gateway
curl -s -H "Authorization: Bearer $ACCESS_TOKEN" \
  http://localhost:8080/api/products | jq

curl -s -H "Authorization: Bearer $ACCESS_TOKEN" \
  http://localhost:8080/api/orders | jq
```

## Troubleshooting

### Common Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| `401 Unauthorized` with valid token | Keycloak not reachable | Check `KEYCLOAK_URL` env var |
| `TokenRelay` not found | Missing OAuth2 client dependency | Add `spring-boot-starter-oauth2-client` |
| `503 Service Unavailable` | Downstream service not running | Start the target microservice |
| CORS errors in browser | Wrong allowed origin | Update `allowedOrigins` in application.yml |
| `Connection refused` | Wrong service URI | Check `*_SERVICE_URI` env vars |

### Debug Logging

Enable debug logging for gateway routes:

```yaml
# Add to application.yml for debugging
logging:
  level:
    org.springframework.cloud.gateway: DEBUG
    org.springframework.security: DEBUG
    org.springframework.web.server.adapter.HttpWebHandlerAdapter: DEBUG
```

### Verify Token Contents

```bash
# Decode JWT to inspect claims (requires jq)
echo $ACCESS_TOKEN | cut -d. -f2 | base64 -d 2>/dev/null | jq
```

## Summary Checklist

- [ ] Infrastructure services running (`docker compose -f docker-compose.infra.yml ps`)
- [ ] Keycloak accessible at `http://localhost:8180`
- [ ] `oms` realm exists and is configured
- [ ] Gateway builds successfully (`./gradlew :gateway:build`)
- [ ] Gateway starts without errors
- [ ] Health endpoint returns `UP` (`/actuator/health`)
- [ ] Unauthenticated requests return `401`
- [ ] Authenticated requests route correctly
- [ ] CORS configured for frontend origin
- [ ] TokenRelay forwards JWT to downstream services