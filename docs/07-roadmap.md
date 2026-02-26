# WMS Backend Roadmap (8 Weeks / 4 Sprints)

## 1) Planning Principles (Approved)
- **Horizon:** 8 weeks, split into **4 sprints** (2 weeks/sprint).
- **Execution order:** **Risk-first before feature breadth**.
- **Delivery style:** small PRs, each with measurable output (test case, migration, endpoint, dashboard metric).
- **Early priority from audits:**  
  1) Security fixes, 2) DB consistency hardening, 3) Async reliability hardening.
- **Quality gate per sprint (mandatory):**
  - Build: `./mvnw clean verify`
  - Tests: unit + integration tests pass
  - Static analysis/lint: run configured Java static checks/lint if available in pipeline (or fallback quality profile: SpotBugs/Checkstyle/PMD onboarding in Sprint 1)

---

## 2) Sprint-Level Roadmap

## Sprint 1 (Weeks 1-2) — Security Stabilization (Highest Risk)
**Goal:** Close critical/high auth and authorization findings before expanding new features.

### Week 1 Milestone
- **Deliverables**
  - Fix refresh-token validation logic (audit F-01).
  - Enforce JWT token type = access token in auth filter (audit F-02).
  - Add/adjust unit tests for token validation matrix (valid/expired/wrong-type/tampered).
  - CI pipeline baseline for build + tests + static analysis/lint execution.
- **Acceptance Criteria**
  - Valid refresh token is accepted only by refresh flow.
  - Refresh token is rejected for protected API bearer authentication.
  - Build succeeds and all auth/security tests pass in CI.
  - Static analysis/lint job executes and reports no blocker-level issues.
- **Dependencies**
  - Security team confirms token claim contract (`type` values).
  - Test data/mocks for JWT secrets and token fixtures.
  - CI runner permissions for quality jobs.
- **Risks + Mitigation**
  - **Risk:** Auth regressions on login/refresh flow.  
    **Mitigation:** add regression suite + staged rollout in non-prod.
  - **Risk:** Inconsistent token generation in existing clients.  
    **Mitigation:** backward compatibility check + client communication note.

### Week 2 Milestone
- **Deliverables**
  - Normalize role checks (`hasRole` / `ROLE_*`) across secured endpoints (audit F-03).
  - Lock down email retry endpoint to ADMIN-only (audit F-04).
  - Remove weak JWT secret fallback and enforce startup validation (audit F-06).
  - Add security integration tests for endpoint authorization matrix.
- **Acceptance Criteria**
  - Non-admin cannot trigger admin-only endpoints (403).
  - Admin access works with standardized role model.
  - Application fails startup if JWT secret missing/weak.
  - CI quality gate for Sprint 1 passes (build/tests/static checks).
- **Dependencies**
  - Approved RBAC matrix from product/security.
  - DevOps updates for environment secret management.
- **Risks + Mitigation**
  - **Risk:** Role mapping mismatch across modules.  
    **Mitigation:** central authority mapping policy + cross-module test cases.
  - **Risk:** Deployment failure due to missing secrets.  
    **Mitigation:** pre-deploy secret validation checklist.

---

## Sprint 2 (Weeks 3-4) — DB Consistency & Migration Reliability
**Goal:** Eliminate schema/entity drift and improve data integrity confidence.

### Week 3 Milestone
- **Deliverables**
  - Correct migration metadata/comment inconsistencies in Flyway scripts.
  - Fix entity mapping defects (e.g., trailing spaces in `@Table`, unmapped intended entities).
  - Add migration validation checks in CI (Flyway validate on clean DB).
  - Produce DB convention checklist (`created_at`, `updated_at`, naming, constraints).
- **Acceptance Criteria**
  - Flyway migration/validate passes on clean environment.
  - Entity mapping loads successfully without table-name mismatch.
  - Migration naming/versioning report has zero unresolved mismatches.
  - CI build + test + static checks pass for Sprint 2 scope.
- **Dependencies**
  - DBA review for migration corrections.
  - Agreement on historical migration handling policy (fix-forward vs amend).
- **Risks + Mitigation**
  - **Risk:** Migration change affects existing environments.  
    **Mitigation:** use forward-only corrective scripts + dry-run on staging clone.
  - **Risk:** Hidden ORM mapping issues appear late.  
    **Mitigation:** add schema-validation startup check in pipeline.

### Week 4 Milestone
- **Deliverables**
  - Define and implement explicit soft-delete strategy decision (or document status-based alternative with invariants).
  - Add repository query standards for active/inactive/deleted visibility.
  - Add DB constraints/index review for high-traffic business tables.
  - Add data consistency test pack (repository + migration integration tests).
- **Acceptance Criteria**
  - Soft-delete/data-lifecycle rule is documented and implemented consistently.
  - Query behavior is deterministic for active vs inactive/deleted records.
  - Key constraints/indexes verified for critical read/write paths.
  - Sprint 2 CI quality gate passes.
- **Dependencies**
  - Product sign-off on data retention/audit policy.
  - DBA support for index impact review.
- **Risks + Mitigation**
  - **Risk:** Performance regression from new constraints/indexes.  
    **Mitigation:** benchmark before/after and rollout with observability.
  - **Risk:** Functional changes in data visibility.  
    **Mitigation:** regression tests for list/search/report endpoints.

---

## Sprint 3 (Weeks 5-6) — Async Reliability & Observability
**Goal:** Reduce message loss/duplication risk and improve async operational control.

### Week 5 Milestone
- **Deliverables**
  - Add RabbitMQ DLQ/DLX topology for email processing.
  - Introduce idempotency guard for email send processing.
  - Wire publisher confirm/return callbacks with structured logging/metrics.
  - Document retry policy boundaries (transient vs permanent failure).
- **Acceptance Criteria**
  - Poison messages are routed to DLQ after retry policy exhaustion.
  - Duplicate delivery attempts do not create duplicate business side effects.
  - Publish nack/return events are visible in logs/metrics dashboards.
  - Sprint 3 quality gate passes.
- **Dependencies**
  - RabbitMQ infrastructure config access.
  - Monitoring stack availability for async metrics/alerts.
- **Risks + Mitigation**
  - **Risk:** Misconfigured bindings cause message blackhole.  
    **Mitigation:** integration tests with test broker + routing verification checklist.
  - **Risk:** Over-retry creates queue backlog.  
    **Mitigation:** capped retries + dead-letter triage SOP.

### Week 6 Milestone
- **Deliverables**
  - Implement outbox pattern for post-commit publish reliability.
  - Externalize scheduler intervals/configuration for operational tuning.
  - Create async runbook (replay, DLQ handling, retry triage).
  - Add end-to-end reliability tests (commit, publish, consume, failure scenarios).
- **Acceptance Criteria**
  - No lost messages in simulated post-commit publish failure scenario.
  - Scheduler intervals configurable per environment without code changes.
  - Runbook approved and usable by on-call team.
  - Sprint 3 CI quality gate passes (including async integration suite).
- **Dependencies**
  - DB table for outbox + relay worker ownership.
  - DevOps agreement on scheduler/env configuration management.
- **Risks + Mitigation**
  - **Risk:** Outbox increases DB load/latency.  
    **Mitigation:** batched relay + index tuning + throughput monitoring.
  - **Risk:** Operational complexity increases.  
    **Mitigation:** clear runbook + alert thresholds + handover training.

---

## Sprint 4 (Weeks 7-8) — Feature Breadth on Hardened Foundation
**Goal:** Deliver prioritized functional breadth after core risk controls are stabilized.

### Week 7 Milestone
- **Deliverables**
  - Deliver next-priority inventory/inbound/outbound API enhancements from backlog.
  - Apply validation standards to exposed request surfaces (length/enums/paging bounds).
  - Align error response contract across auth/global handlers.
  - Ensure each feature ships in small PR slices (endpoint + tests + docs).
- **Acceptance Criteria**
  - New/updated APIs meet agreed request/response/error contract.
  - Validation rejects malformed/oversized inputs with deterministic error codes.
  - Feature PRs include test evidence and API documentation updates.
  - Sprint 4 gate (build/tests/static checks) passes.
- **Dependencies**
  - Finalized backlog priority and acceptance rules from product owner.
  - QA test scenarios for newly added workflows.
- **Risks + Mitigation**
  - **Risk:** Scope creep in late sprint.  
    **Mitigation:** strict WIP limits + defer non-critical items to post-v1 backlog.
  - **Risk:** Contract drift between FE/BE.  
    **Mitigation:** API contract review before merge.

### Week 8 Milestone
- **Deliverables**
  - Stabilization/hardening: bug burn-down, non-functional fixes, release notes.
  - Final audit closure check (security, DB consistency, async reliability).
  - Production readiness checklist (rollback, observability, on-call handover).
  - v1 release candidate tag with measurable quality report.
- **Acceptance Criteria**
  - Open critical/high defects = 0 for v1 scope.
  - Audit findings tracked as closed or accepted with explicit rationale.
  - Release checklist signed by Engineering + Product + Ops.
  - Final Sprint 4 CI/CD quality gate passes end-to-end.
- **Dependencies**
  - Cross-team UAT sign-off.
  - Deployment window and rollback environment readiness.
- **Risks + Mitigation**
  - **Risk:** Late critical defects delay release.  
    **Mitigation:** freeze window + daily triage + release-go/no-go criteria.
  - **Risk:** Incomplete operational handover.  
    **Mitigation:** mandatory runbook walkthrough and ownership confirmation.

---

## 3) Global Delivery Controls (All Sprints)
- **PR policy:** prefer PR size <= ~300 changed lines (excluding generated files) where practical.
- **Definition of Done (each milestone):**
  1) Code merged, 2) tests passing, 3) static checks passing, 4) docs updated, 5) demo evidence attached.
- **Measurable output examples:**
  - # of findings closed by severity
  - # of migration validation failures (target: 0)
  - # of DLQ incidents triaged/resolved
  - CI pass rate and median pipeline duration
