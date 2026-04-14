# How to Run — OMS Monorepo

## Prerequisites

| Tool | Version | Install |
|------|---------|---------|
| Java JDK | 21+ | https://adoptium.net |
| Docker Desktop | 4.x+ | https://docker.com/desktop |
| Node.js | 18+ | https://nodejs.org |
| Git | any | https://git-scm.com |
| IntelliJ IDEA | 2023.x+ | Community or Ultimate |

---

## 1. Clone and open in IntelliJ

```bash
git clone https://github.com/your-org/oms-monorepo.git
cd oms-monorepo
```

Open IntelliJ → **File → Open** → select the `oms-monorepo` folder.
IntelliJ will detect `settings.gradle` and import all submodules automatically.

---

## 2. Configure environment

```bash
cp .env.example .env
```

Edit `.env` and set your Anthropic API key:

```
ANTHROPIC_API_KEY=sk-ant-your-key-here
```

---

## 3. Start all infrastructure + services

```bash
docker compose up -d
```

First run downloads ~2GB of images. Subsequent runs start in ~20 seconds.

Wait for Keycloak to finish importing the realm (~30s):

```bash
docker compose logs -f keycloak | grep "Keycloak.*started"
```

---

## 4. Verify all services are healthy

```bash
docker compose ps
```

All services should show `healthy` or `running`.

| Service | URL | Notes |
|---------|-----|-------|
| React UI | http://localhost:3000 | Main entry point |
| API Gateway | http://localhost:8080 | All API traffic goes here |
| Keycloak Admin | http://localhost:8180/admin | admin / admin |
| Kafdrop (Kafka UI) | http://localhost:9000 | View topics and messages |
| Grafana | http://localhost:3001 | admin / admin |

---

## 5. First login

Open http://localhost:3000 — you'll see the product catalog without logging in.

Click **Register** to create a customer account, or use the seeded test accounts:

| Email | Password | Role |
|-------|----------|------|
| customer@oms.com | customer123 | CUSTOMER |
| admin@oms.com | admin123 | ADMIN |

---

## 6. Run from IntelliJ (individual services, for development)

Each service can be run directly from IntelliJ for faster iteration:

1. Open the Gradle tool window (View → Tool Windows → Gradle)
2. Expand any submodule → Tasks → application → **bootRun**
3. Or use the run configuration created automatically by IntelliJ

Make sure Docker Compose is running first (for PostgreSQL, Kafka, Redis, etc.).

---

## 7. Build all modules

```bash
./gradlew build
```

Build a single module:

```bash
./gradlew :order-service:build
```

---

## 8. Run tests

```bash
./gradlew test                        # all modules
./gradlew :order-service:test         # single module
```

Tests use Testcontainers — Docker must be running.

---

## 9. Stop everything

```bash
docker compose down          # stop containers, keep data volumes
docker compose down -v       # stop and delete all data (fresh start)
```

---

## Module port reference

| Module | Port |
|--------|------|
| react-ui | 3000 |
| gateway | 8080 |
| order-service | 8081 |
| payment-service | 8082 |
| inventory-service | 8083 |
| product-service | 8084 |
| agent-service | 8085 |
| notification-service | 8086 |
| keycloak | 8180 |
| postgres | 5432 |
| mongodb | 27017 |
| redis | 6379 |
| kafka | 9092 |
| kafdrop | 9000 |
| grafana | 3001 |
| prometheus | 9090 |
