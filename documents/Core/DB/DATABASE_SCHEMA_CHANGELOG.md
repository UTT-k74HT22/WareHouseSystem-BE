# Database Schema Changelog

## Version 3.0 - 31/01/2026

### 🔄 Major Changes

#### 1. Primary Key Strategy Changed
- **Before**: `BIGINT AUTO_INCREMENT`
- **After**: `CHAR(36)` UUID
- **Impact**: All tables now use UUID for globally unique identifiers
- **Benefit**: Better for distributed systems and microservices architecture

#### 2. New Tables Added

##### Email Management (V20260125_01)
- **email_logs**: Complete email tracking system
  - Stores all email sending history
  - Tracks status, retry attempts, and errors
  - Supports scheduled emails and priority levels
  - Links to accounts via `triggered_by` field

##### Master Data Module (V20260129_01)
- **warehouses**: Physical warehouse locations
- **locations**: Storage locations within warehouses
- **categories**: Product categorization
- **products**: Product master data with full details
- **units_of_measure**: Measurement units (LENGTH, WEIGHT, VOLUME, COUNT)
- **business_partners**: Suppliers and customers

#### 3. Schema Standardization

All tables now follow consistent patterns:

**Audit Fields (Standard across all tables):**
```sql
created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
created_by VARCHAR(50) or VARCHAR(36)
updated_by VARCHAR(50) or VARCHAR(36)
```

**Primary Keys:**
```sql
id CHAR(36) PRIMARY KEY  -- UUID format
```

**Status Fields:**
- Consistent ENUM types across similar entities
- Clear status progression paths

#### 4. Foreign Key Policies

**Cascade Delete:**
- Used for child/detail records (e.g., `account_roles`, `role_permissions`, `locations`)
- When parent is deleted, children are automatically removed

**Set Null:**
- Used for audit fields and optional references
- Preserves records even if referenced entity is deleted
- Examples: `created_by`, `updated_by`, `manager_id`

#### 5. Indexing Strategy

**Indexes Added:**
- Primary keys (id) on all tables
- Unique indexes on business codes (code, sku, username, email)
- Foreign key indexes for join optimization
- Composite indexes for common query patterns
- Full-text indexes for product search
- Date/timestamp indexes for reporting

**Example Composite Indexes:**
```sql
-- Unique constraint per warehouse
UNIQUE KEY uk_warehouse_code (warehouse_id, code)

-- Product search optimization
INDEX idx_category_id (category_id)
FULLTEXT idx_search (name, description)
```

---

## Migration History

### V20260107_01 - RBAC Foundation
- Created authentication and authorization tables
- Tables: `accounts`, `roles`, `permissions`, `account_roles`, `role_permissions`, `user_profiles`

### V20260107_02 - Default Data
- Inserted default ADMIN role
- Created default admin account (username: admin, password: admin123)
- Linked admin account with ADMIN role
- Created admin user profile

### V20260125_01 - Email System
- Created `email_logs` table
- Added comprehensive email tracking
- Supports retry logic and scheduled emails

### V20260129_01 - Master Data Module
- Created complete master data structure
- Tables: `warehouses`, `locations`, `categories`, `products`, `units_of_measure`, `business_partners`
- Established relationships between entities
- Added proper foreign key constraints

---

## Data Type Changes

### String Fields
| Field Type | Before | After | Reason |
|------------|--------|-------|--------|
| Primary Key | BIGINT | CHAR(36) | UUID support |
| Username | VARCHAR(50) | VARCHAR(50) | No change |
| Password | VARCHAR(255) | VARCHAR(100) | BCrypt only needs 60 chars |
| Email | VARCHAR(100) | VARCHAR(100) | No change |
| Created By | VARCHAR(50) | VARCHAR(50) | No change |
| Updated By | - | VARCHAR(36) | Now tracks UUID |

### Date/Time Fields
- All timestamps use `TIMESTAMP` type
- Default: `CURRENT_TIMESTAMP`
- Auto-update on update: `ON UPDATE CURRENT_TIMESTAMP`

---

## Breaking Changes

### ⚠️ Important Notes

1. **UUID Generation**: Application must generate UUIDs before insert
   ```java
   String id = UUID.randomUUID().toString();
   ```

2. **Foreign Key References**: All FKs now use CHAR(36) instead of BIGINT
   ```java
   // Before
   private Long accountId;
   
   // After
   private String accountId; // UUID
   ```

3. **JPA Entity Changes**: Update entity classes
   ```java
   @Id
   @Column(name = "id", length = 36)
   private String id;
   
   @PrePersist
   public void generateId() {
       if (id == null) {
           id = UUID.randomUUID().toString();
       }
   }
   ```

---

## Current Database State

### Tables Implemented (9 tables)
✅ accounts  
✅ roles  
✅ permissions  
✅ account_roles  
✅ role_permissions  
✅ user_profiles  
✅ email_logs  
✅ warehouses  
✅ locations  
✅ categories  
✅ products  
✅ units_of_measure  
✅ business_partners  

### Tables Pending (Future Modules)
⏳ batches  
⏳ inventory  
⏳ inventory_adjustments  
⏳ purchase_orders  
⏳ purchase_order_lines  
⏳ inbound_receipts  
⏳ inbound_receipt_lines  
⏳ sales_orders  
⏳ sales_order_lines  
⏳ outbound_shipments  
⏳ outbound_shipment_lines  
⏳ stock_movements  
⏳ report_jobs  
⏳ import_jobs  

---

## Testing Recommendations

### 1. Verify UUID Generation
```sql
SELECT id, username FROM accounts;
-- Should return 36-character UUIDs
```

### 2. Check Foreign Key Constraints
```sql
-- This should work
INSERT INTO locations (id, warehouse_id, code, name, type, status)
VALUES (UUID(), 'valid-warehouse-id', 'LOC001', 'Location 1', 'STORAGE', 'ACTIVE');

-- This should fail (FK constraint violation)
INSERT INTO locations (id, warehouse_id, code, name, type, status)
VALUES (UUID(), 'invalid-id', 'LOC001', 'Location 1', 'STORAGE', 'ACTIVE');
```

### 3. Verify Cascade Delete
```sql
-- Delete warehouse should cascade to locations
DELETE FROM warehouses WHERE code = 'TEST-WH';
-- Locations under this warehouse should also be deleted
```

### 4. Verify Set Null Policy
```sql
-- Delete account should set created_by to NULL, not delete the record
DELETE FROM accounts WHERE username = 'test-user';
-- Warehouses created by this user should still exist, with created_by = NULL
```

---

## Performance Monitoring

### Queries to Monitor

1. **Product Search by Category**
```sql
SELECT * FROM products WHERE category_id = ? AND status = 'ACTIVE';
-- Should use idx_category_id and idx_status
```

2. **Full-Text Product Search**
```sql
SELECT * FROM products 
WHERE MATCH(name, description) AGAINST('laptop' IN NATURAL LANGUAGE MODE);
-- Should use idx_search (FULLTEXT)
```

3. **Inventory Lookup**
```sql
SELECT * FROM inventory 
WHERE product_id = ? AND warehouse_id = ? AND location_id = ?;
-- Should use unique constraint uk_inventory
```

---

## Next Steps

### Immediate Actions Required
1. ✅ Update JPA entities to use String IDs (UUID)
2. ✅ Add UUID generation in @PrePersist methods
3. ✅ Update DTOs and request/response objects
4. ✅ Update repository queries that reference IDs
5. ✅ Run full test suite to verify changes

### Future Development
1. Implement remaining modules (Inventory, Orders, Shipments)
2. Add database views for common reporting queries
3. Implement soft delete mechanism for critical tables
4. Add database triggers for complex business rules
5. Set up database backup and recovery procedures

---

**Prepared By**: GitHub Copilot  
**Date**: 31/01/2026  
**Status**: Production Ready ✅
