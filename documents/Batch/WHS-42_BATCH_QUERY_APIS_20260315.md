# WHS-42 Batch Query APIs - 2026-03-15

## Scope
Implemented 4 Batch read-side APIs on `feature/dunghd/WHS-42`:

- `GET /api/v1/batches/{id}/traceability`
- `GET /api/v1/batches/expiring`
- `GET /api/v1/batches/fifo-recommendations`
- `GET /api/v1/batches/by-product/{productId}`

## Business Rules

### Traceability
- Returns only data that already exists in the system.
- Assembles batch master data, current inventory snapshot, inbound history, outbound history, and stock movements.
- Workflow notes are parsed only from stored `batch.notes` lines starting with `[QUARANTINE]` or `[RELEASE]`.
- Does not fabricate a separate audit log because the system does not currently have a dedicated batch audit table.

### Expiring
- Uses configurable `threshold_days`, default `30`.
- Includes only batches with `expiry_date` within `[today, today + threshold]`.
- Includes only statuses `AVAILABLE` and `QUARANTINE`.
- Excludes batches with no physical stock remaining.
- Supports optional `warehouse_id` filter.
- Returns urgency bands:
  - `CRITICAL` when days to expiry `<= 7`
  - `WARNING` when days to expiry `<= 14`
  - `INFO` otherwise

### FIFO Recommendations
- Requires `product_id` and `warehouse_id`.
- Product must exist and have `requires_batch_tracking = true`.
- Warehouse must exist.
- Recommends only batches that are:
  - `AVAILABLE`
  - not expired on the query date
  - have positive available quantity in the requested warehouse
- Sort order follows the batch repository contract by manufacturing date, then expiry date.
- Response includes rank and inventory snapshot per recommended batch.

### By Product
- Requires a batch-tracked product.
- Supports optional `warehouse_id` filter.
- Without warehouse filter: returns all batches for the product with inventory summary.
- With warehouse filter: returns only batches that currently have inventory rows in that warehouse.

## Verification
- `./mvnw -DskipTests compile`
- `./mvnw test -DskipITs "-Dtest=org.demo.whs.service.impl.BatchServiceImplTest,org.demo.whs.controller.BatchControllerTest,org.demo.whs.service.impl.BatchQueryServiceImplTest,org.demo.whs.controller.BatchQueryControllerTest"`
