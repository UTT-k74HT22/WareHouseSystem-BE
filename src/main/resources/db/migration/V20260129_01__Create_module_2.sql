CREATE TABLE warehouses
(
    id          CHAR(36) PRIMARY KEY,
    code        VARCHAR(20) UNIQUE NOT NULL,
    name        VARCHAR(100)       NOT NULL,
    address     VARCHAR(255),
    city        VARCHAR(50),
    state       VARCHAR(50),
    country     VARCHAR(50),
    postal_code VARCHAR(20),
    phone       VARCHAR(20),
    email       VARCHAR(100),
    type        ENUM ('MAIN', 'SATELLITE', 'TRANSIT', 'RETURN') DEFAULT 'MAIN',
    status      ENUM ('ACTIVE', 'INACTIVE', 'MAINTENANCE')      DEFAULT 'ACTIVE',
    capacity    DECIMAL(15, 2) COMMENT 'Total capacity in cubic meters',
    manager_id  CHAR(36),
    created_at  TIMESTAMP                                        DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP                                        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by  CHAR(36),
    updated_by  CHAR(36),

    INDEX idx_code (code),
    INDEX idx_status (status),
    INDEX idx_type (type),
    FOREIGN KEY (manager_id) REFERENCES accounts (id) ON DELETE SET NULL,
    FOREIGN KEY (created_by) REFERENCES accounts (id) ON DELETE SET NULL,
    FOREIGN KEY (updated_by) REFERENCES accounts (id) ON DELETE SET NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE locations
(
    id           CHAR(36) PRIMARY KEY,
    warehouse_id CHAR(36)     NOT NULL,
    code         VARCHAR(50)  NOT NULL,
    name         VARCHAR(100) NOT NULL,
    zone         VARCHAR(50),
    type         ENUM ('STORAGE', 'PICKING', 'PACKING', 'STAGING', 'RETURN') DEFAULT 'STORAGE',
    capacity     DECIMAL(15, 2) COMMENT 'Capacity in cubic meters',
    status       ENUM ('ACTIVE', 'INACTIVE', 'FULL', 'MAINTENANCE')          DEFAULT 'ACTIVE',
    notes        TEXT,
    created_at   TIMESTAMP                                                    DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP                                                    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by   CHAR(36),
    updated_by   CHAR(36),

    UNIQUE KEY uk_warehouse_code (warehouse_id, code),
    INDEX idx_warehouse_id (warehouse_id),
    INDEX idx_zone (zone),
    INDEX idx_status (status),
    FOREIGN KEY (warehouse_id) REFERENCES warehouses (id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES accounts (id) ON DELETE SET NULL,
    FOREIGN KEY (updated_by) REFERENCES accounts (id) ON DELETE SET NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE units_of_measure
(
    id          CHAR(36) PRIMARY KEY,
    code        VARCHAR(10) UNIQUE                           NOT NULL,
    name        VARCHAR(100)                                 NOT NULL,
    description TEXT,
    type        ENUM ('LENGTH', 'WEIGHT', 'VOLUME', 'COUNT') NOT NULL,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by  CHAR(36),
    updated_by  CHAR(36),

    INDEX idx_code (code),
    INDEX idx_type (type),
    FOREIGN KEY (created_by) REFERENCES accounts (id) ON DELETE SET NULL,
    FOREIGN KEY (updated_by) REFERENCES accounts (id) ON DELETE SET NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE categories
(
    id          CHAR(36) PRIMARY KEY,
    code        VARCHAR(20) UNIQUE NOT NULL,
    name        VARCHAR(100)       NOT NULL,
    description VARCHAR(255),
    status      ENUM ('ACTIVE', 'INACTIVE') DEFAULT 'ACTIVE',
    created_at  TIMESTAMP                   DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP                   DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by  CHAR(36),
    updated_by  CHAR(36),

    INDEX idx_code (code),
    INDEX idx_status (status),
    INDEX idx_name (name),
    FOREIGN KEY (created_by) REFERENCES accounts (id) ON DELETE SET NULL,
    FOREIGN KEY (updated_by) REFERENCES accounts (id) ON DELETE SET NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE products
(
    id                      CHAR(36) PRIMARY KEY,
    sku                     VARCHAR(50) UNIQUE NOT NULL,
    name                    VARCHAR(200)       NOT NULL,
    description             TEXT,
    category_id             CHAR(36)           NOT NULL,
    uom_id                  CHAR(36)           NOT NULL,
    weight                  DECIMAL(10, 3) COMMENT 'Weight in KG',
    dimensions              VARCHAR(50) COMMENT 'LxWxH in CM',
    status                  ENUM ('ACTIVE', 'INACTIVE', 'DISCONTINUED') DEFAULT 'ACTIVE',
    min_stock_level         DECIMAL(15, 2)                              DEFAULT 0,
    max_stock_level         DECIMAL(15, 2),
    reorder_point           DECIMAL(15, 2),
    cost_price              DECIMAL(15, 2),
    selling_price           DECIMAL(15, 2),
    barcode                 VARCHAR(100),
    image_url               VARCHAR(255),
    requires_batch_tracking BOOLEAN                                     DEFAULT FALSE,
    created_at              TIMESTAMP                                   DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP                                   DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by              CHAR(36),
    updated_by              CHAR(36),

    INDEX idx_sku (sku),
    INDEX idx_status (status),
    INDEX idx_name (name),
    INDEX idx_category_id (category_id),
    FULLTEXT idx_search (name, description),
    FOREIGN KEY (uom_id) REFERENCES units_of_measure (id),
    FOREIGN KEY (category_id) REFERENCES categories (id),
    FOREIGN KEY (created_by) REFERENCES accounts (id) ON DELETE SET NULL,
    FOREIGN KEY (updated_by) REFERENCES accounts (id) ON DELETE SET NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;


CREATE TABLE business_partners
(
    id             CHAR(36) PRIMARY KEY,
    code           VARCHAR(20) UNIQUE                    NOT NULL,
    name           VARCHAR(200)                          NOT NULL,
    type           ENUM ('SUPPLIER', 'CUSTOMER', 'BOTH') NOT NULL,
    contact_person VARCHAR(100),
    email          VARCHAR(100),
    phone          VARCHAR(20),
    address        VARCHAR(255),
    city           VARCHAR(50),
    country        VARCHAR(50),
    tax_id         VARCHAR(50),
    payment_terms  VARCHAR(100),
    credit_limit   DECIMAL(15, 2),
    status         ENUM ('ACTIVE', 'INACTIVE', 'BLACKLISTED') DEFAULT 'ACTIVE',
    notes          TEXT,
    created_at     TIMESTAMP                                  DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP                                  DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by     CHAR(36),
    updated_by     CHAR(36),

    INDEX idx_code (code),
    INDEX idx_type (type),
    INDEX idx_status (status),
    INDEX idx_name (name),
    FOREIGN KEY (created_by) REFERENCES accounts (id) ON DELETE SET NULL,
    FOREIGN KEY (updated_by) REFERENCES accounts (id) ON DELETE SET NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;