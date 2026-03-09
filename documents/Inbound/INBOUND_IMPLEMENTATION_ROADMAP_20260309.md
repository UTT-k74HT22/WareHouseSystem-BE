# 🚀 Inbound Chain — Implementation Roadmap
## Date: 2026-03-09
## Supersedes: INBOUND_MODULE_REVIEW_AND_PLAN_20260307.md (implementation phases)

---

## 📋 Tổng Quan

Module **Inbound Operations** là chain tiếp theo cần triển khai sau khi **Purchase Orders CRUD** đã hoàn thành 100%.

### Scope

| Sub-module | Jira Parent | APIs | Status |
|------------|-------------|------|--------|
| Purchase Order Lines | `WHS-44` | 3 | 🔴 0% |
| Inbound Receipts | `WHS-45` | 7 | 🔴 0% |
| Inbound Receipt Lines | `WHS-46` | 3 | 🔴 0% |
| Confirm Receipt (core) | `WHS-57` | 1 (trong Receipts) | 🔴 0% |

**Tổng:** 13 APIs + 1 core transaction flow

### Dependency Chain

```
Purchase Orders (✅ Done)
    ↓
Purchase Order Lines (Phase 1)
    ↓
Inbound Receipts CRUD (Phase 2)
    ↓
Inbound Receipt Lines (Phase 3)
    ↓
Confirm Receipt → Inventory + StockMovements (Phase 4)
```

---

## ✅ Pre-requisites (Đã hoàn thành 09/03/2026)

- [x] Fix `InboundReceiptLines.java`: trailing space in `@Table(name)`
- [x] Fix `InboundReceiptLines.java`: `batchId` → `nullable = true`
- [x] Verify `PurchaseOrders.java`: table name OK (no trailing space)
- [x] Confirm PurchaseOrdersStatus enum: `DRAFT, CONFIRMED, PARTIALLY_RECEIVED, COMPLETED, CANCELLED`
- [x] Confirm InboundReceiptsStatus enum: `DRAFT, CONFIRMED, CANCELLED`
- [x] Over-receipt disabled (keep DB constraint `quantity_received <= quantity_ordered`)

---

## Phase 1: Purchase Order Lines (`WHS-44`) — ~2 ngày

### Endpoints

| Method | Endpoint | Mô tả | Auth |
|--------|----------|--------|------|
| `POST` | `/api/v1/purchase-order-lines` | Thêm dòng vào PO | `isAuthenticated()` |
| `PUT` | `/api/v1/purchase-order-lines/{id}` | Cập nhật dòng PO | `isAuthenticated()` |
| `DELETE` | `/api/v1/purchase-order-lines/{id}` | Xóa dòng PO | `isAuthenticated()` |

### Bổ sung read endpoints (khuyến nghị)

| Method | Endpoint | Mô tả |
|--------|----------|--------|
| `GET` | `/api/v1/purchase-order-lines/by-po/{poId}` | Lấy danh sách dòng theo PO |

### Business Rules

1. **DRAFT-only guard:** Chỉ cho phép add/update/delete line khi PO ở trạng thái `DRAFT`
2. **lineNumber auto-increment:** Tự tính `lineNumber` theo max hiện tại + 1 trong PO
3. **Validation:**
   - `quantityOrdered > 0`
   - `unitPrice >= 0`
   - `lineTotal = quantityOrdered × unitPrice` (server-side compute)
   - `productId` phải tồn tại và active
4. **PO totals recomputation:** Sau mỗi add/update/delete line → recompute `subTotal`, `taxAmount`, `totalAmount` của PO parent
5. **Unique constraint:** `(purchaseOrderId, lineNumber)` unique
6. **quantityReceived:** Khởi tạo = 0, chỉ update qua confirm receipt

### DTOs cần tạo

```
PurchaseOrderLineRequest {
    purchaseOrderId: String (required)
    productId: String (required)
    quantityOrdered: BigDecimal (required, > 0)
    unitPrice: BigDecimal (required, >= 0)
    notes: String (optional)
}

UpdatePurchaseOrderLineRequest {
    quantityOrdered: BigDecimal (optional, > 0)
    unitPrice: BigDecimal (optional, >= 0)
    notes: String (optional)
}

PurchaseOrderLineResponse {
    id, purchaseOrderId, productId, productSku, productName,
    lineNumber, quantityOrdered, quantityReceived, unitPrice,
    lineTotal, notes, createdAt, updatedAt
}
```

### Pattern Reference

Sử dụng cùng pattern với `PurchaseOrdersController`:
- `@Tag(name = "Purchase Order Lines")`
- `@PreAuthorize("isAuthenticated()")`
- `@Validated`, `@Valid` trên request body
- Return `BaseResponse<T>`
- `@Operation(summary = "...")`

### Deliverables

- [ ] `PurchaseOrderLineRequest` + `UpdatePurchaseOrderLineRequest` DTOs
- [ ] `PurchaseOrderLineResponse` DTO
- [ ] `PurchaseOrderLineMapper`
- [ ] `PurchaseOrderLinesService` interface methods
- [ ] `PurchaseOrderLinesServiceImpl` + `@Transactional` cho recompute totals
- [ ] `PurchaseOrderLinesController` endpoints
- [ ] Unit tests (DRAFT-only guard, total recomputation, validation)

---

## Phase 2: Inbound Receipts CRUD (`WHS-45`) — ~3 ngày

### Endpoints

| Method | Endpoint | Mô tả | Auth |
|--------|----------|--------|------|
| `POST` | `/api/v1/inbound-receipts` | Tạo phiếu nhập kho (DRAFT) | `isAuthenticated()` |
| `GET` | `/api/v1/inbound-receipts` | Danh sách phiếu nhập (filters + phân trang) | `isAuthenticated()` |
| `GET` | `/api/v1/inbound-receipts/{id}` | Chi tiết phiếu nhập | `isAuthenticated()` |
| `PUT` | `/api/v1/inbound-receipts/{id}` | Cập nhật phiếu nhập | `isAuthenticated()` |
| `DELETE` | `/api/v1/inbound-receipts/{id}` | Xóa phiếu nhập | `isAuthenticated()` |
| `GET` | `/api/v1/inbound-receipts/by-po/{poId}` | Phiếu nhập theo PO | `isAuthenticated()` |

### Business Rules

1. **PO status guard:** Chỉ tạo receipt từ PO ở trạng thái `CONFIRMED` hoặc `PARTIALLY_RECEIVED`
2. **DRAFT-only guard:** Chỉ update/delete receipt ở trạng thái `DRAFT`
3. **Receipt number auto-gen:** Format `GR-YYYY-NNNN` (auto-increment)
4. **warehouseId:** Tự lấy từ PO liên kết, không cho phép user nhập khác warehouse của PO
5. **receiptDate:** Default = today nếu không truyền

### Filter/Sort (copy pattern từ PurchaseOrdersController)

```
Filters: receiptNumber, purchaseOrderId, warehouseId, status, receiptDateFrom, receiptDateTo
Sort fields: createdAt, updatedAt, receiptNumber, receiptDate, status
Page validation: page >= 0, 0 < size <= 100
```

### DTOs cần tạo

```
InboundReceiptRequest {
    purchaseOrderId: String (required)
    receiptDate: LocalDate (optional, default today)
    deliveryNoteNumber: String (optional)
    notes: String (optional)
}

UpdateInboundReceiptRequest {
    receiptDate: LocalDate (optional)
    deliveryNoteNumber: String (optional)
    notes: String (optional)
}

InboundReceiptFilterRequest {
    receiptNumber, purchaseOrderId, warehouseId, status,
    receiptDateFrom, receiptDateTo
}

InboundReceiptResponse {
    id, receiptNumber, purchaseOrderId, purchaseOrderNumber,
    warehouseId, warehouseName, receiptDate, status,
    deliveryNoteNumber, notes, confirmedAt, confirmedBy,
    lines: List<InboundReceiptLineResponse>,
    createdAt, updatedAt
}
```

### Deliverables

- [ ] Request/Response DTOs + Filter DTO
- [ ] `InboundReceiptMapper`
- [ ] `InboundReceiptsService` interface
- [ ] `InboundReceiptsServiceImpl` (CRUD + receipt number generation)
- [ ] `InboundReceiptsRepository` custom queries
- [ ] `InboundReceiptsController` 6 endpoints
- [ ] Unit tests

---

## Phase 3: Inbound Receipt Lines (`WHS-46`) — ~2 ngày

### Endpoints

| Method | Endpoint | Mô tả | Auth |
|--------|----------|--------|------|
| `POST` | `/api/v1/inbound-receipt-lines` | Thêm dòng vào phiếu nhập | `isAuthenticated()` |
| `PUT` | `/api/v1/inbound-receipt-lines/{id}` | Cập nhật dòng phiếu nhập | `isAuthenticated()` |
| `DELETE` | `/api/v1/inbound-receipt-lines/{id}` | Xóa dòng phiếu nhập | `isAuthenticated()` |

### Bổ sung read endpoints (khuyến nghị)

| Method | Endpoint | Mô tả |
|--------|----------|--------|
| `GET` | `/api/v1/inbound-receipt-lines/by-receipt/{receiptId}` | Dòng theo phiếu nhập |

### Business Rules

1. **DRAFT-only guard:** Chỉ add/update/delete khi receipt ở `DRAFT`
2. **PO Line binding:** Phải map về đúng `purchaseOrderLineId` thuộc PO của receipt
3. **Quantity validation:**
   - `quantityReceived > 0`
   - `quantityReceived ≤ remaining` (= PO line `quantityOrdered` − tổng `quantityReceived` từ tất cả confirmed receipts)
4. **Location validation:** `locationId` bắt buộc, phải thuộc đúng warehouse của receipt
5. **Batch validation:**
   - `batchId` bắt buộc nếu product có `requiresBatchTracking = true`
   - `batchId` phải null hoặc empty nếu product không track batch
6. **Quality status:**
   - `qualityStatus = QUARANTINE` → bắt buộc có `notes`
   - Mặc định: `ACCEPTED`
7. **lineNumber:** Auto-increment trong receipt

### DTOs cần tạo

```
InboundReceiptLineRequest {
    inboundReceiptId: String (required)
    purchaseOrderLineId: String (required)
    productId: String (required)
    batchId: String (optional - required if batch-tracked)
    locationId: String (required)
    quantityReceived: BigDecimal (required, > 0)
    qualityStatus: QualityStatus (optional, default ACCEPTED)
    notes: String (optional - required if QUARANTINE)
}

UpdateInboundReceiptLineRequest {
    batchId: String (optional)
    locationId: String (optional)
    quantityReceived: BigDecimal (optional, > 0)
    qualityStatus: QualityStatus (optional)
    notes: String (optional)
}

InboundReceiptLineResponse {
    id, inboundReceiptId, purchaseOrderLineId,
    productId, productSku, productName,
    batchId, batchNumber,
    locationId, locationCode, locationName,
    lineNumber, quantityReceived, qualityStatus, notes,
    createdAt, updatedAt
}
```

---

## Phase 4: Confirm Receipt (`WHS-57`) — ~3 ngày ⚠️ CRITICAL

### Endpoint

| Method | Endpoint | Mô tả | Auth |
|--------|----------|--------|------|
| `PUT` | `/api/v1/inbound-receipts/{id}/confirm` | Xác nhận phiếu nhập → cập nhật inventory | `isAuthenticated()` |

### Transaction Flow (Atomic - `@Transactional`)

```
1.  Load receipt                          → NotFoundException nếu không tìm thấy
2.  Validate receipt.status == DRAFT      → BadRequestException nếu không phải DRAFT
3.  Validate receipt có ít nhất 1 line    → BadRequestException nếu rỗng
4.  Load PO + PO lines                   → Lock row (SELECT FOR UPDATE)
5.  Validate PO.status ∈ {CONFIRMED, PARTIALLY_RECEIVED}
6.  For each receipt line:
    a. Recompute remaining = PO_line.quantityOrdered − SUM(confirmed_receipt_lines.quantityReceived)
    b. Validate receipt_line.quantityReceived ≤ remaining
    c. Validate locationId thuộc receipt.warehouseId
    d. If product.requiresBatchTracking:
       - Create/find Batch (by batchNumber + productId)
    e. Create/find Inventory row (product, warehouse, location, batch)
    f. Increase inventory.onHandQuantity += quantityReceived
    g. Write StockMovements {
         type: INBOUND,
         referenceType: INBOUND_RECEIPT,
         referenceId: receipt.id,
         productId, warehouseId, fromLocationId: null,
         toLocationId: locationId, batchId,
         quantity: quantityReceived
       }
    h. Update PO_line.quantityReceived += quantityReceived
7.  Recompute PO status:
    - If ALL lines: quantityReceived >= quantityOrdered → PO.status = COMPLETED
    - Else → PO.status = PARTIALLY_RECEIVED
8.  Update receipt: status = CONFIRMED, confirmedAt = now(), confirmedBy = currentUser
9.  Commit transaction
```

### Quarantine Rule

- Hàng `QUARANTINE` **vẫn tăng** `onHandQuantity` trên inventory
- Nhưng **không được tính** available trong `check-availability`
- Cần bổ sung logic exclude batch `QUARANTINE` trong `InventoryServiceImpl.checkAvailability()`

### Error Codes (cần thêm)

| Code | Mô tả |
|------|--------|
| `INB_001` | Receipt not found |
| `INB_002` | Receipt is not in DRAFT status |
| `INB_003` | Receipt has no lines |
| `INB_004` | PO not in valid status for receiving |
| `INB_005` | Quantity exceeds remaining on PO line |
| `INB_006` | Location does not belong to receipt warehouse |
| `INB_007` | Batch required for batch-tracked product |
| `INB_008` | Quarantine quality status requires notes |
| `INB_009` | PO line not found |
| `INB_010` | Product not found or inactive |

### Integration Points

| Service | Method | Mục đích |
|---------|--------|----------|
| `InventoryService` | `increaseStock(productId, warehouseId, locationId, batchId, qty)` | Tăng onHandQuantity |
| `StockMovementsService` | `createMovement(INBOUND, INBOUND_RECEIPT, ...)` | Ghi audit trail |
| `BatchService` | `findOrCreateBatch(batchNumber, productId, ...)` | Tạo/tìm batch |

---

## Phase 5: Hardening — ~1 ngày

### Checklist

- [ ] Add `ErrorCode` entries `INB_001` → `INB_010` vào `ErrorCode.java`
- [ ] Add `INBOUND_RECEIPT` vào `ReferenceType` enum (nếu chưa có)
- [ ] Review `@PreAuthorize` matrix (nếu cần phân role Purchasing Manager / Receiving Staff)
- [ ] Swagger `@Operation`, `@ApiResponse` cho tất cả endpoint mới
- [ ] Structured log messages cho confirm flow
- [ ] Integration test: confirm tăng inventory đúng `(product, warehouse, location, batch)`
- [ ] Integration test: confirm ghi StockMovement `INBOUND`
- [ ] Integration test: confirm update PO line `quantityReceived` + PO status
- [ ] Integration test: partial receipt → PO `PARTIALLY_RECEIVED`
- [ ] Integration test: full receipt → PO `COMPLETED`
- [ ] Integration test: non-batch product (batchId = null)
- [ ] Integration test: quarantine line

---

## 📊 Tổng Kết

| Phase | Sub-module | APIs | Effort | Dependency |
|-------|------------|------|--------|------------|
| 1 | PO Lines | 3 (+1 read) | 2 ngày | PO ✅ |
| 2 | Inbound Receipts | 6 | 3 ngày | PO ✅, PO Lines |
| 3 | Inbound Receipt Lines | 3 (+1 read) | 2 ngày | Receipts |
| 4 | Confirm Receipt | 1 (core) | 3 ngày | All above + Inventory + StockMovements |
| 5 | Hardening | — | 1 ngày | All above |
| | **Tổng** | **13+** | **~11 ngày** | |

### Suggested Delivery Cadence

1. **PR #1:** Phase 1 (PO Lines CRUD) → small, self-contained
2. **PR #2:** Phase 2 + 3 (Receipts + Receipt Lines) → có thể gộp nếu scope nhỏ
3. **PR #3:** Phase 4 (Confirm Receipt) → PR riêng vì high-impact
4. **PR #4:** Phase 5 (Hardening + tests) → cleanup PR

---

## 🔜 Sau Inbound: Outbound Chain

Sau khi Inbound hoàn thành, chuyển sang **Outbound Operations** (Sales Orders → SO Lines → Outbound Shipments → Shipment Lines → Confirm Shipment).

Outbound sẽ mirror pattern của Inbound nhưng thêm:
- `reserve`/`unreserve` stock khi confirm/cancel SO
- `decrease` stock khi confirm shipment
- FIFO recommendations cho picking
- Pick list generation

Xem chi tiết: `documents/Outbound/BA_MODULE_06_OUTBOUND_OPERATIONS_V2.md`
