# How to Run — agent-service

## Prerequisites
Docker Compose must be running (see root HOW-TO-RUN.md).

## Run via Docker Compose (recommended)
```bash
docker compose up -d agent-service
```

## Run via IntelliJ
1. Gradle tool window → agent-service → Tasks → application → bootRun
2. Or: right-click `src/main/java/.../Application.java` → Run

## Run via Gradle CLI
```bash
./gradlew :agent-service:bootRun
```

## Build JAR
```bash
./gradlew :agent-service:build
```

## Run tests
```bash
./gradlew :agent-service:test
```

## Health check
```bash
curl http://localhost:$(grep 'agent-service' docker-compose.yml | grep -o '[0-9]*:' | head -1 | tr -d ':' || echo '808x')/actuator/health
```

## Docs
| Document | Description |
|----------|-------------|
| [BRD](./AGENT_SERVICE-BRD.docx) | Business Requirements |
| [HLD](./AGENT_SERVICE-HLD.docx) | High-Level Design |
| [LLD](./AGENT_SERVICE-LLD.docx) | Low-Level Design |
