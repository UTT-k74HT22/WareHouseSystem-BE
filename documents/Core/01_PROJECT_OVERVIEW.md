# Warehouse Management System (WMS) - Version 1
## Tổng Quan Dự Án

---

## 📋 Mục Lục
1. [Giới Thiệu](#giới-thiệu)
2. [Mục Tiêu Dự Án](#mục-tiêu-dự-án)
3. [Phạm Vi Version 1](#phạm-vi-version-1)
4. [Công Nghệ Sử Dụng](#công-nghệ-sử-dụng)
5. [Kiến Trúc Tổng Quan](#kiến-trúc-tổng-quan)
6. [Các Vai Trò](#các-vai-trò)
7. [Quy Trình Nghiệp Vụ Chính](#quy-trình-nghiệp-vụ-chính)

---

## 🎯 Giới Thiệu

**Warehouse Management System (WMS)** là hệ thống quản lý kho hàng được thiết kế để hỗ trợ và tối ưu hóa các hoạt động kho bãi. Hệ thống quản lý toàn bộ luồng hàng hóa từ khi nhập kho (inbound) cho đến khi xuất kho (outbound), đảm bảo tính chính xác và truy xuất nguồn gốc đầy đủ.

### Đặc Điểm Chính
- ✅ Quản lý đa kho (multi-warehouse)
- ✅ Theo dõi tồn kho theo thời gian thực
- ✅ Quản lý lô hàng (batch tracking)
- ✅ Kiểm soát vị trí lưu trữ (location management)
- ✅ Kiểm toán đầy đủ (full audit trail)
- ✅ Báo cáo và xuất dữ liệu (reporting & export)

---

## 🎯 Mục Tiêu Dự Án

### Mục Tiêu Nghiệp Vụ
1. **Tăng độ chính xác tồn kho** lên 99%+
2. **Giảm thời gian xử lý** nhập/xuất kho 40%
3. **Cải thiện khả năng truy xuất** nguồn gốc hàng hóa
4. **Tối ưu hóa không gian kho** thông qua quản lý vị trí
5. **Tự động hóa** quy trình nhập/xuất kho

### Mục Tiêu Kỹ Thuật
1. **Hiệu năng cao**: Xử lý 1000+ giao dịch/phút
2. **Độ tin cậy**: Uptime 99.9%
3. **Bảo mật**: Tuân thủ chuẩn RBAC, JWT authentication
4. **Khả năng mở rộng**: Kiến trúc module hóa, dễ mở rộng
5. **Tích hợp**: API RESTful chuẩn, dễ tích hợp với hệ thống khác

---

## 📦 Phạm Vi Version 1

### ✅ Trong Phạm Vi (In Scope)

#### Chức Năng Nghiệp Vụ
- ✅ **Quản lý dữ liệu cơ bản**: Kho, vị trí, sản phẩm, đối tác
- ✅ **Quy trình nhập kho**: Đơn mua hàng → Nhập kho → Tăng tồn kho
- ✅ **Quy trình xuất kho**: Đơn bán hàng → Xuất kho → Giảm tồn kho
- ✅ **Quản lý tồn kho**: Theo dõi thời gian thực, điều chỉnh tồn kho
- ✅ **Quản lý lô hàng**: Số lô, ngày sản xuất, hạn sử dụng (tùy chọn)
- ✅ **Kiểm toán**: Lịch sử biến động tồn kho đầy đủ
- ✅ **Báo cáo**: Tồn kho, lịch sử giao dịch, xuất PDF/Excel
- ✅ **Import dữ liệu**: Nhập sản phẩm và tồn kho ban đầu từ Excel
- ✅ **Thông báo real-time**: WebSocket notifications
- ✅ **Xác thực & phân quyền**: RBAC, JWT, rate limiting

#### Chức Năng Kỹ Thuật
- ✅ RESTful API với Spring Boot
- ✅ Database MySQL với Flyway migration
- ✅ Redis cache cho hiệu năng
- ✅ RabbitMQ cho xử lý bất đồng bộ
- ✅ WebSocket cho thông báo real-time
- ✅ OpenAPI/Swagger documentation
- ✅ Unit tests & Integration tests
- ✅ Docker containerization

### ❌ Ngoài Phạm Vi (Out of Scope)

- ❌ Logic FIFO/FEFO phức tạp (chỉ tracking, không enforce)
- ❌ Phân cấp vị trí phức tạp (aisle/rack/bin)
- ❌ Multi-tenancy (chỉ hỗ trợ 1 công ty)
- ❌ Tối ưu hóa kho nâng cao (wave picking, task management)
- ❌ Tích hợp ERP/E-commerce bên ngoài
- ❌ Mobile app (chỉ Web UI)
- ❌ Analytics & Dashboard nâng cao
- ❌ Quản lý chuyển kho (warehouse transfer)

---

## 💻 Công Nghệ Sử Dụng

### Backend Stack
| Công Nghệ | Version | Mục Đích |
|-----------|---------|----------|
| **Java** | 17 | Ngôn ngữ lập trình chính |
| **Spring Boot** | 3.5.9 | Framework backend |
| **Spring Security** | 3.5.9 | Bảo mật & xác thực |
| **Spring Data JPA** | 3.5.9 | ORM & database access |
| **MySQL** | 8.0 | Cơ sở dữ liệu chính |
| **Redis** | 7.0 | Cache & session management |
| **RabbitMQ** | 3.x | Message broker |
| **Flyway** | Latest | Database migration |
| **JWT (jjwt)** | 0.12.x | Token authentication |
| **Lombok** | Latest | Giảm boilerplate code |
| **MapStruct** | Latest | DTO mapping |
| **SpringDoc OpenAPI** | 2.x | API documentation |

### Testing & Quality
| Công Nghệ | Mục Đích |
|-----------|----------|
| **JUnit 5** | Unit testing |
| **Mockito** | Mocking framework |
| **Spring Boot Test** | Integration testing |
| **H2 Database** | In-memory testing database |
| **AssertJ** | Fluent assertions |

### DevOps & Tools
| Công Nghệ | Mục Đích |
|-----------|----------|
| **Maven** | Build tool |
| **Docker** | Containerization |
| **Docker Compose** | Multi-container orchestration |
| **Git** | Version control |

---

## 🏗️ Kiến Trúc Tổng Quan

### Kiến Trúc Layers

```
┌─────────────────────────────────────────────────────────┐
│                    PRESENTATION LAYER                    │
│  REST Controllers │ WebSocket │ Exception Handlers      │
└─────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────┐
│                     SERVICE LAYER                        │
│  Business Logic │ Transaction Management │ Validation   │
└─────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────┐
│                  PERSISTENCE LAYER                       │
│  JPA Repositories │ Entity Models │ Database Access     │
└─────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────┐
│                   INFRASTRUCTURE                         │
│  MySQL │ Redis │ RabbitMQ │ File Storage               │
└─────────────────────────────────────────────────────────┘
```

### Kiến Trúc Module

```
┌──────────────┐
│ Auth & RBAC  │ (Foundation)
└──────────────┘
        ↓
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│ Master Data  │────→│    Batch     │────→│  Inventory   │
└──────────────┘     └──────────────┘     └──────────────┘
        ↓                    ↓                     ↓
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│   Inbound    │────→│  Outbound    │────→│Stock Movement│
└──────────────┘     └──────────────┘     └──────────────┘
        ↓                    ↓                     ↓
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│  Reporting   │     │Excel Import  │     │Notifications │
└──────────────┘     └──────────────┘     └──────────────┘
```

---

## 👥 Các Vai Trò

### 1. Admin (Quản Trị Viên Hệ Thống)
**Trách nhiệm:**
- Quản lý người dùng, vai trò, quyền hạn
- Cấu hình hệ thống
- Giám sát hoạt động hệ thống
- Xử lý sự cố

**Quyền hạn:**
- ✅ Toàn quyền truy cập mọi chức năng
- ✅ Quản lý RBAC
- ✅ Xem audit logs

### 2. Warehouse Manager (Quản Lý Kho)
**Trách nhiệm:**
- Giám sát toàn bộ hoạt động kho
- Phê duyệt điều chỉnh tồn kho
- Quản lý nhân sự kho
- Xem và phân tích báo cáo

**Quyền hạn:**
- ✅ Xem tất cả giao dịch
- ✅ Phê duyệt điều chỉnh tồn kho
- ✅ Tạo và xem báo cáo
- ✅ Quản lý master data

### 3. Warehouse Staff (Nhân Viên Kho)
**Trách nhiệm:**
- Xử lý nhập kho
- Xử lý xuất kho
- Kiểm kê tồn kho
- Cập nhật vị trí hàng hóa

**Quyền hạn:**
- ✅ Tạo/cập nhật phiếu nhập/xuất
- ✅ Xem tồn kho
- ✅ Điều chỉnh tồn kho (có giới hạn)
- ❌ Không xem báo cáo tài chính

### 4. Viewer/Auditor (Người Xem/Kiểm Toán)
**Trách nhiệm:**
- Xem dữ liệu cho mục đích kiểm toán
- Theo dõi tuân thủ
- Báo cáo vi phạm

**Quyền hạn:**
- ✅ Chỉ đọc tất cả dữ liệu
- ✅ Xem audit trail
- ✅ Xuất báo cáo
- ❌ Không chỉnh sửa

---

## 🔄 Quy Trình Nghiệp Vụ Chính

### 1. Quy Trình Nhập Kho (Inbound Flow)

```
┌─────────────────┐
│ Tạo Đơn Mua     │ (DRAFT)
│ Purchase Order  │
└────────┬────────┘
         │
         ↓
┌─────────────────┐
│ Xác Nhận Đơn    │ (CONFIRMED)
│ Confirm PO      │
└────────┬────────┘
         │
         ↓
┌─────────────────┐
│ Tạo Phiếu Nhập  │ (DRAFT)
│ Goods Receipt   │
└────────┬────────┘
         │
         ↓
┌─────────────────┐
│ Xác Nhận Nhập   │ (CONFIRMED)
│ Confirm Receipt │
└────────┬────────┘
         │
         ↓
┌─────────────────┐
│ ✅ Tăng Tồn Kho │
│ Stock Increased │
│ + Audit Log     │
└─────────────────┘
```

### 2. Quy Trình Xuất Kho (Outbound Flow)

```
┌─────────────────┐
│ Tạo Đơn Bán     │ (DRAFT)
│ Sales Order     │
└────────┬────────┘
         │
         ↓
┌─────────────────┐
│ Xác Nh��n Đơn    │ (CONFIRMED)
│ Confirm SO      │ → Reserve Stock
└────────┬────────┘
         │
         ↓
┌─────────────────┐
│ Tạo Phiếu Xuất  │ (DRAFT)
│ Create Shipment │
└────��───┬────────┘
         │
         ↓
┌─────────────────┐
│ Lấy Hàng        │ (PICKING)
│ Pick Items      │
└────────┬────────┘
         │
         ↓
┌─────────────────┐
│ Xác Nhận Xuất   │ (SHIPPED)
│ Confirm Shipment│
└────────┬────────┘
         │
         ↓
┌─────────────────┐
│ ✅ Giảm Tồn Kho │
│ Stock Decreased │
│ + Unreserve     │
│ + Audit Log     │
└─────────────────┘
```

### 3. Quy Trình Kiểm Toán (Audit Flow)

```
┌─────────────────┐
│ Mọi Thay Đổi    │
│ Any Stock Change│
└────────┬────────┘
         │
         ↓
┌─────────────────┐
│ Ghi Log Biến    │
│ Động Tự Động    │
│ Stock Movement  │
│ + Who           │
│ + When          │
│ + What          │
│ + Why           │
│ + Reference     │
└────────┬────────┘
         │
         ↓
┌─────────────────┐
│ ✅ Audit Trail  │
│ (Immutable)     │
└────────���────────┘
```

---

## 📊 Chỉ Số Đánh Giá (KPIs)

### Chỉ Số Nghiệp Vụ
- **Inventory Accuracy**: ≥ 99%
- **Order Processing Time**: < 5 phút/đơn
- **Stock-out Rate**: < 2%
- **Space Utilization**: ≥ 85%

### Chỉ Số Kỹ Thuật
- **API Response Time**: < 200ms (95th percentile)
- **System Uptime**: ≥ 99.9%
- **Database Query Time**: < 100ms
- **Cache Hit Rate**: ≥ 80%

---

## 📅 Timeline Version 1

### Phase 1: Foundation (Weeks 1-2)
- ✅ Setup project structure
- ✅ Implement Auth & RBAC
- ✅ Setup CI/CD pipeline

### Phase 2: Core Modules (Weeks 3-6)
- 🔄 Master Data Management
- 🔄 Inventory Management
- 🔄 Batch Management

### Phase 3: Business Flows (Weeks 7-10)
- ⏳ Inbound Flow
- ⏳ Outbound Flow
- ⏳ Stock Movement Audit

### Phase 4: Advanced Features (Weeks 11-12)
- ⏳ Reporting & Export
- ⏳ Excel Import
- ⏳ Real-time Notifications

### Phase 5: Testing & Deployment (Weeks 13-14)
- ⏳ Integration Testing
- ⏳ Performance Testing
- ⏳ UAT & Production Deployment

---

## 📞 Liên Hệ & Hỗ Trợ

**Project Manager:** [Tên PM]  
**Tech Lead:** [Tên Tech Lead]  
**Email:** support@wms.com  
**Documentation:** [Wiki Link]

---

**Cập nhật lần cuối:** 21/01/2026  
**Version:** 1.0  
**Trạng thái:** 🚧 In Progress

