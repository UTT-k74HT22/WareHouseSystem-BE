-- ============================================================================
-- Flyway Migration: Create Outbound Operations Module
-- ============================================================================

CREATE TABLE sales_orders
(
    id                      CHAR(36) PRIMARY KEY,
    so_number               VARCHAR(50) UNIQUE NOT NULL,
    customer_id             CHAR(36)           NOT NULL,
    warehouse_id            CHAR(36)           NOT NULL,
    order_date              DATE               NOT NULL,
    requested_delivery_date DATE,
    status                  ENUM('DRAFT', 'CONFIRMED', 'PARTIALLY_SHIPPED',
                                     'COMPLETED', 'CANCELLED') NOT NULL DEFAULT 'DRAFT',
    sub_total                DECIMAL(15, 2)     NOT NULL DEFAULT 0.00,
    tax_amount              DECIMAL(15, 2)     NOT NULL DEFAULT 0.00,
    total_amount            DECIMAL(15, 2)     NOT NULL DEFAULT 0.00,
    currency                VARCHAR(3)                  DEFAULT 'USD',
    notes                   TEXT,
    confirmed_at            TIMESTAMP,
    confirmed_by            CHAR(36),
    created_at             TIMESTAMP                   DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMP                   DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by             VARCHAR(36),
    updated_by             VARCHAR(36),

    CONSTRAINT fk_so_customer FOREIGN KEY (customer_id)
        REFERENCES business_partners (id) ON DELETE RESTRICT,
    CONSTRAINT fk_so_warehouse FOREIGN KEY (warehouse_id)
        REFERENCES warehouses (id) ON DELETE RESTRICT,
    CONSTRAINT fk_so_confirmed_by FOREIGN KEY (confirmed_by)
        REFERENCES accounts (id) ON DELETE SET NULL
);

CREATE TABLE sales_order_lines
(
    id               CHAR(36) PRIMARY KEY,
    sales_order_id   CHAR(36)       NOT NULL,
    product_id       CHAR(36)       NOT NULL,
    line_number      INT            NOT NULL,
    quantity_ordered DECIMAL(15, 2) NOT NULL,
    quantity_shipped DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    unit_price       DECIMAL(15, 2) NOT NULL,
    line_total       DECIMAL(15, 2) NOT NULL,
    notes            VARCHAR(500),
    created_at             TIMESTAMP                   DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMP                   DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by             VARCHAR(36),
    updated_by             VARCHAR(36),

    CONSTRAINT uk_so_line_number UNIQUE (sales_order_id, line_number),
    CONSTRAINT fk_so_line_so FOREIGN KEY (sales_order_id)
        REFERENCES sales_orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_so_line_product FOREIGN KEY (product_id)
        REFERENCES products (id) ON DELETE RESTRICT
);

CREATE TABLE outbound_shipments
(
    id              CHAR(36) PRIMARY KEY,
    shipment_number VARCHAR(50) UNIQUE NOT NULL,
    sales_order_id  CHAR(36)           NOT NULL,
    warehouse_id    CHAR(36)           NOT NULL,
    shipment_date   DATE               NOT NULL,
    status          ENUM('DRAFT', 'PICKING', 'PACKED', 'SHIPPED', 'CANCELLED')
                            NOT NULL DEFAULT 'DRAFT',
    tracking_number VARCHAR(100),
    carrier         VARCHAR(100),
    shipped_at      TIMESTAMP,
    confirmed_by    CHAR(36),
    notes           TEXT,
    created_at             TIMESTAMP                   DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMP                   DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by             VARCHAR(36),
    updated_by             VARCHAR(36),

    CONSTRAINT fk_shipment_so FOREIGN KEY (sales_order_id)
        REFERENCES sales_orders (id) ON DELETE RESTRICT,
    CONSTRAINT fk_shipment_warehouse FOREIGN KEY (warehouse_id)
        REFERENCES warehouses (id) ON DELETE RESTRICT
);

CREATE TABLE outbound_shipment_lines
(
    id                   CHAR(36) PRIMARY KEY,
    outbound_shipment_id CHAR(36)       NOT NULL,
    sales_order_line_id  CHAR(36)       NOT NULL,
    product_id           CHAR(36)       NOT NULL,
    batch_id             CHAR(36),
    location_id          CHAR(36)       NOT NULL,
    line_number          INT            NOT NULL,
    quantity_shipped     DECIMAL(15, 2) NOT NULL,
    picked_at            TIMESTAMP,
    picked_by            CHAR(36),
    notes                VARCHAR(500),
    created_at             TIMESTAMP                   DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMP                   DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by             VARCHAR(36),
    updated_by             VARCHAR(36),

    CONSTRAINT uk_shipment_line_number UNIQUE (outbound_shipment_id, line_number),
    CONSTRAINT fk_shipment_line_shipment FOREIGN KEY (outbound_shipment_id)
        REFERENCES outbound_shipments (id) ON DELETE CASCADE,
    CONSTRAINT fk_shipment_line_so_line FOREIGN KEY (sales_order_line_id)
        REFERENCES sales_order_lines (id) ON DELETE RESTRICT,
    CONSTRAINT fk_shipment_line_product FOREIGN KEY (product_id)
        REFERENCES products (id) ON DELETE RESTRICT,
    CONSTRAINT fk_shipment_line_batch FOREIGN KEY (batch_id)
        REFERENCES batches (id) ON DELETE RESTRICT,
    CONSTRAINT fk_shipment_line_location FOREIGN KEY (location_id)
        REFERENCES locations (id) ON DELETE RESTRICT
);

CREATE INDEX idx_so_customer ON sales_orders (customer_id);
CREATE INDEX idx_so_status ON sales_orders (status);
CREATE INDEX idx_shipment_so ON outbound_shipments (sales_order_id);
CREATE INDEX idx_shipment_status ON outbound_shipments (status);

-- ============================================================================
-- End of Migration
-- ============================================================================