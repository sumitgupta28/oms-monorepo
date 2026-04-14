# How to Run — order-service

## Prerequisites
Docker Compose must be running (see root HOW-TO-RUN.md).

## Run via Docker Compose (recommended)
```bash
docker compose up -d order-service
```

## Run via IntelliJ
1. Gradle tool window → order-service → Tasks → application → bootRun
2. Or: right-click `src/main/java/.../Application.java` → Run

## Run via Gradle CLI
```bash
./gradlew :order-service:bootRun
```

## Build JAR
```bash
./gradlew :order-service:build
```

## Run tests
```bash
./gradlew :order-service:test
```

## Health check
```bash
curl http://localhost:$(grep 'order-service' docker-compose.yml | grep -o '[0-9]*:' | head -1 | tr -d ':' || echo '808x')/actuator/health
```

## Docs
| Document | Description |
|----------|-------------|
| [BRD](./ORDER_SERVICE-BRD.docx) | Business Requirements |
| [HLD](./ORDER_SERVICE-HLD.docx) | High-Level Design |
| [LLD](./ORDER_SERVICE-LLD.docx) | Low-Level Design |
