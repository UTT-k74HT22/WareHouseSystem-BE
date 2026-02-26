# 08 - v1 Backlog (Epics -> Features -> Tasks)

## Priority Model
- **P0 (Critical):** Must be completed before broad v1 rollout. Focus on security, data integrity, and reliability risks from audits.
- **P1 (High):** Core functional and operational improvements required for stable warehouse operations.
- **P2 (Medium):** Optimization and technical debt cleanup after P0/P1 stabilization.

## Audit Inputs Used for Prioritization
- `docs/03-security-audit.md`
- `docs/04-db-migrations-audit.md`
- `docs/05-redis-rabbitmq-audit.md`

---

## Epic 1 - Security & Access Control Hardening (**P0**)

### Feature 1.1 - Fix refresh/auth token validation defects (**P0**)
**Audit refs:** F-01, F-02 (`03-security-audit.md`)

#### Tasks (small, PR-friendly)
1. **[P0]** Fix inverted refresh token validation condition in `AuthServiceImpl.validRefreshToken`.
2. **[P0]** Enforce `type=accessToken` in `JwtAuthFilter` for bearer auth.
3. **[P0]** Add unit tests for token validation paths (valid/expired/wrong type/signature failure).
4. **[P0]** Add integration tests for `/auth/refresh-token` and protected endpoints with refresh token misuse.
5. **[P0]** Add security regression tests to prevent refresh token acceptance on protected APIs.

**Dependencies:**
- Task 2 depends on JWT claim parser behavior in `JwtProvider`.
- Tasks 3-5 depend on tasks 1-2.

**Test coverage requirement:**
- **Unit:** Auth service and JWT filter branch coverage >= 90% for modified methods.
- **Integration:** End-to-end auth flow tests for login -> access -> refresh.
- **Security regression:** Negative-path suite proving refresh token cannot authorize protected endpoints.

### Feature 1.2 - RBAC consistency & endpoint protection (**P0**)
**Audit refs:** F-03, F-04 (`03-security-audit.md`)

#### Tasks (small, PR-friendly)
1. **[P0]** Standardize role checks (`hasRole('ADMIN')` or `hasAuthority('ROLE_ADMIN')`) across admin endpoints.
2. **[P0]** Add explicit admin guard to `POST /api/v1/emails/{id}/retry`.
3. **[P0]** Create integration authorization matrix tests (admin/user/unauthenticated) for email endpoints.
4. **[P0]** Add security regression tests for privilege escalation attempts.

**Dependencies:**
- Task 3 depends on tasks 1-2.
- Task 4 depends on task 3 baseline test fixtures.

**Test coverage requirement:**
- **Unit:** Authorization expression helper/tests where applicable.
- **Integration:** Role-based endpoint access matrix.
- **Security regression:** Forbidden access scenarios validated with stable error contracts (401/403).

### Feature 1.3 - Secure configuration, validation, and error contract normalization (**P0**)
**Audit refs:** F-05, F-06, F-07, F-08, F-09 (`03-security-audit.md`)

#### Tasks (small, PR-friendly)
1. **[P0]** Remove weak JWT secret fallback; fail startup when `JWT_SECRET` is missing/invalid.
2. **[P0]** Add startup validation to reject wildcard CORS origin when credentials are enabled.
3. **[P0]** Redact sensitive fields in logs (token-like values, PII identifiers).
4. **[P0]** Add input constraints for high-risk request/query surfaces (e.g., product search bounds/lengths).
5. **[P0]** Align auth entry-point error response with global API error contract.
6. **[P0]** Add security regression tests for config hardening and validation boundaries.

**Dependencies:**
- Task 6 depends on tasks 1-5.

**Test coverage requirement:**
- **Unit:** Config validator and masking utility tests.
- **Integration:** Startup/profile tests, request validation behavior, standardized error schema assertions.
- **Security regression:** Malformed input, invalid origin, and secret misconfiguration negative tests.

---

## Epic 2 - Data Integrity & Migration Reliability (**P0**)

### Feature 2.1 - Migration correctness and consistency (**P0**)
**Audit refs:** Version/header mismatches, module comment mismatches (`04-db-migrations-audit.md`)

#### Tasks (small, PR-friendly)
1. **[P0]** Correct migration file header version metadata to match filenames.
2. **[P0]** Correct migration module comments (inbound/outbound labeling).
3. **[P0]** Add migration lint/check script in CI to detect naming/header mismatch.
4. **[P0]** Add migration checksum/validation step to pre-merge pipeline.

**Dependencies:**
- Task 3 depends on conventions finalized in tasks 1-2.
- Task 4 depends on task 3.

**Test coverage requirement:**
- **Unit:** Lint parser tests for valid/invalid filename-header pairs.
- **Integration:** CI pipeline test job executing migration validation against clean DB.
- **Security regression:** N/A (not security-focused); integrity regression required via repeatable migration runs.

### Feature 2.2 - Entity mapping integrity and schema alignment (**P0**)
**Audit refs:** trailing table-name spaces, unmapped entities (`Permission`, `RoleHasPermission`) (`04-db-migrations-audit.md`)

#### Tasks (small, PR-friendly)
1. **[P0]** Fix `@Table` naming with trailing spaces in impacted entities.
2. **[P0]** Resolve `Permission` / `RoleHasPermission` mapping strategy (map explicitly or remove dead classes).
3. **[P0]** Add repository-level integration tests verifying CRUD/mapping for impacted entities.
4. **[P0]** Add startup validation check for entity-table mapping mismatches.

**Dependencies:**
- Task 3 depends on tasks 1-2.
- Task 4 depends on task 2 decisions.

**Test coverage requirement:**
- **Unit:** Entity metadata validation utility tests.
- **Integration:** JPA mapping + repository integration tests against test DB.
- **Security regression:** N/A; data integrity regression required for schema mapping consistency.

### Feature 2.3 - Deletion and audit strategy standardization (**P0**)
**Audit refs:** no soft-delete convention, repository filtering gaps (`04-db-migrations-audit.md`)

#### Tasks (small, PR-friendly)
1. **[P0]** BA + engineering decision record: soft-delete vs status-based deactivation per module.
2. **[P0]** Implement chosen deletion model in base schema/entity conventions.
3. **[P0]** Add repository query filters/specifications to enforce deletion visibility rules.
4. **[P0]** Add audit log fields/rules for delete/deactivate actions.
5. **[P0]** Add integration tests for delete visibility and restore (if applicable).

**Dependencies:**
- Task 2 depends on task 1.
- Tasks 3-5 depend on task 2.

**Test coverage requirement:**
- **Unit:** Visibility rule evaluation and repository predicate unit tests.
- **Integration:** Create/delete/query behavior across active/inactive/deleted records.
- **Security regression:** Regression for unauthorized delete/deactivate attempts.

---

## Epic 3 - Async Messaging Reliability (**P0**)

### Feature 3.1 - RabbitMQ failure isolation with DLQ (**P0**)
**Audit refs:** missing DLX/DLQ (`05-redis-rabbitmq-audit.md`)

#### Tasks (small, PR-friendly)
1. **[P0]** Define DLX/DLQ exchange, queue, routing key naming standard.
2. **[P0]** Implement broker topology config changes for email queue dead-lettering.
3. **[P0]** Route irrecoverable messages to DLQ after retry threshold.
4. **[P0]** Add operational runbook for DLQ triage and replay.
5. **[P0]** Add integration tests for poison message -> DLQ path.

**Dependencies:**
- Task 3 depends on tasks 1-2.
- Task 5 depends on tasks 2-3.

**Test coverage requirement:**
- **Unit:** Retry classification and dead-letter routing decision tests.
- **Integration:** RabbitMQ integration tests verifying DLQ bindings and message routing.
- **Security regression:** Validate no sensitive payload leakage in DLQ logs/monitoring outputs.

### Feature 3.2 - Outbox pattern for guaranteed publish reliability (**P0**)
**Audit refs:** post-commit publish loss window (`05-redis-rabbitmq-audit.md`)

#### Tasks (small, PR-friendly)
1. **[P0]** Create outbox table schema + indexes + status lifecycle.
2. **[P0]** Persist outbound email events into outbox within transaction.
3. **[P0]** Implement outbox relay worker with retry/backoff and delivery marking.
4. **[P0]** Add publisher confirm/return callback handling with metrics.
5. **[P0]** Add integration tests for DB commit success + broker outage + eventual publish recovery.

**Dependencies:**
- Task 3 depends on tasks 1-2.
- Task 4 depends on task 3.
- Task 5 depends on tasks 1-4.

**Test coverage requirement:**
- **Unit:** Outbox state transitions and retry/backoff logic.
- **Integration:** Transactional outbox to broker end-to-end tests.
- **Security regression:** Ensure event payload sanitization and access control on replay operations.

### Feature 3.3 - Idempotency and duplicate-send prevention (**P0**)
**Audit refs:** at-least-once duplicate risk (`05-redis-rabbitmq-audit.md`)

#### Tasks (small, PR-friendly)
1. **[P0]** Define idempotency key standard (`email_log_id`/message id).
2. **[P0]** Add deduplication guard in consumer/service.
3. **[P0]** Add unique constraints/indexes for idempotency key where needed.
4. **[P0]** Add integration tests for repeated message delivery handling.

**Dependencies:**
- Task 2 depends on task 1.
- Task 3 depends on task 1.
- Task 4 depends on tasks 2-3.

**Test coverage requirement:**
- **Unit:** Idempotency decision logic.
- **Integration:** Duplicate message replay with exactly-once business effect assertions.
- **Security regression:** Validate retry/replay endpoints cannot be abused by unauthorized users.

---

## Epic 4 - Core WMS Functional Completeness (**P1**)

### Feature 4.1 - Inbound receiving quality controls (**P1**)

#### Tasks (small, PR-friendly)
1. **[P1]** Enforce SKU/UOM validation at receiving line level.
2. **[P1]** Add lot/batch/expiry mandatory rule by product policy.
3. **[P1]** Add discrepancy workflow for over/short receipt.
4. **[P1]** Add integration tests for ASN/PO receiving edge cases.

**Dependencies:**
- Task 2 depends on product master policy availability.
- Task 3 depends on task 1 validation outcomes.

**Test coverage requirement:**
- **Unit:** Receiving validation rule engine.
- **Integration:** PO/ASN receiving transaction scenarios.
- **Security regression:** Prevent receiving updates by unauthorized roles.

### Feature 4.2 - Outbound allocation & FEFO/FIFO enforcement (**P1**)

#### Tasks (small, PR-friendly)
1. **[P1]** Implement allocation strategy toggle (FIFO/FEFO/LIFO by item/warehouse policy).
2. **[P1]** Add fallback rule when FEFO cannot be satisfied due to missing expiry.
3. **[P1]** Add reservation conflict detection for concurrent order allocation.
4. **[P1]** Add integration tests for picking recommendation correctness.

**Dependencies:**
- Task 2 depends on task 1.
- Task 4 depends on tasks 1-3.

**Test coverage requirement:**
- **Unit:** Allocation comparator and fallback behavior tests.
- **Integration:** Sales order allocation and picking workflow tests.
- **Security regression:** Ensure allocation override endpoints require proper approval role.

### Feature 4.3 - Inventory count & reconciliation controls (**P1**)

#### Tasks (small, PR-friendly)
1. **[P1]** Implement cycle count batch locking rules by location scope.
2. **[P1]** Add variance threshold + approval flow for adjustments.
3. **[P1]** Add stock movement audit linkage to reconciliation transactions.
4. **[P1]** Add integration tests for count -> adjustment -> audit trail flow.

**Dependencies:**
- Task 2 depends on task 1.
- Task 3 depends on task 2.

**Test coverage requirement:**
- **Unit:** Variance calculation and approval threshold rules.
- **Integration:** Cycle count and reconciliation E2E.
- **Security regression:** Unauthorized adjustment attempts must be denied and audited.

---

## Epic 5 - Performance & Configuration Debt Cleanup (**P2**)

### Feature 5.1 - Redis/session/cache configuration rationalization (**P2**)
**Audit refs:** session mismatch, per-cache TTL tuning, unused config (`05-redis-rabbitmq-audit.md`)

#### Tasks (small, PR-friendly)
1. **[P2]** Decide and document Redis session strategy for JWT-stateless architecture.
2. **[P2]** Remove unused `jedis` dependency if not required.
3. **[P2]** Introduce per-cache TTL and cache key standards.
4. **[P2]** Remove or bind `app.rate-limit` properties to runtime behavior.

**Dependencies:**
- Task 2 depends on task 1 decision.
- Tasks 3-4 can run in parallel after task 1.

**Test coverage requirement:**
- **Unit:** Cache config binding and key-generation tests.
- **Integration:** Cache TTL behavior and rate-limit config activation tests.
- **Security regression:** Validate no bypass/regression in rate limit after config refactor.

### Feature 5.2 - Scheduler and async operability tuning (**P2**)

#### Tasks (small, PR-friendly)
1. **[P2]** Externalize scheduler intervals/cron to environment properties.
2. **[P2]** Add metrics dashboard for queue lag, retry count, DLQ depth.
3. **[P2]** Add alerting thresholds and on-call runbook references.

**Dependencies:**
- Task 2 depends on observability naming conventions.
- Task 3 depends on task 2.

**Test coverage requirement:**
- **Unit:** Property binding/default fallback tests.
- **Integration:** Scheduler picks up config values in runtime profiles.
- **Security regression:** Ensure ops endpoints/metrics are access-controlled.

---

## Cross-Epic Dependency Highlights
1. **Epic 1 (security hardening) must complete before exposing new admin/ops endpoints from other epics.**
2. **Feature 2.3 (deletion strategy) should precede broad repository refactors in Epic 4.**
3. **Feature 3.2 (outbox) should precede large-scale event-driven module expansion.**
4. **Feature 3.1 (DLQ) and 3.3 (idempotency) must be in place before raising async throughput limits.**

---

## Definition of Done (DoD)
A backlog item (task/feature) is **Done** only when all conditions below are satisfied:

1. **Business behavior complete**
   - Acceptance criteria implemented with no ambiguity.
   - Edge cases and exception flows covered.

2. **Code quality and reviewability**
   - Change is PR-friendly (target: ~200-400 LOC net change per PR unless justified).
   - Single responsibility per PR (no mixed unrelated refactors).
   - Peer-reviewed and approved by backend + relevant domain reviewer.

3. **Testing requirements met**
   - Required test types for that task are implemented (**unit/integration/security regression** as specified above).
   - All tests pass in CI.
   - No reduction in critical-module coverage.

4. **Data and migration safety**
   - DB changes include migration + rollback/mitigation notes.
   - Data integrity constraints and indexes are validated.

5. **Operational readiness**
   - Logging/metrics updated for new critical flows.
   - Alerting/runbook updated for reliability-sensitive features.

6. **Security and compliance checks**
   - Authorization and input validation verified.
   - Sensitive data handling follows masking/redaction standards.

7. **Documentation updated**
   - API/DB/sequence notes updated in docs.
   - Backlog item status and dependencies updated after merge.

---

## Backlog Maintenance Rules
- Re-prioritize weekly; any newly discovered **security/data integrity/reliability** risk is auto-triaged as candidate **P0**.
- Keep tasks implementation-sized (1 PR per task where possible).
- Track blocked tasks with explicit blocker owner + ETA.
