# Tài liệu BA - Module 5: Nghiệp vụ Nhập kho
## Yêu cầu Nghiệp vụ

---

## 📋 Thông tin Tài liệu

| Thuộc tính | Giá trị |
|------------|---------|
| **Module** | Nghiệp vụ Nhập kho (Phiếu nhập kho & Chi tiết nhập) |
| **Phiên bản** | 1.0 |
| **Ngày** | 03/04/2026 |
| **Trạng thái** | Bản nháp |
| **Tác giả** | Phân tích Nghiệp vụ |

---

## 🎯 Bối cảnh Nghiệp vụ

### Vấn đề Hiện tại

1. **Không theo dõi phiếu nhập**: Phiếu nhập kho được quản lý bằng bảng tính, dễ sai sót và thất lạc
2. **Nhập liệu thủ công**: Nhập liệu số lượng và sản phẩm thủ công gây sai lệch giữa thực tế và hệ thống
3. **Không có kiểm soát chất lượng**: Hàng lỗi được nhập vào tồn kho mà không qua kiểm tra
4. **Cập nhật tồn kho chậm**: Mức tồn kho không được cập nhật cho đến cuối ngày
5. **Không truy xuất nguồn gốc**: Không thể liên kết hàng nhận về với đơn đặt hàng gốc
6. **Nhầm lẫn lô hàng**: Số lô không được ghi nhận trong quá trình nhận hàng

### Giá trị Nghiệp vụ

✅ **Cập nhật tồn kho thời gian thực** - Tồn kho được cập nhật ngay khi xác nhận phiếu nhập  
✅ **Truy xuất nguồn gốc đầy đủ** - Liên kết từ Đơn đặt hàng → Phiếu nhập → Lô → Tồn kho  
✅ **Kiểm soát chất lượng** - Kiểm tra hàng hóa trước khi nhập vào kho  
✅ **Giảm sai sót** - Xác thực hệ thống ngăn chặn số lượng không chính xác  
✅ **Hiệu suất nhà cung cấp** - Theo dõi giao hàng đúng hạn và vấn đề chất lượng  
✅ **Tuân thủ** - Nhật ký kiểm toán đầy đủ cho yêu cầu quy định  

---

## 👥 Vai trò & Người dùng

| Vai trò | Mục tiêu | Quyền hạn |
|---------|----------|-----------|
| **Quản lý Mua hàng** | Tạo và quản lý đơn đặt hàng | Tạo/sửa ĐĐH, xem tất cả ĐĐH |
| **Quản lý Kho** | Giám sát quy trình nhận hàng | Xem tất cả phiếu nhập, phê duyệt sai lệch |
| **Nhân viên Nhận hàng** | Xử lý hàng đến | Tạo phiếu nhập, xác nhận phiếu nhập |
| **Nhân viên Kiểm tra chất lượng** | Kiểm tra hàng nhận về | Xem phiếu nhập, đánh dấu trạng thái chất lượng |
| **Quản lý Tồn kho** | Giám sát tăng tồn kho | Xem phiếu nhập và tác động tồn kho |

---

## 📦 Tổng quan Module

Module Nghiệp vụ Nhập kho quản lý toàn bộ quy trình từ khi tạo đơn đặt hàng đến nhận hàng và tăng tồn kho.

### Các Thực thể Chính

1. **Purchase Orders (Đơn đặt hàng)** - Đơn đặt hàng từ nhà cung cấp
2. **Purchase Order Lines (Dòng đơn đặt hàng)** - Từng mặt hàng trong đơn
3. **Inbound Receipts (Phiếu nhập kho)** - Phiếu nhận hàng vật lý
4. **Inbound Receipt Lines (Dòng phiếu nhập)** - Chi tiết hàng nhận

### Luồng Quy trình

```
Tạo Đơn đặt hàng (DRAFT)
    ↓
Xác nhận Đơn đặt hàng (CONFIRMED)
    → Gửi đơn cho nhà cung cấp
    ↓
Hàng đến kho
    ↓
Tạo Phiếu nhập kho (DRAFT)
    → Thêm các dòng sản phẩm
    → Ghi nhận thông tin lô (nếu có)
    → Kiểm tra chất lượng
    ↓
Xác nhận Phiếu nhập (DRAFT → CONFIRMED)
    → Tăng tồn kho (increase onHandQuantity)
    → Tăng quarantineQuantity (nếu QUARANTINE)
    → Cập nhật dung lượng location
    → Ghi nhận stock movement (INBOUND)
    → Cập nhật quantityReceived cho PO lines
    → Cập nhật trạng thái PO (PARTIALLY_RECEIVED / COMPLETED)
    → Thông báo các bên liên quan
```

### State Diagrams

#### InboundReceipts Status Flow
```
[DRAFT] ──(confirm)──> [CONFIRMED]
   │
   └──(cancel)──> [CANCELLED]
       (chỉ DRAFT mới được hủy)
```

#### PurchaseOrders Status Flow (Liên quan)
```
[DRAFT] ──(confirm)──> [CONFIRMED] ──(nhận một phần)──> [PARTIALLY_RECEIVED] ──(nhận đủ)──> [COMPLETED]
   │                           │                                    │
   └─────────(cancel)─────────┴─────────(cancel)────────────────────┘
         (chỉ DRAFT)                   (chỉ CONFIRMED, chưa nhận hàng)
```

### Phụ thuộc

- **Phụ thuộc vào**: Dữ liệu Chủ (Kho, Vị trí, Sản phẩm, Đối tác)
- **Yêu cầu**: Quản lý Lô (cho sản phẩm theo dõi lô), Quản lý Tồn kho
- **Kích hoạt**: Di chuyển Tồn kho, Thông báo

---

## 📋 Các Tính năng

### Tính năng 1: Quản lý Đơn đặt hàng

**US-INB-01**: Là Quản lý Mua hàng, tôi muốn tạo đơn đặt hàng với nhiều dòng sản phẩm, để đặt hàng từ nhà cung cấp.

**US-INB-02**: Là Quản lý Mua hàng, tôi muốn xác nhận đơn đặt hàng trước khi gửi cho nhà cung cấp, để đơn trở thành đơn chính thức.

**US-INB-03**: Là Nhân viên Nhận hàng, tôi muốn xem các đơn đặt hàng đã xác nhận, để biết những lô hàng nào cần chờ nhận.

**US-INB-04**: Là Quản lý Mua hàng, tôi muốn theo dõi trạng thái đơn (draft, confirmed, partially received, completed), để theo dõi các đơn đang chờ.

**UC-INB-01: Tạo Đơn đặt hàng**
- Chọn nhà cung cấp (đối tác có type = SUPPLIER)
- Chọn kho đích
- Thêm dòng sản phẩm (sản phẩm, số lượng, đơn giá)
- Hệ thống tính tổng tiền
- Lưu dưới dạng DRAFT (có thể chỉnh sửa)
- Hệ thống tự động tạo số đơn (PO-YYYY-NNNN)

**UC-INB-02: Xác nhận Đơn đặt hàng**
- Kiểm tra đơn có ít nhất một dòng
- Kiểm tra tất cả số lượng > 0
- Cập nhật trạng thái thành CONFIRMED
- Gửi thông báo cho kho
- Đơn đã CONFIRMED không thể chỉnh sửa

**Quy tắc nghiệp vụ:**
- BR-INB-01: Số đơn tự động tạo và duy nhất
- BR-INB-02: Đơn phải có ít nhất một dòng
- BR-INB-03: Ngày giao dự kiến không được ở quá khứ
- BR-INB-04: Đơn DRAFT có thể chỉnh sửa và xóa
- BR-INB-05: Chỉ đơn DRAFT mới được xác nhận
- BR-INB-06: Đơn CONFIRMED không thể chỉnh sửa (phải hủy)
- BR-INB-07: Kho được thông báo khi đơn xác nhận

---

### Tính năng 2: Xử lý Phiếu nhập kho

**US-INB-05**: Là Nhân viên Nhận hàng, tôi muốn tạo phiếu nhập kho đối chiếu với đơn đặt hàng, để ghi nhận những gì đã giao.

**US-INB-06**: Là Nhân viên Nhận hàng, tôi muốn chỉ định vị trí kho nơi hàng được đặt, để có thể tìm chúng sau này.

**US-INB-07**: Là Nhân viên Nhận hàng, tôi muốn ghi nhận thông tin lô trong quá trình nhập, để sản phẩm theo dõi lô có khả năng truy xuất đúng.

**US-INB-08**: Là Nhân viên Nhận hàng, tôi muốn xác nhận phiếu nhập để cập nhật tồn kho, để tồn kho sẵn sàng ngay lập tức.

**UC-INB-03: Tạo Phiếu nhập kho**
- Chọn đơn đặt hàng đã CONFIRMED
- Hệ thống tải các dòng đơn với:
  - Sản phẩm
  - Số lượng đặt
  - Đã nhận đến nay
  - Còn lại cần nhận
- Nhập thông tin cho mỗi dòng:
  - Số lượng nhận (lô này)
  - Vị trí (nơi lưu trữ)
  - Thông tin lô (nếu theo dõi lô):
    - Số lô
    - Ngày sản xuất
    - Ngày hết hạn (nếu có)
  - Trạng thái chất lượng (PASS, QUARANTINE)
  - Ghi chú (tùy chọn)
- Hệ thống xác thực:
  - Số lượng nhận ≤ số lượng còn lại của đơn
  - Vị trí tồn tại và ACTIVE
  - Thông tin lô đầy đủ cho sản phẩm theo dõi lô
- Lưu dưới dạng DRAFT
- Hệ thống tự động tạo số phiếu nhập

**UC-INB-04: Xác nhận Phiếu nhập kho**
- Nhân viên xem lại phiếu nhập DRAFT
- Nhân viên nhấn "Xác nhận Phiếu nhập"
- Hệ thống xác thực tất cả dòng
- **Bắt đầu transaction:**
  - Kiểm tra phiếu nhập ở trạng thái DRAFT (với pessimistic lock)
  - Kiểm tra có ít nhất một dòng
  - Kiểm tra đơn đặt hàng ở trạng thái CONFIRMED hoặc PARTIALLY_RECEIVED
  - Kiểm tra kho của phiếu nhập khớp với kho của đơn
  - Kiểm tra sản phẩm của mỗi dòng khớp với dòng đơn
  - Kiểm tra số lượng nhận không vượt quá số lượng còn lại của đơn
  - Kiểm tra sản phẩm ACTIVE
  - Kiểm tra vị trí ACTIVE và cùng kho
  - Kiểm tra lô hợp lệ (nếu sản phẩm theo dõi lô)
  - Kiểm tra dòng QUARANTINE phải có ghi chú
  - Với mỗi dòng:
    - Tạo/cập nhật bản ghi Inventory
    - Tăng onHandQuantity
    - Tăng quarantineQuantity (nếu QUARANTINE)
    - Cập nhật dung lượng location đã sử dụng (nếu không phải quarantine)
    - Ghi nhận StockMovement (INBOUND)
    - Cập nhật quantityReceived cho PO line
  - Tính toán lại trạng thái PO (COMPLETED hoặc PARTIALLY_RECEIVED)
  - Đánh dấu phiếu nhập CONFIRMED (confirmedAt, confirmedBy)
- **Commit transaction**
- Gửi thông báo:
  - Quản lý Tồn kho: tồn kho đã tăng
  - Quản lý Mua hàng: cập nhật tiến độ đơn
- Trả về kết quả thành công

**Quy tắc nghiệp vụ:**
- BR-INB-08: Phiếu nhập phải tham chiếu đơn CONFIRMED
- BR-INB-09: Số phiếu nhập tự động tạo và duy nhất
- BR-INB-10: Thông tin lô bắt buộc cho sản phẩm requires_batch_tracking = true
- BR-INB-11: Phiếu nhập DRAFT có thể chỉnh sửa và xóa
- BR-INB-12: Chính sách nhập quá số lượng có thể cấu hình (mặc định: không cho phép)
- BR-INB-13: Xác nhận phiếu nhập là atomic (tất cả dòng hoặc không dòng nào)
- BR-INB-14: Cập nhật tồn kho và ghi nhận di chuyển tồn kho trong cùng transaction
- BR-INB-15: Trạng thái đơn cập nhật dựa trên số lượng đã nhận
- BR-INB-16: Phiếu nhập CONFIRMED không thể chỉnh sửa hoặc xóa

---

### Tính năng 3: Kiểm tra Chất lượng

**US-INB-09**: Là Nhân viên Kiểm tra chất lượng, tôi muốn đánh dấu dòng phiếu nhập là QUARANTINE nếu phát hiện vấn đề chất lượng, để tồn kho lỗi không được sử dụng.

**US-INB-10**: Là Quản lý Kho, tôi muốn được thông báo khi hàng bị cách ly, để có thể follow-up với nhà cung cấp.

**UC-INB-05: Cách ly Hàng nhận về**
- Trong quá trình tạo/sửa phiếu nhập
- Nhân viên kiểm tra hàng hóa
- Nhân viên xác định vấn đề (hư hỏng, sai sản phẩm, lỗi)
- Nhân viên đặt quality_status = QUARANTINE
- Nhân viên nhập ghi chú giải thích vấn đề
- Khi phiếu nhập được xác nhận:
  - Lô được tạo với trạng thái QUARANTINE
  - Tồn kho được tăng nhưng không tính vào available (theo trạng thái lô)
  - Thông báo gửi cho Quản lý Kho và Quản lý Mua hàng

**Quy tắc nghiệp vụ:**
- BR-INB-17: Hàng QUARANTINE tăng tồn kho nhưng không khả dụng để bán/xuất
- BR-INB-18: Vấn đề chất lượng phải được ghi lại trong ghi chú
- BR-INB-19: Quản lý Kho được thông báo về tất cả mục cách ly

---

### Tính năng 4: Nhập hàng Từng phần & Nhập quá số lượng

**US-INB-11**: Là Nhân viên Nhận hàng, tôi muốn nhập một phần khi đơn đầy đủ chưa có sẵn, để nhà cung cấp có thể giao nhiều lần.

**UC-INB-06: Nhập hàng Từng phần**
- Nhân viên tạo phiếu nhập cho đơn
- Nhân viên nhập số lượng nhận < số lượng đặt
- Hệ thống lưu dưới dạng nhập một phần
- Khi xác nhận:
  - Trạng thái đơn = PARTIALLY_RECEIVED
  - Dòng đơn theo dõi: đã đặt, đã nhận đến nay, còn lại
- Các phiếu nhập tương lai có thể tham chiếu cùng đơn cho đến khi nhận đủ

**UC-INB-07: Nhập quá số lượng**
- Nếu chính sách nhập quá số lượng cho phép, hệ thống hiển thị cảnh báo nhưng vẫn chấp nhận
- Nếu không cho phép, hệ thống từ chối với lỗi
- Tất cả nhập quá số lượng được ghi log và thông báo

**Quy tắc nghiệp vụ:**
- BR-INB-20: Nhiều phiếu nhập có thể tham chiếu cùng một đơn
- BR-INB-21: Tổng số lượng nhận không vượt quá số lượng đặt (trừ khi cho phép nhập quá)
- BR-INB-22: Đơn đánh dấu COMPLETED khi tất cả dòng đã nhận đủ
- BR-INB-23: Nhập quá số lượng có thể cấu hình (mặc định: không cho phép)
- BR-INB-24: Dung sai nhập quá có thể đặt (ví dụ: +10%)
- BR-INB-25: Tất cả nhập quá số lượng được ghi log và thông báo

---

## 🔗 Tóm tắt Ảnh hưởng API

### Inbound Receipts (`/api/v1/inbound-receipts`)

| Method | Endpoint | Mô tả | Vai trò |
|--------|----------|-------|-------|
| POST | /api/v1/inbound-receipts | Tạo phiếu nhập (DRAFT) từ đơn | ADMIN, MANAGER |
| GET | /api/v1/inbound-receipts | Danh sách phiếu nhập (filter, phân trang) | ADMIN, MANAGER |
| GET | /api/v1/inbound-receipts/{id} | Chi tiết phiếu nhập | ADMIN, MANAGER |
| GET | /api/v1/inbound-receipts/by-po/{purchaseOrderId} | Danh sách phiếu nhập theo đơn | ADMIN, MANAGER |
| PUT | /api/v1/inbound-receipts/{id} | Cập nhật phiếu nhập (chỉ DRAFT) | ADMIN, MANAGER |
| DELETE | /api/v1/inbound-receipts/{id} | Xóa phiếu nhập (chỉ DRAFT) | ADMIN, MANAGER |
| PUT | /api/v1/inbound-receipts/{id}/confirm | Xác nhận phiếu nhập → cập nhật tồn kho | ADMIN, MANAGER |

### Inbound Receipt Lines (`/api/v1/inbound-receipt-lines`)

| Method | Endpoint | Mô tả | Vai trò |
|--------|----------|-------|-------|
| POST | /api/v1/inbound-receipt-lines | Thêm dòng vào phiếu nhập | ADMIN, MANAGER |
| GET | /api/v1/inbound-receipt-lines?inboundReceiptId={id} | Danh sách dòng theo phiếu nhập | ADMIN, MANAGER |
| PUT | /api/v1/inbound-receipt-lines/{id} | Cập nhật dòng (chỉ DRAFT) | ADMIN, MANAGER |
| DELETE | /api/v1/inbound-receipt-lines/{id} | Xóa dòng (chỉ DRAFT) | ADMIN, MANAGER |

---

## 💾 Ảnh hưởng Database

Xem chi tiết schema tại [DB_MODULE_05_INBOUND.md](./DB_MODULE_05_INBOUND.md)

### Các Bảng Chính

#### inbound_receipts

**Trường chính:**
- `id` - UUID khóa chính
- `receipt_number` - Định danh duy nhất (GR-YYYY-NNNN)
- `purchase_order_id` - FK đến purchase_orders
- `warehouse_id` - Kho đích
- `status` - DRAFT, CONFIRMED, CANCELLED
- `receipt_date` - Ngày nhập
- `confirmed_at` - Thời điểm xác nhận
- `confirmed_by` - Người xác nhận
- `delivery_note_number` - Số phiếu giao hàng
- `notes` - Ghi chú
- `created_at`, `updated_at`, `created_by`, `updated_by` - Audit fields

#### inbound_receipt_lines

**Trường chính:**
- `id` - UUID khóa chính
- `inbound_receipt_id` - FK đến inbound_receipts
- `purchase_order_line_id` - FK đến purchase_order_lines
- `product_id` - FK đến products
- `batch_id` - FK đến batches (tạo trong quá trình xác nhận)
- `location_id` - Vị trí đặt hàng
- `line_number` - Số dòng
- `quantity_received` - Số lượng nhận
- `quality_status` - PASS, QUARANTINE
- `notes` - Ghi chú
- `created_at`, `updated_at`, `created_by`, `updated_by` - Audit fields

---

## 🔄 Điểm Tích hợp

### Với Module Inventory
- Xác nhận phiếu nhập: `increaseStock()` cho mỗi dòng
- Tăng onHandQuantity cho bản ghi inventory tương ứng
- Tăng quarantineQuantity nếu chất lượng là QUARANTINE
- Cập nhật dung lượng đã sử dụng của location (nếu không phải quarantine)

### Với Module Batch
- Nếu sản phẩm yêu cầu theo dõi lô, lô được tạo trong quá trình xác nhận phiếu nhập
- Trạng thái lô đặt thành QUARANTINE nếu quality_status = QUARANTINE

### Với Module Stock Movement
- Mỗi dòng phiếu nhập tạo stock movement với type = INBOUND
- Truy xuất đầy đủ từ Đơn → Phiếu nhập → Di chuyển

### Với Module Purchase Orders
- Cập nhật quantityReceived cho purchase_order_lines
- Tính toán lại trạng thái đơn (COMPLETED hoặc PARTIALLY_RECEIVED)
- Một đơn có thể có nhiều phiếu nhập

### Với Module Notification
- Đơn xác nhận: thông báo cho kho
- Phiếu nhập xác nhận: thông báo quản lý tồn kho, quản lý mua hàng
- Mục cách ly: thông báo quản lý kho, quản lý mua hàng

---

## 📊 Tóm tắt Quy tắc Nghiệp vụ

| Rule ID | Mô tả |
|---------|-------|
| BR-INB-01 | Số đơn tự động tạo và duy nhất |
| BR-INB-02 | Đơn phải có ít nhất một dòng |
| BR-INB-03 | Ngày giao dự kiến không được ở quá khứ |
| BR-INB-04 | Đơn DRAFT có thể chỉnh sửa/xóa |
| BR-INB-05 | Chỉ đơn DRAFT mới được xác nhận |
| BR-INB-06 | Đơn CONFIRMED không thể chỉnh sửa |
| BR-INB-07 | Kho được thông báo khi đơn xác nhận |
| BR-INB-08 | Phiếu nhập phải tham chiếu đơn CONFIRMED |
| BR-INB-09 | Số phiếu nhập tự động tạo và duy nhất |
| BR-INB-10 | Thông tin lô bắt buộc cho sản phẩm theo dõi lô |
| BR-INB-11 | Phiếu nhập DRAFT có thể chỉnh sửa/xóa |
| BR-INB-12 | Chính sách nhập quá số lượng có thể cấu hình |
| BR-INB-13 | Xác nhận phiếu nhập là atomic |
| BR-INB-14 | Cập nhật tồn kho trong cùng transaction |
| BR-INB-15 | Trạng thái đơn cập nhật theo số lượng nhận |
| BR-INB-16 | Phiếu nhập CONFIRMED bất biến |
| BR-INB-17 | Hàng QUARANTINE không khả dụng để bán |
| BR-INB-18 | Vấn đề chất lượng phải được ghi chú |
| BR-INB-19 | Quản lý được thông báo về hàng cách ly |
| BR-INB-20 | Nhiều phiếu nhập cho một đơn được phép |
| BR-INB-21 | Tổng nhận ≤ đặt (trừ khi cho phép nhập quá) |
| BR-INB-22 | Đơn hoàn thành khi nhận đủ |
| BR-INB-23 | Nhập quá số lượng có thể cấu hình |
| BR-INB-24 | Dung sai nhập quá có thể cấu hình |
| BR-INB-25 | Tất cả nhập quá số lượng được ghi log |

---

## ✅ Tiêu chí Chấp nhận

1. ✅ Có thể tạo và xác nhận đơn đặt hàng
2. ✅ Có thể tạo phiếu nhập kho đối chiếu với đơn đã xác nhận
3. ✅ Thông tin lô được ghi nhận cho sản phẩm theo dõi lô
4. ✅ Tồn kho được cập nhật atomic khi xác nhận phiếu nhập
5. ✅ Hỗ trợ nhập từng phần (nhiều phiếu nhập cho một đơn)
6. ✅ Kiểm tra chất lượng với khả năng cách ly
7. ✅ Xử lý nhập quá số lượng theo chính sách
8. ✅ Nhật ký kiểm toán đầy đủ (Đơn → Phiếu nhập → Tồn kho → Di chuyển)
9. ✅ Thông báo thời gian thực cho các bên liên quan

---

**Phiên bản tài liệu:** 1.0  
**Cập nhật lần cuối:** 03/04/2026  
**Trạng thái:** 🚧 Bản nháp - Chờ Review
