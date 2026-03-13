-- ============================================================================
-- Flyway Migration: Create Inventory Management Module
-- Version: V20260201_02
-- Description: Create inventory, stock_adjustments, stock_transfers tables
-- Author: Database Team
-- Date: 2026-02-01
-- ============================================================================

-- ================================
-- Table: inventory
-- ================================
CREATE TABLE inventory
(
    id                CHAR(36) PRIMARY KEY,
    product_id        CHAR(36)       NOT NULL,
    warehouse_id      CHAR(36)       NOT NULL,
    location_id       CHAR(36),
    batch_id          CHAR(36),
    on_hand_quantity  DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    reserved_quantity DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    version           INT            NOT NULL DEFAULT 0,
    last_movement_at  TIMESTAMP,
    created_at              TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by              VARCHAR(36),
    updated_by              VARCHAR(36),

    CONSTRAINT uk_inventory_location UNIQUE (product_id, warehouse_id, location_id, batch_id),
    CONSTRAINT fk_inventory_product FOREIGN KEY (product_id)
        REFERENCES products (id) ON DELETE RESTRICT,
    CONSTRAINT fk_inventory_warehouse FOREIGN KEY (warehouse_id)
        REFERENCES warehouses (id) ON DELETE RESTRICT,
    CONSTRAINT fk_inventory_location FOREIGN KEY (location_id)
        REFERENCES locations (id) ON DELETE RESTRICT,
    CONSTRAINT fk_inventory_batch FOREIGN KEY (batch_id)
        REFERENCES batches (id) ON DELETE RESTRICT,
    CONSTRAINT fk_inventory_created_by FOREIGN KEY (created_by)
        REFERENCES accounts (id) ON DELETE SET NULL,
    CONSTRAINT fk_inventory_updated_by FOREIGN KEY (updated_by)
        REFERENCES accounts (id) ON DELETE SET NULL,
    CONSTRAINT chk_inventory_quantities CHECK (
        on_hand_quantity >= 0
            AND reserved_quantity >= 0
            AND reserved_quantity <= on_hand_quantity
        )
);

CREATE INDEX idx_inventory_product ON inventory (product_id);
CREATE INDEX idx_inventory_warehouse ON inventory (warehouse_id);
CREATE INDEX idx_inventory_location ON inventory (location_id);
CREATE INDEX idx_inventory_batch ON inventory (batch_id);
CREATE INDEX idx_inventory_availability
    ON inventory (product_id, warehouse_id, on_hand_quantity, reserved_quantity);

-- ================================
-- Table: stock_adjustments
-- ================================
CREATE TABLE stock_adjustments
(
    id                  CHAR(36) PRIMARY KEY,
    adjustment_number   VARCHAR(50) UNIQUE NOT NULL,
    inventory_id        CHAR(36)           NOT NULL,
    product_id          CHAR(36)           NOT NULL,
    warehouse_id        CHAR(36)           NOT NULL,
    location_id         CHAR(36),
    batch_id            CHAR(36),
    quantity_before     DECIMAL(15, 2)     NOT NULL,
    quantity_after      DECIMAL(15, 2)     NOT NULL,
    adjustment_quantity DECIMAL(15, 2)     NOT NULL,
    reason              ENUM('DAMAGE', 'THEFT', 'COUNT_ERROR', 'EXPIRED',
                                 'QUALITY_ISSUE', 'SYSTEM_ERROR', 'OTHER') NOT NULL,
    status              ENUM('PENDING_APPROVAL', 'APPROVED', 'REJECTED')
                            NOT NULL DEFAULT 'PENDING_APPROVAL',
    notes               TEXT,
    requires_approval   BOOLEAN            NOT NULL DEFAULT FALSE,
    approved_by         CHAR(36),
    approved_at         TIMESTAMP,
    rejection_reason    VARCHAR(500),
    created_at              TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by              VARCHAR(36),
    updated_by              VARCHAR(36),

    CONSTRAINT fk_adjustment_inventory FOREIGN KEY (inventory_id)
        REFERENCES inventory (id) ON DELETE RESTRICT,
    CONSTRAINT fk_adjustment_product FOREIGN KEY (product_id)
        REFERENCES products (id) ON DELETE RESTRICT,
    CONSTRAINT fk_adjustment_warehouse FOREIGN KEY (warehouse_id)
        REFERENCES warehouses (id) ON DELETE RESTRICT,
    CONSTRAINT fk_adjustment_location FOREIGN KEY (location_id)
        REFERENCES locations (id) ON DELETE RESTRICT,
    CONSTRAINT fk_adjustment_batch FOREIGN KEY (batch_id)
        REFERENCES batches (id) ON DELETE RESTRICT,
    CONSTRAINT fk_adjustment_approved_by FOREIGN KEY (approved_by)
        REFERENCES accounts (id) ON DELETE SET NULL,
    CONSTRAINT fk_adjustment_created_by FOREIGN KEY (created_by)
        REFERENCES accounts (id) ON DELETE SET NULL,
    CONSTRAINT fk_adjustment_updated_by FOREIGN KEY (updated_by)
        REFERENCES accounts (id) ON DELETE SET NULL,
    CONSTRAINT chk_adjustment_calculation CHECK (
        adjustment_quantity = quantity_after - quantity_before
        )
);

CREATE INDEX idx_adjustment_status ON stock_adjustments (status);
CREATE INDEX idx_adjustment_product ON stock_adjustments (product_id);
CREATE INDEX idx_adjustment_warehouse ON stock_adjustments (warehouse_id);
CREATE INDEX idx_adjustment_created_at ON stock_adjustments (created_at);

-- ================================
-- Table: stock_transfers
-- ================================
CREATE TABLE stock_transfers
(
    id               CHAR(36) PRIMARY KEY,
    transfer_number  VARCHAR(50) UNIQUE NOT NULL,
    product_id       CHAR(36)           NOT NULL,
    warehouse_id     CHAR(36)           NOT NULL,
    batch_id         CHAR(36),
    from_location_id CHAR(36)           NOT NULL,
    to_location_id   CHAR(36)           NOT NULL,
    quantity         DECIMAL(15, 2)     NOT NULL,
    reason           ENUM('REORG', 'PICKING_PREP', 'OVERFLOW', 'CONSOLIDATION', 'OTHER') NOT NULL,
    notes            TEXT,
    status           ENUM('DRAFT', 'COMPLETED', 'CANCELLED') NOT NULL DEFAULT 'DRAFT',
    completed_at     TIMESTAMP,
    created_at              TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by              VARCHAR(36),
    updated_by              VARCHAR(36),

    CONSTRAINT fk_transfer_product FOREIGN KEY (product_id)
        REFERENCES products (id) ON DELETE RESTRICT,
    CONSTRAINT fk_transfer_warehouse FOREIGN KEY (warehouse_id)
        REFERENCES warehouses (id) ON DELETE RESTRICT,
    CONSTRAINT fk_transfer_batch FOREIGN KEY (batch_id)
        REFERENCES batches (id) ON DELETE RESTRICT,
    CONSTRAINT fk_transfer_from_location FOREIGN KEY (from_location_id)
        REFERENCES locations (id) ON DELETE RESTRICT,
    CONSTRAINT fk_transfer_to_location FOREIGN KEY (to_location_id)
        REFERENCES locations (id) ON DELETE RESTRICT,
    CONSTRAINT fk_transfer_created_by FOREIGN KEY (created_by)
        REFERENCES accounts (id) ON DELETE SET NULL,
    CONSTRAINT fk_transfer_updated_by FOREIGN KEY (updated_by)
        REFERENCES accounts (id) ON DELETE SET NULL,
    CONSTRAINT chk_transfer_locations CHECK (from_location_id != to_location_id
) ,
    CONSTRAINT chk_transfer_quantity CHECK (quantity > 0)
);

CREATE INDEX idx_transfer_product ON stock_transfers (product_id);
CREATE INDEX idx_transfer_warehouse ON stock_transfers (warehouse_id);
CREATE INDEX idx_transfer_from_location ON stock_transfers (from_location_id);
CREATE INDEX idx_transfer_to_location ON stock_transfers (to_location_id);
CREATE INDEX idx_transfer_status ON stock_transfers (status);

-- ============================================================================
-- End of Migration
-- ============================================================================