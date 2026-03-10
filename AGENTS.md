# AGENTS.md

This file defines strict operational rules for AI coding agents working on this repository.

This project is a production-grade Warehouse Management System (WMS) backend.

---

# 1. Project Overview

- Architecture: Layered Monolith
- Stack: Java 17, Spring Boot 3.5.9, Spring Data JPA
- Migration: Flyway | Cache: Redis | Messaging: RabbitMQ
- Runtime database: MySQL 8
- Test database: H2 with MySQL compatibility mode
- Build tool: Maven
- Common audit fields include `created_at` and `updated_at`

---

# 2. Build, Test & Run Commands

```bash
# Build
mvn clean install
mvn clean package -DskipTests
mvn -q -DskipTests compile

# Run
mvn spring-boot:run
java -jar target/whs-0.0.1-SNAPSHOT.jar

# Test
mvn test
mvn test -Dtest=ClassName
mvn test -Dtest=ClassName#testMethod
mvn test -Dtest="ClassName#m1+m2"
mvn test -Dspring.profiles.active=test
```

---

# 3. Package Structure

Base package: `org.demo.whs`

- `controller` -> REST endpoints only, no business logic
- `service` -> business logic and transaction boundaries
- `service.impl` -> service implementations
- `repository` -> Spring Data JPA repositories and custom query support
- `entity` -> JPA entities
- `entity.dto.request` -> request DTOs
- `entity.dto.response` -> response DTOs, including `BaseResponse<T>` and `PageResponse<T>`
- `entity.enums` -> domain enums
- `mapper` -> entity to DTO conversion
- `configuration` -> security, Redis, RabbitMQ, web, and infrastructure config
- `exception` -> custom exceptions and global exception handler
- `security`, `interceptor`, `helpers`, `utils` -> cross-cutting support code

---

# 4. Code Style Guidelines

## Import Order
1. Jakarta EE (`jakarta.*`)
2. Spring Framework (`org.springframework.*`)
3. External libraries (`org.*`, `com.*`) alphabetically
4. Internal project (`org.demo.whs.*`)
5. Static imports last

## Naming Conventions
- Classes: PascalCase (`WareHouseController`, `InventoryService`)
- Methods and variables: camelCase
- Constants: UPPER_SNAKE_CASE
- DTOs: `{Entity}{Request|Response}` where it matches the existing module naming
- Tests: `should_DoSomething_When_Condition`

## Formatting
- Use Lombok where the module already follows that pattern
- Max line length: 120 characters
- Indentation: 4 spaces

## Entity
- Use `@Entity`, `@Table`, `@Column`
- Keep entities free of business logic
- Use enums for status fields
- Preserve the current audit strategy based on `BaseEntity` with `@PrePersist` and `@PreUpdate`
- Do not introduce `@CreatedDate` / `@LastModifiedDate` selectively unless the project is migrated consistently

## DTO
- Request DTOs must use Bean Validation (`@NotNull`, `@NotBlank`, `@Size`, and similar)
- Response DTOs should remain mapper-friendly and consistent with current patterns
- Keep DTOs grouped by module under `entity/dto/request/*` and `entity/dto/response/*`

## Service
- Use `@Transactional` intentionally, with `readOnly = true` for pure queries where appropriate
- Throw custom domain exceptions, never new raw `RuntimeException` in business flows
- Log with `@Slf4j`

## Controller
- Use `@RestController`, `@RequestMapping`, `@RequiredArgsConstructor`
- Validate request bodies with `@Valid`
- Keep controllers thin
- Return `BaseResponse<T>` wrappers
- Use `PageResponse<T>` for paginated endpoints

---

# 5. Error Handling

- Prefer custom exceptions extending `BaseException`
- Map exceptions through the global exception handler
- Return `BaseResponse` with stable error codes
- Log at `ERROR` for unexpected failures and `WARN` for business-rule violations

---

# 6. Security Rules

- NEVER disable authentication or authorization
- NEVER expose sensitive data
- Always validate input
- Preserve `@PreAuthorize` and method security on protected endpoints
- Never log credentials, tokens, or secret values

---

# 7. Database & Performance

- Avoid N+1 with fetch joins, `@EntityGraph`, or projection strategies where appropriate
- Index frequent filter and join columns through Flyway migrations
- Paginate list endpoints unless the use case is intentionally bounded
- Use Redis only for read-heavy or coordination use cases that justify cache or shared-state complexity
- Name migrations as `V{version}__{description}.sql`

---

# 8. Testing

- Unit tests: `@ExtendWith(MockitoExtension.class)` with `@Mock` and `@InjectMocks`
- Integration tests: `@SpringBootTest` or focused slice tests with the `test` profile and H2
- Repository tests: `@DataJpaTest`
- Test naming: `should_ThrowException_When_NotFound`
- Cover happy path, validation failures, edge cases, and regression scenarios

---

# 9. Logging

- Use structured logging such as `log.info("message {}", value)`
- Do not leave debug-only code paths in production logic
- Do not add new TODO comments in changed code
- Log meaningful create, update, delete, and status transition events

---

# 10. Boundaries

- Never commit secrets or local credential files
- Never hardcode credentials
- Never disable validation
- No raw SQL unless necessary and justified
- No business logic in controllers

---

# 11. Branch Naming

```text
feature/WHS-{jiraId}-{short-description}
bugfix/WHS-{jiraId}-{short-description}
```

---

# 12. Agent Philosophy

- Senior Java backend developer mindset
- Performance-aware and security-first
- Clean architecture, no shortcut coding

---

# 13. WMS Business Flow Rules

## Domain Rules
- State transitions must be explicit and validated
- Never bypass status validation in the service layer
- Quantity-related fields must never become negative
- Header totals must be recalculated from lines, not trusted from client input
- Child records must belong to the same tenant, company, or warehouse scope as the parent
- Soft business constraints must return business exceptions, not generic errors

## Transaction Rules
- Multi-step inventory mutations must be atomic
- Validate concurrency-sensitive stock updates carefully
- Do not split one business transaction across multiple services unless clearly intentional
- Avoid side effects before all validations pass

## Data Integrity Rules
- Validate referenced entities before create or update mutations
- Never trust frontend-calculated totals, statuses, or derived fields
- On delete or update, verify record ownership and current status before mutation
- Preserve audit fields and metadata updates consistently

## Review Rules for Agents
Before changing code, always identify:
1. invariant being protected
2. state transition being changed
3. affected aggregate root
4. possible regression points
5. minimal verification strategy

If a request conflicts with these rules, explain why and refuse.
