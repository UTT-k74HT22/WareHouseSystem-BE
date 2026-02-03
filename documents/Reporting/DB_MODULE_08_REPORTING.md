# Database Schema - Module 8: Reporting & Export
## Database Design & Migration Guide

---

## 📋 Document Information

| Property | Value |
|----------|-------|
| **Module** | Reporting & Export |
| **Version** | 1.0 |
| **Date** | February 1, 2026 |
| **Status** | Draft |

---

## 🗂️ Schema Overview

### ERD Diagram

```
┌──────────────────────────────────────┐
│       report_requests                │
│                                      │
│  Track async report generation       │
│                                      │
│  - id (PK)                          │
│  - report_type                      │
│  - parameters (JSON)                │
│  - status (PENDING/PROCESSING/...)  │
│  - output_format (PDF/EXCEL/CSV)    │
│  - file_path                        │
│  - file_size                        │
│  - requested_by (FK to accounts)    │
│  - requested_at, completed_at       │
│  - error_message                    │
└──────────────────────────────────────┘

┌──────────────────────────────────────┐
│       report_schedules               │
│                                      │
│  Scheduled report configurations     │
│                                      │
│  - id (PK)                          │
│  - schedule_name                    │
│  - report_type                      │
│  - parameters (JSON)                │
│  - cron_expression                  │
│  - output_format                    │
│  - recipients (JSON array)          │
│  - enabled                          │
│  - last_run_at, next_run_at         │
│  - created_by (FK to accounts)      │
└──────────────────────────────────────┘
```

---

## 📊 Table Definitions

### Table: report_requests

```sql
CREATE TABLE report_requests (
    -- Primary Key
    id                      CHAR(36) PRIMARY KEY,
    
    -- Report Configuration
    report_type             ENUM(
                                'STOCK_CURRENT',
                                'STOCK_VALUATION',
                                'MOVEMENTS',
                                'BATCH_TRACEABILITY',
                                'LOW_STOCK',
                                'EXPIRING_BATCHES',
                                'INBOUND_ACTIVITY',
                                'OUTBOUND_ACTIVITY',
                                'SUPPLIER_PERFORMANCE',
                                'WAREHOUSE_PERFORMANCE'
                            ) NOT NULL,
    parameters              JSON 
                            COMMENT 'Report filters and parameters (warehouse_id, date_range, etc.)',
    output_format           ENUM('PDF', 'EXCEL', 'CSV') NOT NULL DEFAULT 'PDF',
    
    -- Processing Status
    status                  ENUM('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED') 
                            NOT NULL DEFAULT 'PENDING',
    
    -- Output File
    file_path               VARCHAR(500) 
                            COMMENT 'Path to generated file',
    file_size               BIGINT 
                            COMMENT 'File size in bytes',
    download_url            VARCHAR(500) 
                            COMMENT 'Pre-signed download URL',
    expires_at              TIMESTAMP 
                            COMMENT 'When download link expires (7 days default)',
    
    -- Error Handling
    error_message           TEXT 
                            COMMENT 'Error details if status = FAILED',
    retry_count             INT NOT NULL DEFAULT 0 
                            COMMENT 'Number of retry attempts',
    
    -- Timing
    requested_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at              TIMESTAMP 
                            COMMENT 'When processing started',
    completed_at            TIMESTAMP 
                            COMMENT 'When processing finished',
    
    -- User
    requested_by            CHAR(36) NOT NULL 
                            COMMENT 'User who requested report',
    
    -- Audit
    created_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT fk_report_request_user FOREIGN KEY (requested_by) 
        REFERENCES accounts(id) ON DELETE CASCADE,
    
    -- Check Constraints
    CONSTRAINT chk_report_status_dates CHECK (
        (status = 'PENDING' AND started_at IS NULL AND completed_at IS NULL)
        OR (status = 'PROCESSING' AND started_at IS NOT NULL AND completed_at IS NULL)
        OR (status IN ('COMPLETED', 'FAILED') AND started_at IS NOT NULL AND completed_at IS NOT NULL)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Async report generation requests';
```

---

### Table: report_schedules

```sql
CREATE TABLE report_schedules (
    -- Primary Key
    id                      CHAR(36) PRIMARY KEY,
    
    -- Schedule Configuration
    schedule_name           VARCHAR(200) NOT NULL 
                            COMMENT 'Human-readable name',
    description             TEXT,
    report_type             ENUM(
                                'STOCK_CURRENT',
                                'STOCK_VALUATION',
                                'MOVEMENTS',
                                'LOW_STOCK',
                                'EXPIRING_BATCHES',
                                'INBOUND_ACTIVITY',
                                'OUTBOUND_ACTIVITY',
                                'SUPPLIER_PERFORMANCE',
                                'WAREHOUSE_PERFORMANCE'
                            ) NOT NULL,
    parameters              JSON 
                            COMMENT 'Report filters (warehouse_id, date_range_type, etc.)',
    output_format           ENUM('PDF', 'EXCEL', 'CSV') NOT NULL DEFAULT 'EXCEL',
    
    -- Schedule Timing (Cron Expression)
    cron_expression         VARCHAR(100) NOT NULL 
                            COMMENT 'Quartz cron expression (e.g., 0 8 * * 1-5)',
    timezone                VARCHAR(50) DEFAULT 'UTC' 
                            COMMENT 'Timezone for schedule',
    
    -- Recipients
    recipients              JSON NOT NULL 
                            COMMENT 'Array of email addresses ["user1@example.com", "user2@example.com"]',
    
    -- Status
    enabled                 BOOLEAN NOT NULL DEFAULT TRUE 
                            COMMENT 'Whether schedule is active',
    
    -- Execution Tracking
    last_run_at             TIMESTAMP 
                            COMMENT 'When schedule last executed',
    last_run_status         ENUM('SUCCESS', 'FAILED'),
    last_run_error          TEXT,
    next_run_at             TIMESTAMP 
                            COMMENT 'When schedule will run next (calculated)',
    run_count               INT NOT NULL DEFAULT 0 
                            COMMENT 'Total number of executions',
    
    -- Audit
    created_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by              CHAR(36) NOT NULL,
    updated_by              CHAR(36),
    
    -- Constraints
    CONSTRAINT fk_schedule_created_by FOREIGN KEY (created_by) 
        REFERENCES accounts(id) ON DELETE CASCADE,
    CONSTRAINT fk_schedule_updated_by FOREIGN KEY (updated_by) 
        REFERENCES accounts(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Scheduled report configurations';
```

---

## 📑 Indexes

```sql
-- report_requests
CREATE INDEX idx_report_request_user ON report_requests(requested_by);
CREATE INDEX idx_report_request_status ON report_requests(status);
CREATE INDEX idx_report_request_type ON report_requests(report_type);
CREATE INDEX idx_report_request_requested_at ON report_requests(requested_at DESC);
CREATE INDEX idx_report_request_expires ON report_requests(expires_at);

-- For cleanup job (delete expired reports)
CREATE INDEX idx_report_request_cleanup 
    ON report_requests(status, expires_at) 
    WHERE status = 'COMPLETED' AND expires_at < NOW();

-- report_schedules
CREATE INDEX idx_schedule_enabled ON report_schedules(enabled);
CREATE INDEX idx_schedule_next_run ON report_schedules(enabled, next_run_at);
CREATE INDEX idx_schedule_created_by ON report_schedules(created_by);
```

---

## 🔄 Flyway Migration

### File: `V20260201_06__Create_reporting.sql`

```sql
-- ============================================================================
-- Flyway Migration: Create Reporting & Export Module
-- Version: V20260201_06
-- Description: Create report_requests and report_schedules tables
-- Author: Database Team
-- Date: 2026-02-01
-- ============================================================================

-- Table: report_requests
CREATE TABLE report_requests (
    id                      CHAR(36) PRIMARY KEY,
    report_type             ENUM(
                                'STOCK_CURRENT',
                                'STOCK_VALUATION',
                                'MOVEMENTS',
                                'BATCH_TRACEABILITY',
                                'LOW_STOCK',
                                'EXPIRING_BATCHES',
                                'INBOUND_ACTIVITY',
                                'OUTBOUND_ACTIVITY',
                                'SUPPLIER_PERFORMANCE',
                                'WAREHOUSE_PERFORMANCE'
                            ) NOT NULL,
    parameters              JSON,
    output_format           ENUM('PDF', 'EXCEL', 'CSV') NOT NULL DEFAULT 'PDF',
    status                  ENUM('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED') 
                            NOT NULL DEFAULT 'PENDING',
    file_path               VARCHAR(500),
    file_size               BIGINT,
    download_url            VARCHAR(500),
    expires_at              TIMESTAMP,
    error_message           TEXT,
    retry_count             INT NOT NULL DEFAULT 0,
    requested_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at              TIMESTAMP,
    completed_at            TIMESTAMP,
    requested_by            CHAR(36) NOT NULL,
    created_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_report_request_user FOREIGN KEY (requested_by) 
        REFERENCES accounts(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_report_request_user ON report_requests(requested_by);
CREATE INDEX idx_report_request_status ON report_requests(status);
CREATE INDEX idx_report_request_requested_at ON report_requests(requested_at DESC);

-- Table: report_schedules
CREATE TABLE report_schedules (
    id                      CHAR(36) PRIMARY KEY,
    schedule_name           VARCHAR(200) NOT NULL,
    description             TEXT,
    report_type             ENUM(
                                'STOCK_CURRENT',
                                'STOCK_VALUATION',
                                'MOVEMENTS',
                                'LOW_STOCK',
                                'EXPIRING_BATCHES',
                                'INBOUND_ACTIVITY',
                                'OUTBOUND_ACTIVITY',
                                'SUPPLIER_PERFORMANCE',
                                'WAREHOUSE_PERFORMANCE'
                            ) NOT NULL,
    parameters              JSON,
    output_format           ENUM('PDF', 'EXCEL', 'CSV') NOT NULL DEFAULT 'EXCEL',
    cron_expression         VARCHAR(100) NOT NULL,
    timezone                VARCHAR(50) DEFAULT 'UTC',
    recipients              JSON NOT NULL,
    enabled                 BOOLEAN NOT NULL DEFAULT TRUE,
    last_run_at             TIMESTAMP,
    last_run_status         ENUM('SUCCESS', 'FAILED'),
    last_run_error          TEXT,
    next_run_at             TIMESTAMP,
    run_count               INT NOT NULL DEFAULT 0,
    created_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by              CHAR(36) NOT NULL,
    updated_by              CHAR(36),
    
    CONSTRAINT fk_schedule_created_by FOREIGN KEY (created_by) 
        REFERENCES accounts(id) ON DELETE CASCADE,
    CONSTRAINT fk_schedule_updated_by FOREIGN KEY (updated_by) 
        REFERENCES accounts(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_schedule_enabled ON report_schedules(enabled);
CREATE INDEX idx_schedule_next_run ON report_schedules(enabled, next_run_at);

-- ============================================================================
-- End of Migration
-- ============================================================================
```

---

## 📝 Sample Data

```sql
-- Sample report request
INSERT INTO report_requests (
    id, report_type, parameters, output_format, status, requested_by
) VALUES (
    UUID(),
    'STOCK_CURRENT',
    JSON_OBJECT(
        'warehouse_id', '{{warehouse_uuid}}',
        'include_zero_stock', false,
        'category_id', NULL
    ),
    'EXCEL',
    'PENDING',
    '{{user_uuid}}'
);

-- Sample schedule: Daily low stock alert
INSERT INTO report_schedules (
    id, schedule_name, report_type, parameters, output_format,
    cron_expression, recipients, enabled, created_by
) VALUES (
    UUID(),
    'Daily Low Stock Alert',
    'LOW_STOCK',
    JSON_OBJECT(
        'threshold_type', 'REORDER_POINT',
        'warehouse_id', '{{warehouse_uuid}}'
    ),
    'EXCEL',
    '0 8 * * 1-5',  -- Weekdays at 8 AM
    JSON_ARRAY('manager@company.com', 'purchasing@company.com'),
    TRUE,
    '{{admin_uuid}}'
);
```

---

## 🔍 Query Examples

### 1. Find Pending Report Requests (for worker)

```sql
SELECT 
    rr.id,
    rr.report_type,
    rr.parameters,
    rr.output_format,
    rr.requested_by,
    rr.requested_at
FROM report_requests rr
WHERE rr.status = 'PENDING'
ORDER BY rr.requested_at ASC
LIMIT 10;
```

### 2. Find Schedules to Run

```sql
SELECT 
    rs.id,
    rs.schedule_name,
    rs.report_type,
    rs.parameters,
    rs.output_format,
    rs.recipients,
    rs.next_run_at
FROM report_schedules rs
WHERE rs.enabled = TRUE
    AND rs.next_run_at <= NOW()
ORDER BY rs.next_run_at ASC;
```

### 3. User's Recent Reports

```sql
SELECT 
    rr.id,
    rr.report_type,
    rr.output_format,
    rr.status,
    rr.file_size,
    rr.download_url,
    rr.expires_at,
    rr.requested_at,
    rr.completed_at,
    TIMESTAMPDIFF(SECOND, rr.started_at, rr.completed_at) AS generation_time_seconds
FROM report_requests rr
WHERE rr.requested_by = ?
ORDER BY rr.requested_at DESC
LIMIT 20;
```

### 4. Cleanup Expired Reports

```sql
-- Find expired reports to delete
SELECT 
    id, file_path
FROM report_requests
WHERE status = 'COMPLETED'
    AND expires_at < NOW()
    AND file_path IS NOT NULL;

-- After deleting files, update records
UPDATE report_requests
SET file_path = NULL,
    download_url = NULL,
    file_size = NULL
WHERE status = 'COMPLETED'
    AND expires_at < NOW();
```

---

## 🎯 Performance Considerations

### Report Generation Optimization

Most reports query existing tables (inventory, stock_movements, etc.). Key optimizations:

1. **Use Indexes**: Ensure all filter columns have indexes
2. **Pagination**: Generate reports in chunks for very large datasets
3. **Streaming**: Use streaming for Excel export (Apache POI SXSSFWorkbook)
4. **Caching**: Cache report templates, not data
5. **Async**: Queue reports estimated to take > 10 seconds

### Scheduled Job Considerations

```sql
-- Calculate next_run_at using cron expression
-- Update after each execution
UPDATE report_schedules
SET last_run_at = NOW(),
    last_run_status = 'SUCCESS',
    run_count = run_count + 1,
    next_run_at = calculateNextRun(cron_expression, NOW())  -- Application logic
WHERE id = ?;
```

---

## ✅ Data Integrity Checks

```sql
-- Check 1: Find reports with invalid status transitions
SELECT * FROM report_requests
WHERE status = 'COMPLETED' AND file_path IS NULL;

-- Check 2: Find very old pending requests (stuck)
SELECT * FROM report_requests
WHERE status IN ('PENDING', 'PROCESSING')
    AND requested_at < DATE_SUB(NOW(), INTERVAL 1 HOUR);

-- Check 3: Find schedules with no recipients
SELECT * FROM report_schedules
WHERE JSON_LENGTH(recipients) = 0;
```

---

**Document Version:** 1.0  
**Last Updated:** February 1, 2026  
**Status:** 🚧 Draft - Pending Review
