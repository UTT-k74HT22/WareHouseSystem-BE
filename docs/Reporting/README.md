# Module Report - Documentation Index

> Cập nhật: 2026-03-22
> Jira: WHS-52, WHS-67, WHS-68, WHS-69

---

## Giới Thiệu

Đây là bộ tài liệu chi tiết về **Module Report** trong hệ thống quản lý kho (WMS). Module này cung cấp các chức năng tạo, quản lý và xuất báo cáo cho toàn bộ hệ thống.

---

## Danh Sách Tài Liệu

| # | File | Mô Tả |
|---|------|--------|
| 01 | [01-end-to-end-flow.md](./01-end-to-end-flow.md) | Flow tổng quan end-to-end của module Report |
| 02 | [02-on-demand-report-flow.md](./02-on-demand-report-flow.md) | Chi tiết flow báo cáo on-demand (WHS-67) |
| 03 | [03-async-report-flow.md](./03-async-report-flow.md) | Chi tiết flow báo cáo async qua RabbitMQ (WHS-68) |
| 04 | [04-schedule-report-flow.md](./04-schedule-report-flow.md) | Chi tiết flow báo cáo theo lịch (WHS-69) |
| 05 | [05-export-import-flow.md](./05-export-import-flow.md) | Chi tiết flow export/import dữ liệu (WHS-37,38,39) |
| 06 | [06-sequence-diagrams.md](./06-sequence-diagrams.md) | Sequence diagrams chi tiết cho từng use case |
| 07 | [07-data-flow-diagrams.md](./07-data-flow-diagrams.md) | Data flow diagrams và data dictionary |
| 08 | [08-integration-points.md](./08-integration-points.md) | Các điểm tích hợp với hệ thống nội bộ và bên ngoài |

---

## Tổng Quan Nhanh

### Các Loại Báo Cáo

| Report | Description | Processing |
|--------|-------------|------------|
| Current Stock | Tồn kho hiện tại | On-demand |
| Stock Valuation | Định giá tồn kho | On-demand |
| Movements | Lịch sử di chuyển | On-demand |
| Batch Traceability | Truy xuất lô hàng | On-demand |
| Low Stock | Tồn kho thấp | On-demand |
| Expiring Batches | Lô sắp hết hạn | On-demand |

### Định Dạng Export

- **PDF** - JasperReports
- **Excel** - Apache POI
- **CSV** - OpenCSV

### Chế Độ Xử Lý

- **On-demand** - Đồng bộ, xử lý ngay
- **Async** - Bất đồng bộ qua RabbitMQ
- **Scheduled** - Tự động theo lịch cron

---

## Jira Tasks

| Task | Summary | Status |
|------|---------|--------|
| WHS-52 | Reporting API Completion | In Progress |
| WHS-67 | On-demand APIs | To Do |
| WHS-68 | Async APIs | To Do |
| WHS-69 | Schedule Management APIs | To Do |
| WHS-37 | Location Bulk Import | To Do |
| WHS-38 | Product Import | To Do |
| WHS-39 | Product Export | To Do |

---

## Đọc Theo Thứ Tự

1. Đọc **01-end-to-end-flow.md** trước để hiểu tổng quan
2. Đọc tiếp **02, 03, 04, 05** theo từng loại flow cần implement
3. Tham khảo **06** để hiểu chi tiết sequence
4. Tham khảo **07** để hiểu data flow
5. Tham khảo **08** khi cần tích hợp với hệ thống khác

---

## Liên Hệ

- **Module Owner:** Đình Dũng Hoàng
- **Jira Project:** WHS
