# Tài liệu BA Review - Warehouse Management System (WMS)

## Mục lục
1. [Tổng quan](#1-tổng-quan)
2. [Stock Transfer & Stock Adjustment](#2-stock-transfer--stock-adjustment)
3. [Master Data](#3-master-data)
4. [Inbound Flow (Nhập kho)](#4-inbound-flow-nhập-kho)
5. [Inventory & StockMovements](#5-inventory--stockmovements)
6. [Tổng hợp Issues](#6-tổng-hợp-issues)

---

## 1. Tổng quan

### 1.1 Mục đích
Tài liệu này tổng hợp kết quả review nghiệp vụ của các flow chính trong hệ thống WMS Backend.

### 1.2 Phạm vi review
| Flow | Modules | Status |
|------|---------|--------|
| Stock Transfer & Adjustment | StockTransfers, StockAdjustments | ✅ Đã review |
| Master Data | WareHouse, Location, Product | ✅ Đã review |
| Inbound Flow | PurchaseOrders, InboundReceipts | ✅ Đã review |
| Inventory & Movements | Inventory, StockMovements | ✅ Đã review |
| Outbound Flow | SalesOrders, OutboundShipments, OutboundShipmentLines | ✅ Đã implement (25/03/2026) |

### 1.3 Vấn đề chung nhất
**Thiếu Warehouse Ownership Validation** - Tất cả các service đều không kiểm tra user có quyền với warehouse trước khi thao tác.

---

## 2. Stock Transfer & Stock Adjustment

### 2.1 Mô tả
- **Stock Transfer**: Chuyển hàng hóa từ vị trí này sang vị trí khác trong cùng warehouse
- **Stock Adjustment**: Điều chỉnh số lượng tồn kho thực tế

### 2.2 Các thực thể chính

#### StockTransfers Entity
| Trường | Mô tả |
|---------|--------|
| transferNumber | Số chuyển kho |
| productId | ID sản phẩm |
| warehouseId | ID kho |
| fromLocationId | Vị trí nguồn |
| toLocationId | Vị trí đích |
| batchId | ID lô (optional) |
| quantity | Số lượng chuyển |
| reason | Lý do chuyển |
| status | Trạng thái |
| completedAt | Thời điểm hoàn thành |

#### StockTransfersStatus
```
[DRAFT] --------> [COMPLETED]
   |
   v
[CANCELLED]
```

#### StockAdjustments Entity
| Trường | Mô tả |
|---------|--------|
| adjustmentNumber | Số điều chỉnh |
| inventoryId | ID inventory |
| quantityBefore | Số lượng trước |
| quantityAfter | Số lượng sau |
| adjustmentQuantity | Chênh lệch |
| reason | Lý do |
| status | Trạng thái |
| approvedBy | Người duyệt |
| approvedAt | Thời điểm duyệt |

#### StockAdjustmentsStatus
```
[PENDING_APPROVAL] ----> [APPROVED]
       |
       v
    [REJECTED]
```

### 2.3 Luồng xử lý

#### Stock Transfer - Complete
1. Kiểm tra status = DRAFT
2. Lock inventory theo thứ tự cố định (tránh deadlock)
3. Kiểm tra available >= quantity (available = onHand - quarantine - reserved)
4. Tạo inventory đích nếu chưa có
5. Cập nhật onHand: source trừ, dest cộng
6. Tạo 2 StockMovements: TRANSFER_OUT, TRANSFER_IN
7. Cập nhật status = COMPLETED

#### Stock Adjustment - Create
1. Lấy quantityBefore từ inventory
2. Tính adjustmentQuantity = quantityAfter - quantityBefore
3. Validate: quantityAfter >= 0, >= reserved, ≠ 0
4. Kiểm tra requiresApproval:
   - ADMIN: không cần
   - MANAGER: cần nếu THEFT/SYSTEM_ERROR hoặc delta >= 5
   - USER: luôn cần
5. Nếu auto-approve: cập nhật inventory + tạo StockMovement

### 2.4 Issues phát hiện

| # | Issue | Priority | Status |
|---|-------|----------|--------|
| 1 | Thiếu Warehouse Ownership Validation | 🔴 Highest | Chưa fix |
| 2 | Thiếu validation 2 Location cùng Warehouse | 🟠 High | Chưa fix |
| 3 | Chỉ ADMIN mới được approve (nên cho MANAGER) | 🟡 Medium | Chưa fix |
| 4 | Thiếu validation Location/Batch Status | 🟡 Medium | Chưa fix |
| 5 | Thiếu PENDING status cho Transfer | 🟢 Low | Chưa fix |
| 6 | Thiếu API Update/Void cho Adjustment | 🟢 Low | Chưa fix |
| 7 | Cần xác nhận Available Quantity logic | 🟡 Medium | Chưa fix |

---

## 3. Master Data

### 3.1 WareHouse Service

#### Validation hiện có ✅
- Kiểm tra warehouse status ACTIVE trước khi tạo location
- Kiểm tra active locations trước khi delete
- Kiểm tra inventory trước khi delete

#### Issues phát hiện

| # | Issue | Priority |
|---|-------|----------|
| 1 | Thiếu Warehouse Ownership Validation | 🟠 High |
| 2 | Delete chỉ set INACTIVE (soft delete) | 🟢 Low |

### 3.2 Location Service

#### Validation hiện có ✅
- Kiểm tra warehouse ACTIVE trước khi tạo
- Kiểm tra duplicate code
- Kiểm tra status transition

#### Issues phát hiện

| # | Issue | Priority |
|---|-------|----------|
| 1 | Thiếu Warehouse Ownership Validation | 🟠 High |
| 2 | Thiếu kiểm tra inventory khi set INACTIVE (TODO dòng 305-310) | 🟡 Medium |
| 3 | Cho phép update khi warehouse INACTIVE | 🟡 Medium |

### 3.3 Product Service

#### Validation hiện có ✅
- Kiểm tra category ACTIVE
- Kiểm tra UOM tồn tại
- Kiểm tra stock level constraints
- Kiểm tra batch tracking changes

#### Issues phát hiện

| # | Issue | Priority |
|---|-------|----------|
| 1 | Thiếu kiểm tra batch inventory khi disable batch tracking (TODO dòng 392-398) | 🟡 Medium |

---

## 4. Inbound Flow (Nhập kho)

### 4.1 Purchase Orders

#### Sơ đồ trạng thái
```
DRAFT -> CONFIRMED -> (PARTIALLY_RECEIVED) -> COMPLETED
  |                                            ^
  v                                            |
CANCELLED (chỉ từ DRAFT)
```

#### Validation hiện có ✅
- Validate supplier: tồn tại, ACTIVE, type = SUPPLIER/BOTH
- Validate warehouse: tồn tại, ACTIVE
- Validate PO lines: quantity > 0, received <= ordered
- Validate dates: deliveryDate >= orderDate
- Recalculate subtotal từ lines (không trust client)

#### Issues phát hiện

| # | Issue | Priority |
|---|-------|----------|
| 1 | Thiếu Warehouse Ownership Validation | 🟠 High |
| 2 | Thiếu REJECTED status | 🟢 Low |

### 4.2 Inbound Receipts

#### Sơ đồ trạng thái
```
DRAFT -> CONFIRMED
  |
  v
DELETED (chỉ từ DRAFT)
```

#### Validation hiện có ✅
- Chỉ tạo từ PO CONFIRMED hoặc PARTIALLY_RECEIVED
- Kiểm tra PO warehouse = receipt warehouse
- Kiểm tra product ACTIVE
- Kiểm tra location: thuộc warehouse, ACTIVE, không MAINTENANCE
- Kiểm tra batch: AVAILABLE (trừ quarantine)
- Kiểm tra quarantine lines có notes
- Lock order để tránh race condition
- Tự động cập nhật PO status sau khi confirm

#### Issues phát hiện

| # | Issue | Priority |
|---|-------|----------|
| 1 | Thiếu Warehouse Ownership Validation | 🟠 High |
| 2 | Thiếu validation Location Type | 🟡 Medium |
| 3 | Thiếu REJECTED status | 🟢 Low |

---

## 5. Inventory & StockMovements

### 5.1 Inventory Service

#### Chức năng chính
- `getInventories`: Lấy danh sách inventory với filter
- `getSummaryByProduct`: Tổng hợp theo sản phẩm
- `getInventoryByLocation`: Group by location
- `checkAvailability`: Kiểm tra đủ hàng
- `reserve`: Đặt trước inventory
- `unreserve`: Hủy đặt trước
- `increase`: Tăng tồn kho thủ công

#### Validation hiện có ✅
- Kiểm tra duplicate reference (idempotency)
- Kiểm tra dimensions (product, warehouse, location, batch)
- Kiểm tra quantity > 0
- Lock inventory khi update
- Check available trước khi reserve

#### Issues phát hiện

| # | Issue | Priority |
|---|-------|----------|
| 1 | Thiếu Warehouse Ownership Validation | 🟠 High |
| 2 | increase() không kiểm tra Warehouse/Location/Batch status | 🟡 Medium |

### 5.2 StockMovements Service

#### Chức năng chính
- `getById`: Lấy chi tiết movement
- `getAll`: Danh sách movements
- `getByReference`: Lấy theo reference (PO, SO, etc.)
- `recordMovement`: Ghi nhận movement
- `existsByReference`: Kiểm tra trùng lặp

---

## 6. Tổng hợp Issues

### 6.1 Tất cả Issues theo Priority

#### 🔴 Critical / Highest
| # | Jira | GitHub | Flow | Issue |
|---|------|--------|------|-------|
| 1 | WHS-174 | #103 | Stock Transfer/Adjustment | Thiếu Warehouse Ownership Validation |

#### 🟠 High
| # | Jira | GitHub | Flow | Issue |
|---|------|--------|------|-------|
| 1 | WHS-175 | #104 | Stock Transfer | Thiếu validation 2 Location cùng Warehouse |
| 2 | WHS-181 | #110 | Master Data | Thiếu Warehouse Ownership Validation |
| 3 | WHS-185 | #114 | Inbound Flow | Thiếu Warehouse Ownership Validation |
| 4 | WHS-189 | #118 | Inventory | Thiếu Warehouse Ownership Validation |

#### 🟡 Medium
| # | Jira | GitHub | Flow | Issue |
|---|------|--------|------|-------|
| 1 | WHS-176 | #105 | Stock Adjustment | Chỉ ADMIN approve, nên cho MANAGER |
| 2 | WHS-177 | #106 | Stock Transfer | Thiếu validation Location/Batch Status |
| 3 | WHS-178 | #107 | Stock Transfer | Thêm PENDING status |
| 4 | WHS-180 | #109 | Inventory | Xác nhận Available Quantity logic |
| 5 | WHS-182 | #111 | Master Data | Thiếu kiểm tra Inventory khi set Location INACTIVE |
| 6 | WHS-183 | #112 | Master Data | Thiếu kiểm tra Batch khi disable batch tracking |
| 7 | WHS-184 | #113 | Master Data | Cho phép update Location khi Warehouse INACTIVE |
| 8 | WHS-186 | #115 | Inbound Receipt | Thiếu validation Location Type |
| 9 | WHS-190 | #119 | Inventory | increase() không kiểm tra Warehouse/Location/Batch status |

#### 🟢 Low
| # | Jira | GitHub | Flow | Issue |
|---|------|--------|------|-------|
| 1 | WHS-178 | #107 | Stock Transfer | Thêm PENDING status |
| 2 | WHS-179 | #108 | Stock Adjustment | Thêm API Update/Void |
| 3 | WHS-187 | #116 | Purchase Orders | Thêm REJECTED status |
| 4 | WHS-188 | #117 | Inbound Receipts | Thêm REJECTED status |

### 6.2 Thống kê theo Flow

| Flow | Số Issues | Critical | High | Medium | Low |
|------|-----------|----------|------|---------|-----|
| Stock Transfer & Adjustment | 7 | 1 | 1 | 3 | 2 |
| Master Data | 4 | 0 | 1 | 3 | 0 |
| Inbound Flow | 4 | 0 | 1 | 1 | 2 |
| Inventory & StockMovements | 2 | 0 | 1 | 1 | 0 |
| **Tổng** | **17** | **1** | **4** | **8** | **4** |

### 6.3 Issues đã link Jira ↔ GitHub

| Jira | GitHub | Tiêu đề |
|------|--------|----------|
| WHS-174 | #103 | Thiếu Warehouse Ownership Validation (Stock) |
| WHS-175 | #104 | Thiếu validation 2 Location cùng Warehouse |
| WHS-176 | #105 | Cho phép MANAGER approve |
| WHS-177 | #106 | Thiếu validation Location/Batch Status |
| WHS-178 | #107 | Thêm PENDING status cho Transfer |
| WHS-179 | #108 | Thêm API Update/Void cho Adjustment |
| WHS-180 | #109 | Xác nhận Available Quantity logic |
| WHS-181 | #110 | Thiếu Warehouse Ownership Validation (Master) |
| WHS-182 | #111 | Thiếu kiểm tra Inventory khi set Location INACTIVE |
| WHS-183 | #112 | Thiếu kiểm tra Batch khi disable tracking |
| WHS-184 | #113 | Cho phép update Location khi Warehouse INACTIVE |
| WHS-185 | #114 | Thiếu Warehouse Ownership Validation (Inbound) |
| WHS-186 | #115 | Thiếu validation Location Type |
| WHS-187 | #116 | Thêm REJECTED cho Purchase Orders |
| WHS-188 | #117 | Thêm REJECTED cho Inbound Receipts |
| WHS-189 | #118 | Thiếu Warehouse Ownership Validation (Inventory) |
| WHS-190 | #119 | increase() không kiểm tra Warehouse/Location/Batch status |

---

## Phụ lục

### A. Validation tốt cần giữ lại

| Module | Validation | Ghi chú |
|--------|------------|----------|
| Stock Transfer | Lock inventory theo order | Tránh deadlock |
| Stock Transfer | Kiểm tra available = onHand - quarantine - reserved | Chính xác |
| Stock Adjustment | Verify quantityBefore unchanged khi approve | Quan trọng |
| Inbound Receipt | Lock PO khi confirm | Tránh race condition |
| Inbound Receipt | Auto-recalculate PO status | Không trust client |
| Inventory | Idempotency check | Tránh duplicate |
| Inventory | Lock khi update | Data integrity |

### B. Khuyến nghị ưu tiên fix

1. **Ưu tiên 1**: Warehouse Ownership Validation (4 flow)
2. **Ưu tiên 2**: Status validations (Location, Batch, Warehouse)
3. **Ưu tiên 3**: Thêm REJECTED status cho PO và Receipt
4. **Ưu tiên 4**: Cho phép MANAGER approve

### C. Những gì đã làm tốt

- Sử dụng pessimistic lock cho inventory updates
- Recalculate totals từ lines (không trust client)
- Idempotency checks
- Race condition handling
- Comprehensive validation
- Audit trail với StockMovements

---

*Ngày tạo: 2026-03-16*
*Người review: BA WHS*
