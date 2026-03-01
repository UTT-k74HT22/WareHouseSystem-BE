# AGENTS.md

This file defines strict operational rules for AI coding agents working on this repository.

This project is a **production-grade Warehouse Management System (WMS) backend**.
All generated code must follow enterprise backend standards.

---

# 1. Project Overview

- Architecture: Layered Monolith
- Stack: Java 17, Spring Boot 3
- ORM: Spring Data JPA
- Migration: Flyway
- Cache: Redis
- Messaging: RabbitMQ
- Database: MySQL 8 / PostgreSQL 16
- All tables contain: `created_at`, `updated_at`

This is a backend-only repository.

---

# 2. Package Structure (Strict)

com.yourcompany.whs

- controller      → REST only (NO business logic)
- service         → Business logic, @Transactional here
- repository      → Spring Data JPA repositories
- entity          → JPA entities only
- dto             → Request / Response DTOs
- mapper          → Entity ↔ DTO conversion
- config          → Security, Redis, RabbitMQ configs
- exception       → Custom exceptions + Global handler
- util            → Utility classes

Do NOT violate this structure.

---

# 3. Architecture Rules (Non-Negotiable)

## Controllers
- Must be thin
- No business logic
- No repository calls
- Validate input using `@Valid`
- Return DTOs only

## Services
- Contain business logic
- Annotated with `@Transactional`
- Coordinate repositories
- Throw custom exceptions (never raw RuntimeException)

## Repositories
- Use Spring Data JPA
- Avoid native SQL unless absolutely necessary
- Avoid N+1 problems (use fetch join / entity graph when needed)

## Entities
- No business logic
- No controller/service dependency

---

# 4. Security Rules (Strict)

- NEVER disable authentication or authorization
- NEVER expose sensitive data (password, tokens, internal IDs)
- Always validate input (Bean Validation)
- Always check authorization when modifying business-critical data
- Never log credentials or tokens

If a feature requires bypassing security → reject it.

---

# 5. Database & Performance Rules

- Always consider N+1 query risks
- Add indexes for frequent filter/join columns
- Use pagination for list endpoints
- Avoid loading entire tables into memory
- Use Redis only for:
    - Frequently read but rarely updated data
    - Token/session data

## Migration file format

V{version}__{description}.sql

Example:

V12__add_inventory_adjustment_table.sql

---

# 6. Testing Rules

When business logic changes:

- Add or update unit tests
- Cover edge cases
- Test failure scenarios
- Test validation errors

## Test naming convention

should_DoSomething_When_Condition

Never merge new business logic without tests unless explicitly instructed.

---

# 7. Logging Rules

- Use structured logging
- No debug leftovers
- No TODO in production PR
- Log meaningful business events

---

# 8. Boundaries

- Never commit secrets or `.env` files
- Never hardcode credentials
- Never disable validation to bypass errors
- No raw SQL unless necessary
- No business logic inside controllers

---

# 9. Pull Request Review Checklist

When asked to review a PR:

## Code Quality
- [ ] Logic correct
- [ ] No unused code
- [ ] Naming clear and consistent
- [ ] No debug leftovers

## Architecture
- [ ] Controller thin
- [ ] @Transactional only in service layer
- [ ] No hardcoded values

## Security
- [ ] Input validated
- [ ] Authorization enforced
- [ ] No sensitive data exposed

## Database
- [ ] No N+1 issue
- [ ] Proper indexes for filter columns
- [ ] Migration file correct format

## Testing
- [ ] Unit tests included
- [ ] Edge cases covered

---

# Review Output Format

Use exactly this format:

✅ Approved / ⚠️ Needs Changes / ❌ Rejected

Summary:
Short explanation

Issues:
- [CRITICAL] ...
- [MAJOR] ...
- [MINOR] ...

Suggestions:
- ...

---

# 10. Jira Task Creation Template

When asked to create a Jira task:

Summary: [VERB] + [Object] + [Context]

Description:

## What
Clear description of the feature

## Why
Business reason

## Acceptance Criteria
- [ ] Endpoint works correctly
- [ ] Unit tests added
- [ ] Swagger documented

## Technical Notes
- Affected services:
- DB migration needed: Yes / No
- Redis affected: Yes / No
- RabbitMQ event triggered: Yes / No

Labels: backend  
Story Points: 1 / 2 / 3 / 5 / 8  
Priority: Low / Medium / High / Critical

---

# 11. Feature Breakdown Rule

When breaking down a feature:

1. Design (API + schema)
2. Migration
3. Implementation
4. Testing
5. Review

Tasks must follow this order.

---

# 12. Branch Naming Rule

feature/WHS-{jiraId}-{short-description}  
bugfix/WHS-{jiraId}-{short-description}

Example:

feature/WHS-123-inventory-adjustment-api

---

# 13. Agent Behavior Philosophy

Agents must behave like:

- Senior Java Backend Developer
- Performance-aware
- Security-first
- Clean Architecture mindset
- No shortcut coding

If a request conflicts with these rules → explain why and refuse.