-- ============================================================================
-- Flyway Migration: Create Inbound Operations Module
-- Version: V20260301_03
-- Description: Create purchase_orders and inbound_receipts tables
-- Author: Database Team
-- Date: 2026-03-01
-- ============================================================================

-- Table: purchase_orders
CREATE TABLE purchase_orders
(
    id                     CHAR(36) PRIMARY KEY,
    purchase_order_number  VARCHAR(50) UNIQUE NOT NULL,
    supplier_id            CHAR(36)           NOT NULL,
    warehouse_id           CHAR(36)           NOT NULL,
    order_date             DATE               NOT NULL,
    expected_delivery_date DATE,
    status                 ENUM('DRAFT', 'CONFIRMED', 'PARTIALLY_RECEIVED',
                                 'COMPLETED', 'CANCELLED') NOT NULL DEFAULT 'DRAFT',
    sub_total               DECIMAL(15, 2)     NOT NULL DEFAULT 0.00,
    tax_amount             DECIMAL(15, 2)     NOT NULL DEFAULT 0.00,
    total_amount           DECIMAL(15, 2)     NOT NULL DEFAULT 0.00,
    currency               VARCHAR(3)                  DEFAULT 'USD',
    payment_terms          VARCHAR(200),
    notes                  TEXT,
    confirmed_at           TIMESTAMP,
    confirmed_by           CHAR(36),
    created_at             TIMESTAMP                   DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMP                   DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by             VARCHAR(36),
    updated_by             VARCHAR(36),

    CONSTRAINT fk_po_supplier FOREIGN KEY (supplier_id)
        REFERENCES business_partners (id) ON DELETE RESTRICT,
    CONSTRAINT fk_po_warehouse FOREIGN KEY (warehouse_id)
        REFERENCES warehouses (id) ON DELETE RESTRICT,
    CONSTRAINT fk_po_confirmed_by FOREIGN KEY (confirmed_by)
        REFERENCES accounts (id) ON DELETE SET NULL,
    CONSTRAINT fk_po_created_by FOREIGN KEY (created_by)
        REFERENCES accounts (id) ON DELETE SET NULL,
    CONSTRAINT fk_po_updated_by FOREIGN KEY (updated_by)
        REFERENCES accounts (id) ON DELETE SET NULL,
    CONSTRAINT chk_po_totals CHECK (
        sub_total >= 0 AND tax_amount >= 0 AND total_amount >= 0
        )
);

CREATE INDEX idx_po_supplier ON purchase_orders (supplier_id);
CREATE INDEX idx_po_warehouse ON purchase_orders (warehouse_id);
CREATE INDEX idx_po_status ON purchase_orders (status);

-- Table: purchase_order_lines
CREATE TABLE purchase_order_lines
(
    id                CHAR(36) PRIMARY KEY,
    purchase_order_id CHAR(36)       NOT NULL,
    product_id        CHAR(36)       NOT NULL,
    line_number       INT            NOT NULL,
    quantity_ordered  DECIMAL(15, 2) NOT NULL,
    quantity_received DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    unit_price        DECIMAL(15, 2) NOT NULL,
    line_total        DECIMAL(15, 2) NOT NULL,
    notes             VARCHAR(500),
    created_at        TIMESTAMP               DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP               DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by        VARCHAR(36),
    updated_by        VARCHAR(36),

    CONSTRAINT uk_po_line_number UNIQUE (purchase_order_id, line_number),
    CONSTRAINT fk_po_line_po FOREIGN KEY (purchase_order_id)
        REFERENCES purchase_orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_po_line_product FOREIGN KEY (product_id)
        REFERENCES products (id) ON DELETE RESTRICT,
    CONSTRAINT chk_po_line_quantities CHECK (
        quantity_ordered > 0
            AND quantity_received >= 0
            AND quantity_received <= quantity_ordered
        )
);

CREATE INDEX idx_po_line_po ON purchase_order_lines (purchase_order_id);
CREATE INDEX idx_po_line_product ON purchase_order_lines (product_id);

-- Table: inbound_receipts
CREATE TABLE inbound_receipts
(
    id                   CHAR(36) PRIMARY KEY,
    receipt_number       VARCHAR(50) UNIQUE NOT NULL,
    purchase_order_id    CHAR(36)           NOT NULL,
    warehouse_id         CHAR(36)           NOT NULL,
    receipt_date         DATE               NOT NULL,
    status               ENUM('DRAFT', 'CONFIRMED', 'CANCELLED') NOT NULL DEFAULT 'DRAFT',
    confirmed_at         TIMESTAMP,
    confirmed_by         CHAR(36),
    delivery_note_number VARCHAR(100),
    notes                TEXT,
    created_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by           VARCHAR(36),
    updated_by           VARCHAR(36),

    CONSTRAINT fk_receipt_po FOREIGN KEY (purchase_order_id)
        REFERENCES purchase_orders (id) ON DELETE RESTRICT,
    CONSTRAINT fk_receipt_warehouse FOREIGN KEY (warehouse_id)
        REFERENCES warehouses (id) ON DELETE RESTRICT,
    CONSTRAINT fk_receipt_confirmed_by FOREIGN KEY (confirmed_by)
        REFERENCES accounts (id) ON DELETE SET NULL,
    CONSTRAINT fk_receipt_created_by FOREIGN KEY (created_by)
        REFERENCES accounts (id) ON DELETE SET NULL,
    CONSTRAINT fk_receipt_updated_by FOREIGN KEY (updated_by)
        REFERENCES accounts (id) ON DELETE SET NULL
);

CREATE INDEX idx_receipt_po ON inbound_receipts (purchase_order_id);
CREATE INDEX idx_receipt_warehouse ON inbound_receipts (warehouse_id);
CREATE INDEX idx_receipt_status ON inbound_receipts (status);

-- Table: inbound_receipt_lines
CREATE TABLE inbound_receipt_lines
(
    id                     CHAR(36) PRIMARY KEY,
    inbound_receipt_id     CHAR(36)       NOT NULL,
    purchase_order_line_id CHAR(36)       NOT NULL,
    product_id             CHAR(36)       NOT NULL,
    batch_id               CHAR(36),
    location_id            CHAR(36)       NOT NULL,
    line_number            INT            NOT NULL,
    quantity_received      DECIMAL(15, 2) NOT NULL,
    quality_status         ENUM('PASS', 'QUARANTINE') NOT NULL DEFAULT 'PASS',
    notes                  VARCHAR(500),
    created_at             TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by             VARCHAR(36),
    updated_by             VARCHAR(36),

    CONSTRAINT uk_receipt_line_number UNIQUE (inbound_receipt_id, line_number),
    CONSTRAINT fk_receipt_line_receipt FOREIGN KEY (inbound_receipt_id)
        REFERENCES inbound_receipts (id) ON DELETE CASCADE,
    CONSTRAINT fk_receipt_line_po_line FOREIGN KEY (purchase_order_line_id)
        REFERENCES purchase_order_lines (id) ON DELETE RESTRICT,
    CONSTRAINT fk_receipt_line_product FOREIGN KEY (product_id)
        REFERENCES products (id) ON DELETE RESTRICT,
    CONSTRAINT fk_receipt_line_batch FOREIGN KEY (batch_id)
        REFERENCES batches (id) ON DELETE RESTRICT,
    CONSTRAINT fk_receipt_line_location FOREIGN KEY (location_id)
        REFERENCES locations (id) ON DELETE RESTRICT,
    CONSTRAINT chk_receipt_line_quantity CHECK (quantity_received > 0)
);

CREATE INDEX idx_receipt_line_receipt ON inbound_receipt_lines (inbound_receipt_id);
CREATE INDEX idx_receipt_line_po_line ON inbound_receipt_lines (purchase_order_line_id);
CREATE INDEX idx_receipt_line_product ON inbound_receipt_lines (product_id);

-- ============================================================================
-- End of Migration
-- ============================================================================