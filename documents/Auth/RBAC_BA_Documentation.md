# RBAC Module - Technical Specification

## 1. Overview

### Mục tiêu
Module RBAC (Role-Based Access Control) cung cấp hệ thống phân quyền chi tiết theo vai trò, cho phép quản lý quyền truy cập của người dùng vào các tài nguyên và hành động trong hệ thống WMS.

### Phạm vi
- Quản lý Roles (tạo, sửa, xóa, xem)
- Quản lý Permissions (định nghĩa quyền theo Resource + Action)
- Gán Permissions vào Roles
- Gán Roles cho Users
- Middleware kiểm tra quyền trên API
- Audit log ghi nhận thay đổi

---

## 2. Actors & Roles

### Actors trong hệ thống
| Actor | Mô tả |
|-------|-------|
| System Admin | Quản lý toàn bộ hệ thống, có quyền cao nhất |
| Warehouse Manager | Quản lý kho, có quyền quản lý inventory, storage, stock transfers |
| Inventory Staff | Nhân viên kho, có quyền thao tác inventory, đọc product |
| Staff (Default) | Nhân viên mới, chỉ có quyền đọc cơ bản |

### Roles mặc định của hệ thống
| Role | isDefault | Mô tả |
|------|-----------|-------|
| admin | false | Full access - tất cả permissions |
| warehouse_manager | false | Quản lý kho - inventory, storage, stock-transfer full |
| inventory_staff | false | Nhân viên kho - inventory read/write, product read |
| staff | **true** | Nhân viên mặc định - basic read only |

---

## 3. Business Rules

### Quản lý Permission
1. `name` phải unique trong hệ thống (format: `{resource}.{action}`)
2. `resource` phải match với module đã có trong hệ thống
3. `action` chỉ nhận: GET, POST, PUT, DELETE
4. Chỉ xóa được permission không được gán vào role nào

### Quản lý Role
1. `name` phải unique trong hệ thống
2. Chỉ có một role là `isDefault = true` tại một thời điểm
3. Khi set `isDefault = true`, tự động set các role khác về false
4. Không xóa được role mặc định
5. Chỉ xóa được role không có user nào được gán

### Gán Role cho User
1. Thêm mới các role chưa có cho user (idempotent)
2. Không thu hồi được default role khi là role duy nhất
3. Khi thu hồi hết roles, tự động gán default role

### Authorization
1. Mỗi API phải có permission check trong middleware
2. User có thể có nhiều roles, permissions = union từ tất cả roles
3. Public endpoints (login, health check) được skip kiểm tra

---

## 4. Permission Matrix

### Modules trong hệ thống
Dựa trên các Controller hiện có: auth, batch, business-partner, category, email, employee, inventory, location, inbound-receipt, outbound-shipment, otp, product, purchase-order, sales-order, stock-adjustment, stock-movement, stock-transfer, storage, units-of-measure, user, warehouse

### Role × Module × Action Matrix
| Module | admin | warehouse_manager | inventory_staff | staff |
|--------|-------|-------------------|-----------------|-------|
| auth | CRUD | READ | READ | READ |
| batch | CRUD | CRUD | READ | READ |
| business-partner | CRUD | CRUD | READ | READ |
| category | CRUD | CRUD | READ | READ |
| email | CRUD | - | - | - |
| employee | CRUD | CRUD | - | - |
| inventory | CRUD | CRUD | CRU | R |
| location | CRUD | CRUD | R | R |
| inbound-receipt | CRUD | CRUD | R | R |
| outbound-shipment | CRUD | CRUD | R | R |
| product | CRUD | CRUD | R | R |
| purchase-order | CRUD | CRUD | R | R |
| sales-order | CRUD | CRUD | R | R |
| stock-adjustment | CRUD | CRUD | R | - |
| stock-movement | CRUD | CRUD | R | R |
| stock-transfer | CRUD | CRUD | - | - |
| storage | CRUD | CRUD | R | R |
| units-of-measure | CRUD | CRUD | R | R |
| user | CRUD | R | R | - |
| warehouse | CRUD | CRUD | R | R |

*CRUD = Tất cả hành động, CRU = Create/Read/Update, R = Read only*

---

## 5. API Endpoints List

> Jira issue keys không thể renumber sau khi tạo. Bảng dưới đây dùng số thứ tự nội bộ (`No.`) để renumber phạm vi delivery trong module RBAC, đồng thời phân biệt rõ `Legacy Spec Task` và `Execution Subtask`.

### Permission APIs
| No. | Method | Endpoint | Legacy Spec Task | Execution Subtask | Parent Task |
|-----|--------|----------|------------------|-------------------|-------------|
| 1.1 | POST | /api/v1/permissions | [WHS-143](https://dunghd-utt.atlassian.net/browse/WHS-143) | [WHS-150](https://dunghd-utt.atlassian.net/browse/WHS-150) | [WHS-144](https://dunghd-utt.atlassian.net/browse/WHS-144) |
| 1.2 | GET | /api/v1/permissions | [WHS-91](https://dunghd-utt.atlassian.net/browse/WHS-91) | [WHS-151](https://dunghd-utt.atlassian.net/browse/WHS-151) | [WHS-144](https://dunghd-utt.atlassian.net/browse/WHS-144) |
| 1.3 | GET | /api/v1/permissions/{id} | [WHS-92](https://dunghd-utt.atlassian.net/browse/WHS-92) | [WHS-152](https://dunghd-utt.atlassian.net/browse/WHS-152) | [WHS-144](https://dunghd-utt.atlassian.net/browse/WHS-144) |
| 1.4 | PUT | /api/v1/permissions/{id} | [WHS-93](https://dunghd-utt.atlassian.net/browse/WHS-93) | [WHS-153](https://dunghd-utt.atlassian.net/browse/WHS-153) | [WHS-144](https://dunghd-utt.atlassian.net/browse/WHS-144) |
| 1.5 | DELETE | /api/v1/permissions/{id} | [WHS-94](https://dunghd-utt.atlassian.net/browse/WHS-94) | [WHS-154](https://dunghd-utt.atlassian.net/browse/WHS-154) | [WHS-144](https://dunghd-utt.atlassian.net/browse/WHS-144) |

### Role APIs
| No. | Method | Endpoint | Legacy Spec Task | Execution Subtask | Parent Task |
|-----|--------|----------|------------------|-------------------|-------------|
| 2.1 | POST | /api/v1/roles | [WHS-95](https://dunghd-utt.atlassian.net/browse/WHS-95) | [WHS-155](https://dunghd-utt.atlassian.net/browse/WHS-155) | [WHS-145](https://dunghd-utt.atlassian.net/browse/WHS-145) |
| 2.2 | GET | /api/v1/roles | [WHS-96](https://dunghd-utt.atlassian.net/browse/WHS-96) | [WHS-156](https://dunghd-utt.atlassian.net/browse/WHS-156) | [WHS-145](https://dunghd-utt.atlassian.net/browse/WHS-145) |
| 2.3 | GET | /api/v1/roles/{id} | [WHS-97](https://dunghd-utt.atlassian.net/browse/WHS-97) | [WHS-157](https://dunghd-utt.atlassian.net/browse/WHS-157) | [WHS-145](https://dunghd-utt.atlassian.net/browse/WHS-145) |
| 2.4 | PUT | /api/v1/roles/{id} | [WHS-98](https://dunghd-utt.atlassian.net/browse/WHS-98) | [WHS-158](https://dunghd-utt.atlassian.net/browse/WHS-158) | [WHS-145](https://dunghd-utt.atlassian.net/browse/WHS-145) |
| 2.5 | DELETE | /api/v1/roles/{id} | [WHS-99](https://dunghd-utt.atlassian.net/browse/WHS-99) | [WHS-159](https://dunghd-utt.atlassian.net/browse/WHS-159) | [WHS-145](https://dunghd-utt.atlassian.net/browse/WHS-145) |

### Role-Permission APIs
| No. | Method | Endpoint | Legacy Spec Task | Execution Subtask | Parent Task |
|-----|--------|----------|------------------|-------------------|-------------|
| 3.1 | POST | /api/v1/roles/{id}/permissions | [WHS-100](https://dunghd-utt.atlassian.net/browse/WHS-100) | [WHS-160](https://dunghd-utt.atlassian.net/browse/WHS-160) | [WHS-146](https://dunghd-utt.atlassian.net/browse/WHS-146) |
| 3.2 | DELETE | /api/v1/roles/{id}/permissions/{permId} | [WHS-101](https://dunghd-utt.atlassian.net/browse/WHS-101) | [WHS-161](https://dunghd-utt.atlassian.net/browse/WHS-161) | [WHS-146](https://dunghd-utt.atlassian.net/browse/WHS-146) |
| 3.3 | GET | /api/v1/roles/{id}/permissions | [WHS-102](https://dunghd-utt.atlassian.net/browse/WHS-102) | [WHS-162](https://dunghd-utt.atlassian.net/browse/WHS-162) | [WHS-146](https://dunghd-utt.atlassian.net/browse/WHS-146) |

### User-Role APIs
| No. | Method | Endpoint | Legacy Spec Task | Execution Subtask | Parent Task |
|-----|--------|----------|------------------|-------------------|-------------|
| 4.1 | POST | /api/v1/users/{id}/roles | [WHS-103](https://dunghd-utt.atlassian.net/browse/WHS-103) | [WHS-163](https://dunghd-utt.atlassian.net/browse/WHS-163) | [WHS-147](https://dunghd-utt.atlassian.net/browse/WHS-147) |
| 4.2 | DELETE | /api/v1/users/{id}/roles/{roleId} | [WHS-104](https://dunghd-utt.atlassian.net/browse/WHS-104) | [WHS-164](https://dunghd-utt.atlassian.net/browse/WHS-164) | [WHS-147](https://dunghd-utt.atlassian.net/browse/WHS-147) |
| 4.3 | GET | /api/v1/users/{id}/roles | [WHS-105](https://dunghd-utt.atlassian.net/browse/WHS-105) | [WHS-165](https://dunghd-utt.atlassian.net/browse/WHS-165) | [WHS-147](https://dunghd-utt.atlassian.net/browse/WHS-147) |
| 4.4 | GET | /api/v1/roles/{id}/users | [WHS-106](https://dunghd-utt.atlassian.net/browse/WHS-106) | [WHS-166](https://dunghd-utt.atlassian.net/browse/WHS-166) | [WHS-147](https://dunghd-utt.atlassian.net/browse/WHS-147) |

### Auth/Check APIs
| No. | Method | Endpoint | Legacy Spec Task | Execution Subtask | Parent Task |
|-----|--------|----------|------------------|-------------------|-------------|
| 5.1 | POST | /api/v1/auth/check-permission | Doc slot `WHS-107` | [WHS-149](https://dunghd-utt.atlassian.net/browse/WHS-149) | [WHS-148](https://dunghd-utt.atlassian.net/browse/WHS-148) |
| 5.2 | GET | /api/v1/auth/my-permissions | Doc slot `WHS-108` | [WHS-167](https://dunghd-utt.atlassian.net/browse/WHS-167) | [WHS-148](https://dunghd-utt.atlassian.net/browse/WHS-148) |

---

## 6. Data Model

### ER Diagram
```
┌─────────────┐     ┌──────────────────┐     ┌─────────────┐
│    users    │     │    user_roles    │     │    roles    │
├─────────────┤     ├──────────────────┤     ├─────────────┤
│ id (PK)     │◄────│ user_id          │     │ id (PK)     │
│ username    │     │ role_id (FK)     │────►│ name        │
│ email       │     │ assigned_at      │     │ description │
│ ...         │     │ assigned_by      │     │ is_default  │
└─────────────┘     └──────────────────┘     │ created_at  │
                         ▲                   │ updated_at  │
                         │                   └─────────────┘
                         │
┌─────────────┐     ┌──────────────────────┐
│ permissions │     │   role_permissions   │
├─────────────┤     ├──────────────────────┤
│ id (PK)     │◄────│ role_id (FK)         │
│ name        │     │ permission_id (FK)   │
│ resource    │     └──────────────────────┘
│ action      │            ▲
│ description │            │
│ created_at  │            │
└─────────────┘            │
                          │
              ┌─────────────────────────┐
              │  permission_audit_log  │
              ├─────────────────────────┤
              │ id (PK)                 │
              │ user_id                 │
              │ action                  │
              │ entity_type             │
              │ entity_id               │
              │ old_value (JSON)        │
              │ new_value (JSON)        │
              │ timestamp               │
              └─────────────────────────┘
```

### Table Definitions

#### roles
| Column | Type | Constraints |
|--------|------|-------------|
| id | UUID | PK |
| name | VARCHAR(100) | UNIQUE, NOT NULL |
| description | VARCHAR(255) | |
| is_default | BOOLEAN | DEFAULT false |
| created_at | TIMESTAMP | NOT NULL |
| updated_at | TIMESTAMP | NOT NULL |

#### permissions
| Column | Type | Constraints |
|--------|------|-------------|
| id | UUID | PK |
| name | VARCHAR(100) | UNIQUE, NOT NULL |
| resource | VARCHAR(50) | NOT NULL |
| action | VARCHAR(20) | NOT NULL |
| description | VARCHAR(255) | |
| created_at | TIMESTAMP | NOT NULL |

#### role_permissions
| Column | Type | Constraints |
|--------|------|-------------|
| role_id | UUID | PK, FK(roles.id) |
| permission_id | UUID | PK, FK(permissions.id) |

#### user_roles
| Column | Type | Constraints |
|--------|------|-------------|
| user_id | UUID | PK, FK(users.id) |
| role_id | UUID | PK, FK(roles.id) |
| assigned_at | TIMESTAMP | NOT NULL |
| assigned_by | UUID | FK(users.id) |

#### permission_audit_log
| Column | Type | Constraints |
|--------|------|-------------|
| id | UUID | PK |
| user_id | UUID | FK(users.id) |
| action | VARCHAR(20) | NOT NULL |
| entity_type | VARCHAR(20) | NOT NULL |
| entity_id | UUID | NOT NULL |
| old_value | JSON | |
| new_value | JSON | |
| timestamp | TIMESTAMP | NOT NULL |

---

## 7. Flow Diagrams

### Login Flow
```
┌──────────┐     ┌─────────────┐     ┌─────────────┐     ┌──────────────┐
│  Client  │     │ AuthController│    │ AuthService │    │ UserRoleService│
└────┬─────┘     └──────┬──────┘     └──────┬──────┘     └───────┬───────┘
     │ POST /login      │                  │                  │
     │─────────────────►│                  │                  │
     │                  │ validate creds   │                  │
     │                  │────────────────►│                  │
     │                  │                  │ check user exist │
     │                  │                  │─────────────────►│
     │                  │                  │◄─────────────────│
     │                  │                  │ get user roles   │
     │                  │                  │─────────────────►│
     │                  │◄─────────────────│                  │
     │                  │                  │                  │
     │          generate JWT               │                  │
     │          (include role info)        │                  │
     │◄──────────────────│                  │                  │
     │ 200 OK + JWT     │                  │                  │
```

### Request Authorization Flow
```
┌──────────┐     ┌──────────────────┐     ┌─────────────┐     ┌───────────────┐
│  Client  │     │ AuthorizationFilter│   │ AuthService │    │PermissionService│
└────┬─────┘     └────────┬─────────┘     └──────┬──────┘     └───────┬───────┘
     │ PUT /inventory    │                      │                  │
     │ + JWT             │                      │                  │
     │──────────────────►│                      │                  │
     │                   │ extract JWT         │                  │
     │                   │ extract user + path │                  │
     │                   │ map path → resource │                  │
     │                   │ map method → action │                  │
     │                   │                      │ check permission │
     │                   │                      │─────────────────►│
     │                   │                      │◄─────────────────│
     │                   │              allowed?                   │
     │                   │◄─────────────────────│                  │
     │                   │          403 Forbidden                  │
     │◄──────────────────│                      │                  │
```

---

## 8. Error Codes

### Permission Errors
| Code | Description | HTTP Status |
|------|-------------|-------------|
| PERM_001 | Permission name đã tồn tại | 409 |
| PERM_002 | Invalid resource | 400 |
| PERM_003 | Invalid action | 400 |
| PERM_004 | Permission not found | 404 |
| PERM_005 | Permission đang được sử dụng bởi roles | 409 |
| PERM_006 | Permission not assigned to role | 404 |

### Role Errors
| Code | Description | HTTP Status |
|------|-------------|-------------|
| ROLE_001 | Role name đã tồn tại | 409 |
| ROLE_002 | Invalid permission ID | 400 |
| ROLE_003 | Role not found | 404 |
| ROLE_004 | Cannot unset default when only one role | 400 |
| ROLE_005 | Cannot delete default role | 400 |
| ROLE_006 | Role đang được gán cho users | 409 |

### User Errors
| Code | Description | HTTP Status |
|------|-------------|-------------|
| USER_001 | User not found | 404 |
| USER_002 | Role chưa được gán cho user | 404 |
| USER_003 | Cannot revoke default role when only one | 400 |

### Auth Errors
| Code | Description | HTTP Status |
|------|-------------|-------------|
| AUTH_001 | Unauthenticated | 401 |
| AUTH_002 | Unauthorized - No permission | 403 |
| AUTH_003 | Invalid resource/action format | 400 |

---

## 9. Edge Cases & Constraints

### Concurrent Updates
- Khi nhiều request đồng thời thay đổi cùng một role/permission, sử dụng optimistic locking
- Validate business rules sau khi merge changes

### Permission Inheritance
- User có thể có nhiều roles → permissions = union của tất cả roles
- Không có permission inheritance giữa các roles

### Default Role Behavior
- User mới đăng ký sẽ được gán default role tự động
- Khi thu hồi hết roles, default role được gán lại
- Không thể xóa default role nếu đó là role duy nhất

### Cache Strategy
- Permissions của user có thể cache trong token hoặc memory
- Cache invalidate khi có thay đổi role/permission

---

## 10. Open Questions

1. **Có cần hỗ trợ permission theo tenant/company không?**
   - Hiện tại thiết kế theo single-tenant
   - Cần confirm nếu multi-tenant

2. **Có cần permission theo warehouse scope không?**
   - Ví dụ: user chỉ có quyền xem kho A
   - Chưa trong scope V1

3. **Audit log retention policy?**
   - Lưu trữ bao lâu?
   - Cần archive hay delete?

4. **Có cần UI cho RBAC không?**
   - Hiện tại chỉ backend APIs
   - Cần confirm nếu cần frontend

5. **Permission seeding cho các modules hiện có?**
   - Cần confirm list permissions cần seed ban đầu

---

## 11. Jira Tasks Summary

### Active Delivery Parents
| No. | Parent Task | Active Child Scope | Story Points |
|-----|-------------|--------------------|--------------|
| 1 | [WHS-144](https://dunghd-utt.atlassian.net/browse/WHS-144) `[Permissions] API Completion` | WHS-150, WHS-151, WHS-152, WHS-153, WHS-154 | 8 |
| 2 | [WHS-145](https://dunghd-utt.atlassian.net/browse/WHS-145) `[Roles] API Completion` | WHS-155, WHS-156, WHS-157, WHS-158, WHS-159 | 12 |
| 3 | [WHS-146](https://dunghd-utt.atlassian.net/browse/WHS-146) `[RolePermissions] API Completion` | WHS-160, WHS-161, WHS-162 | 5 |
| 4 | [WHS-147](https://dunghd-utt.atlassian.net/browse/WHS-147) `[UserRoles] API Completion` | WHS-163, WHS-164, WHS-165, WHS-166 | 6 |
| 5 | [WHS-148](https://dunghd-utt.atlassian.net/browse/WHS-148) `[Authorization] API Completion` | WHS-149, WHS-167 | 4 |
| 6 | [WHS-168](https://dunghd-utt.atlassian.net/browse/WHS-168) `[RBAC Core] Infrastructure Completion` | WHS-169, WHS-170, WHS-171, WHS-172, WHS-173 | 21 |

### Legacy / Reference Tasks
| Type | Jira Tasks | Note |
|------|------------|------|
| Legacy specification tasks | WHS-143, WHS-91, WHS-92, WHS-93, WHS-94, WHS-95, WHS-96, WHS-97, WHS-98, WHS-99, WHS-100, WHS-101, WHS-102, WHS-103, WHS-104, WHS-105, WHS-106 | Giữ lại để tham chiếu specification chi tiết cho từng API. |
| Deprecated containers | WHS-138, WHS-139, WHS-140, WHS-141, WHS-142 | Đã được supersede bởi hierarchy active dùng WHS-144 đến WHS-148. |

### Epic
- **WHS-89** - RBAC - Role-Based Access Control Module

### Story Points Total: 56

---

*Document created: 2026-03-13*
*Last updated: 2026-03-13*
*Author: BA Team - WHS Project*
