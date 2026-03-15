-- ==============================
-- 1. Convert roles.name from ENUM to VARCHAR
-- ==============================
ALTER TABLE roles
MODIFY COLUMN name VARCHAR(50);

-- ==============================
-- 2. Copy existing role name to code (if empty)
-- ==============================
UPDATE roles
SET code = name
WHERE code IS NULL;

-- ==============================
-- 3. Add unique constraint to code
-- ==============================
ALTER TABLE roles
ADD CONSTRAINT uk_roles_code UNIQUE (code);

-- ==============================
-- 4. Add is_default column to roles
-- ==============================
ALTER TABLE roles
ADD COLUMN is_default BOOLEAN NOT NULL DEFAULT FALSE;

-- =============================
-- Add resource column
-- =============================
ALTER TABLE permissions
ADD COLUMN resource VARCHAR(100);

-- =============================
-- Add action column
-- =============================
ALTER TABLE permissions
ADD COLUMN action VARCHAR(20);

-- =============================
-- Fill action if possible
-- =============================
UPDATE permissions
SET action = SUBSTRING_INDEX(code, '_', -1)
WHERE action IS NULL;
