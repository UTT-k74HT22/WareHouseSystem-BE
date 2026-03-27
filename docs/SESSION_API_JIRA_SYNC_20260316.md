# Session Note - API/Jira Sync - 2026-03-16

## 1. Mục tiêu

- Đối chiếu trạng thái Jira với API đã implement thực tế trên `develop`
- Cập nhật `docs/API_INVENTORY_COMPREHENSIVE.md` để phản ánh đúng code chạy được
- Lưu session context để lần sau không phải rà lại toàn bộ repo từ đầu

## 2. Source of truth đã dùng trong session này

- Jira project: `WHS`
- Controller routes:
  - `src/main/java/org/demo/whs/controller/*.java`
- Service implementation:
  - `src/main/java/org/demo/whs/service/impl/*.java`
- Tài liệu mapping Jira:
  - `documents/Auth/RBAC_BA_Documentation.md`
  - `docs/08-api-inventory-implementation-plan.md`
  - `docs/PHASE5_BATCH_CANONICAL_SYNC_20260315.md`
  - `documents/Inbound/*.md`
  - `documents/Inventory/*.md`

## 3. Kết luận đã khóa

### 3.1 Batch

- `WHS-35`, `WHS-41`, `WHS-42`, `WHS-83`, `WHS-84`, `WHS-85`, `WHS-86` đều `Done`
- Code và docs khớp: Batch hiện `11/11`
- Không cần sync Jira thêm cho Batch ở thời điểm này

### 3.2 Inventory

- `WHS-10..16` đã `Done`, `WHS-17` vẫn chưa xong
- Public APIs đang có:
  - `GET /api/v1/inventories`
  - `GET /api/v1/inventories/summary/{productId}`
  - `GET /api/v1/inventories/by-location`
  - `POST /api/v1/inventories/check-availability`
  - `POST /api/v1/inventories/reserve`
  - `POST /api/v1/inventories/unreserve`
  - `POST /api/v1/inventories/increase`
- Kết luận: inventory hiện `7/8`, parent `WHS-19` giữ `In Progress` là đúng

### 3.3 Inbound

- `WHS-43..46`, `WHS-53..58` đều `Done`
- Purchase Orders, PO Lines, Inbound Receipts, Inbound Receipt Lines đã có controller + service thực thi
- Docs inventory hiện có thể tiếp tục dùng làm baseline cho triển khai outbound

### 3.4 Outbound

- `WHS-47..50`, `WHS-59..65` ✅ Đã hoàn thành (25/03/2026)
- `SalesOrdersController`, `SalesOrderLinesController`, `OutboundShipmentsController`, `OutboundShipmentLinesController` đã implement đầy đủ
- Sales Orders: 6/7 APIs (thiếu delete)
- Outbound Shipments: 8/9 APIs (thiếu pick-list PDF)
- Outbound Shipment Lines: 5/5 APIs

### 3.5 RBAC

- Jira parent/child liên quan:
  - `WHS-144..148`
  - `WHS-149..167`
- Bẫy lớn nhất của repo hiện tại:
  - controller đã tồn tại nên dễ nhìn nhầm là done
  - nhưng `PermissionServiceImpl` và `RoleServiceImpl` vẫn là stub
- Evidence:
  - `src/main/java/org/demo/whs/service/impl/PermissionServiceImpl.java`
  - `src/main/java/org/demo/whs/service/impl/RoleServiceImpl.java`
- Trạng thái thực:
  - `POST/GET/PUT/DELETE /api/v1/permissions` -> chưa implement thực thi
  - `POST/GET/PUT/DELETE /api/v1/roles` -> chưa implement thực thi
  - role-permission APIs -> chưa implement thực thi
  - user-role APIs -> chưa implement thực thi
  - `POST /api/v1/auth/check-permission` -> chưa có route
  - `GET /api/v1/auth/my-permissions` -> chưa có route
- Kết luận: RBAC phải được tính là `0/19`, Jira open state hiện hợp lý hơn docs cũ

## 4. File đã cập nhật

- `docs/API_INVENTORY_COMPREHENSIVE.md`
- `docs/SESSION_API_JIRA_SYNC_20260316.md`

## 5. Khi quay lại session sau, mở theo thứ tự này

1. `docs/SESSION_API_JIRA_SYNC_20260316.md`
2. `docs/API_INVENTORY_COMPREHENSIVE.md`
3. Nếu làm RBAC:
   - `documents/Auth/RBAC_BA_Documentation.md`
   - `src/main/java/org/demo/whs/controller/PermissionController.java`
   - `src/main/java/org/demo/whs/controller/RoleController.java`
   - `src/main/java/org/demo/whs/controller/UserRoleController.java`
   - `src/main/java/org/demo/whs/service/impl/PermissionServiceImpl.java`
   - `src/main/java/org/demo/whs/service/impl/RoleServiceImpl.java`
4. Nếu làm outbound:
   - `src/main/java/org/demo/whs/controller/SalesOrdersController.java`
   - `src/main/java/org/demo/whs/controller/SalesOrderLinesController.java`
   - `src/main/java/org/demo/whs/controller/OutboundShipmentsController.java`
   - `src/main/java/org/demo/whs/controller/OutboundShipmentLinesController.java`

## 6. Next recommended implementation order

1. Hoàn thiện RBAC service + repository + tests trước để đóng gap security/admin operations.
2. Chốt `inventory decrease` và boundary public/internal cho `increase`.
3. Harden inbound integration/regression tests.
4. Chỉ sau đó mới chuyển sang outbound foundation.
