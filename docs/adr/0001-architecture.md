# ADR 0001: Backend Architecture Baseline for WMS v1

## Status
Accepted (v1 baseline)

## Context
The current backend is a Spring Boot monolith with clear package-level layering (`controller`, `service`, `repository`, `entity/dto/mapper`) and cross-cutting infrastructure for security, Redis, RabbitMQ, scheduling, and Flyway.

From implementation and audit documents (`docs/00-system-overview.md`, `docs/01-module-map.md`, `docs/03-security-audit.md`, `docs/05-redis-rabbitmq-audit.md`), v1 must balance:
- Fast delivery for core modules already in progress.
- Reduced operational/security risk in authentication and async flows.
- Practical boundaries that keep code maintainable as inbound/outbound/inventory modules are completed.

Current risk signals to account for:
- AuthN/AuthZ hardening gaps (refresh token validation bug, token type enforcement gap, inconsistent `@PreAuthorize` style).
- Redis usage is effective for rate limiting and selected caching, but session-store config is misaligned with stateless JWT.
- RabbitMQ async email flow exists, but no DLQ/outbox yet and retry/idempotency safeguards are partial.

## Decision

### 1) Layered architecture boundaries (Controller / Service / Repository / Entity + DTO + Mapper)
We will keep and enforce the current layered architecture as the v1 default:

- **Controller boundary**
  - Owns HTTP concerns only: request parsing, validation trigger (`@Valid`), response shaping, status codes.
  - Must not call repositories directly.
  - Must not contain business rules beyond simple request-level checks.

- **Service boundary**
  - Owns use-case/business rules, transaction boundaries, orchestration, and authorization/business validations.
  - Calls repositories and mappers.
  - Is the only place where cross-aggregate logic is allowed.

- **Repository boundary**
  - Owns persistence queries only (JPA/native query where needed).
  - No business decisions, no HTTP/security assumptions.

- **Entity / DTO / Mapper boundary**
  - Entity: persistence model.
  - DTO: API contract only.
  - Mapper: conversion layer between entity and DTO.
  - Do not expose entities directly from controllers.

- **Practical v1 direction**
  - Keep monolith packaging; do not split microservices in v1.
  - Prioritize consistent boundary enforcement while finishing operational modules.

### 2) AuthN/AuthZ model (JWT stateless + role-based policy hardening)
Adopt **stateless JWT** as the only authentication model for v1 APIs:

- `SessionCreationPolicy.STATELESS` remains authoritative.
- Role-based authorization remains method-level (`@PreAuthorize`) and route-level (`SecurityConfig`) with standardized policy expression:
  - Prefer `hasRole('ADMIN')` style consistently.

Hardening commitments for v1 completion:
- Enforce token type in auth filter (access token only at API gateway layer).
- Fix refresh-token validation flow.
- Normalize role/authority mapping usage to prevent accidental 403/over-permit.
- Keep rate limiting in front of JWT filter for abuse protection.

Trade-off:
- JWT stateless keeps scaling and operational behavior simple.
- Immediate token revocation is limited without additional token blacklist/session mechanisms; acceptable for v1 with short access-token TTL and secure refresh flow.

### 3) Caching strategy (Redis usage, TTL, invalidation)
Redis stays as the v1 cache and rate-limit backing store:

- **In scope for v1**
  - Keep Spring Cache + Redis (`RedisCacheManager`) for read-heavy reference data (already used by UOM).
  - Keep Redis Lua-based rate limiting with TTL keys.

- **TTL policy**
  - Use default TTL 1 hour for existing caches as baseline.
  - Allow per-cache TTL tuning only where clear performance pain exists during v1.

- **Invalidation policy**
  - Keep explicit evictions on create/update/delete.
  - Accept broader eviction (`allEntries=true`) when correctness is more important than hit-rate optimization.

- **Risk handling**
  - Document that Redis HTTP session config is currently not aligned with stateless JWT and should be removed or explicitly justified after v1 stabilization.

Trade-off:
- Simpler cache governance and lower implementation cost now.
- Potential cache churn/less optimal hit ratio until finer-grained invalidation is introduced.

### 4) Async strategy (RabbitMQ patterns, retry, DLQ/outbox direction)
Use RabbitMQ as the v1 async backbone for non-critical user-facing latency paths (currently email):

- **Pattern kept for v1**
  - Producer publish after DB commit callback (current behavior) to avoid sending for rolled-back transactions.
  - Consumer updates processing state in DB (`PENDING/SENDING/SENT/FAILED/RETRY`).
  - Scheduler-based recovery/retry remains active.

- **Retry policy**
  - Keep bounded retry using `retryCount`/`maxRetry`.
  - Treat processing as at-least-once; consumers must be idempotent by business key where possible.

- **DLQ direction**
  - v1 completion target: define DLQ topology (DLX + DLQ) for poison messages.
  - If not fully implemented before release, minimum requirement is to keep failed-message visibility in DB with operational runbook.

- **Outbox direction**
  - Outbox is recommended next step for reliability (commit/publish gap), but not a hard blocker for v1 GA unless async expands to inventory/order-critical events.
  - For v1 email scope, current after-commit publish + scheduler fallback is accepted with known risk.

Trade-off:
- Faster delivery by reusing existing queue + scheduler pattern.
- Reliability is good but not strongest-possible without outbox/confirm callbacks/DLQ fully wired.

## Consequences

### Positive
- Preserves a clear, teachable architecture that matches existing code organization.
- Reduces implementation ambiguity for unfinished modules.
- Hardens security posture without heavy redesign.
- Keeps v1 scope realistic while documenting accepted technical debt.

### Negative / Risks Accepted
- Some audit findings remain risk items until hardening tasks are completed.
- Cache strategy may be conservative (more evictions, fewer hit-rate optimizations).
- Async publish is still exposed to post-commit/broker failure window without outbox.
- Mixed maturity across modules remains until scaffolded domains receive full business logic.

## Alternatives considered

1. **Move to microservices in v1**
   - Rejected for v1 due to high coordination/ops overhead and low delivery confidence for current module maturity.

2. **Use stateful server sessions for authentication**
   - Rejected because current architecture is already JWT stateless; stateful sessions add operational complexity and conflict with current security chain intent.

3. **Skip Redis cache until all modules are complete**
   - Rejected because Redis is already in use and valuable for rate limiting + read-heavy reference data.

4. **Rely only on RabbitMQ retries, no DB retry state**
   - Rejected because DB status tracking is needed for support visibility and scheduler-based recovery in current implementation.

5. **Require full DLQ + outbox before v1 release**
   - Deferred (not fully rejected): ideal for stronger guarantees, but may delay v1 completion. Chosen direction is phased hardening with DLQ first, then outbox when async scope broadens.
