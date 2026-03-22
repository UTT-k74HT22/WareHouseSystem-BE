# Report Module - Tổng Quan & Kế Hoạch Triển Khai

> Cập nhật: 2026-03-22
> Jira Project: WHS

---

## 1. Tổng Quan Module

Module Report cung cấp các chức năng tạo, quản lý và xuất báo cáo cho hệ thống quản lý kho (WMS).

### 1.1 Mục Tiêu

- Cung cấp báo cáo on-demand cho phân tích tồn kho, nhập/xuất kho
- Hỗ trợ xuất báo cáo async cho dữ liệu lớn
- Quản lý lịch báo cáo tự động (scheduled reports)
- Hỗ trợ nhiều định dạng: PDF, Excel, CSV

### 1.2 Scope Jira Tasks

| Task | Summary | Status | Priority | Assignee |
|------|---------|--------|----------|----------|
| **WHS-52** | Reporting API Completion (Parent) | In Progress | High | Đình Dũng Hoàng |
| **WHS-67** | Implement reporting on-demand APIs | To Do | High | Unassigned |
| **WHS-68** | Implement reporting async APIs | To Do | Medium | Unassigned |
| **WHS-69** | Implement reporting schedule management APIs | To Do | Medium | Nguyễn Quốc Toản |

### 1.3 Related Export/Import Tasks

| Task | Summary | Status | Priority |
|------|---------|--------|----------|
| **WHS-37** | Implement location bulk import API | To Do | Medium |
| **WHS-38** | Implement product import API | To Do | High |
| **WHS-39** | Implement product export API | To Do | High |

---

## 2. Kiến Trúc Hệ Thống

### 2.1 Technology Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| Reporting Engine | JasperReports | 6.21.5 |
| Excel Export | Apache POI | Cần thêm |
| Async Processing | RabbitMQ | - |
| File Storage | MinIO | 8.5.17 |
| Cache | Redis | - |

### 2.2 Package Structure

```
src/main/java/org/demo/whs/
├── controller/
│   └── ReportController.java              # REST endpoints
├── service/
│   ├── ReportService.java                 # Interface
│   └── impl/
│       └── ReportServiceImpl.java         # Implementation
├── repository/
│   └── custom/
│       ├── ReportRepositoryCustom.java    # Custom queries
│       └── impl/
│           └── ReportRepositoryCustomImpl.java
├── entity/
│   ├── ReportRequest.java                 # Report request entity
│   ├── ReportSchedule.java                # Schedule entity
│   └── dto/
│       ├── request/
│       │   └── Report/
│       │       ├── CurrentStockReportRequest.java
│       │       ├── StockValuationReportRequest.java
│       │       ├── MovementsReportRequest.java
│       │       ├── BatchTraceabilityReportRequest.java
│       │       ├── LowStockReportRequest.java
│       │       ├── ExpiringBatchesReportRequest.java
│       │       ├── AsyncReportRequest.java
│       │       └── ReportScheduleRequest.java
│       └── response/
│           └── Report/
│               ├── ReportResponse.java
│               ├── AsyncReportStatusResponse.java
│               ├── ReportScheduleResponse.java
│               └── ReportExportResponse.java
├── mapper/
│   └── ReportMapper.java                  # Entity <-> DTO
├── configuration/
│   └── ReportConfig.java                  # Report configuration
└── utils/
    └── ReportUtils.java                   # Utility methods

src/main/resources/
├── templates/
│   └── reports/
│       ├── current_stock.jrxml
│       ├── stock_valuation.jrxml
│       ├── movements.jrxml
│       ├── batch_traceability.jrxml
│       ├── low_stock.jrxml
│       └── expiring_batches.jrxml
└── db/
    └── migration/
        └── V0XX__create_report_tables.sql
```

---

## 3. API Specification

### 3.1 On-Demand Reports (WHS-67)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/reports/current-stock` | Báo cáo tồn kho hiện tại |
| POST | `/api/v1/reports/stock-valuation` | Báo cáo định giá tồn kho |
| POST | `/api/v1/reports/movements` | Báo cáo lịch sử di chuyển |
| POST | `/api/v1/reports/batch-traceability` | Báo cáo truy xuất lô hàng |
| POST | `/api/v1/reports/low-stock` | Báo cáo tồn kho thấp |
| POST | `/api/v1/reports/expiring-batches` | Báo cáo lô hàng sắp hết hạn |

### 3.2 Async Reports (WHS-68)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/reports/async/request` | Tạo yêu cầu báo cáo async |
| GET | `/api/v1/reports/async/{requestId}` | Kiểm tra trạng thái báo cáo |
| GET | `/api/v1/reports/async/{requestId}/download` | Tải báo cáo đã tạo |
| GET | `/api/v1/reports/async/my-requests` | Danh sách yêu cầu của user |

### 3.3 Schedule Management (WHS-69)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/reports/schedules` | Tạo lịch báo cáo |
| GET | `/api/v1/reports/schedules` | Danh sách lịch báo cáo |
| PUT | `/api/v1/reports/schedules/{id}` | Cập nhật lịch báo cáo |
| DELETE | `/api/v1/reports/schedules/{id}` | Xóa lịch báo cáo |
| PUT | `/api/v1/reports/schedules/{id}/enable` | Kích hoạt lịch |
| PUT | `/api/v1/reports/schedules/{id}/disable` | Vô hiệu hóa lịch |

### 3.4 Export/Import APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/products/export` | Export sản phẩm (WHS-39) |
| POST | `/api/v1/products/import` | Import sản phẩm (WHS-38) |
| POST | `/api/v1/locations/bulk` | Import vị trí hàng loạt (WHS-37) |

---

## 4. Chi Tiết Từng Loại Báo Cáo

### 4.1 Current Stock Report (Tồn Kho Hiện Tại)

**Mô tả:** Báo cáo tổng hợp tồn kho hiện tại theo nhiều维度

**Filter Parameters:**
```json
{
  "warehouse_id": "uuid",
  "category_id": "uuid",
  "product_ids": ["uuid1", "uuid2"],
  "location_id": "uuid",
  "include_zero_stock": false,
  "group_by": "PRODUCT|WAREHOUSE|LOCATION|CATEGORY"
}
```

**Output Fields:**
- Product SKU, Name, Category
- Warehouse, Location
- On-hand Quantity, Reserved Quantity, Available Quantity
- Batch Number (if applicable)
- Stock Value (cost_price * quantity)

### 4.2 Stock Valuation Report (Định Giá Tồn Kho)

**Mô tả:** Báo cáo giá trị tồn kho theo phương pháp định giá

**Filter Parameters:**
```json
{
  "warehouse_id": "uuid",
  "valuation_method": "FIFO|LIFO|WEIGHTED_AVERAGE",
  "as_of_date": "2026-03-22",
  "currency": "VND|USD"
}
```

**Output Fields:**
- Product details
- Quantity on hand
- Unit cost (based on valuation method)
- Total value
- Comparison with previous period

### 4.3 Movements Report (Di Chuyển Kho)

**Mô tả:** Báo cáo lịch sử nhập/xuất/chuyển kho

**Filter Parameters:**
```json
{
  "warehouse_id": "uuid",
  "product_id": "uuid",
  "movement_type": "INBOUND|OUTBOUND|TRANSFER|ADJUSTMENT",
  "date_from": "2026-03-01",
  "date_to": "2026-03-22",
  "reference_type": "PURCHASE_ORDER|SALES_ORDER|TRANSFER|ADJUSTMENT"
}
```

**Output Fields:**
- Movement date, type
- Product details
- Quantity before, change, after
- Reference document
- User who performed

### 4.4 Batch Traceability Report (Truy Xuất Lô Hàng)

**Mô tả:** Truy xuất lịch sử di chuyển của một lô hàng

**Filter Parameters:**
```json
{
  "batch_id": "uuid",
  "batch_number": "BATCH-001",
  "product_id": "uuid"
}
```

**Output Fields:**
- Batch details (number, expiry, supplier)
- All movements of this batch
- Current locations
- Associated documents (PO, IR, SO, OS)

### 4.5 Low Stock Report (Tồn Kho Thấp)

**Mô tả:** Báo cáo sản phẩm dưới mức tồn kho tối thiểu

**Filter Parameters:**
```json
{
  "warehouse_id": "uuid",
  "below_reorder_point": true,
  "category_id": "uuid"
}
```

**Output Fields:**
- Product details
- Current stock level
- Min stock level, Reorder point
- Quantity to reorder
- Preferred supplier

### 4.6 Expiring Batches Report (Lô Hàng Sắp Hết Hạn)

**Mô tả:** Báo cáo lô hàng sắp hết hạn sử dụng

**Filter Parameters:**
```json
{
  "warehouse_id": "uuid",
  "days_until_expiry": 30,
  "include_expired": false
}
```

**Output Fields:**
- Batch details
- Product details
- Expiry date, days remaining
- Current quantity
- Location

---

## 5. Database Schema

### 5.1 report_requests

```sql
CREATE TABLE report_requests (
    id CHAR(36) PRIMARY KEY,
    report_type VARCHAR(50) NOT NULL,
    parameters JSON NOT NULL,
    status ENUM('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED') NOT NULL,
    format ENUM('PDF', 'EXCEL', 'CSV') NOT NULL,
    file_url VARCHAR(500),
    file_size BIGINT,
    error_message TEXT,
    requested_by CHAR(36) NOT NULL,
    requested_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    INDEX idx_report_requests_status (status),
    INDEX idx_report_requests_user (requested_by),
    INDEX idx_report_requests_type (report_type)
);
```

### 5.2 report_schedules

```sql
CREATE TABLE report_schedules (
    id CHAR(36) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    report_type VARCHAR(50) NOT NULL,
    parameters JSON NOT NULL,
    format ENUM('PDF', 'EXCEL', 'CSV') NOT NULL,
    cron_expression VARCHAR(50) NOT NULL,
    timezone VARCHAR(50) DEFAULT 'Asia/Ho_Chi_Minh',
    is_enabled BOOLEAN DEFAULT TRUE,
    last_run_at TIMESTAMP,
    next_run_at TIMESTAMP,
    recipients JSON,
    created_by CHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    INDEX idx_report_schedules_enabled (is_enabled),
    INDEX idx_report_schedules_next_run (next_run_at)
);
```

---

## 6. Implementation Checklist

### Phase 1: Foundation (WHS-67)
- [ ] Tạo database tables (report_requests, report_schedules)
- [ ] Tạo DTOs cho request/response
- [ ] Tạo ReportService interface
- [ ] Tạo ReportRepositoryCustom
- [ ] Implement Current Stock Report
- [ ] Implement Stock Valuation Report
- [ ] Implement Movements Report
- [ ] Implement Batch Traceability Report
- [ ] Implement Low Stock Report
- [ ] Implement Expiring Batches Report
- [ ] Tạo JasperReports templates
- [ ] Unit tests cho từng report type

### Phase 2: Async Processing (WHS-68)
- [ ] Tạo AsyncReportRequest handler
- [ ] Integrate RabbitMQ cho async processing
- [ ] Implement status tracking
- [ ] Implement file storage (MinIO)
- [ ] Implement download endpoint
- [ ] Implement my-requests endpoint
- [ ] Unit tests cho async flow

### Phase 3: Schedule Management (WHS-69)
- [ ] Tạo ReportSchedule entity
- [ ] Implement cron expression validation
- [ ] Integrate Spring Scheduler
- [ ] Implement enable/disable endpoints
- [ ] Implement email notification
- [ ] Unit tests cho scheduling

### Phase 4: Export/Import (WHS-37, WHS-38, WHS-39)
- [ ] Implement Product Export API
- [ ] Implement Product Import API
- [ ] Implement Location Bulk Import API
- [ ] Excel template generation
- [ ] Validation và error reporting
- [ ] Unit tests

---

## 7. Dependencies Cần Thêm

```xml
<!-- Apache POI for Excel -->
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi</artifactId>
    <version>5.2.5</version>
</dependency>
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.2.5</version>
</dependency>

<!-- OpenCSV for CSV -->
<dependency>
    <groupId>com.opencsv</groupId>
    <artifactId>opencsv</artifactId>
    <version>5.9</version>
</dependency>
```

---

## 8. Security Considerations

- Tất cả report endpoints yêu cầu authentication
- Phân quyền theo role: VIEW_REPORTS, EXPORT_REPORTS, MANAGE_SCHEDULES
- Audit log cho tất cả export activities
- Rate limiting cho on-demand reports
- File access control cho async reports

---

## 9. Performance Considerations

- Sử dụng database views/materialized views cho complex queries
- Pagination cho large datasets
- Cache report results trong Redis
- Async processing cho reports > 10,000 rows
- Compress files before storing

---

## 10. Testing Strategy

### Unit Tests
- Test từng report type với mock data
- Test filter validation
- Test export format generation

### Integration Tests
- Test database queries
- Test RabbitMQ integration
- Test MinIO file operations

### E2E Tests
- Test full report generation flow
- Test async request -> processing -> download
- Test schedule execution

---

## 11. Documentation References

- JasperReports: https://community.jaspersoft.com/documentation
- Apache POI: https://poi.apache.org/
- Spring Scheduler: https://docs.spring.io/spring-framework/reference/integration/scheduling.html
- RabbitMQ: https://spring.io/guides/gs/messaging-rabbitmq/

---

## 12. Session Notes

### 2026-03-22
- Phân tích Jira tasks liên quan đến module Report
- Xác định scope: WHS-52, WHS-67, WHS-68, WHS-69
- Related tasks: WHS-37, WHS-38, WHS-39
- Tạo tài liệu tổng quan module Report
- Next: Bắt đầu implement Phase 1 (WHS-67)

---

## 13. Next Steps

1. **Immediate** (Tuần này):
   - Review và approve tài liệu này
   - Tạo database migration cho report tables
   - Bắt đầu implement WHS-67 (On-demand APIs)

2. **Short Term** (2 tuần):
   - Hoàn thành WHS-67
   - Bắt đầu WHS-68 (Async APIs)

3. **Medium Term** (1 tháng):
   - Hoàn thành WHS-68
   - Bắt đầu WHS-69 (Schedule Management)

4. **Long Term** (2 tháng):
   - Hoàn thành tất cả report features
   - Implement Export/Import APIs
