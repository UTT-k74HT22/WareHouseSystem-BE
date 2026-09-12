# Thiết kế Test Case - Module Quản lý Danh mục sản phẩm (Category)

---

## 1. API: Tạo Danh mục (Create Category) - POST /api/v1/categories

| Test Case ID | Description | Test Data | Test Steps | Expected Response | Test Result |
|--------------|-------------|-----------|------------|-------------------|-------------|
| TC01 | Tạo danh mục mới thành công | code="ELEC", name="Điện tử" | 1. POST http://localhost:8080/api/v1/categories<br>2. Body: {"code": "ELEC", "name": "Điện tử"}<br>3. Send request | 201 Created, status="ACTIVE" | Pass |
| TC02 | Mã danh mục tự động viết hoa | code="appl" | 1. Tạo danh mục với mã viết thường<br>2. Send request | 201 Created, code="APPL" | Pass |
| TC03 | Tạo danh mục trùng mã (Code) | code="ELEC" (đã có) | 1. POST JSON trùng mã đã tồn tại<br>2. Send request | 400 Bad Request / 409 Conflict | Fail |
| TC04 | Tạo danh mục thiếu tên bắt buộc | name="" | 1. POST JSON không có tên danh mục<br>2. Send request | 400 Bad Request | Fail |

---

## 2. API: Lấy thông tin Danh mục (Read Category) - GET /api/v1/categories

| Test Case ID | Description | Test Data | Test Steps | Expected Response | Test Result |
|--------------|-------------|-----------|------------|-------------------|-------------|
| TC05 | Lấy danh sách danh mục có phân trang | page=0, size=10 | 1. GET /api/v1/categories?page=0&size=10<br>2. Send request | 200 OK, trả về list kèm metadata phân trang | Pass |
| TC06 | Tìm kiếm danh mục theo mã hoặc tên | search="Điện" | 1. GET /api/v1/categories?search=Điện<br>2. Send request | 200 OK, trả về các bản ghi phù hợp | Pass |
| TC07 | Lấy chi tiết danh mục theo ID | id="CAT-001" | 1. GET /api/v1/categories/CAT-001<br>2. Send request | 200 OK, trả về chi tiết danh mục | Pass |
| TC08 | Lấy danh mục không tồn tại | id="NON-EXIST" | 1. GET /api/v1/categories/NON-EXIST<br>2. Send request | 404 Not Found | Fail |

---

## 3. API: Cập nhật Danh mục (Update Category) - PUT/PATCH /api/v1/categories/{id}

| Test Case ID | Description | Test Data | Test Steps | Expected Response | Test Result |
|--------------|-------------|-----------|------------|-------------------|-------------|
| TC09 | Cập nhật thông tin thành công | name="Điện tử gia dụng" | 1. PUT /api/v1/categories/{id}<br>2. Body: {"name": "..."}<br>3. Send request | 200 OK, dữ liệu được cập nhật | Pass |
| TC10 | Không cho phép thay đổi mã (Code) | code="NEW-CODE" | 1. PUT /api/v1/categories/{id}<br>2. Body: {"code": "NEW-CODE"}<br>3. Send request | 200 OK nhưng mã cũ giữ nguyên (hoặc 400) | Pass |
| TC11 | Thay đổi trạng thái danh mục | status="INACTIVE" | 1. PATCH /api/v1/categories/{id}/status<br>2. Body: {"status": "INACTIVE"}<br>3. Send request | 200 OK, status được cập nhật | Pass |

---

## 4. API: Xóa Danh mục (Delete Category) - DELETE /api/v1/categories/{id}

| Test Case ID | Description | Test Data | Test Steps | Expected Response | Test Result |
|--------------|-------------|-----------|------------|-------------------|-------------|
| TC12 | Xóa danh mục chưa có sản phẩm | id="EMPTY-CAT" | 1. DELETE /api/v1/categories/EMPTY-CAT<br>2. Send request | 200 OK, Xóa thành công (hoặc xóa mềm) | Pass |
| TC13 | Chặn xóa danh mục đang có sản phẩm | id="CAT-WITH-PROD" | 1. Chọn danh mục đang chứa sản phẩm thực tế<br>2. Thực hiện lệnh DELETE | 400 Bad Request, "Category is in use" | Fail |

---

## 5. Logic Nghiệp vụ & Bảo mật (Business Logic & Security)

| Test Case ID | Description | Test Data | Test Steps | Expected Response | Test Result |
|--------------|-------------|-----------|------------|-------------------|-------------|
| TC14 | Kiểm tra phân quyền (RBAC) | Role: STAFF | 1. Dùng token nhân viên gọi API POST/PUT/DELETE<br>2. Send request | 403 Forbidden | Fail |
| TC15 | Chặn vô hiệu hóa danh mục khi có SP | status="INACTIVE" | 1. Thử PATCH status sang INACTIVE cho danh mục đang có SP<br>2. Send request | 400 Bad Request, "Cannot deactivate in-use category" | Fail |
| TC16 | Sản phẩm không được gán vào danh mục INACTIVE | Category: INACTIVE | 1. Thực hiện tạo mới/cập nhật Sản phẩm<br>2. Chọn một danh mục đang bị khóa | 400 Bad Request, "Category is inactive" | Fail |
