# WHS-83 Batch API Contract And State Machine

## Scope

This document is the contract baseline for Batch tasks after `WHS-83`.

In scope:
- `POST /api/v1/batches`
- `GET /api/v1/batches/{id}`
- `GET /api/v1/batches`
- `PUT /api/v1/batches/{id}`
- `PUT /api/v1/batches/{id}/quarantine`
- `PUT /api/v1/batches/{id}/release`
- `PATCH /api/v1/batches/{id}/status`
- Swagger summary and description alignment

Out of scope for `WHS-83` and handled by later tasks:
- `GET /api/v1/batches/{id}/traceability`
- `GET /api/v1/batches/expiring`
- `GET /api/v1/batches/fifo-recommendations`
- `GET /api/v1/batches/by-product/{productId}`
- dedicated audit log persistence and notification delivery

## Invariants

- `Batch.status` is system-owned. Client cannot choose arbitrary lifecycle status.
- Only products with `requires_batch_tracking = true` can have batches.
- `batch_number` must stay unique per product.
- `manufacturing_date` cannot be in the future.
- `expiry_date` must be after `manufacturing_date` when provided.
- All public Batch endpoints return `BaseResponse<T>`.

## State Machine

Actual enum in code:
- `AVAILABLE`
- `QUARANTINE`
- `EXPIRED`
- `RECALLED`

Allowed transitions:

| From | To | Endpoint / Actor | Notes |
|------|----|------------------|-------|
| `AVAILABLE` | `QUARANTINE` | `PUT /{id}/quarantine` | `reason` required |
| `QUARANTINE` | `AVAILABLE` | `PUT /{id}/release` | `release_notes` required |
| `AVAILABLE` | `EXPIRED` | scheduled expiry job | not a public manual API in this phase |
| `QUARANTINE` | `EXPIRED` | scheduled expiry job | if business later requires auto-expire from quarantine |

Blocked transitions:

| Transition | Error Code |
|------------|------------|
| any manual change through `PATCH /{id}/status` | `BATCH_011` |
| `QUARANTINE -> QUARANTINE` | `BATCH_013` |
| `EXPIRED -> QUARANTINE` | `BATCH_004` |
| `RECALLED -> QUARANTINE` | `BATCH_014` |
| non-`AVAILABLE` -> `QUARANTINE` | `BATCH_015` |
| non-`QUARANTINE` -> `AVAILABLE` via release | `BATCH_016` |
| expired-on-or-before-today batch -> release | `BATCH_017` |
| `RECALLED -> AVAILABLE` via release | `BATCH_018` |

## Request Ownership

### `POST /api/v1/batches`

Client-owned fields:
- `batch_number`
- `product_id`
- `manufacturing_date`
- `expiry_date`
- `supplier_batch_number`
- `notes`

System-owned fields:
- `id`
- `status`
- `created_at`
- `updated_at`
- `created_by`
- `updated_by`

Contract decision:
- remove `status` from create request
- service always sets `status = AVAILABLE`

### `PUT /api/v1/batches/{id}`

Client-owned mutable fields:
- `batch_number`
- `manufacturing_date`
- `expiry_date`
- `supplier_batch_number`
- `notes`

System-owned immutable fields:
- `id`
- `product_id`
- `status`

Contract decision:
- remove `id` from request body because resource id is owned by path variable

### `PUT /api/v1/batches/{id}/quarantine`

Request body:
- `reason` required, max `500`
- `expected_resolution_date` optional
- `notify_manager` optional, default `true`

Current implementation note:
- reason metadata is appended into `batch.notes` until a dedicated audit-log table or workflow is implemented

### `PUT /api/v1/batches/{id}/release`

Request body:
- `release_notes` required, max `1000`

Current implementation note:
- release metadata is appended into `batch.notes` until a dedicated audit-log table or workflow is implemented

### `PATCH /api/v1/batches/{id}/status`

Contract decision:
- public API remains mapped for compatibility but always rejects with `BATCH_011`
- manual lifecycle changes must use explicit workflow endpoints only

## Error Mapping

| API | Scenario | Error Code |
|-----|----------|------------|
| `POST /batches` | batch not found product | `PROD_001` |
| `POST /batches` | product not batch-tracked | `BATCH_009` |
| `POST /batches` | duplicate product + batch number | `BATCH_002` |
| `POST /batches` | manufacturing date future | `BATCH_005` |
| `POST /batches` | expiry before manufacturing | `BATCH_006` |
| `GET /batches/{id}` | batch not found | `BATCH_001` |
| `PUT /batches/{id}` | batch not found | `BATCH_001` |
| `PUT /batches/{id}` | duplicate product + batch number | `BATCH_002` |
| `PUT /batches/{id}` | manufacturing date future | `BATCH_005` |
| `PUT /batches/{id}` | expiry before manufacturing | `BATCH_006` |
| `PUT /batches/{id}/quarantine` | reserved inventory exists | `BATCH_007` |
| `PUT /batches/{id}/quarantine` | invalid current status | `BATCH_013`, `BATCH_004`, `BATCH_014`, `BATCH_015` |
| `PUT /batches/{id}/release` | invalid current status | `BATCH_016`, `BATCH_017`, `BATCH_018` |
| `PATCH /batches/{id}/status` | generic status change blocked | `BATCH_011` |

## Authorization Matrix

Business target from BA:

| API | Business actor |
|-----|----------------|
| create and update | Warehouse Staff |
| list and detail | Viewer |
| quarantine and release | Quality Controller or Warehouse Manager |
| traceability | Compliance Officer |

Current codebase constraint:
- global RBAC currently exposes generic roles `ADMIN`, `USER`, `MANAGER`
- `WHS-83` secures Batch endpoints with `isAuthenticated()` to avoid anonymous access
- fine-grained role mapping should be introduced only when Batch permissions are modeled consistently with the existing RBAC module

## Swagger Baseline

Swagger descriptions must state:
- create uses system-owned `AVAILABLE` status
- update does not accept `id` or `status` in body
- quarantine requires reason
- release requires release notes
- `PATCH /status` is intentionally blocked and should not be used by clients
