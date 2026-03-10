# 📚 CORE Documentation

Thư mục này chứa toàn bộ tài liệu kỹ thuật cốt lõi của WMS Backend.

---

## 📄 Danh sách tài liệu

### 🔷 Main Documents

1. **[01_PROJECT_OVERVIEW.md](01_PROJECT_OVERVIEW.md)**
   - Tổng quan về dự án WMS
   - Mục tiêu và phạm vi
   - Actors và use cases

2. **[02_SYSTEM_ARCHITECTURE.md](02_SYSTEM_ARCHITECTURE.md)**
   - Kiến trúc tổng thể hệ thống
   - Tech stack và dependencies
   - Deployment architecture

3. **[03_API_DOCUMENTATION.md](03_API_DOCUMENTATION.md)**
   - Chi tiết tất cả API endpoints
   - Request/Response examples
   - Authentication & Authorization

4. **[04_DATABASE_SCHEMA.md](04_DATABASE_SCHEMA.md)** ⭐ **UPDATED**
   - Schema database đầy đủ
   - ERD diagrams
   - Table definitions với comments
   - Indexes và constraints
   - Sample data

5. **[05_IMPLEMENTATION_GUIDE.md](05_IMPLEMENTATION_GUIDE.md)**
   - Hướng dẫn implementation
   - Coding standards
   - Best practices

6. **[06_DEPLOYMENT_GUIDE.md](06_DEPLOYMENT_GUIDE.md)**
   - Hướng dẫn deploy
   - Environment setup
   - CI/CD configuration

---

### 🔷 Supplementary Documents

7. **[DATABASE_SCHEMA_CHANGELOG.md](DB/DATABASE_SCHEMA_CHANGELOG.md)** 🆕 **NEW**
   - Chi tiết tất cả thay đổi database
   - Migration history
   - Breaking changes
   - Testing recommendations
   - Performance tips

8. **[DATABASE_UPDATE_SUMMARY.md](DB/DATABASE_UPDATE_SUMMARY.md)** 🆕 **NEW**
   - Tóm tắt ngắn gọn về update database
   - Quick reference cho developers
   - Action items checklist

---

## 🔥 Quick Start - Đọc theo thứ tự

### Cho Developer mới tham gia:
```
1. 01_PROJECT_OVERVIEW.md      → Hiểu tổng quan
2. 02_SYSTEM_ARCHITECTURE.md   → Hiểu kiến trúc
3. 04_DATABASE_SCHEMA.md       → Hiểu database
4. DATABASE_UPDATE_SUMMARY.md  → Xem thay đổi mới nhất ⭐
5. 05_IMPLEMENTATION_GUIDE.md  → Bắt đầu code
```

### Cho Backend Developer đang làm:
```
1. DATABASE_UPDATE_SUMMARY.md  → ⚠️ ĐỌC ĐẦU TIÊN
2. DATABASE_SCHEMA_CHANGELOG.md → Chi tiết breaking changes
3. 04_DATABASE_SCHEMA.md       → Reference đầy đủ
4. 03_API_DOCUMENTATION.md     → API endpoints
```

### Cho Team Lead / Senior:
```
1. DATABASE_SCHEMA_CHANGELOG.md → Review thay đổi
2. 02_SYSTEM_ARCHITECTURE.md    → Kiến trúc tổng thể
3. 06_DEPLOYMENT_GUIDE.md       → Deploy strategy
```

---

## 🎯 Latest Update (31/01/2026)

### ⚡ Database Schema Version 3.0

**Major Changes:**
- 🔄 Changed from `BIGINT` to `UUID (CHAR 36)` for all primary keys
- ➕ Added `email_logs` table (V20260125_01)
- ➕ Added 6 Master Data tables (V20260129_01)
- ✅ Standardized audit fields
- 📊 Updated ERD diagram

**Action Required:**
- ⚠️ Update all Entity classes: `Long id` → `String id`
- ⚠️ Update all DTOs and Repositories
- ⚠️ Run full test suite
- ⚠️ Review breaking changes in `DATABASE_SCHEMA_CHANGELOG.md`

**Quick Links:**
- [Full Changelog](DB/DATABASE_SCHEMA_CHANGELOG.md)
- [Update Summary](DB/DATABASE_UPDATE_SUMMARY.md)
- [Complete Schema](04_DATABASE_SCHEMA.md)

---

## 📊 Database Tables Overview

### ✅ Implemented (13 tables)

**Auth & RBAC (Module 1):**
- accounts
- roles
- permissions
- account_roles
- role_permissions
- user_profiles

**Email Management:**
- email_logs

**Master Data (Module 2):**
- warehouses
- locations
- categories
- products
- units_of_measure
- business_partners

### ⏳ Coming Soon
- batches
- inventory
- purchase_orders
- sales_orders
- stock_movements
- ... (see full list in DATABASE_SCHEMA.md)

---

## 🔍 Search Guide

| Tôi cần... | Xem tài liệu... |
|-----------|----------------|
| Database schema mới nhất | `04_DATABASE_SCHEMA.md` |
| Thay đổi database gần đây | `DATABASE_UPDATE_SUMMARY.md` |
| Chi tiết breaking changes | `DATABASE_SCHEMA_CHANGELOG.md` |
| API endpoints | `03_API_DOCUMENTATION.md` |
| Kiến trúc hệ thống | `02_SYSTEM_ARCHITECTURE.md` |
| Hướng dẫn deploy | `06_DEPLOYMENT_GUIDE.md` |
| Coding standards | `05_IMPLEMENTATION_GUIDE.md` |

---

## 🛠️ Migration Files Location

```
src/main/resources/db/migration/
├── V20260107_01__Create_table_rbac.sql
├── V20260107_02__Insert_db.sql
├── V20260125_01__Create_email_logs_table.sql
└── V20260129_01__Create_module_2.sql
```

**Note:** Always backup database before running new migrations!

---

## ⚠️ Important Notes

### Breaking Changes Alert
Version 3.0 introduces UUID as primary keys. This requires:
1. Code changes in all entities
2. DTO updates
3. Repository method signature changes
4. Full regression testing

### Before You Code
1. ✅ Read `DATABASE_UPDATE_SUMMARY.md`
2. ✅ Check latest schema in `04_DATABASE_SCHEMA.md`
3. ✅ Review migration files
4. ✅ Run existing tests to ensure compatibility

---

## 📞 Need Help?

1. **Database Questions:** Check `04_DATABASE_SCHEMA.md` or `DATABASE_SCHEMA_CHANGELOG.md`
2. **API Questions:** Check `03_API_DOCUMENTATION.md`
3. **Architecture Questions:** Check `02_SYSTEM_ARCHITECTURE.md`
4. **Implementation Questions:** Check `05_IMPLEMENTATION_GUIDE.md`
5. **Still Stuck:** Contact senior developer or team lead

---

## 🔖 Document Version History

| Version | Date | Changes |
|---------|------|---------|
| 3.0 | 31/01/2026 | Updated database schema to UUID, added changelog |
| 2.0 | 29/01/2026 | Added Master Data module |
| 1.5 | 25/01/2026 | Added Email system |
| 1.0 | 07/01/2026 | Initial RBAC implementation |

---

## 📝 Contributing

When updating documentation:
1. Update the relevant document
2. Update this README if structure changes
3. Add entry to changelog if database changes
4. Notify team via Slack/email
5. Update version number and date

---

**Maintained By:** WHS Development Team  
**Last Updated:** 31/01/2026  
**Documentation Version:** 3.0
