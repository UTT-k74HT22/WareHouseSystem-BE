# Reporting / Import-Export Implementation Guide

> Updated: 2026-03-22  
> Scope: implementation-level guide for the current WMS codebase  
> Base package: `org.demo.whs`

---

## 1. Mục tiêu tài liệu

Tài liệu này không thay thế bản technical design tổng thể ở `docs/10-reporting-import-export-technical-design.md`.

Mục tiêu của tài liệu này là:

1. Chỉ rõ mức độ triển khai hiện tại của module reporting / import-export.
2. Giải thích chi tiết pattern đang được áp dụng trong codebase.
3. Giải thích rõ luồng RabbitMQ cho background jobs.
4. Cho team một bản đồ triển khai thực dụng: đang có gì, thiếu gì, làm tiếp theo thứ tự nào.
5. Cung cấp sơ đồ và wireframe để nhìn hệ thống nhanh, không phải đọc code từ đầu.

---

## 2. Trạng thái hiện tại

### 2.1. Đã có trong codebase

#### Job tracking core

- `BackgroundJob`
- `BackgroundJobStepLog`
- `BackgroundJobStatus`
- `BackgroundJobType`
- job DTOs / mapper
- `JobController`
- `BackgroundJobService`
- `BackgroundJobServiceImpl`
- `JobStatusUpdater`
- `JobStatusUpdaterImpl`

#### Strategy + Template Method skeleton

- export strategy:
  - `ExportStrategy`
  - `CsvExportStrategy`
  - `ExcelExportStrategy`
  - `PdfExportStrategy`
- report provider strategy:
  - `ReportDataProvider`
  - `CurrentStockReportProvider`
  - `LowStockReportProvider`
  - `ExpiringBatchesReportProvider`
  - `StockMovementReportProvider`
  - `StockValuationReportProvider`
  - `BatchTraceabilityReportProvider`
- import parser strategy:
  - `ImportParserStrategy`
  - `CsvImportParserStrategy`
  - `XlsxImportParserStrategy`
- template pipeline:
  - `AbstractExportProcessor`
  - `ReportExportProcessor`
  - `AbstractImportProcessor`

#### RabbitMQ background job scaffold

- `BackgroundJobProperties`
- `RabbitMQBackgroundJobConfig`
- `BackgroundJobProducerService`
- `BackgroundJobConsumerService`
- `BackgroundJobMessageDTO`

#### File handling

- `StorageService`
- `StorageServiceImpl`
- upload generated file by `byte[]`
- MinIO presigned URL support for result download

### 2.2. Chưa triển khai business logic thật

- query thật cho từng loại report
- export Excel thật
- export PDF thật
- import parse CSV/XLSX thật
- endpoint tạo report job từ business module
- scheduled report creation flow
- notification flow khi job hoàn tất / thất bại
- retry logic có điều kiện theo loại job cụ thể

### 2.3. Mức độ triển khai tổng quan

| Hạng mục | Trạng thái | Ghi chú |
|---|---|---|
| Job entity + migration | Done | dùng được |
| Job APIs cơ bản | Done | list/detail/status/retry/cancel/download |
| Job state machine | Done | explicit transition qua `JobStatusUpdater` |
| Queue wiring | Done | producer/consumer scaffold |
| Export CSV kỹ thuật | Partial | chạy được ở mức generic |
| Export XLSX/PDF | Scaffold only | chưa có implementation |
| Report query providers | Scaffold only | chưa có query |
| Import parser | Scaffold only | chưa parse file |
| FE UX contract | Ready | DTO đủ cho polling/status/detail |

---

## 3. Bức tranh tổng thể triển khai

### 3.1. Kiến trúc runtime

```text
+--------------------+        +------------------------+
| Frontend Web App   |        | Backend REST API       |
|--------------------|        |------------------------|
| Export button      |------->| JobController          |
| Job list           |<-------| BackgroundJobService   |
| Job detail         |<-------| BackgroundJobMapper    |
| Polling status     |<-------| StorageService         |
+--------------------+        +-----------+------------+
                                           |
                                           | create/retry async job
                                           v
                               +-----------+------------+
                               | background_jobs table  |
                               | step_logs table        |
                               +-----------+------------+
                                           |
                                           | after commit
                                           v
                               +-----------+------------+
                               | RabbitMQ               |
                               |------------------------|
                               | background job queue   |
                               | routing by job type    |
                               +-----------+------------+
                                           |
                                           v
                               +-----------+------------+
                               | Consumer              |
                               |-----------------------|
                               | resolve processor     |
                               | execute pipeline      |
                               +-----------+-----------+
                                           |
                           +---------------+------------------+
                           |                                  |
                           v                                  v
                +----------+-----------+           +----------+-----------+
                | ReportDataProvider   |           | ExportStrategy       |
                | load dataset         |           | CSV / XLSX / PDF     |
                +----------+-----------+           +----------+-----------+
                           |                                  |
                           +---------------+------------------+
                                           |
                                           v
                               +-----------+------------+
                               | MinIO                  |
                               | generated artifact     |
                               +-----------+------------+
                                           |
                                           v
                               +-----------+------------+
                               | Job status updated     |
                               | download link exposed  |
                               +------------------------+
```

### 3.2. Hai lane sync / async

```text
Small request
  -> controller
  -> service
  -> optimized query
  -> return immediately

Large request
  -> controller
  -> create BackgroundJob
  -> send RabbitMQ message
  -> worker process in background
  -> update progress
  -> upload file
  -> FE polls status and downloads result
```

---

## 4. Wireframe để hiểu nhanh

### 4.1. FE wireframe: danh sách job

```text
+--------------------------------------------------------------------------------+
| Reporting Jobs                                                                 |
+--------------------------------------------------------------------------------+
| Filters: [Status v] [Job Type v] [Date Range] [Search Job Code] [Refresh]     |
+--------------------------------------------------------------------------------+
| Job Code      | Type          | Step             | Progress | Status   | Action|
|---------------+---------------+------------------+----------+----------+-------|
| JOB-240322001 | REPORT_EXPORT | GENERATING_FILE  | 78%      | Running  | View  |
| JOB-240322002 | REPORT_EXPORT | COMPLETED        | 100%     | Done     | DL    |
| JOB-240322003 | DATA_IMPORT   | FAILED           | 42%      | Failed   | Retry |
+--------------------------------------------------------------------------------+
| Legend:                                                                    |
| - Running = PENDING / VALIDATING / PROCESSING / GENERATING_FILE / UPLOADING |
| - Done = COMPLETED                                                         |
| - Failed = FAILED / CANCELLED                                              |
+--------------------------------------------------------------------------------+
```

### 4.2. FE wireframe: job detail drawer/modal

```text
+------------------------------------------------------------------+
| Job Detail: JOB-240322001                                        |
+------------------------------------------------------------------+
| Type            : REPORT_EXPORT                                  |
| Business Type   : CURRENT_STOCK                                  |
| Status          : GENERATING_FILE                                |
| Current Step    : GENERATING_FILE                                |
| Progress        : [#####################-----] 78%               |
| Requested By    : 6cb4...                                        |
| Created At      : 2026-03-22 10:15:12                            |
| Started At      : 2026-03-22 10:15:18                            |
| Finished At     : -                                              |
| Error           : -                                              |
+------------------------------------------------------------------+
| Step Timeline                                                    |
| 10:15:18  VALIDATING       Validating export job payload         |
| 10:15:19  PROCESSING       Loading report data                   |
| 10:15:25  PROCESSING       Report data prepared                  |
| 10:15:27  GENERATING_FILE  Generating export file                |
+------------------------------------------------------------------+
| [Cancel] [Retry disabled] [Download disabled]                    |
+------------------------------------------------------------------+
```

### 4.3. FE wireframe: completed state

```text
+------------------------------------------------------------------+
| Job Detail: JOB-240322002                                        |
+------------------------------------------------------------------+
| Status          : COMPLETED                                      |
| Progress        : [##############################] 100%          |
| File            : current_stock_20260322_101512.csv             |
| File Size       : 3.2 MB                                         |
| Download        : [Download File]                                |
+------------------------------------------------------------------+
| Step Timeline                                                    |
| VALIDATING -> PROCESSING -> GENERATING_FILE -> UPLOADING -> DONE |
+------------------------------------------------------------------+
```

---

## 5. Pattern đang áp dụng như thế nào

Phần này là cốt lõi. Mục tiêu là dùng pattern vừa đủ để:

- không nhồi hết logic vào service/controller
- không over-engineer bằng factory/event bus/abstraction thừa
- tách đúng điểm biến thiên

### 5.1. Pattern số 1: Strategy Pattern

#### 5.1.1. Strategy áp dụng ở đâu

Pattern này được dùng cho các điểm có nhiều biến thể, nhưng cùng một contract.

Trong reporting/import-export hiện tại có 3 điểm biến thiên rõ nhất:

1. `Export format`
2. `Import parser`
3. `Report data provider`

#### 5.1.2. ExportStrategy

Contract:

```text
ExportStrategy
  - getFormat()
  - supports(format)
  - export(payload)
```

Ý nghĩa:

- cùng một dataset
- nhưng output có thể là `CSV`, `XLSX`, `PDF`
- mỗi format có logic generate file riêng

Triển khai:

```text
ExportStrategy
 ├── CsvExportStrategy
 ├── ExcelExportStrategy
 └── PdfExportStrategy
```

Luồng hoạt động:

```text
ReportExportProcessor
  -> đọc payload
  -> biết format = CSV
  -> tìm strategy supports(CSV)
  -> gọi strategy.export(payload)
  -> nhận GeneratedExportFile
```

Vì sao đây là đúng chỗ dùng Strategy:

- format là biến thể rõ ràng
- mỗi format có behavior khác nhau
- controller/service không nên if/else dài theo format

Vì sao chưa cần factory phức tạp:

- Spring đã inject `List<ExportStrategy>`
- chỉ cần filter theo `supports(format)`
- chưa có nhu cầu plugin system hay dynamic loading

#### 5.1.3. ReportDataProvider

Contract:

```text
ReportDataProvider
  - getReportType()
  - supports(reportType)
  - loadDataSet(job, payload)
```

Ý nghĩa:

- mỗi report type có query/data shaping khác nhau
- `CURRENT_STOCK` khác `STOCK_VALUATION`
- `BATCH_TRACEABILITY` khác hoàn toàn `LOW_STOCK`

Triển khai:

```text
ReportDataProvider
 ├── CurrentStockReportProvider
 ├── LowStockReportProvider
 ├── ExpiringBatchesReportProvider
 ├── StockMovementReportProvider
 ├── StockValuationReportProvider
 └── BatchTraceabilityReportProvider
```

Luồng hoạt động:

```text
ReportExportProcessor
  -> đọc payload.reportType
  -> chọn provider phù hợp
  -> provider load dataset
  -> trả về ReportDataSet
  -> chuyển sang ExportStrategy
```

Điểm lợi:

- query logic của từng report nằm riêng
- dễ tối ưu query theo từng case
- không làm một service report khổng lồ

#### 5.1.4. ImportParserStrategy

Contract:

```text
ImportParserStrategy
  - getFileType()
  - supports(fileType)
  - parse(inputStream)
```

Ý nghĩa:

- parser CSV khác parser XLSX
- nhưng cả hai cùng trả một normalized result: `ImportParseResult`

Triển khai:

```text
ImportParserStrategy
 ├── CsvImportParserStrategy
 └── XlsxImportParserStrategy
```

### 5.2. Pattern số 2: Template Method

Pattern này được dùng để cố định pipeline chuẩn, nhưng cho phép subclass/custom implementation thay đổi từng bước.

#### 5.2.1. Export pipeline

Base class:

```text
AbstractExportProcessor
  process(job):
    1. start validation
    2. validateJob(job)
    3. start processing
    4. loadDataSet(job)
    5. update progress
    6. mark generating file
    7. generateFile(job, dataSet)
    8. mark uploading
    9. upload to MinIO
   10. mark completed
   11. on error -> mark failed
```

Đây là pipeline chuẩn mà mọi export job đều nên đi theo.

Subclass hiện tại:

```text
ReportExportProcessor
  - validateJob()
  - loadDataSet()
  - generateFile()
  - resolveExportFolder()
```

Ý nghĩa:

- pipeline lifecycle được chuẩn hóa
- logging, progress, error handling, upload, completion không bị copy-paste
- business logic chỉ cần tập trung vào dataset + format selection

#### 5.2.2. Import pipeline

Base class:

```text
AbstractImportProcessor
  process(job, inputStream):
    1. validate input
    2. start processing
    3. resolve parser
    4. parse file
    5. update progress
    6. validate rows
    7. persist rows
    8. on error -> mark failed
```

Điểm lợi:

- import luôn giữ flow nhất quán
- dễ thêm chunk persist, validation summary, error file generation về sau

### 5.3. Pattern số 3: Explicit State Transition

Đây không phải GoF pattern cổ điển, nhưng là pattern nghiệp vụ rất quan trọng trong WMS.

Class:

```text
JobStatusUpdater
JobStatusUpdaterImpl
```

Vai trò:

- tập trung toàn bộ state transition của background job vào một chỗ
- không cho service/consumer/controller tự set trạng thái lung tung

Ví dụ:

```text
PENDING -> VALIDATING -> PROCESSING -> GENERATING_FILE -> UPLOADING -> COMPLETED
FAILED -> PENDING (retry)
CANCELLED -> PENDING (retry)
```

Lợi ích:

- bảo vệ invariant nghiệp vụ
- step log được tạo nhất quán
- progress được update nhất quán
- giảm bug kiểu service A set `COMPLETED` khi file chưa upload xong

### 5.4. Vì sao không dùng thêm pattern phức tạp lúc này

#### Không cần Factory phức tạp

Vì Spring DI + `List<Strategy>` đã đủ.

#### Không cần Chain of Responsibility

Flow export/import hiện là tuyến tính, không phải pipeline nhiều handler tùy điều kiện.

#### Không cần observer/event bus nội bộ

Đã có RabbitMQ cho async boundary. Thêm event bus nội bộ lúc này chỉ tăng complexity.

#### Không cần generic engine quá mức

`ReportExportProcessor` + `ReportDataProvider` + `ExportStrategy` là đủ cho phase đầu.

---

## 6. RabbitMQ áp dụng như thế nào

### 6.1. RabbitMQ giải quyết vấn đề gì

RabbitMQ ở đây không phải để “cho hiện đại”.

Nó giải quyết 4 vấn đề thực tế:

1. Tách request HTTP ra khỏi xử lý lâu.
2. Hấp thụ burst khi nhiều user cùng export/import lớn.
3. Cho phép retry / DLQ / quan sát job tốt hơn.
4. Cho phép điều chỉnh concurrency ở mức worker thay vì mở thread bừa bãi trong app.

### 6.2. Thành phần hiện tại

```text
BackgroundJobProperties
RabbitMQBackgroundJobConfig
BackgroundJobProducerService
BackgroundJobConsumerService
BackgroundJobMessageDTO
```

### 6.3. Luồng RabbitMQ end-to-end

```text
[1] User bấm Export lớn
    |
    v
[2] Backend tạo BackgroundJob(status=PENDING)
    |
    v
[3] Transaction commit xong
    |
    v
[4] BackgroundJobProducerService gửi message vào RabbitMQ
    |
    v
[5] BackgroundJobConsumerService nhận message
    |
    v
[6] Consumer load job từ DB
    |
    v
[7] Consumer resolve processor phù hợp
    |
    v
[8] Processor chạy pipeline
    |
    +--> update step log / progress
    +--> query data
    +--> generate file
    +--> upload MinIO
    +--> update COMPLETED / FAILED
    |
    v
[9] FE polling status endpoint
    |
    v
[10] User thấy progress hoặc link tải file
```

### 6.4. Vì sao publish sau commit

Đây là điểm rất quan trọng.

Nếu gửi RabbitMQ trước khi transaction DB commit:

- queue đã nhận message
- nhưng `background_jobs` có thể chưa commit
- consumer nhận message sớm sẽ không tìm thấy job trong DB

Hiện tại service dùng `afterCommit` để tránh race này.

```text
DB commit thành công
  -> mới send queue message
```

Đây là cách đúng.

### 6.5. Vì sao consumer phải re-load job từ DB

Message queue chỉ nên mang thông tin nhẹ:

- `jobId`
- `jobCode`
- `jobType`
- `businessType`

Consumer sau đó re-load entity từ DB vì:

- đảm bảo đọc trạng thái mới nhất
- tránh payload queue quá lớn
- giảm rủi ro queue message stale

### 6.6. Prefetch và concurrency

Khuyến nghị cho phase đầu:

- `prefetch = 1`
- `concurrentConsumers = 2`
- `maxConcurrentConsumers = 6`

Vì sao:

- report job thường nặng
- không muốn một consumer giữ quá nhiều message
- dễ kiểm soát RAM, DB connection, MinIO upload load

### 6.7. DLQ về sau nên thêm

Phase hiện tại mới có skeleton queue chính.

Nâng cấp tiếp theo nên thêm:

- dead-letter exchange
- dead-letter queue
- retry policy có backoff

Wireframe topology:

```text
                 +-----------------------------+
                 | wms.background-job.exchange |
                 +-------------+---------------+
                               |
                               v
                 +-----------------------------+
                 | wms.background-job.queue    |
                 +-------------+---------------+
                               |
                               v
                 +-----------------------------+
                 | BackgroundJobConsumer       |
                 +-------------+---------------+
                               |
                      success   |   failure
                               |
                     +---------+----------+
                     |                    |
                     v                    v
          +------------------+   +-----------------------+
          | update COMPLETED |   | background-job.dlq    |
          +------------------+   +-----------------------+
```

---

## 7. Chi tiết class interaction

### 7.1. Retry / cancel flow

```text
JobController
  -> BackgroundJobServiceImpl
      -> getOwnedJob()
      -> JobStatusUpdater.resetForRetry() or markCancelled()
      -> dispatchAfterCommit() if retry
      -> BackgroundJobMapper.toStatusResponse()
```

### 7.2. Background processing flow

```text
BackgroundJobConsumerService
  -> find job by id
  -> find matching BackgroundJobProcessor
  -> processor.process(job)

ReportExportProcessor
  -> resolve payload
  -> resolve ReportDataProvider
  -> loadDataSet()
  -> resolve ExportStrategy
  -> export()
  -> StorageService.uploadFile(byte[])
  -> JobStatusUpdater.markCompleted()
```

### 7.3. Download flow

```text
FE click download
  -> GET /api/v1/jobs/{id}/download
  -> BackgroundJobServiceImpl.getJobDownload()
  -> verify ownership
  -> StorageService.getPresignedUrl(storageObjectKey)
  -> return BackgroundJobFileResponse
```

---

## 8. Cách cắm business logic tiếp theo

### 8.1. Bước 1: làm provider đầu tiên

Khuyến nghị bắt đầu bằng:

1. `CurrentStockReportProvider`
2. `LowStockReportProvider`

Vì:

- query dễ hơn valuation / traceability
- xác nhận được pattern end-to-end sớm
- giúp FE có demo usable nhanh

Provider nên làm gì:

- nhận `job` + `payload`
- parse filter
- gọi repository custom / JDBC query projection
- build `ReportDataSet`

Provider không nên làm:

- tự upload file
- tự cập nhật job status
- tự chọn export strategy

### 8.2. Bước 2: làm API tạo report job

Nên có request DTO kiểu:

```text
CreateReportExportJobRequest
  - reportType
  - format
  - filters
  - requestedAsync
```

Service flow:

```text
validate request
estimate sync/async
if sync:
    query + export immediately
else:
    create BackgroundJob
    persist payload as JSON
    send queue after commit
    return job summary/status
```

### 8.3. Bước 3: hoàn thiện export strategy

#### CSV

- đã có khung generic
- đủ để chạy report đầu tiên

#### XLSX

Khuyến nghị:

- Apache POI SXSSF cho large export
- stream row-by-row
- tránh XSSFWorkbook cho dataset lớn

#### PDF

Khuyến nghị:

- JasperReports nếu report template phức tạp
- nếu dùng Jasper, cache compiled template

### 8.4. Bước 4: hoàn thiện import

Nên đi theo flow:

```text
upload source file
create import job
worker parse file
validate rows
persist theo chunk
generate error file nếu có
mark completed/failed
```

---

## 9. Wireframe class map

```text
Controller Layer
----------------
JobController

Service Layer
-------------
BackgroundJobService
└── BackgroundJobServiceImpl

State Control
-------------
JobStatusUpdater
└── JobStatusUpdaterImpl

Queue Layer
-----------
BackgroundJobProducerService
BackgroundJobConsumerService

Processor Layer
---------------
BackgroundJobProcessor
└── AbstractExportProcessor
    └── ReportExportProcessor

Strategy Layer
--------------
ReportDataProvider
├── CurrentStockReportProvider
├── LowStockReportProvider
├── ExpiringBatchesReportProvider
├── StockMovementReportProvider
├── StockValuationReportProvider
└── BatchTraceabilityReportProvider

ExportStrategy
├── CsvExportStrategy
├── ExcelExportStrategy
└── PdfExportStrategy

ImportParserStrategy
├── CsvImportParserStrategy
└── XlsxImportParserStrategy

Infrastructure
--------------
StorageService
RabbitMQBackgroundJobConfig
BackgroundJobProperties
```

---

## 10. Definition of Done cho phase triển khai

### Phase A: end-to-end report export đầu tiên

Done khi:

1. Có API tạo report export job.
2. `CurrentStockReportProvider` query được dữ liệu thật.
3. `CsvExportStrategy` xuất file thật.
4. Worker chạy qua RabbitMQ.
5. Job status update đầy đủ.
6. FE list/detail/download chạy được.

### Phase B: production-ready async export

Done khi:

1. Có XLSX export streaming.
2. Có notification khi complete/fail.
3. Có cleanup policy cho file cũ.
4. Có metrics/logging cho queue lag và job duration.
5. Có retry / DLQ cơ bản.

### Phase C: import và advanced reports

Done khi:

1. import parser chạy thật
2. persist theo chunk
3. error file export
4. valuation / traceability report được optimize query

---

## 11. Khuyến nghị cuối cùng

Hướng triển khai tốt nhất cho repo hiện tại là:

1. Giữ nguyên khung `Strategy + Template Method + JobStatusUpdater + RabbitMQ`.
2. Không mở rộng thêm abstraction mới ở giai đoạn này.
3. Dùng `CurrentStockReportProvider + CSV export` làm vertical slice đầu tiên.
4. Sau khi slice đầu tiên chạy ổn, mới thêm `XLSX`, `PDF`, `import parser`, `scheduled report`.

Nguyên tắc chốt:

- pattern chỉ dùng để giữ code sạch ở đúng điểm biến thiên
- job state transition phải tập trung
- RabbitMQ là boundary cho async lane
- query optimization mới là phần quyết định hiệu năng thật

---

## 12. Mapping với code hiện tại

### Core APIs

- `src/main/java/org/demo/whs/controller/JobController.java`
- `src/main/java/org/demo/whs/service/BackgroundJobService.java`
- `src/main/java/org/demo/whs/service/impl/BackgroundJobServiceImpl.java`

### Job state

- `src/main/java/org/demo/whs/service/JobStatusUpdater.java`
- `src/main/java/org/demo/whs/service/impl/JobStatusUpdaterImpl.java`

### Queue

- `src/main/java/org/demo/whs/configuration/BackgroundJobProperties.java`
- `src/main/java/org/demo/whs/configuration/RabbitMQBackgroundJobConfig.java`
- `src/main/java/org/demo/whs/helpers/producer/BackgroundJobProducerService.java`
- `src/main/java/org/demo/whs/helpers/consumer/BackgroundJobConsumerService.java`

### Export/import/report patterns

- `src/main/java/org/demo/whs/service/processor/export/AbstractExportProcessor.java`
- `src/main/java/org/demo/whs/service/processor/export/ReportExportProcessor.java`
- `src/main/java/org/demo/whs/service/processor/importing/AbstractImportProcessor.java`
- `src/main/java/org/demo/whs/utils/strategy/export/*`
- `src/main/java/org/demo/whs/utils/strategy/report/*`
- `src/main/java/org/demo/whs/utils/strategy/importing/*`

### Storage

- `src/main/java/org/demo/whs/service/StorageService.java`
- `src/main/java/org/demo/whs/service/impl/StorageServiceImpl.java`

---

## 13. Đọc tài liệu theo thứ tự

Nếu là backend dev mới vào phần này, nên đọc theo thứ tự:

1. `docs/09-report-module-overview.md`
2. `docs/10-reporting-import-export-technical-design.md`
3. tài liệu này
4. `BackgroundJobServiceImpl`
5. `JobStatusUpdaterImpl`
6. `ReportExportProcessor`
7. các `Provider` và `Strategy`

