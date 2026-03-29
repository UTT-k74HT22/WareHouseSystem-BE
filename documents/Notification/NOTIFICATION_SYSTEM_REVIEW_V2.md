# REVIEW V2 - NOTIFICATION SYSTEM

**Project:** Warehouse Management System Backend  
**Date:** 2026-03-21  
**Status:** Reviewed and ready for architecture correction  
**Input reviewed:** `documents/Notification/BRD_NOTIFICATION_SYSTEM.md`, `documents/Notification/Review.md`, current backend codebase

## 1. Executive Summary

Hai tài liệu hiện có đi đúng hướng ở ý tưởng `DB + real-time push`, nhưng chưa đủ chuẩn để team implement production cho dự án này.

Vấn đề lớn nhất không nằm ở UI hay WebSocket, mà nằm ở 5 chỗ sau:

1. Notification chưa được gắn chặt với transaction boundary của nghiệp vụ.
2. Thiết kế đang lệch model dữ liệu thật của repo (`Account`, `UserProfile`, `Employee`, `Role`), nên code mẫu trong BRD không thể bê vào dự án này.
3. Multi-instance real-time chưa khép kín, nên tài liệu hiện tại chưa chứng minh được "không mất notification" trong cluster.
4. Cache unread count, catch-up flow, presence, mark-read đang thiếu xử lý race condition và duplicate.
5. Chưa có degraded mode rõ ràng để notification không block các flow tồn kho, nhập, xuất, điều chỉnh.

Kết luận: tài liệu cũ dùng được như ý tưởng ban đầu, nhưng chưa nên coi là implementation contract. Bản thiết kế chuẩn nên chuyển sang mô hình:

- MySQL là source of truth cho notification per recipient.
- `notification_outbox` là transaction bridge giữa business event và notification processing.
- WebSocket chỉ là live acceleration, không phải nguồn dữ liệu chuẩn.
- Catch-up phải đi bằng REST, không replay qua WebSocket.
- Redis chỉ nên dùng cache/phụ trợ, không dùng làm nguồn đúng sai cho unread count hay online status trong DB.

## 2. Những điểm làm tốt

- Chọn hướng `persistent notification + real-time push` là đúng.
- Có phân biệt online và offline experience.
- Có nghĩ tới Redis, RabbitMQ, scalability và metrics từ sớm.
- Có liệt kê nhiều event business hữu ích cho WMS.
- Có xem notification là module dùng chung thay vì nhúng rải rác trong controller.

## 3. Critical Findings

### C1. Thiếu transaction-safe delivery model

BRD hiện mô tả kiểu "service save notification rồi push WebSocket". Cách này chưa đủ an toàn cho các flow đang có lock và validation chặt như:

- `StockAdjustmentsServiceImpl.approve/reject`
- `InboundReceiptsServiceImpl.confirm`
- `StockTransfersServiceImpl.complete/cancel`
- `PurchaseOrdersServiceImpl.confirm`

Với WMS, notification phải là hậu quả của state transition đã commit thành công, không được:

- push trước khi transaction business commit
- gọi external I/O trong lúc transaction đang giữ lock
- phụ thuộc Redis/Rabbit/WebSocket để business flow hoàn tất

Nếu không có outbox:

- có thể gửi "ghost notification" cho event bị rollback
- có thể mất notification khi process crash giữa commit và push
- có thể kéo dài transaction và tăng nguy cơ deadlock

**Kết luận:** phải có `notification_outbox` ghi cùng transaction với aggregate root.

### C2. BRD lệch domain model thật của codebase

Codebase hiện tại dùng:

- `accounts`
- `user_profiles`
- `employees`
- `roles`
- `account_roles`
- `SecurityUtils.getCurrentAccountId()`

Trong khi BRD đang mô tả và sample code theo kiểu:

- `users`
- `UserRepository`
- `User::getId`
- `getCurrentUserId()`

Điều này không phải lỗi câu chữ. Nó làm sai luôn thiết kế recipient resolution, authorization, query, foreign key và WebSocket principal mapping.

**Kết luận:** recipient key phải chuẩn hóa theo `account_id`, không phải `user_id`.

### C3. Multi-instance real-time chưa được khép kín

BRD hiện trộn ba ý:

- local `SimpleBroker`
- Redis Pub/Sub trong diagram
- WebSocket push trực tiếp từ service

Nếu chỉ dùng `SimpleBroker` như sample code thì không scale cluster. Nếu muốn giữ `SimpleBroker`, phải bổ sung bridge cross-instance thực sự.

Với repo này, có 2 hướng hợp lệ:

1. Dùng broker relay cho STOMP.
2. Hoặc giữ local `SimpleBroker`, nhưng dùng RabbitMQ/Redis để fan-out live-delivery event giữa các instance.

Tài liệu hiện tại chưa chốt dứt điểm nên chưa thể triển khai an toàn.

### C4. Recipient scope chưa bám warehouse scope

WMS này không thể có rule kiểu "notify all admin/manager" một cách mù.

Thực tế codebase đã có `Employee.warehouseId`, nghĩa là recipient resolution phải xét:

- role
- warehouse scope
- actor hiện tại
- aggregate liên quan

Nếu không chốt rule này, notification có thể lộ thông tin nghiệp vụ sai người, đặc biệt ở flow:

- stock adjustment pending approval
- transfer giữa kho
- batch quarantine
- inbound receipt confirmed

### C5. Chưa có degraded mode để không block business flow

Notification là quan trọng, nhưng không được phép làm hỏng transaction inventory chỉ vì:

- RabbitMQ chậm
- Redis timeout
- WebSocket down
- frontend mất kết nối

Tài liệu hiện tại nói nhiều về ideal path, nhưng chưa nói rõ khi dependency hỏng thì hệ thống sẽ xử lý thế nào để business vẫn đi tiếp.

**Kết luận:** live push và email phải là asynchronous downstream effect. Chỉ việc persist outbox mới được nằm trong transaction business.

## 4. High Findings

### H1. `user_presence` trong MySQL là thiết kế dư thừa

Presence là dữ liệu volatile. Ghi heartbeat xuống MySQL sẽ tạo write amplification vô ích.

Không nên có `user_presence` table cho bài toán này. Presence nên là:

- local session registry trong instance
- optional Redis TTL key nếu cần metrics/routing

Nhưng không phải source of truth ở database.

### H2. Cache unread count đang dễ lệch do increment/decrement thủ công

Mẫu `incrementUnreadCount()` và `decrementUnreadCount()` trong BRD rất dễ lệch khi xảy ra:

- duplicate event
- retry
- concurrent mark-read
- reconnect rồi fetch lại
- cache eviction giữa chừng

Đối với unread count, chiến lược an toàn hơn là:

- DB là source of truth
- Redis dùng read-through cache
- mọi mutation chỉ `delete` cache key, không tự cộng trừ

### H3. Catch-up qua WebSocket sẽ tạo duplicate và burst

BRD đang mô tả khi user online lại thì WebSocket "gửi all unread". Thiết kế này tạo ra 3 vấn đề:

- duplicate với REST fetch ban đầu
- khó dedupe khi reconnect nhiều lần
- burst lớn nếu user có nhiều unread

**Đề xuất:** WebSocket chỉ nhận notification mới sau khi kết nối. Catch-up và history lấy bằng REST.

### H4. Chưa có idempotency key và dedupe policy

Notification production-grade bắt buộc phải có:

- event-level dedupe key trong outbox
- recipient-level unique key khi fan-out
- frontend merge theo `notification.id`

Nếu không:

- retry outbox tạo duplicate row
- reconnect hoặc multi-tab tạo duplicate UI
- cùng một state transition bị replay nhiều lần

### H5. Ordering semantics chưa được định nghĩa

Tài liệu hiện tại chưa trả lời rõ:

- thứ tự hiển thị theo `created_at` hay theo event time
- nếu hai event cùng aggregate tới gần nhau thì UI xử lý thế nào
- worker retry có giữ thứ tự tuyệt đối không

Khuyến nghị thực tế:

- đảm bảo order theo recipient bằng `created_at DESC, id DESC`
- không hứa "global total ordering"
- UI merge theo `id`, sort lại theo timestamp

### H6. Chưa có chính sách dead-letter / retry cho notification processing

Retry cho email đã có trong codebase. Notification module mới chưa có chính sách tương đương cho:

- outbox processing fail
- recipient resolution fail
- DB batch insert fail

Phải có các trạng thái tối thiểu:

- `PENDING`
- `PROCESSING`
- `DONE`
- `RETRY`
- `DEAD`

### H7. Thiết kế mark-read chưa thật sự idempotent

`markAsRead`, `markAllAsRead`, `markAsReadByIds` phải được định nghĩa như command idempotent:

- gọi lại không làm lỗi
- chỉ update record thuộc current account
- chỉ update record chưa đọc

Nếu để load entity rồi save lại theo kiểu object state thông thường, rất dễ race và thừa DB round-trip.

### H8. Security phần WebSocket chưa đủ chặt

Các điểm cần sửa dứt điểm:

- không truyền JWT qua query string
- không dùng wildcard origin trong production
- principal của WebSocket phải map được về `accountId`
- endpoint query notification phải luôn tự giới hạn theo current account

## 5. Medium Findings

### M1. Event taxonomy đang hơi phình to

Tách quá nhiều enum kiểu `ADJUSTMENT_PENDING`, `ADJUSTMENT_APPROVED`, `TRANSFER_PENDING` dễ dẫn tới enum explosion.

Thiết kế tốt hơn là tách:

- `module`
- `eventCode`
- `severity`

### M2. BRD chưa nói rõ boundary với email module đang có

Repo này đã có email module production-ready hơn nhiều phần notification mới.

Notification design chuẩn nên coi email là:

- optional downstream channel
- reuse `EmailService`
- không trộn logic email retry vào notification core

### M3. Chưa có retention strategy thực dụng

Notification history nếu giữ mãi sẽ lớn nhanh. Cần chốt:

- retention mặc định
- cleanup job
- archive hay hard delete

## 6. Kiến trúc được khuyến nghị

Kiến trúc phù hợp nhất cho repo hiện tại là:

1. Business service commit state transition.
2. Trong cùng transaction, ghi `notification_outbox`.
3. Worker riêng đọc outbox theo batch nhỏ.
4. Worker resolve recipient theo `account_roles` + `employees.warehouse_id`.
5. Worker bulk insert vào `notifications` với unique dedupe key.
6. Worker publish live-delivery event qua RabbitMQ để mọi instance có thể đẩy WebSocket local.
7. Frontend lấy catch-up/history bằng REST, nhận event mới bằng WebSocket.

Điểm quan trọng:

- MySQL là source of truth.
- WebSocket là acceleration layer.
- Redis không quyết định tính đúng sai.
- RabbitMQ hỗ trợ cross-instance live fan-out, không chen vào transaction business.

## 7. Ưu tiên triển khai

### P0

- Chuẩn hóa design theo `Account/UserProfile/Employee`
- Thêm `notifications` và `notification_outbox`
- Chốt recipient scope theo warehouse
- Chốt idempotency + dedupe
- Chốt degraded mode
- Chốt REST catch-up + WebSocket only-for-new-events

### P1

- RabbitMQ live fan-out
- Redis cache unread count theo invalidate-only
- FE dedupe theo `notification.id`
- metrics, cleanup, DLQ

### P2

- per-user preferences
- aggregated notifications
- browser push/mobile push
- cross-tab socket coordination

## 8. Kết luận

BRD hiện tại có giá trị như draft phân tích, nhưng chưa đủ "safe to implement" cho WMS backend này.

Điểm cần sửa không chỉ là vài dòng WebSocket config, mà là chuyển notification từ một tính năng UI-driven thành một cơ chế hậu xử lý transaction-safe, idempotent, scoped và có degraded mode rõ ràng.

Tài liệu thay thế đề xuất cho implementation nằm tại:

- `documents/Notification/NOTIFICATION_SYSTEM_STANDARD.md`
