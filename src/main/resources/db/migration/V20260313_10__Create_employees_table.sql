-- ============================================================================
-- Flyway Migration: Create Employees Table
-- Version: V20260228_01
-- Description: Add employees table for WMS warehouse staff management.
--              Follows Option B (extension pattern): keeps user_profiles,
--              adds employees as a role-specific data extension.
-- Author: WMS Backend Team
-- Date: 2026-02-28
-- ============================================================================

CREATE TABLE employees
(
    id               CHAR(36)                                              NOT NULL,
    account_id       CHAR(36)                                              NOT NULL,
    employee_code    VARCHAR(20)                                           NOT NULL,
    department       VARCHAR(100),
    position         VARCHAR(100),
    hire_date        DATE,
    termination_date DATE,
    salary_grade     VARCHAR(20),
    warehouse_id     CHAR(36),
    status           ENUM ('ACTIVE', 'ON_LEAVE', 'TERMINATED') DEFAULT 'ACTIVE' NOT NULL,
    created_at       TIMESTAMP                                 DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at       TIMESTAMP                                 DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,
    created_by       VARCHAR(36),
    updated_by       VARCHAR(36),

    CONSTRAINT pk_employees PRIMARY KEY (id),
    CONSTRAINT uq_employees_account_id UNIQUE (account_id),
    CONSTRAINT uq_employees_employee_code UNIQUE (employee_code),

    CONSTRAINT fk_employees_account FOREIGN KEY (account_id)
        REFERENCES accounts (id) ON DELETE CASCADE,
    CONSTRAINT fk_employees_warehouse FOREIGN KEY (warehouse_id)
        REFERENCES warehouses (id) ON DELETE SET NULL,
    CONSTRAINT fk_employees_created_by FOREIGN KEY (created_by)
        REFERENCES accounts (id) ON DELETE SET NULL,
    CONSTRAINT fk_employees_updated_by FOREIGN KEY (updated_by)
        REFERENCES accounts (id) ON DELETE SET NULL
);

-- Indexes for common filter/sort columns
CREATE INDEX idx_employees_account_id ON employees (account_id);
CREATE INDEX idx_employees_employee_code ON employees (employee_code);
CREATE INDEX idx_employees_warehouse_id ON employees (warehouse_id);
CREATE INDEX idx_employees_status ON employees (status);
CREATE INDEX idx_employees_position ON employees (position);
CREATE INDEX idx_employees_department ON employees (department);
