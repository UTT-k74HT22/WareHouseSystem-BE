# Database Schema - Module 4: Inventory Management
## Database Design & Migration Guide

---

## 📋 Document Information

| Property | Value |
|----------|-------|
| **Module** | Inventory Management |
| **Version** | 1.0 |
| **Date** | February 1, 2026 |
| **Status** | Draft |
| **Author** | Database Designer |

---

## 📑 Table of Contents

1. [Schema Overview](#schema-overview)
2. [Table Definitions](#table-definitions)
3. [Relationships](#relationships)
4. [Indexes](#indexes)
5. [Constraints](#constraints)
6. [Flyway Migration](#flyway-migration)
7. [Sample Data](#sample-data)
8. [Query Examples](#query-examples)

---

## 🗂️ Schema Overview

### ERD Diagram

```
┌──────────────┐      ┌──────────────┐      ┌──────────────┐      ┌──────────────┐
│  products    │      │ warehouses   │      │  locations   │      │   batches    │
│              │      │              │      │              │      │              │
│ - id (PK)    │      │ - id (PK)    │      │ - id (PK)    │      │ - id (PK)    │
└──────┬───────┘      └──────┬───────┘      └──────┬───────┘      └──────┬───────┘
       │                     │                     │                     │
       │                     │                     │                     │
       └─────────────────────┴─────────────────────┴─────────────────────┘
                                       │
                                       │ All FK to
                                       ▼
                    ┌────────────────────────────────────────┐
                    │           inventory                    │
                    │                                        │
                    │ - id (PK)                             │
                    │ - product_id (FK) ────────────────────┤
                    │ - warehouse_id (FK) ───────────────────┤
                    │ - location_id (FK, nullable) ──────────┤
                    │ - batch_id (FK, nullable) ─────────────┤
                    │ - on_hand_quantity                    │
                    │ - reserved_quantity                   │
                    │ - version (optimistic lock)           │
                    │ - last_movement_at                    │
                    │ - created_at, updated_at              │
                    │                                        │
                    │ UK: (product, warehouse, location,    │
                    │      batch)                            │
                    └────────────────────────────────────────┘
                                       │
                         ┌─────────────┴─────────────┐
                         │                           │
                         ▼                           ▼
            ┌─────────────────────┐      ┌──────────────────────┐
            │ stock_adjustments   │      │  stock_transfers     │
            │                     │      │                      │
            │ - adjustment_number │      │ - transfer_number    │
            │ - inventory_id (FK) │      │ - product_id (FK)    │
            │ - status            │      │ - from_location_id   │
            │ - reason            │      │ - to_location_id     │
            │ - adjustment_qty    │      │ - quantity           │
            │ - approved_by       │      │ - reason             │
            └─────────────────────┘      └──────────────────────┘
```

---

## 📊 Table Definitions

### Table: `inventory`

Core table tracking stock quantities at the most granular level.

```sql
CREATE TABLE inventory (
    -- Primary Key
    id                  CHAR(36) PRIMARY KEY 
                        COMMENT 'UUID primary key',
    
    -- Foreign Keys (Define unique combination)
    product_id          CHAR(36) NOT NULL 
                        COMMENT 'Foreign key to products',
    warehouse_id        CHAR(36) NOT NULL 
                        COMMENT 'Foreign key to warehouses',
    location_id         CHAR(36) 
                        COMMENT 'Foreign key to locations (nullable if no specific location)',
    batch_id            CHAR(36) 
                        COMMENT 'Foreign key to batches (nullable for non-batch products)',
    
    -- Quantity Fields
    on_hand_quantity    DECIMAL(15, 2) NOT NULL DEFAULT 0.00 
                        COMMENT 'Physical stock quantity',
    reserved_quantity   DECIMAL(15, 2) NOT NULL DEFAULT 0.00 
                        COMMENT 'Quantity allocated to orders',
    
    -- Computed: available_quantity = on_hand_quantity - reserved_quantity
    -- Not stored, calculated in queries
    
    -- Concurrency Control
    version             INT NOT NULL DEFAULT 0 
                        COMMENT 'Optimistic locking version number',
    
    -- Tracking
    last_movement_at    TIMESTAMP 
                        COMMENT 'Last time stock was changed',
    
    -- Audit Fields
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP 
                        ON UPDATE CURRENT_TIMESTAMP,
    created_by          CHAR(36),
    updated_by          CHAR(36),
    
    -- Constraints
    CONSTRAINT uk_inventory_location UNIQUE (product_id, warehouse_id, location_id, batch_id),
    CONSTRAINT fk_inventory_product FOREIGN KEY (product_id) 
        REFERENCES products(id) ON DELETE RESTRICT,
    CONSTRAINT fk_inventory_warehouse FOREIGN KEY (warehouse_id) 
        REFERENCES warehouses(id) ON DELETE RESTRICT,
    CONSTRAINT fk_inventory_location FOREIGN KEY (location_id) 
        REFERENCES locations(id) ON DELETE RESTRICT,
    CONSTRAINT fk_inventory_batch FOREIGN KEY (batch_id) 
        REFERENCES batches(id) ON DELETE RESTRICT,
    CONSTRAINT fk_inventory_created_by FOREIGN KEY (created_by) 
        REFERENCES accounts(id) ON DELETE SET NULL,
    CONSTRAINT fk_inventory_updated_by FOREIGN KEY (updated_by) 
        REFERENCES accounts(id) ON DELETE SET NULL,
    
    -- Check Constraints
    CONSTRAINT chk_inventory_quantities CHECK (
        on_hand_quantity >= 0 
        AND reserved_quantity >= 0 
        AND reserved_quantity <= on_hand_quantity
    )
);
```

---

### Table: `stock_adjustments`

Records manual inventory adjustments with approval workflow.

```sql
CREATE TABLE stock_adjustments (
    -- Primary Key
    id                      CHAR(36) PRIMARY KEY,
    adjustment_number       VARCHAR(50) UNIQUE NOT NULL 
                            COMMENT 'Human-readable identifier (e.g., ADJ-2026-0001)',
    
    -- Reference
    inventory_id            CHAR(36) NOT NULL 
                            COMMENT 'Foreign key to inventory record',
    product_id              CHAR(36) NOT NULL 
                            COMMENT 'Denormalized for easier querying',
    warehouse_id            CHAR(36) NOT NULL,
    location_id             CHAR(36),
    batch_id                CHAR(36),
    
    -- Adjustment Details
    quantity_before         DECIMAL(15, 2) NOT NULL 
                            COMMENT 'On-hand quantity before adjustment',
    quantity_after          DECIMAL(15, 2) NOT NULL 
                            COMMENT 'On-hand quantity after adjustment',
    adjustment_quantity     DECIMAL(15, 2) NOT NULL 
                            COMMENT 'Delta (positive or negative)',
    
    -- Reason & Status
    reason                  ENUM('DAMAGE', 'THEFT', 'COUNT_ERROR', 'EXPIRED', 
                                 'QUALITY_ISSUE', 'SYSTEM_ERROR', 'OTHER') 
                            NOT NULL,
    status                  ENUM('PENDING_APPROVAL', 'APPROVED', 'REJECTED') 
                            NOT NULL DEFAULT 'PENDING_APPROVAL',
    notes                   TEXT 
                            COMMENT 'Detailed explanation',
    
    -- Approval Workflow
    requires_approval       BOOLEAN NOT NULL DEFAULT FALSE 
                            COMMENT 'True if above threshold',
    approved_by             CHAR(36) 
                            COMMENT 'Manager who approved/rejected',
    approved_at             TIMESTAMP 
                            COMMENT 'Approval/rejection timestamp',
    rejection_reason        VARCHAR(500) 
                            COMMENT 'Reason if rejected',
    
    -- Audit
    created_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP 
                            ON UPDATE CURRENT_TIMESTAMP,
    created_by              CHAR(36) NOT NULL 
                            COMMENT 'Staff who created adjustment',
    updated_by              CHAR(36),
    
    -- Constraints
    CONSTRAINT fk_adjustment_inventory FOREIGN KEY (inventory_id) 
        REFERENCES inventory(id) ON DELETE RESTRICT,
    CONSTRAINT fk_adjustment_product FOREIGN KEY (product_id) 
        REFERENCES products(id) ON DELETE RESTRICT,
    CONSTRAINT fk_adjustment_warehouse FOREIGN KEY (warehouse_id) 
        REFERENCES warehouses(id) ON DELETE RESTRICT,
    CONSTRAINT fk_adjustment_location FOREIGN KEY (location_id) 
        REFERENCES locations(id) ON DELETE RESTRICT,
    CONSTRAINT fk_adjustment_batch FOREIGN KEY (batch_id) 
        REFERENCES batches(id) ON DELETE RESTRICT,
    CONSTRAINT fk_adjustment_approved_by FOREIGN KEY (approved_by) 
        REFERENCES accounts(id) ON DELETE SET NULL,
    CONSTRAINT fk_adjustment_created_by FOREIGN KEY (created_by) 
        REFERENCES accounts(id) ON DELETE SET NULL,
    CONSTRAINT fk_adjustment_updated_by FOREIGN KEY (updated_by) 
        REFERENCES accounts(id) ON DELETE SET NULL,
    
    -- Check Constraints
    CONSTRAINT chk_adjustment_calculation CHECK (
        adjustment_quantity = quantity_after - quantity_before
    ),
    CONSTRAINT chk_adjustment_approval CHECK (
        (status = 'PENDING_APPROVAL' AND approved_by IS NULL AND approved_at IS NULL)
        OR (status IN ('APPROVED', 'REJECTED') AND approved_by IS NOT NULL AND approved_at IS NOT NULL)
    )
);
```

---

### Table: `stock_transfers`

Records movement of stock between locations within same warehouse.

```sql
CREATE TABLE stock_transfers (
    -- Primary Key
    id                  CHAR(36) PRIMARY KEY,
    transfer_number     VARCHAR(50) UNIQUE NOT NULL 
                        COMMENT 'Human-readable identifier (e.g., TRF-2026-0001)',
    
    -- Reference
    product_id          CHAR(36) NOT NULL,
    warehouse_id        CHAR(36) NOT NULL 
                        COMMENT 'Must be same for from and to locations',
    batch_id            CHAR(36) 
                        COMMENT 'Batch being transferred (if batch-tracked)',
    
    -- Transfer Details
    from_location_id    CHAR(36) NOT NULL 
                        COMMENT 'Source location',
    to_location_id      CHAR(36) NOT NULL 
                        COMMENT 'Destination location',
    quantity            DECIMAL(15, 2) NOT NULL 
                        COMMENT 'Quantity transferred',
    
    -- Reason
    reason              ENUM('REORG', 'PICKING_PREP', 'OVERFLOW', 
                             'CONSOLIDATION', 'OTHER') NOT NULL,
    notes               TEXT,
    
    -- Status
    status              ENUM('DRAFT', 'COMPLETED', 'CANCELLED') 
                        NOT NULL DEFAULT 'DRAFT',
    completed_at        TIMESTAMP 
                        COMMENT 'When transfer was executed',
    
    -- Audit
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP 
                        ON UPDATE CURRENT_TIMESTAMP,
    created_by          CHAR(36) NOT NULL,
    updated_by          CHAR(36),
    
    -- Constraints
    CONSTRAINT fk_transfer_product FOREIGN KEY (product_id) 
        REFERENCES products(id) ON DELETE RESTRICT,
    CONSTRAINT fk_transfer_warehouse FOREIGN KEY (warehouse_id) 
        REFERENCES warehouses(id) ON DELETE RESTRICT,
    CONSTRAINT fk_transfer_batch FOREIGN KEY (batch_id) 
        REFERENCES batches(id) ON DELETE RESTRICT,
    CONSTRAINT fk_transfer_from_location FOREIGN KEY (from_location_id) 
        REFERENCES locations(id) ON DELETE RESTRICT,
    CONSTRAINT fk_transfer_to_location FOREIGN KEY (to_location_id) 
        REFERENCES locations(id) ON DELETE RESTRICT,
    CONSTRAINT fk_transfer_created_by FOREIGN KEY (created_by) 
        REFERENCES accounts(id) ON DELETE SET NULL,
    CONSTRAINT fk_transfer_updated_by FOREIGN KEY (updated_by) 
        REFERENCES accounts(id) ON DELETE SET NULL,
    
    -- Check Constraints
    CONSTRAINT chk_transfer_locations CHECK (
        from_location_id != to_location_id
    ),
    CONSTRAINT chk_transfer_quantity CHECK (
        quantity > 0
    )
);
```

---

## 🔗 Relationships

### Foreign Keys Summary

| Table | Column | References | On Delete | Purpose |
|-------|--------|------------|-----------|---------|
| inventory | product_id | products.id | RESTRICT | Link to product |
| inventory | warehouse_id | warehouses.id | RESTRICT | Link to warehouse |
| inventory | location_id | locations.id | RESTRICT | Link to location |
| inventory | batch_id | batches.id | RESTRICT | Link to batch |
| stock_adjustments | inventory_id | inventory.id | RESTRICT | Link to adjusted inventory |
| stock_adjustments | approved_by | accounts.id | SET NULL | Track approver |
| stock_transfers | from_location_id | locations.id | RESTRICT | Source location |
| stock_transfers | to_location_id | locations.id | RESTRICT | Destination location |

---

## 📑 Indexes

### Table: inventory

```sql
-- Primary and unique
PRIMARY KEY (id)
UNIQUE KEY uk_inventory_location (product_id, warehouse_id, location_id, batch_id)

-- Secondary indexes
CREATE INDEX idx_inventory_product ON inventory(product_id);
CREATE INDEX idx_inventory_warehouse ON inventory(warehouse_id);
CREATE INDEX idx_inventory_location ON inventory(location_id);
CREATE INDEX idx_inventory_batch ON inventory(batch_id);

-- Composite for availability queries
CREATE INDEX idx_inventory_availability 
    ON inventory(product_id, warehouse_id, on_hand_quantity, reserved_quantity);

-- For low stock queries
CREATE INDEX idx_inventory_stock_level 
    ON inventory(product_id, on_hand_quantity);
```

### Table: stock_adjustments

```sql
PRIMARY KEY (id)
UNIQUE KEY (adjustment_number)

CREATE INDEX idx_adjustment_status ON stock_adjustments(status);
CREATE INDEX idx_adjustment_product ON stock_adjustments(product_id);
CREATE INDEX idx_adjustment_warehouse ON stock_adjustments(warehouse_id);
CREATE INDEX idx_adjustment_created_at ON stock_adjustments(created_at);
CREATE INDEX idx_adjustment_pending ON stock_adjustments(status, created_at) 
    WHERE status = 'PENDING_APPROVAL';
```

### Table: stock_transfers

```sql
PRIMARY KEY (id)
UNIQUE KEY (transfer_number)

CREATE INDEX idx_transfer_product ON stock_transfers(product_id);
CREATE INDEX idx_transfer_warehouse ON stock_transfers(warehouse_id);
CREATE INDEX idx_transfer_from_location ON stock_transfers(from_location_id);
CREATE INDEX idx_transfer_to_location ON stock_transfers(to_location_id);
CREATE INDEX idx_transfer_status ON stock_transfers(status);
CREATE INDEX idx_transfer_created_at ON stock_transfers(created_at);
```

---

## 🔄 Flyway Migration

### Migration File: `V20260201_02__Create_inventory_management.sql`

```sql
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
CREATE TABLE inventory (
    id                  CHAR(36) PRIMARY KEY,
    product_id          CHAR(36) NOT NULL,
    warehouse_id        CHAR(36) NOT NULL,
    location_id         CHAR(36),
    batch_id            CHAR(36),
    on_hand_quantity    DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    reserved_quantity   DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    version             INT NOT NULL DEFAULT 0,
    last_movement_at    TIMESTAMP,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by          CHAR(36),
    updated_by          CHAR(36),
    
    CONSTRAINT uk_inventory_location UNIQUE (product_id, warehouse_id, location_id, batch_id),
    CONSTRAINT fk_inventory_product FOREIGN KEY (product_id) 
        REFERENCES products(id) ON DELETE RESTRICT,
    CONSTRAINT fk_inventory_warehouse FOREIGN KEY (warehouse_id) 
        REFERENCES warehouses(id) ON DELETE RESTRICT,
    CONSTRAINT fk_inventory_location FOREIGN KEY (location_id) 
        REFERENCES locations(id) ON DELETE RESTRICT,
    CONSTRAINT fk_inventory_batch FOREIGN KEY (batch_id) 
        REFERENCES batches(id) ON DELETE RESTRICT,
    CONSTRAINT fk_inventory_created_by FOREIGN KEY (created_by) 
        REFERENCES accounts(id) ON DELETE SET NULL,
    CONSTRAINT fk_inventory_updated_by FOREIGN KEY (updated_by) 
        REFERENCES accounts(id) ON DELETE SET NULL,
    CONSTRAINT chk_inventory_quantities CHECK (
        on_hand_quantity >= 0 
        AND reserved_quantity >= 0 
        AND reserved_quantity <= on_hand_quantity
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Inventory tracking at product-warehouse-location-batch level';

CREATE INDEX idx_inventory_product ON inventory(product_id);
CREATE INDEX idx_inventory_warehouse ON inventory(warehouse_id);
CREATE INDEX idx_inventory_location ON inventory(location_id);
CREATE INDEX idx_inventory_batch ON inventory(batch_id);
CREATE INDEX idx_inventory_availability 
    ON inventory(product_id, warehouse_id, on_hand_quantity, reserved_quantity);

-- ================================
-- Table: stock_adjustments
-- ================================
CREATE TABLE stock_adjustments (
    id                      CHAR(36) PRIMARY KEY,
    adjustment_number       VARCHAR(50) UNIQUE NOT NULL,
    inventory_id            CHAR(36) NOT NULL,
    product_id              CHAR(36) NOT NULL,
    warehouse_id            CHAR(36) NOT NULL,
    location_id             CHAR(36),
    batch_id                CHAR(36),
    quantity_before         DECIMAL(15, 2) NOT NULL,
    quantity_after          DECIMAL(15, 2) NOT NULL,
    adjustment_quantity     DECIMAL(15, 2) NOT NULL,
    reason                  ENUM('DAMAGE', 'THEFT', 'COUNT_ERROR', 'EXPIRED', 
                                 'QUALITY_ISSUE', 'SYSTEM_ERROR', 'OTHER') NOT NULL,
    status                  ENUM('PENDING_APPROVAL', 'APPROVED', 'REJECTED') 
                            NOT NULL DEFAULT 'PENDING_APPROVAL',
    notes                   TEXT,
    requires_approval       BOOLEAN NOT NULL DEFAULT FALSE,
    approved_by             CHAR(36),
    approved_at             TIMESTAMP,
    rejection_reason        VARCHAR(500),
    created_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by              CHAR(36) NOT NULL,
    updated_by              CHAR(36),
    
    CONSTRAINT fk_adjustment_inventory FOREIGN KEY (inventory_id) 
        REFERENCES inventory(id) ON DELETE RESTRICT,
    CONSTRAINT fk_adjustment_product FOREIGN KEY (product_id) 
        REFERENCES products(id) ON DELETE RESTRICT,
    CONSTRAINT fk_adjustment_warehouse FOREIGN KEY (warehouse_id) 
        REFERENCES warehouses(id) ON DELETE RESTRICT,
    CONSTRAINT fk_adjustment_location FOREIGN KEY (location_id) 
        REFERENCES locations(id) ON DELETE RESTRICT,
    CONSTRAINT fk_adjustment_batch FOREIGN KEY (batch_id) 
        REFERENCES batches(id) ON DELETE RESTRICT,
    CONSTRAINT fk_adjustment_approved_by FOREIGN KEY (approved_by) 
        REFERENCES accounts(id) ON DELETE SET NULL,
    CONSTRAINT fk_adjustment_created_by FOREIGN KEY (created_by) 
        REFERENCES accounts(id) ON DELETE SET NULL,
    CONSTRAINT fk_adjustment_updated_by FOREIGN KEY (updated_by) 
        REFERENCES accounts(id) ON DELETE SET NULL,
    CONSTRAINT chk_adjustment_calculation CHECK (
        adjustment_quantity = quantity_after - quantity_before
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Manual inventory adjustments with approval workflow';

CREATE INDEX idx_adjustment_status ON stock_adjustments(status);
CREATE INDEX idx_adjustment_product ON stock_adjustments(product_id);
CREATE INDEX idx_adjustment_warehouse ON stock_adjustments(warehouse_id);
CREATE INDEX idx_adjustment_created_at ON stock_adjustments(created_at);

-- ================================
-- Table: stock_transfers
-- ================================
CREATE TABLE stock_transfers (
    id                  CHAR(36) PRIMARY KEY,
    transfer_number     VARCHAR(50) UNIQUE NOT NULL,
    product_id          CHAR(36) NOT NULL,
    warehouse_id        CHAR(36) NOT NULL,
    batch_id            CHAR(36),
    from_location_id    CHAR(36) NOT NULL,
    to_location_id      CHAR(36) NOT NULL,
    quantity            DECIMAL(15, 2) NOT NULL,
    reason              ENUM('REORG', 'PICKING_PREP', 'OVERFLOW', 'CONSOLIDATION', 'OTHER') NOT NULL,
    notes               TEXT,
    status              ENUM('DRAFT', 'COMPLETED', 'CANCELLED') NOT NULL DEFAULT 'DRAFT',
    completed_at        TIMESTAMP,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by          CHAR(36) NOT NULL,
    updated_by          CHAR(36),
    
    CONSTRAINT fk_transfer_product FOREIGN KEY (product_id) 
        REFERENCES products(id) ON DELETE RESTRICT,
    CONSTRAINT fk_transfer_warehouse FOREIGN KEY (warehouse_id) 
        REFERENCES warehouses(id) ON DELETE RESTRICT,
    CONSTRAINT fk_transfer_batch FOREIGN KEY (batch_id) 
        REFERENCES batches(id) ON DELETE RESTRICT,
    CONSTRAINT fk_transfer_from_location FOREIGN KEY (from_location_id) 
        REFERENCES locations(id) ON DELETE RESTRICT,
    CONSTRAINT fk_transfer_to_location FOREIGN KEY (to_location_id) 
        REFERENCES locations(id) ON DELETE RESTRICT,
    CONSTRAINT fk_transfer_created_by FOREIGN KEY (created_by) 
        REFERENCES accounts(id) ON DELETE SET NULL,
    CONSTRAINT fk_transfer_updated_by FOREIGN KEY (updated_by) 
        REFERENCES accounts(id) ON DELETE SET NULL,
    CONSTRAINT chk_transfer_locations CHECK (from_location_id != to_location_id),
    CONSTRAINT chk_transfer_quantity CHECK (quantity > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Stock transfers between locations within same warehouse';

CREATE INDEX idx_transfer_product ON stock_transfers(product_id);
CREATE INDEX idx_transfer_warehouse ON stock_transfers(warehouse_id);
CREATE INDEX idx_transfer_from_location ON stock_transfers(from_location_id);
CREATE INDEX idx_transfer_to_location ON stock_transfers(to_location_id);
CREATE INDEX idx_transfer_status ON stock_transfers(status);

-- ============================================================================
-- End of Migration
-- ============================================================================
```

---

## 📝 Sample Data

```sql
-- Sample inventory records
INSERT INTO inventory (id, product_id, warehouse_id, location_id, batch_id, 
                       on_hand_quantity, reserved_quantity, created_by)
VALUES 
(UUID(), '{{product_1}}', '{{warehouse_main}}', '{{location_a1}}', '{{batch_001}}', 
 500.00, 50.00, '{{admin_user}}'),
(UUID(), '{{product_1}}', '{{warehouse_main}}', '{{location_b2}}', '{{batch_002}}', 
 300.00, 0.00, '{{admin_user}}'),
(UUID(), '{{product_2}}', '{{warehouse_main}}', '{{location_a1}}', NULL, 
 1000.00, 200.00, '{{admin_user}}');
```

---

## 🔍 Query Examples

### 1. Check Available Stock

```sql
SELECT 
    SUM(on_hand_quantity - reserved_quantity) AS available_quantity
FROM inventory
WHERE product_id = ?
    AND warehouse_id = ?
    AND (batch_id IS NULL OR batch_id IN (
        SELECT id FROM batches WHERE status = 'AVAILABLE'
    ));
```

### 2. Reserve Stock (with locking)

```sql
START TRANSACTION;

SELECT id, on_hand_quantity, reserved_quantity, version
FROM inventory
WHERE product_id = ? AND warehouse_id = ? AND location_id = ? AND batch_id = ?
FOR UPDATE;

UPDATE inventory
SET reserved_quantity = reserved_quantity + ?,
    version = version + 1,
    updated_at = NOW()
WHERE id = ? AND version = ?;

COMMIT;
```

### 3. Find Low Stock Products

```sql
SELECT 
    p.sku,
    p.name,
    p.reorder_point,
    SUM(i.on_hand_quantity) AS total_on_hand,
    SUM(i.reserved_quantity) AS total_reserved,
    SUM(i.on_hand_quantity - i.reserved_quantity) AS total_available
FROM products p
INNER JOIN inventory i ON p.id = i.product_id
WHERE p.status = 'ACTIVE'
GROUP BY p.id, p.sku, p.name, p.reorder_point
HAVING total_available < p.reorder_point
ORDER BY total_available ASC;
```

---

**Document Version:** 1.0  
**Last Updated:** February 1, 2026  
**Status:** 🚧 Draft - Pending Review
