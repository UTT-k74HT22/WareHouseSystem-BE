# Schedule Report Flow - Chi Tiết

> Module: WHS-69
> Updated: 2026-03-22

---

## 1. Tổng Quan Scheduled Reports

Scheduled reports được tạo tự động theo lịch (cron expression) và gửi qua email hoặc upload lên MinIO.

### 1.1 Schedule Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /api/v1/reports/schedules | Tạo lịch báo cáo |
| GET | /api/v1/reports/schedules | Danh sách lịch |
| GET | /api/v1/reports/schedules/{id} | Chi tiết lịch |
| PUT | /api/v1/reports/schedules/{id} | Cập nhật lịch |
| DELETE | /api/v1/reports/schedules/{id} | Xóa lịch |
| PUT | /api/v1/reports/schedules/{id}/enable | Kích hoạt |
| PUT | /api/v1/reports/schedules/{id}/disable | Vô hiệu hóa |

### 1.2 Schedule States

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         Schedule State Machine                              │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│                    ┌─────────────┐                                          │
│                    │   ENABLED   │◄─────────────────┐                       │
│                    │             │                   │                       │
│                    └──────┬──────┘                   │                       │
│                           │                          │                       │
│                           │ disable                  │ enable                │
│                           ▼                          │                       │
│                    ┌─────────────┐                   │                       │
│                    │  DISABLED   │───────────────────┘                       │
│                    │             │                                           │
│                    └──────┬──────┘                                           │
│                           │                                                  │
│                           │ delete                                           │
│                           ▼                                                  │
│                    ┌─────────────┐                                          │
│                    │  DELETED    │                                          │
│                    │  (soft)     │                                          │
│                    └─────────────┘                                          │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. End-to-End Schedule Flow

### 2.1 Schedule Management Flow

```
┌────────┐     ┌────────────┐     ┌────────────┐     ┌────────────┐
│ Client │────►│ Controller │────►│  Service   │────►│ Repository │
└────────┘     └────────────┘     └────────────┘     └────────────┘
    │               │                   │                   │
    │ 1. POST       │                   │                   │
    │ /schedules    │                   │                   │
    │──────────────►│                   │                   │
    │               │ 2. Validate       │                   │
    │               │──────────────────►│                   │
    │               │                   │ 3. Check cron     │
    │               │                   │    expression     │
    │               │                   │─────┐             │
    │               │                   │◄────┘             │
    │               │                   │                   │
    │               │                   │ 4. Calculate      │
    │               │                   │    next_run_at    │
    │               │                   │─────┐             │
    │               │                   │◄────┘             │
    │               │                   │                   │
    │               │                   │ 5. Save           │
    │               │                   │──────────────────►│
    │               │ 6. Return         │                   │
    │               │◄──────────────────│                   │
    │ 7. 201        │                   │                   │
    │◄──────────────│                   │                   │
    │               │                   │                   │
```

### 2.2 Scheduled Execution Flow

```
┌─────────────┐     ┌────────────┐     ┌────────────┐     ┌────────────┐     ┌────────┐
│  Scheduler  │────►│  Service   │────►│  Generator │────►│   MinIO    │────►│  Email │
│  (Spring)   │     │            │     │            │     │            │     │ Service│
└─────────────┘     └────────────┘     └────────────┘     └────────────┘     └────────┘
       │                   │                   │                 │                 │
       │ 1. Cron Trigger   │                   │                 │                 │
       │──────────────────►│                   │                 │                 │
       │                   │ 2. Get Active     │                 │                 │
       │                   │    Schedules      │                 │                 │
       │                   │─────┐             │                 │                 │
       │                   │     │ WHERE       │                 │                 │
       │                   │     │ is_enabled=1│                 │                 │
       │                   │     │ next_run_   │                 │                 │
       │                   │     │ at <= NOW() │                 │                 │
       │                   │◄────┘             │                 │                 │
       │                   │                   │                 │                 │
       │                   │ 3. For Each       │                 │                 │
       │                   │    Schedule:      │                 │                 │
       │                   │    ┌──────────────┤                 │                 │
       │                   │    │              │                 │                 │
       │                   │    │ 4. Lock      │                 │                 │
       │                   │    │    (Redis)   │                 │                 │
       │                   │    │─────┐        │                 │                 │
       │                   │    │◄────┘        │                 │                 │
       │                   │    │              │                 │                 │
       │                   │    │ 5. Generate  │                 │                 │
       │                   │    │─────────────────────────────►│                 │
       │                   │    │              │                 │                 │
       │                   │    │ 6. Upload    │                 │                 │
       │                   │    │──────────────────────────────►│                 │
       │                   │    │              │                 │                 │
       │                   │    │ 7. Get URL   │                 │                 │
       │                   │    │◄──────────────────────────────│                 │
       │                   │    │              │                 │                 │
       │                   │    │ 8. Send      │                 │                 │
       │                   │    │    Email     │                 │                 │
       │                   │    │────────────────────────────────────────────────►│
       │                   │    │              │                 │                 │
       │                   │    │ 9. Update    │                 │                 │
       │                   │    │    Schedule  │                 │                 │
       │                   │    │─────┐        │                 │                 │
       │                   │    │     │ last_  │                 │                 │
       │                   │    │     │ run_at │                 │                 │
       │                   │    │     │ next_  │                 │                 │
       │                   │    │     │ run_at │                 │                 │
       │                   │    │◄────┘        │                 │                 │
       │                   │    │              │                 │                 │
       │                   │    └──────────────┤                 │                 │
       │                   │                   │                 │                 │
       │ 10. Complete      │                   │                 │                 │
       │◄──────────────────│                   │                 │                 │
       │                   │                   │                 │                 │
```

---

## 3. Chi Tiết Từng Component

### 3.1 Schedule Entity

```java
@Entity
@Table(name = "report_schedules")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReportSchedule extends BaseEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false)
    private ReportType reportType;

    @Column(name = "parameters", nullable = false, columnDefinition = "JSON")
    private String parameters;

    @Enumerated(EnumType.STRING)
    @Column(name = "format", nullable = false)
    private ExportFormat format;

    @Column(name = "cron_expression", nullable = false, length = 50)
    private String cronExpression;

    @Column(name = "timezone", length = 50)
    @Builder.Default
    private String timezone = "Asia/Ho_Chi_Minh";

    @Column(name = "is_enabled", nullable = false)
    @Builder.Default
    private Boolean isEnabled = true;

    @Column(name = "last_run_at")
    private LocalDateTime lastRunAt;

    @Column(name = "next_run_at")
    private LocalDateTime nextRunAt;

    @Column(name = "recipients", columnDefinition = "JSON")
    private String recipients; // JSON array of email addresses

    @Column(name = "created_by", nullable = false, length = 36)
    private String createdBy;
}
```

### 3.2 Create Schedule Request

```json
POST /api/v1/reports/schedules

{
  "name": "Báo cáo tồn kho hàng ngày",
  "description": "Gửi báo cáo tồn kho vào 8:00 sáng hàng ngày",
  "report_type": "CURRENT_STOCK",
  "parameters": {
    "warehouse_id": "uuid",
    "include_zero_stock": false,
    "group_by": "PRODUCT"
  },
  "format": "EXCEL",
  "cron_expression": "0 0 8 * * ?",
  "timezone": "Asia/Ho_Chi_Minh",
  "recipients": [
    "manager@example.com",
    "warehouse@example.com"
  ]
}
```

### 3.3 Cron Expression Examples

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         Cron Expression Examples                            │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  Format: second minute hour dayOfMonth month dayOfWeek                      │
│                                                                             │
│  Common Schedules:                                                          │
│  ├── "0 0 8 * * ?"     → Every day at 8:00 AM                             │
│  ├── "0 0 8 * * MON"   → Every Monday at 8:00 AM                          │
│  ├── "0 0 8 1 * ?"     → 1st of every month at 8:00 AM                    │
│  ├── "0 0 */4 * * ?"   → Every 4 hours                                    │
│  ├── "0 30 9 * * MON-FRI" → Weekdays at 9:30 AM                           │
│  └── "0 0 0 1 1 ?"     → January 1st at midnight (yearly)                 │
│                                                                             │
│  Validation Rules:                                                          │
│  ├── Minimum interval: 1 hour (prevent too frequent)                       │
│  ├── Maximum interval: 1 year                                              │
│  └── Must be valid Quartz cron expression                                  │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 3.4 Scheduler Implementation

```java
@Component
@Slf4j
@RequiredArgsConstructor
public class ReportScheduler {

    private final ReportScheduleRepository scheduleRepository;
    private final ReportService reportService;
    private final EmailService emailService;
    private final MinioClient minioClient;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 60000) // Check every minute
    public void processScheduledReports() {
        log.debug("Checking for scheduled reports to execute");
        
        LocalDateTime now = LocalDateTime.now();
        
        // 1. Get schedules due for execution
        List<ReportSchedule> schedules = scheduleRepository
                .findByIsEnabledTrueAndNextRunAtLessThanEqual(now);
        
        if (schedules.isEmpty()) {
            return;
        }
        
        log.info("Found {} scheduled reports to execute", schedules.size());
        
        // 2. Process each schedule
        for (ReportSchedule schedule : schedules) {
            try {
                executeSchedule(schedule);
            } catch (Exception e) {
                log.error("Failed to execute schedule: {}", schedule.getId(), e);
                handleScheduleFailure(schedule, e);
            }
        }
    }

    @Transactional
    protected void executeSchedule(ReportSchedule schedule) {
        String lockKey = "report:schedule:lock:" + schedule.getId();
        
        // 1. Acquire distributed lock
        Boolean locked = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "locked", Duration.ofMinutes(10));
        
        if (!Boolean.TRUE.equals(locked)) {
            log.warn("Schedule {} is already being executed by another instance", schedule.getId());
            return;
        }
        
        try {
            log.info("Executing schedule: {} ({})", schedule.getName(), schedule.getId());
            
            // 2. Parse parameters
            Map<String, Object> parameters = objectMapper.readValue(
                    schedule.getParameters(),
                    new TypeReference<Map<String, Object>>() {}
            );
            
            // 3. Generate report
            byte[] reportData = reportService.generateReport(
                    schedule.getReportType(),
                    parameters,
                    schedule.getFormat()
            );
            
            // 4. Upload to MinIO
            String objectName = buildObjectName(schedule);
            uploadToMinio(objectName, reportData, schedule.getFormat());
            
            // 5. Get download URL
            String downloadUrl = getPresignedUrl(objectName);
            
            // 6. Send email to recipients
            if (schedule.getRecipients() != null && !schedule.getRecipients().isEmpty()) {
                sendNotificationEmail(schedule, downloadUrl, reportData);
            }
            
            // 7. Update schedule
            schedule.setLastRunAt(LocalDateTime.now());
            schedule.setNextRunAt(calculateNextRun(schedule.getCronExpression(), schedule.getTimezone()));
            scheduleRepository.save(schedule);
            
            log.info("Schedule executed successfully: {}", schedule.getId());
            
        } finally {
            // 8. Release lock
            redisTemplate.delete(lockKey);
        }
    }

    private void sendNotificationEmail(ReportSchedule schedule, String downloadUrl, byte[] attachment) {
        List<String> recipients = objectMapper.readValue(
                schedule.getRecipients(),
                new TypeReference<List<String>>() {}
        );
        
        EmailRequest emailRequest = EmailRequest.builder()
                .to(recipients)
                .subject("Scheduled Report: " + schedule.getName())
                .templateName("scheduled-report")
                .templateVariables(Map.of(
                        "reportName", schedule.getName(),
                        "generatedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                        "downloadUrl", downloadUrl
                ))
                .attachments(List.of(
                        EmailAttachment.builder()
                                .fileName(buildFileName(schedule))
                                .content(attachment)
                                .contentType(schedule.getFormat().getContentType())
                                .build()
                ))
                .build();
        
        emailService.sendEmail(emailRequest);
    }

    private LocalDateTime calculateNextRun(String cronExpression, String timezone) {
        CronSequenceGenerator generator = new CronSequenceGenerator(cronExpression, TimeZone.getTimeZone(timezone));
        Date nextDate = generator.next(new Date());
        return LocalDateTime.ofInstant(nextDate.toInstant(), ZoneId.of(timezone));
    }
}
```

---

## 4. Email Notification Flow

### 4.1 Email Template

```html
<!-- scheduled-report.html -->
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Scheduled Report Notification</title>
</head>
<body>
    <h2>Scheduled Report: {{reportName}}</h2>
    
    <p>Your scheduled report has been generated successfully.</p>
    
    <table>
        <tr>
            <td><strong>Generated At:</strong></td>
            <td>{{generatedAt}}</td>
        </tr>
        <tr>
            <td><strong>Download Link:</strong></td>
            <td><a href="{{downloadUrl}}">Download Report</a></td>
        </tr>
        <tr>
            <td><strong>Link Expires:</strong></td>
            <td>24 hours from generation time</td>
        </tr>
    </table>
    
    <p>The report file is also attached to this email.</p>
    
    <hr>
    <p><small>This is an automated message from WHS Report System.</small></p>
</body>
</html>
```

### 4.2 Email Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           Email Notification Flow                           │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐ │
│  │  Scheduler  │───►│   Report    │───►│   Email     │───►│   RabbitMQ  │ │
│  │             │    │  Service    │    │  Service    │    │             │ │
│  └─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘ │
│                                                                             │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐                     │
│  │   SMTP      │◄───│   Email     │◄───│  Consumer   │                     │
│  │  Server     │    │  Consumer   │    │             │                     │
│  └─────────────┘    └─────────────┘    └─────────────┘                     │
│                                                                             │
│  Email Content:                                                             │
│  ├── Subject: "Scheduled Report: [Report Name]"                            │
│  ├── Body: HTML template with report info                                  │
│  ├── Attachment: Report file (PDF/Excel/CSV)                               │
│  └── Recipients: From schedule.recipients JSON                             │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 5. Schedule Management APIs

### 5.1 Create Schedule

```
POST /api/v1/reports/schedules

Request:
{
  "name": "Daily Inventory Report",
  "description": "Daily inventory summary for warehouse managers",
  "report_type": "CURRENT_STOCK",
  "parameters": {
    "warehouse_id": "uuid",
    "include_zero_stock": false
  },
  "format": "EXCEL",
  "cron_expression": "0 0 8 * * ?",
  "timezone": "Asia/Ho_Chi_Minh",
  "recipients": ["manager@example.com"]
}

Response:
{
  "success": true,
  "data": {
    "id": "uuid",
    "name": "Daily Inventory Report",
    "report_type": "CURRENT_STOCK",
    "cron_expression": "0 0 8 * * ?",
    "is_enabled": true,
    "next_run_at": "2026-03-23T08:00:00+07:00",
    "created_at": "2026-03-22T10:30:00+07:00"
  }
}
```

### 5.2 List Schedules

```
GET /api/v1/reports/schedules?is_enabled=true&page=0&size=20

Response:
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "uuid",
        "name": "Daily Inventory Report",
        "report_type": "CURRENT_STOCK",
        "format": "EXCEL",
        "cron_expression": "0 0 8 * * ?",
        "is_enabled": true,
        "last_run_at": "2026-03-22T08:00:00+07:00",
        "next_run_at": "2026-03-23T08:00:00+07:00",
        "recipient_count": 2,
        "created_by_name": "Admin User"
      }
    ],
    "page": 0,
    "size": 20,
    "total_elements": 5,
    "total_pages": 1
  }
}
```

### 5.3 Update Schedule

```
PUT /api/v1/reports/schedules/{id}

Request:
{
  "name": "Daily Inventory Report - Updated",
  "cron_expression": "0 0 9 * * ?",
  "recipients": ["manager@example.com", "supervisor@example.com"]
}

Response:
{
  "success": true,
  "data": {
    "id": "uuid",
    "name": "Daily Inventory Report - Updated",
    "cron_expression": "0 0 9 * * ?",
    "next_run_at": "2026-03-23T09:00:00+07:00",
    "updated_at": "2026-03-22T11:00:00+07:00"
  }
}
```

### 5.4 Enable/Disable Schedule

```
PUT /api/v1/reports/schedules/{id}/disable

Response:
{
  "success": true,
  "data": {
    "id": "uuid",
    "is_enabled": false,
    "updated_at": "2026-03-22T11:00:00+07:00"
  }
}

PUT /api/v1/reports/schedules/{id}/enable

Response:
{
  "success": true,
  "data": {
    "id": "uuid",
    "is_enabled": true,
    "next_run_at": "2026-03-23T09:00:00+07:00",
    "updated_at": "2026-03-22T11:05:00+07:00"
  }
}
```

### 5.5 Delete Schedule

```
DELETE /api/v1/reports/schedules/{id}

Response:
{
  "success": true,
  "message": "Schedule deleted successfully"
}
```

---

## 6. Validation Rules

### 6.1 Cron Expression Validation

```java
@Component
public class CronValidator {

    private static final int MIN_INTERVAL_HOURS = 1;
    private static final int MAX_INTERVAL_YEARS = 1;

    public void validateCronExpression(String cronExpression, String timezone) {
        // 1. Check if valid cron expression
        if (!CronSequenceGenerator.isValidExpression(cronExpression)) {
            throw new InvalidCronException("Invalid cron expression: " + cronExpression);
        }
        
        // 2. Calculate next few executions
        CronSequenceGenerator generator = new CronSequenceGenerator(
                cronExpression, 
                TimeZone.getTimeZone(timezone)
        );
        
        Date now = new Date();
        Date next1 = generator.next(now);
        Date next2 = generator.next(next1);
        
        // 3. Check minimum interval
        long intervalMs = next2.getTime() - next1.getTime();
        long intervalHours = TimeUnit.MILLISECONDS.toHours(intervalMs);
        
        if (intervalHours < MIN_INTERVAL_HOURS) {
            throw new InvalidCronException(
                    "Cron interval too frequent. Minimum interval is " + MIN_INTERVAL_HOURS + " hour(s)"
            );
        }
        
        // 4. Check maximum interval
        long intervalYears = TimeUnit.MILLISECONDS.toDays(intervalMs) / 365;
        if (intervalYears > MAX_INTERVAL_YEARS) {
            throw new InvalidCronException(
                    "Cron interval too long. Maximum interval is " + MAX_INTERVAL_YEARS + " year(s)"
            );
        }
    }
}
```

### 6.2 Schedule Validation

```java
@Component
public class ScheduleValidator {

    public void validateScheduleRequest(CreateScheduleRequest request) {
        // 1. Validate name
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new ValidationException("Schedule name is required");
        }
        
        if (request.getName().length() > 100) {
            throw new ValidationException("Schedule name must not exceed 100 characters");
        }
        
        // 2. Validate report type
        if (request.getReportType() == null) {
            throw new ValidationException("Report type is required");
        }
        
        // 3. Validate format
        if (request.getFormat() == null) {
            throw new ValidationException("Export format is required");
        }
        
        // 4. Validate cron expression
        cronValidator.validateCronExpression(request.getCronExpression(), request.getTimezone());
        
        // 5. Validate recipients
        if (request.getRecipients() != null && !request.getRecipients().isEmpty()) {
            for (String email : request.getRecipients()) {
                if (!isValidEmail(email)) {
                    throw new ValidationException("Invalid email address: " + email);
                }
            }
        }
        
        // 6. Validate parameters based on report type
        validateReportParameters(request.getReportType(), request.getParameters());
    }
}
```

---

## 7. Concurrency Handling

### 7.1 Distributed Lock

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                      Distributed Lock Flow                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  Problem: Multiple application instances may try to execute same schedule   │
│                                                                             │
│  Solution: Redis distributed lock                                          │
│                                                                             │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐                     │
│  │ Instance 1  │    │ Instance 2  │    │   Redis     │                     │
│  └──────┬──────┘    └──────┬──────┘    └──────┬──────┘                     │
│         │                  │                  │                             │
│         │ SETNX            │                  │                             │
│         │ lock:key → true  │                  │                             │
│         │─────────────────────────────────────►│                             │
│         │                  │                  │                             │
│         │                  │ SETNX            │                             │
│         │                  │ lock:key → false │                             │
│         │                  │─────────────────►│                             │
│         │                  │                  │                             │
│         │ Execute schedule │ Skip execution   │                             │
│         │────────────┐     │                  │                             │
│         │◄───────────┘     │                  │                             │
│         │                  │                  │                             │
│         │ DEL lock:key     │                  │                             │
│         │─────────────────────────────────────►│                             │
│         │                  │                  │                             │
│                                                                             │
│  Lock Configuration:                                                        │
│  ├── Key: report:schedule:lock:{schedule_id}                               │
│  ├── TTL: 10 minutes (prevent deadlock)                                    │
│  └── Value: instance_id + timestamp                                        │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 7.2 Implementation

```java
@Component
@RequiredArgsConstructor
public class DistributedLockService {

    private final RedisTemplate<String, String> redisTemplate;

    public boolean acquireLock(String key, String value, Duration ttl) {
        Boolean result = redisTemplate.opsForValue()
                .setIfAbsent(key, value, ttl);
        return Boolean.TRUE.equals(result);
    }

    public void releaseLock(String key) {
        redisTemplate.delete(key);
    }

    public boolean executeWithLock(String lockKey, Runnable task, Duration ttl) {
        String lockValue = UUID.randomUUID().toString();
        
        if (!acquireLock(lockKey, lockValue, ttl)) {
            return false;
        }
        
        try {
            task.run();
            return true;
        } finally {
            releaseLock(lockKey);
        }
    }
}
```

---

## 8. Monitoring & Alerts

### 8.1 Metrics

| Metric | Description |
|--------|-------------|
| schedule.execution.count | Number of schedule executions |
| schedule.execution.duration | Execution time per schedule |
| schedule.execution.success | Successful executions |
| schedule.execution.failure | Failed executions |
| schedule.email.sent | Emails sent |
| schedule.email.failed | Email failures |

### 8.2 Alerts

| Condition | Severity | Action |
|-----------|----------|--------|
| Schedule execution failed | Critical | Investigate and retry |
| Email delivery failed | Warning | Check SMTP configuration |
| Next run time in past | Warning | Check scheduler health |
| Schedule locked > 10 min | Critical | Possible deadlock |

---

## 9. Documentation References

- Spring Scheduler: [Spring Task Scheduling](https://docs.spring.io/spring-framework/reference/integration/scheduling.html)
- Quartz Cron Expression: [Quartz Documentation](http://www.quartz-scheduler.org/documentation/quartz-2.3.0/tutorials/crontrigger.html)
- Redis Distributed Locks: [Redis SETNX](https://redis.io/commands/setnx)
