# TÀI LIỆU NGHIỆP VỤ - HỆ THỐNG THÔNG BÁO (NOTIFICATION SYSTEM)
## WAREHOUSE MANAGEMENT SYSTEM

**Phiên bản:** 1.0  
**Ngày:** 21/03/2026  
**Trạng thái:** Ready for Implementation

---

## MỤC LỤC

1. [Tổng quan](#1-tổng-quan)
2. [Kiến trúc hệ thống](#2-kiến-trúc-hệ-thống)
3. [Thiết kế Database](#3-thiết-kế-database)
4. [Các sự kiện và loại thông báo](#4-các-sự-kiện-và-loại-thông-báo)
5. [WebSocket Design](#5-websocket-design)
6. [API Endpoints](#6-api-endpoints)
7. [Chi tiết Implementation](#7-chi-tiết-implementation)
8. [Scaling và Performance](#8-scaling-và-performance)
9. [Bảo mật](#9-bảo-mật)
10. [Testing Strategy](#10-testing-strategy)
11. [Deployment Checklist](#11-deployment-checklist)
12. [Estimate Effort](#12-estimate-effort)

---

## 1. TỔNG QUAN

### 1.1 Mục tiêu

Xây dựng hệ thống thông báo real-time cho WMS đảm bảo:

| Mục tiêu | Mô tả |
|-----------|--------|
| **Real-time** | User online nhận notification ngay lập tức qua WebSocket |
| **Offline Support** | User offline nhận notification khi online lại (lưu vào database) |
| **Scalability** | Hỗ trợ nhiều admin/manager verify nhiều đơn cùng lúc |
| **Reliability** | Không mất message dù user online/offline |

### 1.2 Mô hình hoạt động (Giống Facebook Messenger)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         NOTIFICATION FLOW                                   │
└─────────────────────────────────────────────────────────────────────────────┘

    USER ONLINE                              USER OFFLINE
         │                                        │
         ▼                                        ▼
┌─────────────────────┐                ┌─────────────────────┐
│  WebSocket Connected │                │  Database Queue     │
│  - Real-time push    │                │  - Save notification│
│  - Toast + Bell      │                │  - Mark as unread   │
└─────────────────────┘                └─────────────────────┘
         │                                        │
         │  (SAME LOGIC)                         │
         ▼                                        ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                        NOTIFICATION TABLE (DB)                             │
│  - Save all notifications regardless of online/offline                      │
│  - Track read/unread status                                                 │
│  - Support pagination for history                                           │
└─────────────────────────────────────────────────────────────────────────────┘
         │                                        │
         │  (USER LOGIN)                          │
         ▼                                        ▼
┌─────────────────────┐                ┌─────────────────────┐
│  WebSocket sends    │                │  WebSocket sends    │
│  only NEW ones      │                │  ALL unread ones    │
│  (since last login) │                │  (catch up)         │
└─────────────────────┘                └─────────────────────┘
```

### 1.3 So sánh các cơ chế

| Cơ chế | Độ phức tạp | Real-time | Offline user | Reliability |
|--------|-------------|-----------|--------------|-------------|
| **Polling only** | Thấp | ❌ Trễ (30s) | ❌ | ❌ Mất message |
| **WebSocket only** | Trung bình | ✅ Tức thì | ❌ | ❌ Mất message |
| **DB + WebSocket** | Trung bình | ✅ Tức thì | ✅ | ✅ Không mất |
| **Email only** | Thấp | ❌ Chậm | ✅ | ✅ |

**Lựa chọn:** DB + WebSocket (đảm bảo không mất message)

---

## 2. KIẾN TRÚC HỆ THỐNG

### 2.1 High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              CLIENTS                                         │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   ┌─────────┐     ┌─────────┐     ┌─────────┐     ┌─────────┐            │
│   │ Browser │     │ Mobile  │     │  Tab 2  │     │  Tab 3  │            │
│   │  (FE)   │     │  (FE)   │     │  (FE)   │     │  (FE)   │            │
│   │    ◄────┼─────┼────►    │     │    ◄────┼─────┼────►    │            │
│   └────┬────┘     └────┬────┘     └────┬────┘     └────┬────┘            │
│        │               │               │               │                   │
│        │     WebSocket Connections (STOMP)             │                   │
│        │               │               │               │                   │
└────────┼───────────────┼───────────────┼───────────────┼───────────────────┘
         │               │               │               │
         │    ┌─────────┴───────────────┴─────────┐    │
         │    │      STOMP over SockJS             │    │
         │    │      (Single connection per user)   │    │
         │    └──────────────┬──────────────────────┘    │
         │                   │                            │
         │    ┌──────────────┴──────────────────────┐    │
         │    │        LOAD BALANCER                 │    │
         │    │   (Sticky Session với Redis)        │    │
         │    └──────────────┬──────────────────────┘    │
         │                   │                            │
         └───────────────────┼────────────────────────────┘
                             │
         ┌───────────────────┼────────────────────────────┐
         │                   ▼                            │
┌─────────────────────────────────────────────────────────┐
│                    KUBERNETES / DOCKER SWARM            │
│  ┌─────────────────┐  ┌─────────────────┐              │
│  │   BE Instance 1 │  │   BE Instance 2 │              │
│  │  ┌───────────┐  │  │  ┌───────────┐  │              │
│  │  │ WebSocket │  │  │  │ WebSocket │  │              │
│  │  │  Handler  │  │  │  │  Handler  │  │              │
│  │  └───────────┘  │  │  └───────────┘  │              │
│  └────────┬────────┘  └────────┬────────┘              │
│           │                    │                        │
│           └──────────┬──────────┘                        │
│                      │                                 │
│           ┌─────────┴─────────┐                        │
│           │      REDIS        │                        │
│           │  ┌─────────────┐  │                        │
│           │  │ Pub/Sub    │  │  ← Multi-instance sync │
│           │  │ (Broadcast) │  │                        │
│           │  └─────────────┘  │                        │
│           │  ┌─────────────┐  │                        │
│           │  │   Session   │  │  ← Sticky session      │
│           │  │   Store     │  │                        │
│           │  └─────────────┘  │                        │
│           └──────────────────┘                        │
└─────────────────────────────────────────────────────────┘
                             │
         ┌───────────────────┼────────────────────────────┐
         │                   ▼                            │
┌─────────────────────────────────────────────────────────┐
│                      MYSQL DATABASE                     │
│  ┌─────────────────────────────────────────────────┐   │
│  │              notifications table                  │   │
│  │  - id, user_id, type, event, title, message    │   │
│  │  - reference_id, reference_number, is_read      │   │
│  │  - created_at, metadata                         │   │
│  └─────────────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────────────┐   │
│  │           user_online_status table              │   │
│  │  - user_id, last_seen, is_online              │   │
│  └─────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
```

### 2.2 Scalability Model

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  CASE: 10 Admin cùng verify 100 đơn/giây                                │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│                         ┌──────────────┐                                    │
│                         │   Redis     │                                    │
│                         │   Pub/Sub   │                                    │
│                         │              │                                    │
│                         │  Channel:    │                                    │
│                         │  notification│                                    │
│                         └──────┬───────┘                                    │
│                                │                                             │
│         ┌──────────────────────┼──────────────────────┐                    │
│         │                      │                      │                     │
│         ▼                      ▼                      ▼                     │
│  ┌────────────┐        ┌────────────┐        ┌────────────┐               │
│  │  Instance  │        │  Instance  │        │  Instance  │               │
│  │     1      │        │     2      │        │     N      │               │
│  │            │        │            │        │            │               │
│  │ WebSocket  │        │ WebSocket  │        │ WebSocket  │               │
│  │   Admin 1  │        │   Admin 2  │        │  Admin N   │               │
│  │  Admin 4   │        │   Admin 5  │        │  Admin 10  │               │
│  │  Admin 7   │        │   Admin 8  │        │            │               │
│  └────────────┘        └────────────┘        └────────────┘               │
│                                                                             │
│  → Redis đảm bảo message đến đúng instance                                │
│  → Sticky session giữ WebSocket connection ổn định                        │
│  → Database đảm bảo persistence (không mất message)                       │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 2.3 Infrastructure hiện có

| Component | Status | Chi tiết |
|-----------|--------|----------|
| **spring-boot-starter-websocket** | ✅ Có sẵn | Dependency trong pom.xml |
| **spring-boot-starter-amqp** | ✅ Có sẵn | RabbitMQ integration |
| **spring-boot-starter-data-redis** | ✅ Có sẵn | Redis integration |
| **spring-session-data-redis** | ✅ Có sẵn | Session store |
| **Email infrastructure** | ✅ Có sẵn | EmailProducerService, RabbitMQ email queue |

---

## 3. THIẾT KẾ DATABASE

### 3.1 Notifications Table

```sql
CREATE TABLE notifications (
    id              CHAR(36) PRIMARY KEY,
    user_id         CHAR(36) NOT NULL COMMENT 'Người nhận notification',
    type            VARCHAR(50) NOT NULL COMMENT 'NotificationType enum',
    module          VARCHAR(30) NOT NULL COMMENT 'Module: STOCK_ADJUSTMENT, STOCK_TRANSFER...',
    event           VARCHAR(30) NOT NULL COMMENT 'Event: CREATED, APPROVED, REJECTED...',
    title           VARCHAR(255) NOT NULL COMMENT 'Tiêu đề ngắn gọn',
    message         TEXT COMMENT 'Nội dung chi tiết',
    reference_id    CHAR(36) COMMENT 'ID của entity liên quan (adjustment_id, transfer_id...)',
    reference_number VARCHAR(50) COMMENT 'Mã phiếu: ADJ-xxx, TRF-xxx',
    reference_url   VARCHAR(500) COMMENT 'Deep link đến trang chi tiết',
    metadata        JSON COMMENT 'Dữ liệu bổ sung theo từng loại notification',
    is_read         BOOLEAN DEFAULT FALSE COMMENT 'Đã đọc chưa',
    read_at         DATETIME COMMENT 'Thời gian đánh dấu đã đọc',
    created_by      CHAR(36) COMMENT 'Ai tạo sự kiện (SYSTEM, user_id...)',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_user_unread (user_id, is_read, created_at DESC),
    INDEX idx_user_created (user_id, created_at DESC),
    INDEX idx_reference (module, reference_id),
    INDEX idx_created_at (created_at),
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### 3.2 User Presence Table

```sql
CREATE TABLE user_presence (
    user_id         CHAR(36) PRIMARY KEY,
    is_online       BOOLEAN DEFAULT FALSE,
    last_seen       DATETIME,
    session_id      VARCHAR(100),
    device_info     VARCHAR(255) COMMENT 'Browser, OS info',
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### 3.3 Migration File

```sql
-- V{version}__create_notifications_table.sql

CREATE TABLE notifications (
    id              CHAR(36) PRIMARY KEY,
    user_id         CHAR(36) NOT NULL,
    type            VARCHAR(50) NOT NULL,
    module          VARCHAR(30) NOT NULL,
    event           VARCHAR(30) NOT NULL,
    title           VARCHAR(255) NOT NULL,
    message         TEXT,
    reference_id    CHAR(36),
    reference_number VARCHAR(50),
    reference_url   VARCHAR(500),
    metadata        JSON,
    is_read         BOOLEAN DEFAULT FALSE,
    read_at         DATETIME,
    created_by      CHAR(36),
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_user_unread (user_id, is_read, created_at DESC),
    INDEX idx_user_created (user_id, created_at DESC),
    INDEX idx_reference (module, reference_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_presence (
    user_id         CHAR(36) PRIMARY KEY,
    is_online       BOOLEAN DEFAULT FALSE,
    last_seen       DATETIME,
    session_id      VARCHAR(100),
    device_info     VARCHAR(255),
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

## 4. CÁC SỰ KIỆN VÀ LOẠI THÔNG BÁO

### 4.1 Notification Types Enum (BE)

```java
public enum NotificationType {
    // Stock Adjustment
    ADJUSTMENT_PENDING("Có phiếu điều chỉnh mới cần duyệt"),
    ADJUSTMENT_APPROVED("Phiếu điều chỉnh đã được duyệt"),
    ADJUSTMENT_REJECTED("Phiếu điều chỉnh bị từ chối"),
    
    // Stock Transfer
    TRANSFER_PENDING("Có phiếu chuyển kho mới cần xử lý"),
    TRANSFER_COMPLETED("Phiếu chuyển kho đã hoàn tất"),
    TRANSFER_CANCELLED("Phiếu chuyển kho đã bị hủy"),
    
    // Inbound
    INBOUND_CONFIRMED("Phiếu nhập kho đã được xác nhận"),
    INBOUND_PARTIAL("Phiếu nhập kho nhận một phần"),
    
    // Outbound
    OUTBOUND_CONFIRMED("Đơn hàng đã được xác nhận"),
    OUTBOUND_PICKING("Shipment bắt đầu picking"),
    OUTBOUND_SHIPPED("Shipment đã được giao"),
    OUTBOUND_DELIVERED("Giao hàng thành công"),
    
    // Report
    REPORT_COMPLETED("Báo cáo đã sẵn sàng để tải"),
    REPORT_FAILED("Tạo báo cáo thất bại"),
    
    // System
    LOW_STOCK("Cảnh báo tồn kho thấp"),
    EXPIRY_WARNING("Cảnh báo batch sắp hết hạn"),
    SYSTEM_ALERT("Thông báo hệ thống");
}
```

### 4.2 Notification Module Enum (BE)

```java
public enum NotificationModule {
    STOCK_ADJUSTMENT("Điều chỉnh tồn kho"),
    STOCK_TRANSFER("Chuyển vị trí kho"),
    INBOUND_RECEIPT("Nhập kho"),
    OUTBOUND_ORDER("Đơn hàng"),
    OUTBOUND_SHIPMENT("Giao hàng"),
    REPORT("Báo cáo"),
    SYSTEM("Hệ thống");
}
```

### 4.3 Notification Event Enum (BE)

```java
public enum NotificationEvent {
    CREATED("Đã tạo"),
    PENDING("Đang chờ duyệt"),
    APPROVED("Đã duyệt"),
    REJECTED("Đã từ chối"),
    CONFIRMED("Đã xác nhận"),
    COMPLETED("Hoàn tất"),
    CANCELLED("Đã hủy"),
    SHIPPED("Đã giao hàng"),
    DELIVERED("Đã nhận hàng"),
    PICKING("Đang picking"),
    PROCESSING("Đang xử lý"),
    FAILED("Thất bại"),
    WARNING("Cảnh báo");
}
```

### 4.4 Event Matrix

```
┌──────────────────┬────────────────┬──────────────┬──────────────────────────┐
│ Module           │ Event          │ Recipients   │ Trigger                  │
├──────────────────┼────────────────┼──────────────┼──────────────────────────┤
│ STOCK_ADJUSTMENT │ PENDING        │ Admin,       │ Nhân viên tạo điều chỉnh │
│                  │                │ Manager      │ cần duyệt                │
├──────────────────┼────────────────┼──────────────┼──────────────────────────┤
│ STOCK_ADJUSTMENT │ APPROVED       │ Người tạo    │ Admin duyệt điều chỉnh   │
├──────────────────┼────────────────┼──────────────┼──────────────────────────┤
│ STOCK_ADJUSTMENT │ REJECTED       │ Người tạo    │ Admin từ chối điều chỉnh │
├──────────────────┼────────────────┼──────────────┼──────────────────────────┤
│ STOCK_TRANSFER   │ PENDING        │ Admin,       │ Nhân viên submit transfer │
│                  │                │ Manager      │ cần xử lý                │
├──────────────────┼────────────────┼──────────────┼──────────────────────────┤
│ STOCK_TRANSFER   │ COMPLETED      │ Người tạo    │ Transfer hoàn tất        │
├──────────────────┼────────────────┼──────────────┼──────────────────────────┤
│ STOCK_TRANSFER   │ CANCELLED      │ Người tạo    │ Transfer bị hủy          │
├──────────────────┼────────────────┼──────────────┼──────────────────────────┤
│ INBOUND_RECEIPT  │ CONFIRMED      │ Người tạo,   │ Receipt được confirm      │
│                  │                │ Warehouse Mgr │                           │
├──────────────────┼────────────────┼──────────────┼──────────────────────────┤
│ INBOUND_RECEIPT  │ PARTIAL        │ Người tạo    │ Nhận hàng 1 phần        │
├──────────────────┼────────────────┼──────────────┼──────────────────────────┤
│ OUTBOUND_ORDER   │ CONFIRMED      │ Warehouse     │ Sales order được confirm  │
│                  │                │ Staff        │                           │
├──────────────────┼────────────────┼──────────────┼──────────────────────────┤
│ OUTBOUND_SHIPMENT│ PICKING        │ Warehouse     │ Shipment bắt đầu picking │
│                  │                │ Staff        │                           │
├──────────────────┼────────────────┼──────────────┼──────────────────────────┤
│ OUTBOUND_SHIPMENT│ SHIPPED        │ Người tạo,   │ Shipment đã giao         │
│                  │                │ Customer     │                           │
├──────────────────┼────────────────┼──────────────┼──────────────────────────┤
│ OUTBOUND_SHIPMENT│ DELIVERED      │ Người tạo,   │ Giao hàng thành công     │
│                  │                │ Customer     │                           │
├──────────────────┼────────────────┼──────────────┼──────────────────────────┤
│ REPORT           │ COMPLETED      │ Người yêu cầu│ Report generation xong    │
├──────────────────┼────────────────┼──────────────┼──────────────────────────┤
│ REPORT           │ FAILED         │ Người yêu cầu│ Report generation lỗi    │
├──────────────────┼────────────────┼──────────────┼──────────────────────────┤
│ SYSTEM           │ LOW_STOCK      │ Warehouse Mgr │ Tồn kho dưới ngưỡng      │
├──────────────────┼────────────────┼──────────────┼──────────────────────────┤
│ SYSTEM           │ EXPIRY_WARNING │ Warehouse Mgr │ Batch sắp hết hạn        │
└──────────────────┴────────────────┴──────────────┴──────────────────────────┘
```

### 4.5 Notification Payload

#### Backend Entity

```java
@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification extends BaseEntity {
    
    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private NotificationType type;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "module", nullable = false, length = 30)
    private NotificationModule module;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "event", nullable = false, length = 30)
    private NotificationEvent event;
    
    @Column(name = "title", nullable = false, length = 255)
    private String title;
    
    @Column(name = "message", columnDefinition = "TEXT")
    private String message;
    
    @Column(name = "reference_id", length = 36)
    private String referenceId;
    
    @Column(name = "reference_number", length = 50)
    private String referenceNumber;
    
    @Column(name = "reference_url", length = 500)
    private String referenceUrl;
    
    @Column(name = "metadata", columnDefinition = "JSON")
    private String metadata; // JSON string
    
    @Column(name = "is_read")
    private Boolean isRead = false;
    
    @Column(name = "read_at")
    private LocalDateTime readAt;
    
    @Column(name = "created_by", length = 36)
    private String createdBy;
}
```

#### Frontend TypeScript Interface

```typescript
// notification.types.ts

export interface Notification {
  id: string;
  type: NotificationType;
  module: NotificationModule;
  event: NotificationEvent;
  title: string;
  message: string;
  referenceId: string | null;
  referenceNumber: string | null;
  referenceUrl: string | null;
  metadata: NotificationMetadata | null;
  isRead: boolean;
  readAt: string | null;
  createdBy: string | null;
  createdAt: string;
}

export interface NotificationMetadata {
  // Stock Adjustment
  quantityBefore?: number;
  quantityAfter?: number;
  adjustmentDelta?: number;
  reason?: string;
  productName?: string;
  productSku?: string;
  
  // Stock Transfer
  fromLocation?: string;
  fromLocationCode?: string;
  toLocation?: string;
  toLocationCode?: string;
  quantity?: number;
  uomCode?: string;
  
  // Inbound/Outbound
  orderNumber?: string;
  expectedDate?: string;
  actualDate?: string;
  warehouseName?: string;
  
  // Report
  reportType?: string;
  downloadUrl?: string;
  expiresAt?: string;
  
  // System
  threshold?: number;
  currentStock?: number;
  daysUntilExpiry?: number;
}

export enum NotificationType {
  ADJUSTMENT_PENDING = 'ADJUSTMENT_PENDING',
  ADJUSTMENT_APPROVED = 'ADJUSTMENT_APPROVED',
  ADJUSTMENT_REJECTED = 'ADJUSTMENT_REJECTED',
  TRANSFER_PENDING = 'TRANSFER_PENDING',
  TRANSFER_COMPLETED = 'TRANSFER_COMPLETED',
  TRANSFER_CANCELLED = 'TRANSFER_CANCELLED',
  INBOUND_CONFIRMED = 'INBOUND_CONFIRMED',
  INBOUND_PARTIAL = 'INBOUND_PARTIAL',
  OUTBOUND_CONFIRMED = 'OUTBOUND_CONFIRMED',
  OUTBOUND_PICKING = 'OUTBOUND_PICKING',
  OUTBOUND_SHIPPED = 'OUTBOUND_SHIPPED',
  OUTBOUND_DELIVERED = 'OUTBOUND_DELIVERED',
  REPORT_COMPLETED = 'REPORT_COMPLETED',
  REPORT_FAILED = 'REPORT_FAILED',
  LOW_STOCK = 'LOW_STOCK',
  EXPIRY_WARNING = 'EXPIRY_WARNING',
  SYSTEM_ALERT = 'SYSTEM_ALERT',
}

export enum NotificationModule {
  STOCK_ADJUSTMENT = 'STOCK_ADJUSTMENT',
  STOCK_TRANSFER = 'STOCK_TRANSFER',
  INBOUND_RECEIPT = 'INBOUND_RECEIPT',
  OUTBOUND_ORDER = 'OUTBOUND_ORDER',
  OUTBOUND_SHIPMENT = 'OUTBOUND_SHIPMENT',
  REPORT = 'REPORT',
  SYSTEM = 'SYSTEM',
}

export enum NotificationEvent {
  CREATED = 'CREATED',
  PENDING = 'PENDING',
  APPROVED = 'APPROVED',
  REJECTED = 'REJECTED',
  CONFIRMED = 'CONFIRMED',
  COMPLETED = 'COMPLETED',
  CANCELLED = 'CANCELLED',
  SHIPPED = 'SHIPPED',
  DELIVERED = 'DELIVERED',
  PICKING = 'PICKING',
  PROCESSING = 'PROCESSING',
  FAILED = 'FAILED',
  WARNING = 'WARNING',
}
```

---

## 5. WEBSOCKET DESIGN

### 5.1 STOMP Endpoints

```
┌─────────────────────────────────┬──────────────────────────────────────────┐
│ Endpoint                        │ Mô tả                                    │
├─────────────────────────────────┼──────────────────────────────────────────┤
│ /ws                             │ WebSocket handshake endpoint (SockJS)    │
├─────────────────────────────────┼──────────────────────────────────────────┤
│ /app/notification.send          │ Gửi notification (internal use)          │
├─────────────────────────────────┼──────────────────────────────────────────┤
│ /app/notification.mark-read     │ Đánh dấu đã đọc (single/multiple)       │
├─────────────────────────────────┼──────────────────────────────────────────┤
│ /app/notification.mark-all-read │ Đánh dấu tất cả đã đọc                  │
├─────────────────────────────────┼──────────────────────────────────────────┤
│ /user/queue/notifications       │ Private queue - Tất cả notification      │
│                                 │ của user (auto-delivered when online)    │
├─────────────────────────────────┼──────────────────────────────────────────┤
│ /topic/notifications/{userId}   │ Direct notification đến user cụ thể      │
├─────────────────────────────────┼──────────────────────────────────────────┤
│ /topic/adjustments/pending      │ Broadcast: Admin/Manager nhận           │
│                                 │ khi có adjustment mới cần duyệt          │
├─────────────────────────────────┼──────────────────────────────────────────┤
│ /topic/transfers/pending        │ Broadcast: Admin/Manager nhận           │
│                                 │ khi có transfer mới cần xử lý            │
├─────────────────────────────────┼──────────────────────────────────────────┤
│ /topic/inbound/confirmed         │ Broadcast: Khi receipt được confirm     │
├─────────────────────────────────┼──────────────────────────────────────────┤
│ /topic/outbound/{status}        │ Channel theo status                      │
│                                 │ /topic/outbound/shipped                  │
└─────────────────────────────────┴──────────────────────────────────────────┘
```

### 5.2 Message Flow (Online User)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    ONLINE USER - REAL-TIME FLOW                             │
└─────────────────────────────────────────────────────────────────────────────┘

  Domain Event                    WebSocket Flow                    UI Update
       │                               │                                 │
       ▼                               ▼                                 ▼
┌──────────────────┐       ┌──────────────────┐              ┌──────────────────┐
│ StockAdjustment  │       │ STOMP Broadcast   │              │ Toast Popup      │
│ Service.create() │──────▶│ /topic/notif/    │─────────────▶│ + Bell Badge +1  │
│                  │       │   {userId}       │              │                  │
└──────────────────┘       └──────────────────┘              └──────────────────┘
       │                               │                                 │
       │                               ▼                                 ▼
       │                       ┌──────────────────┐              ┌──────────────────┐
       │                       │ Save to DB       │              │ Notification     │
       │                       │ (is_read=false)  │              │ List Updated     │
       └──────────────────────▶│                  │────────────▶│ (optional)       │
                               └──────────────────┘              └──────────────────┘
```

### 5.3 Message Flow (Offline User)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    OFFLINE USER - CATCH-UP FLOW                            │
└─────────────────────────────────────────────────────────────────────────────┘

  User Offline                    User Online                  Catch-up Flow
       │                               │                               │
       ▼                               ▼                               ▼
┌──────────────────┐       ┌──────────────────┐              ┌──────────────────┐
│ StockAdjustment  │       │ WebSocket        │              │ Query:            │
│ Service.create() │──────▶│ Connected        │─────────────▶│ is_read=false     │
│                  │       │                  │              │ AND created_at >   │
└──────────────────┘       └──────────────────┘              │ lastSyncTime      │
       │                               │               └────────┬───────────────┘
       ▼                               ▼                        │
┌──────────────────┐       ┌──────────────────┐               │
│ Save to DB       │       │ STOMP            │               │
│ (is_read=false)   │◀──────│ /user/queue/notif│◀──────────────┘
│                  │       │ (batch send all) │               │
└──────────────────┘       └──────────────────┘               
```

### 5.4 Backend WebSocket Config

```java
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final HandshakeInterceptor authHandshakeInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Enable simple broker for /topic and /queue
        registry.enableSimpleBroker("/topic", "/queue");
        
        // Prefix for messages from client to server
        registry.setApplicationDestinationPrefixes("/app");
        
        // Prefix for user-specific destinations
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .addInterceptors(authHandshakeInterceptor)
                .withSockJS();
        
        // Alternative without SockJS (for native WebSocket)
        registry.addEndpoint("/ws-native")
                .setAllowedOriginPatterns("*")
                .addInterceptors(authHandshakeInterceptor);
    }

    @Override
    public void configureClientInboundChannel(ChannelInterceptorRegistry registry) {
        registry.addInterceptor(new WebSocketChannelInterceptor());
    }
}
```

### 5.5 Frontend WebSocket Service

```typescript
// notification.service.ts
import { Injectable, OnDestroy } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { StompService } from '@stomp/ng2-stompjs';
import { BehaviorSubject, Subject, Observable, interval } from 'rxjs';
import { filter, takeUntil } from 'rxjs/operators';
import { BaseURL } from '../../environments/BaseURL';
import { ApiResponse } from '../dto/response/ApiResponse';
import { PageResponse } from '../dto/response/PageResponse';
import {
  Notification,
  NotificationType,
  NotificationModule,
  NotificationEvent
} from '../types/notification.types';

@Injectable({ providedIn: 'root' })
export class NotificationService implements OnDestroy {
  private readonly apiUrl = `${BaseURL.API_URL}notifications`;
  private readonly wsUrl = BaseURL.WS_URL || `${BaseURL.API_URL.replace('/api', '')}/ws`;
  
  private destroy$ = new Subject<void>();
  private stompSubscription: any;
  
  private notificationsSubject = new BehaviorSubject<Notification[]>([]);
  private unreadCountSubject = new BehaviorSubject<number>(0);
  private newNotificationSubject = new Subject<Notification>();
  private connectionStatusSubject = new BehaviorSubject<boolean>(false);
  
  notifications$ = this.notificationsSubject.asObservable();
  unreadCount$ = this.unreadCountSubject.asObservable();
  newNotification$ = this.newNotificationSubject.asObservable();
  connectionStatus$ = this.connectionStatusSubject.asObservable();

  constructor(private http: HttpClient) {
    this.connect();
    this.startHeartbeat();
  }

  ngOnDestroy(): void {
    this.disconnect();
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Kết nối WebSocket và subscribe notifications
   */
  connect(): void {
    // Sử dụng stompjs với SockJS
    const socket = new SockJS(this.wsUrl);
    const stompClient = Stomp.over(socket);

    stompClient.connect(
      {},
      (frame: any) => {
        console.log('WebSocket Connected:', frame);
        this.connectionStatusSubject.next(true);
        
        // Subscribe private notifications
        this.stompSubscription = stompClient.subscribe(
          '/user/queue/notifications',
          (message: { body: string }) => {
            const notification: Notification = JSON.parse(message.body);
            this.handleNewNotification(notification);
          }
        );
        
        // Subscribe pending adjustments (Admin/Manager)
        stompClient.subscribe(
          '/topic/adjustments/pending',
          (message: { body: string }) => {
            const notification: Notification = JSON.parse(message.body);
            this.handleNewNotification(notification);
          }
        );
        
        // Subscribe pending transfers (Admin/Manager)
        stompClient.subscribe(
          '/topic/transfers/pending',
          (message: { body: string }) => {
            const notification: Notification = JSON.parse(message.body);
            this.handleNewNotification(notification);
          }
        );
        
        // Subscribe inbound confirmations
        stompClient.subscribe(
          '/topic/inbound/confirmed',
          (message: { body: string }) => {
            const notification: Notification = JSON.parse(message.body);
            this.handleNewNotification(notification);
          }
        );

        // Fetch catch-up notifications (unread since last login)
        this.fetchUnreadNotifications();
      },
      (error: any) => {
        console.error('WebSocket Error:', error);
        this.connectionStatusSubject.next(false);
        this.handleReconnect();
      }
    );

    // Store stompClient reference
    (this as any).stompClient = stompClient;
  }

  /**
   * Xử lý notification mới
   */
  private handleNewNotification(notification: Notification): void {
    // Thêm vào list
    const current = this.notificationsSubject.value;
    this.notificationsSubject.next([notification, ...current]);
    
    // Tăng unread count
    if (!notification.isRead) {
      this.unreadCountSubject.next(this.unreadCountSubject.value + 1);
    }
    
    // Emit cho subscriber khác (toast, sound, etc.)
    this.newNotificationSubject.next(notification);
    
    // Show toast notification
    this.showToast(notification);
    
    // Play sound (optional)
    this.playNotificationSound();
  }

  /**
   * Lấy notifications chưa đọc (catch-up khi login)
   */
  private fetchUnreadNotifications(): void {
    this.http.get<ApiResponse<PageResponse<Notification>>>(
      `${this.apiUrl}?isRead=false&size=50`
    ).subscribe({
      next: (response) => {
        this.notificationsSubject.next(response.data.content);
        this.unreadCountSubject.next(response.data.total_elements);
      },
      error: (err) => console.error('Failed to fetch notifications:', err)
    });
  }

  /**
   * Lấy danh sách notifications (phân trang)
   */
  getNotifications(
    page = 0,
    size = 20,
    filters?: {
      isRead?: boolean;
      module?: NotificationModule;
      startDate?: string;
      endDate?: string;
    }
  ): Observable<ApiResponse<PageResponse<Notification>>> {
    let params = new HttpParams()
      .set('page', page)
      .set('size', size);
    
    if (filters) {
      if (filters.isRead !== undefined) params = params.set('isRead', filters.isRead);
      if (filters.module) params = params.set('module', filters.module);
      if (filters.startDate) params = params.set('startDate', filters.startDate);
      if (filters.endDate) params = params.set('endDate', filters.endDate);
    }
    
    return this.http.get<ApiResponse<PageResponse<Notification>>>(this.apiUrl, { params });
  }

  /**
   * Lấy số lượng notification chưa đọc
   */
  getUnreadCount(): Observable<ApiResponse<number>> {
    return this.http.get<ApiResponse<number>>(`${this.apiUrl}/unread-count`);
  }

  /**
   * Đánh dấu đã đọc
   */
  markAsRead(id: string): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/${id}/read`, {}).pipe(
      // Update local state
    );
  }

  /**
   * Đánh dấu tất cả đã đọc
   */
  markAllAsRead(): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/read-all`, {});
  }

  /**
   * Xóa notification
   */
  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  /**
   * Heartbeat để duy trì connection
   */
  private startHeartbeat(): void {
    interval(30000) // 30s
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        if ((this as any).stompClient?.connected) {
          (this as any).stompClient.send('/app/heartbeat', {});
        }
      });
  }

  /**
   * Reconnect khi mất kết nối
   */
  private handleReconnect(): void {
    const maxAttempts = 10;
    let attempts = 0;
    
    const reconnect = () => {
      if (attempts >= maxAttempts) {
        console.error('Max reconnect attempts reached');
        return;
      }
      
      attempts++;
      const delay = 2000 * Math.pow(2, attempts - 1); // Exponential backoff
      
      setTimeout(() => {
        console.log(`Reconnecting... Attempt ${attempts}`);
        this.connect();
      }, Math.min(delay, 60000)); // Max 60s
    };
    
    reconnect();
  }

  /**
   * Disconnect khi logout
   */
  disconnect(): void {
    if ((this as any).stompSubscription) {
      (this as any).stompSubscription.unsubscribe();
    }
    if ((this as any).stompClient) {
      (this as any).stompClient.disconnect();
      (this as any).stompClient = null;
    }
    this.connectionStatusSubject.next(false);
  }

  /**
   * Hiển thị toast notification
   */
  private showToast(notification: Notification): void {
    // Sử dụng ToastrService hoặc custom implementation
    const toastr = (window as any).toastr;
    
    if (toastr) {
      const config: Record<NotificationType, any> = {
        [NotificationType.ADJUSTMENT_APPROVED]: { type: 'success' },
        [NotificationType.ADJUSTMENT_REJECTED]: { type: 'warning' },
        [NotificationType.TRANSFER_COMPLETED]: { type: 'success' },
        [NotificationType.TRANSFER_CANCELLED]: { type: 'error' },
        [NotificationType.ADJUSTMENT_PENDING]: { type: 'info' },
        [NotificationType.TRANSFER_PENDING]: { type: 'info' },
        [NotificationType.REPORT_COMPLETED]: { type: 'success' },
        [NotificationType.REPORT_FAILED]: { type: 'error' },
      };
      
      const toastConfig = config[notification.type] || { type: 'info' };
      
      toastr[toastConfig.type](notification.message, notification.title, {
        timeOut: 5000,
        extendedTimeOut: 3000,
        closeButton: true,
        progressBar: true,
        onclick: () => {
          if (notification.referenceUrl) {
            window.location.href = notification.referenceUrl;
          }
        }
      });
    }
  }

  /**
   * Play notification sound
   */
  private playNotificationSound(): void {
    // Optional: Play notification sound
    const audio = new Audio('/assets/sounds/notification.mp3');
    audio.volume = 0.5;
    audio.play().catch(() => {
      // Ignore autoplay errors
    });
  }
}
```

---

## 6. API ENDPOINTS

### 6.1 REST API Endpoints

```java
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Slf4j
@Validated
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * GET /api/v1/notifications
     * Lấy danh sách notification (phân trang)
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<PageResponse<NotificationResponse>>> getNotifications(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) Boolean isRead,
            @RequestParam(required = false) NotificationModule module,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        PageResponse<NotificationResponse> response = notificationService.getNotifications(
                page, size, isRead, module, startDate, endDate
        );
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /**
     * GET /api/v1/notifications/unread-count
     * Đếm notification chưa đọc
     */
    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<Long>> getUnreadCount() {
        Long count = notificationService.getUnreadCount();
        return ResponseEntity.ok(BaseResponse.success(count));
    }

    /**
     * GET /api/v1/notifications/{id}
     * Chi tiết notification
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<NotificationResponse>> getById(@PathVariable String id) {
        NotificationResponse response = notificationService.getById(id);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /**
     * PUT /api/v1/notifications/{id}/read
     * Đánh dấu đã đọc
     */
    @PutMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<Void>> markAsRead(@PathVariable String id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok(BaseResponse.success());
    }

    /**
     * PUT /api/v1/notifications/read-all
     * Đánh dấu tất cả đã đọc
     */
    @PutMapping("/read-all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<Void>> markAllAsRead() {
        notificationService.markAllAsRead();
        return ResponseEntity.ok(BaseResponse.success());
    }

    /**
     * PUT /api/v1/notifications/read-by-ids
     * Đánh dấu nhiều notification đã đọc
     */
    @PutMapping("/read-by-ids")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<Integer>> markAsReadByIds(
            @RequestBody @NotEmpty List<String> ids
    ) {
        Integer count = notificationService.markAsReadByIds(ids);
        return ResponseEntity.ok(BaseResponse.success(count));
    }

    /**
     * DELETE /api/v1/notifications/{id}
     * Xóa notification
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<Void>> delete(@PathVariable String id) {
        notificationService.delete(id);
        return ResponseEntity.ok(BaseResponse.success());
    }

    /**
     * DELETE /api/v1/notifications/clear-read
     * Xóa tất cả notification đã đọc
     */
    @DeleteMapping("/clear-read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<Integer>> clearReadNotifications() {
        Integer count = notificationService.clearReadNotifications();
        return ResponseEntity.ok(BaseResponse.success(count));
    }
}
```

### 6.2 Frontend API Service

```typescript
// notification-api.service.ts
import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { BaseURL } from '../../environments/BaseURL';
import { ApiResponse } from '../dto/response/ApiResponse';
import { PageResponse } from '../dto/response/PageResponse';
import { Notification, NotificationModule } from '../types/notification.types';

export interface GetNotificationsParams {
  page?: number;
  size?: number;
  isRead?: boolean;
  module?: NotificationModule;
  startDate?: string;
  endDate?: string;
}

@Injectable({ providedIn: 'root' })
export class NotificationApiService {
  private readonly apiUrl = `${BaseURL.API_URL}notifications`;

  constructor(private http: HttpClient) {}

  getNotifications(params?: GetNotificationsParams): Observable<ApiResponse<PageResponse<Notification>>> {
    let httpParams = new HttpParams();
    
    if (params) {
      if (params.page !== undefined) httpParams = httpParams.set('page', params.page);
      if (params.size !== undefined) httpParams = httpParams.set('size', params.size);
      if (params.isRead !== undefined) httpParams = httpParams.set('isRead', params.isRead);
      if (params.module) httpParams = httpParams.set('module', params.module);
      if (params.startDate) httpParams = httpParams.set('startDate', params.startDate);
      if (params.endDate) httpParams = httpParams.set('endDate', params.endDate);
    }
    
    return this.http.get<ApiResponse<PageResponse<Notification>>>(this.apiUrl, { params: httpParams });
  }

  getUnreadCount(): Observable<ApiResponse<number>> {
    return this.http.get<ApiResponse<number>>(`${this.apiUrl}/unread-count`);
  }

  getById(id: string): Observable<ApiResponse<Notification>> {
    return this.http.get<ApiResponse<Notification>>(`${this.apiUrl}/${id}`);
  }

  markAsRead(id: string): Observable<ApiResponse<void>> {
    return this.http.put<ApiResponse<void>>(`${this.apiUrl}/${id}/read`, {});
  }

  markAllAsRead(): Observable<ApiResponse<void>> {
    return this.http.put<ApiResponse<void>>(`${this.apiUrl}/read-all`, {});
  }

  markAsReadByIds(ids: string[]): Observable<ApiResponse<number>> {
    return this.http.put<ApiResponse<number>>(`${this.apiUrl}/read-by-ids`, ids);
  }

  delete(id: string): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.apiUrl}/${id}`);
  }

  clearReadNotifications(): Observable<ApiResponse<number>> {
    return this.http.delete<ApiResponse<number>>(`${this.apiUrl}/clear-read`);
  }
}
```

---

## 7. CHI TIẾT IMPLEMENTATION

### 7.1 Backend - NotificationService

```java
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final RedisTemplate<String, String> redisTemplate;
    private final NotificationMapper notificationMapper;
    
    private static final String REDIS_ONLINE_KEY = "ws:online:";
    private static final long ONLINE_TTL_SECONDS = 300; // 5 minutes

    /**
     * Tạo và gửi notification
     * - Lưu vào DB (persistence)
     * - Gửi qua WebSocket nếu user online
     */
    public NotificationResponse createAndSend(NotificationRequest request) {
        // 1. Save to database
        Notification notification = notificationMapper.toEntity(request);
        notification = notificationRepository.save(notification);
        
        // 2. Send via WebSocket (async, non-blocking)
        sendRealTimeNotification(notification);
        
        // 3. Update unread count in Redis
        incrementUnreadCount(notification.getUserId());
        
        log.info("Created notification: id={}, userId={}, type={}", 
                notification.getId(), notification.getUserId(), notification.getType());
        
        return notificationMapper.toResponse(notification);
    }
    
    /**
     * Gửi notification qua WebSocket
     * - Check Redis for user online status
     * - Send to /user/queue/notifications if online
     */
    @Async
    protected void sendRealTimeNotification(Notification notification) {
        String userId = notification.getUserId();
        String onlineKey = REDIS_ONLINE_KEY + userId;
        
        try {
            // Check user online status via Redis
            Boolean isOnline = redisTemplate.hasKey(onlineKey);
            
            if (Boolean.TRUE.equals(isOnline)) {
                // User online - send via WebSocket
                NotificationWebSocketMessage wsMessage = notificationMapper.toWebSocketMessage(notification);
                
                messagingTemplate.convertAndSendToUser(
                    userId,
                    "/queue/notifications",
                    wsMessage
                );
                
                log.debug("Sent real-time notification to user: {}", userId);
            } else {
                // User offline - already saved in DB, will receive on next login
                log.debug("User {} is offline, notification saved for catch-up", userId);
            }
        } catch (Exception e) {
            log.error("Failed to send real-time notification to user: {}", userId, e);
            // Notification đã được save vào DB, nên không ảnh hưởng
        }
    }
    
    /**
     * Gửi notification đến nhiều user (broadcast)
     */
    public List<NotificationResponse> broadcastToUsers(List<String> userIds, NotificationRequest request) {
        return userIds.stream()
            .map(userId -> {
                request.setUserId(userId);
                return createAndSend(request);
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Broadcast đến tất cả admin/manager
     */
    public void broadcastToAdminsAndManagers(NotificationRequest request) {
        List<String> adminManagerIds = userRepository.findByRolesIn(Arrays.asList("ADMIN", "MANAGER"))
            .stream()
            .map(User::getId)
            .collect(Collectors.toList());
        
        broadcastToUsers(adminManagerIds, request);
    }
    
    /**
     * Lấy danh sách notification (phân trang)
     */
    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getNotifications(
            Integer page, Integer size,
            Boolean isRead, NotificationModule module,
            LocalDate startDate, LocalDate endDate
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        Page<Notification> notificationPage = notificationRepository.findByFilters(
                SecurityUtils.getCurrentUserId(),
                isRead,
                module,
                startDate,
                endDate,
                pageable
        );
        
        List<NotificationResponse> content = notificationPage.getContent()
            .stream()
            .map(notificationMapper::toResponse)
            .collect(Collectors.toList());
        
        return PageResponse.from(notificationPage, content);
    }
    
    /**
     * Lấy số lượng notification chưa đọc
     */
    @Transactional(readOnly = true)
    public Long getUnreadCount() {
        String userId = SecurityUtils.getCurrentUserId();
        
        // Thử lấy từ Redis cache trước
        String cachedCount = redisTemplate.opsForValue().get("notif:unread:" + userId);
        if (cachedCount != null) {
            return Long.parseLong(cachedCount);
        }
        
        // Query từ database
        Long count = notificationRepository.countByUserIdAndIsReadFalse(userId);
        
        // Cache lại
        redisTemplate.opsForValue().set("notif:unread:" + userId, String.valueOf(count), Duration.ofMinutes(5));
        
        return count;
    }
    
    /**
     * Đánh dấu đã đọc
     */
    public void markAsRead(String id) {
        String userId = SecurityUtils.getCurrentUserId();
        
        Notification notification = notificationRepository.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new NotFoundException("Notification not found", ErrorCode.NOTI_001));
        
        if (!Boolean.TRUE.equals(notification.getIsRead())) {
            notification.setIsRead(true);
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);
            
            // Decrement Redis cache
            decrementUnreadCount(userId);
        }
    }
    
    /**
     * Đánh dấu tất cả đã đọc
     */
    public int markAllAsRead() {
        String userId = SecurityUtils.getCurrentUserId();
        int count = notificationRepository.markAllAsRead(userId, LocalDateTime.now());
        
        // Reset Redis cache
        redisTemplate.opsForValue().set("notif:unread:" + userId, "0");
        
        return count;
    }
    
    private void incrementUnreadCount(String userId) {
        String key = "notif:unread:" + userId;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            redisTemplate.opsForValue().increment(key);
        }
    }
    
    private void decrementUnreadCount(String userId) {
        String key = "notif:unread:" + userId;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            Long current = redisTemplate.opsForValue().decrement(key);
            if (current != null && current < 0) {
                redisTemplate.opsForValue().set(key, "0");
            }
        }
    }
}
```

### 7.2 Backend - Domain Event Listeners

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    /**
     * Stock Adjustment Created → Notify Admin/Manager
     */
    @EventListener
    @Async
    public void handleAdjustmentCreated(StockAdjustmentCreatedEvent event) {
        // Lấy danh sách Admin/Manager
        List<String> adminManagerIds = userRepository.findByRolesIn(Arrays.asList("ADMIN", "MANAGER"))
            .stream()
            .map(User::getId)
            .collect(Collectors.toList());
        
        // Tạo notification cho mỗi admin/manager
        NotificationRequest request = NotificationRequest.builder()
            .userId(null) // Sẽ được set trong broadcastToUsers
            .type(NotificationType.ADJUSTMENT_PENDING)
            .module(NotificationModule.STOCK_ADJUSTMENT)
            .event(NotificationEvent.PENDING)
            .title("Có phiếu điều chỉnh mới cần duyệt")
            .message(String.format("%s tạo điều chỉnh %s cho %s: %s", 
                event.getCreatedByName(),
                event.getAdjustmentNumber(),
                event.getProductName(),
                event.getDelta() > 0 ? "+" + event.getDelta() : String.valueOf(event.getDelta())))
            .referenceId(event.getAdjustmentId())
            .referenceNumber(event.getAdjustmentNumber())
            .referenceUrl("/stock/adjustments/" + event.getAdjustmentId())
            .metadata(Map.of(
                "quantityBefore", event.getQuantityBefore(),
                "quantityAfter", event.getQuantityAfter(),
                "delta", event.getDelta(),
                "reason", event.getReason().name(),
                "productName", event.getProductName(),
                "productSku", event.getProductSku()
            ))
            .createdBy(event.getCreatedBy())
            .build();
        
        notificationService.broadcastToUsers(adminManagerIds, request);
        
        log.info("Sent pending notification to {} admin/managers for adjustment: {}", 
                adminManagerIds.size(), event.getAdjustmentNumber());
    }
    
    /**
     * Stock Adjustment Approved → Notify Creator
     */
    @EventListener
    @Async
    public void handleAdjustmentApproved(StockAdjustmentApprovedEvent event) {
        NotificationRequest request = NotificationRequest.builder()
            .userId(event.getCreatedBy()) // Người tạo
            .type(NotificationType.ADJUSTMENT_APPROVED)
            .module(NotificationModule.STOCK_ADJUSTMENT)
            .event(NotificationEvent.APPROVED)
            .title("Phiếu điều chỉnh đã được duyệt")
            .message(String.format("Admin đã duyệt phiếu %s. Tồn kho: %s → %s", 
                event.getAdjustmentNumber(),
                event.getQuantityBefore(),
                event.getQuantityAfter()))
            .referenceId(event.getAdjustmentId())
            .referenceNumber(event.getAdjustmentNumber())
            .referenceUrl("/stock/adjustments/" + event.getAdjustmentId())
            .metadata(Map.of(
                "quantityBefore", event.getQuantityBefore(),
                "quantityAfter", event.getQuantityAfter(),
                "delta", event.getDelta(),
                "approvedBy", event.getApprovedBy()
            ))
            .createdBy("SYSTEM")
            .build();
        
        notificationService.createAndSend(request);
    }
    
    /**
     * Stock Adjustment Rejected → Notify Creator
     */
    @EventListener
    @Async
    public void handleAdjustmentRejected(StockAdjustmentRejectedEvent event) {
        NotificationRequest request = NotificationRequest.builder()
            .userId(event.getCreatedBy())
            .type(NotificationType.ADJUSTMENT_REJECTED)
            .module(NotificationModule.STOCK_ADJUSTMENT)
            .event(NotificationEvent.REJECTED)
            .title("Phiếu điều chỉnh bị từ chối")
            .message(String.format("Admin đã từ chối phiếu %s. Lý do: %s", 
                event.getAdjustmentNumber(),
                event.getRejectionReason()))
            .referenceId(event.getAdjustmentId())
            .referenceNumber(event.getAdjustmentNumber())
            .referenceUrl("/stock/adjustments/" + event.getAdjustmentId())
            .metadata(Map.of(
                "rejectionReason", event.getRejectionReason(),
                "rejectedBy", event.getRejectedBy()
            ))
            .createdBy("SYSTEM")
            .build();
        
        notificationService.createAndSend(request);
    }
    
    /**
     * Stock Transfer Submitted → Notify Admin/Manager
     */
    @EventListener
    @Async
    public void handleTransferSubmitted(StockTransferSubmittedEvent event) {
        List<String> adminManagerIds = userRepository.findByRolesIn(Arrays.asList("ADMIN", "MANAGER"))
            .stream()
            .map(User::getId)
            .collect(Collectors.toList());
        
        NotificationRequest request = NotificationRequest.builder()
            .type(NotificationType.TRANSFER_PENDING)
            .module(NotificationModule.STOCK_TRANSFER)
            .event(NotificationEvent.PENDING)
            .title("Có phiếu chuyển kho mới cần xử lý")
            .message(String.format("%s tạo phiếu chuyển %s: %s %s từ %s → %s",
                event.getCreatedByName(),
                event.getTransferNumber(),
                event.getQuantity(),
                event.getUomCode(),
                event.getFromLocationCode(),
                event.getToLocationCode()))
            .referenceId(event.getTransferId())
            .referenceNumber(event.getTransferNumber())
            .referenceUrl("/stock/transfers/" + event.getTransferId())
            .metadata(Map.of(
                "quantity", event.getQuantity(),
                "fromLocation", event.getFromLocationCode(),
                "toLocation", event.getToLocationCode(),
                "productName", event.getProductName()
            ))
            .createdBy(event.getCreatedBy())
            .build();
        
        notificationService.broadcastToUsers(adminManagerIds, request);
    }
    
    /**
     * Stock Transfer Completed → Notify Creator
     */
    @EventListener
    @Async
    public void handleTransferCompleted(StockTransferCompletedEvent event) {
        NotificationRequest request = NotificationRequest.builder()
            .userId(event.getCreatedBy())
            .type(NotificationType.TRANSFER_COMPLETED)
            .module(NotificationModule.STOCK_TRANSFER)
            .event(NotificationEvent.COMPLETED)
            .title("Phiếu chuyển kho đã hoàn tất")
            .message(String.format("Phiếu %s đã hoàn tất. Đã chuyển %s %s từ %s → %s",
                event.getTransferNumber(),
                event.getQuantity(),
                event.getUomCode(),
                event.getFromLocationCode(),
                event.getToLocationCode()))
            .referenceId(event.getTransferId())
            .referenceNumber(event.getTransferNumber())
            .referenceUrl("/stock/transfers/" + event.getTransferId())
            .createdBy("SYSTEM")
            .build();
        
        notificationService.createAndSend(request);
    }
    
    /**
     * Stock Transfer Cancelled → Notify Creator
     */
    @EventListener
    @Async
    public void handleTransferCancelled(StockTransferCancelledEvent event) {
        NotificationRequest request = NotificationRequest.builder()
            .userId(event.getCreatedBy())
            .type(NotificationType.TRANSFER_CANCELLED)
            .module(NotificationModule.STOCK_TRANSFER)
            .event(NotificationEvent.CANCELLED)
            .title("Phiếu chuyển kho đã bị hủy")
            .message(String.format("Phiếu %s đã bị hủy bởi %s",
                event.getTransferNumber(),
                event.getCancelledByName()))
            .referenceId(event.getTransferId())
            .referenceNumber(event.getTransferNumber())
            .referenceUrl("/stock/transfers/" + event.getTransferId())
            .createdBy("SYSTEM")
            .build();
        
        notificationService.createAndSend(request);
    }
}
```

### 7.3 Backend - WebSocket Session Management

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketSessionManager {

    private final RedisTemplate<String, String> redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    
    private static final String SESSION_KEY_PREFIX = "ws:session:";
    private static final String ONLINE_KEY_PREFIX = "ws:online:";
    private static final long SESSION_TTL_SECONDS = 3600; // 1 hour

    /**
     * Handle user connected
     */
    public void handleConnected(String sessionId, String userId, String deviceInfo) {
        // Store session info in Redis
        Map<String, String> sessionData = Map.of(
            "userId", userId,
            "sessionId", sessionId,
            "deviceInfo", deviceInfo != null ? deviceInfo : "unknown",
            "connectedAt", LocalDateTime.now().toString()
        );
        
        redisTemplate.opsForHash().putAll(SESSION_KEY_PREFIX + sessionId, sessionData);
        redisTemplate.expire(SESSION_KEY_PREFIX + sessionId, Duration.ofSeconds(SESSION_TTL_SECONDS));
        
        // Mark user as online
        redisTemplate.opsForValue().set(
            ONLINE_KEY_PREFIX + userId, 
            sessionId, 
            Duration.ofSeconds(SESSION_TTL_SECONDS)
        );
        
        // Store user's active sessions
        redisTemplate.opsForSet().add("ws:user:sessions:" + userId, sessionId);
        
        log.info("User {} connected with session {}", userId, sessionId);
        
        // Send online presence update to relevant users (e.g., admin dashboard)
        broadcastUserPresence(userId, true);
    }

    /**
     * Handle user disconnected
     */
    public void handleDisconnected(String sessionId) {
        Map<Object, Object> sessionData = redisTemplate.opsForHash().entries(SESSION_KEY_PREFIX + sessionId);
        
        if (!sessionData.isEmpty()) {
            String userId = (String) sessionData.get("userId");
            
            // Remove session
            redisTemplate.delete(SESSION_KEY_PREFIX + sessionId);
            
            // Remove from user's sessions
            redisTemplate.opsForSet().remove("ws:user:sessions:" + userId, sessionId);
            
            // Check if user has other active sessions
            Set<String> remainingSessions = redisTemplate.opsForSet().members("ws:user:sessions:" + userId);
            
            if (remainingSessions == null || remainingSessions.isEmpty()) {
                // No more sessions, mark as offline
                redisTemplate.delete(ONLINE_KEY_PREFIX + userId);
                
                log.info("User {} is now offline", userId);
                broadcastUserPresence(userId, false);
            } else {
                // Update online key with another session
                String activeSession = remainingSessions.iterator().next();
                redisTemplate.opsForValue().set(
                    ONLINE_KEY_PREFIX + userId,
                    activeSession,
                    Duration.ofSeconds(SESSION_TTL_SECONDS)
                );
            }
        }
    }

    /**
     * Handle heartbeat/ping from client
     */
    public void handleHeartbeat(String sessionId) {
        // Refresh TTL
        redisTemplate.expire(SESSION_KEY_PREFIX + sessionId, Duration.ofSeconds(SESSION_TTL_SECONDS));
        
        Map<Object, Object> sessionData = redisTemplate.opsForHash().entries(SESSION_KEY_PREFIX + sessionId);
        if (!sessionData.isEmpty()) {
            String userId = (String) sessionData.get("userId");
            redisTemplate.expire(ONLINE_KEY_PREFIX + userId, Duration.ofSeconds(SESSION_TTL_SECONDS));
        }
    }

    /**
     * Check if user is online
     */
    public boolean isUserOnline(String userId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(ONLINE_KEY_PREFIX + userId));
    }

    /**
     * Get all online users
     */
    public Set<String> getOnlineUsers() {
        Set<String> keys = redisTemplate.keys(ONLINE_KEY_PREFIX + "*");
        if (keys == null) return Collections.emptySet();
        
        return keys.stream()
            .map(key -> key.replace(ONLINE_KEY_PREFIX, ""))
            .collect(Collectors.toSet());
    }

    /**
     * Broadcast user presence changes
     */
    private void broadcastUserPresence(String userId, boolean isOnline) {
        Map<String, Object> presenceData = Map.of(
            "userId", userId,
            "isOnline", isOnline,
            "timestamp", LocalDateTime.now().toString()
        );
        
        messagingTemplate.convertAndSend("/topic/presence", presenceData);
    }
}
```

### 7.4 Frontend - Notification Bell Component

```typescript
// notification-bell.component.ts
import { Component, OnInit, OnDestroy, Input } from '@angular/core';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { filter } from 'rxjs/operators';
import { NotificationService } from '../../services/notification.service';
import { Notification, NotificationType, NotificationModule } from '../../types/notification.types';

@Component({
  selector: 'app-notification-bell',
  template: `
    <div class="notification-bell-wrapper" *ngIf="isLoggedIn">
      <!-- Connection Status Indicator -->
      <div class="connection-status" [class.connected]="isConnected" [class.disconnected]="!isConnected">
        <span class="status-dot"></span>
      </div>
      
      <!-- Bell Icon with Badge -->
      <div class="notification-bell" (click)="toggleDropdown()">
        <i class="fa-bell"></i>
        <span class="badge" *ngIf="unreadCount > 0">
          {{ unreadCount > 99 ? '99+' : unreadCount }}
        </span>
      </div>
      
      <!-- Dropdown -->
      <div class="notification-dropdown" *ngIf="showDropdown" (click)="$event.stopPropagation()">
        <div class="dropdown-header">
          <span class="title">Thông báo</span>
          <button class="btn-link" (click)="markAllAsRead()" *ngIf="unreadCount > 0">
            Đánh dấu tất cả đã đọc
          </button>
        </div>
        
        <!-- Tabs -->
        <div class="dropdown-tabs">
          <button 
            class="tab" 
            [class.active]="activeTab === 'all'"
            (click)="activeTab = 'all'">
            Tất cả
          </button>
          <button 
            class="tab" 
            [class.active]="activeTab === 'unread'"
            (click)="activeTab = 'unread'; loadUnread()">
            Chưa đọc
            <span class="tab-badge" *ngIf="unreadCount > 0">{{ unreadCount }}</span>
          </button>
        </div>
        
        <div class="dropdown-body" (click)="$event.stopPropagation()">
          <!-- Notification List -->
          <div class="notification-list" *ngIf="!loading; else loadingTemplate">
            <div class="notification-item" 
                 *ngFor="let notification of displayedNotifications"
                 [class.unread]="!notification.isRead"
                 (click)="onNotificationClick(notification)">
              <div class="notification-icon" [ngClass]="getIconClass(notification.type)">
                <i [ngClass]="getIcon(notification.type)"></i>
              </div>
              <div class="notification-content">
                <div class="notification-title">{{ notification.title }}</div>
                <div class="notification-message">{{ notification.message }}</div>
                <div class="notification-time">{{ notification.createdAt | date:'HH:mm, dd/MM/yyyy' }}</div>
              </div>
              <div class="notification-actions" *ngIf="!notification.isRead">
                <button class="btn-mark-read" (click)="markAsRead($event, notification.id)" title="Đánh dấu đã đọc">
                  <i class="fa-check"></i>
                </button>
              </div>
              <div class="notification-unread-dot" *ngIf="!notification.isRead"></div>
            </div>
            
            <!-- Empty State -->
            <div class="empty-state" *ngIf="displayedNotifications.length === 0">
              <i class="fa-bell-slash"></i>
              <p>Không có thông báo nào</p>
            </div>
          </div>
          
          <ng-template #loadingTemplate>
            <div class="loading-state">
              <i class="fa-spinner fa-spin"></i>
              <span>Đang tải...</span>
            </div>
          </ng-template>
        </div>
        
        <div class="dropdown-footer">
          <a href="/notifications" (click)="showDropdown = false">Xem tất cả thông báo</a>
        </div>
      </div>
    </div>
    
    <!-- Click outside to close -->
    <div class="notification-backdrop" *ngIf="showDropdown" (click)="showDropdown = false"></div>
  `,
  styles: [`
    .notification-bell-wrapper {
      position: relative;
      display: inline-flex;
      align-items: center;
    }
    
    .connection-status {
      position: absolute;
      top: -2px;
      right: -2px;
      z-index: 1;
    }
    
    .connection-status .status-dot {
      display: block;
      width: 8px;
      height: 8px;
      border-radius: 50%;
    }
    
    .connection-status.connected .status-dot {
      background: #28a745;
    }
    
    .connection-status.disconnected .status-dot {
      background: #dc3545;
      animation: pulse 1.5s infinite;
    }
    
    @keyframes pulse {
      0%, 100% { opacity: 1; }
      50% { opacity: 0.5; }
    }
    
    .notification-bell {
      position: relative;
      cursor: pointer;
      padding: 8px;
      font-size: 18px;
      color: #666;
      transition: color 0.2s;
    }
    
    .notification-bell:hover {
      color: #333;
    }
    
    .notification-bell .badge {
      position: absolute;
      top: 2px;
      right: 2px;
      min-width: 18px;
      height: 18px;
      padding: 0 4px;
      font-size: 10px;
      font-weight: 600;
      line-height: 18px;
      text-align: center;
      background: #dc3545;
      color: #fff;
      border-radius: 9px;
    }
    
    .notification-dropdown {
      position: absolute;
      top: 100%;
      right: 0;
      width: 380px;
      max-height: 500px;
      margin-top: 8px;
      background: #fff;
      border-radius: 8px;
      box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
      z-index: 1000;
      display: flex;
      flex-direction: column;
    }
    
    .dropdown-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 12px 16px;
      border-bottom: 1px solid #eee;
    }
    
    .dropdown-header .title {
      font-weight: 600;
      font-size: 16px;
    }
    
    .dropdown-tabs {
      display: flex;
      padding: 0 16px;
      border-bottom: 1px solid #eee;
    }
    
    .dropdown-tabs .tab {
      flex: 1;
      padding: 10px;
      background: none;
      border: none;
      border-bottom: 2px solid transparent;
      cursor: pointer;
      font-size: 13px;
      color: #666;
      transition: all 0.2s;
    }
    
    .dropdown-tabs .tab.active {
      color: #007bff;
      border-bottom-color: #007bff;
    }
    
    .dropdown-tabs .tab-badge {
      display: inline-block;
      margin-left: 4px;
      padding: 2px 6px;
      font-size: 11px;
      background: #dc3545;
      color: #fff;
      border-radius: 10px;
    }
    
    .dropdown-body {
      flex: 1;
      overflow-y: auto;
      max-height: 350px;
    }
    
    .notification-list {
      padding: 8px 0;
    }
    
    .notification-item {
      position: relative;
      display: flex;
      align-items: flex-start;
      padding: 12px 16px;
      cursor: pointer;
      transition: background 0.2s;
    }
    
    .notification-item:hover {
      background: #f8f9fa;
    }
    
    .notification-item.unread {
      background: #f0f7ff;
    }
    
    .notification-item.unread:hover {
      background: #e3f0ff;
    }
    
    .notification-icon {
      width: 36px;
      height: 36px;
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: 50%;
      margin-right: 12px;
      font-size: 16px;
    }
    
    .notification-icon.text-success {
      background: #d4edda;
      color: #28a745;
    }
    
    .notification-icon.text-warning {
      background: #fff3cd;
      color: #ffc107;
    }
    
    .notification-icon.text-danger {
      background: #f8d7da;
      color: #dc3545;
    }
    
    .notification-icon.text-info {
      background: #d1ecf1;
      color: #17a2b8;
    }
    
    .notification-content {
      flex: 1;
      min-width: 0;
    }
    
    .notification-title {
      font-weight: 500;
      font-size: 13px;
      margin-bottom: 2px;
    }
    
    .notification-message {
      font-size: 12px;
      color: #666;
      overflow: hidden;
      text-overflow: ellipsis;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
    }
    
    .notification-time {
      font-size: 11px;
      color: #999;
      margin-top: 4px;
    }
    
    .notification-actions {
      margin-left: 8px;
    }
    
    .notification-actions .btn-mark-read {
      padding: 4px 8px;
      background: none;
      border: 1px solid #ddd;
      border-radius: 4px;
      cursor: pointer;
      color: #666;
      font-size: 12px;
    }
    
    .notification-actions .btn-mark-read:hover {
      background: #f8f9fa;
      color: #333;
    }
    
    .notification-unread-dot {
      position: absolute;
      top: 50%;
      right: 16px;
      transform: translateY(-50%);
      width: 8px;
      height: 8px;
      background: #007bff;
      border-radius: 50%;
    }
    
    .empty-state {
      padding: 40px 20px;
      text-align: center;
      color: #999;
    }
    
    .empty-state i {
      font-size: 32px;
      margin-bottom: 12px;
    }
    
    .loading-state {
      padding: 40px;
      text-align: center;
      color: #666;
    }
    
    .loading-state i {
      margin-bottom: 8px;
    }
    
    .dropdown-footer {
      padding: 12px 16px;
      text-align: center;
      border-top: 1px solid #eee;
    }
    
    .dropdown-footer a {
      color: #007bff;
      text-decoration: none;
      font-size: 13px;
    }
    
    .dropdown-footer a:hover {
      text-decoration: underline;
    }
    
    .notification-backdrop {
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      z-index: 999;
    }
  `]
})
export class NotificationBellComponent implements OnInit, OnDestroy {
  @Input() isLoggedIn = false;
  
  notifications: Notification[] = [];
  unreadCount = 0;
  showDropdown = false;
  loading = false;
  activeTab: 'all' | 'unread' = 'all';
  isConnected = false;
  
  private subscriptions = new Subscription();

  constructor(
    private notificationService: NotificationService,
    private router: Router
  ) {}

  ngOnInit(): void {
    // Subscribe to notifications
    this.subscriptions.add(
      this.notificationService.notifications$.subscribe(notifications => {
        this.notifications = notifications;
      })
    );
    
    // Subscribe to unread count
    this.subscriptions.add(
      this.notificationService.unreadCount$.subscribe(count => {
        this.unreadCount = count;
      })
    );
    
    // Subscribe to connection status
    this.subscriptions.add(
      this.notificationService.connectionStatus$.subscribe(status => {
        this.isConnected = status;
      })
    );
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }

  get displayedNotifications(): Notification[] {
    if (this.activeTab === 'unread') {
      return this.notifications.filter(n => !n.isRead);
    }
    return this.notifications;
  }

  toggleDropdown(): void {
    this.showDropdown = !this.showDropdown;
    
    if (this.showDropdown && this.notifications.length === 0) {
      this.loadNotifications();
    }
  }

  loadNotifications(): void {
    this.loading = true;
    this.notificationService.getNotifications({ size: 50 }).subscribe({
      next: (response) => {
        this.notifications = response.data.content;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  loadUnread(): void {
    this.loading = true;
    this.notificationService.getNotifications({ isRead: false, size: 50 }).subscribe({
      next: (response) => {
        this.notifications = response.data.content;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  onNotificationClick(notification: Notification): void {
    if (!notification.isRead) {
      this.notificationService.markAsRead(notification.id).subscribe();
    }
    
    if (notification.referenceUrl) {
      this.router.navigateByUrl(notification.referenceUrl);
    }
    
    this.showDropdown = false;
  }

  markAsRead(event: Event, id: string): void {
    event.stopPropagation();
    this.notificationService.markAsRead(id).subscribe();
  }

  markAllAsRead(): void {
    this.notificationService.markAllAsRead().subscribe();
  }

  getIcon(type: NotificationType): string {
    const icons: Record<NotificationType, string> = {
      [NotificationType.ADJUSTMENT_APPROVED]: 'fa-check-circle',
      [NotificationType.ADJUSTMENT_REJECTED]: 'fa-times-circle',
      [NotificationType.ADJUSTMENT_PENDING]: 'fa-clock',
      [NotificationType.TRANSFER_COMPLETED]: 'fa-check',
      [NotificationType.TRANSFER_CANCELLED]: 'fa-times',
      [NotificationType.TRANSFER_PENDING]: 'fa-clock',
      [NotificationType.INBOUND_CONFIRMED]: 'fa-truck-loading',
      [NotificationType.OUTBOUND_SHIPPED]: 'fa-truck',
      [NotificationType.OUTBOUND_DELIVERED]: 'fa-box-open',
      [NotificationType.REPORT_COMPLETED]: 'fa-file-alt',
      [NotificationType.REPORT_FAILED]: 'fa-exclamation-triangle',
      [NotificationType.LOW_STOCK]: 'fa-exclamation-circle',
      [NotificationType.EXPIRY_WARNING]: 'fa-hourglass-half',
      [NotificationType.SYSTEM_ALERT]: 'fa-bell'
    };
    return icons[type] || 'fa-bell';
  }

  getIconClass(type: NotificationType): string {
    const classes: Record<NotificationType, string> = {
      [NotificationType.ADJUSTMENT_APPROVED]: 'text-success',
      [NotificationType.ADJUSTMENT_REJECTED]: 'text-danger',
      [NotificationType.ADJUSTMENT_PENDING]: 'text-warning',
      [NotificationType.TRANSFER_COMPLETED]: 'text-success',
      [NotificationType.TRANSFER_CANCELLED]: 'text-danger',
      [NotificationType.TRANSFER_PENDING]: 'text-warning',
      [NotificationType.INBOUND_CONFIRMED]: 'text-info',
      [NotificationType.OUTBOUND_SHIPPED]: 'text-info',
      [NotificationType.OUTBOUND_DELIVERED]: 'text-success',
      [NotificationType.REPORT_COMPLETED]: 'text-success',
      [NotificationType.REPORT_FAILED]: 'text-danger',
      [NotificationType.LOW_STOCK]: 'text-warning',
      [NotificationType.EXPIRY_WARNING]: 'text-warning',
      [NotificationType.SYSTEM_ALERT]: 'text-info'
    };
    return classes[type] || 'text-info';
  }
}
```

### 7.5 Frontend - Notification History Page

```typescript
// notification-list.component.ts
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { NotificationService } from '../../services/notification.service';
import { Notification, NotificationModule, NotificationType } from '../../types/notification.types';

@Component({
  selector: 'app-notification-list',
  template: `
    <div class="notification-list-page">
      <div class="page-header">
        <h1>Thông báo</h1>
        <div class="header-actions">
          <button class="btn btn-outline-secondary" (click)="markAllAsRead()" 
                  *ngIf="hasUnread">
            <i class="fa-check-double"></i>
            Đánh dấu tất cả đã đọc
          </button>
        </div>
      </div>
      
      <!-- Filters -->
      <div class="filters-bar">
        <div class="filter-group">
          <label>Lọc theo:</label>
          <select [(ngModel)]="selectedModule" (change)="onFilterChange()">
            <option [ngValue]="null">Tất cả modules</option>
            <option *ngFor="let module of modules" [ngValue]="module.value">
              {{ module.label }}
            </option>
          </select>
        </div>
        
        <div class="filter-group">
          <label>Trạng thái:</label>
          <select [(ngModel)]="selectedStatus" (change)="onFilterChange()">
            <option [ngValue]="null">Tất cả</option>
            <option [ngValue]="false">Chưa đọc</option>
            <option [ngValue]="true">Đã đọc</option>
          </select>
        </div>
        
        <div class="filter-group">
          <label>Từ ngày:</label>
          <input type="date" [(ngModel)]="startDate" (change)="onFilterChange()">
        </div>
        
        <div class="filter-group">
          <label>Đến ngày:</label>
          <input type="date" [(ngModel)]="endDate" (change)="onFilterChange()">
        </div>
        
        <button class="btn btn-outline-secondary" (click)="resetFilters()">
          <i class="fa-redo"></i>
          Reset
        </button>
      </div>
      
      <!-- Notification List -->
      <div class="notification-list" *ngIf="!loading; else loadingTemplate">
        <div class="notification-card" 
             *ngFor="let notification of notifications"
             [class.unread]="!notification.isRead"
             (click)="onNotificationClick(notification)">
          <div class="card-icon" [ngClass]="getIconClass(notification.type)">
            <i [ngClass]="getIcon(notification.type)"></i>
          </div>
          
          <div class="card-content">
            <div class="card-header">
              <span class="card-title">{{ notification.title }}</span>
              <span class="card-time">{{ notification.createdAt | date:'dd/MM/yyyy HH:mm' }}</span>
            </div>
            <div class="card-message">{{ notification.message }}</div>
            <div class="card-meta">
              <span class="badge" [ngClass]="'badge-' + getModuleBadgeClass(notification.module)">
                {{ getModuleLabel(notification.module) }}
              </span>
              <span class="reference" *ngIf="notification.referenceNumber">
                {{ notification.referenceNumber }}
              </span>
            </div>
          </div>
          
          <div class="card-actions">
            <button class="btn-icon" title="Đánh dấu đã đọc" 
                    *ngIf="!notification.isRead"
                    (click)="markAsRead($event, notification.id)">
              <i class="fa-check"></i>
            </button>
            <button class="btn-icon" title="Xóa"
                    (click)="deleteNotification($event, notification.id)">
              <i class="fa-trash"></i>
            </button>
          </div>
        </div>
        
        <!-- Empty State -->
        <div class="empty-state" *ngIf="notifications.length === 0">
          <i class="fa-bell-slash"></i>
          <h3>Không có thông báo nào</h3>
          <p>Các thông báo của bạn sẽ xuất hiện ở đây.</p>
        </div>
      </div>
      
      <ng-template #loadingTemplate>
        <div class="loading-state">
          <i class="fa-spinner fa-spin"></i>
          <span>Đang tải thông báo...</span>
        </div>
      </ng-template>
      
      <!-- Pagination -->
      <div class="pagination" *ngIf="totalPages > 1">
        <button (click)="onPageChange(currentPage - 1)" [disabled]="currentPage === 0">
          « Trước
        </button>
        <span class="page-info">
          Trang {{ currentPage + 1 }} / {{ totalPages }}
        </span>
        <button (click)="onPageChange(currentPage + 1)" [disabled]="currentPage >= totalPages - 1">
          Sau »
        </button>
      </div>
    </div>
  `
})
export class NotificationListComponent implements OnInit {
  notifications: Notification[] = [];
  loading = false;
  currentPage = 0;
  pageSize = 20;
  totalPages = 0;
  
  selectedModule: NotificationModule | null = null;
  selectedStatus: boolean | null = null;
  startDate: string | null = null;
  endDate: string | null = null;
  
  modules = [
    { value: NotificationModule.STOCK_ADJUSTMENT, label: 'Điều chỉnh tồn kho' },
    { value: NotificationModule.STOCK_TRANSFER, label: 'Chuyển vị trí kho' },
    { value: NotificationModule.INBOUND_RECEIPT, label: 'Nhập kho' },
    { value: NotificationModule.OUTBOUND_ORDER, label: 'Đơn hàng' },
    { value: NotificationModule.OUTBOUND_SHIPMENT, label: 'Giao hàng' },
    { value: NotificationModule.REPORT, label: 'Báo cáo' },
    { value: NotificationModule.SYSTEM, label: 'Hệ thống' }
  ];

  constructor(
    private notificationService: NotificationService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadNotifications();
  }

  get hasUnread(): boolean {
    return this.notifications.some(n => !n.isRead);
  }

  loadNotifications(): void {
    this.loading = true;
    
    this.notificationService.getNotifications({
      page: this.currentPage,
      size: this.pageSize,
      isRead: this.selectedStatus ?? undefined,
      module: this.selectedModule ?? undefined,
      startDate: this.startDate ?? undefined,
      endDate: this.endDate ?? undefined
    }).subscribe({
      next: (response) => {
        this.notifications = response.data.content;
        this.totalPages = response.data.total_pages;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  onFilterChange(): void {
    this.currentPage = 0;
    this.loadNotifications();
  }

  resetFilters(): void {
    this.selectedModule = null;
    this.selectedStatus = null;
    this.startDate = null;
    this.endDate = null;
    this.onFilterChange();
  }

  onPageChange(page: number): void {
    if (page < 0 || page >= this.totalPages) return;
    this.currentPage = page;
    this.loadNotifications();
  }

  onNotificationClick(notification: Notification): void {
    if (!notification.isRead) {
      this.markAsRead(new Event('click'), notification.id);
    }
    
    if (notification.referenceUrl) {
      this.router.navigateByUrl(notification.referenceUrl);
    }
  }

  markAsRead(event: Event, id: string): void {
    event.stopPropagation();
    this.notificationService.markAsRead(id).subscribe(() => {
      const notification = this.notifications.find(n => n.id === id);
      if (notification) {
        notification.isRead = true;
      }
    });
  }

  markAllAsRead(): void {
    this.notificationService.markAllAsRead().subscribe(() => {
      this.notifications.forEach(n => n.isRead = true);
    });
  }

  deleteNotification(event: Event, id: string): void {
    event.stopPropagation();
    if (confirm('Bạn có chắc muốn xóa thông báo này?')) {
      this.notificationService.delete(id).subscribe(() => {
        this.notifications = this.notifications.filter(n => n.id !== id);
      });
    }
  }

  getIcon(type: NotificationType): string {
    // ... (same as NotificationBellComponent)
  }

  getIconClass(type: NotificationType): string {
    // ... (same as NotificationBellComponent)
  }

  getModuleLabel(module: NotificationModule): string {
    const found = this.modules.find(m => m.value === module);
    return found?.label || module;
  }

  getModuleBadgeClass(module: NotificationModule): string {
    const classes: Record<NotificationModule, string> = {
      [NotificationModule.STOCK_ADJUSTMENT]: 'primary',
      [NotificationModule.STOCK_TRANSFER]: 'info',
      [NotificationModule.INBOUND_RECEIPT]: 'success',
      [NotificationModule.OUTBOUND_ORDER]: 'warning',
      [NotificationModule.OUTBOUND_SHIPMENT]: 'warning',
      [NotificationModule.REPORT]: 'secondary',
      [NotificationModule.SYSTEM]: 'dark'
    };
    return classes[module] || 'secondary';
  }
}
```

---

## 8. SCALING VÀ PERFORMANCE

### 8.1 Caching Strategy

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         CACHING STRATEGY                                   │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────┬──────────────────────────────────────────┐
│ Data                            │ Cache Strategy                           │
├─────────────────────────────────┼──────────────────────────────────────────┤
│ User online status              │ Redis: ws:online:{userId} (TTL: 5min)  │
│ Unread count per user           │ Redis: notif:unread:{userId} (invalidate)│
│ Recent notifications            │ Redis List: notif:recent:{userId} (100) │
├─────────────────────────────────┼──────────────────────────────────────────┤
│ Notification metadata           │ MySQL with indexing                      │
│ Notification body               │ MySQL (TEXT/JSON)                        │
└─────────────────────────────────┴──────────────────────────────────────────┘
```

### 8.2 Redis Keys

```
# User online status (TTL: 5 minutes, refreshed by heartbeat)
ws:online:{userId} = {sessionId}

# User sessions
ws:user:sessions:{userId} = SET[{sessionId1}, {sessionId2}, ...]

# Session data (TTL: 1 hour)
ws:session:{sessionId} = HASH{
    userId: string,
    deviceInfo: string,
    connectedAt: timestamp
}

# Unread count cache (TTL: 5 minutes)
notif:unread:{userId} = number
```

### 8.3 Rate Limiting

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         RATE LIMITING (Bucket4j + Redis)                     │
└─────────────────────────────────────────────────────────────────────────────┘

Per User Limits:
┌──────────────────────────────────┬────────────────┬──────────────────────────┐
│ Endpoint                         │ Limit          │ Window                   │
├──────────────────────────────────┼────────────────┼──────────────────────────┤
│ GET /notifications               │ 100            │ 1 minute                 │
│ PUT /notifications/{id}/read     │ 60             │ 1 minute                 │
│ PUT /notifications/read-all      │ 10             │ 1 minute                 │
│ DELETE /notifications/{id}       │ 30             │ 1 minute                 │
├──────────────────────────────────┼────────────────┼──────────────────────────┤
│ WebSocket messages sent          │ 100            │ 1 minute                 │
│ WebSocket connect attempts       │ 10             │ 1 minute                 │
└──────────────────────────────────┴────────────────┴──────────────────────────┘

Global Limits:
┌──────────────────────────────────┬────────────────┬──────────────────────────┐
│ Metric                           │ Limit          │ Window                   │
├──────────────────────────────────┼────────────────┼──────────────────────────┤
│ WebSocket messages               │ 10,000         │ 1 second                 │
│ Notification creation            │ 1,000          │ 1 second                 │
│ Database writes                  │ 5,000          │ 1 second                 │
└──────────────────────────────────┴────────────────┴──────────────────────────┘
```

### 8.4 Database Optimization

```sql
-- Index cho notification queries phổ biến
CREATE INDEX idx_notifications_user_unread_created 
ON notifications(user_id, is_read, created_at DESC);

-- Composite index cho filter queries
CREATE INDEX idx_notifications_module_created 
ON notifications(module, created_at DESC);

-- Partitioning theo created_at (nếu > 1M rows/year)
ALTER TABLE notifications 
PARTITION BY RANGE (TO_DAYS(created_at)) (
    PARTITION p_2024 VALUES LESS THAN (TO_DAYS('2025-01-01')),
    PARTITION p_2025 VALUES LESS THAN (TO_DAYS('2026-01-01')),
    PARTITION p_2026 VALUES LESS THAN (TO_DAYS('2027-01-01')),
    PARTITION p_future VALUES LESS THAN MAXVALUE
);

-- Auto-cleanup: Xóa notification > 90 ngày (configurable)
-- Chạy via scheduled job mỗi ngày lúc 2AM
DELETE FROM notifications 
WHERE created_at < DATE_SUB(NOW(), INTERVAL 90 DAY) 
AND is_read = TRUE;
```

### 8.5 Performance Monitoring

```java
// Actuator metrics for monitoring
@Bean
public MeterRegistry meterRegistry() {
    return new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
}

// Custom metrics
@Bean
public NotificationMetrics notificationMetrics(MeterRegistry registry) {
    return new NotificationMetrics(registry);
}

// Metrics:
// - notifications_created_total (counter)
// - notifications_sent_realtime_total (counter)
// - notifications_sent_offline_total (counter)
// - notification_delivery_latency_seconds (histogram)
// - websocket_connections_active (gauge)
// - websocket_reconnect_attempts_total (counter)
```

---

## 9. BẢO MẬT

### 9.1 Authorization Rules

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         AUTHORIZATION RULES                                 │
└─────────────────────────────────────────────────────────────────────────────┘

1. User chỉ đọc được notification của chính mình
2. Admin/Manager nhận broadcast notification (adjustments/transfer pending)
3. Notification của other users không visible
4. Mark as read/delete chỉ áp dụng cho notification của user hiện tại
5. Deep links kiểm tra quyền truy cập resource trước khi redirect
```

### 9.2 WebSocket Security

```java
@Component
@RequiredArgsConstructor
public class AuthHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
    ) throws Exception {
        
        // Extract token from query parameter or header
        String token = extractToken(request);
        
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            return false; // Reject connection
        }
        
        // Get user from token
        String userId = jwtTokenProvider.getUserIdFromToken(token);
        User user = userRepository.findById(userId).orElse(null);
        
        if (user == null) {
            return false;
        }
        
        // Set user in session attributes
        attributes.put("userId", userId);
        attributes.put("username", user.getUsername());
        attributes.put("roles", user.getRoles());
        
        return true;
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception
    ) {
        // Optional: log handshake completion
    }

    private String extractToken(ServerHttpRequest request) {
        // Try query parameter first: /ws?token=xxx
        String query = request.getURI().getQuery();
        if (query != null && query.contains("token=")) {
            return Arrays.stream(query.split("&"))
                .filter(p -> p.startsWith("token="))
                .map(p -> p.substring(6))
                .findFirst()
                .orElse(null);
        }
        
        // Try Authorization header
        List<String> authHeaders = request.getHeaders().get("Authorization");
        if (authHeaders != null && !authHeaders.isEmpty()) {
            String auth = authHeaders.get(0);
            if (auth.startsWith("Bearer ")) {
                return auth.substring(7);
            }
        }
        
        return null;
    }
}
```

### 9.3 Channel Interceptor

```java
@Component
@RequiredArgsConstructor
public class WebSocketChannelInterceptor implements ChannelInterceptor {

    private final NotificationService notificationService;

    @Override
    public Message<?> preSend(Message<?> message, ChannelInterceptor.Chain chain) {
        StompHeaderAccessor accessor = MessageHeaderAccessor
            .getAccessor(message, StompHeaderAccessor.class);
        
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            // Validate session
            String sessionId = accessor.getSessionId();
            Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
            
            if (sessionAttributes == null || !sessionAttributes.containsKey("userId")) {
                throw new IllegalStateException("Unauthorized WebSocket connection");
            }
            
            String userId = (String) sessionAttributes.get("userId");
            
            // Register session with WebSocketSessionManager
            String deviceInfo = accessor.getFirstNativeHeader("X-Device-Info");
            webSocketSessionManager.handleConnected(sessionId, userId, deviceInfo);
        }
        
        if (accessor != null && StompCommand.DISCONNECT.equals(accessor.getCommand())) {
            String sessionId = accessor.getSessionId();
            webSocketSessionManager.handleDisconnected(sessionId);
        }
        
        if (accessor != null && StompCommand.SEND.equals(accessor.getCommand())) {
            // Validate user can send to this destination
            String destination = accessor.getDestination();
            
            // User can only send to /app/* destinations
            if (destination != null && !destination.startsWith("/app/")) {
                throw new IllegalStateException("Invalid destination");
            }
        }
        
        return chain.preSend(message, accessor);
    }
}
```

---

## 10. TESTING STRATEGY

### 10.1 Unit Tests

```java
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {
    
    @Mock
    private NotificationRepository notificationRepository;
    
    @Mock
    private SimpMessagingTemplate messagingTemplate;
    
    @Mock
    private RedisTemplate<String, String> redisTemplate;
    
    @InjectMocks
    private NotificationService notificationService;
    
    @Test
    void should_SaveAndSend_When_UserOnline() {
        // Given
        String userId = "user1";
        NotificationRequest request = createTestRequest(userId);
        
        when(redisTemplate.hasKey("ws:online:" + userId)).thenReturn(true);
        when(notificationRepository.save(any())).thenAnswer(i -> {
            Notification n = i.getArgument(0);
            n.setId(UUID.randomUUID().toString());
            return n;
        });
        
        // When
        NotificationResponse response = notificationService.createAndSend(request);
        
        // Then
        assertNotNull(response);
        assertEquals(NotificationType.ADJUSTMENT_PENDING, response.getType());
        verify(notificationRepository).save(any());
        verify(messagingTemplate).convertAndSendToUser(eq(userId), eq("/queue/notifications"), any());
    }
    
    @Test
    void should_OnlySave_When_UserOffline() {
        // Given
        String userId = "user1";
        NotificationRequest request = createTestRequest(userId);
        
        when(redisTemplate.hasKey("ws:online:" + userId)).thenReturn(false);
        when(notificationRepository.save(any())).thenAnswer(i -> {
            Notification n = i.getArgument(0);
            n.setId(UUID.randomUUID().toString());
            return n;
        });
        
        // When
        NotificationResponse response = notificationService.createAndSend(request);
        
        // Then
        assertNotNull(response);
        verify(notificationRepository).save(any());
        verify(messagingTemplate, never()).convertAndSendToUser(any(), any(), any());
    }
    
    @Test
    void should_IncrementUnreadCount_When_NewNotification() {
        // Given
        String userId = "user1";
        NotificationRequest request = createTestRequest(userId);
        
        when(redisTemplate.hasKey("ws:online:" + userId)).thenReturn(false);
        when(redisTemplate.hasKey("notif:unread:" + userId)).thenReturn(true);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("notif:unread:" + userId)).thenReturn(1L);
        when(notificationRepository.save(any())).thenAnswer(i -> {
            Notification n = i.getArgument(0);
            n.setId(UUID.randomUUID().toString());
            return n;
        });
        
        // When
        notificationService.createAndSend(request);
        
        // Then
        verify(valueOperations).increment("notif:unread:" + userId);
    }
}
```

### 10.2 Integration Tests

```java
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class NotificationIntegrationTest {
    
    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("test")
        .withUsername("test")
        .withPassword("test");
    
    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
        .withExposedPorts(6379);
    
    @Autowired
    private WebSocketStompClient stompClient;
    
    @Autowired
    private StockAdjustmentService stockAdjustmentService;
    
    private StompSession session;
    
    @BeforeEach
    void setup() {
        // Connect WebSocket
        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        headers.add("Authorization", "Bearer " + getTestToken());
        
        session = stompClient.connect(
            "ws://localhost:8080/ws",
            headers,
            new NoOpStompSessionHandler()
        );
    }
    
    @Test
    void should_DeliverNotification_When_WebSocketConnected() throws Exception {
        // Setup handler
        List<Notification> receivedNotifications = new CopyOnWriteArrayList<>();
        session.subscribe("/user/queue/notifications", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return NotificationWebSocketMessage.class;
            }
            
            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                receivedNotifications.add(((NotificationWebSocketMessage) payload).toNotification());
            }
        });
        
        // Trigger domain event
        CreateAdjustmentRequest request = createTestAdjustmentRequest();
        stockAdjustmentService.createAdjustment(request);
        
        // Wait for notification (max 5 seconds)
        await()
            .atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> assertFalse(receivedNotifications.isEmpty()));
        
        // Verify
        Notification notification = receivedNotifications.get(0);
        assertEquals(NotificationType.ADJUSTMENT_PENDING, notification.getType());
    }
}
```

### 10.3 Load Tests

```yaml
# k6-load-test.js
import http from 'k6/http';
import ws from 'k6/ws';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '30s', target: 10 },   // Ramp up
    { duration: '1m', target: 50 },    // Steady
    { duration: '30s', target: 100 },  // Stress
    { duration: '1m', target: 0 },      // Ramp down
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'],
    ws_connect: ['p(95)<100'],
    notification_delivery: ['p(95)<1000'], // 1 second
  },
};

export default function () {
  const url = 'ws://localhost:8080/ws?token=' + __ENV.TEST_TOKEN;
  
  ws.connect(url, {}, function (socket) {
    socket.on('open', () => {
      console.log('WebSocket connected');
      
      // Subscribe to notifications
      socket.send(JSON.stringify({
        command: 'SUBSCRIBE',
        id: 'sub-0',
        destination: '/user/queue/notifications'
      }));
    });
    
    socket.on('message', (data) => {
      const message = JSON.parse(data);
      if (message.type === 'NOTIFICATION') {
        // Verify delivery time
        const now = Date.now();
        const sentAt = message.createdAt;
        console.log(`Notification received in ${now - sentAt}ms`);
      }
    });
    
    socket.on('error', (e) => {
      console.log('WebSocket error:', e);
    });
    
    socket.on('close', () => {
      console.log('WebSocket closed');
    });
    
    // Keep connection alive
    const end = new Date().getTime() + 30000;
    while (new Date().getTime() < end) {
      socket.send(JSON.stringify({
        command: 'PING'
      }));
      sleep(5);
    }
    
    socket.close();
  });
}
```

---

## 11. DEPLOYMENT CHECKLIST

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         DEPLOYMENT CHECKLIST                                │
└─────────────────────────────────────────────────────────────────────────────┘

□ Backend:
  □ Maven build: mvn clean package -DskipTests
  □ Docker image build
  □ Environment variables:
    - SPRING_PROFILES_ACTIVE=prod
    - REDIS_HOST=<redis-host>
    - REDIS_PORT=6379
    - RABBITMQ_HOST=<rabbitmq-host>
    - RABBITMQ_PORT=5672
  □ Database migration:
    - flyway migrate
    - Verify V{version}__create_notifications_table.sql executed
  □ Health check endpoint: /actuator/health
  □ WebSocket endpoint: /ws
  □ WebSocket CORS: configure allowed origins

□ Frontend:
  □ Angular build: npm run build -- --configuration production
  □ Environment:
    - API_URL=https://api.yourdomain.com/api
    - WS_URL=wss://api.yourdomain.com/ws
  □ SockJS client library: @stomp/ng2-stompjs
  □ Notification Bell component integration
  □ Auth guard for notification pages

□ Infrastructure:
  □ Redis available và accessible
  □ Redis AUTH configured (if required)
  □ RabbitMQ available (for async email if needed)
  □ MySQL/PostgreSQL migrations run successfully
  □ Load balancer: Sticky session config for WebSocket
  □ SSL/TLS certificates for WSS

□ Monitoring:
  □ WebSocket connection metrics
  □ Notification delivery success rate
  □ Redis memory usage
  □ Database query performance
  □ Alerting rules configured

□ Testing:
  □ Unit tests pass: mvn test
  □ Integration tests pass
  □ Load tests pass (optional)
  □ Cross-browser tests (Chrome, Firefox, Safari)
```

---

## 12. ESTIMATE EFFORT

```
┌─────────────────────────────────────────────────┬─────────┬────────────────┐
│ Task                                            │ Effort  │ Priority       │
├─────────────────────────────────────────────────┼─────────┼────────────────┤
│ 1. Database Migration (notifications table)     │ 2h      │ P0 (Blocker)   │
│ 2. Redis Configuration & Session Store          │ 4h      │ P0 (Blocker)   │
│ 3. WebSocket Config (BE)                        │ 4h      │ P0 (Blocker)   │
│ 4. WebSocketSessionManager                      │ 4h      │ P0 (Blocker)   │
│ 5. NotificationService (BE)                    │ 8h      │ P0 (Blocker)   │
│ 6. NotificationController (REST API)           │ 4h      │ P0 (Blocker)   │
│ 7. Domain Event Listeners (BE)                   │ 8h      │ P0             │
│ 8. Email ProducerService (reuse existing)        │ 2h      │ P1             │
│ 9. Unit Tests (BE)                             │ 8h      │ P1             │
├─────────────────────────────────────────────────┼─────────┼────────────────┤
│ 10. FE NotificationService (WebSocket client) │ 8h      │ P0 (Blocker)   │
│ 11. FE NotificationBell Component              │ 6h      │ P1             │
│ 12. FE Toast Integration                       │ 4h      │ P1             │
│ 13. FE Notification History Page               │ 8h      │ P2             │
│ 14. FE Unit Tests                              │ 4h      │ P1             │
├─────────────────────────────────────────────────┼─────────┼────────────────┤
│ 15. Integration Tests                           │ 8h      │ P1             │
│ 16. Load Tests (optional)                       │ 4h      │ P3             │
│ 17. Documentation                              │ 4h      │ P2             │
├─────────────────────────────────────────────────┼─────────┼────────────────┤
│ TOTAL                                          │ ~90h    │ 12-14 days     │
└─────────────────────────────────────────────────┴─────────┴────────────────┘
```

---

## 13. SUMMARY

| Aspect | Decision |
|--------|----------|
| **Persistence** | ✅ Save to DB always (Facebook model) |
| **Real-time** | ✅ WebSocket/STOMP for online users |
| **Offline Catch-up** | ✅ Query unread on connect |
| **Scalability** | ✅ Redis Pub/Sub + Sticky Session |
| **Rate Limiting** | ✅ Bucket4j + Redis |
| **Multi-tab** | ✅ Single STOMP connection per user |
| **Reconnection** | ✅ Exponential backoff strategy |
| **Security** | ✅ JWT validation + authorization |
| **Caching** | ✅ Redis for unread counts |
| **Cleanup** | ✅ Scheduled job for old notifications |

---

## 14. REVISIONS

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 21/03/2026 | AI Assistant | Initial version |

---

**Document Status:** Ready for Implementation  
**Next Steps:** 
1. Review and approve this BRD
2. Create Jira/Issue tickets
3. Begin Phase 1: Infrastructure
