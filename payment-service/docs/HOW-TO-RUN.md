# How to Run — payment-service

## Prerequisites
Docker Compose must be running (see root HOW-TO-RUN.md).

## Run via Docker Compose (recommended)
```bash
docker compose up -d payment-service
```

## Run via IntelliJ
1. Gradle tool window → payment-service → Tasks → application → bootRun
2. Or: right-click `src/main/java/.../Application.java` → Run

## Run via Gradle CLI
```bash
./gradlew :payment-service:bootRun
```

## Build JAR
```bash
./gradlew :payment-service:build
```

## Run tests
```bash
./gradlew :payment-service:test
```

## Health check
```bash
curl http://localhost:$(grep 'payment-service' docker-compose.yml | grep -o '[0-9]*:' | head -1 | tr -d ':' || echo '808x')/actuator/health
```

## Docs
| Document | Description |
|----------|-------------|
| [BRD](./PAYMENT_SERVICE-BRD.docx) | Business Requirements |
| [HLD](./PAYMENT_SERVICE-HLD.docx) | High-Level Design |
| [LLD](./PAYMENT_SERVICE-LLD.docx) | Low-Level Design |
