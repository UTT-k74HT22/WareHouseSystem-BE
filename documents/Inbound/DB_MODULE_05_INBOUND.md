# Database Schema - Module 5: Inbound Operations
## Database Design & Migration Guide

---

## 📋 Document Information

| Property | Value |
|----------|-------|
| **Module** | Inbound Operations |
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
5. [Flyway Migration](#flyway-migration)
6. [Query Examples](#query-examples)

---

## 🗂️ Schema Overview

### ERD Diagram

```
┌──────────────────┐         ┌──────────────────┐
│business_partners │         │    warehouses    │
│ (suppliers)      │         │                  │
└────────┬─────────┘         └────────┬─────────┘
         │                            │
         │ N:1                        │ N:1
         │                            │
┌────────▼────────────────────────────▼─────────┐
│          purchase_orders                      │
│                                               │
│ - id (PK)                                    │
│ - po_number (UK)                             │
│ - supplier_id (FK) ──────────────────────────┤
│ - warehouse_id (FK) ──────────────────────────┤
│ - status (DRAFT/CONFIRMED/...)               │
│ - order_date, expected_delivery_date         │
│ - subtotal, tax_amount, total_amount         │
└───────────────────┬───────────────────────────┘
                    │
                    │ 1:N
                    │
┌───────────────────▼──────────────────────────┐
│      purchase_order_lines                    │
│                                              │
│ - id (PK)                                   │
│ - purchase_order_id (FK) ────────────────────┤
│ - product_id (FK)                           │
│ - quantity_ordered                          │
│ - quantity_received                         │
│ - unit_price, line_total                    │
└──────────────────┬───────────────────────────┘
                   │
                   │ Referenced by
                   │
┌──────────────────▼──────────────────────────┐
│         inbound_receipts                    │
│                                             │
│ - id (PK)                                  │
│ - receipt_number (UK)                      │
│ - purchase_order_id (FK) ───────────────────┤
│ - warehouse_id (FK)                        │
│ - status (DRAFT/CONFIRMED/CANCELLED)       │
│ - receipt_date, confirmed_at               │
└────────────────┬────────────────────────────┘
                 │
                 │ 1:N
                 │
┌────────────────▼───────────────────────────┐
│      inbound_receipt_lines                 │
│                                            │
│ - id (PK)                                 │
│ - inbound_receipt_id (FK) ─────────────────┤
│ - purchase_order_line_id (FK)             │
│ - product_id (FK)                         │
│ - batch_id (FK to batches)                │
│ - location_id (FK to locations)           │
│ - quantity_received                       │
│ - quality_status (PASS/QUARANTINE)        │
└────────────────────────────────────────────┘
```

---

## 📊 Table Definitions

### Table: `purchase_orders`

```sql
CREATE TABLE purchase_orders (
    -- Primary Key
    id                      CHAR(36) PRIMARY KEY,
    po_number               VARCHAR(50) UNIQUE NOT NULL 
                            COMMENT 'Human-readable PO number (PO-YYYY-NNNN)',
    
    -- References
    supplier_id             CHAR(36) NOT NULL 
                            COMMENT 'FK to business_partners (type SUPPLIER)',
    warehouse_id            CHAR(36) NOT NULL 
                            COMMENT 'Destination warehouse',
    
    -- Dates
    order_date              DATE NOT NULL 
                            COMMENT 'Date PO was created',
    expected_delivery_date  DATE 
                            COMMENT 'Expected delivery date',
    
    -- Status
    status                  ENUM('DRAFT', 'CONFIRMED', 'PARTIALLY_RECEIVED', 
                                 'COMPLETED', 'CANCELLED') 
                            NOT NULL DEFAULT 'DRAFT',
    
    -- Financial
    subtotal                DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    tax_amount              DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    total_amount            DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    currency                VARCHAR(3) DEFAULT 'USD',
    
    -- Additional Info
    payment_terms           VARCHAR(200),
    notes                   TEXT,
    
    -- Workflow
    confirmed_at            TIMESTAMP 
                            COMMENT 'When PO was confirmed',
    confirmed_by            CHAR(36) 
                            COMMENT 'User who confirmed PO',
    
    -- Audit
    created_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by              CHAR(36) NOT NULL,
    updated_by              CHAR(36),
    
    -- Constraints
    CONSTRAINT fk_po_supplier FOREIGN KEY (supplier_id) 
        REFERENCES business_partners(id) ON DELETE RESTRICT,
    CONSTRAINT fk_po_warehouse FOREIGN KEY (warehouse_id) 
        REFERENCES warehouses(id) ON DELETE RESTRICT,
    CONSTRAINT fk_po_confirmed_by FOREIGN KEY (confirmed_by) 
        REFERENCES accounts(id) ON DELETE SET NULL,
    CONSTRAINT fk_po_created_by FOREIGN KEY (created_by) 
        REFERENCES accounts(id) ON DELETE SET NULL,
    CONSTRAINT fk_po_updated_by FOREIGN KEY (updated_by) 
        REFERENCES accounts(id) ON DELETE SET NULL,
    
    -- Check Constraints
    CONSTRAINT chk_po_totals CHECK (
        subtotal >= 0 
        AND tax_amount >= 0 
        AND total_amount >= 0
    ),
    CONSTRAINT chk_po_delivery_date CHECK (
        expected_delivery_date IS NULL 
        OR expected_delivery_date >= order_date
    )
);
```

---

### Table: `purchase_order_lines`

```sql
CREATE TABLE purchase_order_lines (
    -- Primary Key
    id                      CHAR(36) PRIMARY KEY,
    
    -- References
    purchase_order_id       CHAR(36) NOT NULL 
                            COMMENT 'FK to purchase_orders',
    product_id              CHAR(36) NOT NULL 
                            COMMENT 'FK to products',
    
    -- Line Number
    line_number             INT NOT NULL 
                            COMMENT 'Line sequence number (1, 2, 3...)',
    
    -- Quantities
    quantity_ordered        DECIMAL(15, 2) NOT NULL 
                            COMMENT 'Quantity ordered from supplier',
    quantity_received       DECIMAL(15, 2) NOT NULL DEFAULT 0.00 
                            COMMENT 'Running total of quantity received',
    
    -- Pricing
    unit_price              DECIMAL(15, 2) NOT NULL 
                            COMMENT 'Price per unit',
    line_total              DECIMAL(15, 2) NOT NULL 
                            COMMENT 'quantity_ordered * unit_price',
    
    -- Additional Info
    notes                   VARCHAR(500),
    
    -- Audit
    created_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT uk_po_line_number UNIQUE (purchase_order_id, line_number),
    CONSTRAINT fk_po_line_po FOREIGN KEY (purchase_order_id) 
        REFERENCES purchase_orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_po_line_product FOREIGN KEY (product_id) 
        REFERENCES products(id) ON DELETE RESTRICT,
    
    -- Check Constraints
    CONSTRAINT chk_po_line_quantities CHECK (
        quantity_ordered > 0 
        AND quantity_received >= 0 
        AND quantity_received <= quantity_ordered
    ),
    CONSTRAINT chk_po_line_price CHECK (
        unit_price >= 0 
        AND line_total >= 0
    ),
    CONSTRAINT chk_po_line_total CHECK (
        line_total = quantity_ordered * unit_price
    )
);
```

---

### Table: `inbound_receipts`

```sql
CREATE TABLE inbound_receipts (
    -- Primary Key
    id                      CHAR(36) PRIMARY KEY,
    receipt_number          VARCHAR(50) UNIQUE NOT NULL 
                            COMMENT 'Human-readable receipt number (GR-YYYY-NNNN)',
    
    -- References
    purchase_order_id       CHAR(36) NOT NULL 
                            COMMENT 'FK to purchase_orders',
    warehouse_id            CHAR(36) NOT NULL 
                            COMMENT 'Destination warehouse',
    
    -- Dates
    receipt_date            DATE NOT NULL 
                            COMMENT 'Date goods physically received',
    
    -- Status
    status                  ENUM('DRAFT', 'CONFIRMED', 'CANCELLED') 
                            NOT NULL DEFAULT 'DRAFT',
    
    -- Workflow
    confirmed_at            TIMESTAMP 
                            COMMENT 'When receipt was confirmed (inventory updated)',
    confirmed_by            CHAR(36) 
                            COMMENT 'User who confirmed receipt',
    
    -- Additional Info
    delivery_note_number    VARCHAR(100) 
                            COMMENT 'Supplier delivery note reference',
    notes                   TEXT,
    
    -- Audit
    created_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by              CHAR(36) NOT NULL,
    updated_by              CHAR(36),
    
    -- Constraints
    CONSTRAINT fk_receipt_po FOREIGN KEY (purchase_order_id) 
        REFERENCES purchase_orders(id) ON DELETE RESTRICT,
    CONSTRAINT fk_receipt_warehouse FOREIGN KEY (warehouse_id) 
        REFERENCES warehouses(id) ON DELETE RESTRICT,
    CONSTRAINT fk_receipt_confirmed_by FOREIGN KEY (confirmed_by) 
        REFERENCES accounts(id) ON DELETE SET NULL,
    CONSTRAINT fk_receipt_created_by FOREIGN KEY (created_by) 
        REFERENCES accounts(id) ON DELETE SET NULL,
    CONSTRAINT fk_receipt_updated_by FOREIGN KEY (updated_by) 
        REFERENCES accounts(id) ON DELETE SET NULL
);
```

---

### Table: `inbound_receipt_lines`

```sql
CREATE TABLE inbound_receipt_lines (
    -- Primary Key
    id                      CHAR(36) PRIMARY KEY,
    
    -- References
    inbound_receipt_id      CHAR(36) NOT NULL 
                            COMMENT 'FK to inbound_receipts',
    purchase_order_line_id  CHAR(36) NOT NULL 
                            COMMENT 'FK to purchase_order_lines',
    product_id              CHAR(36) NOT NULL 
                            COMMENT 'Denormalized for easier querying',
    batch_id                CHAR(36) 
                            COMMENT 'FK to batches (created during confirmation)',
    location_id             CHAR(36) NOT NULL 
                            COMMENT 'FK to locations (where stock placed)',
    
    -- Line Number
    line_number             INT NOT NULL 
                            COMMENT 'Line sequence number',
    
    -- Quantity
    quantity_received       DECIMAL(15, 2) NOT NULL 
                            COMMENT 'Quantity received in this receipt',
    
    -- Quality
    quality_status          ENUM('PASS', 'QUARANTINE') 
                            NOT NULL DEFAULT 'PASS',
    
    -- Additional Info
    notes                   VARCHAR(500) 
                            COMMENT 'Observations (damage, defects, etc.)',
    
    -- Audit
    created_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT uk_receipt_line_number UNIQUE (inbound_receipt_id, line_number),
    CONSTRAINT fk_receipt_line_receipt FOREIGN KEY (inbound_receipt_id) 
        REFERENCES inbound_receipts(id) ON DELETE CASCADE,
    CONSTRAINT fk_receipt_line_po_line FOREIGN KEY (purchase_order_line_id) 
        REFERENCES purchase_order_lines(id) ON DELETE RESTRICT,
    CONSTRAINT fk_receipt_line_product FOREIGN KEY (product_id) 
        REFERENCES products(id) ON DELETE RESTRICT,
    CONSTRAINT fk_receipt_line_batch FOREIGN KEY (batch_id) 
        REFERENCES batches(id) ON DELETE RESTRICT,
    CONSTRAINT fk_receipt_line_location FOREIGN KEY (location_id) 
        REFERENCES locations(id) ON DELETE RESTRICT,
    
    -- Check Constraints
    CONSTRAINT chk_receipt_line_quantity CHECK (
        quantity_received > 0
    )
);
```

---

## 📑 Indexes

```sql
-- purchase_orders
CREATE INDEX idx_po_supplier ON purchase_orders(supplier_id);
CREATE INDEX idx_po_warehouse ON purchase_orders(warehouse_id);
CREATE INDEX idx_po_status ON purchase_orders(status);
CREATE INDEX idx_po_order_date ON purchase_orders(order_date);
CREATE INDEX idx_po_expected_delivery ON purchase_orders(expected_delivery_date);

-- purchase_order_lines
CREATE INDEX idx_po_line_po ON purchase_order_lines(purchase_order_id);
CREATE INDEX idx_po_line_product ON purchase_order_lines(product_id);

-- inbound_receipts
CREATE INDEX idx_receipt_po ON inbound_receipts(purchase_order_id);
CREATE INDEX idx_receipt_warehouse ON inbound_receipts(warehouse_id);
CREATE INDEX idx_receipt_status ON inbound_receipts(status);
CREATE INDEX idx_receipt_date ON inbound_receipts(receipt_date);

-- inbound_receipt_lines
CREATE INDEX idx_receipt_line_receipt ON inbound_receipt_lines(inbound_receipt_id);
CREATE INDEX idx_receipt_line_po_line ON inbound_receipt_lines(purchase_order_line_id);
CREATE INDEX idx_receipt_line_product ON inbound_receipt_lines(product_id);
CREATE INDEX idx_receipt_line_batch ON inbound_receipt_lines(batch_id);
CREATE INDEX idx_receipt_line_location ON inbound_receipt_lines(location_id);
```

---

## 🔄 Flyway Migration

### Migration File: `V20260201_03__Create_inbound_operations.sql`

```sql
-- ============================================================================
-- Flyway Migration: Create Inbound Operations Module
-- Version: V20260201_03
-- Description: Create purchase_orders and inbound_receipts tables
-- Author: Database Team
-- Date: 2026-02-01
-- ============================================================================

-- Table: purchase_orders
CREATE TABLE purchase_orders (
    id                      CHAR(36) PRIMARY KEY,
    po_number               VARCHAR(50) UNIQUE NOT NULL,
    supplier_id             CHAR(36) NOT NULL,
    warehouse_id            CHAR(36) NOT NULL,
    order_date              DATE NOT NULL,
    expected_delivery_date  DATE,
    status                  ENUM('DRAFT', 'CONFIRMED', 'PARTIALLY_RECEIVED', 
                                 'COMPLETED', 'CANCELLED') NOT NULL DEFAULT 'DRAFT',
    subtotal                DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    tax_amount              DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    total_amount            DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    currency                VARCHAR(3) DEFAULT 'USD',
    payment_terms           VARCHAR(200),
    notes                   TEXT,
    confirmed_at            TIMESTAMP,
    confirmed_by            CHAR(36),
    created_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by              CHAR(36) NOT NULL,
    updated_by              CHAR(36),
    
    CONSTRAINT fk_po_supplier FOREIGN KEY (supplier_id) 
        REFERENCES business_partners(id) ON DELETE RESTRICT,
    CONSTRAINT fk_po_warehouse FOREIGN KEY (warehouse_id) 
        REFERENCES warehouses(id) ON DELETE RESTRICT,
    CONSTRAINT fk_po_confirmed_by FOREIGN KEY (confirmed_by) 
        REFERENCES accounts(id) ON DELETE SET NULL,
    CONSTRAINT fk_po_created_by FOREIGN KEY (created_by) 
        REFERENCES accounts(id) ON DELETE SET NULL,
    CONSTRAINT fk_po_updated_by FOREIGN KEY (updated_by) 
        REFERENCES accounts(id) ON DELETE SET NULL,
    CONSTRAINT chk_po_totals CHECK (
        subtotal >= 0 AND tax_amount >= 0 AND total_amount >= 0
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_po_supplier ON purchase_orders(supplier_id);
CREATE INDEX idx_po_warehouse ON purchase_orders(warehouse_id);
CREATE INDEX idx_po_status ON purchase_orders(status);

-- Table: purchase_order_lines
CREATE TABLE purchase_order_lines (
    id                      CHAR(36) PRIMARY KEY,
    purchase_order_id       CHAR(36) NOT NULL,
    product_id              CHAR(36) NOT NULL,
    line_number             INT NOT NULL,
    quantity_ordered        DECIMAL(15, 2) NOT NULL,
    quantity_received       DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    unit_price              DECIMAL(15, 2) NOT NULL,
    line_total              DECIMAL(15, 2) NOT NULL,
    notes                   VARCHAR(500),
    created_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    CONSTRAINT uk_po_line_number UNIQUE (purchase_order_id, line_number),
    CONSTRAINT fk_po_line_po FOREIGN KEY (purchase_order_id) 
        REFERENCES purchase_orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_po_line_product FOREIGN KEY (product_id) 
        REFERENCES products(id) ON DELETE RESTRICT,
    CONSTRAINT chk_po_line_quantities CHECK (
        quantity_ordered > 0 
        AND quantity_received >= 0 
        AND quantity_received <= quantity_ordered
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_po_line_po ON purchase_order_lines(purchase_order_id);
CREATE INDEX idx_po_line_product ON purchase_order_lines(product_id);

-- Table: inbound_receipts
CREATE TABLE inbound_receipts (
    id                      CHAR(36) PRIMARY KEY,
    receipt_number          VARCHAR(50) UNIQUE NOT NULL,
    purchase_order_id       CHAR(36) NOT NULL,
    warehouse_id            CHAR(36) NOT NULL,
    receipt_date            DATE NOT NULL,
    status                  ENUM('DRAFT', 'CONFIRMED', 'CANCELLED') NOT NULL DEFAULT 'DRAFT',
    confirmed_at            TIMESTAMP,
    confirmed_by            CHAR(36),
    delivery_note_number    VARCHAR(100),
    notes                   TEXT,
    created_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by              CHAR(36) NOT NULL,
    updated_by              CHAR(36),
    
    CONSTRAINT fk_receipt_po FOREIGN KEY (purchase_order_id) 
        REFERENCES purchase_orders(id) ON DELETE RESTRICT,
    CONSTRAINT fk_receipt_warehouse FOREIGN KEY (warehouse_id) 
        REFERENCES warehouses(id) ON DELETE RESTRICT,
    CONSTRAINT fk_receipt_confirmed_by FOREIGN KEY (confirmed_by) 
        REFERENCES accounts(id) ON DELETE SET NULL,
    CONSTRAINT fk_receipt_created_by FOREIGN KEY (created_by) 
        REFERENCES accounts(id) ON DELETE SET NULL,
    CONSTRAINT fk_receipt_updated_by FOREIGN KEY (updated_by) 
        REFERENCES accounts(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_receipt_po ON inbound_receipts(purchase_order_id);
CREATE INDEX idx_receipt_warehouse ON inbound_receipts(warehouse_id);
CREATE INDEX idx_receipt_status ON inbound_receipts(status);

-- Table: inbound_receipt_lines
CREATE TABLE inbound_receipt_lines (
    id                      CHAR(36) PRIMARY KEY,
    inbound_receipt_id      CHAR(36) NOT NULL,
    purchase_order_line_id  CHAR(36) NOT NULL,
    product_id              CHAR(36) NOT NULL,
    batch_id                CHAR(36),
    location_id             CHAR(36) NOT NULL,
    line_number             INT NOT NULL,
    quantity_received       DECIMAL(15, 2) NOT NULL,
    quality_status          ENUM('PASS', 'QUARANTINE') NOT NULL DEFAULT 'PASS',
    notes                   VARCHAR(500),
    created_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    CONSTRAINT uk_receipt_line_number UNIQUE (inbound_receipt_id, line_number),
    CONSTRAINT fk_receipt_line_receipt FOREIGN KEY (inbound_receipt_id) 
        REFERENCES inbound_receipts(id) ON DELETE CASCADE,
    CONSTRAINT fk_receipt_line_po_line FOREIGN KEY (purchase_order_line_id) 
        REFERENCES purchase_order_lines(id) ON DELETE RESTRICT,
    CONSTRAINT fk_receipt_line_product FOREIGN KEY (product_id) 
        REFERENCES products(id) ON DELETE RESTRICT,
    CONSTRAINT fk_receipt_line_batch FOREIGN KEY (batch_id) 
        REFERENCES batches(id) ON DELETE RESTRICT,
    CONSTRAINT fk_receipt_line_location FOREIGN KEY (location_id) 
        REFERENCES locations(id) ON DELETE RESTRICT,
    CONSTRAINT chk_receipt_line_quantity CHECK (quantity_received > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_receipt_line_receipt ON inbound_receipt_lines(inbound_receipt_id);
CREATE INDEX idx_receipt_line_po_line ON inbound_receipt_lines(purchase_order_line_id);
CREATE INDEX idx_receipt_line_product ON inbound_receipt_lines(product_id);

-- ============================================================================
-- End of Migration
-- ============================================================================
```

---

## 🔍 Query Examples

### 1. Find POs Pending Receipt

```sql
SELECT 
    po.po_number,
    po.order_date,
    po.expected_delivery_date,
    bp.name AS supplier_name,
    po.status,
    COUNT(pol.id) AS total_lines,
    SUM(pol.quantity_ordered) AS total_ordered,
    SUM(pol.quantity_received) AS total_received
FROM purchase_orders po
INNER JOIN business_partners bp ON po.supplier_id = bp.id
LEFT JOIN purchase_order_lines pol ON po.id = pol.purchase_order_id
WHERE po.status IN ('CONFIRMED', 'PARTIALLY_RECEIVED')
GROUP BY po.id, po.po_number, po.order_date, po.expected_delivery_date, bp.name, po.status
ORDER BY po.expected_delivery_date ASC;
```

### 2. PO Receiving Progress

```sql
SELECT 
    pol.line_number,
    p.sku,
    p.name AS product_name,
    pol.quantity_ordered,
    pol.quantity_received,
    pol.quantity_ordered - pol.quantity_received AS remaining,
    ROUND((pol.quantity_received / pol.quantity_ordered) * 100, 2) AS percent_received
FROM purchase_order_lines pol
INNER JOIN products p ON pol.product_id = p.id
WHERE pol.purchase_order_id = ?
ORDER BY pol.line_number;
```

---

**Document Version:** 1.0  
**Last Updated:** February 1, 2026  
**Status:** 🚧 Draft - Pending Review
