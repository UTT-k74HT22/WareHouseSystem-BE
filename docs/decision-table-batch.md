# Bảng Quyết Định - Module Quản Lý Lô Hàng

---

## 1. Tạo Lô Hàng (POST /api/v1/batches)

| Điều Kiện / Test Case | TC01 | TC02 | TC03 | TC04 |
|------------------------|------|------|------|------|
| **C1**: productId và batchNo hợp lệ | T | T | T | T |
| **C2**: batchNo không trùng cho cùng SP | T | F | T | T |
| **C3**: expDate > mfgDate | T | T | F | T |
| **C4**: batchNo đúng định dạng | T | T | T | F |
| **A1**: 201 Created | X | | | |
| **A2**: 400 Bad Request | | X | X | X |
| **A3**: 409 Conflict | | X | | |

---

## 2. Cập Nhật Lô Hàng (PUT /api/v1/batches/{id})

| Điều Kiện / Test Case | TC01 | TC02 |
|------------------------|------|------|
| **C1**: ID tồn tại | T | T |
| **C2**: Thông tin cập nhật hợp lệ | T | T |
| **A1**: 200 OK | X | X |
| **A2**: 400 Bad Request | | |

---

## 3. Lấy Thông Tin Lô Hàng (GET /api/v1/batches)

| Điều Kiện / Test Case | TC01 | TC02 | TC03 | TC04 |
|------------------------|------|------|------|------|
| **C1**: Tham số hợp lệ | T | T | T | T |
| **C2**: Batch tồn tại | T | T | T | F |
| **A1**: 200 OK + list | X | X | | |
| **A1**: 200 OK + detail | | | X | |
| **A4**: 404 Not Found | | | | X |

---

## 4. Xóa Lô Hàng (DELETE /api/v1/batches/{id})

| Điều Kiện / Test Case | TC01 | TC02 |
|------------------------|------|------|
| **C1**: ID tồn tại | T | T |
| **C2**: Không có tồn kho/lịch sử | T | F |
| **A1**: 200 OK | X | |
| **A2**: 400 Bad Request | | X |

---

## Chi Tiết Điều Kiện

| Điều Kiện | Mô Tả |
|-----------|-------|
| C1 | productId và batchNumber có giá trị hợp lệ |
| C2 | batchNumber chưa tồn tại cho cùng sản phẩm |
| C3 | expiryDate lớn hơn manufactureDate |
| C4 | batchNumber đúng định dạng (không có ký tự đặc biệt) |
| C5 | Batch có tồn kho hoặc lịch sử giao dịch |

---

## Các Hành Động

| Hành Động | Mô Tả |
|-----------|-------|
| A1 | 200/201 OK - Thành công |
| A2 | 400 Bad Request - Lỗi validation |
| A3 | 409 Conflict - Batch number đã tồn tại |
| A4 | 404 Not Found - Không tìm thấy tài nguyên |