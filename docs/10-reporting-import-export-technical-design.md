# Reporting / Import-Export Technical Design

> Cập nhật: 2026-03-22  
> Base package: `org.demo.whs`  
> Target stack: Spring Boot 3.5.9, RabbitMQ, Redis, MinIO, MySQL 8  
> Note: Repo hiện đang chạy MySQL 8; các nguyên tắc dưới đây vẫn áp dụng cho PostgreSQL, nhưng SQL/index/procedure cần tune theo engine.

---

## A. Executive Summary

Hướng đi phù hợp nhất cho WMS này là kiến trúc hybrid gồm 2 lane rõ ràng:

1. `Synchronous lane` cho truy vấn nhỏ, phản hồi nhanh, UX tải ngay.
2. `Asynchronous lane` cho dữ liệu lớn, file nặng, xử lý lâu, scheduled report, email notification và các import/export có rủi ro timeout.

Thiết kế nên giữ ở mức thực dụng:

- Dùng `Strategy Pattern` cho format export (`PDF`, `XLSX`, `CSV`) và parser import (`CSV`, `XLSX`).
- Dùng `Template Method Pattern` cho pipeline export/import chuẩn.
- Không mở rộng sớm sang factory phức tạp, event bus nội bộ hoặc quá nhiều abstraction.
- Lấy `query optimization` làm trọng tâm trước cache, before-threading, before-patterns.
- Dùng `RabbitMQ + job tracking + MinIO` cho lane async.
- Dùng `template cache + selective reference cache + very selective result cache`.
- Dùng `stored procedure` có chọn lọc cho một số report/query nặng, ổn định, lặp lại nhiều.
- Dùng concurrency có kiểm soát ở mức worker/job consumer, không song song hóa bừa bãi từng bước trong app.

Kết luận ngắn gọn:

- Sync cho case nhỏ.
- Async cho case lớn.
- Tối ưu query trước.
- Theo dõi job rõ ràng cho FE.
- Cache có chọn lọc.
- Procedure và multi-thread chỉ là công cụ phụ trợ, không phải xương sống kiến trúc.

---

## B. Kiến Trúc Đề Xuất

### B.1. Mục tiêu kiến trúc

Kiến trúc này phải đạt đồng thời 6 mục tiêu:

1. Không timeout với report/export/import lớn.
2. Không phình codebase bằng quá nhiều pattern.
3. Không làm DB và app server quá tải do load full entity/full RAM.
4. Cho FE một UX rõ ràng về tiến trình, trạng thái và lỗi.
5. Reuse tối đa hạ tầng repo hiện có: RabbitMQ, Redis, MinIO, email async, `BaseResponse`, `PageResponse`.
6. Dễ nâng cấp lên snapshot table/read replica/reporting read model nếu quy mô tăng.

### B.2. Kiến trúc tổng thể

```text
+-------------------+        +---------------------+
| Frontend Web App  |<------>| Job Status API      |
| - dashboard       |        | - my jobs           |
| - export/import   |        | - progress/detail   |
| - scheduled report|        | - retry/cancel      |
+---------+---------+        +----------+----------+
          |                             ^
          v                             |
+-------------------+        +----------+----------+
| Report/Import API |------->| Job Tracking Service|
| - sync lane       |        | - create/update job |
| - async dispatch  |        | - step log          |
+---------+---------+        +----------+----------+
          |                             ^
          | sync                        | async status/progress
          v                             |
+-------------------+        +----------+----------+
| Query Layer       |        | RabbitMQ            |
| - projections     |------->| - report queue      |
| - native/JDBC     |        | - import queue      |
| - aggregates      |        | - DLQ               |
+---------+---------+        +----------+----------+
          |                             |
          v                             v
+-------------------+        +---------------------+
| MySQL 8           |        | Worker Consumers    |
| - transactional   |        | - validate          |
| - indexed reads   |        | - chunk process     |
| - optional proc   |        | - generate file     |
+---------+---------+        | - upload MinIO      |
          |                  +----------+----------+
          v                             |
+-------------------+                  v
| Redis             |        +---------------------+
| - template/meta   |        | MinIO               |
| - selective cache |        | - report files      |
| - short TTL data  |        | - import error file |
+-------------------+        +----------+----------+
                                         |
                                         v
                               +---------------------+
                               | Notification        |
                               | - existing email MQ |
                               | - completion alert  |
                               +---------------------+
```

### B.3. Hai lane xử lý

| Tiêu chí | Synchronous lane | Asynchronous lane |
|---|---|---|
| Mục tiêu UX | Tải ngay / phản hồi tức thì | Gửi job, theo dõi tiến trình |
| Thời gian xử lý mục tiêu | `< 3s` tốt, tối đa `5s` | `> 5s` hoặc không chắc chắn |
| Dataset | Nhỏ, bounded | Lớn, khó dự đoán, nhiều join/aggregate |
| File | CSV/XLSX/PDF nhỏ | CSV/XLSX/PDF lớn |
| DB load | Query đơn giản, index-friendly | Query nặng, chunked, background |
| Use case điển hình | Dashboard card, preview, export nhỏ | Scheduled report, traceability lớn, valuation lớn, import nhiều dòng |
| Response FE | File/JSON trả ngay | Job id + progress + link tải khi xong |

### B.4. Quy tắc quyết định sync hay async

Không nên dựa vào một ngưỡng duy nhất. Nên dùng `rule-based decision` theo thứ tự:

1. `Force async` nếu là scheduled report, email report, user chủ động chọn "run in background", hoặc thao tác cần lưu file vào MinIO.
2. `Force async` nếu format là `PDF` và report là dạng chi tiết nhiều dòng.
3. `Force async` nếu query complexity cao:
   - nhiều hơn 3 bảng join lớn
   - valuation tính theo lịch sử
   - batch traceability theo chuỗi movement
   - aggregate theo time range dài
4. `Force async` nếu estimated processing time `> 5s`.
5. `Force async` nếu file dự kiến lớn hoặc số dòng vượt ngưỡng.
6. Chỉ `sync` khi tất cả điều kiện nhỏ đều thỏa.

### B.5. Ngưỡng gợi ý small dataset vs large dataset

#### Export/report

| Loại | Sync gợi ý | Async gợi ý |
|---|---|---|
| CSV | `<= 10,000` rows, file `< 5 MB`, query đơn giản | `> 20,000` rows hoặc time range rộng |
| XLSX | `<= 5,000` rows, file `< 5 MB` | `> 10,000` rows hoặc có nhiều formatting/formula |
| PDF | `<= 2,000 - 3,000` rows, report summary | `> 3,000` rows hoặc report detail |
| Dashboard aggregate | `< 1s - 2s`, số liệu tổng hợp | snapshot/async refresh nếu query nặng |

#### Import

| Loại | Sync gợi ý | Async gợi ý |
|---|---|---|
| CSV master data | `<= 500` rows, validate đơn giản | `> 500` rows hoặc có cross-row validation |
| XLSX master data | `<= 300` rows | `> 300` rows |
| Inventory-affecting import | Chỉ sync nếu rất nhỏ và đã giới hạn rõ | Mặc định ưu tiên async |
| Validate-only preview | `<= 2,000` rows có thể sync | lớn hơn thì async |

#### Rule thực thi đề xuất

```text
if scheduled || requestedAsync || requiresEmail:
    async
else if estimatedDurationMs > 5000:
    async
else if format == PDF && estimatedRows > 3000:
    async
else if format == XLSX && estimatedRows > 5000:
    async
else if format == CSV && estimatedRows > 10000 && complexity != LOW:
    async
else if importRows > 500 || importFileSizeMb > 2:
    async
else:
    sync
```

### B.6. Async processing và status lifecycle

Lane async phải có `job/task tracking` chuẩn để FE không bị kẹt ở spinner.

Trạng thái đề xuất:

```text
PENDING
-> VALIDATING
-> PROCESSING
-> GENERATING_FILE
-> UPLOADING
-> COMPLETED

FAILED
CANCELLED
```

Khuyến nghị thêm 2 trường để FE hiển thị tốt:

- `current_step`: tên bước hiện tại dễ đọc.
- `progress_percent`: 0..100.

Mapping progress gợi ý:

| Status | Progress range |
|---|---|
| PENDING | 0 |
| VALIDATING | 5 - 15 |
| PROCESSING | 15 - 70 |
| GENERATING_FILE | 70 - 85 |
| UPLOADING | 85 - 95 |
| COMPLETED | 100 |
| FAILED / CANCELLED | giữ nguyên progress cuối + error |

### B.7. RabbitMQ topology

Repo đã có RabbitMQ cho email. Với reporting/import-export nên bổ sung topology riêng thay vì nhét vào queue email:

```text
Exchange: wms.jobs.exchange

Queues:
- wms.jobs.report.queue
- wms.jobs.import.queue
- wms.jobs.report.dlq
- wms.jobs.import.dlq

Routing keys:
- jobs.report.requested
- jobs.import.requested
- jobs.report.failed
- jobs.import.failed
```

Khuyến nghị:

- `prefetch = 1` cho worker nặng.
- `concurrentConsumers` khởi đầu thấp: report `2-4`, import `1-3`.
- Có `DLQ` để cô lập poison message.
- Job completion notification có thể reuse hạ tầng email async sẵn có.

### B.8. File storage

MinIO đã có sẵn trong repo và nên là nơi lưu artifact của async job:

- export file
- import error file
- summary CSV
- optional raw uploaded file lưu tạm để retry/audit

Object path gợi ý:

```text
reports/{yyyy}/{MM}/{jobId}/report.xlsx
reports/{yyyy}/{MM}/{jobId}/report.csv
imports/{yyyy}/{MM}/{jobId}/source.xlsx
imports/{yyyy}/{MM}/{jobId}/errors.csv
```

File không nên trả trực tiếp qua app server sau khi xử lý async. Backend nên:

1. authorize user/job ownership
2. generate presigned URL ngắn hạn
3. trả link download

### B.9. FE update channel

Ba lựa chọn:

| Cơ chế | Ưu điểm | Nhược điểm | Khuyến nghị |
|---|---|---|---|
| Polling | đơn giản nhất, dễ triển khai | nhiều request lặp lại | `Phase 1 default` |
| SSE | một chiều, phù hợp progress updates | cần quản lý reconnect | `Phase 2 nếu cần realtime mượt hơn` |
| WebSocket | realtime mạnh, hai chiều | phức tạp hơn, dễ overkill | chỉ dùng nếu hệ thống đã có live notifications rộng |

Khuyến nghị thực dụng:

- `Phase 1`: polling mỗi `3-5s` cho job detail và `10-15s` cho job list.
- `Phase 2`: thêm SSE cho màn hình đang theo dõi job active.
- Không lấy WebSocket làm mặc định chỉ vì dependency đã có trong pom.

---

## C. Thiết Kế Backend Chi Tiết

### C.1. Invariants cần bảo vệ

Trước khi code, module này phải giữ các invariant sau:

1. Không load toàn bộ dataset lớn vào RAM.
2. Job state transition phải explicit và hợp lệ.
3. User chỉ xem/tải được job và file thuộc scope được phép.
4. Không ghi dữ liệu import vượt ra ngoài tenant/company/warehouse scope hợp lệ.
5. Inventory/report totals không được tin vào dữ liệu client, luôn tính lại từ DB.
6. Progress/status phải phản ánh đúng, không bị "COMPLETED" khi upload file lỗi.

### C.2. Module/class đề xuất cho Spring Boot

Package proposal ở mức vừa đủ:

```text
org.demo.whs
├── controller
│   ├── ReportController.java
│   ├── ImportController.java
│   ├── JobController.java
│   └── DashboardController.java
├── service
│   ├── ReportService.java
│   ├── ImportService.java
│   ├── JobService.java
│   ├── ScheduledReportService.java
│   ├── JobStatusUpdater.java
│   ├── NotificationService.java
│   ├── StorageService.java                  # reuse + extend contract
│   └── impl
│       ├── ReportServiceImpl.java
│       ├── ImportServiceImpl.java
│       ├── JobServiceImpl.java
│       ├── ScheduledReportServiceImpl.java
│       ├── JobStatusUpdaterImpl.java
│       └── NotificationServiceImpl.java
├── processor
│   ├── AbstractExportProcessor.java
│   ├── AbstractImportProcessor.java
│   ├── ReportExportProcessor.java
│   ├── ProductImportProcessor.java
│   └── LocationImportProcessor.java
├── strategy
│   ├── export
│   │   ├── ExportStrategy.java
│   │   ├── CsvExportStrategy.java
│   │   ├── ExcelExportStrategy.java
│   │   └── PdfExportStrategy.java
│   ├── importing
│   │   ├── ImportParserStrategy.java
│   │   ├── CsvImportParserStrategy.java
│   │   └── XlsxImportParserStrategy.java
│   └── report
│       ├── ReportDataProvider.java
│       ├── CurrentStockReportProvider.java
│       ├── StockMovementReportProvider.java
│       ├── StockValuationReportProvider.java
│       ├── ExpiringBatchesReportProvider.java
│       └── BatchTraceabilityReportProvider.java
├── repository
│   ├── BackgroundJobRepository.java
│   ├── ScheduledReportRepository.java
│   ├── JobStepLogRepository.java
│   └── custom
│       ├── ReportQueryRepositoryCustom.java
│       ├── DashboardQueryRepositoryCustom.java
│       └── impl
│           ├── ReportQueryRepositoryImpl.java
│           └── DashboardQueryRepositoryImpl.java
├── entity
│   ├── BackgroundJob.java
│   ├── BackgroundJobStepLog.java
│   ├── ScheduledReport.java
│   └── enums
│       ├── BackgroundJobStatus.java
│       ├── BackgroundJobType.java
│       ├── JobStep.java
│       ├── ExportFormat.java
│       └── ImportMode.java
└── entity.dto
    ├── request
    │   ├── report
    │   ├── importing
    │   └── dashboard
    └── response
        ├── report
        ├── importing
        └── job
```

Điểm quan trọng:

- Không cần thêm một lớp factory riêng nếu Spring đã inject được list/map strategy theo enum.
- Không cần event bus nội bộ cho job state change; `JobStatusUpdater` + queue là đủ rõ ràng.
- Không nên tách quá nhiều module con nếu team còn nhỏ; package trên chỉ nên tạo khi thực sự bắt đầu implement.

### C.3. Entity và schema tối thiểu

#### `background_jobs`

Một bảng generic cho async job là hợp lý hơn tạo nhiều bảng nhỏ cho mỗi loại report/import.

Các cột gợi ý:

| Cột | Ý nghĩa |
|---|---|
| `id` | UUID job |
| `job_type` | `REPORT_EXPORT`, `DATA_IMPORT`, `SCHEDULED_REPORT`, `DASHBOARD_REFRESH` |
| `business_type` | `CURRENT_STOCK`, `LOW_STOCK`, `PRODUCT_IMPORT`, `LOCATION_IMPORT` |
| `status` | lifecycle chính |
| `current_step` | bước hiện tại |
| `progress_percent` | 0..100 |
| `requested_by` | user tạo job |
| `trigger_source` | `USER`, `SCHEDULE`, `SYSTEM` |
| `request_payload` | JSON filter/file meta |
| `file_name` | tên file trả user |
| `storage_object_key` | MinIO object key |
| `mime_type` | loại file |
| `file_size` | kích thước file |
| `processed_items` | số row đã xử lý |
| `total_items` | tổng row ước tính hoặc thực tế |
| `success_items` | import/export thành công |
| `failed_items` | import row lỗi |
| `error_code` | mã lỗi ổn định |
| `error_message` | thông tin hiển thị |
| `started_at`, `finished_at` | SLA/metrics |
| `created_at`, `updated_at` | audit |

Index gợi ý:

- `(requested_by, created_at desc)`
- `(status, created_at)`
- `(job_type, status, created_at)`
- `(business_type, created_at)`

#### `background_job_step_logs`

Chỉ cần một bảng log bước đơn giản:

- `job_id`
- `step`
- `message`
- `status`
- `processed_items`
- `created_at`

Không nên lưu quá chi tiết từng row lỗi vào đây. Row lỗi nên đưa vào error file hoặc bảng staging riêng.

#### `scheduled_reports`

Các cột gợi ý:

- `id`
- `report_type`
- `format`
- `cron_expression`
- `request_payload`
- `recipient_emails`
- `active`
- `last_run_at`
- `next_run_at`
- `last_job_id`

Scheduler chỉ nên `enqueue job`, không tự generate file trong thread scheduler.

### C.4. API và controller đề xuất

#### Report/export

```text
POST /api/v1/reports/{type}/preview
POST /api/v1/reports/{type}/export
POST /api/v1/reports/{type}/export/async
```

#### Import

```text
POST /api/v1/imports/{type}/validate
POST /api/v1/imports/{type}
POST /api/v1/imports/{type}/async
```

#### Jobs

```text
GET  /api/v1/jobs/my
GET  /api/v1/jobs/{jobId}
POST /api/v1/jobs/{jobId}/retry
POST /api/v1/jobs/{jobId}/cancel
GET  /api/v1/jobs/{jobId}/download
```

#### Scheduled reports

```text
POST /api/v1/report-schedules
GET  /api/v1/report-schedules
PUT  /api/v1/report-schedules/{id}
DELETE /api/v1/report-schedules/{id}
```

Response luôn bọc `BaseResponse<T>`, list dùng `PageResponse<T>` đúng theo pattern repo hiện tại.

### C.5. Luồng xử lý giữa các class

#### Export sync

```text
FE -> ReportController
   -> ReportService
   -> RequestSizeEstimator
   -> ReportDataProvider
   -> ExportStrategy
   -> HTTP stream response
```

#### Export async

```text
FE -> ReportController
   -> ReportService
   -> RequestSizeEstimator
   -> JobService.create()
   -> RabbitMQ publish
   -> return jobId

Worker -> ReportExportProcessor
       -> JobStatusUpdater
       -> ReportDataProvider
       -> ExportStrategy
       -> StorageService.uploadGeneratedFile(...)
       -> JobService.complete(...)
       -> NotificationService.notifyCompletion(...)
```

#### Import async

```text
FE -> ImportController
   -> upload file + request metadata
   -> JobService.create()
   -> StorageService.upload original file (optional but recommended)
   -> RabbitMQ publish
   -> return jobId

Worker -> ImportProcessor
       -> validate header + schema
       -> choose parser strategy
       -> read rows in chunks
       -> validate business rules
       -> persist batch-by-batch
       -> generate error file if any
       -> upload error file to MinIO
       -> update job summary
```

### C.6. Strategy Pattern: nên dùng ở đâu

#### 1. Export format

```java
public interface ExportStrategy {
    ExportFormat supports();
    void export(ExportContext context, RowStreamSupplier rows, OutputStream outputStream);
}
```

Áp dụng cho:

- `CsvExportStrategy`
- `ExcelExportStrategy`
- `PdfExportStrategy`

Lý do:

- Logic output format khác nhau rõ rệt.
- Dễ thêm format mới mà không đụng pipeline chính.
- Tránh `if/else` dài trong service.

#### 2. Import parser

```java
public interface ImportParserStrategy {
    boolean supports(String fileExtension);
    Stream<RawImportRow> parse(InputStream inputStream);
}
```

Áp dụng cho:

- `CsvImportParserStrategy`
- `XlsxImportParserStrategy`

Lý do:

- Parser CSV/XLSX khác hẳn về thư viện và error handling.
- Dễ giữ pipeline import thống nhất.

#### 3. Report type

`ReportDataProvider` là hợp lý nếu số report khác nhau về query shape:

- current stock
- low stock
- expiring batches
- movement
- valuation
- traceability

Không nên generic hóa quá mức thành một "universal report engine". Report type strategy chỉ nên trả:

- metadata/report title
- projection/query builder
- row mapper
- allowed formats

### C.7. Template Method Pattern: nên dùng ở đâu

#### Export pipeline

```java
public abstract class AbstractExportProcessor<RQ> {

    public ExportResult execute(RQ request, JobContext jobContext) {
        validateRequest(request);
        ExportExecutionPlan plan = buildPlan(request);
        StreamableRows rows = fetchRows(plan);
        GeneratedFile file = generateFile(plan, rows);
        StoredArtifact artifact = store(file);
        afterSuccess(plan, artifact, jobContext);
        return mapResult(artifact);
    }

    protected abstract void validateRequest(RQ request);
    protected abstract ExportExecutionPlan buildPlan(RQ request);
    protected abstract StreamableRows fetchRows(ExportExecutionPlan plan);
}
```

#### Import pipeline

```java
public abstract class AbstractImportProcessor<RQ> {

    public ImportResult execute(RQ request, JobContext jobContext) {
        validateFile(request);
        ParsedFile parsed = parse(request);
        ValidationSummary validation = validateRows(parsed);
        ImportSummary summary = persist(validation);
        GeneratedFile errorFile = generateErrorFile(summary);
        afterCompletion(summary, errorFile, jobContext);
        return mapResult(summary);
    }
}
```

Lý do dừng ở mức này:

- Đủ để chuẩn hóa flow và tái sử dụng.
- Không đẻ thêm abstraction dư thừa.
- Dễ đọc, dễ debug, dễ test theo từng bước.

### C.8. Vì sao không nên lạm dụng thêm pattern quá sớm

| Pattern / hướng tiếp cận | Vì sao chưa nên ưu tiên |
|---|---|
| Factory phức tạp | Spring DI + map enum-to-bean đã đủ cho strategy selection |
| Chain of Responsibility | Flow import/export của WMS khá cố định, không cần chain động |
| Observer/Event bus nội bộ | Tăng khó debug, khó trace, khó test transaction boundary |
| Quá nhiều abstraction | Code đọc khó hơn lợi ích nhận được, đặc biệt với team delivery-focused |

Thực tế nên giữ kiến trúc như sau:

- Controller mỏng.
- Service điều phối.
- Processor quản lý pipeline.
- Strategy xử lý format/parser/report type.
- RepositoryCustom tối ưu query.

### C.9. Query/repository layer

Đây là phần quan trọng nhất của thiết kế.

Khuyến nghị:

1. Report query nên dùng `projection DTO` hoặc `interface-based projection`.
2. Query nặng nên đi qua `RepositoryCustom` với `native query` hoặc `JdbcTemplate`, không load full entity.
3. Chỉ dùng entity/JPA relation khi dataset nhỏ và CRUD-style.
4. Dashboard aggregate nên tính ở DB.
5. Import validation nên prefetch master/reference data theo batch thay vì query từng row.

Ví dụ:

- `CurrentStockRowProjection`
- `StockMovementRowProjection`
- `LowStockRowProjection`
- `BatchTraceabilityRowProjection`

Không nên:

- `findAll()` rồi map bằng Java stream cho report lớn
- join entity graph lớn chỉ để lấy vài cột
- gọi repository từng row trong import loop

### C.10. Query optimization strategy

#### Indexing

Các index nên bám vào filter/join thực tế của WMS:

- `stock(warehouse_id, product_id, location_id)`
- `stock(batch_id)`
- `stock(product_id, warehouse_id)`
- `batch(product_id, expiry_date)`
- `batch(batch_number)`
- `stock_movement(warehouse_id, movement_date)`
- `stock_movement(product_id, movement_date)`
- `stock_movement(batch_id, movement_date)`
- `stock_movement(reference_type, reference_id)`
- `background_jobs(requested_by, created_at desc)`

Nguyên tắc:

- index theo `where + join + order by`
- tránh index trùng lặp
- validate bằng execution plan với query thật

#### Projection thay vì full entity

Đây là rule mặc định cho reporting:

- projection DTO cho report rows
- aggregate DTO cho dashboard cards
- lightweight summary DTO cho job list

#### Pagination cho sync case

Preview API và report screen nên luôn paginated.

- page size gợi ý: `20/50/100`
- export sync chỉ áp dụng khi tổng rows còn nhỏ
- nếu user request export full dataset nhưng dataset lớn, tự động chuyển async

#### Batch/chunk cho async

- export: fetch theo chunk `1,000 - 5,000` rows tùy report
- import: parse/validate/persist theo chunk `100 - 500` rows
- mỗi chunk ghi progress

#### Tránh N+1

- prefetch master data theo tập khóa
- dùng join có kiểm soát
- dùng batch lookup map trong import

#### Aggregate ở DB

Dashboard và report summary phải tính ở DB:

- `SUM`, `COUNT`, `GROUP BY`
- window function nếu DB hỗ trợ và cần
- không kéo raw rows lên Java rồi aggregate

#### Stream thay vì load full RAM

Áp dụng khi:

- export CSV/XLSX lớn
- đọc query qua cursor/fetch size
- parse file lớn

Gợi ý kỹ thuật:

- MySQL: cursor/fetch size phù hợp thay vì load full result
- PostgreSQL: fetch size và streaming result set
- CSV: `BufferedWriter`
- XLSX: `SXSSFWorkbook`

### C.11. Cache strategy

Cache nên phục vụ 3 mục tiêu rõ ràng: giảm latency, giảm query lặp, giảm compile/template cost.

| Loại cache | Nên cache gì | TTL gợi ý | Invalidation |
|---|---|---|---|
| Template cache | compiled Jasper template, template metadata | dài, theo deploy/version | clear khi deploy/template đổi |
| Reference/master data cache | UOM, category, warehouse, location metadata | `10 - 30 phút` | evict khi write |
| Dashboard aggregate cache | card tổng hợp, KPI nhỏ | `30 - 120 giây` | TTL ngắn, optional evict khi stock mutation |
| Report result cache | chỉ cho query summary nhỏ, lặp nhiều | `30 - 60 giây` | TTL ngắn + key theo filter |

#### Nên cache

- Jasper compiled template
- danh mục tham chiếu thay đổi ít
- dashboard numbers được gọi liên tục

#### Không nên cache

- raw report rows lớn
- batch traceability detail dài
- stock valuation chi tiết theo thời điểm lâu
- import validation results lớn
- file download presigned URL quá lâu

#### Template cache

Đây là cache nên có gần như chắc chắn nếu dùng JasperReports.

Khuyến nghị:

- cache local in-memory theo `templateName + version`
- không cần Redis cho object compiled của Jasper
- invalidation theo deploy hoặc checksum template

#### Reference/master data cache

Nên dùng chọn lọc:

- category
- UOM
- warehouse metadata
- location metadata

Không cache tràn lan mọi bảng master nếu write frequency cao.

#### Report result/data cache

Chỉ dùng khi:

- query thực sự được gọi lặp với cùng filter
- dữ liệu chấp nhận stale ngắn
- kết quả nhỏ

Ví dụ hợp lý:

- dashboard low stock count
- dashboard total inbound today

Ví dụ không hợp lý:

- export movement 200k rows
- traceability details per batch

### C.12. StorageService cần mở rộng thực dụng

Repo hiện có `StorageService` chủ yếu phục vụ `MultipartFile`. Với reporting/import-export background jobs, cần thêm API upload generated file:

```java
FileUploadResponse uploadStream(
    InputStream inputStream,
    long contentLength,
    String objectName,
    String contentType
);

FileUploadResponse uploadFile(
    Path path,
    String objectName,
    String contentType
);
```

Lý do:

- export async tạo file nội bộ, không phải `MultipartFile`
- import async có thể cần upload error file hoặc source file
- tránh hack convert file nội bộ thành multipart giả

### C.13. JobStatusUpdater

Nên có một service riêng cho update state để tránh logic rải rác:

```java
jobStatusUpdater.update(
    jobId,
    BackgroundJobStatus.PROCESSING,
    "Loading stock movement rows",
    42,
    processedItems,
    totalItems
);
```

Khuyến nghị:

- update bằng transaction ngắn, độc lập
- validate state transition hợp lệ
- append `job_step_log`
- thêm `error_code`, `error_message` khi fail

Điều này giúp:

- FE thấy progress thật
- logs thống nhất
- dễ metrics/alerting

### C.14. Notification integration

Không cần xây một notification framework mới ở phase đầu.

Nên tận dụng:

- `EmailServiceImpl`
- `EmailProducerService`
- queue email hiện có

Khi job `COMPLETED` hoặc `FAILED`, service chỉ cần:

1. tạo payload mail
2. gọi `sendEmailAsync(...)`

Với đơn urgent hoặc KPI quan trọng:

- phase đầu: dùng scheduled scan hoặc rule-based trigger đơn giản
- chưa cần event bus nội bộ phức tạp

### C.15. Scheduled report

Flow chuẩn:

```text
Scheduler tick
-> load active schedules due now
-> create background job per schedule
-> publish RabbitMQ
-> worker generate file
-> upload MinIO
-> email user link download
```

Không nên:

- generate report trực tiếp trong `@Scheduled`
- loop từng schedule và chờ hoàn tất

### C.16. Thiết kế import theo mức độ an toàn dữ liệu

WMS có rủi ro integrity cao, nên import cần phân loại:

| Loại import | Khuyến nghị |
|---|---|
| Master data đơn giản | có thể row-level continue với error file |
| Inventory-affecting import | validate chặt, chunk transaction có kiểm soát hoặc staging-first |
| Cross-warehouse/batch-sensitive import | mặc định async, không sync |

Nguyên tắc:

- không commit một transaction khổng lồ cho file lớn
- không gọi DB cho từng row nếu có thể prefetch
- row lỗi phải tổng hợp được thành error report dễ tải

---

## D. Thiết Kế Frontend / UX

### D.1. Trải nghiệm chuẩn cho async jobs

FE nên có ít nhất 3 màn hình hoặc component:

1. `Job list` của user.
2. `Job detail` với progress và step log.
3. `Import/export result panel` ngay sau khi submit job.

### D.2. Thông tin nên hiển thị

| Trường | Mục đích UX |
|---|---|
| `job_id` | tra cứu và support |
| `job_type` | user biết đây là import hay report |
| `business_type` | low stock, current stock, product import... |
| `status` | trạng thái tổng |
| `current_step` | user hiểu hệ thống đang làm gì |
| `progress_percent` | tránh spinner vô nghĩa |
| `processed_items / total_items` | tiến độ định lượng |
| `created_at`, `started_at`, `finished_at` | kỳ vọng thời gian |
| `error_message` | biết lỗi và xử lý |
| `download_url` | tải file khi xong |
| `retryable` | cho phép retry |

### D.3. Hành vi UX đề xuất

#### Khi submit async export/import

- trả `job_id` ngay
- hiển thị toast: `Job đã được tạo`
- chuyển user sang job detail hoặc mở side panel theo dõi

#### Khi job đang chạy

- progress bar
- current step
- processed rows
- estimated completion nếu đủ dữ liệu

#### Khi job fail

- hiển thị `error_code`
- message business-friendly
- link tải `error file` nếu có
- nút `Retry` nếu retryable

#### Khi job complete

- nút `Download`
- trạng thái `Completed`
- file expiry note nếu link ngắn hạn

### D.4. Polling vs SSE vs WebSocket

| Cơ chế | Khi dùng | Ưu điểm | Nhược điểm |
|---|---|---|---|
| Polling | phase đầu | nhanh triển khai, dễ debug | tốn request lặp |
| SSE | dashboard/job detail active | push một chiều, nhẹ hơn websocket | reconnect handling |
| WebSocket | hệ thống đã có live notification rộng | realtime tốt nhất | khó vận hành hơn |

Recommendation:

- job list: polling
- job detail đang mở: polling trước, SSE sau
- không cần WebSocket riêng chỉ cho reporting/import-export

### D.5. UX cho dashboard

Dashboard nên chia 2 loại widget:

1. `Fast widgets`: sync + cache ngắn.
2. `Heavy widgets`: lazy load hoặc snapshot-based.

UX rule:

- card số tổng hợp trả nhanh
- chart nặng load sau
- nếu data đang refresh, hiển thị `last updated at`

### D.6. UX cho import

Nên có:

- template file tải sẵn
- validate-only preview
- row error summary
- downloadable error CSV/XLSX

Điều này giảm rất mạnh ticket support khi user upload file sai format.

---

## E. Performance Strategy

### E.1. Immediate wins

Đây là các việc nên làm đầu tiên vì hiệu quả cao, rủi ro thấp:

1. Tối ưu query và index theo use case thật.
2. Dùng projection/native query/JDBC cho reporting.
3. Thêm split `sync vs async` rõ ràng.
4. Thêm job tracking + MinIO artifact storage.
5. Thêm template cache cho JasperReports.
6. Dùng chunked processing cho export/import async.
7. Không cho detailed PDF report chạy sync ở dataset lớn.

### E.2. Medium-term improvements

1. Per-cache TTL thay vì global Redis TTL chung.
2. Thêm request deduplication cho async jobs:
   - cùng user
   - cùng filter hash
   - cùng format
   - đang `PENDING/PROCESSING` hoặc vừa `COMPLETED` trong cửa sổ ngắn
3. SSE cho active job detail.
4. Staging table cho import lớn hoặc inventory-sensitive imports.
5. DB-level summary tables cho dashboard nặng.

### E.3. Advanced scaling options

Chỉ dùng khi thực sự có pressure thật:

1. `Read replica` cho reporting-heavy read.
2. `Snapshot / reporting table`:
   - `stock_daily_snapshot`
   - `stock_movement_daily_agg`
   - `dashboard_kpi_snapshot`
3. `Read model` riêng cho analytics/reporting nếu query OLTP không còn đáp ứng.
4. Tách worker service nếu background load trở thành bottleneck thật.

Không nên nhảy vào read replica hoặc read model quá sớm khi query/index hiện tại chưa tune đúng.

### E.4. Stored procedure: dùng khi nào, không dùng khi nào

#### Kết luận

Stored procedure nên là `selective optimization tool`, không phải xương sống kiến trúc reporting.

#### Nên dùng khi

- query/report rất nặng nhưng ổn định
- logic aggregate lặp lại nhiều
- cần execution plan tối ưu ở DB
- cần pagination/aggregation có kiểm soát tốt hơn native query thường
- ví dụ:
  - stock valuation as-of-date
  - movement summary theo kỳ
  - expiring batches aggregate

#### Không nên dùng khi

- logic report đổi thường xuyên
- business rules cần evolve nhanh theo sprint
- report chỉ là projection đơn giản
- muốn giữ portability giữa MySQL/PostgreSQL cao
- team khó maintain SQL procedural code

#### Ưu điểm

- có thể tối ưu DB execution tốt
- giảm round-trip và một số xử lý trung gian
- phù hợp với aggregate/report ổn định

#### Nhược điểm

- vendor-specific hơn
- khó test hơn trong H2
- tăng độ phức tạp migration/versioning
- business logic bị kéo xuống DB nếu lạm dụng

#### Cách kết hợp với Spring Boot

- giữ business orchestration ở `service`
- procedure gọi từ `RepositoryCustom` hoặc `JdbcTemplate` / `SimpleJdbcCall`
- output map vào projection DTO
- version procedure bằng Flyway

Pragmatic rule:

- bắt đầu bằng optimized SQL/native query
- chỉ chuyển sang procedure khi có profiling chứng minh giá trị

### E.5. Đa luồng / concurrency

#### Kết luận

Không nên lấy multi-thread trong app làm tối ưu đầu tiên. Ưu tiên:

1. query optimization
2. chunking/streaming
3. queue consumer concurrency

#### Khi đa luồng hữu ích

- nhiều job độc lập cần chạy song song
- mỗi worker xử lý job riêng
- có CPU-bound formatting hoặc file generation rõ ràng

#### Khi không nên dùng

- DB đang là bottleneck
- mỗi job đã nặng về RAM/I/O
- thư viện export không thread-safe rõ ràng
- connection pool nhỏ

#### So sánh các hướng

| Hướng | Khi phù hợp | Ưu điểm | Rủi ro |
|---|---|---|---|
| Parallel processing trong app | bước CPU-bound, dữ liệu độc lập | tăng tốc cục bộ | khó thread-safe, tăng RAM |
| Queue consumer concurrency | nhiều job độc lập | dễ scale, dễ kiểm soát | tăng áp lực DB nếu cấu hình sai |
| Streaming/chunking | dataset lớn | ổn định RAM, đơn giản hơn | cần thiết kế progress/transaction tốt |

Recommendation:

- ưu tiên `queue consumer concurrency` ở mức thấp và có kiểm soát
- ưu tiên `streaming/chunking` hơn `parallelStream()`
- tránh `parallelStream()` tràn lan trong service/report code

#### Những thứ phải nhìn trước khi tăng concurrency

- `Hikari maximum-pool-size`
- query latency
- memory footprint của Jasper/POI
- MinIO upload throughput
- transaction duration

### E.6. Observability và operations

Reporting/import-export là vùng rất cần observability vì lỗi thường đến từ dữ liệu lớn, timeout và file IO.

#### Metrics nên có

- `wms_job_created_total`
- `wms_job_completed_total`
- `wms_job_failed_total`
- `wms_job_duration_seconds`
- `wms_job_queue_wait_seconds`
- `wms_export_rows_total`
- `wms_import_rows_total`
- `wms_import_failed_rows_total`
- `wms_storage_upload_duration_seconds`
- `wms_report_query_duration_seconds`
- queue depth và DLQ depth

#### Structured logging

Log nên luôn có:

- `jobId`
- `jobType`
- `businessType`
- `requestedBy`
- `status`
- `step`

#### Tracing / correlation

- dùng `traceId` hoặc `correlationId`
- propagate từ request -> job creation -> queue message -> worker -> notification

#### Alerting

Alert tối thiểu:

- job failure rate tăng bất thường
- queue backlog kéo dài
- DLQ có message
- MinIO upload fail
- query latency cao đột biến

---

## F. Trade-offs và Anti-Patterns

### F.1. Sai lầm cần tránh

| Anti-pattern | Vì sao nguy hiểm |
|---|---|
| Async cho mọi case | tăng độ phức tạp, UX chậm hơn không cần thiết |
| Sync cho report lớn | timeout, crash RAM, request treo |
| Áp dụng quá nhiều pattern | code khó đọc hơn lợi ích mang lại |
| Cache bừa bãi | stale data, invalidation khó, debug khó |
| Stored procedure hóa toàn bộ | khóa chặt DB vendor, tăng maintenance |
| `parallelStream()` khắp nơi | khó kiểm soát DB, RAM, thread-safety |
| Load full dataset vào RAM | rủi ro OOM và GC pressure |
| FE chỉ có spinner | UX tệ, khó support |
| Generate file trực tiếp trong scheduler | block scheduler thread, khó retry |
| Dùng entity graph cho report lớn | memory và N+1 dễ bùng |

### F.2. Trade-off kỹ thuật quan trọng

| Quyết định | Lợi ích | Trade-off |
|---|---|---|
| Generic `background_jobs` table | ít bảng hơn, tái sử dụng cao | payload JSON cần discipline |
| Polling phase đầu | nhanh delivery | nhiều request hơn SSE |
| Strategy + Template Method | vừa đủ mở rộng | thêm một ít boilerplate hợp lý |
| MinIO artifact storage | download ổn định, tách app server | cần cleanup retention |
| Short TTL result cache | giảm tải dashboard | chấp nhận stale ngắn |
| Selective stored procedure | tối ưu report nặng | tăng complexity test/migration |

### F.3. Những điểm cần giữ thực dụng

1. Tối ưu query trước khi thêm worker/concurrency.
2. Chỉ thêm cache sau khi biết rõ key, TTL, invalidation.
3. Chỉ thêm procedure sau profiling.
4. Chỉ thêm read replica/read model khi primary DB thực sự quá tải.
5. Chỉ thêm WebSocket nếu bài toán realtime rộng hơn job progress.

---

## G. Recommendation Cuối Cùng

### Hướng đi tốt nhất hiện tại

Cho WMS backend này, hướng nên chọn là:

1. `Chia 2 lane` rõ ràng:
   - sync cho preview, dashboard nhanh, export nhỏ
   - async cho report/export/import lớn, scheduled report, email report
2. `Dùng RabbitMQ + background job tracking + MinIO` làm xương sống cho lane async.
3. `Dùng Strategy + Template Method` ở mức vừa đủ:
   - strategy cho format và parser
   - template method cho pipeline
4. `Lấy query optimization làm trung tâm`:
   - projection
   - native/JDBC cho report nặng
   - index đúng
   - chunk/stream
5. `Cache có chọn lọc`:
   - template cache gần như bắt buộc
   - reference cache chọn lọc
   - result cache TTL ngắn cho dashboard/small summaries
6. `FE phải thấy progress rõ ràng`, không chỉ spinner.
7. `Stored procedure` chỉ dùng chọn lọc.
8. `Concurrency` ưu tiên ở mức worker/job, không song song hóa vô tội vạ trong từng step.

### Hướng nâng cấp trong tương lai

Khi volume tăng cao hơn:

1. snapshot table cho dashboard/report summary
2. selective stored procedure cho query thật sự nặng
3. SSE cho live progress
4. read replica cho heavy read
5. reporting read model nếu nhu cầu analytics mở rộng đáng kể

---

## H. Roadmap Triển Khai

### Phase 1: Foundation

Mục tiêu: có lane sync/async rõ ràng, job tracking rõ, không over-engineering.

Deliverables:

1. `background_jobs` + `background_job_step_logs`
2. `JobController`
3. `ReportController` và `ImportController` có split sync/async
4. `Strategy` cho export format và import parser
5. `AbstractExportProcessor` + `AbstractImportProcessor`
6. RabbitMQ queues cho report/import + DLQ
7. MinIO upload cho generated files
8. FE polling job detail/list

### Phase 2: Performance hardening

Mục tiêu: giảm latency và tăng độ ổn định.

Deliverables:

1. index tuning cho report queries
2. `RepositoryCustom` + projection/native/JDBC
3. template cache Jasper
4. reference cache có chọn lọc
5. chunked export/import
6. dashboard cache TTL ngắn
7. request deduplication cho async jobs

### Phase 3: Scale-up có chọn lọc

Mục tiêu: chỉ tối ưu nâng cao ở nơi đã có pain thật.

Deliverables:

1. selective stored procedure cho report nặng, ổn định
2. streaming cải tiến cho file rất lớn
3. SSE cho active job tracking
4. staging table cho import lớn / inventory-sensitive
5. snapshot/reporting table cho dashboard nặng
6. read replica nếu DB read pressure tăng thật

### Phase 4: Advanced reporting nếu business đòi hỏi

Chỉ thực hiện khi volume và nhu cầu đã chứng minh:

1. reporting read model riêng
2. worker service scale độc lập
3. advanced scheduling / recurrence / distribution policies
4. retention, archival, governance cho generated artifacts

---

## Appendix A. Quy ước kỹ thuật nên chốt sớm

### A1. Naming

- Queue: `wms.jobs.{domain}.queue`
- DLQ: `wms.jobs.{domain}.dlq`
- Object key: `{domain}/{yyyy}/{MM}/{jobId}/{file}`
- Job type: enum rõ ràng, không dùng string tự do

### A2. Download security

- không public bucket
- link download là presigned URL ngắn hạn
- backend check ownership trước khi trả link

### A3. File format guideline

- `CSV`: large export mặc định
- `XLSX`: business-friendly export trung bình
- `PDF`: chỉ cho summary / presentation-grade reports

### A4. Library guideline

- JasperReports: continue dùng cho PDF/template report
- Apache POI: thêm cho XLSX export/import
- CSV parser: dùng thư viện nhẹ, đừng viết parser thủ công

---

## Final Conclusion

Kiến trúc phù hợp nhất cho tầng reporting/import-export của WMS này là:

- chia `sync lane` cho case nhỏ và `async lane` cho case lớn
- dùng `RabbitMQ + job tracking + MinIO` cho xử lý nền
- dùng `Strategy + Template Method` vừa đủ
- đặt `query optimization` ở trung tâm
- dùng `template cache + selective data cache`
- đảm bảo FE có trải nghiệm theo dõi tiến trình rõ ràng
- dùng `stored procedure` có chọn lọc
- dùng concurrency có kiểm soát, ưu tiên ở mức worker/job

Đây là hướng cân bằng nhất giữa delivery speed, maintainability, performance và khả năng scale trong bối cảnh codebase Spring Boot hiện tại.
