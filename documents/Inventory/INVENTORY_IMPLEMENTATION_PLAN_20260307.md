# Inventory Module Implementation Plan

Updated: 2026-03-07

## 1. Objective

Hoàn chỉnh Module Inventory theo target-state đã chốt trong:

- `documents/Inventory/BA_DB_API_MODULE_04_INVENTORY_OPERATIONS_REDESIGN_V2.md`
- `documents/Inventory/INVENTORY_CONTROLLER_MODULE_REVIEW_20260307.md`

Mục tiêu của plan này:

1. Hoàn tất toàn bộ 8 API của inventory module.
2. Giữ boundary rõ giữa inventory utility APIs và business document workflows.
3. Không tạo regression cho `WHS-10`, `WHS-11`, `WHS-12`.
4. Chuẩn bị sẵn stock utility layer để inbound/outbound/sales order dùng lại.

---

## 2. Scope baseline

### 2.1 Đã hoàn thành trên `develop`

| Jira | API | Trạng thái |
|---|---|---|
| `WHS-10` | `GET /api/v1/inventories` | Done |
| `WHS-11` | `GET /api/v1/inventories/summary/{productId}` | Done |
| `WHS-12` | `GET /api/v1/inventories/by-location` | Done |

### 2.2 Chưa hoàn thành

| Jira | API | Trạng thái |
|---|---|---|
| `WHS-13` | `POST /api/v1/inventories/check-availability` | In Review |
| `WHS-14` | `POST /api/v1/inventories/reserve` | In Progress |
| `WHS-15` | `POST /api/v1/inventories/unreserve` | To Do |
| `WHS-16` | `POST /api/v1/inventories/increase` | To Do |
| `WHS-17` | `POST /api/v1/inventories/decrease` | To Do |

---

## 3. Target API set

| Method | Endpoint | Business meaning | Stock side effect |
|---|---|---|---|
| `GET` | `/api/v1/inventories` | Query inventory detail | No |
| `GET` | `/api/v1/inventories/summary/{productId}` | Product-level stock summary | No |
| `GET` | `/api/v1/inventories/by-location` | Location grouping for operations | No |
| `POST` | `/api/v1/inventories/check-availability` | Pre-check allocate feasibility | No |
| `POST` | `/api/v1/inventories/reserve` | Hold stock for outbound demand | `reserved +` |
| `POST` | `/api/v1/inventories/unreserve` | Release held stock | `reserved -` |
| `POST` | `/api/v1/inventories/increase` | Add on-hand stock | `on_hand +` |
| `POST` | `/api/v1/inventories/decrease` | Reduce on-hand stock | `on_hand -`, optionally `reserved -` |

---

## 4. Design rules that implementation must follow

1. `InventoryController` chỉ làm input validation, request mapping, response wrapping.
2. Business logic phải nằm ở `InventoryService` và `@Transactional` trong service layer.
3. Mọi stock-changing API phải ghi `stock_movements` trong cùng transaction.
4. `available = on_hand - reserved` phải là công thức duy nhất toàn hệ thống.
5. Không phá contract của `WHS-10`, `WHS-11`, `WHS-12`.
6. Không trộn logic của `StockAdjustments` hoặc `StockTransfers` vào inventory generic APIs.
7. Tất cả API stock mutation phải có `reference_type`, `reference_id`, `reference_number` hoặc equivalent audit context.
8. Với stock mutation, ưu tiên lock pessimistic trên inventory row; kết hợp optimistic version để chống stale update.
9. Nếu API có thể bị gọi lặp bởi upstream workflow, phải thiết kế idempotency hoặc ít nhất reservation uniqueness strategy trước khi productionize.

---

## 5. Implementation phases

## Phase 0 - Stabilize current read-side and PR correction

Goal:

1. Merge `WHS-13` theo hướng additive-only.
2. Giữ nguyên `WHS-10..12`.

Tasks:

1. Sửa PR `#74` để:
   - không rewrite `summary`
   - không rewrite `by-location`
   - không đổi mapper strategy của list endpoint
2. Implement `check-availability` theo rule:
   - validate `productId`
   - validate `warehouseId`/`locationId` nếu có
   - aggregate `on_hand`, `reserved`
   - `is_available = available >= requested_quantity`
3. Viết unit tests + integration tests cho `WHS-13`.

Deliverable:

1. `WHS-13` Done
2. Không có regression trên inventory read APIs

---

## Phase 1 - Introduce shared inventory mutation contract

Goal:

1. Tạo bộ DTO và service contract dùng chung cho `reserve`, `unreserve`, `increase`, `decrease`.

Tasks:

1. Thêm request/response DTOs:
   - `ReserveInventoryRequest`
   - `UnreserveInventoryRequest`
   - `IncreaseInventoryRequest`
   - `DecreaseInventoryRequest`
2. Chuẩn hóa field chung:
   - `inventory_id` hoặc dimension tuple
   - `quantity`
   - `reference_type`
   - `reference_id`
   - `reference_number`
   - `notes`
3. Chốt error mapping:
   - `INV_001` inventory not found
   - `INV_004` insufficient available stock
   - bổ sung `INV_002` unreserve exceeds reserved nếu project chưa có
   - bổ sung `INV_003` invalid inventory dimension nếu cần cho increase/find-or-create

Deliverable:

1. DTO contract ổn định
2. Swagger contract draft
3. No controller implementation yet nếu cần tách commit nhỏ

---

## Phase 2 - Implement `reserve`

Goal:

1. Hoàn tất `WHS-14`.

Business rule:

1. Lock inventory row.
2. `available >= quantity`.
3. `reserved_quantity = reserved_quantity + quantity`.
4. `on_hand` không đổi.
5. Ghi movement `RESERVE`.

Tasks:

1. Add service method `reserve(...)`.
2. Add controller endpoint `POST /api/v1/inventories/reserve`.
3. Resolve actor/audit fields.
4. Add movement mapper support for `RESERVE`.
5. Add tests:
   - success
   - insufficient stock
   - inventory not found
   - concurrent reserve
   - duplicate/idempotency baseline behavior

Deliverable:

1. `WHS-14` Done

---

## Phase 3 - Implement `unreserve`

Goal:

1. Hoàn tất `WHS-15`.

Business rule:

1. Lock inventory row.
2. `reserved_quantity >= quantity`.
3. `reserved_quantity = reserved_quantity - quantity`.
4. `on_hand` không đổi.
5. Ghi movement `UNRESERVE`.

Tasks:

1. Add service method `unreserve(...)`.
2. Add controller endpoint.
3. Add tests:
   - success
   - unreserve vượt reserved
   - inventory not found
   - concurrent unreserve

Deliverable:

1. `WHS-15` Done

---

## Phase 4 - Implement `increase`

Goal:

1. Hoàn tất `WHS-16`.

Business rule:

1. Dùng cho luồng inbound hoặc stock add.
2. `quantity > 0`.
3. Tăng `on_hand`.
4. Không đổi `reserved`.
5. Nếu inventory row chưa tồn tại:
   - create mới nếu dimension hợp lệ và business cho phép
   - hoặc fail rõ ràng nếu dimension invalid
6. Ghi movement `INBOUND` hoặc mapped movement theo `reference_type`.

Tasks:

1. Add `findOrCreate` strategy theo dimension.
2. Add service method `increase(...)`.
3. Add controller endpoint.
4. Add tests:
   - increase existing row
   - create new row when allowed
   - invalid dimension
   - concurrent create same dimension

Deliverable:

1. `WHS-16` Done

---

## Phase 5 - Implement `decrease`

Goal:

1. Hoàn tất `WHS-17`.

Business rule:

1. Dùng cho outbound/shipment confirm.
2. `quantity > 0`.
3. Không cho `on_hand` âm.
4. Nếu decrease trên stock đã reserve cho shipment:
   - giảm cả `on_hand` và `reserved`
5. Nếu decrease non-reserved stock:
   - chỉ giảm `on_hand`
   - vẫn phải đảm bảo `on_hand_after >= reserved`
6. Ghi movement `OUTBOUND` hoặc mapped movement theo `reference_type`.

Tasks:

1. Chốt request flag/strategy:
   - `consume_reserved`
   - hoặc derive từ `reference_type`
2. Add service method `decrease(...)`.
3. Add controller endpoint.
4. Add tests:
   - decrease with reserved consumption
   - decrease without reserved consumption
   - insufficient stock
   - reserved violation
   - concurrent decrease

Deliverable:

1. `WHS-17` Done

---

## Phase 6 - Hardening and integration readiness

Goal:

1. Hoàn chỉnh quality gate để inventory module sẵn sàng cho inbound/outbound integration.

Tasks:

1. Review authorization matrix:
   - read-side: authenticated
   - utility mutations: authenticated + role policy
   - internal-only flows nếu cần thì giới hạn rõ
2. Review `stock_movements` coverage:
   - reserve
   - unreserve
   - increase
   - decrease
3. Add integration tests for:
   - arithmetic consistency
   - DB constraints
   - stale update / concurrent update
4. Update docs:
   - API docs
   - sequence diagrams
   - module progress docs

Deliverable:

1. `WHS-19` có thể đóng khi toàn bộ child tasks done

---

## 6. Proposed implementation order by commit / PR slice

1. PR-A: Fix and merge `WHS-13` only
2. PR-B: Shared DTOs + service contract for inventory mutations
3. PR-C: `reserve`
4. PR-D: `unreserve`
5. PR-E: `increase`
6. PR-F: `decrease`
7. PR-G: hardening docs/tests/progress sync

Lý do:

1. Giữ mỗi PR nhỏ, đúng một business capability.
2. Dễ review và rollback.
3. Không trộn read-side refactor với stock mutation rollout.

---

## 7. Repository and service changes expected

### 7.1 Service layer

`InventoryService`

1. `checkAvailability(CheckAvailabilityRequest request)`
2. `reserve(ReserveInventoryRequest request)`
3. `unreserve(UnreserveInventoryRequest request)`
4. `increase(IncreaseInventoryRequest request)`
5. `decrease(DecreaseInventoryRequest request)`

### 7.2 Repository layer

`InventoryRepository`

1. Giữ:
   - `findByIdForUpdate`
   - `findByDimensionForUpdate`
2. Bổ sung:
   - aggregate query cho availability
   - optional dimension-based lookup helper
   - create-or-reload pattern cho concurrent create

### 7.3 Stock movement layer

`StockMovementsType`

Phải đảm bảo cover đủ:

1. `RESERVE`
2. `UNRESERVE`
3. `INBOUND`
4. `OUTBOUND`
5. existing:
   - `ADJUSTMENT_INCREASE`
   - `ADJUSTMENT_DECREASE`
   - `TRANSFER_OUT`
   - `TRANSFER_IN`

---

## 8. Test matrix

### 8.1 Controller tests

1. validation errors
2. auth failures
3. success response shape

### 8.2 Service unit tests

1. positive path
2. insufficient stock
3. reserved violation
4. row not found
5. invalid dimension
6. movement saved correctly
7. actor/audit fields set

### 8.3 Integration tests

1. reserve concurrent calls
2. unreserve concurrent calls
3. increase create-or-reload under race
4. decrease stale version protection
5. DB constraints still hold:
   - `on_hand >= 0`
   - `reserved >= 0`
   - `reserved <= on_hand`

---

## 9. Dependencies on other modules

Inventory module hoàn chỉnh xong sẽ là dependency trực tiếp cho:

1. Sales Orders confirm -> `reserve`
2. Sales Orders cancel -> `unreserve`
3. Outbound Shipments confirm -> `decrease`
4. Inbound Receipts confirm -> `increase`
5. Stock pre-check / allocation UI -> `check-availability`

Do đó inventory module nên hoàn tất trước khi bắt đầu serious implementation cho inbound/outbound workflows.

---

## 10. Success criteria

Inventory module được xem là hoàn chỉnh khi:

1. Toàn bộ `WHS-10..17` Done.
2. `WHS-19` Done.
3. `GET` APIs không bị regression.
4. `check-availability` đúng theo requested quantity.
5. `reserve/unreserve/increase/decrease` đều ghi movement audit.
6. Có concurrency coverage tối thiểu cho stock mutation APIs.
7. Tài liệu API, module review và progress docs đã được sync.

