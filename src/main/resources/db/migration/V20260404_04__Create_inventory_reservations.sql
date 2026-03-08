-- ============================================================================
-- Flyway Migration: Create Inventory Reservations Table
-- Version: V20260404_04
-- Description: Table to track inventory reservations for idempotency and release
-- ============================================================================

CREATE TABLE inventory_reservations
(
    id                CHAR(36) PRIMARY KEY,
    inventory_id      CHAR(36)       NOT NULL,
    product_id        CHAR(36)       NOT NULL,
    warehouse_id      CHAR(36)       NOT NULL,
    location_id       CHAR(36),
    batch_id          CHAR(36),
    quantity          DECIMAL(15, 2) NOT NULL,
    order_line_id     VARCHAR(100),
    request_key       VARCHAR(100),
    status            ENUM('RESERVED', 'RELEASED', 'CONSUMED') NOT NULL DEFAULT 'RESERVED',
    created_at        TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by        VARCHAR(36),
    updated_by        VARCHAR(36),

    CONSTRAINT fk_reservation_inventory FOREIGN KEY (inventory_id)
        REFERENCES inventory (id) ON DELETE CASCADE,
    CONSTRAINT fk_reservation_product FOREIGN KEY (product_id)
        REFERENCES products (id) ON DELETE RESTRICT,
    CONSTRAINT fk_reservation_warehouse FOREIGN KEY (warehouse_id)
        REFERENCES warehouses (id) ON DELETE RESTRICT,
    CONSTRAINT fk_reservation_location FOREIGN KEY (location_id)
        REFERENCES locations (id) ON DELETE SET NULL,
    CONSTRAINT fk_reservation_batch FOREIGN KEY (batch_id)
        REFERENCES batches (id) ON DELETE SET NULL,
    
    -- Idempotency and uniqueness constraints
    CONSTRAINT uk_reservation_request_key UNIQUE (request_key),
    CONSTRAINT uk_reservation_order_line UNIQUE (order_line_id),
    
    CONSTRAINT chk_reservation_quantity CHECK (quantity > 0)
);

CREATE INDEX idx_reservation_request_key ON inventory_reservations (request_key);
CREATE INDEX idx_reservation_order_line ON inventory_reservations (order_line_id);
CREATE INDEX idx_reservation_status ON inventory_reservations (status);
