# Hướng dẫn Triển khai Module 6: Nghiệp vụ Xuất kho (Outbound)

---

## 📋 Thông tin

| Thuộc tính | Giá trị |
|------------|---------|
| **Module** | Outbound Operations |
| **Số APIs** | 19 APIs |
| **Trạng thái** | Chưa triển khai |
| **Ước tính** | ~16-17 ngày |

---

## 🎯 Tổng quan Luồng Nghiệp vụ

```
Tạo Đơn bán hàng (DRAFT)
    ↓
Xác nhận Đơn bán hàng (CONFIRMED) → Đặt trước tồn kho (reserve)
    ↓
Tạo Lô xuất hàng (DRAFT)
    ↓
Bắt đầu Picking (PICKING) → Hiển thị danh sách picking + FIFO recommendations
    ↓
Xác nhận Lô xuất hàng (SHIPPED) → Giảm tồn kho + Ghi movement
```

---

## 📝 Danh sách Tasks cần làm

### Phase 0: Chuẩn bị (0.5 ngày)

| Task | Mô tả | Jira |
|------|-------|------|
| T0.1 | Sửa SalesOrderLinesController - inject đúng Service | - |
| T0.2 | Sửa OutboundShipmentsController - inject Service | - |
| T0.3 | Sửa OutboundShipmentLinesController - inject Service | - |
| T0.4 | Sửa OutboundShipmentLines entity - thêm nullable=false cho locationId | - |

---

### Phase 1: Sales Orders CRUD (3 ngày)

#### Task 1.1: Tạo DTOs cho Sales Orders
**Files cần tạo:**
- `SalesOrdersRequest.java` - Request tạo SO
- `UpdateSalesOrdersRequest.java` - Request cập nhật SO
- `SalesOrdersFilterRequest.java` - Request filter
- `SalesOrdersResponse.java` - Response
- `SalesOrderLinesResponse.java` - Response cho lines

**Logic:**
- Tạo SO mới → sinh số `SO-YYYY-NNNN` tự động
- Tính `lineTotal = quantityOrdered × unitPrice`
- Tính `subTotal`, `taxAmount`, `totalAmount`
- Lưu với status = DRAFT

#### Task 1.2: Implement SalesOrdersService
**Methods cần implement:**
- `create(SalesOrdersRequest)` - Tạo SO mới
- `getAll(Filter, Pageable)` - Danh sách phân trang
- `getById(String id)` - Chi tiết SO
- `update(String id, UpdateSalesOrdersRequest)` - Cập nhật (chỉ DRAFT)
- `delete(String id)` - Xóa (chỉ DRAFT)

**Business Rules:**
- BR-OUT-01: Số SO format `SO-YYYY-NNNN`, tự tăng theo năm
- BR-OUT-03: SO DRAFT được phép chỉnh sửa
- SO phải có ít nhất 1 dòng (OUT_001)
- Customer phải tồn tại, type=CUSTOMER, status=ACTIVE (OUT_002)

#### Task 1.3: Implement SalesOrdersController
| Method | Endpoint | Mô tả |
|--------|----------|--------|
| POST | `/api/v1/sales-orders` | Tạo SO (DRAFT) |
| GET | `/api/v1/sales-orders` | Danh sách SO (filter + phân trang) |
| GET | `/api/v1/sales-orders/{id}` | Chi tiết SO |
| PUT | `/api/v1/sales-orders/{id}` | Cập nhật SO (chỉ DRAFT) |
| DELETE | `/api/v1/sales-orders/{id}` | Xóa SO (chỉ DRAFT) |

---

### Phase 2: Confirm & Cancel SO (3 ngày)

#### Task 2.1: Confirm Sales Order
**Endpoint:** `PUT /api/v1/sales-orders/{id}/confirm`

**Logic chi tiết:**
```
1. Load SO → verify status = DRAFT
2. For EACH line:
   a. Tính available = SUM(inventory.onHand) - SUM(inventory.reserved)
      WHERE product = line.product AND warehouse = SO.warehouse
      AND batch.status != QUARANTINE (nếu batch-tracked)
   b. IF available < line.quantityOrdered → throw OUT_003
3. For EACH line:
   a. inventory.reservedQuantity += line.quantityOrdered
4. Update SO.status = CONFIRMED
5. Update SO.confirmedAt = now()
6. Update SO.confirmedBy = currentUser
```

**Business Rules:**
- BR-OUT-02: Không đủ tồn kho → reject (OUT_003)
- BR-OUT-03: Reservation là ATOMIC - all or nothing
- BR-OUT-04: SO đã CONFIRMED không thể edit
- BR-OUT-06: Available stock loại trừ QUARANTINE batches

#### Task 2.2: Cancel Sales Order
**Endpoint:** `PUT /api/v1/sales-orders/{id}/cancel`

**Logic chi tiết:**
```
1. Load SO → verify status = CONFIRMED
2. Kiểm tra không có shipment active (status ∉ {PICKING, PACKED, SHIPPED})
3. For EACH line:
   a. remaining = line.quantityOrdered - line.quantityShipped
   b. inventory.reservedQuantity -= remaining
4. Update SO.status = CANCELLED
```

**Business Rules:**
- BR-OUT-05: Không thể cancel nếu có active shipments (OUT_005)
- BR-OUT-06: Chỉ cancel được CONFIRMED (OUT_006)
- BR-OUT-09: PARTIALLY_SHIPPED không cancel được
- BR-OUT-14: Giải phóng tất cả reservations

#### Task 2.3: Kiểm tra InventoryService methods
**Cần xác nhận methods tồn tại:**
```java
void reserveStock(String productId, String warehouseId, BigDecimal quantity);
void unreserveStock(String productId, String warehouseId, BigDecimal quantity);
```
Nếu chưa có → implement trong InventoryService

---

### Phase 3: Sales Order Lines (2 ngày)

#### Task 3.1: Implement SalesOrderLines Service + Controller

| Method | Endpoint | Mô tả |
|--------|----------|--------|
| POST | `/api/v1/sales-order-lines` | Thêm dòng SO |
| GET | `/api/v1/sales-order-lines/by-so/{soId}` | Danh sách lines theo SO |
| PUT | `/api/v1/sales-order-lines/{id}` | Cập nhật dòng SO |
| DELETE | `/api/v1/sales-order-lines/{id}` | Xóa dòng SO |

**Logic:**
- Chỉ thao tác được khi SO status = DRAFT (guard)
- Khi thêm/sửa/xóa line → recalculate SO totals
- quantityShipped luôn = 0 khi tạo mới
- lineNumber tự sinh hoặc cho phép custom

---

### Phase 4: Outbound Shipments CRUD (3 ngày)

#### Task 4.1: Tạo DTOs cho Shipments
- `OutboundShipmentsRequest.java`
- `UpdateOutboundShipmentsRequest.java`
- `OutboundShipmentsFilterRequest.java`
- `OutboundShipmentsResponse.java`
- `OutboundShipmentLinesResponse.java`

#### Task 4.2: Implement OutboundShipmentsService

**Logic Tạo Shipment:**
```
1. Load SO → verify status ∈ {CONFIRMED, PARTIALLY_SHIPPED}
2. Generate shipment number: SHIP-YYYY-NNNN
3. Set warehouseId = SO.warehouseId
4. For EACH shipment line:
   a. Verify salesOrderLineId thuộc SO
   b. Verify quantityShipped > 0
   c. Verify quantityShipped ≤ remaining (ordered - shipped)
   d. Verify locationId thuộc warehouse
   e. Verify batchId nếu product batch-tracked
5. Save với status = DRAFT
```

**Business Rules:**
- BR-OUT-10: Số shipment format `SHIP-YYYY-NNNN`
- BR-OUT-11: Cho phép nhiều shipments cho 1 SO
- BR-OUT-12: Tổng shipped ≤ ordered quantity

#### Task 4.3: Implement OutboundShipmentsController

| Method | Endpoint | Mô tả |
|--------|----------|--------|
| POST | `/api/v1/outbound-shipments` | Tạo shipment (DRAFT) |
| GET | `/api/v1/outbound-shipments` | Danh sách shipments |
| GET | `/api/v1/outbound-shipments/{id}` | Chi tiết shipment |

---

### Phase 5: Pick + Confirm Shipment (4 ngày)

#### Task 5.1: Pick Items
**Endpoint:** `PUT /api/v1/outbound-shipments/{id}/pick`

**Logic:**
```
1. Load shipment → verify status = DRAFT
2. Update status = PICKING
3. Generate pick list data (locations, products, quantities)
4. Return pick list + FIFO recommendations
```

**FIFO Recommendations SQL:**
```sql
SELECT b.id, b.batch_number, b.manufacturing_date, b.expiry_date,
       i.on_hand_quantity - i.reserved_quantity AS available_qty,
       l.code AS location_code
FROM inventory i
JOIN batch b ON i.batch_id = b.id
JOIN locations l ON i.location_id = l.id
WHERE i.product_id = :productId
  AND i.warehouse_id = :warehouseId
  AND b.status = 'AVAILABLE'
  AND (i.on_hand_quantity - i.reserved_quantity) > 0
ORDER BY b.manufacturing_date ASC, b.expiry_date ASC
```

**Business Rules:**
- BR-OUT-13: FIFO khuyến nghị nhưng KHÔNG bắt buộc
- BR-OUT-14: Nhân viên có thể override với lý do

#### Task 5.2: Get Pick List
**Endpoint:** `GET /api/v1/outbound-shipments/{id}/pick-list`

**Logic:** Trả về danh sách picking với FIFO recommendations

#### Task 5.3: Confirm Shipment (CRITICAL - Atomic Transaction)
**Endpoint:** `PUT /api/v1/outbound-shipments/{id}/confirm`

**Logic chi tiết:**
```
@Transactional
1. Load shipment → verify status ∈ {PICKING, PACKED}
2. Verify shipment có ít nhất 1 line
3. Load SO + SO lines (SELECT FOR UPDATE - lock)
4. Verify SO.status ∈ {CONFIRMED, PARTIALLY_SHIPPED}

5. For EACH shipment line:
   a. remaining = SO_line.quantityOrdered - SO_line.quantityShipped
   b. IF shipment_line.quantityShipped > remaining → throw OUT_013
   
   c. Load Inventory (product, warehouse, location, batch)
   d. IF inventory.onHandQuantity < quantityShipped → throw OUT_014
   
   e. inventory.onHandQuantity -= quantityShipped
   f. inventory.reservedQuantity -= quantityShipped
   
   g. Write StockMovements {
        type: OUTBOUND,
        referenceType: OUTBOUND_SHIPMENT,
        referenceId: shipment.id,
        productId, warehouseId,
        fromLocationId: locationId, toLocationId: null,
        batchId, quantity: quantityShipped
      }
   
   h. SO_line.quantityShipped += quantityShipped

7. Recompute SO status:
   - IF ALL lines: quantityShipped ≥ quantityOrdered → SO.status = COMPLETED
   - ELSE → SO.status = PARTIALLY_SHIPPED

8. Update shipment: 
   - status = SHIPPED
   - shippedAt = now()
   - confirmedBy = currentUser

9. Commit transaction
```

**Business Rules:**
- BR-OUT-08: Confirmation là ATOMIC - fail sẽ rollback toàn bộ
- BR-OUT-09: Giảm CẢ onHand VÀ reserved
- BR-OUT-16: Không ship nhiều hơn ordered
- BR-OUT-17: Không ship từ location không đủ stock

#### Task 5.4: Implement OutboundShipmentLines CRUD

| Method | Endpoint | Mô tả |
|--------|----------|--------|
| POST | `/api/v1/outbound-shipment-lines` | Thêm dòng shipment |
| PUT | `/api/v1/outbound-shipment-lines/{id}` | Cập nhật dòng shipment |
| DELETE | `/api/v1/outbound-shipment-lines/{id}` | Xóa dòng shipment |

**Logic:** Chỉ thao tác được khi shipment status = DRAFT

---

### Phase 6: Hoàn thiện (1 ngày)

#### Task 6.1: Error Codes
| Code | Mô tả |
|------|--------|
| OUT_001 | SO phải có ít nhất 1 dòng |
| OUT_002 | Customer không hoạt động |
| OUT_003 | Không đủ stock |
| OUT_004 | Chỉ confirm được DRAFT |
| OUT_005 | Không thể cancel nếu có shipments |
| OUT_006 | Chỉ cancel được CONFIRMED |
| OUT_007 | Không tìm thấy SO |
| OUT_008 | Không tìm thấy SO line |
| OUT_009 | Không tìm thấy Shipment |
| OUT_010 | Chỉ sửa được DRAFT shipment |
| OUT_011 | Chỉ pick được DRAFT shipment |
| OUT_012 | Chỉ confirm được PICKING/PACKED |
| OUT_013 | Ship vượt quá remaining |
| OUT_014 | Không đủ on-hand stock |
| OUT_015 | Location không thuộc warehouse |
| OUT_016 | Shipment phải có ít nhất 1 dòng |

#### Task 6.2: Unit Tests
- Create SO success / fail
- Confirm SO - đủ stock / không đủ stock
- Cancel SO - success / có shipment active
- Create Shipment - success / SO chưa confirm
- Confirm Shipment - success / stock không đủ

---

## 🔗 Điểm Tích hợp

### Với InventoryService
```java
// Cần xác nhận/implement:
void reserveStock(String productId, String warehouseId, BigDecimal quantity);
void unreserveStock(String productId, String warehouseId, BigDecimal quantity);
void decreaseStock(String productId, String warehouseId, String locationId, 
                   String batchId, BigDecimal quantity);
```

### Với StockMovementsService
```java
// Cần gọi:
void createMovement(StockMovementsRequest request);
// referenceType: OUTBOUND_SHIPMENT
// type: OUTBOUND
```

### Với BatchService
```java
// Cần gọi:
List<BatchFifoRecommendationResponse> getFifoRecommendations(productId, warehouseId, limit);
```

---

## 📊 Tổng kết Tasks

| Phase | Số Tasks | Thời gian |
|-------|-----------|-----------|
| Phase 0: Chuẩn bị | 4 | 0.5 ngày |
| Phase 1: SO CRUD | 3 | 3 ngày |
| Phase 2: Confirm/Cancel | 3 | 3 ngày |
| Phase 3: SO Lines | 1 | 2 ngày |
| Phase 4: Shipment CRUD | 3 | 3 ngày |
| Phase 5: Pick/Confirm | 4 | 4 ngày |
| Phase 6: Hoàn thiện | 2 | 1 ngày |
| **TỔNG** | **20 tasks** | **~16.5 ngày** |

---

## ⚠️ Lưu ý Quan trọng

1. **Atomic Transactions**: Confirm SO và Confirm Shipment phải là `@Transactional` - fail sẽ rollback toàn bộ
2. **FIFO**: Chỉ là khuyến nghị, không bắt buộc - nhưng cần hiển thị để picker chọn
3. **Partial Shipments**: Cho phép nhiều shipments cho 1 SO - cần track quantityShipped correctly
4. **Reserved vs OnHand**: Khi ship phải giảm CẢ hai
5. **Status Transitions**: Phải validate status hiện tại trước khi chuyển

---

## 📁 Files cần tạo mới

### DTOs
```
entity.dto.request.SalesOrders/
  - SalesOrdersRequest.java
  - UpdateSalesOrdersRequest.java
  - SalesOrdersFilterRequest.java

entity.dto.response.SalesOrders/
  - SalesOrdersResponse.java
  - SalesOrderLinesResponse.java

entity.dto.request.OutboundShipments/
  - OutboundShipmentsRequest.java
  - UpdateOutboundShipmentsRequest.java
  - OutboundShipmentsFilterRequest.java

entity.dto.response.OutboundShipments/
  - OutboundShipmentsResponse.java
  - OutboundShipmentLinesResponse.java
```

### Mappers
```
mapper.SalesOrdersMapper.java
mapper.SalesOrderLinesMapper.java
mapper.OutboundShipmentsMapper.java
mapper.OutboundShipmentLinesMapper.java
```

---

**Cập nhật lần cuối:** 17/03/2026
**Người tạo:** AI Assistant
