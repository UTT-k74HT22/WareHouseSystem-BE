# Data Flow Diagrams - Chi Tiết

> Module: WHS-52 (Report Module)
> Updated: 2026-03-22

---

## 1. System Context Diagram (Level 0)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                                                                             │
│                         ┌─────────────────┐                                 │
│                         │   Client App    │                                 │
│                         │  (Frontend/FE)  │                                 │
│                         └────────┬────────┘                                 │
│                                  │                                          │
│                                  │ HTTP Request                             │
│                                  │ (REST API)                               │
│                                  ▼                                          │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                                                                     │   │
│  │                    WMS Report Module (System)                       │   │
│  │                                                                     │   │
│  │  ┌─────────────────────────────────────────────────────────────┐   │   │
│  │  │  • Generate reports on-demand                               │   │   │
│  │  │  • Process async report requests                            │   │   │
│  │  │  • Manage scheduled reports                                 │   │   │
│  │  │  • Export data to PDF/Excel/CSV                             │   │   │
│  │  │  • Import data from Excel/CSV                               │   │   │
│  │  └─────────────────────────────────────────────────────────────┘   │   │
│  │                                                                     │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                  │                                          │
│           ┌──────────────────────┼──────────────────────┐                  │
│           │                      │                      │                  │
│           ▼                      ▼                      ▼                  │
│  ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐       │
│  │   MySQL DB      │    │   RabbitMQ      │    │   MinIO         │       │
│  │                 │    │                 │    │                 │       │
│  │ • Report data   │    │ • Async queue   │    │ • File storage  │       │
│  │ • Schedule data │    │ • Message bus   │    │ • Report files  │       │
│  └─────────────────┘    └─────────────────┘    └─────────────────┘       │
│                                  │                                          │
│                                  ▼                                          │
│                         ┌─────────────────┐                                 │
│                         │   Email Server  │                                 │
│                         │   (SMTP)        │                                 │
│                         │                 │                                 │
│                         │ • Send reports  │                                 │
│                         └─────────────────┘                                 │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Context Diagram (Level 1)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                                                                             │
│  External Entities:                                                         │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐       │
│  │   Client    │  │  Admin      │  │  Scheduler  │  │  Monitor    │       │
│  │   User      │  │  User       │  │  (Cron)     │  │  System     │       │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘       │
│         │                │                │                │              │
│         │ Request        │ Manage         │ Trigger        │ Metrics      │
│         │ Report         │ Schedule       │ Schedule       │              │
│         ▼                ▼                ▼                ▼              │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                        Report Module                                │   │
│  │                                                                     │   │
│  │  ┌───────────────┐  ┌───────────────┐  ┌───────────────┐           │   │
│  │  │   Report      │  │   Schedule    │  │   Export      │           │   │
│  │  │   Service     │  │   Service     │  │   Service     │           │   │
│  │  └───────┬───────┘  └───────┬───────┘  └───────┬───────┘           │   │
│  │          │                  │                  │                    │   │
│  │          ▼                  ▼                  ▼                    │   │
│  │  ┌─────────────────────────────────────────────────────────────┐   │   │
│  │  │                    Shared Components                        │   │   │
│  │  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐         │   │   │
│  │  │  │  Repository │  │  Generator  │  │  Validator  │         │   │   │
│  │  │  │  (Data)     │  │  (Report)   │  │  (Input)    │         │   │   │
│  │  │  └─────────────┘  └─────────────┘  └─────────────┘         │   │   │
│  │  └─────────────────────────────────────────────────────────────┘   │   │
│  │                                                                     │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Data Flow Diagram (Level 2) - Report Generation

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                     Report Generation Data Flow                             │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐ │
│  │   Client    │───►│ Controller  │───►│  Service    │───►│ Repository  │ │
│  │             │    │             │    │             │    │             │ │
│  └─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘ │
│                                                                             │
│  Data Elements:                                                             │
│                                                                             │
│  1. Request Data (Client → Controller):                                    │
│     ├── report_type: string                                                │
│     ├── parameters: {                                                     │
│     │   ├── warehouse_id: uuid                                            │
│     │   ├── product_id: uuid                                              │
│     │   ├── date_from: date                                               │
│     │   ├── date_to: date                                                 │
│     │   └── filters: {...}                                                │
│     │                                                                     │
│     └── format: PDF | EXCEL | CSV                                         │
│                                                                             │
│  2. Validated Data (Controller → Service):                                 │
│     ├── Same as request data                                              │
│     └── + user context (from SecurityContext)                              │
│                                                                             │
│  3. Query Parameters (Service → Repository):                               │
│     ├── warehouse_id: string                                              │
│     ├── product_id: string                                                │
│     ├── date_from: LocalDateTime                                          │
│     ├── date_to: LocalDateTime                                            │
│     ├── movement_type: enum                                               │
│     └── pagination: {page, size}                                          │
│                                                                             │
│  4. Database Records (Repository → Database):                              │
│     ├── inventory: {product_id, warehouse_id, quantity, ...}              │
│     ├── stock_movements: {type, date, quantity, ...}                       │
│     ├── batches: {number, expiry_date, ...}                               │
│     └── products: {sku, name, category, ...}                              │
│                                                                             │
│  5. Entity Results (Database → Repository):                                │
│     └── List<Inventory>, List<StockMovement>, etc.                        │
│                                                                             │
│  6. Response Data (Service → Controller):                                  │
│     └── ReportResponse {items, summary, metadata}                         │
│                                                                             │
│  7. API Response (Controller → Client):                                    │
│     └── BaseResponse<ReportResponse>                                      │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 4. Data Flow Diagram - Async Processing

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                     Async Report Processing Data Flow                       │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐ │
│  │   Client    │───►│ Controller  │───►│  Service    │───►│ RabbitMQ    │ │
│  │             │    │             │    │             │    │             │ │
│  └─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘ │
│                                                                             │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐ │
│  │   MinIO     │◄───│  Consumer   │◄───│  Queue      │◄───│  Message    │ │
│  │             │    │             │    │             │    │  Broker     │ │
│  └─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘ │
│                                                                             │
│  Data Elements:                                                             │
│                                                                             │
│  1. Async Request (Client → Service):                                      │
│     ├── report_type: ReportType                                           │
│     ├── parameters: Map<String, Object>                                   │
│     ├── format: ExportFormat                                              │
│     ├── priority: Priority                                                │
│     └── callback_url: string (optional)                                   │
│                                                                             │
│  2. Report Request Entity (Service → Database):                            │
│     ├── id: uuid                                                          │
│     ├── report_type: string                                               │
│     ├── parameters: JSON string                                           │
│     ├── format: string                                                    │
│     ├── status: PENDING                                                   │
│     ├── requested_by: string                                              │
│     ├── requested_at: timestamp                                           │
│     └── retry_count: int                                                  │
│                                                                             │
│  3. Message (Service → RabbitMQ):                                          │
│     ├── request_id: uuid                                                  │
│     ├── report_type: string                                               │
│     ├── parameters: Map<String, Object>                                   │
│     └── format: string                                                    │
│                                                                             │
│  4. Processing Data (Consumer):                                            │
│     ├── Read message from queue                                           │
│     ├── Fetch ReportRequest from DB                                       │
│     ├── Generate report data                                              │
│     └── Upload to MinIO                                                   │
│                                                                             │
│  5. File Upload (Consumer → MinIO):                                        │
│     ├── object_name: string (reports/{year}/{month}/{type}/{id}.{ext})    │
│     ├── content: byte[]                                                   │
│     └── content_type: string                                              │
│                                                                             │
│  6. Status Update (Consumer → Database):                                   │
│     ├── status: PROCESSING → COMPLETED                                    │
│     ├── started_at: timestamp                                             │
│     ├── completed_at: timestamp                                           │
│     ├── file_url: string                                                  │
│     └── file_size: long                                                   │
│                                                                             │
│  7. Download URL (Service → Client):                                       │
│     ├── download_url: string (presigned URL)                              │
│     ├── expires_at: timestamp                                             │
│     └── file_name: string                                                 │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 5. Data Flow Diagram - Export/Import

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                     Export/Import Data Flow                                 │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  EXPORT FLOW:                                                               │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐ │
│  │   Client    │───►│ Controller  │───►│  Service    │───►│ Repository  │ │
│  │             │    │             │    │             │    │             │ │
│  └─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘ │
│                                                                             │
│  Data Elements:                                                             │
│  1. Export Request:                                                        │
│     ├── format: EXCEL | CSV | PDF                                         │
│     └── filters: {category_id, status, ...}                               │
│                                                                             │
│  2. Query Results:                                                         │
│     └── List<Product> or List<Location>                                   │
│                                                                             │
│  3. Generated File:                                                        │
│     ├── content: byte[]                                                   │
│     ├── content_type: string                                              │
│     └── filename: string                                                  │
│                                                                             │
│  IMPORT FLOW:                                                               │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐ │
│  │   Client    │───►│ Controller  │───►│  Service    │───►│ Parser      │ │
│  │ (file)      │    │             │    │             │    │             │ │
│  └─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘ │
│                                                                             │
│  Data Elements:                                                             │
│  1. Import Request:                                                        │
│     ├── file: MultipartFile                                               │
│     ├── validate_only: boolean                                            │
│     └── update_existing: boolean                                          │
│                                                                             │
│  2. Parsed Rows:                                                           │
│     └── List<ImportRow>                                                   │
│                                                                             │
│  3. Validation Errors:                                                     │
│     └── List<ValidationError> {row, field, value, message}                │
│                                                                             │
│  4. Import Result:                                                         │
│     ├── total_rows: int                                                   │
│     ├── success_count: int                                                │
│     ├── error_count: int                                                  │
│     ├── skip_count: int                                                   │
│     └── errors: List<ValidationError>                                     │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 6. Data Store Descriptions

### 6.1 Primary Data Stores

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         Data Store Descriptions                             │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  D1: MySQL Database                                                        │
│  ├── Tables:                                                               │
│  │   ├── inventory (product_id, warehouse_id, quantity, ...)              │
│  │   ├── stock_movements (movement_type, product_id, quantity, ...)       │
│  │   ├── batches (batch_number, expiry_date, ...)                         │
│  │   ├── products (sku, name, category_id, ...)                           │
│  │   ├── warehouses (code, name, address, ...)                            │
│  │   ├── locations (code, warehouse_id, type, ...)                        │
│  │   ├── purchase_orders, inbound_receipts, ...                           │
│  │   ├── sales_orders, outbound_shipments, ...                            │
│  │   ├── report_requests (id, report_type, status, ...)                   │
│  │   └── report_schedules (id, name, cron_expression, ...)                │
│  │                                                                        │
│  └── Purpose: Store all business data and report metadata                 │
│                                                                             │
│  D2: RabbitMQ                                                              │
│  ├── Queues:                                                               │
│  │   ├── whs.report.generate.queue (for async reports)                    │
│  │   ├── whs.report.notification.queue (for email)                        │
│  │   └── whs.report.dlq (dead letter queue)                               │
│  │                                                                        │
│  └── Purpose: Message broker for async processing                         │
│                                                                             │
│  D3: MinIO                                                                 │
│  ├── Buckets:                                                              │
│  │   └── whs-reports                                                      │
│  │       ├── reports/{year}/{month}/{type}/{id}.{ext}                     │
│  │       └── imports/{year}/{month}/{type}/{id}.{ext}                     │
│  │                                                                        │
│  └── Purpose: Store generated report files                                │
│                                                                             │
│  D4: Redis                                                                 │
│  ├── Keys:                                                                 │
│  │   ├── report:cache:{hash} (cached report results)                      │
│  │   ├── report:rate:{user_id} (rate limiting)                            │
│  │   ├── report:lock:{id} (distributed locks)                             │
│  │   └── report:progress:{id} (async progress)                            │
│  │                                                                        │
│  └── Purpose: Caching, rate limiting, distributed locks                   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 6.2 Data Dictionary

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                            Data Dictionary                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  Entity: ReportRequest                                                      │
│  ├── id: UUID (PK)                                                        │
│  ├── report_type: VARCHAR(50) - Type of report                            │
│  ├── parameters: JSON - Report parameters                                 │
│  ├── format: ENUM (PDF, EXCEL, CSV) - Output format                       │
│  ├── status: ENUM (PENDING, PROCESSING, COMPLETED, FAILED)                │
│  ├── priority: ENUM (LOW, NORMAL, HIGH)                                   │
│  ├── requested_by: UUID (FK to user)                                      │
│  ├── requested_at: TIMESTAMP                                              │
│  ├── started_at: TIMESTAMP                                                │
│  ├── completed_at: TIMESTAMP                                              │
│  ├── file_url: VARCHAR(500) - MinIO object path                           │
│  ├── file_size: BIGINT                                                    │
│  ├── error_message: TEXT                                                  │
│  ├── retry_count: INT                                                     │
│  ├── max_retries: INT                                                     │
│  ├── created_at: TIMESTAMP                                                │
│  └── updated_at: TIMESTAMP                                                │
│                                                                             │
│  Entity: ReportSchedule                                                     │
│  ├── id: UUID (PK)                                                        │
│  ├── name: VARCHAR(100) - Schedule name                                   │
│  ├── description: TEXT                                                    │
│  ├── report_type: VARCHAR(50)                                             │
│  ├── parameters: JSON                                                     │
│  ├── format: ENUM (PDF, EXCEL, CSV)                                       │
│  ├── cron_expression: VARCHAR(50)                                         │
│  ├── timezone: VARCHAR(50) - Default: Asia/Ho_Chi_Minh                   │
│  ├── is_enabled: BOOLEAN                                                  │
│  ├── last_run_at: TIMESTAMP                                               │
│  ├── next_run_at: TIMESTAMP                                               │
│  ├── recipients: JSON - Array of email addresses                          │
│  ├── created_by: UUID (FK to user)                                        │
│  ├── created_at: TIMESTAMP                                                │
│  └── updated_at: TIMESTAMP                                                │
│                                                                             │
│  Value Object: ValidationError                                              │
│  ├── row: INT - Row number in import file                                 │
│  ├── field: VARCHAR - Field name with error                               │
│  ├── value: VARCHAR - Invalid value                                       │
│  └── message: VARCHAR - Error description                                 │
│                                                                             │
│  Enum: ReportType                                                           │
│  ├── CURRENT_STOCK                                                        │
│  ├── STOCK_VALUATION                                                      │
│  ├── MOVEMENTS                                                            │
│  ├── BATCH_TRACEABILITY                                                   │
│  ├── LOW_STOCK                                                            │
│  └── EXPIRING_BATCHES                                                     │
│                                                                             │
│  Enum: ExportFormat                                                         │
│  ├── PDF (content_type: application/pdf)                                  │
│  ├── EXCEL (content_type: application/vnd.ms-excel)                       │
│  └── CSV (content_type: text/csv)                                         │
│                                                                             │
│  Enum: ReportRequestStatus                                                  │
│  ├── PENDING                                                              │
│  ├── PROCESSING                                                           │
│  ├── COMPLETED                                                            │
│  ├── FAILED                                                               │
│  └── DOWNLOADED                                                           │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 7. Data Transformation Rules

### 7.1 Entity to DTO Mapping

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                      Entity to DTO Transformation                           │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  Inventory → CurrentStockItem:                                             │
│  ├── product_id → productId                                               │
│  ├── product.sku → productSku                                             │
│  ├── product.name → productName                                           │
│  ├── warehouse.name → warehouseName                                       │
│  ├── location.code → locationCode                                         │
│  ├── on_hand_quantity → onHandQuantity                                    │
│  ├── reserved_quantity → reservedQuantity                                 │
│  ├── quarantine_quantity → quarantineQuantity                             │
│  ├── (calculated) → availableQuantity                                     │
│  │   └── on_hand - reserved - quarantine                                  │
│  ├── batch.batch_number → batchNumber                                     │
│  ├── batch.expiry_date → expiryDate                                       │
│  └── (calculated) → stockValue                                            │
│      └── available_quantity * product.cost_price                          │
│                                                                             │
│  StockMovement → MovementItem:                                             │
│  ├── movement_type → movementType                                         │
│  ├── movement_date → movementDate                                         │
│  ├── product.sku → productSku                                             │
│  ├── product.name → productName                                           │
│  ├── warehouse.name → warehouseName                                       │
│  ├── location.code → locationCode                                         │
│  ├── quantity_change → quantityChange                                     │
│  ├── quantity_before → quantityBefore                                     │
│  ├── quantity_after → quantityAfter                                       │
│  ├── reference_type → referenceType                                       │
│  └── reference_number → referenceNumber                                   │
│                                                                             │
│  Batch → BatchInfo:                                                        │
│  ├── batch_number → batchNumber                                           │
│  ├── manufacture_date → manufactureDate                                   │
│  ├── expiry_date → expiryDate                                             │
│  ├── (calculated) → daysUntilExpiry                                       │
│  │   └── expiry_date - current_date                                       │
│  └── status → status                                                      │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 7.2 Aggregation Rules

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          Aggregation Rules                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  Current Stock Summary:                                                     │
│  ├── totalProducts: COUNT(DISTINCT product_id)                            │
│  ├── totalWarehouses: COUNT(DISTINCT warehouse_id)                        │
│  ├── totalOnHand: SUM(on_hand_quantity)                                   │
│  ├── totalReserved: SUM(reserved_quantity)                                │
│  ├── totalAvailable: SUM(available_quantity)                              │
│  └── totalValue: SUM(stock_value)                                         │
│                                                                             │
│  Movements Summary:                                                         │
│  ├── totalMovements: COUNT(*)                                             │
│  ├── inboundCount: COUNT(WHERE movement_type = 'INBOUND')                 │
│  ├── outboundCount: COUNT(WHERE movement_type = 'OUTBOUND')               │
│  ├── transferCount: COUNT(WHERE movement_type = 'TRANSFER')               │
│  ├── adjustmentCount: COUNT(WHERE movement_type = 'ADJUSTMENT')           │
│  ├── totalInboundQty: SUM(WHERE movement_type = 'INBOUND')                │
│  └── totalOutboundQty: SUM(WHERE movement_type = 'OUTBOUND')              │
│                                                                             │
│  Low Stock Summary:                                                         │
│  ├── productsBelowReorder: COUNT(WHERE available < reorder_point)          │
│  ├── productsBelowMin: COUNT(WHERE available < min_stock_level)            │
│  ├── criticalProducts: COUNT(WHERE available = 0)                         │
│  └── totalReorderValue: SUM(reorder_quantity * cost_price)                │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 8. Documentation Index

- [01-end-to-end-flow.md](./01-end-to-end-flow.md) - Flow tổng quan
- [02-on-demand-report-flow.md](./02-on-demand-report-flow.md) - Chi tiết on-demand
- [03-async-report-flow.md](./03-async-report-flow.md) - Chi tiết async
- [04-schedule-report-flow.md](./04-schedule-report-flow.md) - Chi tiết scheduled
- [05-export-import-flow.md](./05-export-import-flow.md) - Chi tiết export/import
- [06-sequence-diagrams.md](./06-sequence-diagrams.md) - Sequence diagrams
- [08-integration-points.md](./08-integration-points.md) - Integration
