# Database Schema - Module 7: Stock Movement & Audit Trail
## Database Design & Migration Guide

---

## 📋 Document Information

| Property | Value |
|----------|-------|
| **Module** | Stock Movement & Audit Trail |
| **Version** | 1.0 |
| **Date** | February 1, 2026 |
| **Status** | Draft |

---

## 🗂️ Schema Overview

### ERD Diagram

```
┌────────────────────────────────────────────────────┐
│              stock_movements                       │
│                                                    │
│  Immutable audit log of all inventory changes     │
│                                                    │
│  - id (PK)                                        │
│  - movement_type (INBOUND/OUTBOUND/...)           │
│  - product_id (FK) ────────────→ products         │
│  - warehouse_id (FK) ───────────→ warehouses      │
│  - location_id (FK) ─────────────→ locations      │
│  - batch_id (FK) ────────────────→ batches        │
│  - quantity_change (+/-)                          │
│  - quantity_before                                │
│  - quantity_after                                 │
│  - movement_date                                  │
│  - reference_type (PO/SO/ADJUSTMENT/TRANSFER)     │
│  - reference_id                                   │
│  - reference_number (PO-2026-0001, ...)           │
│  - notes                                          │
│  - created_at (immutable)                         │
│  - created_by                                     │
└────────────────────────────────────────────────────┘
```

---

## 📊 Table Definition

### Table: stock_movements

```sql
CREATE TABLE stock_movements (
    -- Primary Key
    id                      CHAR(36) PRIMARY KEY,
    
    -- Movement Classification
    movement_type           ENUM(
                                'INBOUND',              -- Goods received
                                'OUTBOUND',             -- Goods shipped
                                'ADJUSTMENT_INCREASE',  -- Manual increase
                                'ADJUSTMENT_DECREASE',  -- Manual decrease
                                'TRANSFER_OUT',         -- From location
                                'TRANSFER_IN',          -- To location
                                'RESERVE',              -- Stock allocated
                                'UNRESERVE'             -- Allocation released
                            ) NOT NULL,
    
    -- Location Details
    product_id              CHAR(36) NOT NULL 
                            COMMENT 'FK to products',
    warehouse_id            CHAR(36) NOT NULL 
                            COMMENT 'FK to warehouses',
    location_id             CHAR(36) 
                            COMMENT 'FK to locations (nullable if not specific)',
    batch_id                CHAR(36) 
                            COMMENT 'FK to batches (nullable for non-batch products)',
    
    -- Quantity Tracking
    quantity_change         DECIMAL(15, 2) NOT NULL 
                            COMMENT 'Positive for increase, negative for decrease',
    quantity_before         DECIMAL(15, 2) NOT NULL 
                            COMMENT 'Quantity before this movement',
    quantity_after          DECIMAL(15, 2) NOT NULL 
                            COMMENT 'Quantity after this movement',
    
    -- Business Date
    movement_date           TIMESTAMP NOT NULL 
                            COMMENT 'Business date/time of movement',
    
    -- Reference to Source Document
    reference_type          ENUM('PURCHASE_ORDER', 'SALES_ORDER', 'INBOUND_RECEIPT', 
                                 'OUTBOUND_SHIPMENT', 'STOCK_ADJUSTMENT', 
                                 'STOCK_TRANSFER', 'SYSTEM') NOT NULL,
    reference_id            CHAR(36) 
                            COMMENT 'ID of source document',
    reference_number        VARCHAR(50) 
                            COMMENT 'Human-readable reference (PO-2026-0001)',
    
    -- Additional Context
    notes                   TEXT 
                            COMMENT 'Additional notes or reason',
    
    -- Audit (Immutable)
    created_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP 
                            COMMENT 'When record was created (never updated)',
    created_by              CHAR(36) 
                            COMMENT 'User who triggered the movement',
    
    -- Foreign Keys
    CONSTRAINT fk_movement_product FOREIGN KEY (product_id) 
        REFERENCES products(id) ON DELETE RESTRICT,
    CONSTRAINT fk_movement_warehouse FOREIGN KEY (warehouse_id) 
        REFERENCES warehouses(id) ON DELETE RESTRICT,
    CONSTRAINT fk_movement_location FOREIGN KEY (location_id) 
        REFERENCES locations(id) ON DELETE RESTRICT,
    CONSTRAINT fk_movement_batch FOREIGN KEY (batch_id) 
        REFERENCES batches(id) ON DELETE RESTRICT,
    CONSTRAINT fk_movement_created_by FOREIGN KEY (created_by) 
        REFERENCES accounts(id) ON DELETE SET NULL,
    
    -- Check Constraints
    CONSTRAINT chk_movement_quantity_calc CHECK (
        quantity_after = quantity_before + quantity_change
    ),
    CONSTRAINT chk_movement_quantities CHECK (
        quantity_before >= 0 AND quantity_after >= 0
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Immutable audit log of all inventory changes';
```

---

## 📑 Indexes

### Performance Indexes

```sql
-- Primary Key
-- Already has clustered index on id

-- Product movement history (most common query)
CREATE INDEX idx_movement_product_date 
    ON stock_movements(product_id, movement_date DESC);

-- Batch traceability
CREATE INDEX idx_movement_batch 
    ON stock_movements(batch_id, movement_date DESC);

-- Warehouse analytics
CREATE INDEX idx_movement_warehouse_date 
    ON stock_movements(warehouse_id, movement_date DESC);

-- Location tracking
CREATE INDEX idx_movement_location 
    ON stock_movements(location_id, movement_date DESC);

-- Movement type filtering
CREATE INDEX idx_movement_type 
    ON stock_movements(movement_type, movement_date DESC);

-- Reference lookup (find movements for specific PO/SO/etc.)
CREATE INDEX idx_movement_reference 
    ON stock_movements(reference_type, reference_id);

-- User activity tracking
CREATE INDEX idx_movement_created_by 
    ON stock_movements(created_by, created_at DESC);

-- Date range queries
CREATE INDEX idx_movement_date 
    ON stock_movements(movement_date DESC);

-- Composite for common filter combination
CREATE INDEX idx_movement_product_warehouse 
    ON stock_movements(product_id, warehouse_id, movement_date DESC);
```

### Index Usage Patterns

| Query Type | Index Used | Estimated Performance |
|------------|------------|----------------------|
| Product history | idx_movement_product_date | < 50ms |
| Batch traceability | idx_movement_batch | < 100ms |
| Warehouse activity | idx_movement_warehouse_date | < 50ms |
| Reference lookup | idx_movement_reference | < 10ms |
| Date range filter | idx_movement_date | < 100ms |

---

## 🔄 Flyway Migration

### File: `V20260201_05__Create_stock_movements.sql`

```sql
-- ============================================================================
-- Flyway Migration: Create Stock Movement & Audit Trail Module
-- Version: V20260201_05
-- Description: Create stock_movements table for complete audit trail
-- Author: Database Team
-- Date: 2026-02-01
-- ============================================================================

CREATE TABLE stock_movements (
    id                      CHAR(36) PRIMARY KEY,
    movement_type           ENUM(
                                'INBOUND',
                                'OUTBOUND',
                                'ADJUSTMENT_INCREASE',
                                'ADJUSTMENT_DECREASE',
                                'TRANSFER_OUT',
                                'TRANSFER_IN',
                                'RESERVE',
                                'UNRESERVE'
                            ) NOT NULL,
    product_id              CHAR(36) NOT NULL,
    warehouse_id            CHAR(36) NOT NULL,
    location_id             CHAR(36),
    batch_id                CHAR(36),
    quantity_change         DECIMAL(15, 2) NOT NULL,
    quantity_before         DECIMAL(15, 2) NOT NULL,
    quantity_after          DECIMAL(15, 2) NOT NULL,
    movement_date           TIMESTAMP NOT NULL,
    reference_type          ENUM('PURCHASE_ORDER', 'SALES_ORDER', 'INBOUND_RECEIPT', 
                                 'OUTBOUND_SHIPMENT', 'STOCK_ADJUSTMENT', 
                                 'STOCK_TRANSFER', 'SYSTEM') NOT NULL,
    reference_id            CHAR(36),
    reference_number        VARCHAR(50),
    notes                   TEXT,
    created_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by              CHAR(36),
    
    CONSTRAINT fk_movement_product FOREIGN KEY (product_id) 
        REFERENCES products(id) ON DELETE RESTRICT,
    CONSTRAINT fk_movement_warehouse FOREIGN KEY (warehouse_id) 
        REFERENCES warehouses(id) ON DELETE RESTRICT,
    CONSTRAINT fk_movement_location FOREIGN KEY (location_id) 
        REFERENCES locations(id) ON DELETE RESTRICT,
    CONSTRAINT fk_movement_batch FOREIGN KEY (batch_id) 
        REFERENCES batches(id) ON DELETE RESTRICT,
    CONSTRAINT fk_movement_created_by FOREIGN KEY (created_by) 
        REFERENCES accounts(id) ON DELETE SET NULL,
    CONSTRAINT chk_movement_quantity_calc CHECK (
        quantity_after = quantity_before + quantity_change
    ),
    CONSTRAINT chk_movement_quantities CHECK (
        quantity_before >= 0 AND quantity_after >= 0
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Immutable audit log of all inventory changes';

-- Create indexes
CREATE INDEX idx_movement_product_date 
    ON stock_movements(product_id, movement_date DESC);

CREATE INDEX idx_movement_batch 
    ON stock_movements(batch_id, movement_date DESC);

CREATE INDEX idx_movement_warehouse_date 
    ON stock_movements(warehouse_id, movement_date DESC);

CREATE INDEX idx_movement_type 
    ON stock_movements(movement_type, movement_date DESC);

CREATE INDEX idx_movement_reference 
    ON stock_movements(reference_type, reference_id);

CREATE INDEX idx_movement_date 
    ON stock_movements(movement_date DESC);

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
```

---

## 📝 Sample Data

```sql
-- Example movement records (replace UUIDs with actual values)

-- INBOUND movement (from goods receipt)
INSERT INTO stock_movements (
    id, movement_type, product_id, warehouse_id, location_id, batch_id,
    quantity_change, quantity_before, quantity_after,
    movement_date, reference_type, reference_id, reference_number,
    notes, created_by
) VALUES (
    UUID(),
    'INBOUND',
    '{{product_uuid}}',
    '{{warehouse_uuid}}',
    '{{location_uuid}}',
    '{{batch_uuid}}',
    100.00,
    0.00,
    100.00,
    NOW(),
    'INBOUND_RECEIPT',
    '{{receipt_uuid}}',
    'GR-2026-0001',
    'Received from supplier XYZ',
    '{{user_uuid}}'
);

-- OUTBOUND movement (from shipment)
INSERT INTO stock_movements (
    id, movement_type, product_id, warehouse_id, location_id, batch_id,
    quantity_change, quantity_before, quantity_after,
    movement_date, reference_type, reference_id, reference_number,
    notes, created_by
) VALUES (
    UUID(),
    'OUTBOUND',
    '{{product_uuid}}',
    '{{warehouse_uuid}}',
    '{{location_uuid}}',
    '{{batch_uuid}}',
    -50.00,
    100.00,
    50.00,
    NOW(),
    'OUTBOUND_SHIPMENT',
    '{{shipment_uuid}}',
    'SHIP-2026-0001',
    'Shipped to customer ABC',
    '{{user_uuid}}'
);
```

---

## 🔍 Query Examples

### 1. Product Movement History

```sql
SELECT 
    sm.movement_date,
    sm.movement_type,
    sm.quantity_change,
    sm.quantity_before,
    sm.quantity_after,
    sm.reference_type,
    sm.reference_number,
    w.name AS warehouse,
    l.name AS location,
    b.batch_number,
    a.username AS performed_by,
    sm.notes
FROM stock_movements sm
INNER JOIN warehouses w ON sm.warehouse_id = w.id
LEFT JOIN locations l ON sm.location_id = l.id
LEFT JOIN batches b ON sm.batch_id = b.id
LEFT JOIN accounts a ON sm.created_by = a.id
WHERE sm.product_id = ?
ORDER BY sm.movement_date DESC
LIMIT 100;
```

### 2. Batch Forward Traceability (Batch → Customers)

```sql
-- Find all outbound shipments for a batch
SELECT 
    sm.movement_date AS shipment_date,
    sm.quantity_change AS quantity_shipped,
    sm.reference_number AS shipment_number,
    os.tracking_number,
    so.so_number AS sales_order,
    bp.name AS customer_name,
    bp.email AS customer_email
FROM stock_movements sm
INNER JOIN outbound_shipments os ON sm.reference_id = os.id 
    AND sm.reference_type = 'OUTBOUND_SHIPMENT'
INNER JOIN sales_orders so ON os.sales_order_id = so.id
INNER JOIN business_partners bp ON so.customer_id = bp.id
WHERE sm.batch_id = ?
    AND sm.movement_type = 'OUTBOUND'
ORDER BY sm.movement_date DESC;
```

### 3. Batch Backward Traceability (Batch → Supplier)

```sql
-- Find original receipt for a batch
SELECT 
    sm.movement_date AS receipt_date,
    sm.quantity_change AS quantity_received,
    sm.reference_number AS receipt_number,
    po.po_number AS purchase_order,
    bp.name AS supplier_name,
    bp.email AS supplier_email
FROM stock_movements sm
INNER JOIN inbound_receipts ir ON sm.reference_id = ir.id 
    AND sm.reference_type = 'INBOUND_RECEIPT'
INNER JOIN purchase_orders po ON ir.purchase_order_id = po.id
INNER JOIN business_partners bp ON po.supplier_id = bp.id
WHERE sm.batch_id = ?
    AND sm.movement_type = 'INBOUND'
ORDER BY sm.movement_date ASC
LIMIT 1;
```

### 4. Warehouse Activity Summary

```sql
SELECT 
    DATE(sm.movement_date) AS activity_date,
    sm.movement_type,
    COUNT(*) AS transaction_count,
    SUM(ABS(sm.quantity_change)) AS total_quantity
FROM stock_movements sm
WHERE sm.warehouse_id = ?
    AND sm.movement_date BETWEEN ? AND ?
    AND sm.movement_type NOT IN ('RESERVE', 'UNRESERVE') -- Exclude non-physical
GROUP BY DATE(sm.movement_date), sm.movement_type
ORDER BY activity_date DESC, sm.movement_type;
```

### 5. User Activity Audit

```sql
SELECT 
    DATE(sm.created_at) AS date,
    a.username,
    sm.movement_type,
    COUNT(*) AS movements,
    SUM(ABS(sm.quantity_change)) AS total_quantity
FROM stock_movements sm
INNER JOIN accounts a ON sm.created_by = a.id
WHERE sm.created_at BETWEEN ? AND ?
GROUP BY DATE(sm.created_at), a.username, sm.movement_type
ORDER BY date DESC, total_quantity DESC;
```

---

## 🎯 Performance Considerations

### Table Size Estimation

| Warehouse Size | Daily Movements | Annual Movements | Est. Table Size (5 years) |
|----------------|-----------------|------------------|---------------------------|
| Small | 100 | 36,500 | ~50 MB |
| Medium | 1,000 | 365,000 | ~500 MB |
| Large | 10,000 | 3,650,000 | ~5 GB |

### Optimization Strategies

1. **Partitioning (if needed for large warehouses)**:
   ```sql
   -- Partition by year
   ALTER TABLE stock_movements 
   PARTITION BY RANGE (YEAR(movement_date)) (
       PARTITION p2024 VALUES LESS THAN (2025),
       PARTITION p2025 VALUES LESS THAN (2026),
       PARTITION p2026 VALUES LESS THAN (2027)
   );
   ```

2. **Archiving old data** (optional):
   - Move movements older than 5 years to archive table
   - Keep archive accessible for compliance queries

3. **Query optimization**:
   - Always include date range in queries
   - Use product_id or batch_id filters
   - Limit result sets with pagination

---

## ✅ Data Integrity Checks

```sql
-- Check 1: Verify quantity calculations
SELECT * FROM stock_movements
WHERE quantity_after != quantity_before + quantity_change;

-- Check 2: Find negative quantities (should be none)
SELECT * FROM stock_movements
WHERE quantity_before < 0 OR quantity_after < 0;

-- Check 3: Orphaned movements (missing references)
SELECT 
    sm.id,
    sm.reference_type,
    sm.reference_id
FROM stock_movements sm
WHERE sm.reference_type = 'INBOUND_RECEIPT'
    AND NOT EXISTS (
        SELECT 1 FROM inbound_receipts ir WHERE ir.id = sm.reference_id
    );
```

---

**Document Version:** 1.0  
**Last Updated:** February 1, 2026  
**Status:** 🚧 Draft - Pending Review
