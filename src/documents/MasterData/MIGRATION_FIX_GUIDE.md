# Fix for Flyway Migration Error - Module 2

## Problem Summary
The migration `V20260129_01__Create_module_2.sql` failed with error:
```
Referencing column 'manager_id' and referenced column 'id' in foreign key constraint 'fk_warehouses_manager' are incompatible.
```

## Root Cause
**Data type mismatch** between foreign key columns and the referenced primary key:
- `accounts.id` is defined as `CHAR(36)` (UUID)
- Foreign key columns (`created_by`, `updated_by`) were defined as `VARCHAR(50)`
- MySQL requires exact data type match for foreign key relationships

## What Was Fixed
Changed all audit columns in module 2 tables from `VARCHAR(50)` to `CHAR(36)`:

### Tables Modified:
1. **warehouses** - `created_by`, `updated_by`
2. **locations** - `created_by`, `updated_by`
3. **units_of_measure** - `created_by`, `updated_by`
4. **categories** - `created_by`, `updated_by`
5. **products** - `created_by`, `updated_by`
6. **business_partners** - `created_by`, `updated_by`

## Steps to Fix and Restart

### Option 1: Manual Database Cleanup (Recommended for Development)

1. **Connect to your MySQL database** using MySQL Workbench or CLI:
   ```bash
   mysql -u your_username -p your_database_name
   ```

2. **Run the cleanup script**:
   ```sql
   -- Drop tables created before the error
   DROP TABLE IF EXISTS business_partners;
   DROP TABLE IF EXISTS products;
   DROP TABLE IF EXISTS categories;
   DROP TABLE IF EXISTS units_of_measure;
   DROP TABLE IF EXISTS locations;
   DROP TABLE IF EXISTS warehouses;

   -- Remove the failed migration record
   DELETE FROM flyway_schema_history 
   WHERE version = '20260129.01' 
     AND success = 0;
   ```

3. **Clean Maven build artifacts**:
   ```powershell
   cd C:\WareHouseSystem\whsBE
   mvn clean
   ```

4. **Restart your application**:
   ```powershell
   mvn spring-boot:run
   ```

### Option 2: Complete Database Reset (Nuclear Option)

If you want to start completely fresh:

1. **Drop and recreate the entire database**:
   ```sql
   DROP DATABASE whs_db;
   CREATE DATABASE whs_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```

2. **Clean and restart**:
   ```powershell
   cd C:\WareHouseSystem\whsBE
   mvn clean
   mvn spring-boot:run
   ```

### Option 3: Use Flyway Repair Command

If tables don't exist but Flyway history is corrupted:

```powershell
mvn flyway:repair
mvn spring-boot:run
```

## Verification

After restarting, check the logs for:
```
✅ Successfully applied migration V20260129_01__Create_module_2.sql
```

Verify tables were created:
```sql
SHOW TABLES;

-- Should show:
-- warehouses
-- locations
-- units_of_measure
-- categories
-- products
-- business_partners
```

## Prevention for Future Migrations

### Standard Data Types to Use:

| Column Type | Data Type | Example |
|------------|-----------|---------|
| Primary Key (UUID) | `CHAR(36)` | `id CHAR(36) PRIMARY KEY` |
| Foreign Key to accounts | `CHAR(36)` | `created_by CHAR(36)` |
| Username/Code | `VARCHAR(50)` | `username VARCHAR(50)` |
| Name | `VARCHAR(100)` | `name VARCHAR(100)` |
| Description | `TEXT` | `description TEXT` |
| Timestamps | `TIMESTAMP` | `created_at TIMESTAMP` |

### Checklist Before Creating New Tables:
- [ ] All foreign keys to `accounts.id` use `CHAR(36)`
- [ ] All foreign keys to other UUID tables use `CHAR(36)`
- [ ] All tables have `created_at`, `updated_at` columns
- [ ] All tables have `created_by`, `updated_by` as `CHAR(36)`
- [ ] All indexes are defined for search/filter columns
- [ ] All foreign keys have proper `ON DELETE` and `ON UPDATE` rules

## Contact
If you encounter any issues, check:
1. Database connection settings in `application.yml`
2. Flyway configuration
3. MySQL version compatibility (MySQL 8+ required)

---
**Fixed by:** Senior Backend Engineer
**Date:** 2026-01-29
