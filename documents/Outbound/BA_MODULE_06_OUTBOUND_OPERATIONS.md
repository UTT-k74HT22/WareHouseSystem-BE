# Tài liệu BA - Module 6: Nghiệp vụ Xuất kho
## Yêu cầu Nghiệp vụ

---

## 📋 Thông tin Tài liệu

| Thuộc tính | Giá trị |
|------------|---------|
| **Module** | Nghiệp vụ Xuất kho (Đơn bán hàng & Lô xuất) |
| **Phiên bản** | 1.0 |
| **Ngày** | 01/02/2026 |
| **Trạng thái** | Bản nháp |
| **Tác giả** | Phân tích Nghiệp vụ |

---

## 🎯 Bối cảnh Nghiệp vụ

### Vấn đề Hiện tại

1. **Không theo dõi đơn hàng**: Đơn bán hàng được theo dõi bằng bảng tính, dễ sai sót
2. **Phân bổ kép**: Cùng một tồn kho hứa hẹn cho nhiều khách hàng
3. **Lỗi picking**: Sản phẩm hoặc số lượng picking sai
4. **Không thực thi FIFO**: Tồn kho cũ không được picking trước, dẫn đến hết hạn
5. **Cập nhật chậm**: Tồn kho không được cập nhật cho đến cuối ngày
6. **Giao tiếp khách hàng kém**: Không thể cung cấp ETA chính xác

### Giá trị Nghiệp vụ

✅ **Ngăn chặn bán quá tồn kho** - Tồn kho được đặt trước khi xác nhận đơn  
✅ **Picking chính xác** - Danh sách picking do hệ thống tạo giảm lỗi  
✅ **Tuân thủ FIFO** - Khuyến nghị đảm bảo tồn kho cũ được xuất trước  
✅ **Cập nhật thời gian thực** - Tồn kho giảm ngay khi xuất hàng  
✅ **Sự hài lòng khách hàng** - Trạng thái đơn hàng và theo dõi chính xác  
✅ **Truy xuất nguồn gốc đầy đủ** - Theo dõi lô nào xuất cho khách hàng nào  

---

## 📦 Tổng quan Module

### Các Thực thể Chính
1. **Sales Orders** - Đơn bán hàng từ khách
2. **Sales Order Lines** - Dòng sản phẩm trong đơn
3. **Outbound Shipments** - Lô xuất hàng vật lý cho khách
4. **Outbound Shipment Lines** - Dòng sản phẩm trong lô xuất

### Luồng Quy trình

```
Tạo Đơn bán hàng (DRAFT)
    ↓
Xác nhận Đơn bán hàng (CONFIRMED)
    → Đặt trước tồn kho (reserve)
    ↓
Tạo Lô xuất hàng (DRAFT)
    → Thêm các dòng sản phẩm
    ↓
Bắt đầu Picking (DRAFT → PICKING)
    → Di chuyển inventory từ reserved location → PICKING location
    → Validate quantity vs reservation
    ↓
Đóng gói (PICKING → PACKED)
    → Di chuyển inventory từ PICKING → PACKING location
    ↓
Xác nhận Xuất hàng (PACKED → SHIPPED)
    → Di chuyển từ PACKING → STAGING
    → Giảm tồn kho (decrease from STAGING)
    → Cập nhật shipped_quantity cho SO lines
    → Cập nhật SO status (PARTIALLY_SHIPPED / COMPLETED)
    → Ghi nhận stock movement (OUTBOUND)
    → Thông báo khách hàng
```

### State Diagrams

#### OutboundShipments Status Flow
```
[DRAFT] ──(start-picking)──> [PICKING] ──(mark-as-packed)──> [PACKED] ──(ship)──> [SHIPPED]
    │                           │                              │                         │
    │                           │                              │                         │
    └─────────(cancel)─────────┴──────────────────(cancel)────┴─────────(cancel)─────────┘
                                                                              (không thể cancel nếu đã SHIPPED)
```

#### SalesOrders Status Flow
```
[DRAFT] ──(confirm)──> [CONFIRMED] ──(tạo shipment)──> [PARTIALLY_SHIPPED] ──(xuất đủ)──> [COMPLETED]
    │                           │                                    │
    │                           │                                    │
    └─────────(cancel)─────────┴─────────(cancel)────────────────────┘
          (chỉ DRAFT)                  (chỉ CONFIRMED, chưa shipped)
```

---

## 📋 Các Tính năng

### Tính năng 1: Quản lý Đơn bán hàng

**US-OUT-01**: Là Sales Manager, tôi muốn tạo đơn bán hàng với nhiều dòng sản phẩm, để xử lý đơn hàng khách hàng.

**US-OUT-02**: Là Sales Manager, tôi muốn xác nhận đơn bán hàng để đặt trước tồn kho, để tồn kho được phân bổ cho khách hàng.

**UC-OUT-01: Tạo Đơn bán hàng**
- Chọn khách hàng (đối tác có type = CUSTOMER)
- Thêm dòng sản phẩm (sản phẩm, số lượng, giá)
- Tính tổng tiền
- Lưu dưới dạng DRAFT (có thể chỉnh sửa)

**UC-OUT-02: Xác nhận Đơn bán hàng**
- Kiểm tra tồn kho khả dụng cho tất cả dòng
- Đặt trước tồn kho (gọi inventory.reserveStock())
- Cập nhật trạng thái thành CONFIRMED
- Gửi thông báo cho kho
- Gửi email xác nhận đơn cho khách hàng

**Quy tắc nghiệp vụ:**
- BR-OUT-01: Số SO tự động tạo (SO-YYYY-NNNN)
- BR-OUT-02: Không thể xác nhận nếu không đủ tồn kho
- BR-OUT-03: Đặt trước tồn kho atomic (tất cả dòng hoặc không có dòng nào)
- BR-OUT-04: SO đã CONFIRMED không thể chỉnh sửa (phải hủy)

---

### Tính năng 2: Xử lý Lô xuất hàng

**US-OUT-03**: Là Nhân viên kho, tôi muốn tạo lô xuất hàng từ đơn bán hàng, để chuẩn bị hàng xuất.

**US-OUT-04**: Là Picker, tôi muốn xem khuyến nghị FIFO, để xuất lô cũ nhất trước.

**US-OUT-05**: Là Nhân viên kho, tôi muốn xác nhận lô xuất hàng để cập nhật tồn kho, để mức tồn kho chính xác.

**UC-OUT-03: Tạo Lô xuất hàng & Tạo Danh sách Picking**
- Chọn đơn bán hàng đã CONFIRMED
- Hệ thống gợi ý lô theo thứ tự FIFO (ngày sản xuất cũ nhất trước)
- Nhân viên có thể ghi đè nhưng hệ thống cảnh báo
- Gán cho picker
- In/hiển thị danh sách picking với vị trí

**UC-OUT-04: Pick Items**
- Picker scan sản phẩm từ danh sách picking
- Hệ thống xác minh đúng sản phẩm, số lượng, lô
- Đánh dấu trạng thái PICKING

**UC-OUT-05: Xác nhận Lô xuất hàng**
- Xác minh tất cả sản phẩm đã được pick
- Bắt đầu transaction:
  - Giảm tồn kho (on-hand - số lượng xuất)
  - Giảm reserved (reserved - số lượng xuất)
  - Ghi nhận stock movement (OUTBOUND)
  - Cập nhật SO line shipped_quantity
  - Cập nhật trạng thái SO (PARTIALLY_SHIPPED hoặc COMPLETED)
- Commit transaction
- Cập nhật trạng thái shipment thành SHIPPED
- Thông báo khách hàng với thông tin tracking

**Quy tắc nghiệp vụ:**
- BR-OUT-05: Số shipment tự động tạo (SHIP-YYYY-NNNN)
- BR-OUT-06: Không thể xuất nhiều hơn số lượng đặt
- BR-OUT-07: FIFO khuyến nghị nhưng không bắt buộc
- BR-OUT-08: Xác nhận shipment là atomic
- BR-OUT-09: Giảm on-hand inventory khi xuất hàng (reserved đã được consume trong picking)
- BR-OUT-09a: Shipment chỉ được tạo từ SO CONFIRMED hoặc PARTIALLY_SHIPPED
- BR-OUT-09b: Shipment phải có ít nhất 1 line để bắt đầu picking
- BR-OUT-09c: Cancel shipment không được nếu đã SHIPPED
- BR-OUT-09d: Cancel trong PICKING/PACKED sẽ unreserve inventory đã reserved
- BR-OUT-09e: Ship (xuất hàng) chỉ được khi ở PACKED status
- BR-OUT-09f: Shipment lines được di chuyển qua các location: reserved → PICKING → PACKING → STAGING → (decrease)

---

### Tính năng 3: Xuất hàng Từng phần

**US-OUT-06**: Là Nhân viên kho, tôi muốn xuất một phần khi đơn đầy đủ không có sẵn, để khách hàng nhận được những gì có sẵn.

**UC-OUT-06: Xuất hàng Từng phần**
- Tạo shipment với số lượng < số lượng đặt
- Số lượng còn lại vẫn được reserved
- Trạng thái SO = PARTIALLY_SHIPPED
- Các shipments tương lai có thể xuất nốt

**Quy tắc nghiệp vụ:**
- BR-OUT-10: Cho phép nhiều shipments cho một SO
- BR-OUT-11: Tổng xuất ≤ số lượng đặt
- BR-OUT-12: SO được đánh dấu COMPLETED khi tất cả dòng đã xuất đủ

---

### Tính năng 4: Hủy Đơn hàng

**US-OUT-07**: Là Sales Manager, tôi muốn hủy đơn hàng, để giải phóng tồn kho đã đặt trước.

**UC-OUT-07: Hủy Đơn bán hàng**
- Chỉ SO đã CONFIRMED (chưa xuất hàng) mới có thể hủy
- Giải phóng tất cả tồn kho đã reserved
- Cập nhật trạng thái thành CANCELLED
- Gửi thông báo hủy

**Quy tắc nghiệp vụ:**
- BR-OUT-13: Không thể hủy sau khi tạo shipment
- BR-OUT-14: Hủy giải phóng tất cả reservations
- BR-OUT-15: Đơn hàng hủy được lưu giữ cho audit

---

## 🔗 Tóm tắt Ảnh hưởng API

### Sales Orders (`/api/v1/sales-orders`)

| Method | Endpoint | Mô tả | Vai trò |
|--------|----------|-------|-------|
| POST | /api/v1/sales-orders | Tạo SO (DRAFT) | ADMIN, MANAGER |
| GET | /api/v1/sales-orders | Danh sách SO (filter, phân trang) | ADMIN, MANAGER |
| GET | /api/v1/sales-orders/{id} | Chi tiết SO | ADMIN, MANAGER |
| PUT | /api/v1/sales-orders/{id} | Cập nhật SO (chỉ DRAFT) | ADMIN, MANAGER |
| PUT | /api/v1/sales-orders/{id}/confirm | Xác nhận SO → đặt trước tồn kho | ADMIN, MANAGER |
| PUT | /api/v1/sales-orders/{id}/cancel | Hủy SO → giải phóng tồn kho | ADMIN, MANAGER |

### Sales Order Lines (`/api/v1/sales-order-lines`)

| Method | Endpoint | Mô tả | Vai trò |
|--------|----------|-------|-------|
| POST | /api/v1/sales-order-lines | Thêm dòng vào SO | ADMIN, MANAGER |
| GET | /api/v1/sales-order-lines/by-so/{soId} | Danh sách dòng theo SO | ADMIN, MANAGER |
| GET | /api/v1/sales-order-lines/{id} | Chi tiết dòng | ADMIN, MANAGER |
| PUT | /api/v1/sales-order-lines/{id} | Cập nhật dòng (chỉ DRAFT) | ADMIN, MANAGER |

### Outbound Shipments (`/api/v1/outbound-shipments`)

| Method | Endpoint | Mô tả | Vai trò |
|--------|----------|-------|-------|
| POST | /api/v1/outbound-shipments | Tạo shipment (DRAFT) | ADMIN, MANAGER |
| GET | /api/v1/outbound-shipments | Danh sách shipments (filter, phân trang) | ADMIN, MANAGER |
| GET | /api/v1/outbound-shipments/{id} | Chi tiết shipment | ADMIN, MANAGER |
| PUT | /api/v1/outbound-shipments/{id} | Cập nhật shipment (chỉ DRAFT) | ADMIN, MANAGER |
| PUT | /api/v1/outbound-shipments/{id}/start-picking | Bắt đầu picking (DRAFT → PICKING) | ADMIN, MANAGER |
| PUT | /api/v1/outbound-shipments/{id}/mark-as-packed | Đánh dấu đóng gói (PICKING → PACKED) | ADMIN, MANAGER |
| PUT | /api/v1/outbound-shipments/{id}/ship | Xác nhận xuất hàng (PACKED → SHIPPED) | ADMIN, MANAGER |
| PUT | /api/v1/outbound-shipments/{id}/cancel | Hủy shipment (trừ SHIPPED) | ADMIN, MANAGER |

### Outbound Shipment Lines (`/api/v1/outbound-shipment-lines`)

| Method | Endpoint | Mô tả | Vai trò |
|--------|----------|-------|-------|
| POST | /api/v1/outbound-shipment-lines | Thêm dòng vào shipment | ADMIN, MANAGER |
| GET | /api/v1/outbound-shipment-lines/shipment/{shipmentId} | Danh sách dòng theo shipment | ADMIN, MANAGER |
| GET | /api/v1/outbound-shipment-lines/{id} | Chi tiết dòng | ADMIN, MANAGER |
| PUT | /api/v1/outbound-shipment-lines/{id} | Cập nhật dòng (chỉ DRAFT) | ADMIN, MANAGER |
| DELETE | /api/v1/outbound-shipment-lines/{id} | Xóa dòng (chỉ DRAFT) | ADMIN, MANAGER |

---

## 💾 Ảnh hưởng Database

Xem [DB_MODULE_06_OUTBOUND.md](./DB_MODULE_06_OUTBOUND.md)

### Các Bảng Mới

#### sales_orders
- `so_number` (SO-YYYY-NNNN)
- `customer_id` FK đến business_partners
- `warehouse_id`
- `status` (DRAFT, CONFIRMED, PARTIALLY_SHIPPED, COMPLETED, CANCELLED)
- `order_date`, `requested_delivery_date`
- Các trường tài chính

#### sales_order_lines
- `sales_order_id` FK
- `product_id`
- `quantity_ordered`, `quantity_shipped`, `quantity_remaining`
- `unit_price`, `line_total`

#### outbound_shipments
- `shipment_number` (SHIP-YYYY-NNNN)
- `sales_order_id` FK
- `warehouse_id`
- `status` (DRAFT, PICKING, PACKED, SHIPPED, CANCELLED)
- `shipped_at`, `tracking_number`

#### outbound_shipment_lines
- `outbound_shipment_id` FK
- `sales_order_line_id` FK
- `product_id`, `batch_id`, `location_id`
- `quantity_shipped`
- `picked_by`, `picked_at`

---

## 🔄 Điểm Tích hợp

### Với Module Inventory
- Xác nhận SO: `reserveStock()` cho mỗi dòng
- Xác nhận shipment: `decreaseStock()` (cả on-hand và reserved)
- Hủy: `unreserveStock()`

### Với Module Batch
- Khuyến nghị FIFO truy vấn lô theo manufacturing_date
- Chỉ các lô có status AVAILABLE được đưa vào khuyến nghị

### Với Module Stock Movement
- Xác nhận shipment tạo movement OUTBOUND
- Hủy tạo movement UNRESERVE

### Với Module Notification
- SO đã xác nhận: thông báo cho kho, khách hàng
- Shipment đã tạo: thông báo cho picker
- Shipment đã xuất: thông báo khách hàng với tracking

---

## 📊 Tóm tắt Quy tắc Nghiệp vụ

| Rule ID | Mô tả |
|---------|-------|
| BR-OUT-01 | Số SO tự động tạo |
| BR-OUT-02 | Không thể xác nhận nếu không đủ tồn kho |
| BR-OUT-03 | Đặt trước tồn kho atomic |
| BR-OUT-04 | SO đã CONFIRMED không thể sửa |
| BR-OUT-05 | Số shipment tự động tạo |
| BR-OUT-06 | Không thể xuất nhiều hơn đặt |
| BR-OUT-07 | FIFO khuyến nghị không bắt buộc |
| BR-OUT-08 | Xác nhận shipment atomic |
| BR-OUT-09 | Giảm cả on-hand và reserved |
| BR-OUT-10 | Nhiều shipments cho một SO |
| BR-OUT-11 | Tổng xuất ≤ đặt |
| BR-OUT-12 | SO hoàn thành khi xuất đủ |
| BR-OUT-13 | Không thể hủy sau khi tạo shipment |
| BR-OUT-14 | Hủy giải phóng reservations |
| BR-OUT-15 | Đơn hủy được lưu giữ |

---

**Phiên bản tài liệu:** 1.0  
**Cập nhật lần cuối:** 01/02/2026  
**Trạng thái:** 🚧 Bản nháp - Chờ Review
