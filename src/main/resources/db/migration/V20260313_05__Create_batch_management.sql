-- ============================================================================
-- Flyway Migration: Create Batch Management Module
-- Version: V20260201_01
-- Description: Create batches table with indexes and constraints
-- Author: Database Team
-- Date: 2026-02-01
-- ============================================================================

-- Table: batches
CREATE TABLE batches
(
    id                    CHAR(36) PRIMARY KEY,
    batch_number          VARCHAR(50) NOT NULL,
    product_id            CHAR(36)    NOT NULL,
    manufacturing_date    DATE        NOT NULL,
    expiry_date           DATE,
    status                ENUM('AVAILABLE', 'QUARANTINE', 'EXPIRED', 'RECALLED')
                            NOT NULL DEFAULT 'AVAILABLE',
    supplier_batch_number VARCHAR(50),
    notes                 TEXT,
    created_at              TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by              VARCHAR(36),
    updated_by              VARCHAR(36),

    -- Unique constraint
    CONSTRAINT uk_product_batch UNIQUE (product_id, batch_number),

    -- Foreign keys
    CONSTRAINT fk_batch_product FOREIGN KEY (product_id)
        REFERENCES products (id) ON DELETE RESTRICT,
    CONSTRAINT fk_batch_created_by FOREIGN KEY (created_by)
        REFERENCES accounts (id) ON DELETE SET NULL,
    CONSTRAINT fk_batch_updated_by FOREIGN KEY (updated_by)
        REFERENCES accounts (id) ON DELETE SET NULL,

    -- Check constraints
    CONSTRAINT chk_batch_dates CHECK (
        expiry_date IS NULL OR expiry_date > manufacturing_date
        )
);

-- Indexes
CREATE INDEX idx_batch_product_id ON batches (product_id);
CREATE INDEX idx_batch_status ON batches (status);
CREATE INDEX idx_batch_expiry_date ON batches (expiry_date);
CREATE INDEX idx_batch_manufacturing_date ON batches (manufacturing_date);
CREATE INDEX idx_batch_fifo ON batches (product_id, status, manufacturing_date, expiry_date);
CREATE INDEX idx_batch_number ON batches (batch_number);

-- ============================================================================
-- End of Migration
-- ============================================================================