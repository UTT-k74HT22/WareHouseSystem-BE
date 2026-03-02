# 📋 EMPLOYEE MODULE — API DOCUMENTATION

> **Module:** Employee Management  
> **Base Path:** `/api/v1/employees`  
> **Ngày cập nhật:** 02/03/2026  
> **Trạng thái:** ✅ **100% Implemented (5/5 APIs)**

---

## 📌 TỔNG QUAN MODULE

Module Employee quản lý vòng đời nhân viên trong hệ thống WMS (Warehouse Management System).  
Mỗi nhân viên được liên kết với:
- **Account** (`accounts`) — thông tin đăng nhập (username/password)
- **UserProfile** (`user_profiles`) — thông tin cá nhân (họ tên, email, SĐT)
- **Employee** (`employees`) — thông tin vận hành WMS (mã NV, phòng ban, chức vụ, kho phụ trách)

### 🔐 Phân quyền

| Role | Quyền |
|------|-------|
| `ADMIN` | Toàn quyền (tạo, xem, sửa, xóa) |
| `MANAGER` | Chỉ xem danh sách nhân viên |

---

## 📊 DANH SÁCH API

| # | Method | Endpoint | Mô tả | Auth | Status |
|---|--------|----------|--------|------|--------|
| 1 | `POST` | `/api/v1/employees` | Tạo nhân viên mới | `ADMIN` | ✅ Done |
| 2 | `GET` | `/api/v1/employees` | Danh sách nhân viên (phân trang + filter) | `ADMIN`, `MANAGER` | ✅ Done |
| 3 | `GET` | `/api/v1/employees/{id}` | Chi tiết nhân viên theo ID | `ADMIN` | ✅ Done |
| 4 | `PUT` | `/api/v1/employees/{id}` | Cập nhật thông tin nhân viên | `ADMIN` | ✅ Done |
| 5 | `DELETE` | `/api/v1/employees/{id}` | Xóa mềm nhân viên (TERMINATED) | `ADMIN` | ✅ Done |

---

## 🔍 CHI TIẾT TỪNG API

---

### 1. `POST /api/v1/employees` — Tạo nhân viên mới

**Mô tả:** Tạo mới một nhân viên kho. Thao tác này tự động tạo `Account`, gán `Role`, tạo `UserProfile`, và tạo bản ghi `Employee` trong một transaction duy nhất.

**Phân quyền:** `ADMIN` only

#### Request Body

```json
{
  "username": "john.doe",
  "password": "Password@123",
  "role": "STAFF",
  "first_name": "John",
  "last_name": "Doe",
  "email": "john.doe@warehouse.com",
  "phone_number": "0912345678",
  "employee_code": "EMP-001",
  "department": "Warehouse",
  "position": "PICKER",
  "hire_date": "2026-01-15"
}
```

#### Request Fields

| Field | Type | Required | Validation | Mô tả |
|-------|------|----------|------------|-------|
| `username` | `string` | ✅ | 1–50 ký tự, unique | Tên đăng nhập |
| `password` | `string` | ✅ | ≥8 ký tự, gồm: chữ hoa, chữ thường, số, ký tự đặc biệt (`@$!%*?&`) | Mật khẩu |
| `role` | `enum` | ✅ | Giá trị hợp lệ: `ADMIN`, `MANAGER`, `STAFF` | Vai trò hệ thống |
| `first_name` | `string` | ✅ | max 50 ký tự | Tên |
| `last_name` | `string` | ✅ | max 50 ký tự | Họ |
| `email` | `string` | ✅ | định dạng email, max 100 ký tự | Email |
| `phone_number` | `string` | ❌ | 10–15 chữ số | Số điện thoại |
| `employee_code` | `string` | ✅ | max 20 ký tự, unique | Mã nhân viên (vd: `EMP-001`) |
| `department` | `string` | ❌ | max 100 ký tự | Phòng ban (vd: `Warehouse`, `Logistics`) |
| `position` | `string` | ❌ | max 100 ký tự | Chức vụ WMS (vd: `PICKER`, `PACKER`, `SUPERVISOR`) |
| `hire_date` | `date` | ❌ | định dạng `YYYY-MM-DD` | Ngày vào làm |

#### Response — `201 Created`

```json
{
  "success": true,
  "message": "Success",
  "data": {
    "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "account_id": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
    "employee_code": "EMP-001",
    "first_name": "John",
    "last_name": "Doe",
    "email": "john.doe@warehouse.com",
    "phone_number": "0912345678",
    "address": null,
    "date_of_birth": null,
    "department": "Warehouse",
    "position": "PICKER",
    "hire_date": "2026-01-15",
    "termination_date": null,
    "salary_grade": null,
    "warehouse_id": null,
    "status": "ACTIVE",
    "created_at": "2026-03-02T10:00:00",
    "updated_at": "2026-03-02T10:00:00"
  }
}
```

#### Business Rules

- `username` phải unique trong bảng `accounts`
- `employee_code` phải unique trong bảng `employees`
- `role` phải tồn tại trong bảng `roles`
- Nhân viên mới luôn có `status = ACTIVE`
- `warehouse_id` không được gán lúc tạo; dùng `PUT /{id}` để gán kho sau
- Toàn bộ thao tác được bọc trong **1 transaction** — nếu bất kỳ bước nào thất bại, rollback toàn bộ

#### Lỗi có thể xảy ra

| HTTP | Error Code | Mô tả |
|------|-----------|-------|
| `400` | `COM_005` | Username đã tồn tại |
| `400` | `EMP_002` | Employee code đã tồn tại |
| `404` | `ROLE_001` | Role không tồn tại |
| `400` | Validation | Các trường không hợp lệ (thiếu required, sai format) |

---

### 2. `GET /api/v1/employees` — Danh sách nhân viên (phân trang)

**Mô tả:** Lấy danh sách nhân viên với phân trang và bộ lọc. Mặc định chỉ trả về nhân viên `ACTIVE`.

**Phân quyền:** `ADMIN`, `MANAGER`

#### Query Parameters

| Param | Type | Required | Default | Mô tả |
|-------|------|----------|---------|-------|
| `keyword` | `string` | ❌ | `null` | Tìm kiếm theo `employee_code`, `department`, hoặc `position` (case-insensitive) |
| `status` | `string` | ❌ | `ACTIVE` | Lọc theo trạng thái: `ACTIVE`, `ON_LEAVE`, `TERMINATED` |
| `warehouseId` | `string` | ❌ | `null` | Lọc theo kho được phân công |
| `page` | `int` | ❌ | `0` | Số trang (0-based) |
| `size` | `int` | ❌ | `10` | Số bản ghi mỗi trang (tối đa 100) |
| `sort` | `string` | ❌ | `createdAt,desc` | Sắp xếp (ví dụ: `employeeCode,asc`) |

#### Ví dụ Request

```
GET /api/v1/employees?keyword=picker&status=ACTIVE&warehouseId=wh-001&page=0&size=10
```

#### Response — `200 OK`

```json
{
  "success": true,
  "message": "Success",
  "data": {
    "content": [
      {
        "id": "a1b2c3d4-...",
        "account_id": "b2c3d4e5-...",
        "employee_code": "EMP-001",
        "first_name": "John",
        "last_name": "Doe",
        "email": "john.doe@warehouse.com",
        "phone_number": "0912345678",
        "address": null,
        "date_of_birth": null,
        "department": "Warehouse",
        "position": "PICKER",
        "hire_date": "2026-01-15",
        "termination_date": null,
        "salary_grade": "L1",
        "warehouse_id": "wh-001",
        "status": "ACTIVE",
        "created_at": "2026-03-02T10:00:00",
        "updated_at": "2026-03-02T10:00:00"
      }
    ],
    "page": 0,
    "size": 10,
    "total_elements": 1,
    "total_pages": 1,
    "last": true
  }
}
```

#### Business Rules

- Nếu `status` không được truyền → mặc định lọc `ACTIVE`
- Nếu `status` không hợp lệ → trả về `400 Bad Request`
- `keyword` tìm kiếm đồng thời trên: `employee_code`, `department`, `position` (OR condition)
- `page` phải ≥ 0; `size` phải trong khoảng `1–100`
- Profile cá nhân (`first_name`, `last_name`, `email`, ...) được load batch từ `user_profiles` để tối ưu performance (tránh N+1 query)

#### Lỗi có thể xảy ra

| HTTP | Error Code | Mô tả |
|------|-----------|-------|
| `400` | `COM_001` | `page` âm, `size` không hợp lệ (≤0 hoặc >100), hoặc `status` không hợp lệ |

---

### 3. `GET /api/v1/employees/{id}` — Chi tiết nhân viên

**Mô tả:** Lấy thông tin đầy đủ của một nhân viên theo `id`.

**Phân quyền:** `ADMIN` only

#### Path Parameters

| Param | Type | Required | Mô tả |
|-------|------|----------|-------|
| `id` | `string (UUID)` | ✅ | ID của nhân viên |

#### Response — `200 OK`

```json
{
  "success": true,
  "message": "Success",
  "data": {
    "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "account_id": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
    "employee_code": "EMP-001",
    "first_name": "John",
    "last_name": "Doe",
    "email": "john.doe@warehouse.com",
    "phone_number": "0912345678",
    "address": "123 Main St, Hanoi",
    "date_of_birth": "1990-05-20",
    "department": "Warehouse",
    "position": "PICKER",
    "hire_date": "2026-01-15",
    "termination_date": null,
    "salary_grade": "L1",
    "warehouse_id": "wh-uuid-001",
    "status": "ACTIVE",
    "created_at": "2026-03-02T10:00:00",
    "updated_at": "2026-03-02T10:00:00"
  }
}
```

#### Lỗi có thể xảy ra

| HTTP | Error Code | Mô tả |
|------|-----------|-------|
| `404` | `EMP_001` | Không tìm thấy nhân viên với `id` tương ứng |

---

### 4. `PUT /api/v1/employees/{id}` — Cập nhật thông tin nhân viên

**Mô tả:** Cập nhật thông tin vận hành WMS/HR của nhân viên. Áp dụng **partial-update semantics** — chỉ các trường được gửi và có giá trị khác `null` mới được cập nhật.

> ⚠️ **Lưu ý:** API này chỉ cập nhật thông tin WMS/HR. Không thể thay đổi `username`, `password`, `email`, `first_name`, `last_name` qua endpoint này.

**Phân quyền:** `ADMIN` only

#### Path Parameters

| Param | Type | Required | Mô tả |
|-------|------|----------|-------|
| `id` | `string (UUID)` | ✅ | ID của nhân viên |

#### Request Body

```json
{
  "department": "Logistics",
  "position": "SUPERVISOR",
  "hire_date": "2026-01-15",
  "termination_date": null,
  "salary_grade": "L2",
  "warehouse_id": "wh-uuid-001"
}
```

#### Request Fields

| Field | Type | Required | Validation | Mô tả |
|-------|------|----------|------------|-------|
| `department` | `string` | ❌ | max 100 ký tự | Phòng ban |
| `position` | `string` | ❌ | max 100 ký tự | Chức vụ WMS |
| `hire_date` | `date` | ❌ | `YYYY-MM-DD` | Ngày vào làm |
| `termination_date` | `date` | ❌ | `YYYY-MM-DD` | Ngày nghỉ việc |
| `salary_grade` | `string` | ❌ | max 20 ký tự | Bậc lương (vd: `L1`, `L2`, `SENIOR`) |
| `warehouse_id` | `string (UUID)` | ❌ | Kho phải tồn tại và ở trạng thái `ACTIVE` | Phân công kho |

#### Response — `200 OK`

```json
{
  "success": true,
  "message": "Success",
  "data": {
    "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "account_id": "b2c3d4e5-...",
    "employee_code": "EMP-001",
    "first_name": "John",
    "last_name": "Doe",
    "email": "john.doe@warehouse.com",
    "phone_number": "0912345678",
    "address": "123 Main St, Hanoi",
    "date_of_birth": "1990-05-20",
    "department": "Logistics",
    "position": "SUPERVISOR",
    "hire_date": "2026-01-15",
    "termination_date": null,
    "salary_grade": "L2",
    "warehouse_id": "wh-uuid-001",
    "status": "ACTIVE",
    "created_at": "2026-03-02T10:00:00",
    "updated_at": "2026-03-02T10:30:00"
  }
}
```

#### Business Rules

- Tất cả các trường đều optional — chỉ gửi trường cần thay đổi
- Khi `warehouse_id` được cung cấp: hệ thống kiểm tra kho tồn tại và ở trạng thái `ACTIVE`
- Không thể thay đổi `status` qua API này — dùng `DELETE /{id}` để terminate

#### Lỗi có thể xảy ra

| HTTP | Error Code | Mô tả |
|------|-----------|-------|
| `404` | `EMP_001` | Không tìm thấy nhân viên |
| `400` | `EMP_006` | Warehouse không tồn tại hoặc không ở trạng thái `ACTIVE` |

---

### 5. `DELETE /api/v1/employees/{id}` — Xóa mềm nhân viên

**Mô tả:** Thực hiện "soft delete" bằng cách chuyển trạng thái nhân viên sang `TERMINATED`. Dữ liệu không bị xóa vật lý khỏi database.  
Nếu `termination_date` chưa được đặt, hệ thống tự động gán ngày hiện tại.

**Phân quyền:** `ADMIN` only

#### Path Parameters

| Param | Type | Required | Mô tả |
|-------|------|----------|-------|
| `id` | `string (UUID)` | ✅ | ID của nhân viên cần xóa |

#### Response — `200 OK`

```json
{
  "success": true,
  "message": "Success",
  "data": null
}
```

#### Business Rules

- Nhân viên đã có trạng thái `TERMINATED` không thể xóa lần nữa → `400 EMP_005`
- Nếu `termination_date` chưa được đặt → tự động set bằng ngày hiện tại (`LocalDate.now()`)
- Dữ liệu được giữ nguyên trong DB (soft delete)

#### Lỗi có thể xảy ra

| HTTP | Error Code | Mô tả |
|------|-----------|-------|
| `404` | `EMP_001` | Không tìm thấy nhân viên |
| `400` | `EMP_005` | Nhân viên đã bị terminate trước đó |

---

## 📐 DATA MODEL

### Employee Entity (`employees` table)

| Column | Type | Nullable | Mô tả |
|--------|------|----------|-------|
| `id` | `CHAR(36)` (UUID) | NOT NULL | Primary key |
| `account_id` | `CHAR(36)` | NOT NULL, UNIQUE | Liên kết tới `accounts.id` |
| `employee_code` | `VARCHAR(20)` | NOT NULL, UNIQUE | Mã nhân viên (vd: `EMP-001`) |
| `department` | `VARCHAR(100)` | NULL | Phòng ban |
| `position` | `VARCHAR(100)` | NULL | Chức vụ WMS |
| `hire_date` | `DATE` | NULL | Ngày vào làm |
| `termination_date` | `DATE` | NULL | Ngày nghỉ việc |
| `salary_grade` | `VARCHAR(20)` | NULL | Bậc lương |
| `warehouse_id` | `CHAR(36)` | NULL | Kho được phân công |
| `status` | `ENUM` | NOT NULL | `ACTIVE` / `ON_LEAVE` / `TERMINATED` |
| `created_at` | `DATETIME` | NOT NULL | Thời điểm tạo |
| `updated_at` | `DATETIME` | NOT NULL | Thời điểm cập nhật lần cuối |

### EmployeeStatus Enum

| Value | Mô tả |
|-------|-------|
| `ACTIVE` | Đang làm việc (mặc định) |
| `ON_LEAVE` | Đang nghỉ phép |
| `TERMINATED` | Đã nghỉ việc (soft deleted) |

---

## 🔗 QUAN HỆ LIÊN BẢNG

```
accounts (1) ──── (1) user_profiles     [personal info]
accounts (1) ──── (1) employees         [WMS operational info]
accounts (1) ──── (N) account_has_roles [role assignment]
employees   (N) ──── (1) warehouses     [warehouse assignment]
```

---

## ⚠️ LƯU Ý TRIỂN KHAI

### 1. API chưa có — Đề xuất bổ sung
Các API sau đây **chưa được triển khai** nhưng có thể cần trong tương lai:

| # | Method | Endpoint | Mô tả | Ưu tiên |
|---|--------|----------|--------|---------|
| 6 | `PATCH` | `/api/v1/employees/{id}/status` | Đổi trạng thái thủ công (`ACTIVE` ↔ `ON_LEAVE`) | 🟡 Medium |
| 7 | `GET` | `/api/v1/employees/warehouse/{warehouseId}` | Nhân viên theo kho (không phân trang) | 🟡 Medium |
| 8 | `GET` | `/api/v1/employees/code/{employeeCode}` | Tìm theo mã nhân viên | 🟢 Low |

### 2. Hạn chế hiện tại
- **Cập nhật thông tin cá nhân** (email, phone, address, date_of_birth): Chưa có endpoint riêng. Cần tạo `PUT /api/v1/employees/{id}/profile` hoặc tích hợp vào `UserProfile` API.
- **Đổi trạng thái `ON_LEAVE`**: Không có endpoint PATCH status; hiện chỉ có thể TERMINATE qua DELETE.
- **Tìm kiếm theo tên** (`first_name`, `last_name`): Query hiện tại chỉ tìm theo `employee_code`, `department`, `position` — không tìm được theo tên.

### 3. Performance
- Endpoint `GET /api/v1/employees` sử dụng **batch loading** UserProfile để tránh N+1 query.
- Mặc định sort theo `createdAt DESC`, tối đa 100 records/page.

---

## 🧪 VÍ DỤ CURL

### Tạo nhân viên mới
```bash
curl -X POST http://localhost:8080/api/v1/employees \
  -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john.doe",
    "password": "Password@123",
    "role": "STAFF",
    "first_name": "John",
    "last_name": "Doe",
    "email": "john.doe@warehouse.com",
    "phone_number": "0912345678",
    "employee_code": "EMP-001",
    "department": "Warehouse",
    "position": "PICKER",
    "hire_date": "2026-01-15"
  }'
```

### Lấy danh sách nhân viên ACTIVE trong kho
```bash
curl -X GET "http://localhost:8080/api/v1/employees?status=ACTIVE&warehouseId=wh-001&page=0&size=20" \
  -H "Authorization: Bearer <ADMIN_OR_MANAGER_TOKEN>"
```

### Phân công kho cho nhân viên
```bash
curl -X PUT http://localhost:8080/api/v1/employees/a1b2c3d4-e5f6-7890-abcd-ef1234567890 \
  -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "warehouse_id": "wh-uuid-001",
    "salary_grade": "L1"
  }'
```

### Terminate nhân viên
```bash
curl -X DELETE http://localhost:8080/api/v1/employees/a1b2c3d4-e5f6-7890-abcd-ef1234567890 \
  -H "Authorization: Bearer <ADMIN_TOKEN>"
```
