# How to Run — gateway

## Prerequisites
Docker Compose must be running (see root HOW-TO-RUN.md).

## Run via Docker Compose (recommended)
```bash
docker compose up -d gateway
```

## Run via IntelliJ
1. Gradle tool window → gateway → Tasks → application → bootRun
2. Or: right-click `src/main/java/.../Application.java` → Run

## Run via Gradle CLI
```bash
./gradlew :gateway:bootRun
```

## Build JAR
```bash
./gradlew :gateway:build
```

## Run tests
```bash
./gradlew :gateway:test
```

## Health check
```bash
curl http://localhost:$(grep 'gateway' docker-compose.yml | grep -o '[0-9]*:' | head -1 | tr -d ':' || echo '808x')/actuator/health
```

## Docs
| Document | Description |
|----------|-------------|
| [BRD](./GATEWAY-BRD.docx) | Business Requirements |
| [HLD](./GATEWAY-HLD.docx) | High-Level Design |
| [LLD](./GATEWAY-LLD.docx) | Low-Level Design |
