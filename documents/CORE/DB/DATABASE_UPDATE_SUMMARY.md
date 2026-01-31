# 📝 Database Schema Update Summary

**Date:** 31 January 2026  
**Version:** 3.0  
**Status:** ✅ Completed

---

## 🎯 Mục đích

Cập nhật tài liệu database schema để phản ánh chính xác trạng thái hiện tại của database sau khi chạy tất cả migration files.

---

## 📦 Files đã cập nhật

### 1. **04_DATABASE_SCHEMA.md** (Updated)
**Location:** `src/documents/CORE/04_DATABASE_SCHEMA.md`

**Nội dung:**
- ✅ Cập nhật tất cả table definitions theo migration files
- ✅ Thay đổi primary key từ `BIGINT AUTO_INCREMENT` → `CHAR(36)` UUID
- ✅ Thêm bảng `email_logs` (V20260125_01)
- ✅ Thêm 6 bảng Master Data (V20260129_01)
- ✅ Cập nhật ERD diagram
- ✅ Cập nhật sample data với UUID format
- ✅ Thêm phần Database Schema Evolution
- ✅ Thêm phần Performance Considerations
- ✅ Thêm phần Data Integrity Rules

### 2. **DATABASE_SCHEMA_CHANGELOG.md** (New)
**Location:** `src/documents/CORE/DATABASE_SCHEMA_CHANGELOG.md`

**Nội dung:**
- 🆕 Tài liệu chi tiết về tất cả thay đổi
- 🆕 Migration history đầy đủ
- 🆕 Breaking changes và cách xử lý
- 🆕 Testing recommendations
- 🆕 Performance monitoring queries
- 🆕 Next steps cho development team

---

## 🔄 Thay đổi chính

### 1. Primary Key Strategy
**Before:**
```sql
id BIGINT PRIMARY KEY AUTO_INCREMENT
```

**After:**
```sql
id CHAR(36) PRIMARY KEY  -- UUID format
```

**Impact:**
- Tất cả entities phải dùng `String` thay vì `Long`
- Cần thêm UUID generation logic
- Foreign keys cũng đổi sang `CHAR(36)`

### 2. Tables Added

#### A. Email Management (V20260125_01)
```sql
email_logs
```
- Track tất cả email được gửi
- Hỗ trợ retry logic
- Scheduled emails
- Priority levels

#### B. Master Data Module (V20260129_01)
```sql
warehouses
locations
categories  
products
units_of_measure
business_partners
```

### 3. Standardized Audit Fields

Tất cả bảng đều có:
```sql
created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
created_by VARCHAR(50) or VARCHAR(36)
updated_by VARCHAR(50) or VARCHAR(36)
```

### 4. Foreign Key Policies

**CASCADE DELETE:**
- `account_roles`
- `role_permissions`
- `locations` (khi xóa warehouse)

**SET NULL:**
- `created_by`
- `updated_by`
- `manager_id`

---

## 📊 Current Database State

### ✅ Tables Implemented (13 tables)

**Module 1: Auth & RBAC**
1. accounts
2. roles
3. permissions
4. account_roles
5. role_permissions
6. user_profiles

**Module 2: Email**
7. email_logs

**Module 3: Master Data**
8. warehouses
9. locations
10. categories
11. products
12. units_of_measure
13. business_partners

### ⏳ Tables Pending (Future Modules)
- batches
- inventory
- inventory_adjustments
- purchase_orders
- purchase_order_lines
- inbound_receipts
- inbound_receipt_lines
- sales_orders
- sales_order_lines
- outbound_shipments
- outbound_shipment_lines
- stock_movements
- report_jobs
- import_jobs

---

## 🔧 Migration History

| Version | Date | Description | Status |
|---------|------|-------------|--------|
| V20260107_01 | 07/01/2026 | Create RBAC tables | ✅ Applied |
| V20260107_02 | 07/01/2026 | Insert default admin | ✅ Applied |
| V20260125_01 | 25/01/2026 | Create email_logs | ✅ Applied |
| V20260129_01 | 29/01/2026 | Create Master Data tables | ✅ Applied |

---

## ⚠️ Breaking Changes

### For Java/Spring Boot Code

**1. Entity Classes - Change ID type:**
```java
// Before
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;

// After
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

**2. Repository Methods:**
```java
// Before
Optional<Product> findById(Long id);

// After
Optional<Product> findById(String id);
```

**3. DTOs:**
```java
// Before
private Long categoryId;

// After
private String categoryId; // UUID
```

---

## ✅ Action Items for Developers

### Immediate (Must Do Now)
- [ ] Update all Entity classes với UUID IDs
- [ ] Update all Repository interfaces
- [ ] Update all DTOs/Request/Response objects
- [ ] Update service layer methods
- [ ] Update test cases
- [ ] Run full test suite

### Short Term (This Week)
- [ ] Review all foreign key references
- [ ] Update API documentation với UUID examples
- [ ] Add UUID validation trong request DTOs
- [ ] Update Postman collection
- [ ] Test migration on dev environment

### Medium Term (Next Sprint)
- [ ] Implement remaining modules (Inventory, Orders)
- [ ] Add database performance monitoring
- [ ] Create database backup strategy
- [ ] Document rollback procedures

---

## 📈 Performance Notes

### Indexes Created
- Primary keys: All `id` columns
- Unique indexes: `code`, `sku`, `username`, `email`
- Foreign key indexes: All FK columns
- Composite indexes: `(warehouse_id, code)`, etc.
- Full-text indexes: `products(name, description)`

### Query Optimization
```sql
-- Efficient product search
SELECT * FROM products 
WHERE category_id = ? AND status = 'ACTIVE'
-- Uses: idx_category_id, idx_status

-- Full-text product search
SELECT * FROM products 
WHERE MATCH(name, description) AGAINST('laptop')
-- Uses: idx_search (FULLTEXT)
```

---

## 🧪 Testing Checklist

### Database Level
- [ ] Verify all migrations run successfully
- [ ] Check all foreign key constraints work
- [ ] Test cascade delete behavior
- [ ] Test set null behavior
- [ ] Verify UUID generation works

### Application Level
- [ ] Test all CRUD operations
- [ ] Test entity relationships
- [ ] Verify audit fields populate correctly
- [ ] Test queries with UUID filters
- [ ] Load test với realistic data volume

---

## 📚 Documentation Links

- **Full Schema:** [04_DATABASE_SCHEMA.md](../04_DATABASE_SCHEMA.md)
- **Detailed Changelog:** [DATABASE_SCHEMA_CHANGELOG.md](DATABASE_SCHEMA_CHANGELOG.md)
- **Migration Files:** `src/main/resources/db/migration/`
- **API Docs:** [03_API_DOCUMENTATION.md](../03_API_DOCUMENTATION.md)

---

## 💡 Tips & Best Practices

### UUID Generation
```java
// In Entity
@PrePersist
public void generateId() {
    if (this.id == null) {
        this.id = UUID.randomUUID().toString();
    }
}
```

### Query by UUID
```java
// Repository
@Query("SELECT p FROM Product p WHERE p.id = :id")
Optional<Product> findById(@Param("id") String id);
```

### Validation
```java
// In DTO
@NotNull(message = "Product ID is required")
@Pattern(regexp = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$", 
         message = "Invalid UUID format")
private String productId;
```

---

## 🎓 Learning Resources

### UUID in JPA
- [Baeldung - UUID as Entity ID](https://www.baeldung.com/java-uuid)
- [Spring Data JPA with UUID](https://www.baeldung.com/spring-data-jpa-uuid)

### Database Design
- [MySQL UUID Best Practices](https://dev.mysql.com/doc/refman/8.0/en/miscellaneous-functions.html#function_uuid)
- [Flyway Migration Guide](https://flywaydb.org/documentation/)

---

## 📞 Support

Nếu gặp vấn đề:
1. Kiểm tra migration logs: `target/flyway/`
2. Review changelog: `DATABASE_SCHEMA_CHANGELOG.md`
3. Check error messages in console
4. Liên hệ senior developer hoặc team lead

---

## 📝 Summary

✅ **Completed:**
- Updated database schema documentation to match actual migrations
- Changed from auto-increment to UUID for all primary keys
- Added email_logs table for email tracking
- Added 6 Master Data tables (warehouses, locations, etc.)
- Standardized audit fields across all tables
- Updated ERD diagram
- Created comprehensive changelog document

⚠️ **Action Required:**
- Update all Java entities to use String IDs (UUID)
- Update DTOs and API responses
- Run full test suite
- Update API documentation

🎯 **Next Steps:**
- Implement remaining modules (Inventory, Orders)
- Add performance monitoring
- Create backup/restore procedures
- Document rollback strategy

---

**Prepared By:** GitHub Copilot  
**Reviewed By:** _Pending_  
**Approved By:** _Pending_  
**Date:** 31/01/2026
