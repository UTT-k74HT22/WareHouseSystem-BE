-- ============================================================================
-- Flyway Migration: WHS-70 Inventory Schema Consistency Refactor
-- Version: V20260404_01
-- Description: Harden inventory uniqueness and document workflow constraints
-- ============================================================================

-- ================================
-- inventory: enforce logical uniqueness when nullable dimensions are present
-- ================================
ALTER TABLE inventory
    DROP INDEX uk_inventory_location;

ALTER TABLE inventory
    ADD COLUMN location_id_nvl CHAR(36)
        GENERATED ALWAYS AS (COALESCE(location_id, '00000000-0000-0000-0000-000000000000')) STORED,
    ADD COLUMN batch_id_nvl CHAR(36)
        GENERATED ALWAYS AS (COALESCE(batch_id, '00000000-0000-0000-0000-000000000000')) STORED;

ALTER TABLE inventory
    ADD CONSTRAINT uk_inventory_dimension_normalized
        UNIQUE (product_id, warehouse_id, location_id_nvl, batch_id_nvl);

CREATE INDEX idx_inventory_dimension_lookup
    ON inventory (product_id, warehouse_id, location_id, batch_id);

-- ================================
-- stock_adjustments: tighten quantity and workflow constraints
-- ================================
ALTER TABLE stock_adjustments
    DROP CHECK chk_adjustment_calculation;

ALTER TABLE stock_adjustments
    ADD CONSTRAINT chk_adjustment_non_negative
        CHECK (quantity_before >= 0 AND quantity_after >= 0),
    ADD CONSTRAINT chk_adjustment_calculation
        CHECK (adjustment_quantity = quantity_after - quantity_before),
    ADD CONSTRAINT chk_adjustment_non_zero
        CHECK (adjustment_quantity <> 0),
    ADD CONSTRAINT chk_adjustment_status_metadata
        CHECK (
            (status = 'PENDING_APPROVAL' AND approved_by IS NULL AND approved_at IS NULL AND rejection_reason IS NULL)
            OR (status = 'APPROVED' AND approved_by IS NOT NULL AND approved_at IS NOT NULL AND rejection_reason IS NULL)
            OR (status = 'REJECTED' AND approved_by IS NOT NULL AND approved_at IS NOT NULL AND rejection_reason IS NOT NULL)
        );

CREATE INDEX idx_adjustment_status_created_at
    ON stock_adjustments (status, created_at);

CREATE INDEX idx_adjustment_inventory_status_created_at
    ON stock_adjustments (inventory_id, status, created_at);

-- ================================
-- stock_movements: align schema with application expectations and query paths
-- ================================
ALTER TABLE stock_movements
    MODIFY warehouse_id CHAR(36) NOT NULL,
    MODIFY location_id CHAR(36) NULL,
    MODIFY reference_id CHAR(36) NULL,
    MODIFY reference_number VARCHAR(50) NULL;

CREATE INDEX idx_movement_product_warehouse_date
    ON stock_movements (product_id, warehouse_id, movement_date DESC);

CREATE INDEX idx_movement_location_date
    ON stock_movements (location_id, movement_date DESC);

-- ================================
-- stock_transfers: add indexes for workflow queries
-- ================================
CREATE INDEX idx_transfer_status_created_at
    ON stock_transfers (status, created_at);

CREATE INDEX idx_transfer_inventory_lookup
    ON stock_transfers (product_id, warehouse_id, from_location_id, batch_id, status);

-- ============================================================================
-- End of Migration
-- ============================================================================
