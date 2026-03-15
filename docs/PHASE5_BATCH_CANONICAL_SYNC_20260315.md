# Phase 5 Canonical + Batch Sync Audit

Date: 2026-03-15
Scope: canonicalize Phase 5 and audit Batch module across code, docs, and Jira
Status: Working review baseline

---

## 1. Purpose

This document is the canonical reference for two questions that are currently inconsistent across the repository:

1. What `Phase 5` means in the current implementation baseline
2. How far `Batch` is actually implemented versus what Jira and documents say

This document does not overwrite historical roadmap documents. It defines the current source of truth to sync implementation, Jira, and API inventory.

---

## 2. Canonical Decision for Phase 5

## 2.1 Current canonical meaning

As of 2026-03-15, `Phase 5` should be interpreted as:

`Inventory + Inbound hardening after inbound core is already usable`

It is not a single Jira task.

It is a grouped stabilization phase composed from:

- inbound confirm hardening
- inbound receipt line regression hardening
- inventory hardening for outbound readiness
- API and documentation contract sync

## 2.2 Source precedence

Use sources in this order when Phase 5 is referenced:

1. `docs/API_INVENTORY_COMPREHENSIVE.md`
2. `docs/08-api-inventory-implementation-plan.md`
3. `documents/Inbound/INBOUND_IMPLEMENTATION_ROADMAP_20260309.md`
4. Older planning documents only for historical traceability

## 2.3 Why the confusion exists

Historical numbering changed between documents:

| Source | Meaning of Phase 5 |
|---|---|
| `documents/Inbound/INBOUND_MODULE_REVIEW_AND_PLAN_20260307.md` | `WHS-57` Confirm Receipt |
| `documents/Inbound/INBOUND_IMPLEMENTATION_ROADMAP_20260309.md` | Hardening |
| `docs/API_INVENTORY_COMPREHENSIVE.md` | Harden Inventory + Inbound |

Therefore:

- `WHS-57` is the old `Phase 5` in one planning document
- current canonical `Phase 5` is the hardening phase after `WHS-57` and `WHS-58`

## 2.4 Jira mapping for canonical Phase 5

There is no single Jira issue that fully represents current `Phase 5`.

Use this mapping:

| Canonical work item | Closest Jira scope | Jira status | Sync status |
|---|---|---|---|
| Inbound confirm core | `WHS-57` | Done | implemented |
| Inbound receipt lines hardening | `WHS-58` | Done | implemented but still needs regression confidence review |
| Inventory decrease for outbound readiness | `WHS-17` | In Progress | real open gap |
| Inventory module parent | `WHS-19` | In Progress | parent only, no hardening subtasks |
| Hardening docs/auth/tests sync | no dedicated Jira issue found | n/a | missing Jira breakdown |

## 2.5 Canonical Phase 5 requirements

Current Phase 5 scope is:

| Priority | Work item | Canonical requirement |
|---|---|---|
| P0 | Inbound integration test hardening | Cover `PO -> receipt -> receipt lines -> confirm`, partial receipt, quarantine, rollback, concurrent confirm risk |
| P0 | Inbound Receipt Lines regression hardening | Cover split-line, duplicate business dimension, remaining quantity guard, batch and quality validation, draft-only mutation |
| P1 | Inventory hardening | Finalize `increase` public/internal contract, complete `decrease`, review reserve and unreserve auth + idempotency |
| P1 | API and doc sync | Align Swagger, error code matrix, request and response examples, and controller contract with real code |

## 2.6 Implementation note

The current codebase already absorbed part of this hardening:

- quarantine is excluded from inventory available quantity
- reserve path already has idempotency ledger support
- inbound confirm and receipt line rules already have meaningful unit tests

But the phase is not complete because:

- `decrease` is still missing
- inbound hardening is not represented as dedicated Jira work
- contract and review items are still inconsistent across controller, docs, and Jira

---

## 3. Batch Canonical Review Frame

Before changing Batch implementation, keep these review anchors locked:

| Item | Canonical value |
|---|---|
| Invariant being protected | batch lifecycle must not bypass status rules; batch-tracked product ownership must remain valid; unavailable batches must not re-enter shipping availability incorrectly |
| State transitions being changed | `AVAILABLE -> QUARANTINE`, `QUARANTINE -> AVAILABLE`, auto/controlled moves to `EXPIRED` or `RECALLED` |
| Affected aggregate root | `Batch` |
| Main regression points | invalid transition bypass, reserved stock quarantine, expired batch release, contract mismatch between controller and BA/Jira, missing query APIs for traceability and expiry |
| Minimal verification strategy | controller tests, service tests for transitions, repository/integration tests for queries, contract review for response envelope and auth |

---

## 4. Batch Source of Truth

## 4.1 Business source

Business baseline:

- `documents/Batch/BA_MODULE_03_BATCH_MANAGEMENT.md`
- `documents/Batch/DB_MODULE_03_BATCH.md`

## 4.2 Jira source

Current Jira structure:

| Jira | Summary | Status |
|---|---|---|
| `WHS-35` | Batch API Remaining Completion | In Progress |
| `WHS-83` | Design Batch API contract + state machine | To Do |
| `WHS-84` | Migration + index plan for Batch query APIs | To Do |
| `WHS-41` | Implement Batch lifecycle APIs | To Do |
| `WHS-42` | Implement Batch traceability APIs | To Do |
| `WHS-86` | Testing plan + coverage for Batch APIs | To Do |
| `WHS-85` | Final review + release readiness | To Do |

Important consequence:

`WHS-41` is still `To Do`, but the codebase already contains partial lifecycle implementation. Jira is not synchronized with real code state.

---

## 5. Batch API Inventory: Canonical Expectation

## 5.1 Business and Jira expected APIs

If business doc plus Jira are treated as the target state, Batch has `11` APIs, not `10`.

| # | Method | Endpoint | Expected source |
|---|---|---|---|
| 1 | `POST` | `/api/v1/batches` | implemented core CRUD |
| 2 | `GET` | `/api/v1/batches` | implemented core CRUD |
| 3 | `GET` | `/api/v1/batches/{id}` | implemented core CRUD |
| 4 | `PUT` | `/api/v1/batches/{id}` | lifecycle scope |
| 5 | `PATCH` or controlled alternative | `/api/v1/batches/{id}/status` | present in code and API inventory, but not clearly governed in Jira scope |
| 6 | `PUT` | `/api/v1/batches/{id}/quarantine` | lifecycle scope |
| 7 | `PUT` | `/api/v1/batches/{id}/release` | lifecycle scope |
| 8 | `GET` | `/api/v1/batches/{id}/traceability` | `WHS-42` |
| 9 | `GET` | `/api/v1/batches/expiring` | `WHS-42` |
| 10 | `GET` | `/api/v1/batches/fifo-recommendations` | `WHS-42` and BA |
| 11 | `GET` | `/api/v1/batches/by-product/{productId}` | `WHS-42` |

## 5.2 Current mismatch with API inventory doc

`docs/API_INVENTORY_COMPREHENSIVE.md` currently counts Batch as `7/10` and does not include `GET /api/v1/batches/fifo-recommendations`.

Canonical correction:

- if Batch follows BA and Jira target state, module target size is `11`
- if FIFO is intentionally moved out of Batch scope later, that decision must be written explicitly in Jira and docs

Until that decision exists, treat FIFO recommendation as an open Batch API gap.

---

## 6. Batch Code Reality Check

## 6.1 What is implemented in code

Implemented endpoints found in code:

- `POST /api/v1/batches`
- `GET /api/v1/batches`
- `GET /api/v1/batches/{id}`
- `PUT /api/v1/batches/{id}`
- `PATCH /api/v1/batches/{id}/status`
- `PUT /api/v1/batches/{id}/quarantine`
- `PUT /api/v1/batches/{id}/release`

Implemented business validations found in service:

- product must support batch tracking on create
- unique `(product_id, batch_number)`
- manufacturing date cannot be future
- expiry date must be after manufacturing date
- quarantine blocks reserved stock
- quarantine blocks already quarantined, recalled, expired, and non-available states
- release only allowed from `QUARANTINE`
- release blocks recalled and expired batches

## 6.2 What is not implemented in code

Missing endpoints:

- `GET /api/v1/batches/{id}/traceability`
- `GET /api/v1/batches/expiring`
- `GET /api/v1/batches/fifo-recommendations`
- `GET /api/v1/batches/by-product/{productId}`

Missing delivery quality:

- no Batch controller test found
- no Batch integration test found
- no Swagger annotations found on `BatchController`
- no method-level authorization found on `BatchController`

---

## 7. Batch Sync Gaps by Capability

| Capability | Code state | Business/Jira sync | Canonical verdict |
|---|---|---|---|
| Create batch | implemented | partial mismatch | keep implemented, fix request contract and auth/doc/test gaps |
| Get batch by id | implemented | mostly aligned | needs auth/doc/test coverage |
| List batches | implemented with only `page` and `size` | not aligned with BA filter expectations and unused `SearchBatchRequest` | incomplete |
| Update batch | implemented | partial mismatch | redundant `id` in body and no auth/swagger/tests |
| Generic change status | implemented | not clearly aligned with Jira design | high-risk bypass of transition matrix |
| Quarantine batch | implemented | partial mismatch | missing reason payload, response envelope mismatch, no auth/swagger/controller tests |
| Release batch | implemented | partial mismatch | missing release notes payload, response envelope mismatch, no auth/swagger/controller tests |
| Traceability | missing | expected by BA and Jira | open gap |
| Expiring query | missing | expected by BA and Jira | open gap |
| FIFO recommendations | missing | expected by BA and Jira but omitted by API inventory doc | open gap plus doc mismatch |
| Batches by product | missing | expected by BA and Jira | open gap |

---

## 8. Concrete Mismatches That Must Be Resolved

## 8.1 Contract mismatches in current code

| Area | Current state | Canonical issue |
|---|---|---|
| `CreateBatchRequest` | requires `status` from client | business says status defaults to `AVAILABLE`; service ignores client value |
| `UpdateBatchRequest` | requires `id` in request body | path already owns batch ID; body `id` is redundant and drift-prone |
| `quarantine` and `release` responses | return raw `BatchResponse` | project standard requires `BaseResponse<T>` |
| `PATCH /status` | directly sets any status from request | bypasses lifecycle rules and weakens state machine control |
| list query contract | controller only supports `page` and `size` | BA and search DTO imply richer filters |

## 8.2 Security and documentation gaps

| Area | Current state | Canonical issue |
|---|---|---|
| Authorization | no `@PreAuthorize` found on Batch controller | does not satisfy BA/Jira role matrix and security review expectations |
| Swagger/OpenAPI | no `@Operation` or `@ApiResponse` found | does not satisfy `WHS-41`, `WHS-42`, `WHS-85` acceptance criteria |
| Response envelope consistency | mixed `BaseResponse<T>` and raw `BatchResponse` | inconsistent public API contract |

## 8.3 Testing gaps

Observed test coverage:

- create happy path and some create validation
- quarantine happy path and some quarantine validation

Not covered in current test file:

- controller validation and status code behavior
- list and get-by-id
- update path
- generic status-change path
- release happy path and failure paths
- integration coverage for queries and persistence behavior

This means `WHS-86` is correctly still `To Do`.

## 8.4 DB and migration sync gaps

| Area | Current state | Canonical issue |
|---|---|---|
| migration file header | header version does not match filename | migration audit drift still exists |
| query API indexes | migration has base indexes only | no evidence that query/index plan for expiring, FIFO, by-product has been finalized against implementation |
| DB doc | describes additional query patterns not yet implemented in code | DB design and code are ahead/behind in different places |

---

## 9. Jira Sync Verdict

## 9.1 `WHS-41` Lifecycle APIs

Current verdict: `Partially implemented in code, not ready for Done`

Reason:

- update, quarantine, release endpoints exist
- but acceptance criteria are not met yet because auth, Swagger, response consistency, and test coverage are not complete
- generic `PATCH /status` also muddies the state machine boundary

Recommended Jira action:

- keep `WHS-41` open
- update description or comment that code is partially present
- define whether `PATCH /status` is an accepted API or should be removed/restricted

## 9.2 `WHS-42` Traceability APIs

Current verdict: `Correctly open`

Reason:

- all four query APIs are still missing from code
- FIFO recommendation is also still missing and must be tracked explicitly

## 9.3 `WHS-83` Design contract

Current verdict: `Still needed`

Reason:

- current code contract conflicts with BA expectations in multiple places
- response envelope, payload ownership, transition control, and auth matrix are not yet canonically locked

## 9.4 `WHS-84` Migration and index plan

Current verdict: `Still needed`

Reason:

- lifecycle code exists, but query-side index and migration decisions for expiring, FIFO, and traceability are not yet reflected as finalized implementation work

## 9.5 `WHS-86` Testing

Current verdict: `Still needed`

Reason:

- only partial service-level unit tests exist
- controller and integration coverage are missing

## 9.6 `WHS-85` Final review

Current verdict: `Correctly open`

Reason:

- design, migration, query APIs, and testing are not complete yet

---

## 10. Canonical Batch Completion Backlog

Use this order to reach business-correct synchronization:

1. Lock contract decisions in `WHS-83`
   - remove client-owned `status` from create request
   - remove body `id` from update request
   - decide fate of `PATCH /status`
   - standardize `BaseResponse<T>` for all endpoints
   - define auth matrix per endpoint

2. Finish lifecycle quality for `WHS-41`
   - add auth
   - add Swagger
   - add reason or notes payload where business requires
   - add controller tests
   - add release tests

3. Finalize query plan in `WHS-84`
   - confirm filter contract for list
   - confirm index sufficiency for expiring, FIFO, traceability, by-product
   - fix migration header drift

4. Implement `WHS-42`
   - traceability
   - expiring
   - FIFO recommendations
   - by-product

5. Execute `WHS-86`
   - controller tests
   - service tests
   - integration tests
   - verify error-code and response schema behavior

6. Close `WHS-85`
   - sync Jira statuses
   - sync `docs/API_INVENTORY_COMPREHENSIVE.md`
   - close remaining contract drift

---

## 11. Final Canonical Statements

Use these statements going forward:

- `Phase 5` currently means `Inventory + Inbound hardening`, not `WHS-57` alone.
- `WHS-57` is core confirm receipt and is already done.
- Batch lifecycle code exists, but Batch is not business-complete.
- `WHS-41` should not be closed yet.
- Batch target scope is currently `11 APIs` unless FIFO is explicitly moved out of the module by decision record.
- `docs/API_INVENTORY_COMPREHENSIVE.md` should be updated later using this document as the reference baseline.

