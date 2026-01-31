# Database Schema - Warehouse Management System
## Complete Database Design & SQL Scripts

---

## 📋 Mục Lục
1. [Database Overview](#database-overview)
2. [ERD Diagram](#erd-diagram)
3. [Table Definitions](#table-definitions)
   - 1. Auth & User Management
   - 2. Email Management
   - 3. Master Data
   - 4. Batch Management
   - 5. Inventory
   - 6. Inbound Operations
   - 7. Outbound Operations
   - 8. Stock Movement Audit
   - 9. Reporting & Import Jobs
4. [Indexes & Constraints](#indexes--constraints-summary)
5. [Sample Data](#sample-data)
6. [Database Schema Evolution](#database-schema-evolution)
7. [Performance Considerations](#performance-considerations)
8. [Data Integrity Rules](#data-integrity-rules)

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
1. ✅ **UUID Primary Keys** - Using CHAR(36) for globally unique identifiers
2. ✅ **Normalized to 3NF** - Minimize data redundancy
3. ✅ **Audit Trail** - Track who/when for all changes (`created_at`, `updated_at`, `created_by`, `updated_by`)
4. ✅ **Soft Delete** - Use status fields instead of hard deletes
5. ✅ **Referential Integrity** - Foreign key constraints with CASCADE/SET NULL policies
6. ✅ **Optimistic Locking** - Version fields for concurrency control (where needed)
7. ✅ **Indexing Strategy** - Optimize for common queries and joins
8. ✅ **ENUMs for Status** - Type-safe status values

---

## 📊 ERD Diagram

### High-Level Entity Relationship
```
┌─────────────────────────────────────────────────────────────────┐
│                    AUTHENTICATION & AUTHORIZATION                │
└─────────────────────────────────────────────────────────────────┘

┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│  ACCOUNTS   │────▶│ACCOUNT_ROLES│◀────│   ROLES     │
│  (CHAR36)   │     │  (Junction) │     │  (CHAR36)   │
└──────┬──────┘     └─────────────┘     └──────┬──────┘
       │                                        │
       │                                        ▼
       │                                 ┌─────────────┐
       │                                 │ROLE_PERMS   │
       │                                 │ (Junction)  │
       │                                 └──────┬──────┘
       │                                        │
       ▼                                        ▼
┌─────────────┐                          ┌─────────────┐
│USER_PROFILES│                          │ PERMISSIONS │
│  (CHAR36)   │                          │  (CHAR36)   │
└─────────────┘                          └─────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                         EMAIL MANAGEMENT                         │
└─────────────────────────────────────────────────────────────────┘

┌─────────────┐
│ EMAIL_LOGS  │───triggered_by──▶ ACCOUNTS
│  (CHAR36)   │
└─────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                    MASTER DATA (MODULE 2)                        │
└─────────────────────────────────────────────────────────────────┘

┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│ WAREHOUSES  │────▶│  LOCATIONS  │     │ CATEGORIES  │
│  (CHAR36)   │     │  (CHAR36)   │     │  (CHAR36)   │
└──────┬──────┘     └─────────────┘     └──────┬──────┘
       │                                        │
       │            ┌─────────────┐             │
       │            │ UNITS_OF_   │             │
       │            │  MEASURE    │             │
       │            │  (CHAR36)   │             │
       │            └──────┬──────┘             │
       │                   │                    │
       │                   ▼                    ▼
       │            ┌─────────────────────────────┐
       │            │       PRODUCTS              │
       │            │       (CHAR36)              │
       │            │  - category_id ────────────┘
       │            │  - uom_id                   
       │            └──────┬──────────────────────┘
       │                   │
       │                   ▼
       │            ┌─────────────┐
       │            │   BATCHES   │ (If batch tracking)
       │            │  (CHAR36)   │
       │            └─────────────┘
       │
       └────────────────────┐
                            │
                            ▼
                    ┌─────────────┐
                    │  INVENTORY  │
                    │  (CHAR36)   │
                    │ product_id  │
                    │ warehouse_id│
                    │ location_id │
                    │ batch_id    │
                    └──────┬──────┘
                           │
                           ▼
                    ┌─────────────┐
                    │   STOCK     │
                    │  MOVEMENTS  │
                    │  (CHAR36)   │
                    └─────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                    BUSINESS PARTNERS                             │
└─────────────────────────────────────────────────────────────────┘

┌─────────────┐
│  BUSINESS   │
│  PARTNERS   │ (type: SUPPLIER, CUSTOMER, BOTH)
│  (CHAR36)   │
└──────┬──────┘
       │
       ├─────▶ PURCHASE_ORDERS ────▶ INBOUND_RECEIPTS
       │       (supplier_id)         
       │
       └─────▶ SALES_ORDERS ────▶ OUTBOUND_SHIPMENTS
               (customer_id)

┌─────────────────────────────────────────────────────────────────┐
│                    FUTURE MODULES (NOT YET)                      │
└─────────────────────────────────────────────────────────────────┘

- PURCHASE_ORDERS & PURCHASE_ORDER_LINES
- INBOUND_RECEIPTS & INBOUND_RECEIPT_LINES
- SALES_ORDERS & SALES_ORDER_LINES
- OUTBOUND_SHIPMENTS & OUTBOUND_SHIPMENT_LINES
- INVENTORY_ADJUSTMENTS
- REPORT_JOBS
- IMPORT_JOBS
```

### Entity Relationships Summary

**1-to-1 Relationships:**
- `accounts` ←→ `user_profiles`

**1-to-Many Relationships:**
- `accounts` → `account_roles` (User has many roles)
- `roles` → `role_permissions` (Role has many permissions)
- `warehouses` → `locations` (Warehouse has many locations)
- `categories` → `products` (Category has many products)
- `units_of_measure` → `products` (UOM used by many products)
- `products` → `batches` (Product has many batches)
- `business_partners` → `purchase_orders` / `sales_orders`

**Many-to-Many Relationships:**
- `accounts` ←→ `roles` (through `account_roles`)
- `roles` ←→ `permissions` (through `role_permissions`)

**Composite Relationships:**
- `inventory` references: `product`, `warehouse`, `location`, `batch`
- `stock_movements` tracks all inventory changes with audit trail

---

## 📋 Table Definitions

### 1. Auth & User Management

#### accounts
```sql
CREATE TABLE accounts (
    id CHAR(36) PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    status ENUM('ACTIVE', 'INACTIVE', 'SUSPENDED', 'DELETED') DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

**Columns:**
- `id`: UUID (CHAR(36)) - Primary key
- `username`: VARCHAR(50) - Unique username
- `password`: VARCHAR(100) - Encrypted password (BCrypt)
- `status`: ENUM - Account status (ACTIVE, INACTIVE, SUSPENDED, DELETED)
- `created_at`, `updated_at`: Audit timestamps
- `created_by`, `updated_by`: Audit user tracking

---

#### roles
```sql
CREATE TABLE roles (
    id CHAR(36) PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name ENUM('ADMIN', 'USER') NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

**Columns:**
- `id`: UUID (CHAR(36)) - Primary key
- `code`: VARCHAR(50) - Unique role code (e.g., ROLE_ADMIN)
- `name`: ENUM - Role name (ADMIN, USER)
- `description`: TEXT - Role description
- Audit fields: created_at, updated_at, created_by, updated_by

---

#### permissions
```sql
CREATE TABLE permissions (
    id CHAR(36) PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

**Columns:**
- `id`: UUID (CHAR(36)) - Primary key
- `code`: VARCHAR(50) - Unique permission code
- `name`: VARCHAR(100) - Permission name
- `description`: TEXT - Permission description
- Audit fields: created_at, updated_at, created_by, updated_by

---

#### account_roles (Junction Table)
```sql
CREATE TABLE account_roles (
    account_id CHAR(36),
    role_id CHAR(36),
    PRIMARY KEY (account_id, role_id),
    FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

**Purpose:** Many-to-many relationship between accounts and roles

---

#### role_permissions (Junction Table)
```sql
CREATE TABLE role_permissions (
    role_id CHAR(36),
    permission_id CHAR(36),
    PRIMARY KEY (role_id, permission_id),
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

**Purpose:** Many-to-many relationship between roles and permissions

---

#### user_profiles
```sql
CREATE TABLE user_profiles (
    id CHAR(36) PRIMARY KEY,
    account_id CHAR(36) UNIQUE,
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    email VARCHAR(100) NOT NULL UNIQUE,
    phone_number VARCHAR(15),
    address VARCHAR(255),
    date_of_birth DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

**Columns:**
- `id`: UUID (CHAR(36)) - Primary key
- `account_id`: CHAR(36) - Foreign key to accounts table
- `first_name`, `last_name`: User's name
- `email`: VARCHAR(100) - Unique email address
- `phone_number`: VARCHAR(15) - Contact number
- `address`: VARCHAR(255) - Physical address
- `date_of_birth`: DATE - User's birth date
- Audit fields: created_at, updated_at, created_by, updated_by

---

### 2. Email Management

#### email_logs
```sql
CREATE TABLE IF NOT EXISTS email_logs (
    id CHAR(36) NOT NULL PRIMARY KEY COMMENT 'UUID primary key',
    recipient VARCHAR(255) NOT NULL COMMENT 'Email recipient',
    cc VARCHAR(1000) NULL COMMENT 'CC recipients (comma-separated)',
    bcc VARCHAR(1000) NULL COMMENT 'BCC recipients (comma-separated)',
    subject VARCHAR(500) NOT NULL COMMENT 'Email subject',
    content TEXT NOT NULL COMMENT 'Email content (HTML or plain text)',
    email_type VARCHAR(50) NOT NULL COMMENT 'Type of email: WELCOME, PASSWORD_RESET, etc.',
    status VARCHAR(20) NOT NULL COMMENT 'Status: PENDING, SENDING, SENT, FAILED, RETRY',
    retry_count INT NOT NULL DEFAULT 0 COMMENT 'Number of retry attempts',
    max_retry INT NOT NULL DEFAULT 3 COMMENT 'Maximum retry attempts',
    error_message TEXT NULL COMMENT 'Error message if failed',
    sent_at DATETIME NULL COMMENT 'Timestamp when email was successfully sent',
    has_attachment BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Whether email has attachment',
    attachment_path VARCHAR(500) NULL COMMENT 'Path to attachment file',
    priority INT NOT NULL DEFAULT 5 COMMENT 'Priority (1=highest, 10=lowest)',
    scheduled_at DATETIME NULL COMMENT 'Scheduled time to send email',
    triggered_by CHAR(36) NULL COMMENT 'Account ID who triggered this email',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Record creation timestamp',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Record update timestamp',
    created_by VARCHAR(50) NULL COMMENT 'User who created this record',
    updated_by VARCHAR(50) NULL COMMENT 'User who last updated this record',

    INDEX idx_recipient (recipient),
    INDEX idx_status (status),
    INDEX idx_email_type (email_type),
    INDEX idx_created_at (created_at),
    INDEX idx_scheduled_at (scheduled_at),
    INDEX idx_triggered_by (triggered_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Email sending history and logs';
```

**Columns:**
- `id`: UUID (CHAR(36)) - Primary key
- `recipient`: VARCHAR(255) - Primary email recipient
- `cc`: VARCHAR(1000) - CC recipients (comma-separated)
- `bcc`: VARCHAR(1000) - BCC recipients (comma-separated)
- `subject`: VARCHAR(500) - Email subject line
- `content`: TEXT - Email content (HTML or plain text)
- `email_type`: VARCHAR(50) - Email template type (WELCOME, PASSWORD_RESET, ORDER_CONFIRMATION, INVENTORY_ALERT)
- `status`: VARCHAR(20) - Email status (PENDING, SENDING, SENT, FAILED, RETRY)
- `retry_count`: INT - Current retry attempt count
- `max_retry`: INT - Maximum allowed retry attempts (default: 3)
- `error_message`: TEXT - Error details if sending failed
- `sent_at`: DATETIME - Timestamp when successfully sent
- `has_attachment`: BOOLEAN - Whether email includes attachments
- `attachment_path`: VARCHAR(500) - File path to attachment
- `priority`: INT - Email priority (1=highest, 10=lowest, default: 5)
- `scheduled_at`: DATETIME - Scheduled delivery time
- `triggered_by`: CHAR(36) - Account ID who triggered the email
- Audit fields: created_at, updated_at, created_by, updated_by

**Purpose:** Track all email sending history, status, and retry logic for audit and debugging

---

### 3. Master Data

#### units_of_measure
```sql
CREATE TABLE units_of_measure (
    id CHAR(36) PRIMARY KEY,
    code VARCHAR(10) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    type ENUM('LENGTH', 'WEIGHT', 'VOLUME', 'COUNT') NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(36),
    
    INDEX idx_code (code),
    INDEX idx_type (type),
    FOREIGN KEY (created_by) REFERENCES accounts(id) ON DELETE SET NULL,
    FOREIGN KEY (updated_by) REFERENCES accounts(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

**Columns:**
- `id`: UUID (CHAR(36)) - Primary key
- `code`: VARCHAR(10) - Unique UOM code (e.g., PCS, KG, M)
- `name`: VARCHAR(100) - UOM name
- `description`: TEXT - Detailed description
- `type`: ENUM - UOM type (LENGTH, WEIGHT, VOLUME, COUNT)
- Audit fields: created_at, updated_at, created_by, updated_by

---

#### categories
```sql
CREATE TABLE categories (
    id CHAR(36) PRIMARY KEY,
    code VARCHAR(20) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    status ENUM('ACTIVE', 'INACTIVE') DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(36),
    
    INDEX idx_code (code),
    INDEX idx_status (status),
    INDEX idx_name (name),
    FOREIGN KEY (created_by) REFERENCES accounts(id) ON DELETE SET NULL,
    FOREIGN KEY (updated_by) REFERENCES accounts(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

**Columns:**
- `id`: UUID (CHAR(36)) - Primary key
- `code`: VARCHAR(20) - Unique category code
- `name`: VARCHAR(100) - Category name
- `description`: VARCHAR(255) - Category description
- `status`: ENUM - Status (ACTIVE, INACTIVE)
- Audit fields: created_at, updated_at, created_by, updated_by

---

#### warehouses
```sql
CREATE TABLE warehouses (
    id CHAR(36) PRIMARY KEY,
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
    manager_id CHAR(36),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(36),
    
    INDEX idx_code (code),
    INDEX idx_status (status),
    INDEX idx_type (type),
    FOREIGN KEY (manager_id) REFERENCES accounts(id) ON DELETE SET NULL,
    FOREIGN KEY (created_by) REFERENCES accounts(id) ON DELETE SET NULL,
    FOREIGN KEY (updated_by) REFERENCES accounts(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

**Columns:**
- `id`: UUID (CHAR(36)) - Primary key
- `code`: VARCHAR(20) - Unique warehouse code
- `name`: VARCHAR(100) - Warehouse name
- `address`, `city`, `state`, `country`, `postal_code`: Location details
- `phone`, `email`: Contact information
- `type`: ENUM - Warehouse type (MAIN, SATELLITE, TRANSIT, RETURN)
- `status`: ENUM - Status (ACTIVE, INACTIVE, MAINTENANCE)
- `capacity`: DECIMAL(15,2) - Total capacity in cubic meters
- `manager_id`: CHAR(36) - Foreign key to accounts table
- Audit fields: created_at, updated_at, created_by, updated_by

---

#### locations
```sql
CREATE TABLE locations (
    id CHAR(36) PRIMARY KEY,
    warehouse_id CHAR(36) NOT NULL,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    zone VARCHAR(50),
    type ENUM('STORAGE', 'PICKING', 'PACKING', 'STAGING', 'RETURN') DEFAULT 'STORAGE',
    capacity DECIMAL(15,2) COMMENT 'Capacity in cubic meters',
    status ENUM('ACTIVE', 'INACTIVE', 'FULL', 'MAINTENANCE') DEFAULT 'ACTIVE',
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(36),
    updated_by VARCHAR(36),
    
    UNIQUE KEY uk_warehouse_code (warehouse_id, code),
    INDEX idx_warehouse_id (warehouse_id),
    INDEX idx_zone (zone),
    INDEX idx_status (status),
    FOREIGN KEY (warehouse_id) REFERENCES warehouses(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES accounts(id) ON DELETE SET NULL,
    FOREIGN KEY (updated_by) REFERENCES accounts(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

**Columns:**
- `id`: UUID (CHAR(36)) - Primary key
- `warehouse_id`: CHAR(36) - Foreign key to warehouses table
- `code`: VARCHAR(50) - Location code (unique per warehouse)
- `name`: VARCHAR(100) - Location name
- `zone`: VARCHAR(50) - Zone designation
- `type`: ENUM - Location type (STORAGE, PICKING, PACKING, STAGING, RETURN)
- `capacity`: DECIMAL(15,2) - Capacity in cubic meters
- `status`: ENUM - Status (ACTIVE, INACTIVE, FULL, MAINTENANCE)
- `notes`: TEXT - Additional notes
- Audit fields: created_at, updated_at, created_by, updated_by

---

#### products
```sql
CREATE TABLE products (
    id CHAR(36) PRIMARY KEY,
    sku VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    category_id CHAR(36) NOT NULL,
    uom_id CHAR(36) NOT NULL,
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
    created_by VARCHAR(36),
    updated_by VARCHAR(36),

    INDEX idx_sku (sku),
    INDEX idx_status (status),
    INDEX idx_name (name),
    INDEX idx_category_id (category_id),
    FULLTEXT idx_search (name, description),
    FOREIGN KEY (uom_id) REFERENCES units_of_measure(id),
    FOREIGN KEY (category_id) REFERENCES categories(id),
    FOREIGN KEY (created_by) REFERENCES accounts(id) ON DELETE SET NULL,
    FOREIGN KEY (updated_by) REFERENCES accounts(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

**Columns:**
- `id`: UUID (CHAR(36)) - Primary key
- `sku`: VARCHAR(50) - Unique stock keeping unit code
- `name`: VARCHAR(200) - Product name
- `description`: TEXT - Product description
- `category_id`: CHAR(36) - Foreign key to categories table
- `uom_id`: CHAR(36) - Foreign key to units_of_measure table
- `weight`: DECIMAL(10,3) - Product weight in KG
- `dimensions`: VARCHAR(50) - Dimensions (LxWxH) in CM
- `status`: ENUM - Product status (ACTIVE, INACTIVE, DISCONTINUED)
- `min_stock_level`, `max_stock_level`, `reorder_point`: Inventory thresholds
- `cost_price`, `selling_price`: DECIMAL(15,2) - Pricing information
- `barcode`: VARCHAR(100) - Product barcode
- `image_url`: VARCHAR(255) - Product image URL
- `requires_batch_tracking`: BOOLEAN - Whether batch tracking is required
- Audit fields: created_at, updated_at, created_by, updated_by

---

#### business_partners
```sql
CREATE TABLE business_partners (
    id CHAR(36) PRIMARY KEY,
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
    created_by VARCHAR(36),
    updated_by VARCHAR(36),
    
    INDEX idx_code (code),
    INDEX idx_type (type),
    INDEX idx_status (status),
    INDEX idx_name (name),
    FOREIGN KEY (created_by) REFERENCES accounts(id) ON DELETE SET NULL,
    FOREIGN KEY (updated_by) REFERENCES accounts(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

**Columns:**
- `id`: UUID (CHAR(36)) - Primary key
- `code`: VARCHAR(20) - Unique business partner code
- `name`: VARCHAR(200) - Partner name
- `type`: ENUM - Partner type (SUPPLIER, CUSTOMER, BOTH)
- `contact_person`: VARCHAR(100) - Contact person name
- `email`, `phone`: Contact information
- `address`, `city`, `country`: Location details
- `tax_id`: VARCHAR(50) - Tax identification number
- `payment_terms`: VARCHAR(100) - Payment terms description
- `credit_limit`: DECIMAL(15,2) - Credit limit amount
- `status`: ENUM - Partner status (ACTIVE, INACTIVE, BLACKLISTED)
- `notes`: TEXT - Additional notes
- Audit fields: created_at, updated_at, created_by, updated_by

---

### 4. Batch Management

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

### 5. Inventory

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

### 6. Inbound Operations

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

### 7. Outbound Operations

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

### 8. Stock Movement Audit

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

### 9. Reporting & Import Jobs

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

### Default Admin Account
```sql
-- Password: admin123 (BCrypt hash)
INSERT INTO accounts (id, username, password, status)
VALUES (
    UUID(),
    'admin',
    '$2a$10$2FrGg2/7Rtr7lQWRZ9UNV..WQblwoUUgJgOxWYnhP.okeEd3Jo5si',
    'ACTIVE'
);
```

### Insert Roles
```sql
INSERT INTO roles (id, code, name, description)
VALUES (
    UUID(),
    'ROLE_ADMIN',
    'ADMIN',
    'Administrator role with full access'
);

INSERT INTO roles (id, code, name, description)
VALUES (
    UUID(),
    'ROLE_USER',
    'USER',
    'Standard user role'
);
```

### Link Admin Account with Role
```sql
INSERT INTO account_roles (account_id, role_id)
SELECT a.id, r.id
FROM accounts a
JOIN roles r ON r.name = 'ADMIN'
WHERE a.username = 'admin';
```

### Insert Admin User Profile
```sql
INSERT INTO user_profiles (id, account_id, first_name, last_name, email)
SELECT
    UUID(),
    a.id,
    'System',
    'Administrator',
    'admin@whs.local'
FROM accounts a
WHERE a.username = 'admin';
```

### Insert Categories
```sql
INSERT INTO categories (id, code, name, description, status)
VALUES 
    (UUID(), 'ELEC', 'Electronics', 'Electronic devices and accessories', 'ACTIVE'),
    (UUID(), 'FOOD', 'Food', 'Perishable and non-perishable food items', 'ACTIVE'),
    (UUID(), 'FURN', 'Furniture', 'Office and home furniture', 'ACTIVE'),
    (UUID(), 'CHEM', 'Chemicals', 'Industrial chemicals', 'ACTIVE');
```

### Insert UOMs
```sql
INSERT INTO units_of_measure (id, code, name, description, type)
VALUES
    (UUID(), 'PCS', 'Pieces', 'Individual units', 'COUNT'),
    (UUID(), 'BOX', 'Box', 'Boxed items', 'COUNT'),
    (UUID(), 'KG', 'Kilogram', 'Weight in kilograms', 'WEIGHT'),
    (UUID(), 'LTR', 'Liter', 'Volume in liters', 'VOLUME'),
    (UUID(), 'M', 'Meter', 'Length in meters', 'LENGTH');
```

### Insert Sample Warehouse
```sql
INSERT INTO warehouses (id, code, name, address, city, country, type, status, capacity)
VALUES (
    UUID(),
    'WH-HN01',
    'Main Warehouse Hanoi',
    '123 Giai Phong Street',
    'Hanoi',
    'Vietnam',
    'MAIN',
    'ACTIVE',
    50000.00
);
```

### Insert Sample Business Partners
```sql
-- Supplier
INSERT INTO business_partners (id, code, name, type, contact_person, email, phone, status)
VALUES (
    UUID(),
    'SUP001',
    'ABC Electronics Supplier',
    'SUPPLIER',
    'John Doe',
    'john@abc-electronics.com',
    '+84901234567',
    'ACTIVE'
);

-- Customer
INSERT INTO business_partners (id, code, name, type, contact_person, email, phone, status)
VALUES (
    UUID(),
    'CUS001',
    'XYZ Retail Customer',
    'CUSTOMER',
    'Jane Smith',
    'jane@xyz-retail.com',
    '+84907654321',
    'ACTIVE'
);
```

---

## 🔄 Database Schema Evolution

### Migration History
1. **V20260107_01** - Create RBAC tables (accounts, roles, permissions, user_profiles)
2. **V20260107_02** - Insert default admin account and role
3. **V20260125_01** - Create email_logs table for email management
4. **V20260129_01** - Create Module 2 Master Data tables (warehouses, locations, products, categories, UOMs, business_partners)

### Key Changes from Initial Design
- Changed from `BIGINT AUTO_INCREMENT` to `CHAR(36)` UUID for primary keys
- Standardized audit fields: `created_at`, `updated_at`, `created_by`, `updated_by`
- Added comprehensive indexes for performance optimization
- Implemented proper foreign key constraints with CASCADE and SET NULL rules
- Added `email_logs` table for email tracking and audit trail

---

## 📈 Performance Considerations

### Indexes Strategy
1. **Primary Keys**: UUID (CHAR(36)) - Unique identifier for all records
2. **Unique Indexes**: Business codes (SKU, warehouse code, user email)
3. **Foreign Key Indexes**: All FK columns for join performance
4. **Composite Indexes**: Multi-column queries (product + warehouse + location)
5. **Full-Text Indexes**: Product search by name and description
6. **Date Indexes**: Time-based queries (created_at, order_date)

### Optimization Tips
```sql
-- Example: Composite index for inventory lookups
CREATE INDEX idx_inventory_lookup 
ON inventory(product_id, warehouse_id, location_id);

-- Example: Covering index for product search
CREATE INDEX idx_product_search 
ON products(status, category_id, name);

-- Full-text search example
SELECT * FROM products 
WHERE MATCH(name, description) AGAINST('laptop' IN NATURAL LANGUAGE MODE);
```

---

## 🔐 Data Integrity Rules

### Foreign Key Policies
- **CASCADE**: Delete child records when parent is deleted (order_lines, receipt_lines)
- **SET NULL**: Preserve child records but clear FK when parent is deleted (created_by, updated_by)
- **RESTRICT**: Prevent parent deletion if children exist (default for most cases)

### Constraints
```sql
-- Prevent negative inventory
CHECK (on_hand_quantity >= 0)
CHECK (reserved_quantity >= 0)

-- Ensure valid dates
CHECK (expiry_date >= manufacture_date)
CHECK (delivery_date >= order_date)

-- Business logic constraints
CHECK (selling_price >= cost_price)
CHECK (max_stock_level >= min_stock_level)
```

---

**Document Version:** 3.0  
**Last Updated:** 31/01/2026  
**Database Version:** MySQL 8.0  
**Schema Status:** Production Ready ✅
