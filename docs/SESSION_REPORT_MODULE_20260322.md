# Session Note - Report Module Planning - 2026-03-22

## 1. Mục Tiêu

- Phân tích các task Jira liên quan đến module Report
- Tạo tài liệu tổng quan cho module Report
- Cập nhật roadmap và module map
- Chuẩn bị cho việc triển khai module Report

## 2. Kết Quả

### 2.1 Jira Tasks Đã Phân Tích

| Task | Summary | Status | Priority | Assignee |
|------|---------|--------|----------|----------|
| **WHS-52** | Reporting API Completion (Parent) | In Progress | High | Đình Dũng Hoàng |
| **WHS-67** | Implement reporting on-demand APIs | To Do | High | Unassigned |
| **WHS-68** | Implement reporting async APIs | To Do | Medium | Unassigned |
| **WHS-69** | Implement reporting schedule management APIs | To Do | Medium | Nguyễn Quốc Toản |
| **WHS-37** | Implement location bulk import API | To Do | Medium | Unassigned |
| **WHS-38** | Implement product import API | To Do | High | Unassigned |
| **WHS-39** | Implement product export API | To Do | High | Unassigned |

### 2.2 Scope Module Report

**On-Demand Reports (WHS-67):**
- POST /api/v1/reports/current-stock
- POST /api/v1/reports/stock-valuation
- POST /api/v1/reports/movements
- POST /api/v1/reports/batch-traceability
- POST /api/v1/reports/low-stock
- POST /api/v1/reports/expiring-batches

**Async Reports (WHS-68):**
- POST /api/v1/reports/async/request
- GET /api/v1/reports/async/{requestId}
- GET /api/v1/reports/async/{requestId}/download
- GET /api/v1/reports/async/my-requests

**Schedule Management (WHS-69):**
- POST /api/v1/reports/schedules
- GET /api/v1/reports/schedules
- PUT /api/v1/reports/schedules/{id}
- DELETE /api/v1/reports/schedules/{id}
- PUT /api/v1/reports/schedules/{id}/enable
- PUT /api/v1/reports/schedules/{id}/disable

**Export/Import:**
- GET /api/v1/products/export (WHS-39)
- POST /api/v1/products/import (WHS-38)
- POST /api/v1/locations/bulk (WHS-37)

### 2.3 Files Đã Tạo/Cập Nhật

| File | Action | Description |
|------|--------|-------------|
| `docs/09-report-module-overview.md` | Created | Tài liệu tổng quan module Report |
| `docs/07-roadmap.md` | Updated | Cập nhật Sprint 4 với module Report |
| `docs/01-module-map.md` | Updated | Thêm module G (Report) và H (Export/Import) |
| `docs/SESSION_REPORT_MODULE_20260322.md` | Created | Session note này |

## 3. Kiến Trúc Đề Xuất

### Technology Stack
- **Reporting:** JasperReports 6.21.5 (đã có)
- **Excel:** Apache POI (cần thêm)
- **CSV:** OpenCSV (cần thêm)
- **Async:** RabbitMQ (đã có)
- **Storage:** MinIO (đã có)
- **Scheduling:** Spring Scheduler

### Package Structure
```
src/main/java/org/demo/whs/
├── controller/ReportController.java
├── service/ReportService.java
├── service/impl/ReportServiceImpl.java
├── repository/custom/ReportRepositoryCustom.java
├── repository/custom/impl/ReportRepositoryCustomImpl.java
├── entity/ReportRequest.java
├── entity/ReportSchedule.java
├── entity/dto/request/Report/*.java
├── entity/dto/response/Report/*.java
├── mapper/ReportMapper.java
├── configuration/ReportConfig.java
└── utils/ReportUtils.java
```

### Database Tables
- `report_requests`: Lưu trữ yêu cầu báo cáo async
- `report_schedules`: Lưu trữ lịch báo cáo tự động

## 4. Implementation Phases

### Phase 1: Foundation (WHS-67) - Week 7
- Tạo database tables
- Tạo DTOs
- Implement 6 report types
- JasperReports templates
- Unit tests

### Phase 2: Async Processing (WHS-68) - Week 8
- Async request handler
- RabbitMQ integration
- Status tracking
- MinIO file storage
- Download API

### Phase 3: Schedule Management (WHS-69) - Week 8
- ReportSchedule entity
- CRUD endpoints
- Spring Scheduler integration
- Email notification

### Phase 4: Export/Import (WHS-37, WHS-38, WHS-39) - Week 8
- Product Export API
- Product Import API
- Location Bulk Import API

## 5. Dependencies Cần Thêm

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

## 6. Khi Quay Lại Session Sau

1. Mở `docs/SESSION_REPORT_MODULE_20260322.md` (file này)
2. Mở `docs/09-report-module-overview.md` (tài liệu chi tiết)
3. Mở `docs/07-roadmap.md` (kế hoạch Sprint 4)
4. Bắt đầu implement Phase 1:
   - Tạo database migration
   - Tạo DTOs
   - Tạo ReportService interface
   - Implement đầu tiên: Current Stock Report

## 7. Next Steps

1. **Immediate** (Hôm nay):
   - Review tài liệu `09-report-module-overview.md`
   - Đồng ý với kiến trúc đề xuất
   - Thêm dependencies vào pom.xml

2. **Tuần này**:
   - Tạo database migration
   - Tạo DTOs cho report
   - Bắt đầu implement WHS-67

3. **Tuần sau**:
   - Hoàn thành WHS-67
   - Bắt đầu WHS-68, WHS-69

## 8. Notes

- Module Report hiện tại chưa có code nào (chỉ có Jira tasks)
- JasperReports dependency đã có trong pom.xml
- Cần review lại JasperReports version (6.21.5) xem có cần update không
- Cần thiết kế UI cho JasperReports templates
- Cần coordinate với frontend team về response format
