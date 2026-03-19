# Tài liệu BA - Module 4: Quản lý Kho hàng (Inventory Management)
## Đặc tả Yêu cầu Nghiệp vụ

---

## 📋 Thông tin Tài liệu

| Thuộc tính | Giá trị |
|----------|-------|
| **Module** | Quản lý Kho hàng (Inventory Management) |
| **Phiên bản** | 1.0 |
| **Ngày** | 01/02/2026 |
| **Trạng thái** | Bản thảo (Draft) |
| **Tác giả** | Business Analyst |

---

## 📑 Mục lục

1. [Bối cảnh Nghiệp vụ](#bối-cảnh-nghiệp-vụ)
2. [Các Tác nhân & Vai trò](#các-tác-nhân--vai-trò)
3. [Tổng quan Module](#tổng-quan-module)
4. [Tính năng 1: Theo dõi Tồn kho Thời gian thực](#tính-năng-1-theo-dõi-tồn-kho-thời-gian-thực)
5. [Tính năng 2: Giữ chỗ Tồn kho (Stock Reservation)](#tính-năng-2-giữ-chỗ-tồn-kho-stock-reservation)
6. [Tính năng 3: Điều chỉnh Kho (Stock Adjustment)](#tính-năng-3-điều-chỉnh-kho-stock-adjustment)
7. [Tính năng 4: Điều chuyển giữa các vị trí (Stock Transfer Between Locations)](#tính-năng-4-điều-chuyển-giữa-các-vị-trí-stock-transfer-between-locations)
8. [Tính năng 5: Cảnh báo Tồn kho Thấp](#tính-năng-5-cảnh-báo-tồn-kho-thấp)
9. [Tóm tắt Tác động API](#tóm-tắt-tác-động-api)
10. [Tóm tắt Tác động Cơ sở Dữ liệu](#tóm-tắt-tác-động-cơ-sở-dữ-liệu)
11. [Các Điểm Tích hợp](#các-điểm-tích-hợp)

---

## 🎯 Bối cảnh Nghiệp vụ

### Các vấn đề hiện tại (Pain Points)

1. **Sai lệch Tồn kho**: Tồn kho thực tế không khớp với hồ sơ hệ thống, dẫn đến bán quá mức (overselling) hoặc hết hàng (stock-outs).
2. **Thiếu khả năng hiển thị thời gian thực**: Quản lý không thể xem mức tồn kho hiện tại giữa các vị trí trong thời gian thực.
3. **Phân bổ trùng lặp (Double Allocation)**: Cùng một lượng hàng được phân bổ cho nhiều đơn hàng, gây ra vấn đề trong việc hoàn tất đơn hàng.
4. **Kiểm kê thủ công**: Việc đếm số lượng thực tế tốn thời gian và có tỷ lệ lỗi cao.
5. **Nhầm lẫn vị trí**: Hàng hóa được lưu trữ nhưng vị trí không được theo dõi, lãng phí thời gian tìm kiếm.
6. **Thiếu hệ thống cảnh báo**: Chỉ phát hiện hết hàng khi không thể thực hiện đơn hàng của khách hàng.

### Giá trị Nghiệp vụ

✅ **Độ chính xác tồn kho trên 99%** - Loại bỏ sự khác biệt giữa kho thực tế và hệ thống.  
✅ **Hiển thị thời gian thực** - Xem tức thời mức tồn kho trên tất cả các kho và vị trí.  
✅ **Ngăn chặn bán quá mức (Overselling)** - Tồn kho được giữ chỗ (reserved) đảm bảo đơn hàng có thể được thực hiện.  
✅ **Tối ưu hóa lưu trữ** - Theo dõi chính xác nơi lưu trữ hàng hóa để lấy hàng nhanh hơn.  
✅ **Bổ sung hàng hóa chủ động** - Tự động cảnh báo khi tồn kho xuống dưới mức tối thiểu.  
✅ **Giảm thiểu tình trạng hết hàng** - Duy trì mức tồn kho tối ưu dựa trên các ngưỡng tối thiểu/tối đa.  

---

## 👥 Các Tác nhân & Vai trò

| Tác nhân | Mục tiêu | Quyền hạn |
|-------|------|-------------|
| **Quản lý Kho (Warehouse Manager)** | Giám sát tình trạng tồn kho tổng thể, phê duyệt điều chỉnh | Xem tất cả tồn kho, phê duyệt điều chỉnh kho |
| **Kiểm soát viên Kho (Inventory Controller)** | Duy trì hồ sơ tồn kho chính xác | Tạo/cập nhật tồn kho, thực hiện kiểm đếm |
| **Nhân viên Kho (Warehouse Staff)** | Xử lý nhập và xuất hàng | Xem tồn kho, cập nhật vị trí |
| **Quản lý Mua hàng (Purchasing Manager)** | Xác định nhu cầu đặt hàng lại | Xem mức tồn kho, nhận cảnh báo tồn kho thấp |
| **Quản lý Bán hàng (Sales Manager)** | Kiểm tra tình trạng sẵn có của sản phẩm | Xem tồn kho có sẵn (chỉ đọc) |

---

## 📦 Tổng quan Module

Quản lý Kho hàng là module cốt lõi theo dõi số lượng và vị trí của tất cả sản phẩm trong thời gian thực. Nó duy trì việc theo dõi riêng biệt cho tồn kho thực tế (on-hand), đã giữ chỗ (reserved) và có sẵn (available).

### Các Thực thể Chính (Core Entities)

1. **Tồn kho (Inventory)** - Hồ sơ kho theo tổ hợp sản phẩm-kho-vị trí-lô hàng.
2. **Điều chỉnh Kho (Stock Adjustments)** - Các hiệu chỉnh thủ công đối với tồn kho với quy trình phê duyệt.
3. **Điều chuyển Kho (Stock Transfers)** - Di chuyển hàng hóa giữa các vị trí trong cùng một kho.
4. **Cảnh báo Kho (Stock Alerts)** - Thông báo khi tồn kho thấp.

### Các Khái niệm Chính

- **Số lượng Thực tế (On-Hand Quantity)**: Tồn kho thực tế trong kho (tổng cộng).
- **Số lượng Giữ chỗ (Reserved Quantity)**: Tồn kho được phân bổ cho các đơn hàng đã xác nhận (chưa giao).
- **Số lượng Có sẵn (Available Quantity)**: Thực tế trừ đi giữ chỗ (có thể phân bổ cho đơn hàng mới).
- **Điều chỉnh Kho (Stock Adjustment)**: Hiệu chỉnh thủ công với lý do và phê duyệt.
- **Kiểm kê định kỳ (Cycle Counting)**: Đếm thường xuyên một nhóm nhỏ sản phẩm để đảm bảo tính chính xác.

### Phụ thuộc (Dependencies)

- **Phụ thuộc vào**: Dữ liệu Master (Kho, Vị trí, Sản phẩm, Lô hàng).
- **Được yêu cầu bởi**: Các module Nhập hàng (Inbound), Xuất hàng (Outbound), Báo cáo (Reporting).

---

## 📋 Tính năng 1: Theo dõi Tồn kho Thời gian thực

### User Stories

**US-INV-01**: Là một Quản lý Kho, tôi muốn xem mức tồn kho thời gian thực của bất kỳ sản phẩm nào trên tất cả các vị trí, để tôi có thể đưa ra quyết định sáng suốt về phân bổ kho.

**US-INV-02**: Là một Quản lý Bán hàng, tôi muốn kiểm tra số lượng có sẵn trước khi hứa hẹn giao hàng cho khách hàng, để tránh bán quá mức.

**US-INV-03**: Là một Kiểm soát viên Kho, tôi muốn thấy tồn kho được chia nhỏ theo lô và vị trí, để tôi có thể xác định hàng cũ nhất (FIFO) và tối ưu hóa việc lấy hàng.

**US-INV-04**: Là một Nhân viên Kho, tôi muốn kiểm tra nhanh xem một sản phẩm còn hàng tại một vị trí cụ thể hay không, để tôi có thể hướng dẫn người lấy hàng một cách hiệu quả.

---

### Use Cases

#### UC-INV-01: Xem Tóm tắt Tồn kho

**Mô tả ngắn gọn**: Hiển thị tồn kho tổng hợp trên tất cả các kho và vị trí cho một sản phẩm.

**Tác nhân chính**: Quản lý Kho, Quản lý Bán hàng, Kiểm soát viên Kho.

**Điều kiện tiên quyết**:
- Người dùng đã được xác thực.
- Người dùng có quyền VIEW_INVENTORY.

**Hậu điều kiện**:
- Tóm tắt tồn kho được hiển thị.

**Luồng chính**:
1. Người dùng mở Dashboard Tồn kho.
2. Người dùng tìm kiếm theo sản phẩm (SKU, tên, hoặc quét mã vạch).
3. Hệ thống hiển thị tóm tắt:
   - Tổng số lượng thực tế trên tất cả các vị trí.
   - Tổng số lượng đã giữ chỗ.
   - Tổng số lượng có sẵn.
   - Chia nhỏ theo kho.
   - Chia nhỏ theo vị trí trong từng kho.
   - Chia nhỏ theo lô (nếu có theo dõi theo lô).
   - Ngày/giờ biến động cuối cùng.
4. Người dùng có thể xem chi tiết vào kho/vị trí cụ thể.
5. Hệ thống hiển thị chế độ xem chi tiết với:
   - Mã vị trí cụ thể.
   - Số lượng tại vị trí đó.
   - Thông tin lô hàng (nếu áp dụng).
   - Số ngày kể từ lần biến động cuối cùng.

**Quy tắc Nghiệp vụ**:
- BR-INV-01: Có sẵn (Available) = Thực tế (On-Hand) - Giữ chỗ (Reserved).
- BR-INV-02: Dữ liệu thời gian thực phải chính xác trong vòng 1 giây.
- BR-INV-03: Hồ sơ tồn kho được tạo tự động khi nhập hàng lần đầu.

---

#### UC-INV-02: Truy vấn Tồn kho Có sẵn

**Mô tả ngắn gọn**: Kiểm tra xem có đủ tồn kho để hoàn tất đơn hàng hay không.

**Tác nhân chính**: Quản lý Bán hàng, Dịch vụ Xuất hàng (Hệ thống).

**Điều kiện tiên quyết**:
- Người dùng hoặc hệ thống cần kiểm tra tính sẵn có.
- Sản phẩm và kho đã được xác định.

**Hậu điều kiện**:
- Xác nhận tính sẵn có hoặc thông báo không đủ hàng.

**Luồng chính**:
1. Hệ thống nhận truy vấn tính sẵn có:
   - product_id
   - warehouse_id
   - requested_quantity
   - location_id (tùy chọn)
   - batch_id (tùy chọn)
2. Hệ thống truy vấn tồn kho:
   ```sql
   SELECT SUM(on_hand_quantity - reserved_quantity) AS available
   FROM inventory
   WHERE product_id = ? 
     AND warehouse_id = ?
     AND (location_id = ? OR ? IS NULL)
     AND (batch_id = ? OR ? IS NULL)
   ```
3. Hệ thống so sánh số lượng có sẵn và yêu cầu.
4. Hệ thống trả về kết quả:
   - `{ "available": true, "quantity_available": 150 }`
   - HOẶC `{ "available": false, "quantity_available": 50, "shortfall": 50 }`

**Quy tắc Nghiệp vụ**:
- BR-INV-04: Chỉ tính các lô có trạng thái AVAILABLE (loại trừ QUARANTINE, EXPIRED).
- BR-INV-05: Không cho phép tồn kho âm (được thực thi bằng các ràng buộc - constraints).

---

## 📋 Tính năng 2: Giữ chỗ Tồn kho (Stock Reservation)

### User Stories

**US-INV-05**: Là một Hệ thống, tôi muốn giữ chỗ tồn kho khi đơn hàng bán được xác nhận, để cùng một lượng hàng không bị phân bổ cho nhiều đơn hàng.

**US-INV-06**: Là một Quản lý Kho, tôi muốn xem số lượng đã giữ chỗ, để tôi hiểu tại sao số lượng có sẵn khác với số lượng thực tế.

**US-INV-07**: Là một Hệ thống, tôi muốn tự động giải phóng giữ chỗ nếu đơn hàng bị hủy, để hàng hóa trở lại trạng thái có sẵn.

---

### Use Cases

#### UC-INV-03: Giữ chỗ Tồn kho (Reserve Stock)

**Mô tả ngắn gọn**: Phân bổ tồn kho cho một đơn hàng bán đã xác nhận, ngăn chặn việc phân bổ cho nơi khác.

**Tác nhân chính**: Dịch vụ Xuất hàng (Hệ thống).

**Điều kiện tiên quyết**:
- Đơn hàng bán đã được xác nhận.
- Tồn kho có sẵn đủ số lượng.
- Lô hàng ở trạng thái AVAILABLE.

**Hậu điều kiện**:
- Số lượng giữ chỗ tăng lên.
- Số lượng có sẵn giảm xuống.
- Việc giữ chỗ được ghi lại trong nhật ký kiểm tra (audit trail).

**Luồng chính**:
1. Dịch vụ xuất hàng gọi `reserveStock(productId, warehouseId, locationId, batchId, quantity)`.
2. Hệ thống kiểm tra tồn kho có sẵn:
   ```sql
   SELECT on_hand_quantity - reserved_quantity AS available
   FROM inventory
   WHERE product_id = ? AND warehouse_id = ? 
     AND location_id = ? AND batch_id = ?
   FOR UPDATE; -- Khóa dòng để kiểm soát đồng thời (concurrency control)
   ```
3. Hệ thống xác thực: `available >= quantity`.
4. Nếu đủ:
   ```sql
   UPDATE inventory
   SET reserved_quantity = reserved_quantity + ?,
       updated_at = NOW()
   WHERE id = ?;
   ```
5. Hệ thống ghi nhận biến động kho:
   - movement_type = 'RESERVE'
   - quantity_change = +quantity (trên phần giữ chỗ)
6. Hệ thống hoàn tất giao dịch (commit transaction).
7. Hệ thống trả về thành công.

**Luồng thay thế**:
- A1: Không đủ hàng
  - Hệ thống trả về lỗi: `InsufficientStockException`
  - Giao dịch bị hoàn tác (rolled back).

**Luồng ngoại lệ**:
- E1: Giữ chỗ đồng thời (một đơn hàng khác vừa giữ chỗ cùng một lượng hàng)
  - Sai lệch phiên bản khóa lạc quan (Optimistic lock version mismatch).
  - Giao dịch được thử lại hoặc trả về lỗi xung đột.

**Quy tắc Nghiệp vụ**:
- BR-INV-06: Việc giữ chỗ là nguyên tử - atomic (tất cả hoặc không có gì cho mỗi đơn hàng).
- BR-INV-07: Số lượng giữ chỗ không được vượt quá số lượng thực tế.
- BR-INV-08: Việc giữ chỗ sử dụng khóa bi quan - pessimistic locking (`SELECT FOR UPDATE`).

---

#### UC-INV-04: Giải phóng Giữ chỗ (Unreserve)

**Mô tả ngắn gọn**: Giải phóng tồn kho đã giữ chỗ khi đơn hàng bị hủy hoặc việc giao hàng đã được xác nhận.

**Tác nhân chính**: Dịch vụ Xuất hàng (Hệ thống).

**Điều kiện tiên quyết**:
- Tồn kho đã được giữ chỗ trước đó.
- Đơn hàng đang bị hủy HOẶC việc giao hàng đã hoàn tất.

**Hậu điều kiện**:
- Số lượng giữ chỗ giảm xuống.
- Số lượng có sẵn tăng lên (nếu hủy) HOẶC số lượng thực tế giảm xuống (nếu đã giao).

**Luồng chính (Đơn hàng bị hủy)**:
1. Dịch vụ xuất hàng gọi `unreserveStock(...)`.
2. Hệ thống giảm reserved_quantity:
   ```sql
   UPDATE inventory
   SET reserved_quantity = reserved_quantity - ?,
       updated_at = NOW()
   WHERE id = ?;
   ```
3. Hệ thống ghi nhận biến động (type = 'UNRESERVE').
4. Hàng hóa trở lại trạng thái có sẵn.

**Luồng chính (Xác nhận Giao hàng)**:
1. Dịch vụ xuất hàng gọi `decreaseStock(...)`.
2. Hệ thống cập nhật cả thực tế và giữ chỗ:
   ```sql
   UPDATE inventory
   SET on_hand_quantity = on_hand_quantity - ?,
       reserved_quantity = reserved_quantity - ?,
       updated_at = NOW()
   WHERE id = ?;
   ```
3. Hệ thống ghi nhận biến động (type = 'OUTBOUND').

**Quy tắc Nghiệp vụ**:
- BR-INV-09: Không thể giải phóng nhiều hơn số lượng hiện đang giữ chỗ.
- BR-INV-10: Xác nhận giao hàng làm giảm cả số lượng thực tế VÀ giữ chỗ.

---

## 📋 Tính năng 3: Điều chỉnh Kho (Stock Adjustment)

### User Stories

**US-INV-08**: Là một Kiểm soát viên Kho, tôi muốn điều chỉnh số lượng tồn kho sau khi kiểm đếm thực tế, để hệ thống khớp với thực tế.

**US-INV-09**: Là một Quản lý Kho, tôi muốn phê duyệt các điều chỉnh kho trên một ngưỡng nhất định, để các sai lệch lớn được xem xét.

**US-INV-10**: Là một Kiểm toán viên, tôi muốn xem tất cả các điều chỉnh kho kèm theo lý do, để tôi có thể xác định các mô hình thất thoát hoặc hư hỏng.

---

### Use Cases

#### UC-INV-05: Tạo Điều chỉnh Kho

**Mô tả ngắn gọn**: Tạo một điều chỉnh kho thủ công để hiệu chỉnh các sai lệch tồn kho.

**Tác nhân chính**: Kiểm soát viên Kho.

**Điều kiện tiên quyết**:
- Người dùng có quyền CREATE_ADJUSTMENT.
- Việc kiểm đếm thực tế đã hoàn thành.
- Sai lệch đã được xác định.

**Hậu điều kiện**:
- Điều chỉnh kho được tạo ở trạng thái PENDING (Chờ duyệt).
- Nếu dưới ngưỡng phê duyệt, tự động phê duyệt và cập nhật tồn kho.
- Nếu trên ngưỡng, chờ quản lý phê duyệt.

**Luồng chính**:
1. Người dùng mở biểu mẫu Điều chỉnh Kho.
2. Người dùng nhập:
   - sản phẩm (tìm kiếm/quét)
   - kho
   - vị trí
   - lô (nếu có theo dõi theo lô)
   - số lượng hiện tại (từ hệ thống)
   - số lượng thực tế (từ kiểm đếm)
   - số lượng điều chỉnh (tính toán: thực tế - hiện tại)
   - lý do (HƯ HỎNG, MẤT CẮP, LỖI KIỂM ĐẾM, HẾT HẠN, KHÁC)
   - ghi chú (giải thích chi tiết)
3. Hệ thống xác thực đầu vào.
4. Hệ thống kiểm tra ngưỡng phê duyệt (có thể cấu hình, ví dụ: ±100 đơn vị hoặc giá trị $1000).
5. Nếu **dưới ngưỡng**:
   - Tạo điều chỉnh với trạng thái APPROVED (Đã duyệt).
   - Áp dụng vào tồn kho ngay lập tức.
   - Ghi vào nhật ký kiểm tra.
6. Nếu **trên ngưỡng**:
   - Tạo điều chỉnh với trạng thái PENDING_APPROVAL (Chờ phê duyệt).
   - Gửi thông báo cho Quản lý Kho.
   - Chờ phê duyệt.
7. Hệ thống trả về xác nhận.

**Quy tắc Nghiệp vụ**:
- BR-INV-11: Lý do điều chỉnh là bắt buộc.
- BR-INV-12: Các điều chỉnh trên ngưỡng yêu cầu quản lý phê duyệt.
- BR-INV-13: Ngưỡng phê duyệt mặc định: 100 đơn vị HOẶC giá trị > $1000.
- BR-INV-14: Các điều chỉnh âm (thất thoát) được làm nổi bật trong báo cáo.

---

#### UC-INV-06: Phê duyệt Điều chỉnh Kho

**Mô tả ngắn gọn**: Quản lý Kho xem xét và phê duyệt/từ chối các điều chỉnh kho đang chờ xử lý.

**Tác nhân chính**: Quản lý Kho.

**Điều kiện tiên quyết**:
- Điều chỉnh ở trạng thái PENDING_APPROVAL.
- Người dùng có quyền APPROVE_ADJUSTMENT.

**Hậu điều kiện**:
- Nếu được duyệt: Tồn kho được cập nhật, điều chỉnh được đánh dấu APPROVED.
- Nếu bị từ chối: Điều chỉnh được đánh dấu REJECTED, tồn kho không thay đổi.

**Luồng chính (Phê duyệt)**:
1. Quản lý mở danh sách Điều chỉnh Chờ xử lý.
2. Quản lý chọn điều chỉnh để xem xét.
3. Hệ thống hiển thị:
   - Chi tiết sản phẩm.
   - Số lượng hiện tại so với thực tế.
   - Lượng điều chỉnh.
   - Lý do và ghi chú từ người tạo.
   - Tác động giá trị tiền tệ.
4. Quản lý nhấn "Approve" (Phê duyệt).
5. Hệ thống áp dụng điều chỉnh vào tồn kho:
   ```sql
   UPDATE inventory
   SET on_hand_quantity = on_hand_quantity + adjustment_qty
   WHERE id = ?;
   ```
6. Hệ thống đánh dấu điều chỉnh là APPROVED.
7. Hệ thống ghi nhận biến động kho.
8. Hệ thống gửi xác nhận cho người yêu cầu.

**Luồng chính (Từ chối)**:
1-4. Tương tự như phê duyệt.
5. Quản lý nhấn "Reject" (Từ chối) và nhập lý do từ chối.
6. Hệ thống đánh dấu điều chỉnh là REJECTED.
7. Tồn kho không thay đổi.
8. Hệ thống thông báo cho người yêu cầu kèm lý do từ chối.

**Quy tắc Nghiệp vụ**:
- BR-INV-15: Chỉ các điều chỉnh PENDING mới có thể được phê duyệt/từ chối.
- BR-INV-16: Quản lý không thể phê duyệt các điều chỉnh do chính mình tạo ra.
- BR-INV-17: Lý do từ chối là bắt buộc.

---

## 📋 Tính năng 4: Điều chuyển giữa các vị trí (Stock Transfer Between Locations)

### User Stories

**US-INV-11**: Là một Nhân viên Kho, tôi muốn chuyển hàng từ vị trí này sang vị trí khác trong cùng một kho, để tôi có thể tối ưu hóa không gian lưu trữ.

**US-INV-12**: Là một Quản lý Kho, tôi muốn theo dõi tất cả các lần điều chuyển kho, để tôi có thể đảm bảo tính chính xác của vị trí.

---

### Use Cases

#### UC-INV-07: Tạo Điều chuyển Kho

**Mô tả ngắn gọn**: Di chuyển hàng hóa từ vị trí này sang vị trí khác trong cùng một kho.

**Tác nhân chính**: Nhân viên Kho.

**Điều kiện tiên quyết**:
- Người dùng có quyền TRANSFER_STOCK.
- Vị trí nguồn có đủ hàng.
- Vị trí đích tồn tại và đang hoạt động (ACTIVE).

**Hậu điều kiện**:
- Số lượng hàng giảm tại vị trí nguồn.
- Số lượng hàng tăng tại vị trí đích.
- Việc điều chuyển được ghi lại trong nhật ký kiểm tra.

**Luồng chính**:
1. Người dùng mở biểu mẫu Điều chuyển Kho.
2. Người dùng nhập:
   - sản phẩm (tìm kiếm/quét)
   - kho
   - từ_vị_trí (from_location)
   - đến_vị_trí (to_location)
   - lô (nếu có theo dõi theo lô)
   - số lượng
   - lý do (SẮP XẾP LẠI, CHUẨN BỊ LẤY HÀNG, QUÁ TẢI, KHÁC)
3. Hệ thống xác thực:
   - Nguồn có đủ hàng.
   - Nguồn ≠ Đích.
   - Cả hai vị trí trong cùng một kho.
4. Hệ thống tạo điều chuyển trong một giao dịch:
   ```sql
   -- Giảm tại nguồn
   UPDATE inventory
   SET on_hand_quantity = on_hand_quantity - ?
   WHERE product_id = ? AND warehouse_id = ? 
     AND location_id = ? AND batch_id = ?;
   
   -- Tăng hoặc tạo mới tại đích
   INSERT INTO inventory (...) VALUES (...)
   ON DUPLICATE KEY UPDATE 
     on_hand_quantity = on_hand_quantity + ?;
   ```
5. Hệ thống ghi nhận hai biến động kho:
   - Biến động 1: TRANSFER_OUT từ vị trí nguồn.
   - Biến động 2: TRANSFER_IN tới vị trí đích.
6. Hệ thống trả về thành công.

**Quy tắc Nghiệp vụ**:
- BR-INV-18: Việc điều chuyển phải trong cùng một kho (sử dụng quy trình khác cho điều chuyển liên kho).
- BR-INV-19: Không thể chuyển nhiều hơn số lượng có sẵn tại nguồn.
- BR-INV-20: Việc điều chuyển là nguyên tử (cả hai vị trí được cập nhật trong cùng một giao dịch).

---

## 📋 Tính năng 5: Cảnh báo Tồn kho Thấp

### User Stories

**US-INV-13**: Là một Quản lý Kho, tôi muốn được cảnh báo khi tồn kho xuống dưới mức tối thiểu, để tôi có thể bắt đầu đặt hàng lại.

**US-INV-14**: Là một Quản lý Mua hàng, tôi muốn xem danh sách các sản phẩm dưới điểm đặt hàng lại, để tôi có thể tạo đơn mua hàng.

---

### Use Cases

#### UC-INV-08: Tự động Phát hiện Tồn kho Thấp

**Mô tả ngắn gọn**: Hệ thống tự động phát hiện các sản phẩm dưới mức tồn kho tối thiểu và gửi cảnh báo.

**Tác nhân chính**: Hệ thống (Công việc lập lịch - Scheduled Job).

**Điều kiện tiên quyết**:
- Sản phẩm đã được cấu hình min_stock_level hoặc reorder_point.
- Công việc lập lịch được cấu hình để chạy định kỳ.

**Hậu điều kiện**:
- Các mặt hàng tồn kho thấp được xác định.
- Cảnh báo được gửi tới các bên liên quan.
- Dashboard được cập nhật.

**Luồng chính**:
1. Hệ thống chạy công việc lập lịch (ví dụ: mỗi 6 giờ).
2. Hệ thống truy vấn:
   ```sql
   SELECT 
     p.id, p.sku, p.name,
     p.min_stock_level, p.reorder_point,
     SUM(i.on_hand_quantity) AS current_stock
   FROM products p
   INNER JOIN inventory i ON p.id = i.product_id
   WHERE p.status = 'ACTIVE'
   GROUP BY p.id, p.sku, p.name, p.min_stock_level, p.reorder_point
   HAVING current_stock < p.reorder_point;
   ```
3. Đối với mỗi sản phẩm tồn kho thấp:
   - Tính toán số lượng đặt hàng khuyến nghị:
     `order_qty = max_stock_level - current_stock`
   - Xác định mức độ khẩn cấp:
     - NGHIÊM TRỌNG (CRITICAL): kho < min_stock_level
     - CẢNH BÁO (WARNING): kho < reorder_point
4. Hệ thống gửi email tổng hợp cho Quản lý Mua hàng.
5. Hệ thống cập nhật các bộ đếm trên dashboard.
6. Hệ thống ghi nhật ký phát hiện.

**Quy tắc Nghiệp vụ**:
- BR-INV-21: Điểm đặt hàng lại thường được đặt ở mức đủ cho 2 tuần nhu cầu trung bình.
- BR-INV-22: Mức tồn kho tối thiểu là tồn kho an toàn (ví dụ: 1 tuần nhu cầu).
- BR-INV-23: Cảnh báo tồn kho thấp được gửi tối đa một lần mỗi ngày cho mỗi sản phẩm.

---

## 🔗 Tóm tắt Tác động API

### Các Endpoint Mới

| Phương thức | Endpoint | Mô tả | Vai trò yêu cầu |
|--------|----------|-------------|---------------|
| GET | /api/inventory | Liệt kê tồn kho với bộ lọc | VIEWER |
| GET | /api/inventory/summary/{productId} | Xem tóm tắt tồn kho cho sản phẩm | VIEWER |
| GET | /api/inventory/by-location | Xem tồn kho nhóm theo vị trí | VIEWER |
| POST | /api/inventory/check-availability | Kiểm tra xem hàng còn không | VIEWER |
| POST | /api/inventory/reserve | Giữ chỗ tồn kho (nội bộ) | SYSTEM |
| POST | /api/inventory/unreserve | Giải phóng giữ chỗ | SYSTEM |
| POST | /api/inventory/increase | Tăng kho (từ nhập hàng) | SYSTEM |
| POST | /api/inventory/decrease | Giảm kho (từ xuất hàng) | SYSTEM |
| POST | /api/stock-adjustments | Tạo điều chỉnh kho | INVENTORY_CONTROLLER |
| GET | /api/stock-adjustments | Liệt kê các điều chỉnh | INVENTORY_CONTROLLER |
| PUT | /api/stock-adjustments/{id}/approve | Phê duyệt điều chỉnh | WAREHOUSE_MANAGER |
| PUT | /api/stock-adjustments/{id}/reject | Từ chối điều chỉnh | WAREHOUSE_MANAGER |
| POST | /api/stock-transfers | Tạo điều chuyển kho | WAREHOUSE_STAFF |
| GET | /api/stock-transfers | Liệt kê các lần điều chuyển | WAREHOUSE_STAFF |
| GET | /api/inventory/low-stock | Xem các mặt hàng tồn kho thấp | PURCHASING_MANAGER |

---

## 💾 Tóm tắt Tác động Cơ sở Dữ liệu

Xem chi tiết schema tại [DB_MODULE_04_INVENTORY.md](./DB_MODULE_04_INVENTORY.md)

### Các Bảng Mới

#### Bảng: inventory

Bảng chính theo dõi kho ở cấp độ sản phẩm-kho-vị trí-lô hàng.

**Các trường chính**:
- `id` - UUID
- `product_id` - Khóa ngoại tới products
- `warehouse_id` - Khóa ngoại tới warehouses
- `location_id` - Khóa ngoại tới locations (có thể null)
- `batch_id` - Khóa ngoại tới batches (có thể null)
- `on_hand_quantity` - Kho thực tế
- `reserved_quantity` - Đã phân bổ cho đơn hàng
- `version` - Khóa lạc quan (Optimistic locking)
- `last_movement_at` - Dấu thời gian giao dịch cuối cùng

**Khóa duy nhất (Unique Key)**: (product_id, warehouse_id, location_id, batch_id)

#### Bảng: stock_adjustments

Ghi lại các điều chỉnh kho thủ công.

**Các trường chính**:
- `adjustment_number` - Mã định danh dễ đọc
- `status` - PENDING_APPROVAL, APPROVED, REJECTED
- `reason` - DAMAGE, THEFT, COUNT_ERROR, v.v.
- `adjustment_quantity` - Có thể dương hoặc âm
- `approved_by`, `approved_at` - Theo dõi phê duyệt

#### Bảng: stock_transfers

Ghi lại việc di chuyển giữa các vị trí.

**Các trường chính**:
- `transfer_number`
- `from_location_id`
- `to_location_id`
- `quantity`
- `reason`

---

## 🔄 Các Điểm Tích hợp

### Với Module Nhập hàng (Inbound)
- Xác nhận phiếu nhập kho (Inbound receipt confirmation) sẽ gọi hàm `increaseStock()`.
- **Chống trùng lặp (Idempotency)**: Sử dụng `reference_type` (INBOUND_RECEIPT) và `reference_number` để đảm bảo mỗi phiếu nhập chỉ được xử lý tăng kho duy nhất một lần.
- **Cơ chế khóa (Locking)**: Áp dụng khóa phân tán Redisson để ngăn chặn tình trạng Race Condition (tranh chấp dữ liệu) khi có nhiều yêu cầu xác nhận cùng lúc.
- Cập nhật bản ghi tồn kho và ghi nhật ký biến động kho (Stock movement logs) một cách nguyên tử (atomic) trong cùng một giao dịch.
- Số lượng tồn kho thực tế (On-hand) được tăng lên tương ứng.

### Với Module Xuất hàng (Outbound)
- Xác nhận đơn hàng bán gọi `reserveStock()`.
- Tạo lô hàng có thể kiểm tra tính sẵn có.
- Xác nhận giao hàng gọi `decreaseStock()`.
- Cả tồn kho thực tế và giữ chỗ đều giảm.

### Với Module Lô hàng (Batch)
- Tồn kho được theo dõi theo từng lô cho các sản phẩm có quản lý lô.
- Trạng thái lô hàng (QUARANTINE, EXPIRED) sẽ loại trừ hàng khỏi số lượng có sẵn.

### Với Module Biến động Kho (Stock Movement)
- Mỗi thay đổi tồn kho đều tạo ra một hồ sơ biến động kho.
- Cung cấp nhật ký kiểm tra (audit trail) đầy đủ.

### Với Module Thông báo (Notification)
- Cảnh báo tồn kho thấp được gửi cho Quản lý Mua hàng.
- Cảnh báo hết hàng được gửi cho Quản lý Kho.
- Các yêu cầu phê duyệt điều chỉnh sẽ thông báo cho quản lý.

---

## 📊 Tóm tắt Quy tắc Nghiệp vụ

| ID Quy tắc | Mô tả |
|---------|-------------|
| BR-INV-01 | Có sẵn = Thực tế - Giữ chỗ |
| BR-INV-02 | Độ chính xác dữ liệu thời gian thực trong vòng 1 giây |
| BR-INV-03 | Hồ sơ tồn kho tự động tạo khi nhập hàng lần đầu |
| BR-INV-04 | Chỉ tính các lô AVAILABLE trong tồn kho có sẵn |
| BR-INV-05 | Không cho phép tồn kho âm |
| BR-INV-06 | Việc giữ chỗ là nguyên tử (tất cả hoặc không có gì) |
| BR-INV-07 | Giữ chỗ không được vượt quá thực tế |
| BR-INV-08 | Giữ chỗ sử dụng khóa bi quan (pessimistic locking) |
| BR-INV-09 | Không thể giải phóng giữ chỗ nhiều hơn mức đã giữ |
| BR-INV-10 | Giao hàng làm giảm cả thực tế và giữ chỗ |
| BR-INV-11 | Lý do điều chỉnh là bắt buộc |
| BR-INV-12 | Điều chỉnh trên ngưỡng yêu cầu phê duyệt |
| BR-INV-13 | Ngưỡng mặc định: 100 đơn vị HOẶC giá trị $1000 |
| BR-INV-14 | Điều chỉnh âm được làm nổi bật trong báo cáo |
| BR-INV-15 | Chỉ các điều chỉnh PENDING mới có thể được phê duyệt/từ chối |
| BR-INV-16 | Quản lý không thể phê duyệt điều chỉnh của chính mình |
| BR-INV-17 | Lý do từ chối là bắt buộc |
| BR-INV-18 | Chỉ điều chuyển trong cùng một kho |
| BR-INV-19 | Không thể điều chuyển nhiều hơn số lượng có sẵn |
| BR-INV-20 | Việc điều chuyển là nguyên tử |
| BR-INV-21 | Điểm đặt hàng lại = 2 tuần nhu cầu trung bình |
| BR-INV-22 | Kho tối thiểu = 1 tuần nhu cầu (kho an toàn) |
| BR-INV-23 | Cảnh báo tồn kho thấp tối đa 1 lần/ngày/sản phẩm |

---

## ✅ Tiêu chí Chấp nhận (Acceptance Criteria)

1. ✅ Tồn kho thời gian thực hiển thị trên tất cả các kho và vị trí.
2. ✅ Tồn kho có sẵn được tính toán chính xác (thực tế - giữ chỗ).
3. ✅ Giữ chỗ tồn kho ngăn chặn việc phân bổ trùng lặp.
4. ✅ Điều chỉnh trên ngưỡng yêu cầu phê duyệt.
5. ✅ Điều chuyển kho cập nhật cả hai vị trí một cách nguyên tử.
6. ✅ Cảnh báo tồn kho thấp được gửi khi dưới điểm đặt hàng lại.
7. ✅ Xử lý được các vấn đề đồng thời (concurrency) - không làm mất dữ liệu cập nhật.
8. ✅ Tất cả thay đổi tồn kho được ghi lại trong nhật ký kiểm tra.

---

**Phiên bản Tài liệu:** 1.0  
**Cập nhật lần cuối:** 01/02/2026  
**Trạng thái:** 🚧 Bản thảo - Đang chờ xem xét
