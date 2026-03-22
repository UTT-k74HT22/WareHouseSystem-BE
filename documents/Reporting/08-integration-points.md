# Integration Points - Chi Tiết

> Module: WHS-52 (Report Module)
> Updated: 2026-03-22

---

## 1. System Integration Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                      System Integration Architecture                        │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│                         ┌─────────────────┐                                 │
│                         │   Client App    │                                 │
│                         │  (Frontend/FE)  │                                 │
│                         └────────┬────────┘                                 │
│                                  │                                          │
│                                  │ REST API                                 │
│                                  ▼                                          │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                                                                     │   │
│  │                    WMS Backend (Spring Boot)                        │   │
│  │                                                                     │   │
│  │  ┌─────────────────────────────────────────────────────────────┐   │   │
│  │  │                     Report Module                           │   │   │
│  │  │                                                             │   │   │
│  │  │  ┌───────────┐  ┌───────────┐  ┌───────────┐  ┌─────────┐ │   │   │
│  │  │  │ Controller│  │  Service  │  │ Repository│  │Generator│ │   │   │
│  │  │  └───────────┘  └───────────┘  └───────────┘  └─────────┘ │   │   │
│  │  │                                                             │   │   │
│  │  └─────────────────────────────────────────────────────────────┘   │   │
│  │                                                                     │   │
│  │  ┌─────────────────────────────────────────────────────────────┐   │   │
│  │  │                  Internal Module Dependencies               │   │   │
│  │  │                                                             │   │   │
│  │  │  ┌───────────┐  ┌───────────┐  ┌───────────┐  ┌─────────┐ │   │   │
│  │  │  │ Inventory │  │ Inbound   │  │ Outbound  │  │  Batch  │ │   │   │
│  │  │  │ Module    │  │ Module    │  │ Module    │  │ Module  │ │   │   │
│  │  │  └───────────┘  └───────────┘  └───────────┘  └─────────┘ │   │   │
│  │  │                                                             │   │   │
│  │  │  ┌───────────┐  ┌───────────┐  ┌───────────┐              │   │   │
│  │  │  │ Warehouse │  │  Product  │  │   Email   │              │   │   │
│  │  │  │ Module    │  │  Module   │  │  Module   │              │   │   │
│  │  │  └───────────┘  └───────────┘  └───────────┘              │   │   │
│  │  │                                                             │   │   │
│  │  └─────────────────────────────────────────────────────────────┘   │   │
│  │                                                                     │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                  │                                          │
│           ┌──────────────────────┼──────────────────────┐                  │
│           │                      │                      │                  │
│           ▼                      ▼                      ▼                  │
│  ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐       │
│  │   MySQL 8       │    │   RabbitMQ      │    │   MinIO         │       │
│  │   (Database)    │    │   (Message)     │    │   (Storage)     │       │
│  └─────────────────┘    └─────────────────┘    └─────────────────┘       │
│                                  │                                          │
│                                  ▼                                          │
│                         ┌─────────────────┐                                 │
│                         │   Redis         │                                 │
│                         │   (Cache)       │                                 │
│                         └─────────────────┘                                 │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Internal Module Integrations

### 2.1 Inventory Module Integration

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                     Inventory Module Integration                            │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  Report Module                                              Inventory Module│
│  ┌─────────────────────┐                               ┌─────────────────┐ │
│  │                     │                               │                 │ │
│  │  ReportService      │──── findCurrentStock() ──────►│ InventoryService│ │
│  │                     │                               │                 │ │
│  │                     │──── getStockLevels() ────────►│                 │ │
│  │                     │                               │                 │ │
│  │                     │──── getInventoryByProduct() ─►│                 │ │
│  │                     │                               │                 │ │
│  └─────────────────────┘                               └─────────────────┘ │
│                                                                             │
│  Data Used:                                                                 │
│  ├── inventory table: product_id, warehouse_id, location_id               │
│  ├── inventory quantities: on_hand_quantity, reserved_quantity             │
│  │   quarantine_quantity                                                   │
│  └── inventory relations: products, warehouses, locations, batches        │
│                                                                             │
│  Reports Dependent:                                                         │
│  ├── Current Stock Report (R1)                                            │
│  ├── Stock Valuation Report (R2)                                          │
│  ├── Low Stock Report (R5)                                                │
│  └── Expiring Batches Report (R6)                                         │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 2.2 Inbound Module Integration

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                       Inbound Module Integration                            │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  Report Module                                              Inbound Module  │
│  ┌─────────────────────┐                               ┌─────────────────┐ │
│  │                     │                               │                 │ │
│  │  ReportService      │──── getReceiptsByDate() ─────►│ PurchaseOrder   │ │
│  │                     │                               │ Service         │ │
│  │                     │──── getReceiptLines() ───────►│                 │ │
│  │                     │                               │                 │ │
│  │                     │──── getSupplierOrders() ─────►│ InboundReceipt  │ │
│  │                     │                               │ Service         │ │
│  └─────────────────────┘                               └─────────────────┘ │
│                                                                             │
│  Data Used:                                                                 │
│  ├── purchase_orders: supplier_id, order_date, status, total_amount       │
│  ├── purchase_order_lines: product_id, quantity, unit_price               │
│  ├── inbound_receipts: receipt_date, purchase_order_id, status            │
│  └── inbound_receipt_lines: product_id, received_quantity, batch_id       │
│                                                                             │
│  Reports Dependent:                                                         │
│  ├── Batch Traceability Report (R4)                                       │
│  └── Movements Report (R3) - INBOUND movements                            │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 2.3 Outbound Module Integration

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                       Outbound Module Integration                           │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  Report Module                                              Outbound Module │
│  ┌─────────────────────┐                               ┌─────────────────┐ │
│  │                     │                               │                 │ │
│  │  ReportService      │──── getShipmentsByDate() ────►│ SalesOrder      │ │
│  │                     │                               │ Service         │ │
│  │                     │──── getShipmentLines() ──────►│                 │ │
│  │                     │                               │                 │ │
│  │                     │──── getCustomerOrders() ─────►│ Outbound        │ │
│  │                     │                               │ Shipment Service│ │
│  └─────────────────────┘                               └─────────────────┘ │
│                                                                             │
│  Data Used:                                                                 │
│  ├── sales_orders: customer_id, order_date, status, total_amount          │
│  ├── sales_order_lines: product_id, quantity, unit_price                  │
│  ├── outbound_shipments: shipment_date, sales_order_id, status            │
│  └── outbound_shipment_lines: product_id, shipped_quantity, batch_id      │
│                                                                             │
│  Reports Dependent:                                                         │
│  ├── Batch Traceability Report (R4)                                       │
│  └── Movements Report (R3) - OUTBOUND movements                           │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 2.4 Batch Module Integration

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         Batch Module Integration                            │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  Report Module                                              Batch Module    │
│  ┌─────────────────────┐                               ┌─────────────────┐ │
│  │                     │                               │                 │ │
│  │  ReportService      │──── getBatchById() ──────────►│ BatchService    │ │
│  │                     │                               │                 │ │
│  │                     │──── getBatchesByProduct() ───►│                 │ │
│  │                     │                               │                 │ │
│  │                     │──── getExpiringBatches() ────►│                 │ │
│  │                     │                               │                 │ │
│  └─────────────────────┘                               └─────────────────┘ │
│                                                                             │
│  Data Used:                                                                 │
│  ├── batches: batch_number, manufacture_date, expiry_date, status         │
│  └── batch relations: product_id, supplier_id, purchase_order_id          │
│                                                                             │
│  Reports Dependent:                                                         │
│  ├── Batch Traceability Report (R4)                                       │
│  └── Expiring Batches Report (R6)                                         │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 2.5 Email Module Integration

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         Email Module Integration                            │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  Report Module                                              Email Module    │
│  ┌─────────────────────┐                               ┌─────────────────┐ │
│  │                     │                               │                 │ │
│  │  ReportScheduler    │──── sendEmail() ─────────────►│ EmailService    │ │
│  │                     │                               │                 │ │
│  │                     │──── sendAsync() ─────────────►│ EmailProducer   │ │
│  │                     │                               │ Service         │ │
│  └─────────────────────┘                               └─────────────────┘ │
│                                                                             │
│  Integration Pattern:                                                       │
│  ├── Sync: Direct email sending for immediate notifications               │
│  └── Async: Via RabbitMQ for scheduled report notifications               │
│                                                                             │
│  Email Types Used:                                                          │
│  └── REPORT_EXPORT: Report generation notification                        │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 3. External System Integrations

### 3.1 MinIO Integration

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           MinIO Integration                                 │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  Configuration:                                                             │
│  ├── Endpoint: http://minio:9000                                          │
│  ├── Access Key: From environment variable                                │
│  ├── Secret Key: From environment variable                                │
│  └── Bucket: whs-reports                                                  │
│                                                                             │
│  Operations:                                                                │
│  ├── PUT: Upload generated report files                                   │
│  ├── GET: Download report files                                           │
│  ├── DELETE: Clean up expired files                                       │
│  └── GET_PRESIGNED_URL: Generate temporary download URLs                  │
│                                                                             │
│  File Structure:                                                            │
│  └── whs-reports/                                                         │
│      ├── reports/                                                         │
│      │   └── {year}/{month}/{report_type}/{request_id}.{ext}             │
│      └── imports/                                                         │
│          └── {year}/{month}/{type}/{import_id}.{ext}                     │
│                                                                             │
│  Retention Policy:                                                          │
│  ├── Async reports: 7 days                                                │
│  ├── Scheduled reports: 30 days                                           │
│  └── Import files: 3 days                                                 │
│                                                                             │
│  Integration Code:                                                          │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ @Configuration                                                      │   │
│  │ public class MinioConfig {                                          │   │
│  │     @Bean                                                           │   │
│  │     public MinioClient minioClient(MinioProperties props) {         │   │
│  │         return MinioClient.builder()                                │   │
│  │             .endpoint(props.getEndpoint())                          │   │
│  │             .credentials(props.getAccessKey(), props.getSecretKey())│   │
│  │             .build();                                               │   │
│  │     }                                                               │   │
│  │ }                                                                   │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 3.2 RabbitMQ Integration

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          RabbitMQ Integration                               │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  Configuration:                                                             │
│  ├── Host: rabbitmq                                                       │
│  ├── Port: 5672                                                           │
│  ├── Username: From environment variable                                  │
│  └── Password: From environment variable                                  │
│                                                                             │
│  Exchange & Queue Topology:                                                 │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                                                                     │   │
│  │  ┌───────────────────────┐                                          │   │
│  │  │ whs.report.exchange   │ (Topic Exchange)                        │   │
│  │  └───────────┬───────────┘                                          │   │
│  │              │                                                      │   │
│  │     ┌────────┴────────┬─────────────┐                               │   │
│  │     │                 │             │                               │   │
│  │     ▼                 ▼             ▼                               │   │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐                          │   │
│  │  │ report.  │  │ report.  │  │ report.  │                          │   │
│  │  │ generate │  │ notif.   │  │ dlq      │                          │   │
│  │  │ .queue   │  │ .queue   │  │          │                          │   │
│  │  └──────────┘  └──────────┘  └──────────┘                          │   │
│  │                                                                     │   │
│  │  Routing Keys:                                                      │   │
│  │  ├── report.generate → report.generate.queue                       │   │
│  │  ├── report.notification → report.notification.queue               │   │
│  │  └── report.dlq → report.dlq (for failed messages)                 │   │
│  │                                                                     │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  Message Format:                                                            │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ {                                                                   │   │
│  │   "request_id": "uuid",                                            │   │
│  │   "report_type": "CURRENT_STOCK",                                  │   │
│  │   "parameters": {                                                  │   │
│  │     "warehouse_id": "uuid",                                        │   │
│  │     "include_zero_stock": false                                    │   │
│  │   },                                                               │   │
│  │   "format": "EXCEL",                                               │   │
│  │   "timestamp": "2026-03-22T10:30:00Z"                              │   │
│  │ }                                                                   │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  Integration Code:                                                          │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ @Configuration                                                      │   │
│  │ public class RabbitMQReportConfig {                                 │   │
│  │     public static final String REPORT_EXCHANGE = "whs.report.exchange";│ │
│  │     public static final String REPORT_GENERATE_QUEUE = "...";       │   │
│  │                                                                     │   │
│  │     @Bean                                                           │   │
│  │     public TopicExchange reportExchange() {                         │   │
│  │         return new TopicExchange(REPORT_EXCHANGE);                  │   │
│  │     }                                                               │   │
│  │                                                                     │   │
│  │     @Bean                                                           │   │
│  │     public Queue reportGenerateQueue() {                            │   │
│  │         return QueueBuilder.durable(REPORT_GENERATE_QUEUE)          │   │
│  │             .withArgument("x-dead-letter-exchange", "...")          │   │
│  │             .build();                                               │   │
│  │     }                                                               │   │
│  │ }                                                                   │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 3.3 Redis Integration

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                            Redis Integration                                │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  Configuration:                                                             │
│  ├── Host: redis                                                          │
│  ├── Port: 6379                                                           │
│  └── Password: From environment variable                                  │
│                                                                             │
│  Use Cases:                                                                 │
│  ├── 1. Report Result Caching                                             │
│  │   ├── Key: report:cache:{hash}                                        │
│  │   ├── Value: Serialized report data                                   │
│  │   ├── TTL: 5 minutes                                                  │
│  │   └── Purpose: Avoid regenerating same report within short period     │
│  │                                                                        │
│  ├── 2. Rate Limiting                                                     │
│  │   ├── Key: report:rate:{user_id}                                     │
│  │   ├── Value: Request count                                            │
│  │   ├── TTL: 1 minute                                                   │
│  │   └── Purpose: Limit on-demand report requests per user               │
│  │                                                                        │
│  ├── 3. Distributed Locks                                                 │
│  │   ├── Key: report:lock:{schedule_id}                                 │
│  │   ├── Value: Instance ID                                              │
│  │   ├── TTL: 10 minutes                                                 │
│  │   └── Purpose: Prevent duplicate schedule execution                   │
│  │                                                                        │
│  └── 4. Async Progress Tracking                                           │
│      ├── Key: report:progress:{request_id}                               │
│      ├── Value: Progress percentage (0-100)                               │
│      ├── TTL: 30 minutes                                                  │
│      └── Purpose: Track async report generation progress                  │
│                                                                             │
│  Integration Code:                                                          │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ @Configuration                                                      │   │
│  │ public class RedisConfig {                                          │   │
│  │     @Bean                                                           │   │
│  │     public RedisTemplate<String, Object> redisTemplate(             │   │
│  │             RedisConnectionFactory factory) {                       │   │
│  │         RedisTemplate<String, Object> template = new RedisTemplate<>();│ │
│  │         template.setConnectionFactory(factory);                     │   │
│  │         template.setKeySerializer(new StringRedisSerializer());    │   │
│  │         template.setValueSerializer(new Jackson2JsonRedisSerializer<>(│ │
│  │             Object.class));                                         │   │
│  │         return template;                                            │   │
│  │     }                                                               │   │
│  │ }                                                                   │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 3.4 MySQL Database Integration

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         MySQL Database Integration                          │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  Configuration:                                                             │
│  ├── URL: jdbc:mysql://localhost:3306/warehouse_system                    │
│  ├── Username: whs_user                                                   │
│  ├── Password: From environment variable                                  │
│  └── Driver: com.mysql.cj.jdbc.Driver                                     │
│                                                                             │
│  Tables Used by Report Module:                                             │
│  ├── report_requests (own table)                                          │
│  │   ├── id, report_type, parameters, format                             │
│  │   ├── status, priority, requested_by, requested_at                    │
│  │   ├── started_at, completed_at, file_url, file_size                   │
│  │   └── error_message, retry_count, max_retries                         │
│  │                                                                        │
│  ├── report_schedules (own table)                                         │
│  │   ├── id, name, description, report_type                              │
│  │   ├── parameters, format, cron_expression, timezone                   │
│  │   ├── is_enabled, last_run_at, next_run_at                            │
│  │   ├── recipients, created_by                                          │
│  │   └── created_at, updated_at                                          │
│  │                                                                        │
│  ├── inventory (read-only for reports)                                    │
│  ├── stock_movements (read-only for reports)                              │
│  ├── batches (read-only for reports)                                      │
│  ├── products (read-only for reports)                                     │
│  ├── warehouses (read-only for reports)                                   │
│  ├── locations (read-only for reports)                                    │
│  ├── purchase_orders (read-only for reports)                              │
│  ├── inbound_receipts (read-only for reports)                             │
│  ├── sales_orders (read-only for reports)                                 │
│  └── outbound_shipments (read-only for reports)                           │
│                                                                             │
│  Flyway Migrations:                                                         │
│  └── V0XX__create_report_tables.sql                                       │
│                                                                             │
│  Indexes for Report Queries:                                               │
│  ├── inventory(product_id, warehouse_id)                                  │
│  ├── stock_movements(movement_date, product_id)                           │
│  ├── batches(expiry_date)                                                 │
│  ├── report_requests(status, requested_by)                                │
│  └── report_schedules(is_enabled, next_run_at)                            │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 4. External API Integrations

### 4.1 JasperReports Integration

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                      JasperReports Integration                              │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  Dependency:                                                                │
│  ├── Group: net.sf.jasperreports                                          │
│  ├── Artifact: jasperreports                                              │
│  └── Version: 6.21.5                                                      │
│                                                                             │
│  Template Location:                                                         │
│  └── src/main/resources/templates/reports/*.jrxml                         │
│                                                                             │
│  Templates:                                                                 │
│  ├── current_stock.jrxml                                                  │
│  ├── stock_valuation.jrxml                                                │
│  ├── movements.jrxml                                                      │
│  ├── batch_traceability.jrxml                                             │
│  ├── low_stock.jrxml                                                      │
│  └── expiring_batches.jrxml                                               │
│                                                                             │
│  Integration Code:                                                          │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ @Service                                                            │   │
│  │ public class JasperReportGenerator implements ReportGenerator {     │   │
│  │                                                                     │   │
│  │     @Override                                                       │   │
│  │     public byte[] generatePdf(ReportData data, String templateName) │   │
│  │             throws ReportGenerationException {                      │   │
│  │         try {                                                       │   │
│  │             // 1. Load template                                     │   │
│  │             InputStream templateStream = getClass()                 │   │
│  │                 .getResourceAsStream("/templates/reports/"          │   │
│  │                     + templateName + ".jrxml");                     │   │
│  │                                                                     │   │
│  │             // 2. Compile template                                  │   │
│  │             JasperReport jasperReport = JasperCompileManager        │   │
│  │                 .compileReport(templateStream);                     │   │
│  │                                                                     │   │
│  │             // 3. Prepare data source                               │   │
│  │             JRBeanCollectionDataSource dataSource =                 │   │
│  │                 new JRBeanCollectionDataSource(data.getItems());    │   │
│  │                                                                     │   │
│  │             // 4. Fill parameters                                   │   │
│  │             Map<String, Object> parameters = new HashMap<>();       │   │
│  │             parameters.put("REPORT_TITLE", data.getTitle());        │   │
│  │             parameters.put("GENERATED_DATE", new Date());           │   │
│  │                                                                     │   │
│  │             // 5. Fill report                                       │   │
│  │             JasperPrint jasperPrint = JasperFillManager             │   │
│  │                 .fillReport(jasperReport, parameters, dataSource);  │   │
│  │                                                                     │   │
│  │             // 6. Export to PDF                                     │   │
│  │             return JasperExportManager                              │   │
│  │                 .exportReportToPdf(jasperPrint);                    │   │
│  │                                                                     │   │
│  │         } catch (JRException e) {                                  │   │
│  │             throw new ReportGenerationException(                    │   │
│  │                 "Failed to generate PDF report", e);               │   │
│  │         }                                                           │   │
│  │     }                                                               │   │
│  │ }                                                                   │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 4.2 Apache POI Integration

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         Apache POI Integration                              │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  Dependencies:                                                              │
│  ├── Group: org.apache.poi                                                │
│  ├── Artifact: poi                                                        │
│  └── Version: 5.2.5                                                       │
│                                                                             │
│  ├── Group: org.apache.poi                                                │
│  ├── Artifact: poi-ooxml                                                  │
│  └── Version: 5.2.5                                                       │
│                                                                             │
│  Integration Code:                                                          │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ @Service                                                            │   │
│  │ public class ExcelReportGenerator implements ReportGenerator {      │   │
│  │                                                                     │   │
│  │     @Override                                                       │   │
│  │     public byte[] generateExcel(ReportData data, String sheetName)  │   │
│  │             throws ReportGenerationException {                      │   │
│  │         try (Workbook workbook = new XSSFWorkbook()) {              │   │
│  │                                                                     │   │
│  │             // 1. Create sheet                                      │   │
│  │             Sheet sheet = workbook.createSheet(sheetName);          │   │
│  │                                                                     │   │
│  │             // 2. Create header style                               │   │
│  │             CellStyle headerStyle = createHeaderStyle(workbook);    │   │
│  │                                                                     │   │
│  │             // 3. Write headers                                     │   │
│  │             Row headerRow = sheet.createRow(0);                     │   │
│  │             String[] headers = data.getHeaders();                   │   │
│  │             for (int i = 0; i < headers.length; i++) {             │   │
│  │                 Cell cell = headerRow.createCell(i);                │   │
│  │                 cell.setCellValue(headers[i]);                      │   │
│  │                 cell.setCellStyle(headerStyle);                     │   │
│  │             }                                                       │   │
│  │                                                                     │   │
│  │             // 4. Write data rows                                   │   │
│  │             int rowNum = 1;                                         │   │
│  │             for (ReportItem item : data.getItems()) {              │   │
│  │                 Row row = sheet.createRow(rowNum++);                │   │
│  │                 populateRow(row, item);                            │   │
│  │             }                                                       │   │
│  │                                                                     │   │
│  │             // 5. Auto-size columns                                 │   │
│  │             for (int i = 0; i < headers.length; i++) {             │   │
│  │                 sheet.autoSizeColumn(i);                            │   │
│  │             }                                                       │   │
│  │                                                                     │   │
│  │             // 6. Write to byte array                               │   │
│  │             ByteArrayOutputStream bos = new ByteArrayOutputStream();│   │
│  │             workbook.write(bos);                                    │   │
│  │             return bos.toByteArray();                               │   │
│  │                                                                     │   │
│  │         } catch (IOException e) {                                  │   │
│  │             throw new ReportGenerationException(                    │   │
│  │                 "Failed to generate Excel report", e);             │   │
│  │         }                                                           │   │
│  │     }                                                               │   │
│  │ }                                                                   │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 4.3 OpenCSV Integration

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          OpenCSV Integration                                │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  Dependency:                                                                │
│  ├── Group: com.opencsv                                                   │
│  ├── Artifact: opencsv                                                    │
│  └── Version: 5.9                                                         │
│                                                                             │
│  Integration Code:                                                          │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ @Service                                                            │   │
│  │ public class CsvReportGenerator implements ReportGenerator {        │   │
│  │                                                                     │   │
│  │     @Override                                                       │   │
│  │     public byte[] generateCsv(ReportData data)                      │   │
│  │             throws ReportGenerationException {                      │   │
│  │         try (ByteArrayOutputStream bos = new ByteArrayOutputStream();│  │
│  │              OutputStreamWriter writer = new OutputStreamWriter(    │   │
│  │                  bos, StandardCharsets.UTF_8);                      │   │
│  │              CSVWriter csvWriter = new CSVWriter(writer)) {         │   │
│  │                                                                     │   │
│  │             // Write BOM for Excel compatibility                    │   │
│  │             bos.write(0xEF);                                        │   │
│  │             bos.write(0xBB);                                        │   │
│  │             bos.write(0xBF);                                        │   │
│  │                                                                     │   │
│  │             // Write headers                                        │   │
│  │             csvWriter.writeNext(data.getHeaders());                 │   │
│  │                                                                     │   │
│  │             // Write data rows                                      │   │
│  │             for (ReportItem item : data.getItems()) {              │   │
│  │                 csvWriter.writeNext(item.toArray());               │   │
│  │             }                                                       │   │
│  │                                                                     │   │
│  │             csvWriter.flush();                                      │   │
│  │             return bos.toByteArray();                               │   │
│  │                                                                     │   │
│  │         } catch (IOException e) {                                  │   │
│  │             throw new ReportGenerationException(                    │   │
│  │                 "Failed to generate CSV report", e);               │   │
│  │         }                                                           │   │
│  │     }                                                               │   │
│  │ }                                                                   │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 5. Integration Error Handling

### 5.1 Error Scenarios

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         Integration Error Handling                          │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  MinIO Errors:                                                              │
│  ├── Connection refused → Retry 3 times, then fail                        │
│  ├── Bucket not found → Create bucket, retry                              │
│  ├── Upload failed → Retry with exponential backoff                       │
│  └── Presigned URL failed → Return error to client                        │
│                                                                             │
│  RabbitMQ Errors:                                                           │
│  ├── Connection refused → Retry 3 times, then fail                        │
│  ├── Queue full → Return 429 Too Many Requests                            │
│  ├── Message rejected → Move to DLQ                                       │
│  └── Publish failed → Retry with exponential backoff                      │
│                                                                             │
│  Redis Errors:                                                              │
│  ├── Connection refused → Degrade gracefully (no cache)                   │
│  ├── Lock acquisition failed → Skip execution (for schedules)             │
│  └── Cache miss → Regenerate report                                       │
│                                                                             │
│  Database Errors:                                                           │
│  ├── Connection timeout → Retry 3 times                                   │
│  ├── Query timeout → Return timeout error                                 │
│  └── Constraint violation → Return validation error                       │
│                                                                             │
│  JasperReports Errors:                                                      │
│  ├── Template not found → Return system error                             │
│  ├── Compilation failed → Return system error                             │
│  └── Fill failed → Return system error                                    │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 5.2 Retry Configuration

```yaml
# application.yml
report:
  integration:
    minio:
      retry:
        max-attempts: 3
        initial-interval: 1000
        max-interval: 5000
        multiplier: 2.0
    rabbitmq:
      retry:
        max-attempts: 3
        initial-interval: 1000
        max-interval: 5000
        multiplier: 2.0
    redis:
      retry:
        max-attempts: 2
        initial-interval: 500
        max-interval: 2000
        multiplier: 2.0
    database:
      retry:
        max-attempts: 3
        initial-interval: 1000
        max-interval: 5000
        multiplier: 2.0
```

---

## 6. Integration Testing Strategy

### 6.1 Test Types

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                       Integration Testing Strategy                          │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  1. Unit Tests (Mocked Dependencies)                                       │
│     ├── Test service logic with mocked repositories                       │
│     ├── Test generator with mocked templates                              │
│     └── Test validators with test data                                    │
│                                                                             │
│  2. Repository Integration Tests (@DataJpaTest)                            │
│     ├── Test custom queries against H2                                    │
│     ├── Test JPA mappings                                                  │
│     └── Test constraint validations                                        │
│                                                                             │
│  3. Service Integration Tests (@SpringBootTest)                            │
│     ├── Test with embedded database                                       │
│     ├── Test with mocked external services (MinIO, RabbitMQ)              │
│     └── Test full report generation flow                                  │
│                                                                             │
│  4. Controller Integration Tests (@WebMvcTest)                             │
│     ├── Test request validation                                            │
│     ├── Test response format                                               │
│     └── Test error handling                                                │
│                                                                             │
│  5. External Integration Tests (Testcontainers)                            │
│     ├── Test with real MySQL                                              │
│     ├── Test with real RabbitMQ                                           │
│     ├── Test with real MinIO                                              │
│     └── Test with real Redis                                              │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 6.2 Test Configuration

```java
@SpringBootTest
@Testcontainers
class ReportIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @Container
    static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3-management");

    @Container
    static MinIOContainer minio = new MinIOContainer("minio/minio");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.rabbitmq.host", rabbitmq::getHost);
        registry.add("spring.rabbitmq.port", rabbitmq::getAmqpPort);
        registry.add("minio.endpoint", () -> "http://" + minio.getHost() + ":" + minio.getFirstMappedPort());
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", () -> redis.getFirstMappedPort());
    }

    @Test
    void should_GenerateCurrentStockReport_When_ValidRequest() {
        // Test implementation
    }
}
```

---

## 7. Documentation Index

- [01-end-to-end-flow.md](01-end-to-end-flow.md) - Flow tổng quan
- [02-on-demand-report-flow.md](02-on-demand-report-flow.md) - Chi tiết on-demand
- [03-async-report-flow.md](03-async-report-flow.md) - Chi tiết async
- [04-schedule-report-flow.md](04-schedule-report-flow.md) - Chi tiết scheduled
- [05-export-import-flow.md](05-export-import-flow.md) - Chi tiết export/import
- [06-sequence-diagrams.md](06-sequence-diagrams.md) - Sequence diagrams
- [07-data-flow-diagrams.md](07-data-flow-diagrams.md) - Data flow diagrams
