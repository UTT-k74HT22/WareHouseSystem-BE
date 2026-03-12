# API Inventory Implementation Plan (Jira-driven)

Updated: 2026-03-12

## 1. Scope baseline

- Source of truth for API coverage: `docs/API_INVENTORY_COMPREHENSIVE.md`
- Code scan completed against current controllers in `src/main/java/org/demo/whs/controller`
- Service alignment checked for high-impact modules:
  - `InventoryServiceImpl`
  - `InboundReceiptsServiceImpl`
- Planning baseline moved from "controller exists" to:
  - endpoint exists
  - service implementation exists
  - business flow is usable end-to-end

## 2. Current implementation snapshot

### 2.1. Completed and usable now

- Master data core:
  - Warehouse
  - Location
  - Product
  - UOM
  - Business Partner
  - Category
  - Employee
- Inventory core:
  - list inventories
  - summary by product
  - by-location
  - check availability
  - reserve
  - unreserve
  - increase
- Stock mutation modules:
  - Stock Adjustments
  - Stock Transfers
- Inbound chain completed at core level:
  - Purchase Orders
  - Purchase Order Lines
  - Inbound Receipts
  - Confirm Receipt

### 2.2. Partially done

- Batch:
  - create
  - list
  - detail
  - change status
- Stock Movements:
  - list
  - detail
  - by reference

### 2.3. Still shell / not implemented

- Inbound Receipt Lines
- Sales Orders
- Sales Order Lines
- Outbound Shipments
- Outbound Shipment Lines
- Reporting

## 3. Jira map by current status

### Done in current baseline

- `WHS-53` Purchase Orders CRUD
- `WHS-54` Purchase Order confirm
- `WHS-55` Purchase Order Lines
- `WHS-56` Inbound Receipts CRUD
- `WHS-57` Confirm Receipt
- Existing inventory/stock tasks under `WHS-19`, `WHS-20`, `WHS-25` that already landed in code

### Remaining / next execution pool

- `WHS-58` Inbound Receipt Lines
- `WHS-59`, `WHS-60` Sales Orders
- `WHS-61` Sales Order Lines
- `WHS-62`, `WHS-63` Outbound Shipments
- `WHS-65` Outbound Shipment Lines
- `WHS-41`, `WHS-42` Batch enhancements
- `WHS-64`, `WHS-66` Stock Movements enhancements
- `WHS-67`, `WHS-68`, `WHS-69` Reporting
- `WHS-29`, `WHS-30` remaining auth endpoints

## 4. Delivery principles

### 4.1. Planning rule

Before each phase, lock:

- request/response DTO
- state machine
- validation rules
- error codes
- side effects
- ownership of transaction boundary

### 4.2. Service rule

- Controller stays thin
- Transaction logic stays in service
- Inventory mutation and movement write must remain atomic
- Derived fields must be recomputed, never trusted from client

### 4.3. Verification rule

Each phase must end with:

- unit tests for service rules
- integration tests for main transaction
- docs update
- manual review of auth and invariants

## 5. Recommended execution order

### Phase A — Inbound hardening and remaining line API

**Goal:** close the last inbound gap and stabilize the current inbound/inventory baseline before starting outbound.

**Scope:**

- `WHS-58` Inbound Receipt Lines CRUD
- harden current `WHS-56/57`
- sync docs and examples

**Deliverables:**

- `POST /api/v1/inbound-receipt-lines`
- `PUT /api/v1/inbound-receipt-lines/{id}`
- `DELETE /api/v1/inbound-receipt-lines/{id}`
- line validation:
  - only mutate when receipt is `DRAFT`
  - product/location/PO line binding valid
  - batch required only when `requiresBatchTracking = true`
  - quarantine requires notes
- receipt detail returns line data consistently
- integration tests for:
  - draft line mutations
  - confirm after line edits
  - rollback when one line invalid
  - partial receipt
  - quarantine receipt

**Acceptance criteria:**

- inbound chain works end-to-end from `PO -> IR -> IRL -> confirm`
- no direct inventory mutation outside confirm for inbound flow
- docs are aligned with controller and service behavior

**Suggested effort:** 4-5 days

### Phase B — Inventory hardening for outbound readiness

**Goal:** make current inventory APIs safe to reuse for outbound chain.

**Scope:**

- review `increase` public exposure
- define `decrease` contract
- harden reserve/unreserve for outbound reuse
- finalize idempotency and invariant checks

**Deliverables:**

- decision record:
  - keep `POST /inventories/increase` public or move to internal-only usage
- design + implement `POST /inventories/decrease`
- add tests for:
  - `available = onHand - quarantine - reserved`
  - reserve/unreserve race conditions
  - decrease not allowed below available stock
- clarify movement write ownership for reserve vs decrease flows

**Acceptance criteria:**

- outbound modules can consume inventory APIs without redesign
- decrease flow preserves movement invariant and stock non-negative rule

**Suggested effort:** 2-3 days

### Phase C — Sales order foundation

**Goal:** build commercial demand side before physical shipment execution.

**Scope:**

- `WHS-59`, `WHS-60` Sales Orders
- `WHS-61` Sales Order Lines

**Deliverables:**

- Sales Order header CRUD
- confirm/cancel lifecycle
- Sales Order line CRUD
- total recomputation from lines
- reserve inventory on confirm
- unreserve on cancel where business rule applies

**Acceptance criteria:**

- `SalesOrder` becomes aggregate root
- header/line mutation blocked outside `DRAFT`
- confirm creates inventory reservations atomically

**Suggested effort:** 5-6 days

### Phase D — Outbound shipment execution

**Goal:** implement physical outbound after demand has been confirmed.

**Scope:**

- `WHS-62`, `WHS-63` Outbound Shipments
- `WHS-65` Outbound Shipment Lines
- inventory decrease integration

**Deliverables:**

- shipment CRUD
- shipment line CRUD
- picking state
- shipment confirm
- inventory decrease
- stock movement write

**Acceptance criteria:**

- shipment confirm consumes reserved/available stock correctly
- no negative stock
- stock movement traces outbound references clearly

**Suggested effort:** 6-7 days

### Phase E — Batch and traceability enhancement

**Goal:** strengthen quality, compliance, and investigation capabilities.

**Scope:**

- `WHS-41`, `WHS-42`
- `WHS-64`, `WHS-66`

**Deliverables:**

- batch update
- quarantine / release actions
- expiring batches query
- batches by product
- stock movements by product
- stock movements by batch
- forward/backward traceability

**Acceptance criteria:**

- batch lifecycle is consistent with inbound quarantine
- operations team can trace inventory history without raw DB queries

**Suggested effort:** 4-5 days

### Phase F — Reporting and residual auth

**Goal:** complete read-heavy and platform-completeness features.

**Scope:**

- `WHS-67`, `WHS-68`, `WHS-69`
- `WHS-29`, `WHS-30`

**Deliverables:**

- reporting controller and report request model
- on-demand reports
- async reporting workflow
- scheduled reporting
- auth verify-email and resend-verification

**Acceptance criteria:**

- no reporting endpoint introduced before read paths are indexed and paginated appropriately
- auth flow complete for registration lifecycle

**Suggested effort:** 6-8 days

## 6. Recommended PR slicing

### For each API slice

- controller
- DTOs
- service interface
- service implementation
- repository changes if needed
- tests
- doc update

### For each business-transaction slice

- lock/validation helpers first
- mutation logic second
- stock movement and side effects third
- tests last before review

## 7. High-risk checkpoints

These must be reviewed explicitly before merging:

- inventory mutation under concurrency
- reserve/decrease consistency
- PO / receipt / shipment lifecycle transitions
- quarantine availability exclusion
- batch-to-product ownership validation
- location-to-warehouse ownership validation

## 8. Definition of done for the next phase

Phase A is considered done only when:

- `InboundReceiptLinesController` is no longer shell-only
- line CRUD is fully implemented
- inbound integration tests exist for confirm transaction
- docs remain aligned with actual endpoints
- no open blocker remains on current inbound confirm behavior
