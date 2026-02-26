# Product Scope v1 (Backend) — WHS

## 1) Scope (v1 In-Scope)

> Planning principle: **risk-first**.  
> Security, data integrity, and performance/reliability foundations are prioritized before expanding into new operational modules.

### 1.1 Foundation Hardening (must complete in v1)

1. **Authentication & token safety**
   - Fix refresh-token validation logic defect (audit F-01).
   - Enforce JWT `type=accessToken` in auth filter for API access (audit F-02).
   - Remove weak JWT secret fallback; require `JWT_SECRET` in runtime config (audit F-06).

2. **Authorization correctness**
   - Standardize admin authorization checks (`hasRole('ADMIN')` or `hasAuthority('ROLE_ADMIN')`) (audit F-03).
   - Restrict email retry endpoint to admin role explicitly (audit F-04).

3. **Input/error contract baseline**
   - Add missing validation on high-risk request surfaces (search/paging/path-query bounds) (audit F-08).
   - Align 401/403 and global error response contracts for client consistency (audit F-09).

4. **Data integrity and schema hygiene**
   - Keep Flyway as single source for schema evolution (already enabled).
   - Correct migration metadata/comment mismatches that cause operational confusion (DB audit section 1).
   - Confirm entity-table mapping consistency for known anomalies (e.g., trailing table-name spaces, unmapped entities noted in DB audit).

5. **Performance and abuse protection baseline**
   - Keep Redis-backed rate limiting for auth endpoints as active control.
   - Preserve cache strategy on implemented UOM flows and verify eviction behavior under update/delete.

### 1.2 Implemented Functional Modules Included in v1

Based on current implementation status (system overview + module map), v1 includes modules with real business logic/API behavior:

1. **Auth & RBAC API**
   - Login, refresh-token flow, JWT validation pipeline, role-based endpoint protection.

2. **Master Data (implemented parts)**
   - Warehouse management APIs.
   - Location management APIs.
   - Product management APIs.
   - Units of Measure (UOM) APIs with Redis cache behavior.

3. **User Read APIs**
   - Manager list and user-profile read endpoints.

4. **Email/Notification module**
   - Sync/async send flows, email log lifecycle, retry/reprocess/statistics endpoints.
   - RabbitMQ integration and scheduler-based maintenance already in codebase.

5. **Operational readiness**
   - OpenAPI exposure, Actuator health/info, Flyway migration execution in deployment pipeline.

---

## 2) Non-Scope (Explicit Exclusions for v1)

### 2.1 Functional modules excluded from v1 delivery

The following modules are **schema-present/scaffold-present but not feature-complete**; therefore excluded from v1 committed scope:

1. **Inbound operations** (PO/ASN/Receiving/Putaway full workflows)
2. **Outbound operations** (SO/Allocation/Picking/Packing/Shipping full workflows)
3. **Inventory operations** (cycle count, reconciliation, transfer orchestration)
4. **Stock movement operational workflows** beyond existing schema/base scaffolds
5. **Batch/Lot advanced business flows** beyond current entity/repository skeleton

### 2.2 Advanced platform capabilities excluded from v1

1. Full **outbox pattern** for guaranteed post-commit broker publish.
2. Full **DLQ/DLX** topology and delayed retry orchestration for RabbitMQ.
3. Broad, domain-wide soft-delete framework (`deleted_at`, `is_deleted`, global repository filters).
4. New tenant/isolation redesign (current structure remains as-is in v1).
5. Large-scale reporting/analytics modules not already implemented.

---

## 3) Assumptions

1. **Architecture continuity**: backend remains single Spring Boot service (modular monolith style) with existing layer boundaries.
2. **Database continuity**: MySQL + Flyway remain mandatory for all schema changes.
3. **Auth model continuity**: JWT stateless authentication remains primary security approach.
4. **Infrastructure availability**:
   - Redis is available for rate limiting and current cache use.
   - RabbitMQ is available for current email async flow.
5. **Persona focus**: v1 business usage centers on:
   - Warehouse Admin
   - Warehouse Operator
6. **Scope discipline**: scaffolded modules are not considered “done” unless APIs + service rules + validations are implemented and testable.

---

## 4) Constraints

1. **Implementation reality constraint**
   - v1 scope must align with currently implemented modules; scaffold-only modules cannot be committed as deliverables.

2. **Security-first release constraint**
   - High-risk security defects (token validation, token type enforcement, authorization gaps, weak secret fallback) must be resolved before broadening business scope.

3. **Data integrity constraint**
   - No direct schema drift outside Flyway migrations.
   - Migration/entity mismatches identified in audit must be triaged and corrected before adding complex operational logic.

4. **Operational simplicity constraint**
   - Existing async model (email queue + scheduler) is retained in v1; no large asynchronous redesign committed.

5. **Capacity/time constraint**
   - v1 prioritizes stable, auditable foundations and implemented master-data capabilities over breadth of warehouse operations.

---

## 5) Personas (v1)

## 5.1 Warehouse Admin

- **Goals**
  - Manage master data (warehouse/location/product/UOM).
  - Control privileged operations (email retries/admin actions).
  - Monitor system health and auditability.
- **Pain points addressed in v1**
  - Inconsistent authorization behavior.
  - Security and error-contract gaps impacting admin operations.
  - Need stable baseline before scaling to inbound/outbound complexity.

## 5.2 Warehouse Operator

- **Goals**
  - Use reliable, low-friction APIs backed by accurate master data.
  - Perform daily operations without auth/session/rate-limit instability.
- **Pain points addressed in v1**
  - API reliability and predictable validation responses.
  - Consistent access behavior and reduced runtime defects from security flaws.

---

## 6) Non-Functional Requirements (NFRs) for v1

## 6.1 Security

1. Access-token enforcement:
   - 100% of authenticated API requests must reject JWTs whose `type != accessToken`.
2. Refresh flow correctness:
   - Valid, non-expired refresh tokens are accepted; invalid/expired/wrong-type tokens are rejected (automated test coverage required).
3. Authorization correctness:
   - Admin-only endpoints must return **403** for non-admin authenticated users in integration tests.
4. Secret management:
   - Application startup must fail if `JWT_SECRET` is absent or does not satisfy key-strength requirements.

## 6.2 Performance

1. Rate-limit controls:
   - Auth endpoints retain configured rate limits (e.g., login 5 requests/5 minutes per configured key policy).
2. API response baseline:
   - For implemented read-heavy master-data endpoints, p95 server response target ≤ **500ms** under normal internal load baseline.
3. Cache effectiveness:
   - UOM cached endpoints must show measurable latency reduction vs cold path in test environment (baseline benchmark documented).

## 6.3 Reliability

1. Schema reliability:
   - 100% schema changes executed through Flyway with versioned scripts (no manual production drift).
2. Email async resilience (current-model scope):
   - Failed sends are persisted with retry metadata and recoverable via existing retry/reprocess mechanisms.
3. Regression protection:
   - Security-critical flows (login/refresh/authz) covered by automated tests before release.

## 6.4 Observability

1. API errors:
   - 401/403/4xx/5xx responses follow consistent response envelope for alerting and client handling.
2. Security/audit visibility:
   - Authentication and authorization failures are logged with correlation-safe metadata (without exposing secrets/tokens).
3. Async monitoring baseline:
   - Email queue processing outcomes (success/failure/retry) remain queryable from email log data.

## 6.5 Maintainability

1. Standards consistency:
   - Validation, error handling, and authorization patterns are standardized across implemented controllers.
2. Migration readability:
   - Flyway script metadata/comments match actual module content to reduce maintenance risk.
3. Scope traceability:
   - v1 roadmap and excluded scaffold modules remain explicitly documented to prevent scope confusion.

---

## 7) Scope Alignment Note (Implemented vs Scaffold)

- **Implemented and committed in v1**: Auth/RBAC, Warehouse, Location, Product, UOM, User-read APIs, Email module, and foundational hardening.
- **Scaffold/schema-only and excluded from v1 feature commitment**: inbound, outbound, inventory, stock movement, batch advanced business workflows.

This alignment is intentional to ensure v1 is secure, stable, and operable before expanding functional breadth.
