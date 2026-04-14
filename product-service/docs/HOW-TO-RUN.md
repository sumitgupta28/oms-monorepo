# How to Run — product-service

## Prerequisites
Docker Compose must be running (see root HOW-TO-RUN.md).

## Run via Docker Compose (recommended)
```bash
docker compose up -d product-service
```

## Run via IntelliJ
1. Gradle tool window → product-service → Tasks → application → bootRun
2. Or: right-click `src/main/java/.../Application.java` → Run

## Run via Gradle CLI
```bash
./gradlew :product-service:bootRun
```

## Build JAR
```bash
./gradlew :product-service:build
```

## Run tests
```bash
./gradlew :product-service:test
```

## Health check
```bash
curl http://localhost:$(grep 'product-service' docker-compose.yml | grep -o '[0-9]*:' | head -1 | tr -d ':' || echo '808x')/actuator/health
```

## Docs
| Document | Description |
|----------|-------------|
| [BRD](./PRODUCT_SERVICE-BRD.docx) | Business Requirements |
| [HLD](./PRODUCT_SERVICE-HLD.docx) | High-Level Design |
| [LLD](./PRODUCT_SERVICE-LLD.docx) | Low-Level Design |
