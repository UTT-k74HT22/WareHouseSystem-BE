# Module Report - End-to-End Flow Documentation

> Version: 1.0
> Updated: 2026-03-22
> Jira: WHS-52, WHS-67, WHS-68, WHS-69

---

## 1. Tổng Quan Hệ Thống

### 1.1 Hệ Thống Tham Gia

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                            WMS Backend System                              │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐    ┌──────────┐ │
│  │   Report     │    │  Inventory   │    │   Inbound    │    │ Outbound │ │
│  │   Module     │◄──►│   Module     │    │   Module     │    │  Module  │ │
│  │  (WHS-52)    │    │  (WHS-10-19) │    │  (WHS-43-46) │    │(WHS-47-50)│ │
│  └──────┬───────┘    └──────────────┘    └──────────────┘    └──────────┘ │
│         │                                                                  │
│         ▼                                                                  │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐    ┌──────────┐ │
│  │  Scheduler   │    │   RabbitMQ   │    │    MinIO     │    │  Redis   │ │
│  │  (Spring)    │    │   (Queue)    │    │  (Storage)   │    │ (Cache)  │ │
│  └──────────────┘    └──────────────┘    └──────────────┘    └──────────┘ │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 1.2 Các Loại Báo Cáo

| ID | Report Type | Description | Processing |
|----|-------------|-------------|------------|
| R1 | Current Stock | Tồn kho hiện tại | On-demand |
| R2 | Stock Valuation | Định giá tồn kho | On-demand |
| R3 | Movements | Lịch sử di chuyển | On-demand |
| R4 | Batch Traceability | Truy xuất lô hàng | On-demand |
| R5 | Low Stock | Tồn kho thấp | On-demand |
| R6 | Expiring Batches | Lô sắp hết hạn | On-demand |
| R7 | Async Reports | Báo cáo lớn | Async |
| R8 | Scheduled Reports | Báo cáo tự động | Scheduled |

---

## 2. End-to-End Flow Tổng Quan

### 2.1 Luồng Tổng Thể

```
                                    ┌─────────────────┐
                                    │   Client App    │
                                    │  (Frontend/FE)  │
                                    └────────┬────────┘
                                             │
                                             │ HTTP Request
                                             ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                              API Gateway / Auth                             │
│                         (SecurityConfig, JwtAuthFilter)                     │
└─────────────────────────────────────────────────────────────────────────────┘
                                             │
                                             │ Authenticated Request
                                             ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                            ReportController                                 │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐       │
│  │ /current-   │  │ /async/     │  │ /schedules  │  │ /export     │       │
│  │ stock       │  │ request     │  │             │  │ /import     │       │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘       │
└─────────┼────────────────┼────────────────┼────────────────┼───────────────┘
          │                │                │                │
          │                │                │                │
          ▼                ▼                ▼                ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                            ReportService                                    │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐       │
│  │ generate    │  │ process     │  │ manage      │  │ handle      │       │
│  │ Report()    │  │ Async()     │  │ Schedule()  │  │ Export()    │       │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘       │
└─────────┼────────────────┼────────────────┼────────────────┼───────────────┘
          │                │                │                │
          ▼                ▼                ▼                ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                           ReportGenerator                                   │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐       │
│  │ JasperReports│  │ Apache POI │  │   OpenCSV   │  │  MinIO      │       │
│  │   (PDF)     │  │  (Excel)   │  │   (CSV)     │  │  (Storage)  │       │
│  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘       │
└─────────────────────────────────────────────────────────────────────────────┘
          │                │                │                │
          ▼                ▼                ▼                ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                           Data Sources                                      │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐       │
│  │  Inventory  │  │  Stock      │  │   Batch     │  │  Warehouse  │       │
│  │  Repository │  │ Movements   │  │ Repository  │  │ Repository  │       │
│  │             │  │ Repository  │  │             │  │             │       │
│  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘       │
└─────────────────────────────────────────────────────────────────────────────┘
          │                │                │                │
          ▼                ▼                ▼                ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                              MySQL Database                                 │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐       │
│  │ inventory   │  │stock_       │  │  batches    │  │ warehouses  │       │
│  │             │  │movements    │  │             │  │             │       │
│  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘       │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 2.2 Flow Categories

```
┌─────────────────────────────────────────────────────────────────┐
│                    Report Processing Modes                      │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌───────────────┐    ┌───────────────┐    ┌───────────────┐   │
│  │   On-Demand   │    │    Async      │    │   Scheduled   │   │
│  │   (Sync)      │    │   (Queue)     │    │   (Cron)      │   │
│  ├───────────────┤    ├───────────────┤    ├───────────────┤   │
│  │ • Immediate   │    │ • Large data  │    │ • Recurring   │   │
│  │ • Small data  │    │ • Background  │    │ • Automated   │   │
│  │ • Real-time   │    │ • Download    │    │ • Email       │   │
│  └───────────────┘    └───────────────┘    └───────────────┘   │
│         │                    │                    │              │
│         ▼                    ▼                    ▼              │
│  ┌───────────────────────────────────────────────────────┐     │
│  │              Shared Components                        │     │
│  ├───────────────────────────────────────────────────────┤     │
│  │ • ReportRepository    • ReportGenerator               │     │
│  │ • ReportValidator     • ExportFormatters (PDF/Excel/CSV)│    │
│  │ • ReportMapper        • MinIO Storage                 │     │
│  └───────────────────────────────────────────────────────┘     │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 3. Chi Tiết Từng Flow

### 3.1 On-Demand Report Flow

```
┌────────┐     ┌────────────┐     ┌────────────┐     ┌────────────┐     ┌────────┐
│ Client │────►│ Controller │────►│  Service   │────►│ Repository │────►│   DB   │
└────────┘     └────────────┘     └────────────┘     └────────────┘     └────────┘
    │               │                   │                   │                 │
    │ 1. Request    │                   │                   │                 │
    │──────────────►│                   │                   │                 │
    │               │ 2. Validate       │                   │                 │
    │               │──────────────────►│                   │                 │
    │               │                   │ 3. Query Data     │                 │
    │               │                   │──────────────────►│                 │
    │               │                   │                   │ 4. Execute SQL  │
    │               │                   │                   │────────────────►│
    │               │                   │                   │ 5. Return Data  │
    │               │                   │                   │◄────────────────│
    │               │                   │ 6. Map to DTO     │                 │
    │               │                   │◄──────────────────│                 │
    │               │ 7. Generate       │                   │                 │
    │               │    Report         │                   │                 │
    │               │◄──────────────────│                   │                 │
    │ 8. Response   │                   │                   │                 │
    │◄──────────────│                   │                   │                 │
    │               │                   │                   │                 │
```

**Chi tiết:** Xem [02-on-demand-report-flow.md](./02-on-demand-report-flow.md)

### 3.2 Async Report Flow

```
┌────────┐     ┌────────────┐     ┌────────────┐     ┌────────────┐     ┌────────┐
│ Client │────►│ Controller │────►│  Service   │────►│  RabbitMQ  │────►│ Worker │
└────────┘     └────────────┘     └────────────┘     └────────────┘     └────────┘
    │               │                   │                   │                 │
    │ 1. Request    │                   │                   │                 │
    │──────────────►│                   │                   │                 │
    │               │ 2. Create Job     │                   │                 │
    │               │──────────────────►│                   │                 │
    │               │                   │ 3. Publish Message│                 │
    │               │                   │──────────────────►│                 │
    │ 4. Return     │                   │                   │                 │
    │    Request ID │                   │                   │                 │
    │◄──────────────│                   │                   │                 │
    │               │                   │                   │ 5. Consume      │
    │               │                   │                   │◄────────────────│
    │               │                   │                   │                 │
    │               │                   │                   │ 6. Process      │
    │               │                   │                   │    Report       │
    │               │                   │                   │    ┌────────────┤
    │               │                   │                   │    │7. Query DB │
    │               │                   │                   │    │8. Generate │
    │               │                   │                   │    │9. Upload   │
    │               │                   │                   │    │   to MinIO │
    │               │                   │                   │    └────────────┤
    │               │                   │                   │                 │
    │ 10. Poll      │                   │                   │                 │
    │    Status     │                   │                   │                 │
    │──────────────►│                   │                   │                 │
    │               │ 11. Get Status    │                   │                 │
    │               │──────────────────►│                   │                 │
    │ 12. Return    │                   │                   │                 │
    │    Status     │                   │                   │                 │
    │◄──────────────│                   │                   │                 │
    │               │                   │                   │                 │
    │ 13. Download  │                   │                   │                 │
    │──────────────►│                   │                   │                 │
    │               │ 14. Get File URL  │                   │                 │
    │               │──────────────────►│                   │                 │
    │ 15. Return    │                   │                   │                 │
    │    File       │                   │                   │                 │
    │◄──────────────│                   │                   │                 │
    │               │                   │                   │                 │
```

**Chi tiết:** Xem [03-async-report-flow.md](./03-async-report-flow.md)

### 3.3 Scheduled Report Flow

```
┌─────────────┐     ┌────────────┐     ┌────────────┐     ┌────────────┐
│  Scheduler  │────►│  Service   │────►│  RabbitMQ  │────►│   Email    │
│  (Spring)   │     │            │     │            │     │  Service   │
└─────────────┘     └────────────┘     └────────────┘     └────────────┘
       │                   │                   │                 │
       │ 1. Trigger        │                   │                 │
       │    (Cron)         │                   │                 │
       │──────────────────►│                   │                 │
       │                   │ 2. Get Active     │                 │
       │                   │    Schedules      │                 │
       │                   │──────────────────►│                 │
       │                   │                   │                 │
       │                   │ 3. For Each       │                 │
       │                   │    Schedule:      │                 │
       │                   │    ┌──────────────┤                 │
       │                   │    │4. Generate    │                 │
       │                   │    │   Report      │                 │
       │                   │    │5. Upload to   │                 │
       │                   │    │   MinIO       │                 │
       │                   │    │6. Send Email  │                 │
       │                   │    │──────────────────────────────►│
       │                   │    │7. Update      │                 │
       │                   │    │   Schedule    │                 │
       │                   │    └──────────────┤                 │
       │                   │                   │                 │
       │ 8. Complete       │                   │                 │
       │◄──────────────────│                   │                 │
       │                   │                   │                 │
```

**Chi tiết:** Xem [04-schedule-report-flow.md](./04-schedule-report-flow.md)

---

## 4. Data Flow Diagram

### 4.1 Data Sources cho Từng Report

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           Report Data Sources                               │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                      Current Stock Report                           │   │
│  ├─────────────────────────────────────────────────────────────────────┤   │
│  │  Tables: inventory, products, warehouses, locations, batches        │   │
│  │  Joins: inventory.product_id -> products.id                         │   │
│  │         inventory.warehouse_id -> warehouses.id                     │   │
│  │         inventory.location_id -> locations.id                       │   │
│  │         inventory.batch_id -> batches.id                            │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                      Stock Valuation Report                         │   │
│  ├─────────────────────────────────────────────────────────────────────┤   │
│  │  Tables: inventory, products, stock_movements                       │   │
│  │  Logic: Calculate weighted average cost from movements              │   │
│  │         inventory.on_hand_quantity * calculated_unit_cost           │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                      Movements Report                               │   │
│  ├─────────────────────────────────────────────────────────────────────┤   │
│  │  Tables: stock_movements, products, warehouses, locations           │   │
│  │  Filter: movement_date, movement_type, reference_type               │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                      Batch Traceability Report                      │   │
│  ├─────────────────────────────────────────────────────────────────────┤   │
│  │  Tables: batches, stock_movements, inventory                        │   │
│  │         purchase_orders, inbound_receipts                           │   │
│  │         sales_orders, outbound_shipments                            │   │
│  │  Logic: Trace batch from creation to current state                  │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                      Low Stock Report                               │   │
│  ├─────────────────────────────────────────────────────────────────────┤   │
│  │  Tables: inventory, products                                        │   │
│  │  Filter: products.reorder_point > inventory.available_quantity      │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                      Expiring Batches Report                        │   │
│  ├─────────────────────────────────────────────────────────────────────┤   │
│  │  Tables: batches, inventory, products                               │   │
│  │  Filter: batches.expiry_date <= (today + days_until_expiry)         │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 4.2 Data Flow Pattern

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         Data Flow Pattern                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐             │
│  │  Filter  │───►│  Query   │───►│  Map to  │───►│ Generate │             │
│  │  Params  │    │  Builder │    │   DTO    │    │  Report  │             │
│  └──────────┘    └──────────┘    └──────────┘    └──────────┘             │
│       │               │               │               │                   │
│       ▼               ▼               ▼               ▼                   │
│  ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐             │
│  │ Validate │    │ Execute  │    │ Enrich   │    │ Export   │             │
│  │ Input    │    │ SQL/JPA  │    │ Data     │    │ Format   │             │
│  └──────────┘    └──────────┘    └──────────┘    └──────────┘             │
│                                                                             │
│  Step 1: Filter Parameters                                                  │
│  ├── warehouse_id, product_id, date_range, etc.                           │
│  └── Validate business rules and access permissions                        │
│                                                                             │
│  Step 2: Query Builder                                                      │
│  ├── Build JPA CriteriaQuery or Native SQL                                 │
│  ├── Apply filters, joins, aggregations                                    │
│  └── Add pagination for large datasets                                     │
│                                                                             │
│  Step 3: Map to DTO                                                         │
│  ├── Convert Entity -> Response DTO                                        │
│  ├── Calculate derived fields                                              │
│  └── Apply formatting rules                                                │
│                                                                             │
│  Step 4: Generate Report                                                    │
│  ├── Fill JasperReports template (PDF)                                     │
│  ├── Create Excel workbook (Apache POI)                                    │
│  └── Generate CSV rows (OpenCSV)                                           │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 5. Integration Points

### 5.1 External Systems

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        External Integration Points                          │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                         MinIO Storage                               │   │
│  ├─────────────────────────────────────────────────────────────────────┤   │
│  │  Purpose: Store generated report files                              │   │
│  │  Bucket: whs-reports                                                │   │
│  │  Path: /{year}/{month}/{report_type}/{report_id}.{ext}             │   │
│  │  Retention: 30 days for async, 7 days for scheduled                 │   │
│  │  Access: Presigned URLs with expiry                                 │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                         RabbitMQ                                    │   │
│  ├─────────────────────────────────────────────────────────────────────┤   │
│  │  Exchange: whs.report.exchange                                      │   │
│  │  Queues:                                                            │   │
│  │    - whs.report.generate.queue (for async report generation)        │   │
│  │    - whs.report.notification.queue (for email notifications)        │   │
│  │  DLQ: whs.report.dlq                                                │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                         Redis                                       │   │
│  ├─────────────────────────────────────────────────────────────────────┤   │
│  │  Purpose: Cache report results, rate limiting                       │   │
│  │  Keys:                                                              │   │
│  │    - report:cache:{hash} (TTL: 5 minutes)                           │   │
│  │    - report:rate:{user_id} (TTL: 1 minute)                          │   │
│  │    - report:lock:{request_id} (TTL: 30 minutes)                     │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                         Email Service                               │   │
│  ├─────────────────────────────────────────────────────────────────────┤   │
│  │  Purpose: Send scheduled report notifications                       │   │
│  │  Template: report-notification.html                                 │   │
│  │  Attachments: Report file (PDF/Excel/CSV)                           │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 5.2 Internal Module Dependencies

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                      Internal Module Dependencies                           │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│                          ┌─────────────────┐                               │
│                          │  Report Module  │                               │
│                          │    (WHS-52)     │                               │
│                          └────────┬────────┘                               │
│                                   │                                        │
│       ┌───────────────┬───────────┼───────────┬───────────────┐           │
│       │               │           │           │               │           │
│       ▼               ▼           ▼           ▼               ▼           │
│  ┌─────────┐    ┌─────────┐ ┌─────────┐ ┌─────────┐    ┌─────────┐       │
│  │Inventory│    │ Inbound │ │Outbound │ │  Batch  │    │Warehouse│       │
│  │ Module  │    │ Module  │ │ Module  │ │ Module  │    │ Module  │       │
│  └─────────┘    └─────────┘ └─────────┘ └─────────┘    └─────────┘       │
│       │               │           │           │               │           │
│       ▼               ▼           ▼           ▼               ▼           │
│  ┌─────────┐    ┌─────────┐ ┌─────────┐ ┌─────────┐    ┌─────────┐       │
│  │Inventory│    │Purchase │ │ Sales   │ │ Batches │    │Ware-    │       │
│  │         │    │ Orders  │ │ Orders  │ │         │    │houses   │       │
│  │Stock    │    │Inbound  │ │Outbound │ │         │    │Locations│       │
│  │Movement │    │Receipts │ │Shipments│ │         │    │         │       │
│  └─────────┘    └─────────┘ └─────────┘ └─────────┘    └─────────┘       │
│                                                                           │
│  Dependencies:                                                            │
│  ├── InventoryService: getInventoryByProduct(), getStockLevels()         │
│  ├── InboundReceiptsService: getReceiptsByDateRange()                    │
│  ├── OutboundShipmentsService: getShipmentsByDateRange()                 │
│  ├── BatchService: getBatchById(), getBatchesByProduct()                 │
│  ├── WareHouseService: getWarehouseById(), getLocations()                │
│  └── ProductService: getProductById(), getProductsByFilter()            │
│                                                                           │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 6. Error Handling Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          Error Handling Flow                                │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                         Error Types                                 │   │
│  ├─────────────────────────────────────────────────────────────────────┤   │
│  │                                                                     │   │
│  │  1. Validation Errors (400)                                         │   │
│  │     ├── Invalid filter parameters                                   │   │
│  │     ├── Invalid date range                                          │   │
│  │     ├── Missing required fields                                     │   │
│  │     └── Invalid export format                                       │   │
│  │                                                                     │   │
│  │  2. Authorization Errors (403)                                      │   │
│  │     ├── No permission to view report                                │   │
│  │     ├── No permission to export data                                │   │
│  │     └── Access to specific warehouse denied                         │   │
│  │                                                                     │   │
│  │  3. Not Found Errors (404)                                          │   │
│  │     ├── Report request not found                                    │   │
│  │     ├── Schedule not found                                          │   │
│  │     └── Generated file not found                                    │   │
│  │                                                                     │   │
│  │  4. Business Errors (422)                                           │   │
│  │     ├── No data found for filter criteria                           │   │
│  │     ├── Report generation timeout                                   │   │
│  │     └── Schedule conflict                                            │   │
│  │                                                                     │   │
│  │  5. System Errors (500)                                             │   │
│  │     ├── Database connection error                                   │   │
│  │     ├── MinIO upload error                                          │   │
│  │     ├── RabbitMQ connection error                                   │   │
│  │     └── Report template error                                       │   │
│  │                                                                     │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                       Error Response Format                         │   │
│  ├─────────────────────────────────────────────────────────────────────┤   │
│  │                                                                     │   │
│  │  {                                                                  │   │
│  │    "success": false,                                                │   │
│  │    "error_code": "REPORT_GENERATION_FAILED",                        │   │
│  │    "message": "Failed to generate report due to database error",    │   │
│  │    "field_errors": [                                                │   │
│  │      {                                                              │   │
│  │        "field": "date_from",                                        │   │
│  │        "message": "Date from must be before date to"                │   │
│  │      }                                                              │   │
│  │    ],                                                               │   │
│  │    "timestamp": "2026-03-22T10:30:00Z"                              │   │
│  │  }                                                                  │   │
│  │                                                                     │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 7. Security Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           Security Flow                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                      Authentication                                 │   │
│  ├─────────────────────────────────────────────────────────────────────┤   │
│  │  1. Client sends JWT token in Authorization header                  │   │
│  │  2. JwtAuthFilter validates token                                   │   │
│  │  3. Extract user_id and roles from token                            │   │
│  │  4. Load user details from database                                 │   │
│  │  5. Set SecurityContext for request                                 │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                      Authorization                                  │   │
│  ├─────────────────────────────────────────────────────────────────────┤   │
│  │                                                                     │   │
│  │  Roles:                                                             │   │
│  │  ├── ADMIN: Full access to all reports and schedules                │   │
│  │  ├── MANAGER: Access to reports for assigned warehouses             │   │
│  │  ├── OPERATOR: Access to on-demand reports only                     │   │
│  │  └── VIEWER: Read-only access to generated reports                  │   │
│  │                                                                     │   │
│  │  Permissions:                                                       │   │
│  │  ├── VIEW_REPORTS: Can view report data                            │   │
│  │  ├── EXPORT_REPORTS: Can export reports                            │   │
│  │  ├── MANAGE_SCHEDULES: Can create/edit/delete schedules            │   │
│  │  └── VIEW_ALL_WAREHOUSES: Can view reports for all warehouses      │   │
│  │                                                                     │   │
│  │  Endpoints:                                                         │   │
│  │  ├── POST /api/v1/reports/*: Requires VIEW_REPORTS                 │   │
│  │  ├── GET /api/v1/reports/*/export: Requires EXPORT_REPORTS         │   │
│  │  ├── POST /api/v1/reports/schedules: Requires MANAGE_SCHEDULES     │   │
│  │  └── GET /api/v1/reports/async/my-requests: Requires VIEW_REPORTS  │   │
│  │                                                                     │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                         Audit Log                                   │   │
│  ├─────────────────────────────────────────────────────────────────────┤   │
│  │  Log Events:                                                        │   │
│  │  ├── REPORT_GENERATED: On-demand report created                     │   │
│  │  ├── REPORT_EXPORTED: Report exported to file                       │   │
│  │  ├── REPORT_DOWNLOADED: Async report downloaded                     │   │
│  │  ├── SCHEDULE_CREATED: New schedule created                         │   │
│  │  ├── SCHEDULE_UPDATED: Schedule modified                            │   │
│  │  ├── SCHEDULE_DELETED: Schedule deleted                             │   │
│  │  └── SCHEDULE_EXECUTED: Scheduled report executed                   │   │
│  │                                                                     │   │
│  │  Log Fields:                                                        │   │
│  │  ├── timestamp                                                      │   │
│  │  ├── user_id                                                        │   │
│  │  ├── action                                                         │   │
│  │  ├── resource_type (report_type or schedule_id)                     │   │
│  │  ├── resource_id (report_request_id or schedule_id)                 │   │
│  │  ├── details (JSON with additional info)                            │   │
│  │  └── ip_address                                                     │   │
│  │                                                                     │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 8. Performance Considerations

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                       Performance Considerations                            │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                         Database                                    │   │
│  ├─────────────────────────────────────────────────────────────────────┤   │
│  │  • Use database views for complex joins                             │   │
│  │  • Add indexes on frequently filtered columns                       │   │
│  │  │  ├── inventory(product_id, warehouse_id)                         │   │
│  │  │  ├── stock_movements(movement_date, product_id)                  │   │
│  │  │  └── batches(expiry_date)                                        │   │
│  │  • Limit result sets with pagination                                │   │
│  │  • Use read replicas for report queries                             │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                         Caching                                     │   │
│  ├─────────────────────────────────────────────────────────────────────┤   │
│  │  • Cache report results in Redis (TTL: 5 minutes)                   │   │
│  │  • Cache product/warehouse master data (TTL: 1 hour)                │   │
│  │  • Invalidate cache on data changes                                 │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                         Async Processing                            │   │
│  ├─────────────────────────────────────────────────────────────────────┤   │
│  │  • Reports > 10,000 rows should use async mode                      │   │
│  │  • Set timeout for report generation (5 minutes)                    │   │
│  │  • Implement retry logic for failed jobs                            │   │
│  │  • Use DLQ for poison messages                                      │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                         Rate Limiting                               │   │
│  ├─────────────────────────────────────────────────────────────────────┤   │
│  │  • On-demand reports: 10 requests/minute per user                   │   │
│  │  • Async reports: 5 requests/hour per user                          │   │
│  │  • Export operations: 20 exports/day per user                       │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 9. Monitoring & Observability

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                       Monitoring & Observability                            │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                         Metrics                                     │   │
│  ├─────────────────────────────────────────────────────────────────────┤   │
│  │  • report_generation_duration_seconds (Histogram)                   │   │
│  │  • report_requests_total (Counter)                                  │   │
│  │  • report_errors_total (Counter)                                    │   │
│  │  • report_queue_size (Gauge)                                        │   │
│  │  • report_file_size_bytes (Histogram)                               │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                         Alerts                                      │   │
│  ├─────────────────────────────────────────────────────────────────────┤   │
│  │  • Report generation time > 5 minutes                               │   │
│  │  • Error rate > 5%                                                  │   │
│  │  • Queue size > 100                                                 │   │
│  │  • MinIO storage > 80%                                              │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                         Logs                                        │   │
│  ├─────────────────────────────────────────────────────────────────────┤   │
│  │  • Structured logging with correlation_id                           │   │
│  │  • Log report request parameters                                    │   │
│  │  • Log generation time and file size                                │   │
│  │  • Log errors with stack trace                                      │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 10. Summary

### Flow Overview

| Flow | Trigger | Processing | Output |
|------|---------|------------|--------|
| On-demand | HTTP Request | Sync | Inline response |
| Async | HTTP Request | Queue | Download link |
| Scheduled | Cron | Auto | Email with attachment |
| Export | HTTP Request | Sync/Async | File download |
| Import | HTTP Request | Async | Status/Errors |

### Key Components

| Component | Responsibility |
|-----------|----------------|
| ReportController | API endpoints, validation |
| ReportService | Business logic, orchestration |
| ReportRepository | Data access, query building |
| ReportGenerator | Report creation (Jasper/POI/CSV) |
| MinIOClient | File storage |
| RabbitMQ | Async message processing |
| Spring Scheduler | Scheduled execution |

### Documentation Index

- [02-on-demand-report-flow.md](./02-on-demand-report-flow.md) - Chi tiết flow on-demand
- [03-async-report-flow.md](./03-async-report-flow.md) - Chi tiết flow async
- [04-schedule-report-flow.md](./04-schedule-report-flow.md) - Chi tiết flow scheduled
- [05-export-import-flow.md](./05-export-import-flow.md) - Chi tiết flow export/import
- [06-sequence-diagrams.md](./06-sequence-diagrams.md) - Sequence diagrams
- [07-data-flow-diagrams.md](./07-data-flow-diagrams.md) - Data flow diagrams
- [08-integration-points.md](./08-integration-points.md) - Integration details
