_# Thiết kế Test Case - Module Nhập Kho (Inbound)

---

## 1. API: Tạo Đơn mua hàng (Create Purchase Order) - POST /api/v1/purchase-orders

| Test Case ID | Description | Test Data | Test Steps | Expected Response | Test Result |
|--------------|-------------|-----------|------------|-------------------|-------------|
| TC01 | Tạo PO (DRAFT) thành công | supplierId="SUP-01", warehouseId="WHS-01", lines=[{productId: "P01", qty: 100}] | 1. POST http://localhost:8080/api/v1/purchase-orders<br>2. Body: JSON with supplier, warehouse and lines<br>3. Send request | 201 Created, status="DRAFT" | Pass |
| TC02 | Tạo PO thiếu line sản phẩm | lines=[] | 1. POST http://localhost:8080/api/v1/purchase-orders<br>2. Body: JSON with empty lines array<br>3. Send request | 400 Bad Request, "Lines cannot be empty" | Fail |
| TC03 | Tạo PO với NCC không tồn tại | supplierId="999" | 1. POST http://localhost:8080/api/v1/purchase-orders<br>2. Body: JSON with non-existent supplierId<br>3. Send request | 404 Not Found | Fail |
| TC04 | Tạo PO với số lượng âm | qty=-10 | 1. POST http://localhost:8080/api/v1/purchase-orders<br>2. Body: JSON with negative quantity in lines<br>3. Send request | 400 Bad Request | Fail |

---

## 2. API: Xác nhận Đơn mua hàng (Confirm PO) - PUT /api/v1/purchase-orders/{id}/confirm

| Test Case ID | Description | Test Data | Test Steps | Expected Response | Test Result |
|--------------|-------------|-----------|------------|-------------------|-------------|
| TC05 | Xác nhận PO thành công | poId="DRAFT_ID" | 1. PUT http://localhost:8080/api/v1/purchase-orders/{id}/confirm<br>2. Body: Empty or valid confirm data<br>3. Send request | 200 OK, status="CONFIRMED" | Pass |
| TC06 | Xác nhận PO đã xác nhận rồi | poId="CONFIRMED_ID" | 1. PUT http://localhost:8080/api/v1/purchase-orders/{id}/confirm<br>2. Body: Empty JSON<br>3. Send request | 400 Bad Request | Fail |

---

## 3. API: Tạo Phiếu nhập kho (Create Inbound Receipt) - POST /api/v1/inbound-receipts

| Test Case ID | Description | Test Data | Test Steps | Expected Response | Test Result |
|--------------|-------------|-----------|------------|-------------------|-------------|
| TC07 | Tạo phiếu nhập từ PO CONFIRMED | poId="CONFIRMED_ID" | 1. POST http://localhost:8080/api/v1/inbound-receipts<br>2. Body: JSON with poId<br>3. Send request | 201 Created, status="DRAFT" | Pass |
| TC08 | Tạo phiếu nhập từ PO DRAFT | poId="DRAFT_ID" | 1. POST http://localhost:8080/api/v1/inbound-receipts<br>2. Body: JSON with poId in DRAFT state<br>3. Send request | 400 Bad Request | Fail |

---

## 4. API: Xác nhận Thực nhập (Confirm Receipt) - PUT /api/v1/inbound-receipts/{id}/confirm

| Test Case ID | Description | Test Data | Test Steps | Expected Response | Test Result |
|--------------|-------------|-----------|------------|-------------------|-------------|
| TC09 | Xác nhận nhập kho - Tăng tồn kho | actualQty=100, locationId="LOC-01" | 1. PUT http://localhost:8080/api/v1/inbound-receipts/{id}/confirm<br>2. Body: JSON with actualQuantity and locationId<br>3. Send request | 200 OK, Stock Increased | Pass |
| TC10 | Xác nhận nhập kho thiếu Location | locationId=null | 1. PUT http://localhost:8080/api/v1/inbound-receipts/{id}/confirm<br>2. Body: JSON missing locationId<br>3. Send request | 400 Bad Request | Fail |
| TC11 | Nhập kho SP cần quản lý Lô (Batch) | batchNumber="LOT01" | 1. PUT http://localhost:8080/api/v1/inbound-receipts/{id}/confirm<br>2. Body: JSON with batchNumber and expiryDate<br>3. Send request | 200 OK, Batch inventory created | Pass |
| TC17 | Xác nhận Receipt không tồn tại | id="NON-EXISTENT-ID" | 1. PUT /api/v1/inbound-receipts/NON-EXISTENT-ID/confirm<br>2. Send request | 404 Not Found | Fail |
| TC18 | Xác nhận Receipt sai trạng thái (Đã COMPLETED) | id="COMPLETED-ID" | 1. Lấy 1 Receipt đã nhập xong<br>2. Gửi request confirm lại | 400 Bad Request, "Receipt already completed" | Fail |

---

## 5. Bảo mật & Logic nghiệp vụ (Security & Business Logic)

| Test Case ID | Description | Test Data | Test Steps | Expected Response | Test Result |
|--------------|-------------|-----------|------------|-------------------|-------------|
| TC12 | Nhân viên không có quyền tạo PO | Role: STAFF | 1. POST .../purchase-orders với STAFF token<br>2. Send request | 403 Forbidden | Fail |
| TC13 | Nhập kho vượt quá số lượng PO | poQty=100, actualQty=150 | 1. Confirm Receipt với thực nhập > đặt hàng | 400 Bad Request (tùy cấu hình) | Fail |
| TC14 | Tranh chấp khi 2 người cùng Confirm | ReceiptID: 100 | 1. Gửi đồng thời 2 request PUT tới /confirm<br>2. Kiểm tra kết quả | 1 Pass, 1 Fail (Optimistic Lock) | Fail |
| TC19 | Sản phẩm trong Receipt không thuộc PO gốc | Product: "PROD-999" (không có trong PO) | 1. Thêm SP lạ vào Receipt<br>2. Gửi request Confirm | 400 Bad Request, "Product mismatch" | Fail |
| TC20 | Xác nhận Receipt không có dòng sản phẩm | lines=[] | 1. Gửi request Confirm với danh sách SP trống | 400 Bad Request, "Receipt lines required" | Fail |
| TC21 | Hệ thống đang xử lý confirm (Inventory Lock) | ReceiptID: 100 | 1. Gửi request 1 (đang xử lý)<br>2. Gửi request 2 ngay lập tức | Request 2 bị reject hoặc đợi (theo cơ chế lock) | Fail |

---

## 6. Luồng Phục hồi & Tích hợp (Recovery Flow & Integration)

| Test Case ID | Description | Test Data | Test Steps | Expected Response | Test Result |
|--------------|-------------|-----------|------------|-------------------|-------------|
| TC22 | Luồng thử lại (Retry Flow) sau khi sửa lỗi | Data lỗi -> Fix -> OK | 1. Confirm với Location sai (Fail TC10)<br>2. Sửa lại Location đúng<br>3. Gửi lại request Confirm | 200 OK, Tồn kho tăng chính xác | Pass |
| TC15 | Kiểm tra Nhật ký biến động kho (Stock Movement) | ReceiptID: "RC01" | 1. Thực hiện TC09 thành công<br>2. Check DB bảng stock_movements | Có record type 'INBOUND' | Pass |
| TC16 | Kiểm tra Tồn kho khả dụng (Available Stock) | Product A | 1. So sánh tồn kho trước/sau TC09 | OnHand tăng đúng số lượng thực nhập | Pass |_
