# NOTIFICATION SYSTEM STANDARD

**Project:** Warehouse Management System Backend  
**Date:** 2026-03-21  
**Status:** Proposed implementation standard  
**Supersedes for implementation:** `documents/Notification/BRD_NOTIFICATION_SYSTEM.md`

## 1. Purpose

Tài liệu này là implementation contract cho module notification của WMS backend.

Mục tiêu:

- lưu notification bền vững theo từng người nhận
- đẩy real-time cho user đang online
- không làm block hoặc làm sai transaction nghiệp vụ
- chạy an toàn trong môi trường multi-instance
- tránh duplicate, race condition, deadlock và cache drift

## 2. Scope

### In scope

- in-app notifications
- unread count
- notification history
- real-time push qua WebSocket
- cross-instance live fan-out
- optional hand-off sang email module hiện có

### Out of scope for P0

- browser push/mobile push
- notification template CMS
- user-customized rules quá chi tiết
- notification analytics nâng cao

## 3. Codebase Constraints

Thiết kế phải bám đúng repo hiện tại:

- identity chính là `Account`
- profile nằm ở `UserProfile`
- warehouse assignment nằm ở `Employee.warehouseId`
- role lấy qua `roles` và `account_roles`
- current actor lấy bằng `SecurityUtils.getCurrentAccountId()`
- audit fields dùng `BaseEntity`
- RabbitMQ, Redis, WebSocket, Email module đã có sẵn
- response REST phải theo `BaseResponse<T>` và `PageResponse<T>`

Hệ quả:

- recipient key chuẩn là `account_id`
- không dùng model `users/user_id/UserRepository` trong thiết kế mới

## 4. Core Invariants

Module này phải bảo vệ các invariant sau:

1. Notification không bao giờ được tạo thành công cho một state transition bị rollback.
2. Business transaction không được phụ thuộc vào WebSocket, RabbitMQ live fan-out, Redis cache hay email delivery.
3. Mỗi recipient chỉ có tối đa một notification cho cùng một business event theo dedupe policy đã định nghĩa.
4. User chỉ đọc, đánh dấu đã đọc, và query được notification của chính mình trừ khi có use case admin riêng được phê duyệt.
5. Unread count là dữ liệu suy ra từ `notifications`, không phải dữ liệu nguồn độc lập.

## 5. Architecture Decisions

| Decision | Standard |
|---|---|
| Source of truth | MySQL `notifications` table |
| Transaction bridge | MySQL `notification_outbox` table |
| Delivery semantics | Persist exactly-once per recipient by unique key, live delivery at-least-once |
| Real-time transport | Local WebSocket + RabbitMQ AMQP fan-out giữa các instance |
| Catch-up | REST only |
| Presence | Local session registry; không dùng MySQL presence table |
| Unread count cache | Redis read-through + invalidate only |
| Email integration | Reuse existing `EmailService`, không nhúng vào core notification transaction |

## 6. High-Level Flow

### 6.1 End-to-End Flow

1. Business service hoàn tất validate state transition.
2. Trong cùng transaction với aggregate root, ghi một row vào `notification_outbox`.
3. Transaction commit thành công.
4. `NotificationOutboxDispatcher` poll outbox theo batch nhỏ.
5. Dispatcher resolve recipient theo role và warehouse scope.
6. Dispatcher bulk insert `notifications` theo từng recipient, có dedupe key.
7. Dispatcher publish live event lên RabbitMQ exchange `wms.notification.live.exchange`.
8. Mỗi backend instance có queue riêng, consume live event và nếu recipient có session local thì push qua WebSocket.
9. Frontend merge notification theo `id`.
10. Frontend lấy history và unread count qua REST.

### 6.2 Important Consequences

- Nếu WebSocket down: notification vẫn có trong DB.
- Nếu RabbitMQ live exchange fail: history và unread count vẫn đúng.
- Nếu Redis fail: query count trực tiếp từ DB.
- Nếu email fail: notification in-app không bị rollback.

## 7. Data Model

## 7.1 `notifications`

Đây là bảng canonical per recipient.

```sql
CREATE TABLE notifications (
    id                   CHAR(36)     NOT NULL,
    recipient_account_id CHAR(36)     NOT NULL,
    module               VARCHAR(50)  NOT NULL,
    event_code           VARCHAR(80)  NOT NULL,
    severity             VARCHAR(20)  NOT NULL,
    title                VARCHAR(255) NOT NULL,
    message              TEXT         NOT NULL,
    reference_type       VARCHAR(50)  NOT NULL,
    reference_id         CHAR(36)     NULL,
    reference_number     VARCHAR(50)  NULL,
    reference_url        VARCHAR(500) NULL,
    dedupe_key           VARCHAR(191) NOT NULL,
    payload_json         JSON         NULL,
    is_read              BIT(1)       NOT NULL DEFAULT b'0',
    read_at              DATETIME     NULL,
    created_by           CHAR(36)     NULL,
    created_at           DATETIME     NOT NULL,
    updated_by           CHAR(36)     NULL,
    updated_at           DATETIME     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_notifications_recipient_dedupe
        UNIQUE (recipient_account_id, dedupe_key),
    CONSTRAINT fk_notifications_account
        FOREIGN KEY (recipient_account_id) REFERENCES accounts(id),
    INDEX idx_notifications_recipient_created
        (recipient_account_id, created_at DESC, id DESC),
    INDEX idx_notifications_recipient_unread
        (recipient_account_id, is_read, created_at DESC),
    INDEX idx_notifications_reference
        (reference_type, reference_id, created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### Notes

- `recipient_account_id` dùng `accounts.id`
- `dedupe_key` là bắt buộc
- `payload_json` chỉ chứa metadata hiển thị, không chứa secret
- không lưu presence vào bảng này

## 7.2 `notification_outbox`

Đây là bảng transaction-safe bridge từ business event sang notification processing.

```sql
CREATE TABLE notification_outbox (
    id               CHAR(36)     NOT NULL,
    aggregate_type   VARCHAR(50)  NOT NULL,
    aggregate_id     CHAR(36)     NOT NULL,
    warehouse_id     CHAR(36)     NULL,
    event_code       VARCHAR(80)  NOT NULL,
    actor_account_id CHAR(36)     NULL,
    dedupe_key       VARCHAR(191) NOT NULL,
    payload_json     JSON         NOT NULL,
    status           VARCHAR(20)  NOT NULL,
    attempt_count    INT          NOT NULL DEFAULT 0,
    next_retry_at    DATETIME     NULL,
    locked_by        VARCHAR(100) NULL,
    locked_at        DATETIME     NULL,
    error_message    VARCHAR(500) NULL,
    created_at       DATETIME     NOT NULL,
    updated_at       DATETIME     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_notification_outbox_dedupe UNIQUE (dedupe_key),
    INDEX idx_notification_outbox_poll
        (status, next_retry_at, created_at),
    INDEX idx_notification_outbox_aggregate
        (aggregate_type, aggregate_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### Outbox Status

- `PENDING`
- `PROCESSING`
- `DONE`
- `RETRY`
- `DEAD`

## 7.3 `notification_preferences` (P1)

Không bắt buộc cho P0. Nếu làm P1:

- key theo `account_id + module`
- bật/tắt `in_app`
- bật/tắt `email`
- `muted_until`

## 7.4 Explicit Non-Requirement

Không tạo `user_presence` table trong MySQL.

Lý do:

- dữ liệu này volatile
- heartbeat write xuống MySQL là hotspot vô ích
- không cần cho correctness của notification

## 8. Dedupe Policy

## 8.1 Outbox Dedupe Key

Format khuyến nghị:

```text
{aggregateType}:{aggregateId}:{eventCode}:{version}
```

Ví dụ:

```text
STOCK_ADJUSTMENT:6f2...:PENDING_APPROVAL:v1
INBOUND_RECEIPT:9ab...:CONFIRMED:v1
```

## 8.2 Recipient-Level Dedupe Key

Mỗi notification row dùng cùng `dedupe_key` từ outbox. Unique constraint nằm trên:

```text
(recipient_account_id, dedupe_key)
```

Điều này đảm bảo:

- retry dispatcher không tạo duplicate row
- process restart giữa chừng vẫn an toàn

## 8.3 Frontend Dedupe

Frontend phải merge theo `notification.id`.

Không dựa vào:

- title
- message
- timestamp gần đúng

## 9. Recipient Resolution Standard

Recipient resolution phải tách riêng thành `NotificationRecipientService`.

Input của service:

- `module`
- `eventCode`
- `aggregateId`
- `warehouseId`
- `actorAccountId`
- payload business liên quan

Output:

- tập `accountId` không duplicate

### Resolution Sources

- `account_roles`
- `roles`
- `employees.warehouse_id`
- aggregate cụ thể nếu cần creator/requester

### Mandatory Rules

- loại actor khỏi recipient nếu use case không cần self-notify
- không broadcast toàn bộ admin/manager toàn hệ thống cho event chỉ thuộc một kho
- mọi recipient resolution phải deterministic

## 10. Event Matrix for P0

| Module | Event | Trigger source | Recipient rule | Severity |
|---|---|---|---|---|
| `STOCK_ADJUSTMENT` | `PENDING_APPROVAL` | tạo adjustment cần duyệt | `ADMIN` + `MANAGER` cùng `warehouse_id` | `ACTION_REQUIRED` |
| `STOCK_ADJUSTMENT` | `APPROVED` | approve adjustment | creator/requester của adjustment | `SUCCESS` |
| `STOCK_ADJUSTMENT` | `REJECTED` | reject adjustment | creator/requester của adjustment | `WARNING` |
| `STOCK_TRANSFER` | `PENDING` | transfer được submit/pending | `ADMIN` + `MANAGER` của kho nguồn | `ACTION_REQUIRED` |
| `STOCK_TRANSFER` | `COMPLETED` | complete transfer | creator + manager kho nguồn + manager kho đích | `SUCCESS` |
| `STOCK_TRANSFER` | `CANCELLED` | cancel transfer | creator | `WARNING` |
| `PURCHASE_ORDER` | `CONFIRMED` | confirm PO | manager kho nhận + creator | `INFO` |
| `INBOUND_RECEIPT` | `CONFIRMED` | confirm inbound receipt | creator receipt + manager kho nhận + requester PO nếu khác actor | `SUCCESS` |
| `BATCH` | `QUARANTINED` | quarantine batch | manager kho chứa batch | `WARNING` |
| `BATCH` | `EXPIRING_SOON` | scheduled job | manager kho chứa batch | `WARNING` |

### Notes

- `SALES_ORDER` và `OUTBOUND_SHIPMENT` để P1 vì outbound service hiện chưa hoàn chỉnh.
- Event matrix có thể mở rộng, nhưng mọi event mới phải định nghĩa recipient rule và dedupe key.

## 11. Transaction Boundary Standard

## 11.1 Inside Business Transaction

Được phép:

- validate state transition
- update aggregate
- ghi audit
- insert `notification_outbox`

Không được phép:

- gọi RabbitMQ live fan-out
- gọi WebSocket push
- query Redis để quyết định đúng sai nghiệp vụ
- gọi email delivery trực tiếp

## 11.2 Why

Các flow như inbound confirm hoặc stock adjustment approval đang đụng inventory/state transition. Nếu notification làm I/O trong transaction sẽ:

- giữ lock lâu hơn
- tăng deadlock probability
- biến notification thành blocker của inventory flow

## 12. Outbox Dispatcher Standard

## 12.1 Polling Strategy

- chu kỳ 1 giây hoặc 2 giây
- batch size 50 đến 100
- dùng transaction riêng cho từng batch

## 12.2 Claim Strategy

MySQL 8 hỗ trợ `FOR UPDATE SKIP LOCKED`. Worker claim row theo thứ tự:

- `created_at ASC`
- `id ASC`

Điều này giảm khả năng nhiều worker tranh nhau cùng một batch.

## 12.3 Retry Strategy

Backoff khuyến nghị:

- lần 1: +15s
- lần 2: +60s
- lần 3: +5m
- lần 4: +15m
- sau đó chuyển `DEAD`

## 12.4 Insert Strategy

Fan-out notification là chỗ hiếm hoi được phép dùng native SQL hoặc `JdbcTemplate` batch insert/upsert.

Lý do:

- cần hiệu năng tốt
- cần idempotent upsert
- tránh N+1 insert khi một event gửi nhiều recipient

Điều này là ngoại lệ có chủ đích, không phải mở đường cho raw SQL tràn lan.

## 13. Real-Time Delivery Standard

## 13.1 Chosen Approach

Giữ WebSocket broker local trong từng instance và dùng RabbitMQ AMQP exchange để fan-out live event cho mọi instance.

### Exchange

- `wms.notification.live.exchange`

### Queue per instance

- `wms.notification.live.{instanceId}`

### Queue properties

- non-durable hoặc auto-delete nếu chỉ dùng cho live event
- consumer cạnh tranh ở mức instance-local

## 13.2 Why This Approach

- tận dụng RabbitMQ đang có sẵn trong repo
- không bắt business transaction phụ thuộc vào WebSocket broker relay
- live event fail không làm mất canonical notification
- đơn giản hơn việc biến WebSocket broker thành source of truth

## 13.3 WebSocket Contract

### Endpoint

```text
/ws
```

### Subscribe

```text
/user/queue/notifications
```

### Authentication

JWT phải nằm trong STOMP `CONNECT` header:

```text
Authorization: Bearer <token>
```

Không truyền token qua query string.

### Principal Mapping

WebSocket principal name phải map về `accountId` để thống nhất với `recipient_account_id`.

## 13.4 Catch-Up Rule

Không replay unread notifications qua WebSocket sau login hoặc reconnect.

Catch-up chỉ đi bằng REST:

- `GET /api/v1/notifications`
- `GET /api/v1/notifications/unread-count`

## 14. Redis Standard

Redis chỉ là phụ trợ.

### Allowed usage

- cache unread count
- optional metrics hoặc lightweight routing metadata

### Not allowed as source of truth

- online/offline correctness
- notification history
- read state

## 14.1 Unread Count Cache Strategy

### Read path

1. đọc Redis key `notif:unread:{accountId}`
2. nếu miss thì query DB
3. cache lại TTL ngắn, ví dụ 60 giây

### Write path

Sau các mutation sau:

- insert notification mới
- `markAsRead`
- `markAllAsRead`
- batch mark read

chỉ cần:

- `DELETE notif:unread:{accountId}`

Không tự `increment/decrement`.

## 15. REST API Standard

Tất cả endpoint trả theo `BaseResponse<T>`.

## 15.1 Query

### `GET /api/v1/notifications`

Filters:

- `page`
- `size`
- `isRead`
- `module`
- `startDate`
- `endDate`

Response:

- `BaseResponse<PageResponse<NotificationResponse>>`

### `GET /api/v1/notifications/unread-count`

Response:

- `BaseResponse<Long>`

## 15.2 Commands

### `PATCH /api/v1/notifications/{id}/read`

Rule:

- idempotent
- chỉ update row của current account
- chỉ update nếu `is_read = false`

### `PATCH /api/v1/notifications/read-all`

Rule:

- idempotent
- update unread rows của current account

### `PATCH /api/v1/notifications/read-batch`

Payload:

- list `ids`

Rule:

- chỉ update row thuộc current account
- bỏ qua `id` không tồn tại hoặc đã đọc

## 16. DTO and Package Standard

### Entity

- `org.demo.whs.entity.Notification`
- `org.demo.whs.entity.NotificationOutbox`

### Enums

- `NotificationModule`
- `NotificationSeverity`
- `NotificationOutboxStatus`

`eventCode` nên để dạng string chuẩn hóa thay vì enum phình quá lớn nếu team muốn linh hoạt mở rộng.

### DTO request

- `entity.dto.request.notification.MarkReadBatchRequest`

### DTO response

- `entity.dto.response.notification.NotificationResponse`
- `entity.dto.response.notification.NotificationLiveMessage`

### Services

- `NotificationCommandService`
- `NotificationQueryService`
- `NotificationRecipientService`
- `NotificationOutboxDispatcherService`
- `NotificationLivePublisher`

### Repository

- `NotificationRepository`
- `NotificationOutboxRepository`
- custom recipient query repository

## 17. Concurrency and Deadlock Rules

## 17.1 Race Conditions Covered

- outbox retry duplicate insert
- concurrent `markAsRead`
- concurrent `markAllAsRead`
- multi-tab receive same notification
- reconnect cùng lúc với REST catch-up

## 17.2 Mandatory Handling

- unique key trên `(recipient_account_id, dedupe_key)`
- frontend merge theo `id`
- `markAsRead` dùng update idempotent thay vì read-modify-write
- catch-up chỉ qua REST
- cache invalidate only

## 17.3 Deadlock Minimization

- không gọi external I/O trong business transaction
- dispatcher claim batch nhỏ
- insert notification theo thứ tự recipient ổn định nếu có batch lớn
- tránh update nhiều bảng không cần thiết sau mỗi live push

## 18. Degraded Mode

## 18.1 RabbitMQ live fan-out down

Expected behavior:

- business transaction vẫn commit
- outbox vẫn xử lý insert notification nếu DB ổn
- live push có thể fail hoặc delayed
- user vẫn thấy notification qua REST/history

## 18.2 Redis down

Expected behavior:

- unread count chuyển sang DB query
- không fail business flow
- không fail mark read command

## 18.3 WebSocket disconnected

Expected behavior:

- không ảnh hưởng backend correctness
- UI reconnect với backoff
- user lấy history bằng REST

## 18.4 Email module down

Expected behavior:

- in-app notification vẫn hoạt động
- email retry theo module email hiện có

## 19. Security Standard

- WebSocket auth bằng STOMP header, không dùng query string
- production CORS phải whitelist domain cụ thể
- mọi REST query tự ép `recipient_account_id = currentAccountId`
- không trả metadata chứa dữ liệu nhạy cảm
- log chỉ ghi `notificationId`, `module`, `eventCode`, `recipientAccountId`, không ghi token

## 20. Observability Standard

Metrics bắt buộc:

- `notification_outbox_pending_total`
- `notification_outbox_retry_total`
- `notification_outbox_dead_total`
- `notification_dispatch_duration_ms`
- `notification_live_publish_fail_total`
- `notification_unread_count_cache_hit_total`
- `notification_duplicate_insert_ignored_total`
- `notification_read_commands_total`

Logs bắt buộc:

- outbox claimed
- recipients resolved
- batch insert count
- live publish fail
- dead-letter transition

## 21. Cleanup and Retention

Khuyến nghị:

- giữ unread notifications không xóa tự động
- read notifications retention mặc định 180 ngày
- cleanup job chạy ngoài giờ cao điểm
- nếu cần compliance lâu hơn, archive sang storage khác ở P2

## 22. Testing Standard

## 22.1 Unit Tests

- recipient resolution theo role và warehouse
- dedupe key generation
- outbox retry calculation
- unread count cache invalidation

## 22.2 Integration Tests

- business transaction commit tạo outbox
- dispatcher insert notification đúng recipient
- retry không tạo duplicate
- `markAsRead` và `markAllAsRead` idempotent
- REST history chỉ trả notification của current account

## 22.3 Concurrency Tests

- 2 worker claim outbox cùng lúc
- 2 request `markAsRead` cùng notification
- reconnect + REST fetch + live event cùng lúc

## 22.4 Failure Injection

- RabbitMQ unavailable
- Redis timeout
- DB duplicate key trên notification insert
- exception trong recipient resolution

## 23. Implementation Roadmap

### Phase 1

- migration `notifications` + `notification_outbox`
- entity/repository/service skeleton
- REST query/read commands

### Phase 2

- outbox dispatcher
- recipient resolution theo role + warehouse
- batch insert + dedupe

### Phase 3

- RabbitMQ live fan-out
- WebSocket auth interceptor
- frontend merge logic

### Phase 4

- metrics
- cleanup job
- optional email bridge

## 24. Migration Naming

Migration file khuyến nghị:

```text
V20260321_02__create_notifications_and_notification_outbox.sql
```

Nếu thêm preferences:

```text
V20260321_03__create_notification_preferences.sql
```

## 25. Final Decision

Notification module của dự án này phải được xây như một hậu xử lý transaction-safe của business event, không phải một lớp WebSocket gắn trực tiếp lên service nghiệp vụ.

Nếu team bám theo tài liệu này:

- business correctness sẽ không bị notification làm nhiễu
- multi-instance vẫn push real-time được
- retry không tạo duplicate
- unread count không drift
- hệ thống có degraded mode rõ ràng khi Redis, RabbitMQ hoặc WebSocket gặp sự cố
