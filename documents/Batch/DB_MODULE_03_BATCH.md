# Database Schema - Module 3: Batch Management
## Database Design & Migration Guide

---

## 📋 Document Information

| Property | Value |
|----------|-------|
| **Module** | Batch Management |
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
┌──────────────────┐
│    products      │
│                  │
│ - id (PK)        │
│ - sku            │
│ - requires_batch_│
│   tracking       │
└────────┬─────────┘
         │
         │ 1:N
         │
┌────────▼─────────────────────────────┐
│           batches                    │
│                                      │
│ - id (PK)                           │
│ - batch_number (UK per product)     │
│ - product_id (FK) ──────────────────┤
│ - manufacturing_date                │
│ - expiry_date                       │
│ - status (AVAILABLE/QUARANTINE/...)│
│ - supplier_batch_number             │
│ - notes                             │
│ - created_at, updated_at            │
│ - created_by, updated_by            │
└─────────┬────────────────────────────┘
          │
          │ Referenced by:
          │ - inventory.batch_id
          │ - inbound_receipt_lines.batch_id
          │ - outbound_shipment_lines.batch_id
          │ - stock_movements.batch_id
          │
```

---

## 📊 Table Definitions

### Table: `batches`

Stores batch master data for products that require batch tracking.

```sql
CREATE TABLE batches (
    -- Primary Key
    id                      CHAR(36) PRIMARY KEY 
                            COMMENT 'UUID primary key',
    
    -- Batch Identification
    batch_number            VARCHAR(50) NOT NULL 
                            COMMENT 'Batch/Lot number (unique per product)',
    product_id              CHAR(36) NOT NULL 
                            COMMENT 'Foreign key to products table',
    
    -- Date Information
    manufacturing_date      DATE NOT NULL 
                            COMMENT 'Date when batch was manufactured',
    expiry_date             DATE 
                            COMMENT 'Date when batch expires (nullable for non-perishables)',
    
    -- Status Management
    status                  ENUM('AVAILABLE', 'QUARANTINE', 'EXPIRED', 'RECALLED') 
                            NOT NULL DEFAULT 'AVAILABLE'
                            COMMENT 'Current status of batch',
    
    -- Traceability
    supplier_batch_number   VARCHAR(50) 
                            COMMENT 'Original batch number from supplier',
    
    -- Additional Information
    notes                   TEXT 
                            COMMENT 'Additional notes or quality information',
    
    -- Audit Fields
    created_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP 
                            COMMENT 'Record creation timestamp',
    updated_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP 
                            ON UPDATE CURRENT_TIMESTAMP 
                            COMMENT 'Last update timestamp',
    created_by              CHAR(36) 
                            COMMENT 'User who created this record',
    updated_by              CHAR(36) 
                            COMMENT 'User who last updated this record',
    
    -- Constraints
    CONSTRAINT uk_product_batch UNIQUE (product_id, batch_number),
    CONSTRAINT fk_batch_product FOREIGN KEY (product_id) 
        REFERENCES products(id) ON DELETE RESTRICT,
    CONSTRAINT fk_batch_created_by FOREIGN KEY (created_by) 
        REFERENCES accounts(id) ON DELETE SET NULL,
    CONSTRAINT fk_batch_updated_by FOREIGN KEY (updated_by) 
        REFERENCES accounts(id) ON DELETE SET NULL,
    
    -- Check Constraints
    CONSTRAINT chk_batch_dates CHECK (
        expiry_date IS NULL OR expiry_date > manufacturing_date
    ),
    CONSTRAINT chk_batch_manufacturing_date CHECK (
        manufacturing_date <= CURRENT_DATE
    )
);
```

### Field Descriptions

| Field | Type | Nullable | Description | Business Rules |
|-------|------|----------|-------------|----------------|
| `id` | CHAR(36) | No | UUID primary key | Auto-generated |
| `batch_number` | VARCHAR(50) | No | Batch identifier | Unique per product, alphanumeric |
| `product_id` | CHAR(36) | No | Link to product | Must be batch-tracked product |
| `manufacturing_date` | DATE | No | Production date | Cannot be future date |
| `expiry_date` | DATE | Yes | Expiration date | Must be after manufacturing date if set |
| `status` | ENUM | No | Batch status | Defaults to AVAILABLE |
| `supplier_batch_number` | VARCHAR(50) | Yes | Supplier's batch ref | For traceability |
| `notes` | TEXT | Yes | Additional info | Free text |
| `created_at` | TIMESTAMP | No | Creation time | Auto-set |
| `updated_at` | TIMESTAMP | No | Last update time | Auto-updated |
| `created_by` | CHAR(36) | Yes | Creator user ID | From auth context |
| `updated_by` | CHAR(36) | Yes | Last updater ID | From auth context |

---

## 🔗 Relationships

### Foreign Keys

| From Table | From Column | To Table | To Column | On Delete | Description |
|------------|-------------|----------|-----------|-----------|-------------|
| batches | product_id | products | id | RESTRICT | Each batch belongs to one product |
| batches | created_by | accounts | id | SET NULL | Track who created batch |
| batches | updated_by | accounts | id | SET NULL | Track who updated batch |

### Referenced By

| Table | Column | Relationship | Description |
|-------|--------|--------------|-------------|
| inventory | batch_id | N:1 | Inventory records reference batches |
| inbound_receipt_lines | batch_id | N:1 | Receipts track which batch received |
| outbound_shipment_lines | batch_id | N:1 | Shipments track which batch shipped |
| stock_movements | batch_id | N:1 | All movements reference batches |

---

## 📑 Indexes

### Primary Index
- `PRIMARY KEY (id)` - Clustered index for row lookups

### Unique Indexes
- `uk_product_batch (product_id, batch_number)` - Ensure batch number unique per product

### Secondary Indexes

```sql
-- Index for querying batches by product
CREATE INDEX idx_batch_product_id 
    ON batches(product_id);

-- Index for status filtering
CREATE INDEX idx_batch_status 
    ON batches(status);

-- Index for expiry date queries (expiry management)
CREATE INDEX idx_batch_expiry_date 
    ON batches(expiry_date) 
    WHERE expiry_date IS NOT NULL;

-- Index for manufacturing date (FIFO ordering)
CREATE INDEX idx_batch_manufacturing_date 
    ON batches(manufacturing_date);

-- Composite index for FIFO queries with status filter
CREATE INDEX idx_batch_fifo 
    ON batches(product_id, status, manufacturing_date, expiry_date);

-- Index for expiring batches query
CREATE INDEX idx_batch_expiring 
    ON batches(status, expiry_date) 
    WHERE status = 'AVAILABLE' AND expiry_date IS NOT NULL;

-- Index for batch number search
CREATE INDEX idx_batch_number 
    ON batches(batch_number);
```

### Index Usage Patterns

| Query Pattern | Index Used | Performance |
|---------------|------------|-------------|
| Find batch by ID | PRIMARY KEY | O(1) |
| Find batches by product | idx_batch_product_id | O(log n) |
| Filter by status | idx_batch_status | O(log n) |
| Expiry check (daily job) | idx_batch_expiring | Optimized |
| FIFO recommendations | idx_batch_fifo | Optimized |
| Batch number search | idx_batch_number | O(log n) |

---

## ⚙️ Constraints

### Business Rule Constraints

```sql
-- Expiry date must be after manufacturing date
CONSTRAINT chk_batch_dates CHECK (
    expiry_date IS NULL OR expiry_date > manufacturing_date
);

-- Manufacturing date cannot be in the future
CONSTRAINT chk_batch_manufacturing_date CHECK (
    manufacturing_date <= CURRENT_DATE
);
```

### Referential Integrity

- `ON DELETE RESTRICT` on product_id: Cannot delete product if batches exist
- `ON DELETE SET NULL` on created_by/updated_by: Preserve batch if user deleted

---

## 🔄 Flyway Migration

### Migration File: `V20260201_01__Create_batch_management.sql`

```sql
-- ============================================================================
-- Flyway Migration: Create Batch Management Module
-- Version: V20260201_01
-- Description: Create batches table with indexes and constraints
-- Author: Database Team
-- Date: 2026-02-01
-- ============================================================================

-- Table: batches
CREATE TABLE batches (
    id                      CHAR(36) PRIMARY KEY,
    batch_number            VARCHAR(50) NOT NULL,
    product_id              CHAR(36) NOT NULL,
    manufacturing_date      DATE NOT NULL,
    expiry_date             DATE,
    status                  ENUM('AVAILABLE', 'QUARANTINE', 'EXPIRED', 'RECALLED') 
                            NOT NULL DEFAULT 'AVAILABLE',
    supplier_batch_number   VARCHAR(50),
    notes                   TEXT,
    created_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP 
                            ON UPDATE CURRENT_TIMESTAMP,
    created_by              CHAR(36),
    updated_by              CHAR(36),
    
    -- Unique constraint
    CONSTRAINT uk_product_batch UNIQUE (product_id, batch_number),
    
    -- Foreign keys
    CONSTRAINT fk_batch_product FOREIGN KEY (product_id) 
        REFERENCES products(id) ON DELETE RESTRICT,
    CONSTRAINT fk_batch_created_by FOREIGN KEY (created_by) 
        REFERENCES accounts(id) ON DELETE SET NULL,
    CONSTRAINT fk_batch_updated_by FOREIGN KEY (updated_by) 
        REFERENCES accounts(id) ON DELETE SET NULL,
    
    -- Check constraints
    CONSTRAINT chk_batch_dates CHECK (
        expiry_date IS NULL OR expiry_date > manufacturing_date
    ),
    CONSTRAINT chk_batch_manufacturing_date CHECK (
        manufacturing_date <= CURRENT_DATE
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Batch/Lot tracking for products';

-- Indexes
CREATE INDEX idx_batch_product_id ON batches(product_id);
CREATE INDEX idx_batch_status ON batches(status);
CREATE INDEX idx_batch_expiry_date ON batches(expiry_date);
CREATE INDEX idx_batch_manufacturing_date ON batches(manufacturing_date);
CREATE INDEX idx_batch_fifo ON batches(product_id, status, manufacturing_date, expiry_date);
CREATE INDEX idx_batch_number ON batches(batch_number);

-- ============================================================================
-- End of Migration
-- ============================================================================
```

### Rollback Script: `V20260201_01__Create_batch_management_rollback.sql`

```sql
-- ============================================================================
-- Flyway Rollback: Drop Batch Management Module
-- Note: Use with caution - data will be lost
-- ============================================================================

-- Drop indexes first
DROP INDEX IF EXISTS idx_batch_product_id ON batches;
DROP INDEX IF EXISTS idx_batch_status ON batches;
DROP INDEX IF EXISTS idx_batch_expiry_date ON batches;
DROP INDEX IF EXISTS idx_batch_manufacturing_date ON batches;
DROP INDEX IF EXISTS idx_batch_fifo ON batches;
DROP INDEX IF EXISTS idx_batch_number ON batches;

-- Drop table
DROP TABLE IF EXISTS batches;

-- ============================================================================
-- End of Rollback
-- ============================================================================
```

---

## 📝 Sample Data

### Test Data for Development

```sql
-- Sample batches for development/testing
-- Note: Replace UUID and foreign keys with actual values from your environment

-- Batch 1: Recent batch with expiry
INSERT INTO batches (
    id, batch_number, product_id, 
    manufacturing_date, expiry_date, 
    status, supplier_batch_number, 
    notes, created_by
) VALUES (
    UUID(), 
    'BATCH-2026-001', 
    '{{product_uuid_1}}',
    '2026-01-15',
    '2027-01-15',
    'AVAILABLE',
    'SUPPLIER-BATCH-2026-001',
    'Quality inspection passed on 2026-01-16',
    '{{admin_user_uuid}}'
);

-- Batch 2: Older batch (for FIFO testing)
INSERT INTO batches (
    id, batch_number, product_id, 
    manufacturing_date, expiry_date, 
    status, supplier_batch_number, 
    notes, created_by
) VALUES (
    UUID(), 
    'BATCH-2025-099', 
    '{{product_uuid_1}}',
    '2025-12-01',
    '2026-12-01',
    'AVAILABLE',
    'SUPPLIER-BATCH-2025-099',
    'Older batch - should be picked first (FIFO)',
    '{{admin_user_uuid}}'
);

-- Batch 3: Quarantined batch
INSERT INTO batches (
    id, batch_number, product_id, 
    manufacturing_date, expiry_date, 
    status, supplier_batch_number, 
    notes, created_by
) VALUES (
    UUID(), 
    'BATCH-2026-002', 
    '{{product_uuid_2}}',
    '2026-01-20',
    '2027-01-20',
    'QUARANTINE',
    'SUPPLIER-BATCH-2026-002',
    'Quarantined due to quality concerns - sample sent to lab',
    '{{quality_controller_uuid}}'
);

-- Batch 4: Expiring soon (for alert testing)
INSERT INTO batches (
    id, batch_number, product_id, 
    manufacturing_date, expiry_date, 
    status, supplier_batch_number, 
    notes, created_by
) VALUES (
    UUID(), 
    'BATCH-2025-050', 
    '{{product_uuid_3}}',
    '2025-02-01',
    DATE_ADD(CURRENT_DATE, INTERVAL 10 DAY),
    'AVAILABLE',
    'SUPPLIER-BATCH-2025-050',
    'Expiring in 10 days - priority shipment required',
    '{{admin_user_uuid}}'
);

-- Batch 5: No expiry date (non-perishable)
INSERT INTO batches (
    id, batch_number, product_id, 
    manufacturing_date, expiry_date, 
    status, supplier_batch_number, 
    notes, created_by
) VALUES (
    UUID(), 
    'BATCH-2026-STEEL-001', 
    '{{product_uuid_4}}',
    '2026-01-10',
    NULL,
    'AVAILABLE',
    'SUPPLIER-BATCH-STEEL-001',
    'Steel parts - no expiry date',
    '{{admin_user_uuid}}'
);
```

---

## 🔍 Query Examples

### 1. Find All Available Batches for a Product (FIFO Order)

```sql
-- Used by FIFO recommendation system
SELECT 
    b.id,
    b.batch_number,
    b.manufacturing_date,
    b.expiry_date,
    DATEDIFF(b.expiry_date, CURRENT_DATE) AS days_to_expiry,
    SUM(i.on_hand_quantity) AS available_quantity
FROM batches b
LEFT JOIN inventory i ON b.id = i.batch_id
WHERE b.product_id = ?
    AND b.status = 'AVAILABLE'
    AND (b.expiry_date IS NULL OR b.expiry_date > CURRENT_DATE)
GROUP BY b.id, b.batch_number, b.manufacturing_date, b.expiry_date
HAVING available_quantity > 0
ORDER BY 
    b.manufacturing_date ASC,  -- FIFO: oldest first
    b.expiry_date ASC;         -- Within same mfg date, earliest expiry first
```

### 2. Find Batches Expiring Soon (For Alert System)

```sql
-- Daily scheduled job to find expiring batches
SELECT 
    b.id,
    b.batch_number,
    p.sku,
    p.name AS product_name,
    b.expiry_date,
    DATEDIFF(b.expiry_date, CURRENT_DATE) AS days_to_expiry,
    SUM(i.on_hand_quantity) AS quantity_on_hand,
    w.name AS warehouse_name,
    CASE 
        WHEN DATEDIFF(b.expiry_date, CURRENT_DATE) <= 7 THEN 'CRITICAL'
        WHEN DATEDIFF(b.expiry_date, CURRENT_DATE) <= 14 THEN 'WARNING'
        ELSE 'INFO'
    END AS urgency_level
FROM batches b
INNER JOIN products p ON b.product_id = p.id
LEFT JOIN inventory i ON b.id = i.batch_id
LEFT JOIN warehouses w ON i.warehouse_id = w.id
WHERE b.status = 'AVAILABLE'
    AND b.expiry_date IS NOT NULL
    AND b.expiry_date BETWEEN CURRENT_DATE AND DATE_ADD(CURRENT_DATE, INTERVAL 30 DAY)
GROUP BY b.id, b.batch_number, p.sku, p.name, b.expiry_date, w.name
HAVING quantity_on_hand > 0
ORDER BY days_to_expiry ASC, quantity_on_hand DESC;
```

### 3. Mark Expired Batches (Daily Scheduled Job)

```sql
-- Transaction to mark expired batches
START TRANSACTION;

UPDATE batches
SET 
    status = 'EXPIRED',
    updated_at = CURRENT_TIMESTAMP,
    updated_by = 'SYSTEM'
WHERE status = 'AVAILABLE'
    AND expiry_date IS NOT NULL
    AND expiry_date < CURRENT_DATE;

-- Log affected batches for notification
SELECT 
    b.id,
    b.batch_number,
    p.sku,
    p.name AS product_name,
    b.expiry_date,
    SUM(i.on_hand_quantity) AS expired_quantity
FROM batches b
INNER JOIN products p ON b.product_id = p.id
LEFT JOIN inventory i ON b.id = i.batch_id
WHERE b.status = 'EXPIRED'
    AND b.updated_at >= DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 1 MINUTE)
GROUP BY b.id, b.batch_number, p.sku, p.name, b.expiry_date;

COMMIT;
```

### 4. Batch Traceability Query

```sql
-- Complete traceability for a batch
SELECT 
    'INBOUND' AS transaction_type,
    ir.receipt_number AS document_number,
    ir.receipt_date AS transaction_date,
    irl.quantity,
    bp.name AS partner_name,
    w.name AS warehouse,
    l.name AS location
FROM inbound_receipt_lines irl
INNER JOIN inbound_receipts ir ON irl.receipt_id = ir.id
INNER JOIN business_partners bp ON ir.supplier_id = bp.id
INNER JOIN warehouses w ON ir.warehouse_id = w.id
LEFT JOIN locations l ON irl.location_id = l.id
WHERE irl.batch_id = ?

UNION ALL

SELECT 
    'OUTBOUND' AS transaction_type,
    os.shipment_number AS document_number,
    os.shipped_at AS transaction_date,
    osl.quantity_shipped AS quantity,
    bp.name AS partner_name,
    w.name AS warehouse,
    l.name AS location
FROM outbound_shipment_lines osl
INNER JOIN outbound_shipments os ON osl.shipment_id = os.id
INNER JOIN sales_orders so ON os.sales_order_id = so.id
INNER JOIN business_partners bp ON so.customer_id = bp.id
INNER JOIN warehouses w ON os.warehouse_id = w.id
LEFT JOIN locations l ON osl.location_id = l.id
WHERE osl.batch_id = ?

UNION ALL

SELECT 
    sm.movement_type AS transaction_type,
    sm.reference_number AS document_number,
    sm.movement_date AS transaction_date,
    sm.quantity_change AS quantity,
    NULL AS partner_name,
    w.name AS warehouse,
    l.name AS location
FROM stock_movements sm
INNER JOIN warehouses w ON sm.warehouse_id = w.id
LEFT JOIN locations l ON sm.location_id = l.id
WHERE sm.batch_id = ?

ORDER BY transaction_date DESC;
```

### 5. Batch Inventory Summary

```sql
-- Summary of inventory by batch
SELECT 
    b.id,
    b.batch_number,
    p.sku,
    p.name AS product_name,
    b.manufacturing_date,
    b.expiry_date,
    b.status AS batch_status,
    SUM(i.on_hand_quantity) AS total_on_hand,
    SUM(i.reserved_quantity) AS total_reserved,
    SUM(i.on_hand_quantity - i.reserved_quantity) AS total_available,
    COUNT(DISTINCT i.warehouse_id) AS warehouse_count,
    COUNT(DISTINCT i.location_id) AS location_count
FROM batches b
INNER JOIN products p ON b.product_id = p.id
LEFT JOIN inventory i ON b.id = i.batch_id
WHERE b.product_id = ?
GROUP BY b.id, b.batch_number, p.sku, p.name, b.manufacturing_date, b.expiry_date, b.status
ORDER BY b.manufacturing_date ASC;
```

### 6. Quarantine Status History

```sql
-- Track batch status changes (requires audit log table)
SELECT 
    b.batch_number,
    b.status AS current_status,
    al.action,
    al.old_value AS previous_status,
    al.new_value AS new_status,
    al.reason,
    al.created_at AS status_changed_at,
    a.username AS changed_by
FROM batches b
LEFT JOIN audit_logs al ON al.entity_type = 'BATCH' 
    AND al.entity_id = b.id 
    AND al.field_name = 'status'
LEFT JOIN accounts a ON al.created_by = a.id
WHERE b.id = ?
ORDER BY al.created_at DESC;
```

---

## 🎯 Performance Considerations

### Index Strategy
- **Primary lookups**: Use PRIMARY KEY (id) for direct batch access
- **Product-based queries**: idx_batch_product_id accelerates filtering by product
- **FIFO queries**: Composite index idx_batch_fifo optimizes FIFO recommendations
- **Expiry management**: idx_batch_expiring optimized for daily expiry checks

### Query Optimization Tips
1. Always include `status` filter to leverage indexes
2. Use `expiry_date IS NOT NULL` in WHERE clause for expiry queries
3. Join with inventory only when quantity needed
4. Use pagination for batch lists (typical warehouse has 1000+ batches)

### Estimated Table Size
- **Small warehouse**: 500-1,000 batches
- **Medium warehouse**: 5,000-10,000 batches
- **Large warehouse**: 50,000+ batches
- **Row size**: ~300 bytes
- **Expected growth**: 100-500 batches/month

---

## ✅ Data Integrity Checks

### Validation Queries

```sql
-- Check 1: Find batches with invalid dates
SELECT * FROM batches 
WHERE expiry_date IS NOT NULL 
    AND expiry_date <= manufacturing_date;

-- Check 2: Find batches with future manufacturing date
SELECT * FROM batches 
WHERE manufacturing_date > CURRENT_DATE;

-- Check 3: Find batches for non-batch-tracked products
SELECT b.*, p.sku, p.requires_batch_tracking
FROM batches b
INNER JOIN products p ON b.product_id = p.id
WHERE p.requires_batch_tracking = FALSE;

-- Check 4: Find duplicate batch numbers per product
SELECT product_id, batch_number, COUNT(*) as count
FROM batches
GROUP BY product_id, batch_number
HAVING count > 1;

-- Check 5: Find orphaned batches (no inventory)
SELECT b.id, b.batch_number, b.status
FROM batches b
LEFT JOIN inventory i ON b.id = i.batch_id
WHERE b.status = 'AVAILABLE'
    AND i.id IS NULL;
```

---

## 📚 Additional Notes

### MySQL-Specific Features Used
- **ENUM type**: For status field (efficient storage and validation)
- **CHECK constraints**: Available in MySQL 8.0.16+
- **ON UPDATE CURRENT_TIMESTAMP**: Automatic update tracking
- **CHAR(36)**: Fixed-length for UUID storage

### Migration Best Practices
- Run migrations during low-traffic periods
- Test on staging environment first
- Backup database before migration
- Monitor query performance after index creation

---

**Document Version:** 1.0  
**Last Updated:** February 1, 2026  
**Status:** 🚧 Draft - Pending Review
