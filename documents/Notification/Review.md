# Review: BRD Notification System — WMS

**Phiên bản BRD gốc:** 1.0 — 21/03/2026  
**Người review:** Claude  
**Ngày review:** 21/03/2026  
**Trạng thái:** Cần sửa trước khi implementation

---

## Tổng quan đánh giá

| Hạng mục | Số lượng |
|---|---|
| 🔴 Critical — phải sửa trước production | 4 |
| 🟠 Warning — ảnh hưởng performance & reliability | 5 |
| 🔵 Improvement — nâng cao scalability & UX | 6 |
| 🟢 Good — thiết kế đúng hướng | 5 |

> **Kết luận tổng thể:** Kiến trúc tổng thể (DB + WebSocket, Redis cache, Prometheus metrics) được thiết kế đúng hướng. Tuy nhiên có **1 vấn đề kiến trúc nghiêm trọng** (SimpleBroker không scale được) và **1 vấn đề bảo mật** (JWT lộ qua URL) cần fix bắt buộc trước khi release.

---

## 🔴 Critical — Phải sửa trước production

### 1. SimpleBroker không scale được multi-instance

**Vị trí:** Section 5.4 — `WebSocketConfig.java`

**Vấn đề:**

Tài liệu vẽ sơ đồ Redis Pub/Sub để sync cross-instance, nhưng code thực tế dùng `enableSimpleBroker`. Đây là mâu thuẫn kiến trúc nghiêm trọng — SimpleBroker là in-memory broker, message chỉ deliver trong cùng 1 JVM instance.

**Kịch bản lỗi cụ thể:**
- User A kết nối WS vào **Instance 1**
- Admin B verify đơn hàng tại **Instance 2** → trigger notification
- Instance 2 broadcast → chỉ Instance 2 xử lý → **User A không bao giờ nhận được**

Toàn bộ scaling strategy với Redis Pub/Sub trong diagram hoàn toàn không hoạt động nếu giữ SimpleBroker.

```java
// ❌ Hiện tại — chỉ hoạt động single-instance
registry.enableSimpleBroker("/topic", "/queue");
```

**Fix:**

May mắn là hệ thống đã có RabbitMQ sẵn trong infra (Section 2.3). Chỉ cần switch sang `StompBrokerRelay`:

```java
// ✅ Dùng STOMP Broker Relay với RabbitMQ (đã có sẵn)
registry.enableStompBrokerRelay("/topic", "/queue")
    .setRelayHost(rabbitmqHost)
    .setRelayPort(61613)                // STOMP port (khác với AMQP 5672)
    .setClientLogin("guest")
    .setClientPasscode("guest")
    .setSystemLogin("guest")
    .setSystemPasscode("guest")
    .setVirtualHost("/");
```

```bash
# RabbitMQ cần enable STOMP plugin
rabbitmq-plugins enable rabbitmq_stomp
```

> **Lưu ý:** Sau khi switch sang StompBrokerRelay, Redis Pub/Sub trong diagram sẽ không còn cần thiết để sync WS — RabbitMQ đảm nhiệm vai trò này. Redis vẫn giữ nguyên cho unread count cache và online status.

---

### 2. JWT token lộ qua URL query parameter

**Vị trí:** Section 9.2 & Section 5.5 — Frontend `NotificationService`

**Vấn đề:**

Frontend gửi token qua URL: `ws://api.com/ws?token=eyJhbGci...`

Token trong URL bị lưu vào:
- Browser history (ai dùng chung máy có thể xem)
- Server access log / Nginx log
- Proxy log và load balancer log
- HTTP Referer header khi redirect

Đây là **security audit fail** ngay lập tức trong môi trường enterprise WMS.

```typescript
// ❌ Token lộ trong URL
const socket = new SockJS(this.wsUrl + '?token=' + jwt);
```

**Fix:**

Dùng STOMP CONNECT frame headers — không bao giờ xuất hiện trong URL hay log:

```typescript
// ✅ Token trong STOMP header — không lộ ra ngoài
stompClient.connect(
  { 'Authorization': 'Bearer ' + this.authService.getToken() },
  (frame: any) => {
    this.connectionStatusSubject.next(true);
    // ...
  }
);
```

```java
// Backend: đọc từ STOMP header trong ChannelInterceptor
@Override
public Message<?> preSend(Message<?> message, MessageChannel channel) {
    StompHeaderAccessor accessor = MessageHeaderAccessor
        .getAccessor(message, StompHeaderAccessor.class);
    
    if (StompCommand.CONNECT.equals(accessor.getCommand())) {
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        // Validate "Bearer eyJ..." token ở đây
    }
    return message;
}
```

---

### 3. Race condition trong `broadcastToUsers()` — mutate shared object

**Vị trí:** Section 7.1 — `NotificationService.broadcastToUsers()`

**Vấn đề:**

Method gọi `request.setUserId(userId)` bên trong stream, mutate object được share giữa các iterations. Khi `@Async` thread pool xử lý song song, nhiều thread cùng set `userId` trên cùng 1 object → notification gửi nhầm người.

```java
// ❌ Race condition — shared mutable state trong parallel stream
public List<NotificationResponse> broadcastToUsers(List<String> userIds, NotificationRequest request) {
    return userIds.stream()
        .map(userId -> {
            request.setUserId(userId); // ← Nhiều thread cùng set field này!
            return createAndSend(request);
        })
        .collect(Collectors.toList());
}
```

**Fix:**

Tạo object riêng cho từng user và dùng bulk insert để giảm DB round-trips:

```java
// ✅ Immutable copy cho từng user + bulk insert
public void broadcastToUsers(List<String> userIds, NotificationRequest template) {
    // Build notifications riêng biệt, không share state
    List<Notification> notifications = userIds.stream()
        .map(uid -> {
            Notification n = notificationMapper.toEntity(template);
            n.setUserId(uid);  // set trên object riêng
            n.setId(UUID.randomUUID().toString());
            return n;
        })
        .collect(Collectors.toList());
    
    // Bulk insert thay vì N single INSERTs
    notificationRepository.saveAll(notifications);
    
    // Gửi WS async cho từng notification
    notifications.forEach(n ->
        CompletableFuture.runAsync(() -> sendRealTimeNotification(n), taskExecutor)
    );
}
```

---

### 4. Frontend reconnect tạo connection mới nhưng không destroy cũ

**Vị trí:** Section 5.5 — `handleReconnect()` trong `NotificationService`

**Vấn đề:**

`handleReconnect()` gọi lại `this.connect()` nhưng `connect()` không cleanup `stompClient` cũ. Sau 3 lần reconnect, có 3 WebSocket connections cùng active → nhận notification trùng lặp × 3, memory leak tăng dần.

```typescript
// ❌ Không cleanup connection cũ trước khi reconnect
private handleReconnect(): void {
    const reconnect = () => {
        attempts++;
        setTimeout(() => {
            this.connect(); // ← Old stompClient vẫn tồn tại và subscribe!
        }, delay);
    };
}
```

**Fix:**

```typescript
// ✅ Cleanup trước khi reconnect, prevent duplicate timers
private reconnectTimer: ReturnType<typeof setTimeout> | null = null;

private handleReconnect(): void {
    if (this.reconnectTimer) return; // Đã có timer đang chạy, không tạo thêm
    
    let attempts = 0;
    const MAX_ATTEMPTS = 10;
    
    const attempt = () => {
        if (attempts >= MAX_ATTEMPTS) {
            this.reconnectTimer = null;
            console.error('Max reconnect attempts reached. Please refresh page.');
            return;
        }
        
        attempts++;
        // Exponential backoff với jitter để tránh thundering herd
        const baseDelay = Math.min(1000 * Math.pow(2, attempts), 60000);
        const jitter = Math.random() * 2000; // ±2s random
        
        this.reconnectTimer = setTimeout(() => {
            this.disconnect(); // ← Cleanup connection cũ trước
            this.connect();    // ← Tạo connection mới
            this.reconnectTimer = null;
        }, baseDelay + jitter);
    };
    
    attempt();
}
```

---

## 🟠 Warning — Ảnh hưởng performance & reliability

### 5. N+1 DB writes khi broadcast to Admin/Manager

**Vị trí:** `broadcastToAdminsAndManagers()` — Section 7.1

**Vấn đề:**

Với 10 admin, mỗi event trigger 10 riêng lẻ `INSERT` statements. Với tải 100 events/giây (như BRD mô tả) → 1.000 single INSERTs/giây, đây là nguyên nhân DB bottleneck đầu tiên khi scale.

**Fix:**

Bật Hibernate batch insert trong `application.yml`:

```yaml
spring:
  jpa:
    properties:
      hibernate:
        jdbc:
          batch_size: 50
        order_inserts: true
        order_updates: true
```

Kết hợp với `saveAll()` từ fix #3 ở trên — đây là cặp cần làm cùng nhau.

---

### 6. Redis unread count có thể bị lệch do race condition

**Vị trí:** Section 7.1 — `incrementUnreadCount()` / `decrementUnreadCount()`

**Vấn đề:**

Flow hiện tại: `hasKey() → increment()` là 2 Redis operations riêng biệt, không atomic. Nếu key expire giữa 2 operations, `increment()` tạo key mới với value `1` thay vì sync từ DB → unread count sai vĩnh viễn đến lần cache invalidate tiếp theo.

```java
// ❌ Non-atomic — key có thể expire giữa hasKey() và increment()
private void incrementUnreadCount(String userId) {
    String key = "notif:unread:" + userId;
    if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
        redisTemplate.opsForValue().increment(key); // key có thể đã expire tại đây!
    }
}
```

**Fix:**

Dùng Lua script để đảm bảo atomic:

```java
// ✅ Atomic via Lua script
private static final String INCREMENT_SCRIPT = """
    local v = redis.call('GET', KEYS[1])
    if v == false then
        -- Key không tồn tại, sync từ DB
        return -1
    end
    local newVal = redis.call('INCR', KEYS[1])
    redis.call('EXPIRE', KEYS[1], 300)
    return newVal
""";

private void incrementUnreadCount(String userId) {
    String key = "notif:unread:" + userId;
    Long result = redisTemplate.execute(
        RedisScript.of(INCREMENT_SCRIPT, Long.class),
        Collections.singletonList(key)
    );
    
    if (result != null && result == -1) {
        // Key đã expire, re-sync từ DB
        Long count = notificationRepository.countByUserIdAndIsReadFalse(userId);
        redisTemplate.opsForValue().set(key, String.valueOf(count), Duration.ofMinutes(5));
    }
}
```

---

### 7. `user_presence` table MySQL là redundant và tốn write

**Vị trí:** Section 3.2 — Database Design

**Vấn đề:**

Online status đã được track bằng Redis (`ws:online:{userId}` với TTL 5 min). Đồng bộ thêm sang MySQL `user_presence` tạo thêm write mỗi connect/disconnect/heartbeat (30s interval).

**Ước tính write overhead:** 1.000 concurrent users × heartbeat 30s = 2.000 writes/phút vào MySQL chỉ để track presence — hoàn toàn không cần thiết.

**Fix:**

- Bỏ table `user_presence` khỏi schema MySQL
- `is_online`: đọc từ Redis TTL (`hasKey("ws:online:{userId}")`)
- `last_seen`: persist vào Redis với TTL 24h, chỉ flush xuống DB khi user disconnect (không phải mỗi heartbeat)

```java
// Khi user disconnect, ghi last_seen 1 lần duy nhất
@EventListener
public void handleDisconnect(SessionDisconnectEvent event) {
    String userId = ...;
    // Ghi last_seen vào DB 1 lần khi disconnect
    userRepository.updateLastSeen(userId, LocalDateTime.now());
    // Xóa Redis key online
    redisTemplate.delete("ws:online:" + userId);
}
```

---

### 8. Không có dead-letter queue cho notification thất bại

**Vị trí:** Section 2 & 7.1 — Architecture

**Vấn đề:**

Nếu DB save thất bại (connection pool exhausted, deadlock, disk full), notification mất hoàn toàn. BRD có RabbitMQ nhưng chỉ dùng cho email.

**Kịch bản:** DB down 30 giây × 100 events/giây = **3.000 notifications mất**, không thể recovery.

**Fix — Outbox Pattern:**

```sql
-- Thêm table outbox (cùng transaction với business logic)
CREATE TABLE notification_outbox (
    id          CHAR(36) PRIMARY KEY,
    payload     JSON NOT NULL,
    status      ENUM('PENDING', 'PROCESSING', 'DONE', 'FAILED') DEFAULT 'PENDING',
    retry_count INT DEFAULT 0,
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    processed_at DATETIME,
    INDEX idx_status_created (status, created_at)
);
```

```java
// Business logic + outbox trong cùng 1 DB transaction
@Transactional
public void createAdjustment(CreateAdjustmentRequest req) {
    Adjustment adj = adjustmentRepository.save(...);
    
    // Ghi outbox trong cùng transaction
    notificationOutboxRepository.save(NotificationOutbox.builder()
        .payload(objectMapper.writeValueAsString(buildNotificationPayload(adj)))
        .build());
}

// Worker riêng poll và process outbox (có thể dùng Spring Scheduler hoặc Debezium CDC)
@Scheduled(fixedDelay = 1000)
public void processOutbox() {
    List<NotificationOutbox> pending = outboxRepository.findTop100ByStatus(PENDING);
    pending.forEach(this::processWithRetry);
}
```

---

### 9. Frontend dùng `window.toastr` global — anti-pattern trong Angular

**Vị trí:** Section 5.5 — `showToast()` trong `NotificationService`

**Vấn đề:**

`(window as any).toastr` phụ thuộc vào global jQuery variable. Không injectable, không mockable trong unit test, không có type safety. Nếu script load chậm → `toastr` là `undefined`, toast không hiện nhưng không có error nào được throw.

**Fix:**

```typescript
// ✅ Inject ToastrService từ ngx-toastr
import { ToastrService } from 'ngx-toastr';

@Injectable({ providedIn: 'root' })
export class NotificationService {
  constructor(
    private http: HttpClient,
    private toastr: ToastrService  // ← Injectable, testable, typed
  ) {}

  private showToast(notification: Notification): void {
    const config = { timeOut: 5000, progressBar: true, closeButton: true };
    
    const actionTypes: Partial<Record<NotificationType, () => void>> = {
      [NotificationType.ADJUSTMENT_APPROVED]: () =>
        this.toastr.success(notification.message, notification.title, config),
      [NotificationType.ADJUSTMENT_REJECTED]: () =>
        this.toastr.warning(notification.message, notification.title, config),
      [NotificationType.REPORT_FAILED]: () =>
        this.toastr.error(notification.message, notification.title, config),
    };
    
    (actionTypes[notification.type] ?? (() =>
      this.toastr.info(notification.message, notification.title, config)))();
  }
}
```

---

## 🔵 Improvement — Nâng cao scalability & UX

### 10. Thiếu notification aggregation/grouping

**Vấn đề:** Nếu 50 phiếu điều chỉnh tạo trong 10 giây, user nhận 50 toast + 50 bell items riêng lẻ → spam UX. Facebook, Slack đều aggregate: *"50 phiếu điều chỉnh mới cần duyệt"*.

**Đề xuất:**

Thêm field vào schema:
```sql
ALTER TABLE notifications ADD COLUMN group_key VARCHAR(100) COMMENT 'e.g. ADJUSTMENT_PENDING_2026-03-21';
ALTER TABLE notifications ADD COLUMN group_count INT DEFAULT 1;
```

Frontend group theo `group_key` khi render. Backend debounce bằng Redis: nếu cùng `group_key` nhận >3 events trong 5s, gộp thành 1 notification tổng hợp thay vì gửi riêng lẻ.

---

### 11. Thiếu Browser Push Notifications API cho tab đóng

**Vấn đề:** User đóng tab hoặc minimize browser không nhận được notification quan trọng (ADJUSTMENT_REJECTED, LOW_STOCK) dù đang "online" (WS vẫn connected).

**Đề xuất:**

Tích hợp Web Push API cho high-priority notifications:
- FE đăng ký service worker + subscription endpoint
- BE dùng `java-webpush` library để gửi push
- Chỉ enable cho: `ADJUSTMENT_REJECTED`, `LOW_STOCK`, `EXPIRY_WARNING`, `SYSTEM_ALERT`
- Thêm user preference để opt-in/opt-out

---

### 12. Thiếu notification preferences per user

**Vấn đề:** Tất cả notifications gửi cho tất cả user trong role. Khi scale lên nhiều kho hàng, một manager chỉ quan tâm đến kho của mình → notification noise tăng theo số lượng kho.

**Đề xuất:**

```sql
CREATE TABLE notification_preferences (
    user_id        CHAR(36) NOT NULL,
    notif_type     VARCHAR(50) NOT NULL,
    channel        ENUM('WS', 'EMAIL', 'PUSH') NOT NULL,
    enabled        BOOLEAN DEFAULT TRUE,
    PRIMARY KEY (user_id, notif_type, channel),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
```

Cache preferences trong Redis TTL 30 phút. Khi broadcast, filter theo preferences trước khi send.

---

### 13. Thiếu circuit breaker cho Redis failure

**Vấn đề:** Nếu Redis timeout 30s và có 100 concurrent notifications → 100 threads bị block 30s → thread pool exhaustion → toàn bộ API bị slow.

**Đề xuất:**

```java
// Dùng Resilience4j CircuitBreaker
@CircuitBreaker(name = "redis", fallbackMethod = "sendRealTimeNotificationFallback")
protected void sendRealTimeNotification(Notification notification) {
    // ... existing logic
}

protected void sendRealTimeNotificationFallback(Notification notification, Exception e) {
    // Fallback: skip online check, assume offline (notification đã save DB)
    log.warn("Redis unavailable, skipping real-time delivery for user: {}", notification.getUserId());
}
```

Thêm timeout config cho RedisTemplate:
```yaml
spring:
  data:
    redis:
      timeout: 2000ms      # read timeout
      connect-timeout: 1000ms
```

---

### 14. Duplicate notifications khi WS và REST race nhau

**Vị trí:** `fetchUnreadNotifications()` — Section 5.5

**Vấn đề:** Flow: subscribe WS → fetch REST unread. Trong khoảng thời gian đó, có thể nhận cùng 1 notification qua WS VÀ trong REST response → hiển thị trùng lặp.

**Fix:**

```typescript
// ✅ Deduplication bằng Set of IDs
private seenIds = new Set<string>();

private handleNewNotification(notification: Notification): void {
    if (this.seenIds.has(notification.id)) return; // Skip duplicate
    this.seenIds.add(notification.id);
    
    const current = this.notificationsSubject.value;
    this.notificationsSubject.next([notification, ...current]);
    // ...
}
```

---

### 15. CORS wildcard không phù hợp production

**Vị trí:** Section 5.4 — `WebSocketConfig`

**Vấn đề:** `setAllowedOriginPatterns("*")` cho phép bất kỳ domain nào kết nối WebSocket.

**Fix:**

```java
// ✅ Whitelist từ config
@Value("${cors.allowed-origins}")
private List<String> allowedOrigins;

registry.addEndpoint("/ws")
    .setAllowedOrigins(allowedOrigins.toArray(new String[0]))
    .withSockJS();
```

```yaml
# application-prod.yml
cors:
  allowed-origins:
    - https://wms.company.com
    - https://admin.company.com
```

---

## 🟢 Điểm tốt — Giữ nguyên và phát huy

### ✅ DB + WebSocket pattern — đảm bảo no message loss

Thiết kế "save first, send second" đảm bảo notification không bao giờ mất dù user offline hoặc WS send fail. Đây là approach chuẩn của production systems như Facebook Messenger. Cần giữ nguyên.

### ✅ Redis cache unread count — đúng cho hot path

Bell badge count là hot path, được fetch mỗi page load. Cache vào Redis TTL 5 phút là đúng strategy. Eventual consistency ở đây là acceptable cho UX — user không cần số chính xác tức thì.

### ✅ Exponential backoff reconnect — đúng pattern

`2^attempts` delay với cap 60s là standard. Chỉ cần bổ sung jitter (random offset ±20%) để tránh thundering herd khi server restart đồng loạt. Và fix bug "không cleanup connection cũ" như đề cập ở Critical #4.

### ✅ Database partitioning và cleanup strategy

RANGE partition theo `created_at` và scheduled cleanup sau 90 ngày là thiết kế đúng cho notification table có thể tăng trưởng lớn. Gợi ý thêm: cân nhắc **archive** sang cold storage thay vì hard delete nếu có compliance requirement (audit log).

### ✅ Prometheus metrics được thiết kế từ đầu

Thiết kế observability sớm rất đúng đắn. Metrics hiện có (`notification_delivery_latency_seconds` histogram, `websocket_connections_active` gauge) đủ để setup SLA monitoring. Gợi ý thêm metric: `notification_duplicate_detected_total` để monitor sau khi fix issue #14.

---

## Thứ tự ưu tiên implementation

| # | Task | Lý do |
|---|---|---|
| 1 | Fix SimpleBroker → StompBrokerRelay | Unlock toàn bộ multi-instance scaling |
| 2 | Fix JWT auth qua STOMP header | Security audit fail, phải fix trước release |
| 3 | Fix race condition `broadcastToUsers` | Data correctness — user nhận nhầm notification |
| 4 | Fix reconnect cleanup logic (FE) | Memory leak, notification duplicate |
| 5 | Batch insert + Hibernate config | Performance khi scale |
| 6 | Atomic Redis increment via Lua | Data consistency |
| 7 | Bỏ `user_presence` MySQL table | Giảm DB write overhead |
| 8 | Outbox pattern | Reliability — không mất notification khi DB spike |
| 9 | Fix toastr injection (FE) | Testability, reliability |
| 10 | Notification aggregation/grouping | UX improvement (Phase 2) |
| 11 | Notification preferences | UX + scale improvement (Phase 2) |
| 12 | Circuit breaker Redis | Resilience (Phase 2) |
| 13 | Web Push API | UX enhancement (Phase 2) |

---

## Estimate effort điều chỉnh

Thêm vào estimate gốc (~90h):

| Task bổ sung | Effort |
|---|---|
| Switch sang StompBrokerRelay + RabbitMQ STOMP config | 4h |
| Fix JWT auth qua STOMP header (BE + FE) | 3h |
| Fix race conditions + batch insert | 4h |
| Outbox pattern | 8h |
| Notification preferences table + logic | 8h |
| **Total bổ sung** | **~27h** |

**Tổng estimate mới: ~117h (~15-17 ngày)**

---

*Document này được tạo tự động từ review BRD_NOTIFICATION_SYSTEM v1.0*