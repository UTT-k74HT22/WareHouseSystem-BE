# Async Report Flow - Chi Tiết

> Module: WHS-68
> Updated: 2026-03-22

---

## 1. Tổng Quan Async Reports

Async reports được xử lý trong background qua RabbitMQ, phù hợp cho báo cáo lớn (> 10,000 rows) hoặc báo cáo cần thời gian xử lý lâu.

### 1.1 Async Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /api/v1/reports/async/request | Tạo yêu cầu báo cáo async |
| GET | /api/v1/reports/async/{requestId} | Kiểm tra trạng thái |
| GET | /api/v1/reports/async/{requestId}/download | Tải báo cáo |
| GET | /api/v1/reports/async/my-requests | Danh sách yêu cầu |

### 1.2 Request States

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         Request State Machine                               │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│                         ┌─────────────┐                                     │
│                         │   PENDING   │                                     │
│                         │             │                                     │
│                         └──────┬──────┘                                     │
│                                │                                            │
│                                │ Consumer picks up                          │
│                                ▼                                            │
│                         ┌─────────────┐                                     │
│                         │ PROCESSING  │                                     │
│                         │             │                                     │
│                         └──────┬──────┘                                     │
│                                │                                            │
│                    ┌───────────┴───────────┐                                │
│                    │                       │                                │
│                    ▼                       ▼                                │
│             ┌─────────────┐         ┌─────────────┐                        │
│             │  COMPLETED  │         │   FAILED    │                        │
│             │             │         │             │                        │
│             └──────┬──────┘         └──────┬──────┘                        │
│                    │                       │                                │
│                    │ Downloaded            │ Retry                          │
│                    ▼                       │                                │
│             ┌─────────────┐                │                                │
│             │  DOWNLOADED │                │                                │
│             │             │                │                                │
│             └─────────────┘                │                                │
│                    ▲                       │                                │
│                    │                       ▼                                │
│                    │                ┌─────────────┐                        │
│                    └────────────────│   PENDING   │                        │
│                      (reset)        │   (retry)   │                        │
│                                     └─────────────┘                        │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. End-to-End Async Flow

### 2.1 Complete Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        Async Report Complete Flow                           │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ PHASE 1: Request Creation                                           │   │
│  ├─────────────────────────────────────────────────────────────────────┤   │
│  │                                                                     │   │
│  │  Client ──► Controller ──► Service ──► Create ReportRequest         │   │
│  │                                           │                         │   │
│  │                                           ├── Save to DB            │   │
│  │                                           ├── Publish to RabbitMQ   │   │
│  │                                           └── Return Request ID     │   │
│  │                                                                     │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ PHASE 2: Async Processing                                           │   │
│  ├─────────────────────────────────────────────────────────────────────┤   │
│  │                                                                     │   │
│  │  RabbitMQ ──► Consumer ──► Process Report                          │   │
│  │                               │                                     │   │
│  │                               ├── Update status: PROCESSING         │   │
│  │                               ├── Query data from DB                │   │
│  │                               ├── Generate report file              │   │
│  │                               ├── Upload to MinIO                   │   │
│  │                               └── Update status: COMPLETED          │   │
│  │                                                                     │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ PHASE 3: Status Check & Download                                    │   │
│  ├─────────────────────────────────────────────────────────────────────┤   │
│  │                                                                     │   │
│  │  Client ──► GET /async/{requestId} ──► Return Status                │   │
│  │                                                                     │   │
│  │  Client ──► GET /async/{requestId}/download ──► Return File         │   │
│  │                                                                     │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 2.2 Sequence Diagram

```
┌────────┐     ┌────────────┐     ┌────────────┐     ┌────────────┐     ┌────────────┐     ┌────────┐
│ Client │     │ Controller │     │  Service   │     │  RabbitMQ  │     │  Consumer  │     │  MinIO │
└────────┘     └────────────┘     └────────────┘     └────────────┘     └────────────┘     └────────┘
    │               │                   │                   │                 │                │
    │ 1. POST       │                   │                   │                 │                │
    │ /async/request│                   │                   │                 │                │
    │──────────────►│                   │                   │                 │                │
    │               │ 2. createRequest()│                   │                 │                │
    │               │──────────────────►│                   │                 │                │
    │               │                   │                   │                 │                │
    │               │                   │ 3. Save to DB     │                 │                │
    │               │                   │─────┐             │                 │                │
    │               │                   │     │ INSERT      │                 │                │
    │               │                   │     │ report_     │                 │                │
    │               │                   │     │ requests    │                 │                │
    │               │                   │◄────┘             │                 │                │
    │               │                   │                   │                 │                │
    │               │                   │ 4. Publish        │                 │                │
    │               │                   │    Message        │                 │                │
    │               │                   │──────────────────►│                 │                │
    │               │                   │                   │                 │                │
    │               │ 5. Return         │                   │                 │                │
    │               │    Request ID     │                   │                 │                │
    │               │◄──────────────────│                   │                 │                │
    │               │                   │                   │                 │                │
    │ 6. 202        │                   │                   │                 │                │
    │ Accepted      │                   │                   │                 │                │
    │ {request_id}  │                   │                   │                 │                │
    │◄──────────────│                   │                   │                 │                │
    │               │                   │                   │                 │                │
    │               │                   │                   │ 7. Consume      │                │
    │               │                   │                   │    Message      │                │
    │               │                   │                   │────────────────►│                │
    │               │                   │                   │                 │                │
    │               │                   │                   │                 │ 8. Update      │
    │               │                   │                   │                 │    PROCESSING  │
    │               │                   │                   │                 │─────┐          │
    │               │                   │                   │                 │◄────┘          │
    │               │                   │                   │                 │                │
    │               │                   │                   │                 │ 9. Query DB   │
    │               │                   │                   │                 │─────┐          │
    │               │                   │                   │                 │◄────┘          │
    │               │                   │                   │                 │                │
    │               │                   │                   │                 │ 10. Generate   │
    │               │                   │                   │                 │     Report     │
    │               │                   │                   │                 │─────┐          │
    │               │                   │                   │                 │     │ Jasper   │
    │               │                   │                   │                 │     │ POI      │
    │               │                   │                   │                 │     │ CSV      │
    │               │                   │                   │                 │◄────┘          │
    │               │                   │                   │                 │                │
    │               │                   │                   │                 │ 11. Upload    │
    │               │                   │                   │                 │────────────────►│
    │               │                   │                   │                 │                │
    │               │                   │                   │                 │ 12. Return    │
    │               │                   │                   │                 │    File URL    │
    │               │                   │                   │                 │◄────────────────│
    │               │                   │                   │                 │                │
    │               │                   │                   │                 │ 13. Update    │
    │               │                   │                   │                 │     COMPLETED  │
    │               │                   │                   │                 │─────┐          │
    │               │                   │                   │                 │◄────┘          │
    │               │                   │                   │                 │                │
    │ 14. GET       │                   │                   │                 │                │
    │ /async/{id}   │                   │                   │                 │                │
    │──────────────►│                   │                   │                 │                │
    │               │ 15. getStatus()   │                   │                 │                │
    │               │──────────────────►│                   │                 │                │
    │               │ 16. Return Status │                   │                 │                │
    │               │◄──────────────────│                   │                 │                │
    │ 17. 200       │                   │                   │                 │                │
    │ {status:      │                   │                   │                 │                │
    │  "COMPLETED", │                   │                   │                 │                │
    │  file_url}    │                   │                   │                 │                │
    │◄──────────────│                   │                   │                 │                │
    │               │                   │                   │                 │                │
    │ 18. GET       │                   │                   │                 │                │
    │ /async/{id}/  │                   │                   │                 │                │
    │ download      │                   │                   │                 │                │
    │──────────────►│                   │                   │                 │                │
    │               │ 19. getFile()     │                   │                 │                │
    │               │──────────────────►│                   │                 │                │
    │               │                   │ 20. Get presigned │                 │                │
    │               │                   │     URL           │                 │                │
    │               │                   │────────────────────────────────────────────────────────►│
    │               │                   │                   │                 │                │
    │               │                   │ 21. Return URL    │                 │                │
    │               │                   │◄────────────────────────────────────────────────────────│
    │               │ 22. Redirect      │                   │                 │                │
    │               │◄──────────────────│                   │                 │                │
    │ 23. 302       │                   │                   │                 │                │
    │ Redirect      │                   │                   │                 │                │
    │◄──────────────│                   │                   │                 │                │
    │               │                   │                   │                 │                │
    │ 24. GET       │                   │                   │                 │                │
    │ presigned_url │                   │                   │                 │                │
    │──────────────────────────────────────────────────────────────────────────────────────────►│
    │               │                   │                   │                 │                │
    │ 25. File      │                   │                   │                 │                │
    │◄──────────────────────────────────────────────────────────────────────────────────────────│
    │               │                   │                   │                 │                │
```

---

## 3. Chi Tiết Từng Phase

### 3.1 Phase 1: Request Creation

#### Request

```json
POST /api/v1/reports/async/request

{
  "report_type": "CURRENT_STOCK",
  "parameters": {
    "warehouse_id": "uuid",
    "include_zero_stock": false,
    "group_by": "PRODUCT"
  },
  "format": "EXCEL",
  "priority": "NORMAL",
  "callback_url": "https://example.com/webhook" // Optional
}
```

#### Service Logic

```java
@Service
@Slf4j
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ReportRequestRepository reportRequestRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ReportValidator validator;

    @Override
    @Transactional
    public AsyncReportResponse createAsyncReportRequest(AsyncReportRequest request) {
        log.info("Creating async report request: {}", request.getReportType());
        
        // 1. Validate request
        validator.validateAsyncReportRequest(request);
        
        // 2. Create ReportRequest entity
        ReportRequest reportRequest = ReportRequest.builder()
                .id(UUID.randomUUID().toString())
                .reportType(request.getReportType())
                .parameters(objectMapper.writeValueAsString(request.getParameters()))
                .format(request.getFormat())
                .status(ReportRequestStatus.PENDING)
                .priority(request.getPriority())
                .requestedBy(SecurityContextHolder.getContext().getAuthentication().getName())
                .requestedAt(LocalDateTime.now())
                .retryCount(0)
                .maxRetries(3)
                .build();
        
        // 3. Save to database
        reportRequest = reportRequestRepository.save(reportRequest);
        
        // 4. Publish to RabbitMQ
        ReportMessage message = ReportMessage.builder()
                .requestId(reportRequest.getId())
                .reportType(request.getReportType())
                .parameters(request.getParameters())
                .format(request.getFormat())
                .build();
        
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.REPORT_EXCHANGE,
                RabbitMQConfig.REPORT_GENERATE_ROUTING_KEY,
                message
        );
        
        log.info("Async report request created: {}", reportRequest.getId());
        
        // 5. Return response
        return AsyncReportResponse.builder()
                .requestId(reportRequest.getId())
                .status(reportRequest.getStatus())
                .estimatedCompletionTime(calculateEstimatedTime(request))
                .build();
    }
}
```

#### Database Record

```sql
INSERT INTO report_requests (
    id, report_type, parameters, format, status,
    priority, requested_by, requested_at, retry_count, max_retries,
    created_at, updated_at
) VALUES (
    'uuid',
    'CURRENT_STOCK',
    '{"warehouse_id":"uuid","include_zero_stock":false}',
    'EXCEL',
    'PENDING',
    'NORMAL',
    'user-uuid',
    '2026-03-22 10:30:00',
    0,
    3,
    '2026-03-22 10:30:00',
    '2026-03-22 10:30:00'
);
```

### 3.2 Phase 2: Async Processing

#### Consumer Implementation

```java
@Component
@Slf4j
@RequiredArgsConstructor
public class ReportConsumerService {

    private final ReportRequestRepository reportRequestRepository;
    private final ReportGenerator reportGenerator;
    private final MinioClient minioClient;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = RabbitMQConfig.REPORT_GENERATE_QUEUE)
    public void processReportRequest(ReportMessage message) {
        log.info("Processing report request: {}", message.getRequestId());
        
        ReportRequest reportRequest = null;
        
        try {
            // 1. Get request from database
            reportRequest = reportRequestRepository.findById(message.getRequestId())
                    .orElseThrow(() -> new ReportNotFoundException(message.getRequestId()));
            
            // 2. Update status to PROCESSING
            reportRequest.setStatus(ReportRequestStatus.PROCESSING);
            reportRequest.setStartedAt(LocalDateTime.now());
            reportRequestRepository.save(reportRequest);
            
            // 3. Parse parameters
            Map<String, Object> parameters = objectMapper.readValue(
                    reportRequest.getParameters(),
                    new TypeReference<Map<String, Object>>() {}
            );
            
            // 4. Generate report
            byte[] reportData = reportGenerator.generate(
                    reportRequest.getReportType(),
                    parameters,
                    reportRequest.getFormat()
            );
            
            // 5. Upload to MinIO
            String objectName = buildObjectName(reportRequest);
            uploadToMinio(objectName, reportData, reportRequest.getFormat());
            
            // 6. Update status to COMPLETED
            reportRequest.setStatus(ReportRequestStatus.COMPLETED);
            reportRequest.setCompletedAt(LocalDateTime.now());
            reportRequest.setFileUrl(objectName);
            reportRequest.setFileSize((long) reportData.length);
            reportRequestRepository.save(reportRequest);
            
            log.info("Report request completed: {}", message.getRequestId());
            
        } catch (Exception e) {
            log.error("Failed to process report request: {}", message.getRequestId(), e);
            handleFailure(reportRequest, e);
        }
    }

    private void handleFailure(ReportRequest request, Exception e) {
        if (request == null) {
            return;
        }
        
        request.setRetryCount(request.getRetryCount() + 1);
        request.setErrorMessage(e.getMessage());
        
        if (request.getRetryCount() < request.getMaxRetries()) {
            // Retry
            request.setStatus(ReportRequestStatus.PENDING);
            log.info("Will retry report request: {} (attempt {}/{})",
                    request.getId(), request.getRetryCount(), request.getMaxRetries());
        } else {
            // Max retries reached
            request.setStatus(ReportRequestStatus.FAILED);
            log.error("Report request failed after {} retries: {}",
                    request.getMaxRetries(), request.getId());
        }
        
        reportRequestRepository.save(request);
    }

    private String buildObjectName(ReportRequest request) {
        LocalDateTime now = LocalDateTime.now();
        return String.format("reports/%d/%d/%s/%s.%s",
                now.getYear(),
                now.getMonthValue(),
                request.getReportType().toLowerCase(),
                request.getId(),
                request.getFormat().getExtension()
        );
    }

    private void uploadToMinio(String objectName, byte[] data, ExportFormat format) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket("whs-reports")
                            .object(objectName)
                            .stream(new ByteArrayInputStream(data), data.length, -1)
                            .contentType(format.getContentType())
                            .build()
            );
        } catch (Exception e) {
            throw new ReportStorageException("Failed to upload report to storage", e);
        }
    }
}
```

### 3.3 Phase 3: Status Check & Download

#### Status Check

```
GET /api/v1/reports/async/{requestId}

Response (PENDING):
{
  "success": true,
  "data": {
    "request_id": "uuid",
    "report_type": "CURRENT_STOCK",
    "status": "PENDING",
    "format": "EXCEL",
    "requested_at": "2026-03-22T10:30:00Z",
    "started_at": null,
    "completed_at": null,
    "progress_percentage": 0,
    "estimated_completion": "2026-03-22T10:35:00Z",
    "error_message": null
  }
}

Response (PROCESSING):
{
  "success": true,
  "data": {
    "request_id": "uuid",
    "report_type": "CURRENT_STOCK",
    "status": "PROCESSING",
    "format": "EXCEL",
    "requested_at": "2026-03-22T10:30:00Z",
    "started_at": "2026-03-22T10:31:00Z",
    "completed_at": null,
    "progress_percentage": 45,
    "estimated_completion": "2026-03-22T10:34:00Z",
    "error_message": null
  }
}

Response (COMPLETED):
{
  "success": true,
  "data": {
    "request_id": "uuid",
    "report_type": "CURRENT_STOCK",
    "status": "COMPLETED",
    "format": "EXCEL",
    "requested_at": "2026-03-22T10:30:00Z",
    "started_at": "2026-03-22T10:31:00Z",
    "completed_at": "2026-03-22T10:33:00Z",
    "progress_percentage": 100,
    "file_url": "reports/2026/3/current_stock/uuid.xlsx",
    "file_size": 123456,
    "download_url": "https://minio.example.com/whs-reports/reports/2026/3/current_stock/uuid.xlsx?X-Amz-Algorithm=...",
    "download_url_expires_at": "2026-03-22T11:33:00Z",
    "error_message": null
  }
}
```

#### Download

```
GET /api/v1/reports/async/{requestId}/download

Response:
{
  "success": true,
  "data": {
    "download_url": "https://minio.example.com/whs-reports/reports/2026/3/current_stock/uuid.xlsx?X-Amz-Algorithm=...",
    "expires_at": "2026-03-22T11:33:00Z",
    "file_name": "current_stock_report_20260322.xlsx",
    "file_size": 123456
  }
}
```

---

## 4. RabbitMQ Configuration

### 4.1 Exchange & Queue Setup

```java
@Configuration
public class RabbitMQReportConfig {

    public static final String REPORT_EXCHANGE = "whs.report.exchange";
    public static final String REPORT_GENERATE_QUEUE = "whs.report.generate.queue";
    public static final String REPORT_DLQ = "whs.report.dlq";
    public static final String REPORT_GENERATE_ROUTING_KEY = "report.generate";

    @Bean
    public TopicReportExchange reportExchange() {
        return new TopicReportExchange(REPORT_EXCHANGE);
    }

    @Bean
    public Queue reportGenerateQueue() {
        return QueueBuilder.durable(REPORT_GENERATE_QUEUE)
                .withArgument("x-dead-letter-exchange", REPORT_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "report.dlq")
                .build();
    }

    @Bean
    public Queue reportDlq() {
        return QueueBuilder.durable(REPORT_DLQ).build();
    }

    @Bean
    public Binding reportGenerateBinding() {
        return BindingBuilder
                .bind(reportGenerateQueue())
                .to(reportExchange())
                .with(REPORT_GENERATE_ROUTING_KEY);
    }

    @Bean
    public Binding reportDlqBinding() {
        return BindingBuilder
                .bind(reportDlq())
                .to(reportExchange())
                .with("report.dlq");
    }
}
```

### 4.2 Message Format

```json
{
  "request_id": "uuid",
  "report_type": "CURRENT_STOCK",
  "parameters": {
    "warehouse_id": "uuid",
    "include_zero_stock": false
  },
  "format": "EXCEL",
  "timestamp": "2026-03-22T10:30:00Z"
}
```

---

## 5. My Requests Endpoint

### 5.1 Request

```
GET /api/v1/reports/async/my-requests?status=COMPLETED&page=0&size=20
```

### 5.2 Response

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "request_id": "uuid",
        "report_type": "CURRENT_STOCK",
        "status": "COMPLETED",
        "format": "EXCEL",
        "requested_at": "2026-03-22T10:30:00Z",
        "completed_at": "2026-03-22T10:33:00Z",
        "file_size": 123456,
        "download_available": true,
        "download_expires_at": "2026-03-23T10:33:00Z"
      },
      {
        "request_id": "uuid-2",
        "report_type": "STOCK_VALUATION",
        "status": "PROCESSING",
        "format": "PDF",
        "requested_at": "2026-03-22T10:25:00Z",
        "completed_at": null,
        "progress_percentage": 60
      }
    ],
    "page": 0,
    "size": 20,
    "total_elements": 45,
    "total_pages": 3
  }
}
```

---

## 6. Error Handling & Retry

### 6.1 Retry Logic

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           Retry Logic                                       │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  Max Retries: 3                                                             │
│                                                                             │
│  Retry Strategy: Exponential Backoff                                        │
│  ├── Attempt 1: Immediate                                                  │
│  ├── Attempt 2: After 1 minute                                             │
│  ├── Attempt 3: After 5 minutes                                            │
│  └── Max Retries: Mark as FAILED                                           │
│                                                                             │
│  Retryable Errors:                                                          │
│  ├── Database connection timeout                                           │
│  ├── MinIO upload failure                                                  │
│  ├── Report generation timeout                                             │
│  └── Temporary system errors                                               │
│                                                                             │
│  Non-Retryable Errors:                                                      │
│  ├── Invalid parameters                                                    │
│  ├── Permission denied                                                     │
│  ├── Resource not found                                                    │
│  └── Business rule violation                                               │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 6.2 DLQ Handling

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         DLQ Handling                                        │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  When message goes to DLQ:                                                  │
│  1. Log error with full context                                            │
│  2. Update ReportRequest status to FAILED                                  │
│  3. Send alert to monitoring system                                        │
│  4. Admin can manually retry from DLQ                                      │
│                                                                             │
│  DLQ Monitoring:                                                            │
│  ├── Alert when DLQ size > 10                                              │
│  ├── Alert when message age > 1 hour                                       │
│  └── Dashboard showing DLQ metrics                                         │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 7. Performance Considerations

### 7.1 Timeout Configuration

```yaml
spring:
  rabbitmq:
    listener:
      simple:
        acknowledge-mode: manual
        concurrency: 3
        max-concurrency: 10
        prefetch: 1
        retry:
          enabled: true
          initial-interval: 1000
          max-attempts: 3
          multiplier: 2.0

report:
  generation:
    timeout: 300000  # 5 minutes
    max-rows: 100000
  download:
    url-expiry: 3600  # 1 hour
```

### 7.2 Resource Limits

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         Resource Limits                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  Per User:                                                                  │
│  ├── Max 5 concurrent async requests                                       │
│  ├── Max 20 async requests per day                                         │
│  └── Max file size: 50 MB                                                  │
│                                                                             │
│  System:                                                                    │
│  ├── Max 100 concurrent report generations                                 │
│  ├── Max queue size: 1000                                                  │
│  └── Report retention: 7 days                                              │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 8. Monitoring & Alerts

### 8.1 Metrics

```java
@Component
public class ReportMetrics {

    private final MeterRegistry meterRegistry;
    
    public void recordReportGeneration(String reportType, long durationMs, boolean success) {
        Timer.builder("report.generation.duration")
                .tag("report_type", reportType)
                .tag("success", String.valueOf(success))
                .register(meterRegistry)
                .record(durationMs, TimeUnit.MILLISECONDS);
        
        Counter.builder("report.generation.total")
                .tag("report_type", reportType)
                .tag("success", String.valueOf(success))
                .register(meterRegistry)
                .increment();
    }
}
```

### 8.2 Alerts

| Condition | Severity | Action |
|-----------|----------|--------|
| Queue size > 100 | Warning | Scale up consumers |
| Error rate > 10% | Critical | Investigate root cause |
| Generation time > 10 min | Warning | Optimize queries |
| DLQ size > 10 | Critical | Manual intervention |

---

## 9. Documentation References

- RabbitMQ Spring: [Spring AMQP](https://spring.io/projects/spring-amqp)
- MinIO Java SDK: [MinIO Docs](https://min.io/docs/minio/linux/developers/java/minio-java.html)
- Dead Letter Queues: [RabbitMQ DLX](https://www.rabbitmq.com/dlx.html)
