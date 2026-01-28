# AGENTS.md

This file provides shared guidance for AI coding agents working on this repository.

## Project overview

- Warehouse Management System (WMS) – **Backend only**.
- Tech stack:
    - Java 17
    - Spring Boot 3
    - Spring Data JPA
    - Flyway
    - Redis
    - RabbitMQ
- Database:
    - MySQL 8 / PostgreSQL 16
    - All tables have `created_at` and `updated_at`.

## Common commands

- Run application:
    - `./mvnw spring-boot:run`
- Run tests:
    - `./mvnw test`

## Code style

- Follow existing package structure and naming conventions.
- Keep controllers thin, business logic in services.
- Prefer explicit, readable code over clever abstractions.
- Always update or add tests when business logic changes.

## Boundaries

- Never commit secrets or `.env` files.
- Never disable security, validation, or authorization just to bypass errors.
