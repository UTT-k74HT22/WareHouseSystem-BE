# Phase 5 Canonical + Batch Sync Audit

Date: 2026-03-15
Scope: canonicalize Phase 5 and keep Batch code, docs, and Jira aligned
Status: refreshed after `WHS-83`, `WHS-41`, `WHS-42`, `WHS-84`, `WHS-86`, and ongoing `WHS-85`

---

## 1. Purpose

This document is the current source of truth for:

1. what `Phase 5` means in the Inventory/Inbound roadmap
2. what `Batch` scope is now considered complete in code
3. which gaps are still outside `WHS-35` release readiness

This document supersedes earlier Batch sync notes created before the Batch branches were completed.

---

## 2. Canonical Decision for Phase 5

As of 2026-03-15, `Phase 5` means:

`Inventory + Inbound hardening after inbound core is already usable`

It is not a single Jira task.

Canonical source precedence remains:

1. `docs/API_INVENTORY_COMPREHENSIVE.md`
2. `docs/08-api-inventory-implementation-plan.md`
3. `documents/Inbound/INBOUND_IMPLEMENTATION_ROADMAP_20260309.md`
4. older planning documents only for historical traceability

Current canonical Phase 5 work items:

- inbound confirm hardening
- inbound receipt line regression hardening
- inventory hardening for outbound readiness
- API and documentation contract sync

Phase 5 is still not fully closed because Inventory `decrease` remains open.

---

## 3. Batch Delivery Snapshot

### 3.1 Jira structure used for Batch

| Jira | Summary | Jira status | Code/doc status |
|---|---|---|---|
| `WHS-35` | Batch API Remaining Completion | In Progress | parent scope still open until Jira sync is done |
| `WHS-83` | Design Batch API contract + state machine | In Progress | implemented on branch baseline |
| `WHS-84` | Migration + index plan for Batch query APIs | Done | synced |
| `WHS-41` | Implement Batch lifecycle APIs | Done | synced |
| `WHS-42` | Implement Batch traceability APIs | In Progress | code complete, Jira not yet updated |
| `WHS-86` | Testing plan + coverage for Batch APIs | In Progress | code complete, Jira not yet updated |
| `WHS-85` | Final review + release readiness | In Progress | current working branch |

### 3.2 Branch and commit baseline

| Task | Branch | Commit | Result |
|---|---|---|---|
| `WHS-83` | `feature/dunghd/WHS-83` | `3c75094` | contract + state machine locked |
| `WHS-41` | `feature/dunghd/WHS-41` | `9b613ac` | lifecycle APIs hardened |
| `WHS-42` | `feature/dunghd/WHS-42` | `1410afd` | traceability/expiring/FIFO/by-product implemented |
| `WHS-84` | `feature/dunghd/WHS-84` | `32bba69` | migration + index plan added |
| `WHS-86` | `feature/dunghd/WHS-86` | `993b58c` | controller/service/repository coverage added |
| `WHS-85` | `feature/dunghd/WHS-85` | working tree | final review + doc sync + filtered list hardening |

Important sync note:

Jira still understates actual completion for `WHS-42` and `WHS-86`. Manual Jira update is still required.

---

## 4. Batch API Scope: Canonical Target State

Batch target size is `11` APIs.

| # | Method | Endpoint | Current state |
|---|---|---|---|
| 1 | `POST` | `/api/v1/batches` | implemented |
| 2 | `GET` | `/api/v1/batches` | implemented with business filters + quantity summary |
| 3 | `GET` | `/api/v1/batches/{id}` | implemented with quantity summary |
| 4 | `PUT` | `/api/v1/batches/{id}` | implemented |
| 5 | `PATCH` | `/api/v1/batches/{id}/status` | implemented as blocked generic transition (`BATCH_011`) |
| 6 | `PUT` | `/api/v1/batches/{id}/quarantine` | implemented |
| 7 | `PUT` | `/api/v1/batches/{id}/release` | implemented |
| 8 | `GET` | `/api/v1/batches/{id}/traceability` | implemented |
| 9 | `GET` | `/api/v1/batches/expiring` | implemented |
| 10 | `GET` | `/api/v1/batches/fifo-recommendations` | implemented |
| 11 | `GET` | `/api/v1/batches/by-product/{productId}` | implemented |

Canonical conclusion:

`Batch = 11/11 implemented within WHS-35 scope`

---

## 5. Batch Business Rules Now Locked in Code

### 5.1 Lifecycle rules

- create always defaults status to `AVAILABLE`
- generic `PATCH /status` is blocked by `BATCH_011`
- only `AVAILABLE` batches can move to `QUARANTINE`
- reserved stock blocks quarantine
- only `QUARANTINE` batches can be released
- expired or recalled batches cannot be released

### 5.2 Query rules

- `GET /api/v1/batches` supports `keyword`, `product_id`, `warehouse_id`, `status`, `manufacturing_date_from/to`, `expiry_date_from/to`
- list/detail responses include `total_on_hand_quantity`, `total_quarantine_quantity`, `total_reserved_quantity`, `total_available_quantity`
- FIFO recommendations only return eligible `AVAILABLE` batches with positive available quantity and non-expired date semantics
- expiring query excludes batches without physical stock
- traceability reads inbound, outbound, inventory, movement, and workflow notes together

### 5.3 Error contract

- lifecycle bypass is mapped to `BATCH_011`
- invalid Batch filter date range is mapped to `BATCH_019`
- response envelope uses `BaseResponse<T>` on controller layer

---

## 6. Verification Baseline

Batch verification currently present:

- `BatchControllerTest`
- `BatchQueryControllerTest`
- `BatchServiceImplTest`
- `BatchQueryServiceImplTest`
- `BatchRepositoryQueryIntegrationTest`
- `InventoryRepositoryBatchQueryIntegrationTest`
- `BatchTraceabilityRepositoryIntegrationTest`

Current test command used on `WHS-85`:

`./mvnw test -DskipITs "-Dtest=org.demo.whs.service.impl.BatchServiceImplTest,org.demo.whs.controller.BatchControllerTest,org.demo.whs.service.impl.BatchQueryServiceImplTest,org.demo.whs.controller.BatchQueryControllerTest,org.demo.whs.repository.BatchRepositoryQueryIntegrationTest,org.demo.whs.repository.InventoryRepositoryBatchQueryIntegrationTest,org.demo.whs.repository.BatchTraceabilityRepositoryIntegrationTest"`

Result on 2026-03-15:

- `41 tests`
- `0 failures`
- `0 errors`

---

## 7. Remaining Limits Outside WHS-35 Scope

These are not release blockers for current Batch scope, but must not be confused with completed work:

- granular RBAC permission matrix for Batch endpoints is not yet enforced with endpoint-specific authorities; current protection remains authenticated access baseline
- auto-expiry scheduler and expiry alert delivery from BA feature 3 are not delivered in `WHS-35`
- Stock Movements analytics/export work remains open
- outbound foundation is still open, so Batch is complete as a module slice, not as the whole warehouse outbound chain

---

## 8. Canonical Verdict

### Phase 5

- canonical meaning remains `Inventory + Inbound hardening`
- Batch completion does not close Phase 5 because Inventory `decrease` remains open

### Batch

- Batch module is now functionally complete for `WHS-35` scope
- code, tests, and API inventory have been aligned to `11/11` APIs
- the main remaining work is Jira synchronization and the `WHS-85` release-readiness closeout note
