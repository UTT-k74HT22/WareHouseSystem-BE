# WHS-84 Batch Migration + Index Plan - 2026-03-15

## Decision
Migration required: `Yes`

## Why
`WHS-42` introduced 4 Batch read-side APIs with new filter/sort patterns:

- traceability by `batch_id`
- expiring by `status + expiry_date` with optional `warehouse_id`
- FIFO by `product_id + warehouse_id + batch_id`
- by-product by `product_id + manufacturing_date + expiry_date`

Existing indexes covered parts of this surface, but not the full composite access paths now used by the Batch service.

## Existing Coverage
Already present before this task:

- `batches`
  - `idx_batch_product_id`
  - `idx_batch_status`
  - `idx_batch_expiry_date`
  - `idx_batch_manufacturing_date`
  - `idx_batch_fifo (product_id, status, manufacturing_date, expiry_date)`
- `inventory`
  - `idx_inventory_batch`
  - `idx_inventory_dimension_lookup (product_id, warehouse_id, location_id, batch_id)`
- `stock_movements`
  - `idx_movement_batch (batch_id, movement_date DESC)`

## Gaps Found

### 1. `batches` by-product timeline query
Current repository method:
- `findByProductIdOrderByManufacturingDateAscExpiryDateAscCreatedAtAsc`

Gap:
- `idx_batch_fifo` places `status` between `product_id` and timeline columns.
- The by-product query does not filter by `status`, so the existing index is not a clean fit.

Decision:
- Add `idx_batch_product_timeline (product_id, manufacturing_date, expiry_date, created_at)`

### 2. `batches` expiring query
Current repository method:
- `findExpiringBatches(startDate, endDate, statuses)`

Gap:
- Existing single-column indexes on `status` and `expiry_date` are weaker than a composite access path for the combined filter + sort.

Decision:
- Add `idx_batch_status_expiry_timeline (status, expiry_date, manufacturing_date, created_at)`

### 3. `inventory` expiring query with warehouse filter
Current repository method:
- `findByWarehouseIdAndBatchIdIn`

Gap:
- Existing single-column indexes on `warehouse_id` and `batch_id` force less efficient filtering than a composite warehouse+batch lookup.

Decision:
- Add `idx_inventory_warehouse_batch (warehouse_id, batch_id)`

### 4. `inventory` FIFO and by-product query path
Current repository method:
- `findByProductIdAndWarehouseIdAndBatchIdIn`

Gap:
- `idx_inventory_dimension_lookup` includes `location_id` before `batch_id`, which weakens support for queries that do not filter by location.

Decision:
- Add `idx_inventory_product_warehouse_batch (product_id, warehouse_id, batch_id)`

### 5. Inbound and outbound traceability lines
Current repository methods:
- `findByBatchIdOrderByCreatedAtDesc` on inbound receipt lines
- `findByBatchIdOrderByCreatedAtDesc` on outbound shipment lines

Gap:
- Batch lookups should use a composite index aligned with both filter and sort.

Decision:
- Add `idx_inbound_receipt_lines_batch_created_at (batch_id, created_at DESC)`
- Add `idx_outbound_shipment_lines_batch_created_at (batch_id, created_at DESC)`

## Constraint Decision
No new DB constraint is added for quarantine/release lifecycle transitions.

Rationale:
- Transition validity depends on reserved stock existence and expiry semantics.
- Those checks are cross-row / workflow-aware and are already enforced safely in `BatchServiceImpl`.
- Encoding them as simple row-level DB constraints would be brittle and likely incomplete.

## Files
- Migration: `src/main/resources/db/migration/V20260315_01__add_batch_query_indexes.sql`

## Risk Note
- Added indexes improve read paths for Batch queries but increase write cost on affected tables.
- The chosen set is limited to the exact new query shapes introduced by `WHS-42`.
- No existing index is dropped in this task to avoid accidental regression on unrelated flows.

## Rollback Note
If rollback is required, drop these indexes:

- `idx_batch_product_timeline`
- `idx_batch_status_expiry_timeline`
- `idx_inventory_warehouse_batch`
- `idx_inventory_product_warehouse_batch`
- `idx_inbound_receipt_lines_batch_created_at`
- `idx_outbound_shipment_lines_batch_created_at`
