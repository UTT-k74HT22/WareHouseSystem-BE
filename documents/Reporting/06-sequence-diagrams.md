# Sequence Diagrams - Chi Tiết

> Module: WHS-52 (Report Module)
> Updated: 2026-03-22

---

## 1. On-Demand Report Sequence

### 1.1 Current Stock Report

```
┌────────┐     ┌────────────┐     ┌────────────┐     ┌────────────┐     ┌────────────┐
│ Client │     │ Controller │     │  Service   │     │ Repository │     │    DB      │
└────────┘     └────────────┘     └────────────┘     └────────────┘     └────────────┘
    │               │                   │                   │                 │
    │ 1. POST       │                   │                   │                 │
    │ /reports/     │                   │                   │                 │
    │ current-stock │                   │                   │                 │
    │──────────────►│                   │                   │                 │
    │               │                   │                   │                 │
    │               │ 2. @Valid         │                   │                 │
    │               │ Validate request  │                   │                 │
    │               │─────┐             │                   │                 │
    │               │     │ Check:      │                   │                 │
    │               │     │ - warehouse │                   │                 │
    │               │     │ - date range│                   │                 │
    │               │     │ - format    │                   │                 │
    │               │◄────┘             │                   │                 │
    │               │                   │                   │                 │
    │               │ 3. generateCurrentStockReport()       │                 │
    │               │──────────────────►│                   │                 │
    │               │                   │                   │                 │
    │               │                   │ 4. checkPermission()                │
    │               │                   │─────┐             │                 │
    │               │                   │     │ Get user    │                 │
    │               │                   │     │ from        │                 │
    │               │                   │     │ SecurityCtx │                 │
    │               │                   │     │ Check role  │                 │
    │               │                   │◄────┘             │                 │
    │               │                   │                   │                 │
    │               │                   │ 5. findCurrentStock(filters)        │
    │               │                   │──────────────────►│                 │
    │               │                   │                   │                 │
    │               │                   │                   │ 6. SELECT       │
    │               │                   │                   │    i.*, p.*,    │
    │               │                   │                   │    w.*, l.*,    │
    │               │                   │                   │    b.*          │
    │               │                   │                   │    FROM         │
    │               │                   │                   │    inventory i  │
    │               │                   │                   │    JOIN...      │
    │               │                   │                   │────────────────►│
    │               │                   │                   │                 │
    │               │                   │                   │ 7. ResultSet    │
    │               │                   │                   │◄────────────────│
    │               │                   │                   │                 │
    │               │                   │ 8. List<Inventory>│                 │
    │               │                   │◄──────────────────│                 │
    │               │                   │                   │                 │
    │               │                   │ 9. toCurrentStockItems()            │
    │               │                   │─────┐             │                 │
    │               │                   │     │ Map Entity  │                 │
    │               │                   │     │ to DTO      │                 │
    │               │                   │     │ Calculate   │                 │
    │               │                   │     │ available_  │                 │
    │               │                   │     │ quantity    │                 │
    │               │                   │◄────┘             │                 │
    │               │                   │                   │                 │
    │               │                   │ 10. calculateSummary()              │
    │               │                   │─────┐             │                 │
    │               │                   │     │ Sum totals  │                 │
    │               │                   │◄────┘             │                 │
    │               │                   │                   │                 │
    │               │ 11. Response      │                   │                 │
    │               │◄──────────────────│                   │                 │
    │               │                   │                   │                 │
    │ 12. 200 OK    │                   │                   │                 │
    │ {success:true,│                   │                   │                 │
    │  data:{...}}  │                   │                   │                 │
    │◄──────────────│                   │                   │                 │
    │               │                   │                   │                 │
```

### 1.2 Export Report (PDF)

```
┌────────┐     ┌────────────┐     ┌────────────┐     ┌────────────┐     ┌────────────┐
│ Client │     │ Controller │     │  Service   │     │  Generator │     │ Template   │
└────────┘     └────────────┘     └────────────┘     └────────────┘     └────────────┘
    │               │                   │                   │                 │
    │ 1. POST       │                   │                   │                 │
    │ /reports/     │                   │                   │                 │
    │ current-stock │                   │                   │                 │
    │ /export?      │                   │                   │                 │
    │ format=pdf    │                   │                   │                 │
    │──────────────►│                   │                   │                 │
    │               │                   │                   │                 │
    │               │ 2. exportCurrentStockReport()         │                 │
    │               │──────────────────►│                   │                 │
    │               │                   │                   │                 │
    │               │                   │ 3. generateCurrentStockReport()     │
    │               │                   │─────┐             │                 │
    │               │                   │     │ (same as    │                 │
    │               │                   │     │ above)      │                 │
    │               │                   │◄────┘             │                 │
    │               │                   │                   │                 │
    │               │                   │ 4. generatePdf(reportData, "current_stock")
    │               │                   │──────────────────►│                 │
    │               │                   │                   │                 │
    │               │                   │                   │ 5. loadTemplate()│
    │               │                   │                   │────────────────►│
    │               │                   │                   │                 │
    │               │                   │                   │ 6. current_stock.jrxml
    │               │                   │                   │◄────────────────│
    │               │                   │                   │                 │
    │               │                   │                   │ 7. JasperFillManager
    │               │                   │                   │    .fillReport()│
    │               │                   │                   │─────┐           │
    │               │                   │                   │     │ Fill     │
    │               │                   │                   │     │ template │
    │               │                   │                   │     │ with data│
    │               │                   │                   │◄────┘           │
    │               │                   │                   │                 │
    │               │                   │                   │ 8. JasperExportManager
    │               │                   │                   │    .exportToPdf()│
    │               │                   │                   │─────┐           │
    │               │                   │                   │     │ Generate │
    │               │                   │                   │     │ PDF bytes│
    │               │                   │                   │◄────┘           │
    │               │                   │                   │                 │
    │               │                   │ 9. byte[]         │                 │
    │               │                   │◄──────────────────│                 │
    │               │                   │                   │                 │
    │               │ 10. byte[]        │                   │                 │
    │               │◄──────────────────│                   │                 │
    │               │                   │                   │                 │
    │ 11. 200 OK    │                   │                   │                 │
    │ Content-Type: │                   │                   │                 │
    │ application/pdf│                  │                   │                 │
    │ Content-Disposition:              │                   │                 │
    │ attachment;   │                   │                   │                 │
    │ filename=...  │                   │                   │                 │
    │ [PDF bytes]   │                   │                   │                 │
    │◄──────────────│                   │                   │                 │
    │               │                   │                   │                 │
```

---

## 2. Async Report Sequence

### 2.1 Request Creation

```
┌────────┐     ┌────────────┐     ┌────────────┐     ┌────────────┐     ┌────────────┐
│ Client │     │ Controller │     │  Service   │     │ Repository │     │ RabbitMQ   │
└────────┘     └────────────┘     └────────────┘     └────────────┘     └────────────┘
    │               │                   │                   │                 │
    │ 1. POST       │                   │                   │                 │
    │ /async/request│                   │                   │                 │
    │ {report_type, │                   │                   │                 │
    │  parameters,  │                   │                   │                 │
    │  format}      │                   │                   │                 │
    │──────────────►│                   │                   │                 │
    │               │                   │                   │                 │
    │               │ 2. @Valid         │                   │                 │
    │               │─────┐             │                   │                 │
    │               │◄────┘             │                   │                 │
    │               │                   │                   │                 │
    │               │ 3. createAsyncReportRequest()         │                 │
    │               │──────────────────►│                   │                 │
    │               │                   │                   │                 │
    │               │                   │ 4. validateAsyncReportRequest()     │
    │               │                   │─────┐             │                 │
    │               │                   │◄────┘             │                 │
    │               │                   │                   │                 │
    │               │                   │ 5. create ReportRequest entity      │
    │               │                   │─────┐             │                 │
    │               │                   │     │ id = UUID   │                 │
    │               │                   │     │ status=     │                 │
    │               │                   │     │ PENDING     │                 │
    │               │                   │◄────┘             │                 │
    │               │                   │                   │                 │
    │               │                   │ 6. save(reportRequest)              │
    │               │                   │──────────────────►│                 │
    │               │                   │                   │                 │
    │               │                   │                   │ 7. INSERT       │
    │               │                   │                   │────────────────►│
    │               │                   │                   │                 │
    │               │                   │                   │ 8. OK           │
    │               │                   │                   │◄────────────────│
    │               │                   │                   │                 │
    │               │                   │ 9. ReportRequest  │                 │
    │               │                   │◄──────────────────│                 │
    │               │                   │                   │                 │
    │               │                   │ 10. convertAndSend()                │
    │               │                   │────────────────────────────────────►│
    │               │                   │                   │                 │
    │               │                   │ 11. build AsyncReportResponse       │
    │               │                   │─────┐             │                 │
    │               │                   │     │ requestId   │                 │
    │               │                   │     │ status      │                 │
    │               │                   │     │ estimated   │                 │
    │               │                   │◄────┘             │                 │
    │               │                   │                   │                 │
    │               │ 12. Response      │                   │                 │
    │               │◄──────────────────│                   │                 │
    │               │                   │                   │                 │
    │ 13. 202       │                   │                   │                 │
    │ Accepted      │                   │                   │                 │
    │ {request_id,  │                   │                   │                 │
    │  status:      │                   │                   │                 │
    │  "PENDING"}   │                   │                   │                 │
    │◄──────────────│                   │                   │                 │
    │               │                   │                   │                 │
```

### 2.2 Async Processing (Consumer Side)

```
┌────────────┐     ┌────────────┐     ┌────────────┐     ┌────────────┐     ┌────────┐
│  RabbitMQ  │     │  Consumer  │     │ Repository │     │  MinIO     │     │   DB   │
└────────────┘     └────────────┘     └────────────┘     └────────────┘     └────────┘
       │                   │                   │                 │                │
       │ 1. deliver        │                   │                 │                │
       │    message        │                   │                 │                │
       │──────────────────►│                   │                 │                │
       │                   │                   │                 │                │
       │                   │ 2. findById(requestId)              │                │
       │                   │──────────────────►│                 │                │
       │                   │                   │ 3. SELECT       │                │
       │                   │                   │────────────────►│                │
       │                   │                   │ 4. ReportRequest│                │
       │                   │                   │◄────────────────│                │
       │                   │ 5. ReportRequest  │                 │                │
       │                   │◄──────────────────│                 │                │
       │                   │                   │                 │                │
       │                   │ 6. Update PROCESSING                │                │
       │                   │──────────────────►│                 │                │
       │                   │                   │ 7. UPDATE       │                │
       │                   │                   │────────────────►│                │
       │                   │                   │                 │                │
       │                   │ 8. generateReport()                 │                │
       │                   │─────┐             │                 │                │
       │                   │     │ Parse params│                 │                │
       │                   │     │ Query DB    │                 │                │
       │                   │     │ Generate    │                 │                │
       │                   │◄────┘             │                 │                │
       │                   │                   │                 │                │
       │                   │ 9. uploadToMinio()                  │                │
       │                   │─────────────────────────────────────►│                │
       │                   │                   │                 │                │
       │                   │                   │                 │ 10. PUT        │
       │                   │                   │                 │───────────────►│
       │                   │                   │                 │                │
       │                   │                   │                 │ 11. OK         │
       │                   │                   │                 │◄───────────────│
       │                   │                   │                 │                │
       │                   │ 12. objectName    │                 │                │
       │                   │◄─────────────────────────────────────│                │
       │                   │                   │                 │                │
       │                   │ 13. Update COMPLETED                │                │
       │                   │──────────────────►│                 │                │
       │                   │                   │ 14. UPDATE      │                │
       │                   │                   │────────────────►│                │
       │                   │                   │                 │                │
       │                   │ 15. ack message   │                 │                │
       │──────────────────►│                   │                 │                │
       │                   │                   │                 │                │
```

### 2.3 Status Check & Download

```
┌────────┐     ┌────────────┐     ┌────────────┐     ┌────────────┐     ┌────────┐
│ Client │     │ Controller │     │  Service   │     │ Repository │     │ MinIO  │
└────────┘     └────────────┘     └────────────┘     └────────────┘     └────────┘
    │               │                   │                   │                 │
    │ 1. GET        │                   │                   │                 │
    │ /async/{id}   │                   │                   │                 │
    │──────────────►│                   │                   │                 │
    │               │                   │                   │                 │
    │               │ 2. getStatus(requestId)               │                 │
    │               │──────────────────►│                   │                 │
    │               │                   │                   │                 │
    │               │                   │ 3. findById(id)   │                 │
    │               │                   │──────────────────►│                 │
    │               │                   │                   │                 │
    │               │                   │ 4. ReportRequest  │                 │
    │               │                   │◄──────────────────│                 │
    │               │                   │                   │                 │
    │               │                   │ 5. buildStatusResponse()            │
    │               │                   │─────┐             │                 │
    │               │                   │     │ Include     │                 │
    │               │                   │     │ download_url│                 │
    │               │                   │     │ if COMPLETED│                 │
    │               │                   │◄────┘             │                 │
    │               │                   │                   │                 │
    │               │ 6. Response       │                   │                 │
    │               │◄──────────────────│                   │                 │
    │               │                   │                   │                 │
    │ 7. 200 OK     │                   │                   │                 │
    │ {status:      │                   │                   │                 │
    │  "COMPLETED", │                   │                   │                 │
    │  download_url}│                   │                   │                 │
    │◄──────────────│                   │                   │                 │
    │               │                   │                   │                 │
    │               │                   │                   │                 │
    │ 8. GET        │                   │                   │                 │
    │ /async/{id}/  │                   │                   │                 │
    │ download      │                   │                   │                 │
    │──────────────►│                   │                   │                 │
    │               │                   │                   │                 │
    │               │ 9. getDownloadUrl(requestId)          │                 │
    │               │──────────────────►│                   │                 │
    │               │                   │                   │                 │
    │               │                   │ 10. findById(id)  │                 │
    │               │                   │──────────────────►│                 │
    │               │                   │                   │                 │
    │               │                   │ 11. ReportRequest │                 │
    │               │                   │◄──────────────────│                 │
    │               │                   │                   │                 │
    │               │                   │ 12. getPresignedUrl(objectName)     │
    │               │                   │────────────────────────────────────►│
    │               │                   │                   │                 │
    │               │                   │ 13. presigned URL │                 │
    │               │                   │◄────────────────────────────────────│
    │               │                   │                   │                 │
    │               │ 14. Response      │                   │                 │
    │               │◄──────────────────│                   │                 │
    │               │                   │                   │                 │
    │ 15. 200 OK    │                   │                   │                 │
    │ {download_url,│                   │                   │                 │
    │  expires_at}  │                   │                   │                 │
    │◄──────────────│                   │                   │                 │
    │               │                   │                   │                 │
```

---

## 3. Schedule Report Sequence

### 3.1 Create Schedule

```
┌────────┐     ┌────────────┐     ┌────────────┐     ┌────────────┐
│ Client │     │ Controller │     │  Service   │     │ Repository │
└────────┘     └────────────┘     └────────────┘     └────────────┘
    │               │                   │                   │
    │ 1. POST       │                   │                   │
    │ /schedules    │                   │                   │
    │──────────────►│                   │                   │
    │               │                   │                   │
    │               │ 2. @Valid         │                   │
    │               │─────┐             │                   │
    │               │◄────┘             │                   │
    │               │                   │                   │
    │               │ 3. createSchedule()                   │
    │               │──────────────────►│                   │
    │               │                   │                   │
    │               │                   │ 4. validateSchedule()               │
    │               │                   │─────┐             │
    │               │                   │     │ Validate    │
    │               │                   │     │ cron expr   │
    │               │                   │     │ Validate    │
    │               │                   │     │ recipients  │
    │               │                   │◄────┘             │
    │               │                   │                   │
    │               │                   │ 5. calculateNextRun()               │
    │               │                   │─────┐             │
    │               │                   │     │ Parse cron  │
    │               │                   │     │ Calculate   │
    │               │                   │◄────┘             │
    │               │                   │                   │
    │               │                   │ 6. save(schedule) │
    │               │                   │──────────────────►│
    │               │                   │                   │
    │               │                   │ 7. Schedule       │
    │               │                   │◄──────────────────│
    │               │                   │                   │
    │               │ 8. Response       │                   │
    │               │◄──────────────────│                   │
    │               │                   │                   │
    │ 9. 201        │                   │                   │
    │ Created       │                   │                   │
    │◄──────────────│                   │                   │
    │               │                   │                   │
```

### 3.2 Scheduled Execution

```
┌─────────────┐     ┌────────────┐     ┌────────────┐     ┌────────────┐     ┌────────┐
│  Scheduler  │     │  Service   │     │ Repository │     │  MinIO     │     │ Email  │
└─────────────┘     └────────────┘     └────────────┘     └────────────┘     └────────┘
       │                   │                   │                 │                │
       │ 1. @Scheduled     │                   │                 │                │
       │    (every minute) │                   │                 │                │
       │──────────────────►│                   │                 │                │
       │                   │                   │                 │                │
       │                   │ 2. findByIsEnabledTrueAndNextRunAtLessThanEqual(now)
       │                   │──────────────────►│                 │                │
       │                   │                   │                 │                │
       │                   │                   │ 3. List<Schedule>               │
       │                   │                   │◄────────────────│                │
       │                   │                   │                 │                │
       │                   │ 4. For each schedule:              │                │
       │                   │    ┌──────────────┤                │                │
       │                   │    │              │                │                │
       │                   │    │ 5. acquireLock()              │                │
       │                   │    │─────────────────────────────────────────────────►│
       │                   │    │              │                │                │
       │                   │    │ 6. locked=true               │                │
       │                   │    │◄─────────────────────────────────────────────────│
       │                   │    │              │                │                │
       │                   │    │ 7. generateReport()          │                │
       │                   │    │─────┐        │                │                │
       │                   │    │     │ Query  │                │                │
       │                   │    │     │ data   │                │                │
       │                   │    │     │ Generate               │                │
       │                   │    │◄────┘        │                │                │
       │                   │    │              │                │                │
       │                   │    │ 8. uploadToMinio()           │                │
       │                   │    │──────────────────────────────►│                │
       │                   │    │              │                │                │
       │                   │    │ 9. downloadUrl               │                │
       │                   │    │◄──────────────────────────────│                │
       │                   │    │              │                │                │
       │                   │    │ 10. sendEmail()              │                │
       │                   │    │────────────────────────────────────────────────►│
       │                   │    │              │                │                │
       │                   │    │ 11. Update schedule          │                │
       │                   │    │─────┐        │                │                │
       │                   │    │     │ last_  │                │                │
       │                   │    │     │ run_at │                │                │
       │                   │    │     │ next_  │                │                │
       │                   │    │     │ run_at │                │                │
       │                   │    │◄────┘        │                │                │
       │                   │    │              │                │                │
       │                   │    │ 12. save(schedule)           │                │
       │                   │    │──────────────────────────────►│                │
       │                   │    │              │                │                │
       │                   │    │ 13. releaseLock()            │                │
       │                   │    │────────────────────────────────────────────────►│
       │                   │    │              │                │                │
       │                   │    └──────────────┤                │                │
       │                   │                   │                │                │
       │ 14. Complete      │                   │                │                │
       │◄──────────────────│                   │                │                │
       │                   │                   │                │                │
```

---

## 4. Export/Import Sequence

### 4.1 Product Export

```
┌────────┐     ┌────────────┐     ┌────────────┐     ┌────────────┐
│ Client │     │ Controller │     │  Service   │     │ Repository │
└────────┘     └────────────┘     └────────────┘     └────────────┘
    │               │                   │                   │
    │ 1. GET        │                   │                   │
    │ /products/    │                   │                   │
    │ export?format │                   │                   │
    │ =excel        │                   │                   │
    │──────────────►│                   │                   │
    │               │                   │                   │
    │               │ 2. exportProducts()                   │
    │               │──────────────────►│                   │
    │               │                   │                   │
    │               │                   │ 3. buildSpecification()             │
    │               │                   │─────┐             │
    │               │                   │◄────┘             │
    │               │                   │                   │
    │               │                   │ 4. findAll(spec)  │
    │               │                   │──────────────────►│
    │               │                   │                   │
    │               │                   │                   │ 5. SELECT       │
    │               │                   │                   │────────────────►│
    │               │                   │                   │ 6. List<Product>│
    │               │                   │                   │◄────────────────│
    │               │                   │                   │
    │               │                   │ 7. List<Product>  │
    │               │                   │◄──────────────────│
    │               │                   │                   │
    │               │                   │ 8. createWorkbook()│
    │               │                   │─────┐             │
    │               │                   │     │ Create sheet│
    │               │                   │     │ Add headers │
    │               │                   │     │ Add data    │
    │               │                   │     │ Auto-size   │
    │               │                   │◄────┘             │
    │               │                   │                   │
    │               │ 9. byte[]         │                   │
    │               │◄──────────────────│                   │
    │               │                   │                   │
    │ 10. 200 OK    │                   │                   │
    │ Content-Type: │                   │                   │
    │ application/  │                   │                   │
    │ vnd.ms-excel  │                   │                   │
    │ [Excel bytes] │                   │                   │
    │◄──────────────│                   │                   │
    │               │                   │                   │
```

### 4.2 Product Import

```
┌────────┐     ┌────────────┐     ┌────────────┐     ┌────────────┐
│ Client │     │ Controller │     │  Service   │     │ Repository │
└────────┘     └────────────┘     └────────────┘     └────────────┘
    │               │                   │                   │
    │ 1. POST       │                   │                   │
    │ /products/    │                   │                   │
    │ import        │                   │                   │
    │ (file)        │                   │                   │
    │──────────────►│                   │                   │
    │               │                   │                   │
    │               │ 2. importProducts(file, options)       │
    │               │──────────────────►│                   │
    │               │                   │                   │
    │               │                   │ 3. parseFile()    │
    │               │                   │─────┐             │
    │               │                   │     │ Read Excel  │
    │               │                   │     │ Parse rows  │
    │               │                   │◄────┘             │
    │               │                   │                   │
    │               │                   │ 4. validateRows() │
    │               │                   │─────┐             │
    │               │                   │     │ Check SKU   │
    │               │                   │     │ Check refs  │
    │               │                   │     │ Check dupes │
    │               │                   │◄────┘             │
    │               │                   │                   │
    │               │                   │ 5. For each valid row:              │
    │               │                   │    ┌──────────────┤                │
    │               │                   │    │              │                │
    │               │                   │    │ 6. findBySku()                │
    │               │                   │    │──────────────────────────────►│
    │               │                   │    │              │                │
    │               │                   │    │ 7. Optional<Product>          │
    │               │                   │    │◄──────────────────────────────│
    │               │                   │    │              │                │
    │               │                   │    │ 8. create or update           │
    │               │                   │    │─────┐        │                │
    │               │                   │    │◄────┘        │                │
    │               │                   │    │              │                │
    │               │                   │    │ 9. save()     │                │
    │               │                   │    │──────────────────────────────►│
    │               │                   │    │              │                │
    │               │                   │    └──────────────┤                │
    │               │                   │                   │                │
    │               │                   │ 10. buildResult() │                │
    │               │                   │─────┐             │                │
    │               │                   │     │ success     │                │
    │               │                   │     │ error       │                │
    │               │                   │     │ skip counts │                │
    │               │                   │◄────┘             │                │
    │               │                   │                   │                │
    │               │ 11. Response      │                   │                │
    │               │◄──────────────────│                   │                │
    │               │                   │                   │                │
    │ 12. 200 OK    │                   │                   │                │
    │ {success_count│                   │                   │                │
    │  error_count, │                   │                   │                │
    │  errors:...}  │                   │                   │                │
    │◄──────────────│                   │                   │                │
    │               │                   │                   │                │
```

---

## 5. Error Handling Sequence

### 5.1 Validation Error

```
┌────────┐     ┌────────────┐     ┌────────────┐
│ Client │     │ Controller │     │  Service   │
└────────┘     └────────────┘     └────────────┘
    │               │                   │
    │ 1. POST       │                   │
    │ (invalid)     │                   │
    │──────────────►│                   │
    │               │                   │
    │               │ 2. @Valid         │
    │               │─────┐             │
    │               │     │ Validation  │
    │               │     │ fails       │
    │               │◄────┘             │
    │               │                   │
    │ 3. 400        │                   │
    │ Bad Request   │                   │
    │ {success:false│                   │
    │  error_code:  │                   │
    │  VALIDATION_  │                   │
    │  ERROR,       │                   │
    │  field_errors:│                   │
    │  [...]}       │                   │
    │◄──────────────│                   │
    │               │                   │
```

### 5.2 Business Error

```
┌────────┐     ┌────────────┐     ┌────────────┐     ┌────────────┐
│ Client │     │ Controller │     │  Service   │     │ Repository │
└────────┘     └────────────┘     └────────────┘     └────────────┘
    │               │                   │                   │
    │ 1. POST       │                   │                   │
    │──────────────►│                   │                   │
    │               │                   │                   │
    │               │ 2. process()      │                   │
    │               │──────────────────►│                   │
    │               │                   │                   │
    │               │                   │ 3. query()        │
    │               │                   │──────────────────►│
    │               │                   │                   │
    │               │                   │ 4. empty result   │
    │               │                   │◄──────────────────│
    │               │                   │                   │
    │               │                   │ 5. throw NoDataFoundException       │
    │               │◄──────────────────│                   │
    │               │                   │                   │
    │ 6. 422        │                   │                   │
    │ Unprocessable │                   │                   │
    │ {success:false│                   │                   │
    │  error_code:  │                   │                   │
    │  NO_DATA_FOUND│                   │                   │
    │  message:...} │                   │                   │
    │◄──────────────│                   │                   │
    │               │                   │                   │
```

---

## 6. Documentation Index

- [01-end-to-end-flow.md](01-end-to-end-flow.md) - Flow tổng quan
- [02-on-demand-report-flow.md](02-on-demand-report-flow.md) - Chi tiết on-demand
- [03-async-report-flow.md](03-async-report-flow.md) - Chi tiết async
- [04-schedule-report-flow.md](04-schedule-report-flow.md) - Chi tiết scheduled
- [05-export-import-flow.md](05-export-import-flow.md) - Chi tiết export/import
- [07-data-flow-diagrams.md](07-data-flow-diagrams.md) - Data flow
- [08-integration-points.md](08-integration-points.md) - Integration
