# Database Schema - Module 6: Nghiệp vụ Xuất kho
## Thiết kế Database & Hướng dẫn Migration

---

## 📋 Thông tin Tài liệu

| Thuộc tính | Giá trị |
|------------|---------|
| **Module** | Nghiệp vụ Xuất kho |
| **Phiên bản** | 1.0 |
| **Ngày** | 01/02/2026 |
| **Trạng thái** | Bản nháp |

---

## 🗂️ Tổng quan Schema

### Sơ đồ ERD

```
┌──────────────────┐         ┌──────────────────┐
│business_partners │         │    warehouses    │
│  (customers)     │         │                  │
└────────┬─────────┘         └────────┬─────────┘
         │                            │
         │ N:1                        │ N:1
         ▼                            ▼
┌────────────────────────────────────────┐
│          sales_orders                  │
│                                        │
│ - id, so_number (SO-YYYY-NNNN)        │
│ - customer_id FK, warehouse_id FK     │
│ - status (DRAFT/CONFIRMED/...)        │
│ - order_date, requested_delivery_date │
└────────────────┬───────────────────────┘
                 │ 1:N
                 ▼
┌────────────────────────────────────────┐
│       sales_order_lines                │
│                                        │
│ - id, sales_order_id FK, product_id FK│
│ - quantity_ordered, quantity_shipped  │
│ - unit_price, line_total              │
└────────────────┬───────────────────────┘
                 │
                 │ Referenced by
                 ▼
┌────────────────────────────────────────┐
│        outbound_shipments              │
│                                        │
│ - id, shipment_number (SHIP-...)      │
│ - sales_order_id FK, warehouse_id FK  │
│ - status (DRAFT/PICKING/SHIPPED)      │
│ - shipped_at, tracking_number         │
└────────────────┬───────────────────────┘
                 │ 1:N
                 ▼
┌────────────────────────────────────────┐
│     outbound_shipment_lines            │
│                                        │
│ - outbound_shipment_id FK             │
│ - sales_order_line_id FK              │
│ - product_id FK, batch_id FK          │
│ - location_id FK                      │
│ - quantity_shipped                    │
│ - picked_at, picked_by                │
└────────────────────────────────────────┘
```

---

## 📊 Định nghĩa Các Bảng

### Bảng: sales_orders

```sql
CREATE TABLE sales_orders (
    id                          CHAR(36) PRIMARY KEY,
    so_number                   VARCHAR(50) UNIQUE NOT NULL,
    customer_id                 CHAR(36) NOT NULL,
    warehouse_id                CHAR(36) NOT NULL,
    order_date                  DATE NOT NULL,
    requested_delivery_date     DATE,
    status                      ENUM('DRAFT', 'CONFIRMED', 'PARTIALLY_SHIPPED', 
                                     'COMPLETED', 'CANCELLED') NOT NULL DEFAULT 'DRAFT',
    subtotal                    DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    tax_amount                  DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    total_amount                DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    currency                    VARCHAR(3) DEFAULT 'USD',
    notes                       TEXT,
    confirmed_at                TIMESTAMP,
    confirmed_by                CHAR(36),
    created_at                  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by                  CHAR(36) NOT NULL,
    updated_by                  CHAR(36),
    
    CONSTRAINT fk_so_customer FOREIGN KEY (customer_id) 
        REFERENCES business_partners(id) ON DELETE RESTRICT,
    CONSTRAINT fk_so_warehouse FOREIGN KEY (warehouse_id) 
        REFERENCES warehouses(id) ON DELETE RESTRICT,
    CONSTRAINT fk_so_confirmed_by FOREIGN KEY (confirmed_by) 
        REFERENCES accounts(id) ON DELETE SET NULL,
    CONSTRAINT fk_so_created_by FOREIGN KEY (created_by) 
        REFERENCES accounts(id) ON DELETE SET NULL,
    CONSTRAINT fk_so_updated_by FOREIGN KEY (updated_by) 
        REFERENCES accounts(id) ON DELETE SET NULL,
    CONSTRAINT chk_so_totals CHECK (
        subtotal >= 0 AND tax_amount >= 0 AND total_amount >= 0
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### Bảng: sales_order_lines

```sql
CREATE TABLE sales_order_lines (
    id                      CHAR(36) PRIMARY KEY,
    sales_order_id          CHAR(36) NOT NULL,
    product_id              CHAR(36) NOT NULL,
    line_number             INT NOT NULL,
    quantity_ordered        DECIMAL(15, 2) NOT NULL,
    quantity_shipped        DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    unit_price              DECIMAL(15, 2) NOT NULL,
    line_total              DECIMAL(15, 2) NOT NULL,
    notes                   VARCHAR(500),
    created_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    CONSTRAINT uk_so_line_number UNIQUE (sales_order_id, line_number),
    CONSTRAINT fk_so_line_so FOREIGN KEY (sales_order_id) 
        REFERENCES sales_orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_so_line_product FOREIGN KEY (product_id) 
        REFERENCES products(id) ON DELETE RESTRICT,
    CONSTRAINT chk_so_line_quantities CHECK (
        quantity_ordered > 0 
        AND quantity_shipped >= 0 
        AND quantity_shipped <= quantity_ordered
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### Bảng: outbound_shipments

```sql
CREATE TABLE outbound_shipments (
    id                      CHAR(36) PRIMARY KEY,
    shipment_number         VARCHAR(50) UNIQUE NOT NULL,
    sales_order_id          CHAR(36) NOT NULL,
    warehouse_id            CHAR(36) NOT NULL,
    shipment_date           DATE NOT NULL,
    status                  ENUM('DRAFT', 'PICKING', 'PACKED', 'SHIPPED', 'CANCELLED') 
                            NOT NULL DEFAULT 'DRAFT',
    tracking_number         VARCHAR(100),
    carrier                 VARCHAR(100),
    shipped_at              TIMESTAMP,
    confirmed_by            CHAR(36),
    notes                   TEXT,
    created_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by              CHAR(36) NOT NULL,
    updated_by              CHAR(36),
    
    CONSTRAINT fk_shipment_so FOREIGN KEY (sales_order_id) 
        REFERENCES sales_orders(id) ON DELETE RESTRICT,
    CONSTRAINT fk_shipment_warehouse FOREIGN KEY (warehouse_id) 
        REFERENCES warehouses(id) ON DELETE RESTRICT,
    CONSTRAINT fk_shipment_confirmed_by FOREIGN KEY (confirmed_by) 
        REFERENCES accounts(id) ON DELETE SET NULL,
    CONSTRAINT fk_shipment_created_by FOREIGN KEY (created_by) 
        REFERENCES accounts(id) ON DELETE SET NULL,
    CONSTRAINT fk_shipment_updated_by FOREIGN KEY (updated_by) 
        REFERENCES accounts(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### Bảng: outbound_shipment_lines

```sql
CREATE TABLE outbound_shipment_lines (
    id                      CHAR(36) PRIMARY KEY,
    outbound_shipment_id    CHAR(36) NOT NULL,
    sales_order_line_id     CHAR(36) NOT NULL,
    product_id              CHAR(36) NOT NULL,
    batch_id                CHAR(36),
    location_id             CHAR(36) NOT NULL,
    line_number             INT NOT NULL,
    quantity_shipped        DECIMAL(15, 2) NOT NULL,
    picked_at               TIMESTAMP,
    picked_by               CHAR(36),
    notes                   VARCHAR(500),
    created_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    CONSTRAINT uk_shipment_line_number UNIQUE (outbound_shipment_id, line_number),
    CONSTRAINT fk_shipment_line_shipment FOREIGN KEY (outbound_shipment_id) 
        REFERENCES outbound_shipments(id) ON DELETE CASCADE,
    CONSTRAINT fk_shipment_line_so_line FOREIGN KEY (sales_order_line_id) 
        REFERENCES sales_order_lines(id) ON DELETE RESTRICT,
    CONSTRAINT fk_shipment_line_product FOREIGN KEY (product_id) 
        REFERENCES products(id) ON DELETE RESTRICT,
    CONSTRAINT fk_shipment_line_batch FOREIGN KEY (batch_id) 
        REFERENCES batches(id) ON DELETE RESTRICT,
    CONSTRAINT fk_shipment_line_location FOREIGN KEY (location_id) 
        REFERENCES locations(id) ON DELETE RESTRICT,
    CONSTRAINT fk_shipment_line_picked_by FOREIGN KEY (picked_by) 
        REFERENCES accounts(id) ON DELETE SET NULL,
    CONSTRAINT chk_shipment_line_quantity CHECK (quantity_shipped > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

## 📑 Indexes (Chỉ mục)

```sql
-- sales_orders
CREATE INDEX idx_so_customer ON sales_orders(customer_id);
CREATE INDEX idx_so_warehouse ON sales_orders(warehouse_id);
CREATE INDEX idx_so_status ON sales_orders(status);
CREATE INDEX idx_so_order_date ON sales_orders(order_date);

-- outbound_shipments
CREATE INDEX idx_shipment_so ON outbound_shipments(sales_order_id);
CREATE INDEX idx_shipment_warehouse ON outbound_shipments(warehouse_id);
CREATE INDEX idx_shipment_status ON outbound_shipments(status);
CREATE INDEX idx_shipment_date ON outbound_shipments(shipment_date);
```

---

## 🔄 Flyway Migration

### File: `V20260201_04__Create_outbound_operations.sql`

```sql
-- ============================================================================
-- Flyway Migration: Tạo Module Nghiệp vụ Xuất kho
-- ============================================================================

CREATE TABLE sales_orders (
    id                          CHAR(36) PRIMARY KEY,
    so_number                   VARCHAR(50) UNIQUE NOT NULL,
    customer_id                 CHAR(36) NOT NULL,
    warehouse_id                CHAR(36) NOT NULL,
    order_date                  DATE NOT NULL,
    requested_delivery_date     DATE,
    status                      ENUM('DRAFT', 'CONFIRMED', 'PARTIALLY_SHIPPED', 
                                     'COMPLETED', 'CANCELLED') NOT NULL DEFAULT 'DRAFT',
    subtotal                    DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    tax_amount                  DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    total_amount                DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    currency                    VARCHAR(3) DEFAULT 'USD',
    notes                       TEXT,
    confirmed_at                TIMESTAMP,
    confirmed_by                CHAR(36),
    created_at                  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by                  CHAR(36) NOT NULL,
    updated_by                  CHAR(36),
    
    CONSTRAINT fk_so_customer FOREIGN KEY (customer_id) 
        REFERENCES business_partners(id) ON DELETE RESTRICT,
    CONSTRAINT fk_so_warehouse FOREIGN KEY (warehouse_id) 
        REFERENCES warehouses(id) ON DELETE RESTRICT,
    CONSTRAINT fk_so_confirmed_by FOREIGN KEY (confirmed_by) 
        REFERENCES accounts(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE sales_order_lines (
    id                      CHAR(36) PRIMARY KEY,
    sales_order_id          CHAR(36) NOT NULL,
    product_id              CHAR(36) NOT NULL,
    line_number             INT NOT NULL,
    quantity_ordered        DECIMAL(15, 2) NOT NULL,
    quantity_shipped        DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    unit_price              DECIMAL(15, 2) NOT NULL,
    line_total              DECIMAL(15, 2) NOT NULL,
    notes                   VARCHAR(500),
    created_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    CONSTRAINT uk_so_line_number UNIQUE (sales_order_id, line_number),
    CONSTRAINT fk_so_line_so FOREIGN KEY (sales_order_id) 
        REFERENCES sales_orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_so_line_product FOREIGN KEY (product_id) 
        REFERENCES products(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE outbound_shipments (
    id                      CHAR(36) PRIMARY KEY,
    shipment_number         VARCHAR(50) UNIQUE NOT NULL,
    sales_order_id          CHAR(36) NOT NULL,
    warehouse_id            CHAR(36) NOT NULL,
    shipment_date           DATE NOT NULL,
    status                  ENUM('DRAFT', 'PICKING', 'PACKED', 'SHIPPED', 'CANCELLED') 
                            NOT NULL DEFAULT 'DRAFT',
    tracking_number         VARCHAR(100),
    carrier                 VARCHAR(100),
    shipped_at              TIMESTAMP,
    confirmed_by            CHAR(36),
    notes                   TEXT,
    created_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by              CHAR(36) NOT NULL,
    updated_by              CHAR(36),
    
    CONSTRAINT fk_shipment_so FOREIGN KEY (sales_order_id) 
        REFERENCES sales_orders(id) ON DELETE RESTRICT,
    CONSTRAINT fk_shipment_warehouse FOREIGN KEY (warehouse_id) 
        REFERENCES warehouses(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE outbound_shipment_lines (
    id                      CHAR(36) PRIMARY KEY,
    outbound_shipment_id    CHAR(36) NOT NULL,
    sales_order_line_id     CHAR(36) NOT NULL,
    product_id              CHAR(36) NOT NULL,
    batch_id                CHAR(36),
    location_id             CHAR(36) NOT NULL,
    line_number             INT NOT NULL,
    quantity_shipped        DECIMAL(15, 2) NOT NULL,
    picked_at               TIMESTAMP,
    picked_by               CHAR(36),
    notes                   VARCHAR(500),
    created_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    CONSTRAINT uk_shipment_line_number UNIQUE (outbound_shipment_id, line_number),
    CONSTRAINT fk_shipment_line_shipment FOREIGN KEY (outbound_shipment_id) 
        REFERENCES outbound_shipments(id) ON DELETE CASCADE,
    CONSTRAINT fk_shipment_line_so_line FOREIGN KEY (sales_order_line_id) 
        REFERENCES sales_order_lines(id) ON DELETE RESTRICT,
    CONSTRAINT fk_shipment_line_product FOREIGN KEY (product_id) 
        REFERENCES products(id) ON DELETE RESTRICT,
    CONSTRAINT fk_shipment_line_batch FOREIGN KEY (batch_id) 
        REFERENCES batches(id) ON DELETE RESTRICT,
    CONSTRAINT fk_shipment_line_location FOREIGN KEY (location_id) 
        REFERENCES locations(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_so_customer ON sales_orders(customer_id);
CREATE INDEX idx_so_status ON sales_orders(status);
CREATE INDEX idx_shipment_so ON outbound_shipments(sales_order_id);
CREATE INDEX idx_shipment_status ON outbound_shipments(status);

-- ============================================================================
-- End of Migration
-- ============================================================================
```

---

**Phiên bản tài liệu:** 1.0  
**Cập nhật lần cuối:** 01/02/2026
