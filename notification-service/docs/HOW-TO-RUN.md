# How to Run — notification-service

## Prerequisites
Docker Compose must be running (see root HOW-TO-RUN.md).

## Run via Docker Compose (recommended)
```bash
docker compose up -d notification-service
```

## Run via IntelliJ
1. Gradle tool window → notification-service → Tasks → application → bootRun
2. Or right-click the Application.java main class → Run

## Run via Gradle CLI
```bash
./gradlew :notification-service:bootRun
```

## Build JAR
```bash
./gradlew :notification-service:build
```

## Run tests
```bash
./gradlew :notification-service:test
```

## Health check
```bash
curl http://localhost:808x/actuator/health
```

## Docs
| Document | Description |
|----------|-------------|
| BRD.docx | Business Requirements |
| HLD.docx | High-Level Design |
| LLD.docx | Low-Level Design |
