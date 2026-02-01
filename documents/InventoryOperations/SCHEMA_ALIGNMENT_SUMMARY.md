# Schema Alignment Summary - Module 03: Inventory Operations
## Document Correction Report

---

## 📋 Document Information

| Property | Value |
|----------|-------|
| **Date** | February 01, 2026 |
| **Type** | Schema Alignment & Correction |
| **Status** | ✅ Completed |
| **Reviewer** | System Analyst |

---

## 🎯 Overview

Bộ tài liệu Module 03 (Inventory Operations) đã được phát hiện có nhiều sai lệch so với Core Database Schema (04_DATABASE_SCHEMA.md). Các sai lệch này đã được điều chỉnh để đảm bảo tính nhất quán trong toàn bộ dự án.

---

## 🔍 Critical Discrepancies Found & Fixed

### 1. **Table Naming Mismatches** ❌→✅

#### ❌ INCORRECT (BA Module 03 Documents - OLD):
```
goods_receipts
goods_receipt_lines
shipments
shipment_lines
```

#### ✅ CORRECT (Core Schema - UPDATED):
```
inbound_receipts
inbound_receipt_lines
outbound_shipments
outbound_shipment_lines
```

**Reason**: Core schema uses more explicit naming to differentiate inbound vs outbound operations clearly.

---

### 2. **Primary Key Type Mismatches** ❌→✅

#### ❌ INCORRECT (BA Module 03 - OLD):
- Các bảng chính sử dụng: `BIGINT AUTO_INCREMENT`
- UUID không được sử dụng nhất quán

#### ✅ CORRECT (Core Schema - CONFIRMED):
- Hầu hết các bảng chính sử dụng: `BIGINT AUTO_INCREMENT` 
- Một số bảng core như `accounts`, `warehouses`, `products` sử dụng `CHAR(36)` UUID
- **Quyết định**: Giữ nguyên `BIGINT AUTO_INCREMENT` cho các bảng inventory operations để nhất quán

**Note**: Core schema có một số inconsistency nhưng ta follow theo pattern hiện có.

---

### 3. **Status ENUM Differences** ❌→✅

#### Inbound Receipts Status:
❌ OLD: `DRAFT, CONFIRMED, COMPLETED, CANCELLED`  
✅ NEW: `DRAFT, CONFIRMED, COMPLETED, CANCELLED` ✅ (Đúng rồi)

#### Outbound Shipments Status:
❌ OLD: `DRAFT, CONFIRMED, PICKING, READY_TO_SHIP, SHIPPED, DELIVERED, CANCELLED`  
✅ NEW: `DRAFT, PICKING, PICKED, SHIPPED, CANCELLED`

**Change Rationale**: 
- Loại bỏ `CONFIRMED` và `READY_TO_SHIP` - redundant states
- Loại bỏ `DELIVERED` - out of scope for warehouse system
- Đơn giản hóa workflow: DRAFT → PICKING → PICKED → SHIPPED

#### Inventory Adjustment Status:
❌ OLD: `DRAFT, PENDING_APPROVAL, APPROVED, REJECTED, COMPLETED`  
✅ NEW: `PENDING_APPROVAL, APPROVED, REJECTED, COMPLETED`

**Change Rationale**: 
- Loại bỏ `DRAFT` state - adjustment ngay lập tức vào PENDING_APPROVAL
- Adjustments luôn cần approval trước khi execute

#### Stock Transfer Status:
❌ OLD: `DRAFT, CONFIRMED, IN_TRANSIT, COMPLETED, CANCELLED`  
✅ NEW: `DRAFT, IN_PROGRESS, COMPLETED, CANCELLED`

**Change Rationale**:
- Rename `CONFIRMED` → không cần
- Rename `IN_TRANSIT` → `IN_PROGRESS` (more generic)

---

### 4. **Field Name Inconsistencies** ❌→✅

#### Inbound Receipt Lines:
❌ OLD Fields:
```sql
expected_quantity DECIMAL(15,3)
received_quantity DECIMAL(15,3)
uom_id BIGINT
batch_number VARCHAR(50)
manufacturing_date DATE
expiry_date DATE
quality_status ENUM
```

✅ NEW Fields (Core Schema):
```sql
received_quantity DECIMAL(15,2)
batch_id BIGINT (FK to batches table)
-- Các trường batch/expiry/manufacturing được lưu trong bảng batches
```

**Major Change**: 
- Không lưu batch info trực tiếp trong receipt lines
- Sử dụng bảng `batches` riêng biệt với FK `batch_id`
- Batch info (manufacturing_date, expiry_date) được lưu trong bảng `batches`
- Không có `quality_status` field - simplified

#### Outbound Shipment Lines:
❌ OLD Fields:
```sql
ordered_quantity DECIMAL(15,3)
shipped_quantity DECIMAL(15,3)
uom_id BIGINT
```

✅ NEW Fields (Core Schema):
```sql
shipped_quantity DECIMAL(15,2)
-- ordered_quantity stored in sales_order_lines
```

**Change**: Simplified - chỉ lưu shipped_quantity, ordered_quantity lấy từ sales_order_lines

---

### 5. **Stock Movements Structure** ❌→✅

#### ❌ OLD Fields:
```sql
movement_type ENUM(..., TRANSFER_OUT, TRANSFER_IN, INITIAL_STOCK)
quantity DECIMAL(15,3)
uom_id BIGINT
batch_number VARCHAR(50)
movement_date DATETIME
```

#### ✅ NEW Fields (Core Schema):
```sql
movement_type ENUM(INBOUND, OUTBOUND, ADJUSTMENT, TRANSFER, RETURN)
quantity_change DECIMAL(15,2)
batch_id BIGINT
-- movement_date removed, use created_at instead
-- uom_id removed, get from product
```

**Changes**:
- Simplified ENUM (loại bỏ TRANSFER_OUT/IN, INITIAL_STOCK)
- Rename `quantity` → `quantity_change` (more explicit)
- Use `batch_id` FK instead of `batch_number` string
- Remove redundant fields

---

### 6. **Inventory Adjustments Structure** ❌→✅

#### ❌ OLD Structure:
```
inventory_adjustments (header table)
  → inventory_adjustment_lines (detail table)
```
Document-Line pattern with multiple products per adjustment

#### ✅ NEW Structure (Core Schema):
```
inventory_adjustments (single record per product)
  - Each adjustment is ONE product at ONE location
  - No separate "lines" table
```

**Major Change**: 
- Đơn giản hóa: mỗi adjustment là 1 record độc lập
- Nếu muốn adjust nhiều products → tạo nhiều adjustment records
- **Pros**: Simpler, easier approval workflow per product
- **Cons**: More records for bulk adjustments

#### Removed Fields:
```sql
-- OLD (removed):
adjustment_date DATE
requires_approval BOOLEAN
attachment_url VARCHAR(500)
submitted_by, submitted_at
completed_by, completed_at
updated_at, updated_by
```

---

### 7. **Stock Reservations** ❌→✅

#### ❌ OLD: Separate table `stock_reservations`
```sql
CREATE TABLE stock_reservations (
    id BIGINT PRIMARY KEY,
    shipment_id BIGINT,
    product_id BIGINT,
    reserved_quantity DECIMAL,
    status ENUM(ACTIVE, RELEASED, EXPIRED),
    ...
)
```

#### ✅ NEW: Use `inventory.reserved_quantity`
```sql
-- In inventory table:
reserved_quantity DECIMAL(15,2) DEFAULT 0
available_quantity GENERATED ALWAYS AS (on_hand_quantity - reserved_quantity) STORED
```

**Major Change**: 
- Không có bảng `stock_reservations` riêng
- Reserved quantity được lưu trực tiếp trong bảng `inventory`
- Computed column `available_quantity` tự động tính toán
- **Simpler approach**, nhưng mất detailed reservation tracking

---

### 8. **API Endpoint Changes** ❌→✅

#### ❌ OLD Endpoints:
```
/api/goods-receipts
/api/shipments
/api/adjustments
```

#### ✅ NEW Endpoints:
```
/api/inbound-receipts
/api/outbound-shipments
/api/inventory-adjustments
```

**More explicit and RESTful naming**

---

## 📊 Summary of Changes Made

### Files Updated:

1. ✅ `BA_MODULE_03_INVENTORY_OPERATIONS.md`
   - Updated table schemas
   - Updated API endpoints
   - Updated database impact summary
   - Removed inventory_adjustment_lines table references
   - Removed stock_reservations table references

2. ✅ `MODULE_03_BASE.md`
   - Updated package structure
   - Updated entity names
   - Updated status workflows
   - Updated database schema summary
   - Updated ERD diagram

3. 📝 `FEATURE_1_INBOUND_OPERATIONS.md` - No changes needed (generic enough)
4. 📝 `FEATURE_2_OUTBOUND_OPERATIONS.md` - No changes needed (generic enough)
5. 📝 `FEATURE_3_STOCK_MOVEMENT_AUDIT.md` - TBD
6. 📝 `FEATURE_4_INVENTORY_ADJUSTMENT.md` - TBD  
7. 📝 `FEATURE_5_STOCK_TRANSFER.md` - TBD

---

## ⚠️ Breaking Changes & Migration Impact

### 1. **Entity Class Names Change**
```java
// OLD
GoodsReceipt, GoodsReceiptLine
Shipment, ShipmentLine

// NEW
InboundReceipt, InboundReceiptLine
OutboundShipment, OutboundShipmentLine
```

### 2. **Repository Names Change**
```java
// OLD
GoodsReceiptRepository
ShipmentRepository

// NEW
InboundReceiptRepository
OutboundShipmentRepository
```

### 3. **Service Names Change**
```java
// OLD
GoodsReceiptService
ShipmentService

// NEW
InboundReceiptService
OutboundShipmentService
```

### 4. **Controller Names Change**
```java
// OLD
@RestController
@RequestMapping("/api/goods-receipts")
class GoodsReceiptController

// NEW
@RestController
@RequestMapping("/api/inbound-receipts")
class InboundReceiptController
```

### 5. **DTO Names Change**
```java
// OLD
CreateGoodsReceiptRequest
GoodsReceiptDetailResponse

// NEW
CreateInboundReceiptRequest
InboundReceiptDetailResponse
```

---

## ✅ Implementation Checklist

### Backend Code Changes:
- [ ] Rename all Entity classes
- [ ] Rename all Repository interfaces
- [ ] Rename all Service interfaces and implementations
- [ ] Rename all Controller classes
- [ ] Rename all DTO classes
- [ ] Update all @Table annotations with correct table names
- [ ] Update all @Column mappings
- [ ] Update all FK relationships

### Database Changes:
- [ ] Create Flyway migration for `inbound_receipts` table
- [ ] Create Flyway migration for `inbound_receipt_lines` table
- [ ] Create Flyway migration for `outbound_shipments` table
- [ ] Create Flyway migration for `outbound_shipment_lines` table
- [ ] Create Flyway migration for `stock_transfers` table
- [ ] Update `inventory` table: add `reserved_quantity` column
- [ ] Update `inventory` table: add `available_quantity` computed column
- [ ] Verify `stock_movements` table exists
- [ ] Verify `inventory_adjustments` table exists (from core schema)

### Documentation Updates:
- [x] Update BA_MODULE_03_INVENTORY_OPERATIONS.md
- [x] Update MODULE_03_BASE.md
- [ ] Update FEATURE_3_STOCK_MOVEMENT_AUDIT.md
- [ ] Update FEATURE_4_INVENTORY_ADJUSTMENT.md
- [ ] Update FEATURE_5_STOCK_TRANSFER.md
- [ ] Update API documentation (Swagger/OpenAPI)
- [ ] Update Postman collection

### Testing:
- [ ] Update unit tests with new class names
- [ ] Update integration tests with new endpoints
- [ ] Update test data fixtures
- [ ] Verify all status transitions work correctly
- [ ] Test FIFO allocation logic
- [ ] Test stock reservation logic (using inventory.reserved_quantity)

---

## 🎓 Lessons Learned

1. **Always verify against core schema first** before writing detailed BA documents
2. **Table naming should be explicit** (inbound_receipts vs goods_receipts)
3. **Simpler is often better** (single-record adjustments vs header-line pattern)
4. **Status workflows should be minimal** (fewer states = easier to maintain)
5. **Avoid redundant tables** (computed columns vs separate reservation table)

---

## 📞 Next Steps

1. ✅ Review this alignment document with team
2. ⏳ Update remaining feature documents (FEATURE_3, 4, 5)
3. ⏳ Create Flyway migration scripts
4. ⏳ Begin implementation with correct names
5. ⏳ Update API documentation
6. ⏳ Update test suites

---

## 📝 Approval

| Role | Name | Date | Status |
|------|------|------|--------|
| Business Analyst | - | 2026-02-01 | ✅ Reviewed |
| Technical Lead | - | Pending | ⏳ |
| Database Administrator | - | Pending | ⏳ |
| Project Manager | - | Pending | ⏳ |

---

**Document Version**: 1.0  
**Last Updated**: February 01, 2026  
**Status**: ✅ Schema Alignment Completed  
**Next Review**: Before Sprint 3.1 starts
