# Tài liệu Nghiệp vụ Stock Transfer & Stock Adjustment

## Mục lục
1. [Tổng quan](#1-tổng-quan)
2. [Stock Transfer (Chuyển kho)](#2-stock-transfer-chuyển-kho)
3. [Stock Adjustment (Điều chỉnh tồn kho)](#3-stock-adjustment-điều-chỉnh-tồn-kho)
4. [Các vấn đề phát hiện](#4-các-vấn-đề-phát-hiện)
5. [Khuyến nghị](#5-khuyến-nghị)

---

## 1. Tổng quan

### 1.1 Mục đích tài liệu
Tài liệu này mô tả chi tiết luồng nghiệp vụ của 2 module:
- **Stock Transfer**: Chuyển hàng hóa từ vị trí này sang vị trí khác trong kho
- **Stock Adjustment**: Điều chỉnh số lượng tồn kho thực tế

### 1.2 Các thực thể liên quan

| Thực thể | Mô tả |
|-----------|-------|
| `Inventory` | Bản ghi tồn kho theo (product, warehouse, location, batch) |
| `StockTransfers` | Lưu thông tin chuyển kho |
| `StockAdjustments` | Lưu thông tin điều chỉnh tồn kho |
| `StockMovements` | Lịch sử biến động tồn kho |
| `Locations` | Vị trí lưu trữ trong kho |
| `Products` | Sản phẩm |
| `Batches` | Lô hàng |

### 1.3 Cấu trúc Inventory

```java
Inventory {
    productId          // ID sản phẩm
    warehouseId        // ID kho
    locationId         // ID vị trí (có thể null)
    batchId            // ID lô (có thể null)
    onHandQuantity     // Số lượng tồn thực tế
    quarantineQuantity // Số lượng cách ly
    reservedQuantity   // Số lượng đã đặt trước
    // Available = onHand - quarantine - reserved
}
```

---

## 2. Stock Transfer (Chuyển kho)

### 2.1 Mô tả nghiệp vụ
Chuyển hàng hóa từ vị trí nguồn (fromLocation) sang vị trí đích (toLocation) trong cùng một warehouse.

### 2.2 Sơ đồ trạng thái

```
[DRAFT] --------> [COMPLETED]     (Complete Transfer)
   |
   v
[CANCELLED]                     (Cancel Transfer)
```

### 2.3 Các trạng thái

| Trạng thái | Mô tả |
|-------------|-------|
| `DRAFT` | Bản nháp, chờ hoàn thành |
| `COMPLETED` | Đã hoàn thành chuyển kho |
| `CANCELLED` | Đã hủy |

### 2.4 Lý do chuyển kho (StockTransfersReason)

| Mã | Mô tả |
|-----|-------|
| `REORG` | Tổ chức lại kho |
| `PICKING_PREP` | Chuẩn bị lấy hàng |
| `OVERFLOW` | Chuyển sang vị trí tràn |
| `CONSOLIDATION` | Hợp nhất hàng |
| `OTHER` | Lý do khác |

### 2.5 API Endpoints

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| POST | `/api/v1/stock-transfers` | Tạo mới transfer (trạng thái DRAFT) |
| GET | `/api/v1/stock-transfers/{id}` | Lấy chi tiết transfer |
| GET | `/api/v1/stock-transfers` | Danh sách transfer (phân trang) |
| PUT | `/api/v1/stock-transfers/{id}/complete` | Hoàn thành transfer |
| PUT | `/api/v1/stock-transfers/{id}/cancel` | Hủy transfer |

### 2.6 Luồng xử lý chi tiết

#### 2.6.1 Tạo mới Transfer (POST /stock-transfers)

**Input:**
```json
{
  "product_id": "uuid",
  "warehouse_id": "uuid",
  "from_location_id": "uuid",
  "to_location_id": "uuid",
  "batch_id": "uuid (optional)",
  "quantity": 10.00,
  "reason": "REORG",
  "notes": "Ghi chú..."
}
```

**Validation:**
1. ✅ Product tồn tại
2. ✅ From Location tồn tại
3. ✅ To Location tồn tại
4. ✅ From Location và To Location khác nhau
5. ✅ From Location và To Location cùng thuộc warehouseId
6. ✅ Batch tồn tại (nếu có) và thuộc đúng product
7. ✅ Quantity > 0
8. ✅ Reason không null

**Xử lý:**
- Tạo `StockTransfers` với status = `DRAFT`
- Sinh transferNumber tự động (prefix: TRF)
- Lưu vào database

**Output:**
```json
{
  "id": "uuid",
  "transfer_number": "TRF-000001",
  "status": "DRAFT",
  ...
}
```

#### 2.6.2 Hoàn thành Transfer (PUT /stock-transfers/{id}/complete)

**Điều kiện:**
- Chỉ thực hiện khi status = `DRAFT`

**Xử lý (trong transaction):**

1. **Lock inventory theo thứ tự cố định** (tránh deadlock)
   - So sánh inventory key để xác định lock order
   
2. **Kiểm tra tồn kho nguồn**
   - Tìm inventory theo (product, warehouse, fromLocation, batch)
   - Kiểm tra tồn tại
   - Kiểm tra `available >= quantity`
     - `available = onHand - quarantine - reserved`

3. **Xử lý tồn kho đích**
   - Nếu chưa có inventory đích → tạo mới với onHand = 0
   - Nếu đã có → cập nhật

4. **Cập nhật số lượng**
   ```
   Source:
   - onHandBefore = onHand hiện tại
   - onHandAfter = onHandBefore - quantity
   
   Destination:
   - onHandBefore = onHand hiện tại  
   - onHandAfter = onHandBefore + quantity
   ```

5. **Tạo StockMovements**
   - TRANSFER_OUT: quantity âm (-10), sourceBefore → sourceAfter
   - TRANSFER_IN: quantity dương (+10), destBefore → destAfter

6. **Cập nhật trạng thái**
   - status = `COMPLETED`
   - completedAt = thời điểm hiện tại

#### 2.6.3 Hủy Transfer (PUT /stock-transfers/{id}/cancel)

**Điều kiện:**
- Chỉ thực hiện khi status = `DRAFT`

**Xử lý:**
- status = `CANCELLED`
- Không ảnh hưởng đến inventory

---

## 3. Stock Adjustment (Điều chỉnh tồn kho)

### 3.1 Mô tả nghiệp vụ
Điều chỉnh số lượng tồn kho thực tế so với sổ sách. Thường dùng khi:
- Kiểm kê phát hiện sai số
- Hàng hỏng, mất mát
- Lỗi hệ thống

### 3.2 Sơ đồ trạng thái

```
[PENDING_APPROVAL] ----> [APPROVED]      (Approve)
       |
       v
    [REJECTED]                        (Reject)
```

### 3.3 Các trạng thái

| Trạng thái | Mô tả |
|-------------|-------|
| `PENDING_APPROVAL` | Chờ duyệt |
| `APPROVED` | Đã duyệt - đã áp dụng vào tồn kho |
| `REJECTED` | Đã từ chối |

### 3.4 Lý do điều chỉnh (ReasonType)

| Mã | Mô tả |
|-----|-------|
| `DAMAGE` | Hàng hỏng |
| `THEFT` | Mất cắp |
| `COUNT_ERROR` | Lỗi đếm/kiểm kê |
| `EXPIRED` | Hết hạn |
| `QUALITY_ISSUE` | Vấn đề chất lượng |
| `SYSTEM_ERROR` | Lỗi hệ thống |
| `OTHER` | Lý do khác |

### 3.5 API Endpoints

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| POST | `/api/v1/stock-adjustments` | Tạo mới adjustment |
| GET | `/api/v1/stock-adjustments/{id}` | Lấy chi tiết |
| GET | `/api/v1/stock-adjustments` | Danh sách (có filter, phân trang) |
| PUT | `/api/v1/stock-adjustments/{id}/approve` | Duyệt adjustment |
| PUT | `/api/v1/stock-adjustments/{id}/reject` | Từ chối adjustment |

### 3.6 Luồng xử lý chi tiết

#### 3.6.1 Tạo mới Adjustment (POST /stock-adjustments)

**Input:**
```json
{
  "inventory_id": "uuid",
  "quantity_after": 80.00,
  "reason": "COUNT_ERROR",
  "notes": "Kiểm kê phát hiện thiếu 20 sản phẩm"
}
```

**Validation:**
1. ✅ Inventory tồn tại
2. ✅ quantity_after không null, >= 0
3. ✅ quantity_after không quá 13 chữ số + 2 decimal
4. ✅ quantity_after >= reserved_quantity (không thể điều chỉnh xuống dưới số đã đặt)
5. ✅ adjustment_quantity ≠ 0 (phải có thay đổi)
6. ✅ Reason không null

**Xử lý:**

1. **Lấy thông tin inventory hiện tại**
   - quantityBefore = inventory.onHandQuantity
   - quantityAfter = request.quantityAfter
   - adjustmentQuantity = quantityAfter - quantityBefore

2. **Kiểm tra yêu cầu duyệt**
   ```java
   requiresApproval = 
       // ADMIN: không cần duyệt
       (role != ADMIN) 
       // MANAGER: cần duyệt nếu lý do nhạy cảm HOẶC delta >= 5
       && (reason == THEFT || reason == SYSTEM_ERROR || |adjustment| >= 5)
       // USER: luôn cần duyệt
   ```

3. **Tạo StockAdjustments**
   - status = `PENDING_APPROVAL` HOẶC `APPROVED` (tùy requiresApproval)
   - Sinh adjustmentNumber tự động (prefix: ADJ)

4. **Nếu không cần duyệt (auto-approve)**
   - Cập nhật inventory.onHandQuantity = quantityAfter
   - Tạo StockMovement (ADJUSTMENT_INCREASE hoặc ADJUSTMENT_DECREASE)

---

#### 3.6.2 Duyệt Adjustment (PUT /stock-adjustments/{id}/approve)

**Điều kiện:**
- status = `PENDING_APPROVAL`
- User có role `ADMIN`

**Validation:**
1. ✅ Adjustment tồn tại và đang ở trạng thái PENDING_APPROVAL
2. ✅ Inventory chưa thay đổi so với lúc tạo (quantityBefore khớp)
3. ✅ quantityAfter >= 0 và >= reservedQuantity
4. ✅ adjustmentQuantity ≠ 0

**Xử lý:**

1. **Lock inventory** với pessimistic lock
2. **Verify quantityBefore không đổi**
3. **Cập nhật inventory**
   - inventory.onHandQuantity = quantityAfter
4. **Tạo StockMovement**
   - Type: ADJUSTMENT_INCREASE hoặc ADJUSTMENT_DECREASE
5. **Cập nhật adjustment**
   - status = `APPROVED`
   - approvedBy = user hiện tại
   - approvedAt = thời điểm hiện tại

---

#### 3.6.3 Từ chối Adjustment (PUT /stock-adjustments/{id}/reject)

**Điều kiện:**
- status = `PENDING_APPROVAL`
- User có role `ADMIN`
- Phải cung cấp rejectionReason

**Xử lý:**

1. **Kiểm tra trạng thái**
2. **Cập nhật adjustment**
   - status = `REJECTED`
   - rejectionReason = lý do từ chối
   - approvedBy = user hiện tại
   - approvedAt = thời điểm hiện tại

---

## 4. Các vấn đề phát hiện

### 4.1 Vấn đề nghiêm trọng (Cần fix ngay)

#### 🔴 Issue #1: Thiếu Warehouse Ownership Validation

**Mô tả:**
Cả StockTransfer và StockAdjustment đều không kiểm tra:
- User hiện tại có quyền truy cập warehouse của tài nguyên không
- Tài nguyên (Inventory, Location) có thuộc warehouse đúng không

**Rủi ro:**
- User có thể thao tác inventory của warehouse khác
- Vi phạm nguyên tắc phân quyền theo warehouse

**Vị trí:**
- `StockTransfersServiceImpl.java` - `validateTransferRequest()`
- `StockAdjustmentsServiceImpl.java` - `createAdjustment()`

---

#### 🔴 Issue #2: Stock Transfer - Thiếu validation 2 location cùng warehouse

**Mô tả:**
Hiện tại code kiểm tra từng location thuộc warehouseId được cung cấp, nhưng **không kiểm tra rõ ràng** 2 location cùng thuộc một warehouse.

**Code hiện tại (dòng 252-255):**
```java
if (!fromLocation.getWarehouseId().equals(request.getWarehouseId())
        || !toLocation.getWarehouseId().equals(request.getWarehouseId())) {
    throw new BadRequestException("Transfer locations must belong to the provided warehouse", ErrorCode.STF_002);
}
```

**Vấn đề:**
- Nếu nghiệp vụ yêu cầu chỉ chuyển **trong cùng warehouse** → cần validate rõ hơn
- Nếu cho phép cross-warehouse → cần xem xét thêm logic

---

### 4.2 Vấn đề quan trọng (Nên fix)

#### ⚠️ Issue #3: Stock Adjustment - Chỉ ADMIN mới được duyệt

**Mô tả:**
Hiện tại chỉ role ADMIN mới có quyền approve/reject. Theo nghiệp vụ thông thường, MANAGER cũng nên có quyền này.

**Code hiện tại (dòng 436-442):**
```java
private void assertCanApproveReject(List<String> roles) {
    if (!hasRole(roles, RoleType.ADMIN)) {
        throw new BadRequestException("You do not have permission to approve/reject adjustments", ErrorCode.AUTH_002);
    }
}
```

**Đề xuất:**
- Cho phép ADMIN và MANAGER approve/reject

---

#### ⚠️ Issue #4: Thiếu Location Status/Type Validation

**Mô tả:**
Không kiểm tra:
- Location có đang active không
- Location có đúng type (STORAGE, PICKING, RECEIVING, etc.) không
- Batch có đang active và chưa expired không

**Vị trí:**
- `StockTransfersServiceImpl.java` - `validateTransferRequest()`

---

### 4.3 Vấn đề cải thiện (Nice to have)

#### 💡 Issue #5: Thiếu luồng PENDING cho Stock Transfer

**Mô tả:**
Hiện tại Stock Transfer chỉ có DRAFT → COMPLETED. Thiếu trạng thái trung gian PENDING để:
- Người khác review trước khi complete
- Kiểm tra lại thông tin

---

#### 💡 Issue #6: Thiếu API Update/Void cho Stock Adjustment

**Mô tả:**
Không thể:
- Edit adjustment đang ở PENDING
- Hủy adjustment đã approved (void)

---

#### 💡 Issue #7: Inventory Available Calculation không nhất quán

**Mô tả:**
Code tính:
```java
available = onHand - quarantine - reserved
```

Cần xác nhận với business:
- `Available` có nên trừ quarantine không?
- Hay `Available = OnHand - Reserved` (quarantine vẫn available cho some cases)?

---

## 5. Khuyến nghị

### 5.1 Fix ưu tiên cao

| # | Hành động | Mô tả |
|---|-----------|-------|
| 1 | Thêm warehouse ownership check | Kiểm tra user có quyền với warehouse trước khi thao tác |
| 2 | Validate 2 location cùng warehouse | Rõ ràng hóa logic transfer trong/cross warehouse |

### 5.2 Fix ưu tiên trung bình

| # | Hành động | Mô tả |
|---|-----------|-------|
| 3 | Cho phép MANAGER approve | Sửa `assertCanApproveReject` để cho phép cả ADMIN và MANAGER |
| 4 | Validate Location status/type | Kiểm tra location active và đúng type |
| 5 | Validate Batch status | Kiểm tra batch active và chưa expired |

### 5.3 Fix ưu tiên thấp

| # | Hành động | Mô tả |
|---|-----------|-------|
| 6 | Thêm PENDING status cho Transfer | Luồng DRAFT → PENDING → COMPLETED |
| 7 | Thêm Update/Void cho Adjustment | Cho phép edit/hủy adjustment |
| 8 | Xác nhận available calculation | Làm rõ business logic cho available quantity |

---

## Phụ lục

### A. Error Codes

| Code | Mô tả |
|------|-------|
| `STF_001` | Stock Transfer not found |
| `STF_002` | Invalid stock transfer state/transition |
| `STF_003` | Invalid quantity |
| `STA_001` | Stock Adjustment validation error |
| `STA_002` | Adjustment not in pending status |
| `STA_003` | Rejection reason required |
| `INV_001` | Inventory not found |
| `INV_004` | Insufficient available stock |

### B. Security Rules

1. Tất cả API đều yêu cầu authentication
2. Approval/Rejection chỉ cho ADMIN (đề xuất: cả ADMIN và MANAGER)
3. Warehouse isolation cần được enforce ở service layer

### C. Transaction Rules

1. Complete Transfer: atomic, lock inventory theo order
2. Create Adjustment (auto-approve): atomic
3. Approve Adjustment: atomic, verify quantityBefore unchanged
4. Sử dụng pessimistic lock cho inventory updates
