-- ============================================================================
-- Flyway Migration: Add Batch Query Indexes
-- Version: V20260315_01
-- Description: Add composite indexes for Batch traceability, expiring, FIFO, and by-product queries
-- Rollback note: Drop the indexes added in this file if query regression or write amplification is observed.
-- ============================================================================

-- batches: support by-product timeline reads without relying on status in the index prefix.
CREATE INDEX idx_batch_product_timeline
    ON batches (product_id, manufacturing_date, expiry_date, created_at);

-- batches: support expiring reads by status + expiry window with stable ordering.
CREATE INDEX idx_batch_status_expiry_timeline
    ON batches (status, expiry_date, manufacturing_date, created_at);

-- inventory: support expiring queries filtered by warehouse and batch.
CREATE INDEX idx_inventory_warehouse_batch
    ON inventory (warehouse_id, batch_id);

-- inventory: support FIFO and by-product queries filtered by product, warehouse, and batch.
CREATE INDEX idx_inventory_product_warehouse_batch
    ON inventory (product_id, warehouse_id, batch_id);

-- inbound traceability: support batch lookup ordered by creation time.
CREATE INDEX idx_inbound_receipt_lines_batch_created_at
    ON inbound_receipt_lines (batch_id, created_at DESC);

-- outbound traceability: support batch lookup ordered by creation time.
CREATE INDEX idx_outbound_shipment_lines_batch_created_at
    ON outbound_shipment_lines (batch_id, created_at DESC);

-- Note: quarantine/release transition rules stay in the service layer.
-- They depend on reserved stock and expiry semantics and are not safe as simple row-level DB constraints.
-- ============================================================================
