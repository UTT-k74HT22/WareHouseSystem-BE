# Database Schema - Warehouse Management System
## Complete Database Design & SQL Scripts

---

## 📋 Mục Lục
1. [Database Overview](#database-overview)
2. [ERD Diagram](#erd-diagram)
3. [Table Definitions](#table-definitions)
4. [Indexes & Constraints](#indexes--constraints)
5. [Sample Data](#sample-data)

---

## 🗄️ Database Overview

### Database Configuration
```yaml
Database: MySQL 8.0
Character Set: utf8mb4
Collation: utf8mb4_unicode_ci
Engine: InnoDB
Time Zone: UTC
```

### Design Principles
1. ✅ **Normalized to 3NF** - Minimize data redundancy
2. ✅ **Audit Trail** - Track who/when for all changes
3. ✅ **Soft Delete** - Use status fields instead of hard deletes
4. ✅ **Referential Integrity** - Foreign key constraints
5. ✅ **Optimistic Locking** - Version fields for concurrency control
6. ✅ **Indexing Strategy** - Optimize for common queries

---

## 📊 ERD Diagram

### High-Level Entity Relationship
```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│  ACCOUNTS   │────▶│ACCOUNT_ROLES│◀────│   ROLES     │
└─────────────┘     └─────────────┘     └─────────────┘
                                               │
                                               ▼
                                        ┌─────────────┐
                                        │ROLE_PERMS   │
                                        └─────────────┘
                                               │
                                               ▼
                                        ┌─────────────┐
                                        │ PERMISSIONS │
                                        └─────────────┘

┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│ WAREHOUSES  │────▶│  LOCATIONS  │     │  PRODUCTS   │
└─────────────┘     └─────────────┘     └─────────────┘
       │                   │                    │
       │                   │                    ▼
       │                   │             ┌─────────────┐
       │                   │             │   BATCHES   │
       │                   │             └─────────────┘
       │                   │                    │
       └───────────────────┼────────────────────┘
                           ▼
                    ┌─────────────┐
                    │  INVENTORY  │
                    └─────────────┘
                           │
                           ▼
                    ┌─────────────┐
                    │   STOCK     │
                    │  MOVEMENTS  │
                    └─────────────┘

┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│  BUSINESS   │────▶│  PURCHASE   │────▶│  INBOUND    │
│  PARTNERS   │     │   ORDERS    │     │  RECEIPTS   │
└─────────────┘     └─────────────┘     └─────────────┘
       │
       └───────────▶┌─────────────┐     ┌─────────────┐
                    │   SALES     │────▶│  OUTBOUND   │
                    │   ORDERS    │     │  SHIPMENTS  │
                    └─────────────┘     └─────────────┘
```

---

## 📋 Table Definitions

### 1. Auth & User Management

#### accounts
```sql
CREATE TABLE accounts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    status ENUM('ACTIVE', 'INACTIVE', 'LOCKED') DEFAULT 'ACTIVE',
    failed_login_attempts INT DEFAULT 0,
    locked_until TIMESTAMP NULL,
    last_login_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_username (username),
    INDEX idx_email (email),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

#### roles
```sql
CREATE TABLE roles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) UNIQUE NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

#### permissions
```sql
CREATE TABLE permissions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) UNIQUE NOT NULL,
    resource VARCHAR(50) NOT NULL,
    action VARCHAR(50) NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE KEY uk_resource_action (resource, action),
    INDEX idx_resource (resource)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

#### account_roles (Junction Table)
```sql
CREATE TABLE account_roles (
    account_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    assigned_by BIGINT,
    
    PRIMARY KEY (account_id, role_id),
    FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    FOREIGN KEY (assigned_by) REFERENCES accounts(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

#### role_permissions (Junction Table)
```sql
CREATE TABLE role_permissions (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    granted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    PRIMARY KEY (role_id, permission_id),
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

#### user_profiles
```sql
CREATE TABLE user_profiles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    account_id BIGINT UNIQUE NOT NULL,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    phone VARCHAR(20),
    avatar_url VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

### 2. Master Data

#### units_of_measure
```sql
CREATE TABLE units_of_measure (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(10) UNIQUE NOT NULL,
    name VARCHAR(50) NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

#### product_categories
```sql
CREATE TABLE product_categories (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(20) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    status ENUM('ACTIVE', 'INACTIVE') DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,

    INDEX idx_code (code),
    INDEX idx_status (status),
    INDEX idx_name (name),
    FOREIGN KEY (created_by) REFERENCES accounts(id) ON DELETE SET NULL,
    FOREIGN KEY (updated_by) REFERENCES accounts(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

#### warehouses
```sql
CREATE TABLE warehouses (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(20) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    address VARCHAR(255),
    city VARCHAR(50),
    state VARCHAR(50),
    country VARCHAR(50),
    postal_code VARCHAR(20),
    phone VARCHAR(20),
    email VARCHAR(100),
    type ENUM('MAIN', 'SATELLITE', 'TRANSIT', 'RETURN') DEFAULT 'MAIN',
    status ENUM('ACTIVE', 'INACTIVE', 'MAINTENANCE') DEFAULT 'ACTIVE',
    capacity DECIMAL(15,2) COMMENT 'Total capacity in cubic meters',
    manager_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    
    INDEX idx_code (code),
    INDEX idx_status (status),
    INDEX idx_type (type),
    FOREIGN KEY (manager_id) REFERENCES accounts(id) ON DELETE SET NULL,
    FOREIGN KEY (created_by) REFERENCES accounts(id) ON DELETE SET NULL,
    FOREIGN KEY (updated_by) REFERENCES accounts(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

#### locations
```sql
CREATE TABLE locations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    warehouse_id BIGINT NOT NULL,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    zone VARCHAR(50),
    type ENUM('STORAGE', 'PICKING', 'PACKING', 'STAGING', 'RETURN') DEFAULT 'STORAGE',
    capacity DECIMAL(15,2) COMMENT 'Capacity in cubic meters',
    status ENUM('ACTIVE', 'INACTIVE', 'FULL', 'MAINTENANCE') DEFAULT 'ACTIVE',
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    
    UNIQUE KEY uk_warehouse_code (warehouse_id, code),
    INDEX idx_warehouse_id (warehouse_id),
    INDEX idx_zone (zone),
    INDEX idx_status (status),
    FOREIGN KEY (warehouse_id) REFERENCES warehouses(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES accounts(id) ON DELETE SET NULL,
    FOREIGN KEY (updated_by) REFERENCES accounts(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

#### products
```sql
CREATE TABLE products (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    sku VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    category_id BIGINT,
    uom_id BIGINT NOT NULL,
    weight DECIMAL(10,3) COMMENT 'Weight in KG',
    dimensions VARCHAR(50) COMMENT 'LxWxH in CM',
    status ENUM('ACTIVE', 'INACTIVE', 'DISCONTINUED') DEFAULT 'ACTIVE',
    min_stock_level DECIMAL(15,2) DEFAULT 0,
    max_stock_level DECIMAL(15,2),
    reorder_point DECIMAL(15,2),
    cost_price DECIMAL(15,2),
    selling_price DECIMAL(15,2),
    barcode VARCHAR(100),
    image_url VARCHAR(255),
    requires_batch_tracking BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,

    INDEX idx_sku (sku),
    INDEX idx_status (status),
    INDEX idx_category_id (category_id),
    INDEX idx_name (name),
    FULLTEXT idx_search (name, description),
    FOREIGN KEY (category_id) REFERENCES product_categories(id),
    FOREIGN KEY (uom_id) REFERENCES units_of_measure(id),
    FOREIGN KEY (created_by) REFERENCES accounts(id) ON DELETE SET NULL,
    FOREIGN KEY (updated_by) REFERENCES accounts(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

#### business_partners
```sql
CREATE TABLE business_partners (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(20) UNIQUE NOT NULL,
    name VARCHAR(200) NOT NULL,
    type ENUM('SUPPLIER', 'CUSTOMER', 'BOTH') NOT NULL,
    contact_person VARCHAR(100),
    email VARCHAR(100),
    phone VARCHAR(20),
    address VARCHAR(255),
    city VARCHAR(50),
    country VARCHAR(50),
    tax_id VARCHAR(50),
    payment_terms VARCHAR(100),
    credit_limit DECIMAL(15,2),
    status ENUM('ACTIVE', 'INACTIVE', 'BLACKLISTED') DEFAULT 'ACTIVE',
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    
    INDEX idx_code (code),
    INDEX idx_type (type),
    INDEX idx_status (status),
    INDEX idx_name (name),
    FOREIGN KEY (created_by) REFERENCES accounts(id) ON DELETE SET NULL,
    FOREIGN KEY (updated_by) REFERENCES accounts(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

### 3. Batch Management

#### batches
```sql
CREATE TABLE batches (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    batch_number VARCHAR(50) NOT NULL,
    product_id BIGINT NOT NULL,
    manufacture_date DATE,
    expiry_date DATE,
    status ENUM('ACTIVE', 'EXPIRED', 'RECALLED', 'QUARANTINE') DEFAULT 'ACTIVE',
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT,
    
    UNIQUE KEY uk_batch_product (batch_number, product_id),
    INDEX idx_batch_number (batch_number),
    INDEX idx_product_id (product_id),
    INDEX idx_expiry_date (expiry_date),
    INDEX idx_status (status),
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES accounts(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

### 4. Inventory

#### inventory
```sql
CREATE TABLE inventory (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    warehouse_id BIGINT NOT NULL,
    location_id BIGINT NOT NULL,
    batch_id BIGINT,
    on_hand_quantity DECIMAL(15,2) NOT NULL DEFAULT 0 CHECK (on_hand_quantity >= 0),
    reserved_quantity DECIMAL(15,2) NOT NULL DEFAULT 0 CHECK (reserved_quantity >= 0),
    available_quantity DECIMAL(15,2) GENERATED ALWAYS AS (on_hand_quantity - reserved_quantity) STORED,
    last_count_date DATE,
    last_movement_date TIMESTAMP,
    version BIGINT DEFAULT 0 COMMENT 'Optimistic locking',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    UNIQUE KEY uk_inventory (product_id, warehouse_id, location_id, batch_id),
    INDEX idx_product_id (product_id),
    INDEX idx_warehouse_id (warehouse_id),
    INDEX idx_location_id (location_id),
    INDEX idx_batch_id (batch_id),
    INDEX idx_available_quantity (available_quantity),
    INDEX idx_composite (product_id, warehouse_id, location_id),
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    FOREIGN KEY (warehouse_id) REFERENCES warehouses(id) ON DELETE CASCADE,
    FOREIGN KEY (location_id) REFERENCES locations(id) ON DELETE CASCADE,
    FOREIGN KEY (batch_id) REFERENCES batches(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

#### inventory_adjustments
```sql
CREATE TABLE inventory_adjustments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    adjustment_number VARCHAR(50) UNIQUE NOT NULL,
    product_id BIGINT NOT NULL,
    warehouse_id BIGINT NOT NULL,
    location_id BIGINT NOT NULL,
    batch_id BIGINT,
    adjustment_type ENUM('MANUAL', 'DAMAGE', 'LOSS', 'FOUND', 'CYCLE_COUNT', 'RETURN') NOT NULL,
    quantity_before DECIMAL(15,2) NOT NULL,
    quantity_change DECIMAL(15,2) NOT NULL,
    quantity_after DECIMAL(15,2) NOT NULL,
    reason VARCHAR(255) NOT NULL,
    notes TEXT,
    status ENUM('PENDING_APPROVAL', 'APPROVED', 'REJECTED', 'COMPLETED') DEFAULT 'PENDING_APPROVAL',
    approved_by BIGINT,
    approved_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT NOT NULL,
    
    INDEX idx_adjustment_number (adjustment_number),
    INDEX idx_product_id (product_id),
    INDEX idx_warehouse_id (warehouse_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at),
    FOREIGN KEY (product_id) REFERENCES products(id),
    FOREIGN KEY (warehouse_id) REFERENCES warehouses(id),
    FOREIGN KEY (location_id) REFERENCES locations(id),
    FOREIGN KEY (batch_id) REFERENCES batches(id),
    FOREIGN KEY (created_by) REFERENCES accounts(id),
    FOREIGN KEY (approved_by) REFERENCES accounts(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

### 5. Inbound Operations

#### purchase_orders
```sql
CREATE TABLE purchase_orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_number VARCHAR(50) UNIQUE NOT NULL,
    supplier_id BIGINT NOT NULL,
    warehouse_id BIGINT NOT NULL,
    order_date DATE NOT NULL,
    expected_delivery_date DATE,
    status ENUM('DRAFT', 'CONFIRMED', 'PARTIAL_RECEIVED', 'COMPLETED', 'CANCELLED') DEFAULT 'DRAFT',
    total_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    currency VARCHAR(3) DEFAULT 'VND',
    notes TEXT,
    confirmed_at TIMESTAMP NULL,
    confirmed_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT NOT NULL,
    updated_by BIGINT,
    
    INDEX idx_order_number (order_number),
    INDEX idx_supplier_id (supplier_id),
    INDEX idx_warehouse_id (warehouse_id),
    INDEX idx_status (status),
    INDEX idx_order_date (order_date),
    FOREIGN KEY (supplier_id) REFERENCES business_partners(id),
    FOREIGN KEY (warehouse_id) REFERENCES warehouses(id),
    FOREIGN KEY (created_by) REFERENCES accounts(id),
    FOREIGN KEY (confirmed_by) REFERENCES accounts(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

#### purchase_order_lines
```sql
CREATE TABLE purchase_order_lines (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    purchase_order_id BIGINT NOT NULL,
    line_number INT NOT NULL,
    product_id BIGINT NOT NULL,
    ordered_quantity DECIMAL(15,2) NOT NULL,
    received_quantity DECIMAL(15,2) DEFAULT 0,
    unit_price DECIMAL(15,2) NOT NULL,
    total_price DECIMAL(15,2) GENERATED ALWAYS AS (ordered_quantity * unit_price) STORED,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    UNIQUE KEY uk_po_line (purchase_order_id, line_number),
    INDEX idx_purchase_order_id (purchase_order_id),
    INDEX idx_product_id (product_id),
    FOREIGN KEY (purchase_order_id) REFERENCES purchase_orders(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

#### inbound_receipts
```sql
CREATE TABLE inbound_receipts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    receipt_number VARCHAR(50) UNIQUE NOT NULL,
    purchase_order_id BIGINT NOT NULL,
    warehouse_id BIGINT NOT NULL,
    receipt_date DATE NOT NULL,
    status ENUM('DRAFT', 'CONFIRMED', 'COMPLETED', 'CANCELLED') DEFAULT 'DRAFT',
    notes TEXT,
    confirmed_at TIMESTAMP NULL,
    confirmed_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT NOT NULL,
    
    INDEX idx_receipt_number (receipt_number),
    INDEX idx_purchase_order_id (purchase_order_id),
    INDEX idx_warehouse_id (warehouse_id),
    INDEX idx_status (status),
    INDEX idx_receipt_date (receipt_date),
    FOREIGN KEY (purchase_order_id) REFERENCES purchase_orders(id),
    FOREIGN KEY (warehouse_id) REFERENCES warehouses(id),
    FOREIGN KEY (created_by) REFERENCES accounts(id),
    FOREIGN KEY (confirmed_by) REFERENCES accounts(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

#### inbound_receipt_lines
```sql
CREATE TABLE inbound_receipt_lines (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    inbound_receipt_id BIGINT NOT NULL,
    purchase_order_line_id BIGINT NOT NULL,
    line_number INT NOT NULL,
    product_id BIGINT NOT NULL,
    location_id BIGINT NOT NULL,
    batch_id BIGINT,
    received_quantity DECIMAL(15,2) NOT NULL,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE KEY uk_receipt_line (inbound_receipt_id, line_number),
    INDEX idx_inbound_receipt_id (inbound_receipt_id),
    INDEX idx_purchase_order_line_id (purchase_order_line_id),
    INDEX idx_product_id (product_id),
    INDEX idx_batch_id (batch_id),
    FOREIGN KEY (inbound_receipt_id) REFERENCES inbound_receipts(id) ON DELETE CASCADE,
    FOREIGN KEY (purchase_order_line_id) REFERENCES purchase_order_lines(id),
    FOREIGN KEY (product_id) REFERENCES products(id),
    FOREIGN KEY (location_id) REFERENCES locations(id),
    FOREIGN KEY (batch_id) REFERENCES batches(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

### 6. Outbound Operations

#### sales_orders
```sql
CREATE TABLE sales_orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_number VARCHAR(50) UNIQUE NOT NULL,
    customer_id BIGINT NOT NULL,
    warehouse_id BIGINT NOT NULL,
    order_date DATE NOT NULL,
    delivery_date DATE,
    status ENUM('DRAFT', 'CONFIRMED', 'PARTIAL_SHIPPED', 'COMPLETED', 'CANCELLED') DEFAULT 'DRAFT',
    total_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    currency VARCHAR(3) DEFAULT 'VND',
    shipping_address TEXT,
    notes TEXT,
    confirmed_at TIMESTAMP NULL,
    confirmed_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT NOT NULL,
    updated_by BIGINT,
    
    INDEX idx_order_number (order_number),
    INDEX idx_customer_id (customer_id),
    INDEX idx_warehouse_id (warehouse_id),
    INDEX idx_status (status),
    INDEX idx_order_date (order_date),
    FOREIGN KEY (customer_id) REFERENCES business_partners(id),
    FOREIGN KEY (warehouse_id) REFERENCES warehouses(id),
    FOREIGN KEY (created_by) REFERENCES accounts(id),
    FOREIGN KEY (confirmed_by) REFERENCES accounts(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

#### sales_order_lines
```sql
CREATE TABLE sales_order_lines (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    sales_order_id BIGINT NOT NULL,
    line_number INT NOT NULL,
    product_id BIGINT NOT NULL,
    ordered_quantity DECIMAL(15,2) NOT NULL,
    shipped_quantity DECIMAL(15,2) DEFAULT 0,
    unit_price DECIMAL(15,2) NOT NULL,
    total_price DECIMAL(15,2) GENERATED ALWAYS AS (ordered_quantity * unit_price) STORED,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    UNIQUE KEY uk_so_line (sales_order_id, line_number),
    INDEX idx_sales_order_id (sales_order_id),
    INDEX idx_product_id (product_id),
    FOREIGN KEY (sales_order_id) REFERENCES sales_orders(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

#### outbound_shipments
```sql
CREATE TABLE outbound_shipments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    shipment_number VARCHAR(50) UNIQUE NOT NULL,
    sales_order_id BIGINT NOT NULL,
    warehouse_id BIGINT NOT NULL,
    shipment_date DATE NOT NULL,
    status ENUM('DRAFT', 'PICKING', 'PICKED', 'SHIPPED', 'CANCELLED') DEFAULT 'DRAFT',
    carrier VARCHAR(100),
    tracking_number VARCHAR(100),
    notes TEXT,
    picked_at TIMESTAMP NULL,
    picked_by BIGINT,
    shipped_at TIMESTAMP NULL,
    shipped_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT NOT NULL,
    
    INDEX idx_shipment_number (shipment_number),
    INDEX idx_sales_order_id (sales_order_id),
    INDEX idx_warehouse_id (warehouse_id),
    INDEX idx_status (status),
    INDEX idx_shipment_date (shipment_date),
    FOREIGN KEY (sales_order_id) REFERENCES sales_orders(id),
    FOREIGN KEY (warehouse_id) REFERENCES warehouses(id),
    FOREIGN KEY (created_by) REFERENCES accounts(id),
    FOREIGN KEY (picked_by) REFERENCES accounts(id),
    FOREIGN KEY (shipped_by) REFERENCES accounts(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

#### outbound_shipment_lines
```sql
CREATE TABLE outbound_shipment_lines (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    outbound_shipment_id BIGINT NOT NULL,
    sales_order_line_id BIGINT NOT NULL,
    line_number INT NOT NULL,
    product_id BIGINT NOT NULL,
    location_id BIGINT NOT NULL,
    batch_id BIGINT,
    shipped_quantity DECIMAL(15,2) NOT NULL,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE KEY uk_shipment_line (outbound_shipment_id, line_number),
    INDEX idx_outbound_shipment_id (outbound_shipment_id),
    INDEX idx_sales_order_line_id (sales_order_line_id),
    INDEX idx_product_id (product_id),
    INDEX idx_batch_id (batch_id),
    FOREIGN KEY (outbound_shipment_id) REFERENCES outbound_shipments(id) ON DELETE CASCADE,
    FOREIGN KEY (sales_order_line_id) REFERENCES sales_order_lines(id),
    FOREIGN KEY (product_id) REFERENCES products(id),
    FOREIGN KEY (location_id) REFERENCES locations(id),
    FOREIGN KEY (batch_id) REFERENCES batches(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

### 7. Stock Movement Audit

#### stock_movements
```sql
CREATE TABLE stock_movements (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    movement_type ENUM('INBOUND', 'OUTBOUND', 'ADJUSTMENT', 'TRANSFER', 'RETURN') NOT NULL,
    product_id BIGINT NOT NULL,
    warehouse_id BIGINT NOT NULL,
    location_id BIGINT NOT NULL,
    batch_id BIGINT,
    quantity_change DECIMAL(15,2) NOT NULL COMMENT 'Positive for increase, negative for decrease',
    quantity_before DECIMAL(15,2) NOT NULL,
    quantity_after DECIMAL(15,2) NOT NULL,
    reference_type VARCHAR(50) COMMENT 'inbound_receipt, outbound_shipment, adjustment',
    reference_id BIGINT COMMENT 'ID of the reference document',
    reference_number VARCHAR(50),
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT NOT NULL,
    
    INDEX idx_movement_type (movement_type),
    INDEX idx_product_id (product_id),
    INDEX idx_warehouse_id (warehouse_id),
    INDEX idx_batch_id (batch_id),
    INDEX idx_created_at (created_at),
    INDEX idx_reference (reference_type, reference_id),
    INDEX idx_composite (product_id, warehouse_id, created_at),
    FOREIGN KEY (product_id) REFERENCES products(id),
    FOREIGN KEY (warehouse_id) REFERENCES warehouses(id),
    FOREIGN KEY (location_id) REFERENCES locations(id),
    FOREIGN KEY (batch_id) REFERENCES batches(id),
    FOREIGN KEY (created_by) REFERENCES accounts(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

### 8. Reporting & Import Jobs

#### report_jobs
```sql
CREATE TABLE report_jobs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    job_id VARCHAR(50) UNIQUE NOT NULL,
    report_type VARCHAR(50) NOT NULL,
    format ENUM('PDF', 'EXCEL', 'CSV') NOT NULL,
    status ENUM('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED') DEFAULT 'PENDING',
    filters JSON COMMENT 'Report filter parameters',
    file_path VARCHAR(255),
    file_size BIGINT,
    error_message TEXT,
    started_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT NOT NULL,
    
    INDEX idx_job_id (job_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at),
    FOREIGN KEY (created_by) REFERENCES accounts(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

#### import_jobs
```sql
CREATE TABLE import_jobs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    job_id VARCHAR(50) UNIQUE NOT NULL,
    import_type VARCHAR(50) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    status ENUM('PENDING', 'PROCESSING', 'COMPLETED', 'COMPLETED_WITH_ERRORS', 'FAILED') DEFAULT 'PENDING',
    total_rows INT,
    successful_rows INT DEFAULT 0,
    failed_rows INT DEFAULT 0,
    error_details JSON,
    started_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT NOT NULL,
    
    INDEX idx_job_id (job_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at),
    FOREIGN KEY (created_by) REFERENCES accounts(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

## 🔑 Indexes & Constraints Summary

### Primary Indexes (Already in CREATE TABLE)
- All tables have PRIMARY KEY on `id`
- Unique constraints on business identifiers (SKU, order numbers, etc.)

### Foreign Key Constraints
```sql
-- All foreign keys enforce referential integrity
-- CASCADE on child records (order lines, receipt lines)
-- SET NULL on audit fields (created_by, updated_by)
```

### Check Constraints
```sql
-- Prevent negative inventory
ALTER TABLE inventory 
ADD CONSTRAINT chk_on_hand_quantity CHECK (on_hand_quantity >= 0);

ALTER TABLE inventory 
ADD CONSTRAINT chk_reserved_quantity CHECK (reserved_quantity >= 0);
```

### Composite Indexes for Performance
```sql
-- Inventory lookup
CREATE INDEX idx_inventory_lookup ON inventory(product_id, warehouse_id, location_id);

-- Stock movement history
CREATE INDEX idx_stock_movement_history ON stock_movements(product_id, created_at DESC);

-- Order status queries
CREATE INDEX idx_po_status_date ON purchase_orders(status, order_date DESC);
CREATE INDEX idx_so_status_date ON sales_orders(status, order_date DESC);
```

---

## 📊 Sample Data

### Insert Categories
```sql
INSERT INTO product_categories (code, name, description, status) VALUES
('ELEC', 'Electronics', 'Electronic devices and accessories', 'ACTIVE'),
('FOOD', 'Food', 'Perishable and non-perishable food items', 'ACTIVE');
```

### Insert UOMs
```sql
INSERT INTO units_of_measure (code, name, description) VALUES
('PCS', 'Pieces', 'Individual units'),
('BOX', 'Box', 'Boxed items'),
('KG', 'Kilogram', 'Weight in kilograms'),
('LTR', 'Liter', 'Volume in liters'),
('M', 'Meter', 'Length in meters');
```

### Insert Roles
```sql
INSERT INTO roles (name, description) VALUES
('ADMIN', 'System administrator with full access'),
('WAREHOUSE_MANAGER', 'Warehouse manager with operational access'),
('WAREHOUSE_STAFF', 'Warehouse staff for daily operations'),
('VIEWER', 'Read-only access for reporting');
```

### Insert Sample Warehouse
```sql
INSERT INTO warehouses (code, name, address, city, country, type, status) VALUES
('WH-MAIN', 'Main Warehouse', '123 Main Street', 'Hanoi', 'Vietnam', 'MAIN', 'ACTIVE');
```

---

**Cập nhật lần cuối:** 29/01/2026  
**Version:** 2.0
