-- ============================================================================
-- Flyway Migration: Create Customers Table
-- Version: V20260403_01
-- Description: Add customers table for B2C self-registration flow.
--              Follows the same extension pattern as employees:
--                accounts        → login credentials
--                user_profiles   → shared personal info (name, email, phone,
--                                  address, date_of_birth) — NOT duplicated here
--                customers       → customer-specific data only
--              Customers register themselves via /api/v1/auth/register.
-- Author: WMS Backend Team
-- Date: 2026-03-01
-- ============================================================================

-- ----------------------------------------------------------------------------
-- TABLE: customers
--
-- ┌─────────────┐   1:1   ┌───────────────┐   1:1   ┌───────────────────┐
-- │  accounts   │◄───────►│ user_profiles │         │     customers     │
-- │  (login)    │         │ (shared info) │◄───────►│ (customer-only)   │
-- └─────────────┘         └───────────────┘         └───────────────────┘
--
-- Thông tin chung (name, email, phone, address, dob) → user_profiles
-- Thông tin riêng của khách hàng                     → customers (bảng này)
-- ----------------------------------------------------------------------------
CREATE TABLE customers
(
    -- ── Primary key ──────────────────────────────────────────────────────────
    id                  CHAR(36)     NOT NULL,

    -- ── Auth link (1-to-1, mirrors employees pattern) ────────────────────────
    -- Joins to accounts → user_profiles for all shared personal info.
    account_id          CHAR(36)     NOT NULL,

    -- ── Customer identity code ────────────────────────────────────────────────
    -- Auto-generated unique business code, e.g. CUST-000001.
    -- Separate from account username; used in orders, invoices, support tickets.
    customer_code       VARCHAR(20)  NOT NULL
        COMMENT 'Auto-generated unique code used in orders & invoices, e.g. CUST-000001',

    -- ── Financial ─────────────────────────────────────────────────────────────
    credit_limit        DECIMAL(15, 2) DEFAULT 0.00
        COMMENT 'Max outstanding balance allowed; 0 = pay-upfront only',

    -- ── Loyalty / tier ────────────────────────────────────────────────────────
    loyalty_points      INT            DEFAULT 0
        COMMENT 'Accumulated reward points from purchases',
    customer_tier       ENUM ('STANDARD', 'SILVER', 'GOLD', 'PLATINUM') DEFAULT 'STANDARD'
        COMMENT 'Tier governs discount rates and fulfillment priority',

    -- ── Tax info (optional, customer-specific) ────────────────────────────────
    tax_id              VARCHAR(50)
        COMMENT 'Personal or business tax ID for invoice generation',

    -- ── Optional B2B upgrade link ─────────────────────────────────────────────
    -- Set if this customer is later promoted to a formal business partner.
    -- Keeps order history unified without merging the two domains.
    business_partner_id CHAR(36)
        COMMENT 'FK to business_partners when customer is promoted to B2B',

    -- ── Lifecycle ─────────────────────────────────────────────────────────────
    status              ENUM ('ACTIVE', 'INACTIVE', 'SUSPENDED', 'BLACKLISTED') DEFAULT 'ACTIVE' NOT NULL,
    email_verified      BOOLEAN        DEFAULT FALSE NOT NULL
        COMMENT 'Mirrors accounts email-verification state for quick filtering',
    notes               TEXT,

    -- ── Audit fields ──────────────────────────────────────────────────────────
    created_at          TIMESTAMP      DEFAULT CURRENT_TIMESTAMP             NOT NULL,
    updated_at          TIMESTAMP      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,
    created_by          VARCHAR(36)
        COMMENT 'NULL when customer self-registers; populated when created by admin',
    updated_by          VARCHAR(36),

    -- ── Constraints ───────────────────────────────────────────────────────────
    CONSTRAINT pk_customers                  PRIMARY KEY (id),
    CONSTRAINT uq_customers_account_id       UNIQUE (account_id),
    CONSTRAINT uq_customers_customer_code    UNIQUE (customer_code),

    CONSTRAINT fk_customers_account          FOREIGN KEY (account_id)
        REFERENCES accounts (id) ON DELETE CASCADE,
    CONSTRAINT fk_customers_business_partner FOREIGN KEY (business_partner_id)
        REFERENCES business_partners (id) ON DELETE SET NULL,
    CONSTRAINT fk_customers_created_by       FOREIGN KEY (created_by)
        REFERENCES accounts (id) ON DELETE SET NULL,
    CONSTRAINT fk_customers_updated_by       FOREIGN KEY (updated_by)
        REFERENCES accounts (id) ON DELETE SET NULL
);

-- ── Indexes ───────────────────────────────────────────────────────────────────
CREATE INDEX idx_customers_account_id        ON customers (account_id);
CREATE INDEX idx_customers_customer_code     ON customers (customer_code);
CREATE INDEX idx_customers_status            ON customers (status);
CREATE INDEX idx_customers_tier              ON customers (customer_tier);
CREATE INDEX idx_customers_business_partner  ON customers (business_partner_id);
