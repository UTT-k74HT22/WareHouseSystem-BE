-- ============================================================================
-- Flyway Migration: Create Stock Movement & Audit Trail Module
-- Version: V20260201_05
-- Description: Create stock_movements table for complete audit trail
-- Author: Database Team
-- Date: 2026-02-01
-- ============================================================================

CREATE TABLE stock_movements
(
    id               CHAR(36) PRIMARY KEY,
    movement_type    ENUM(
                                'INBOUND',
                                'OUTBOUND',
                                'ADJUSTMENT_INCREASE',
                                'ADJUSTMENT_DECREASE',
                                'TRANSFER_OUT',
                                'TRANSFER_IN',
                                'RESERVE',
                                'UNRESERVE'
                            ) NOT NULL,
    product_id       CHAR(36)       NOT NULL,
    warehouse_id     CHAR(36)       NOT NULL,
    location_id      CHAR(36),
    batch_id         CHAR(36),
    quantity_change  DECIMAL(15, 2) NOT NULL,
    quantity_before  DECIMAL(15, 2) NOT NULL,
    quantity_after   DECIMAL(15, 2) NOT NULL,
    movement_date    TIMESTAMP      NOT NULL,
    reference_type   ENUM('PURCHASE_ORDER', 'SALES_ORDER', 'INBOUND_RECEIPT',
                                 'OUTBOUND_SHIPMENT', 'STOCK_ADJUSTMENT',
                                 'STOCK_TRANSFER', 'SYSTEM') NOT NULL,
    reference_id     CHAR(36),
    reference_number VARCHAR(50),
    notes            TEXT,
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by       VARCHAR(36),
    updated_by       VARCHAR(36),

    CONSTRAINT fk_movement_product FOREIGN KEY (product_id)
        REFERENCES products (id) ON DELETE RESTRICT,
    CONSTRAINT fk_movement_warehouse FOREIGN KEY (warehouse_id)
        REFERENCES warehouses (id) ON DELETE RESTRICT,
    CONSTRAINT fk_movement_location FOREIGN KEY (location_id)
        REFERENCES locations (id) ON DELETE RESTRICT,
    CONSTRAINT fk_movement_batch FOREIGN KEY (batch_id)
        REFERENCES batches (id) ON DELETE RESTRICT,
    CONSTRAINT fk_movement_created_by FOREIGN KEY (created_by)
        REFERENCES accounts (id) ON DELETE SET NULL,
    CONSTRAINT chk_movement_quantity_calc CHECK (
        quantity_after = quantity_before + quantity_change
        ),
    CONSTRAINT chk_movement_quantities CHECK (
        quantity_before >= 0 AND quantity_after >= 0
        )
);

-- Create indexes
CREATE INDEX idx_movement_product_date
    ON stock_movements (product_id, movement_date DESC);

CREATE INDEX idx_movement_batch
    ON stock_movements (batch_id, movement_date DESC);

CREATE INDEX idx_movement_warehouse_date
    ON stock_movements (warehouse_id, movement_date DESC);

CREATE INDEX idx_movement_type
    ON stock_movements (movement_type, movement_date DESC);

CREATE INDEX idx_movement_reference
    ON stock_movements (reference_type, reference_id);

CREATE INDEX idx_movement_date
    ON stock_movements (movement_date DESC);

-- ============================================================================
-- Application-Level Enforcement for Immutability
-- Note: This table should be INSERT-only in application layer
-- Grant only SELECT and INSERT permissions (no UPDATE or DELETE)
-- ============================================================================

-- Example permission setup (adjust role names as needed):
-- GRANT SELECT, INSERT ON stock_movements TO 'wms_app_user'@'%';
-- (Do NOT grant UPDATE or DELETE)

-- ============================================================================
-- End of Migration
-- ============================================================================