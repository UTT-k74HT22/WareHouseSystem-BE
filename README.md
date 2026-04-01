# Warehouse Management System Backend

Spring Boot backend for the WHS workspace.

## Stack

- Java 17
- Spring Boot 3.5.9
- MySQL 8
- Redis 7
- RabbitMQ 3
- Flyway

## Repository Scope

This repository owns backend source code, backend tests, and backend image publishing.

Shared production deployment now lives in `WareHouseSystem-INFRA`.

## Local Development

1. Copy `.env.example` to `.env`.
2. Start local dependencies:

```bash
docker compose up -d
```

3. Run the backend:

```bash
./mvnw spring-boot:run
```

Windows:

```bash
mvnw.cmd spring-boot:run
```

## Common Commands

```bash
./mvnw test
./mvnw clean package
docker compose ps
docker compose down
```

## Local Endpoints

- App: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- API docs: `http://localhost:8080/v3/api-docs`
- Health: `http://localhost:8080/actuator/health`
- RabbitMQ UI: `http://localhost:15672`

## Production Note

- Backend image is built and published by `.github/workflows/ci.yml`.
- FE + BE + Nginx + monitoring deployment is managed in `WareHouseSystem-INFRA`.
