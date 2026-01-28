# GitHub Copilot – Backend Instructions (WMS)

## Project context

- This repository is the **Backend** of a Warehouse Management System (WMS).
- Architecture:
    - Monolith Spring Boot application.
    - Layered: **Controller → Service → Domain/Entity → Repository**.
- Tech stack:
    - Java 17
    - Spring Boot 3 (Web, Security, Validation)
    - Spring Data JPA
    - Spring Security + JWT
    - Flyway for DB migration
    - Redis for caching / token / rate limiting
    - RabbitMQ for async tasks (export report, heavy jobs)
    - MySQL 8 or PostgreSQL 16

- Database rules:
    - Every table must have `created_at` and `updated_at`.
    - Prefer status flags / soft delete instead of hard delete when possible.

## General behavior

- Always **respect the current architecture and patterns** in the project.
- Before suggesting big refactors:
    - Read related controllers, services, entities, repositories.
    - Keep naming and package structure consistent.
- Prefer:
    - Clear, easy-to-read code over clever one-liners.
    - Explicit error handling and logging.

## Language & docs

- Code, comments, and Javadoc: **English**.
- In explanations (Copilot Chat), you can mix English + short Vietnamese notes if the user asks.

## Coding style – Java & Spring

- Use **constructor injection**, not field injection.
- Controllers:
    - Only handle HTTP mapping, validation, and call services.
    - Do not put business logic directly in controllers.
- Services:
    - Contain business logic.
    - Encapsulate transactional boundaries (`@Transactional` where needed).
- Repositories:
    - Extend `JpaRepository` or `PagingAndSortingRepository`.
    - Use clear method names; when queries are complex, prefer `@Query` with parameters.

- Validation:
    - Use `jakarta.validation` annotations on request DTOs.
    - Handle validation errors via global exception handler.

## Flyway & database changes

- Use versioned migration files: `Vxxx__description.sql`.
- Never modify existing migrations; always create a new one.
- Make migrations idempotent when possible and safe.

## Caching & performance

- Use Redis for:
    - Caching frequently read data.
    - Handling rate-limiting logic if present.
- When generating queries:
    - Think about indexes on filter/sort columns.
    - Avoid N+1 query problems (use `join fetch` or proper fetch strategy).

## Testing

- Use JUnit 5 + Mockito.
- For any new business logic:
    - Add unit tests in `src/test/java`.
    - At least:
        - 1 “happy path” test.
        - Several edge-case tests (validation failure, missing data, etc.).
- Follow existing naming conventions for test classes and methods.

## Error handling & logging

- Use custom exceptions for business errors with clear error codes and messages.
- Log:
    - At INFO level for main flow start/end.
    - At WARN/ERROR for exceptional situations.
- Do not log sensitive data (passwords, tokens, personal information).

## Security

- Use Spring Security + JWT properly:
    - No hardcoded secrets.
    - No disabling security filters just to “make it work”.
- When generating code for authentication/authorization:
    - Respect the current security config.
    - Use roles/authorities consistent with the project.

## Git & PR hygiene

- Do not introduce breaking API changes without mentioning them.
- Group changes by feature/module.
- When suggesting commit messages or PR descriptions:
    - Use a short clear title.
    - Provide bullet points explaining key changes.
