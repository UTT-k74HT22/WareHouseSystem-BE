# Thiết kế Test Case - Module Quản lý Lô hàng (Batch)

---

## 1. API: Tạo Lô hàng (Create Batch) - POST /api/v1/batches

| Test Case ID | Description | Test Data | Test Steps | Expected Response | Test Result |
|--------------|-------------|-----------|------------|-------------------|-------------|
| TC01 | Tạo lô hàng mới thành công | productId="P01", batchNo="LOT01", expDate="2027-01-01" | 1. POST http://localhost:8080/api/v1/batches<br>2. Body: Valid JSON<br>3. Send request | 201 Created, status="ACTIVE" | Pass |
| TC02 | Tạo trùng số lô cho cùng một sản phẩm | productId="P01", batchNo="LOT01" | 1. Tạo trùng mã lô cho cùng 1 SP<br>2. Send request | 400 Bad Request / 409 Conflict | Fail |

---

## 2. API: Lấy thông tin Lô hàng (Read Batch) - GET /api/v1/batches

| Test Case ID | Description | Test Data | Test Steps | Expected Response | Test Result |
|--------------|-------------|-----------|------------|-------------------|-------------|
| TC03 | Lấy danh sách lô hàng có phân trang | page=0, size=10 | 1. GET /api/v1/batches?page=0&size=10<br>2. Send request | 200 OK, trả về list batch kèm metadata phân trang | Pass |
| TC04 | Tìm kiếm lô hàng theo mã (Batch Number) | batchNumber="LOT01" | 1. GET /api/v1/batches?search=LOT01<br>2. Send request | 200 OK, trả về đúng lô hàng cần tìm | Pass |
| TC05 | Lấy chi tiết một lô hàng theo ID | id="BATCH-001" | 1. GET /api/v1/batches/BATCH-001<br>2. Send request | 200 OK, trả về đầy đủ thông tin lô hàng | Pass |
| TC06 | Lấy chi tiết lô hàng không tồn tại | id="999-NOT-FOUND" | 1. GET /api/v1/batches/999-NOT-FOUND<br>2. Send request | 404 Not Found | Fail |

---

## 3. API: Cập nhật Lô hàng (Update Batch) - PUT/PATCH /api/v1/batches/{id}

| Test Case ID | Description | Test Data | Test Steps | Expected Response | Test Result |
|--------------|-------------|-----------|------------|-------------------|-------------|
| TC07 | Cập nhật thông tin lô hàng (Hạn sử dụng) | expiryDate="2028-12-31" | 1. PUT /api/v1/batches/{id}<br>2. Body: {"expiryDate": "2028-12-31"}<br>3. Send request | 200 OK, thông tin được cập nhật | Pass |
| TC08 | Cập nhật trạng thái lô hàng (Status) | status="QUARANTINE" | 1. PATCH /api/v1/batches/{id}/status<br>2. Body: {"status": "QUARANTINE"}<br>3. Send request | 200 OK, status được cập nhật | Pass |

---

## 4. API: Xóa Lô hàng (Delete Batch) - DELETE /api/v1/batches/{id}

| Test Case ID | Description | Test Data | Test Steps | Expected Response | Test Result |
|--------------|-------------|-----------|------------|-------------------|-------------|
| TC09 | Xóa lô hàng chưa có phát sinh giao dịch | batchId="LOT-NEW" | 1. DELETE /api/v1/batches/LOT-NEW<br>2. Send request | 200 OK, Xóa thành công (thường là Xóa mềm) | Pass |
| TC10 | Xóa lô hàng đã có tồn kho hoặc giao dịch | batchId="LOT-IN-USE" | 1. Chọn lô hàng đang có hàng trong kho<br>2. Thực hiện DELETE | 400 Bad Request, "Cannot delete batch with inventory/history" | Fail |

---

## 5. Logic Nghiệp vụ nâng cao (Business Logic)

| Test Case ID | Description | Test Data | Test Steps | Expected Response | Test Result |
|--------------|-------------|-----------|------------|-------------------|-------------|
| TC11 | Tự động chuyển trạng thái Hết hạn (EXPIRED) | system_date > expDate | 1. Giả lập ngày hệ thống vượt hạn lô hàng<br>2. Kiểm tra trạng thái lô | Status tự động chuyển sang EXPIRED | Pass |
| TC12 | Ưu tiên xuất lô hết hạn trước (FEFO) | Batch A (2026) < Batch B (2027) | 1. Thực hiện Xuất kho tự động (Auto-pick)<br>2. Kiểm tra lô hàng được chọn | Hệ thống gợi ý Batch A (Hạn gần hơn) | Pass |
| TC13 | Chặn xuất lô đang Biệt trữ (QUARANTINE) | status="QUARANTINE" | 1. Thực hiện Xuất kho<br>2. Chọn lô đang bị khóa để kiểm định | 400 Bad Request, "Batch is in quarantine" | Fail |
