# Bảng Quyết Định - Module Quản Lý Danh Mục Sản Phẩm

---

## 1. Tạo Danh Mục (POST /api/v1/categories)

| Điều Kiện / Test Case | TC01 | TC02 | TC03 | TC04 |
|------------------------|------|------|------|------|
| **C1**: code và name hợp lệ | T | T | T | T |
| **C2**: code không trùng | T | T | F | T |
| **C3**: name không rỗng | T | T | T | F |
| **A1**: 201 Created | X | X | | |
| **A2**: 400 Bad Request | | | X | X |
| **A3**: 409 Conflict | | | X | |

---

## 2. Cập Nhật Danh Mục (PUT /api/v1/categories/{id})

| Điều Kiện / Test Case | TC01 | TC02 | TC03 |
|------------------------|------|------|------|
| **C1**: ID tồn tại | T | T | T |
| **C2**: code không thay đổi | T | F | T |
| **C3**: name/description hợp lệ | T | T | T |
| **A1**: 200 OK | X | | X |
| **A2**: 400 Bad Request | | X | |

---

## 3. Xóa Danh Mục (DELETE /api/v1/categories/{id})

| Điều Kiện / Test Case | TC01 | TC02 |
|------------------------|------|------|
| **C1**: ID tồn tại | T | T |
| **C2**: Danh mục không có sản phẩm | T | F |
| **A1**: 200 OK | X | |
| **A2**: 400 Bad Request | | X |

---

## 4. Lấy Danh Sách (GET /api/v1/categories)

| Điều Kiện / Test Case | TC01 | TC02 |
|------------------------|------|------|
| **C1**: Tham số hợp lệ | T | T |
| **C2**: Có kết quả | T | - |
| **A1**: 200 OK + list | X | |
| **A4**: 200 OK + empty | | X |

---

## Chi Tiết Điều Kiện

| Điều Kiện | Mô Tả |
|-----------|-------|
| C1 | code và name có giá trị hợp lệ |
| C2 | code chưa tồn tại trong hệ thống |
| C3 | name không rỗng/null |
| C4 | category không có sản phẩm liên kết |

---

## Các Hành Động

| Hành Động | Mô Tả |
|-----------|-------|
| A1 | 200/201 OK - Thành công |
| A2 | 400 Bad Request - Lỗi validation |
| A3 | 409 Conflict - Code đã tồn tại |
| A4 | 200 OK - Kết quả rỗng |
