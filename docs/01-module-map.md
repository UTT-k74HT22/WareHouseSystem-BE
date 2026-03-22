# WHS Backend — Module Map

## 1) Package-level map

| Package | Responsibility | Notes |
|---|---|---|
| `org.demo.whs.controller` | REST endpoints under `/api/v1/*` | Mix of implemented and placeholder controllers |
| `org.demo.whs.service` + `.impl` | Business logic contracts and implementations | Several modules still scaffold-only |
| `org.demo.whs.repository` | JPA repositories + custom queries | Mostly `JpaRepository`; custom SQL in user profile repo |
| `org.demo.whs.entity` | Persistence models | Most entities extend `BaseEntity` |
| `org.demo.whs.entity.dto` | Request/response contracts | Used heavily in implemented modules |
| `org.demo.whs.mapper` | DTO/entity mapping | Dedicated mapper per domain in many modules |
| `org.demo.whs.security` | JWT, authz, filters, security helpers | Includes rate limiting filter and JWT stack |
| `org.demo.whs.configuration` | Security, Redis, RabbitMQ, CORS, OpenAPI | Central infra wiring |
| `org.demo.whs.helpers` | Integration helpers | Email producer/consumer/scheduler |

## 2) Domain module status and key classes

### A. Authentication & RBAC
- **Responsibility:** login, token refresh, role loading, request authorization.
- **Key classes:**
  - Controller: `AuthController`
  - Service: `AuthServiceImpl`
  - Security: `SecurityConfig`, `JwtProvider`, `JwtAuthFilter`, `CustomUserDetailsService`
  - Repos/entities: `AccountRepository`, `RoleRepository`, `Account`, `Role`, `AccountHasRole`
- **Boundary:** handles identity and token lifecycle; business modules consume authenticated context.

### B. Warehouse & Location Master
- **Responsibility:** warehouse and location CRUD/search/status changes.
- **Key classes:**
  - Controllers: `WareHouseController`, `LocationController`
  - Services: `WareHouseServiceImpl`, `LocationServiceImpl`
  - Repos/entities: `WareHouseRepository`, `LocationRepository`, `Warehouses`, `Locations`
  - Supporting query projection: `UserProfileRepositoryImpl` (manager profile enrichment)
- **Boundary:** validates warehouse/location constraints; does not yet enforce inventory-dependent location status rules (TODO comments in service).

### C. Product / Category / UOM / Partner Master Data
- **Responsibility:** product lifecycle and core references.
- **Key classes:**
  - Product implemented: `ProductController`, `ProductServiceImpl`, `ProductRepository`, `Products`
  - UOM implemented: `UnitsOfMeasureController`, `UnitsOfMeasureImpl`, `UnitsOfMeasureRepository`, `UnitsOfMeasure`
  - Category/Business partner currently minimal controller/service coverage (`CategoryController` empty; `BusinessPartnerController` placeholder GET)
- **Boundary:** product service validates category/UOM references and stock-level constraints; category/UOM relation is by IDs (not JPA object graph).

### D. User Read Module
- **Responsibility:** fetch manager list and user profile by account ID.
- **Key classes:** `UserController`, `UserServiceImpl`, `UserProfileRepositoryImpl`.
- **Boundary:** read-focused module via custom native queries.

### E. Email & Notification Module
- **Responsibility:** send email (sync/async), log status, retry/reprocess, statistics, scheduled cleanup.
- **Key classes:**
  - API: `EmailController`
  - Service: `EmailServiceImpl`
  - Queue integration: `EmailProducerService`, `EmailConsumerService`
  - Scheduler: `EmailScheduledService`
  - Repos/entities: `EmailLogRepository`, `EmailLog`
  - Config: `EmailProperties`, `RabbitMQConfig`, `RabbitMQEmailConfig`
- **Boundary:** module owns email lifecycle and retries; uses RabbitMQ + DB log table for async reliability pattern.

### F. Inbound / Outbound / Inventory / Stock Movement / Batch (current state)
- **Schema exists via Flyway:**
  - Inbound: purchase orders, receipts, receipt lines
  - Outbound: sales orders, shipments, shipment lines
  - Inventory: inventory, stock adjustments, stock transfers
  - Stock audit: stock movements
  - Batch: batches
- **Code presence:**
  - Entities + repositories exist for above domains.
  - Controllers/services are mostly scaffolds (class-level route and injected service; limited endpoint/business implementation).
- **Representative scaffold classes:**
  - `InboundReceiptsController` / `InboundReceiptsServiceImpl`
  - `PurchaseOrdersController` / `PurchaseOrdersServiceImpl`
  - `SalesOrdersController` / `SalesOrdersServiceImpl`
  - `InventoryController` / `InventoryServiceImpl`
  - `StockMovementsController` / `StockMovementsServiceImpl`
  - `BatchController` / `BatchServiceImpl`

### G. Report Module (Planned - WHS-52)
- **Responsibility:** tạo, quản lý và xuất báo cáo cho hệ thống kho.
- **Jira Tasks:** WHS-52 (Parent), WHS-67 (On-demand), WHS-68 (Async), WHS-69 (Schedule)
- **Planned Key Classes:**
  - Controller: `ReportController`
  - Service: `ReportService` / `ReportServiceImpl`
  - Repository: `ReportRepositoryCustom` / `ReportRepositoryCustomImpl`
  - Entities: `ReportRequest`, `ReportSchedule`
  - DTOs: `Report/*Request`, `Report/*Response`
  - Config: `ReportConfig`
- **Report Types:**
  - Current Stock Report
  - Stock Valuation Report
  - Movements Report
  - Batch Traceability Report
  - Low Stock Report
  - Expiring Batches Report
- **Export Formats:** PDF (JasperReports), Excel (Apache POI), CSV
- **Processing Modes:** On-demand (sync), Async (RabbitMQ), Scheduled (Spring Scheduler)
- **Boundary:** Report module sẽ query data từ Inventory, Inbound, Outbound, Batch modules. Không duplicate data, chỉ aggregate và format.

### H. Export/Import Module (Planned - WHS-37, WHS-38, WHS-39)
- **Responsibility:** import/export dữ liệu sản phẩm, vị trí.
- **Jira Tasks:** WHS-37 (Location Bulk Import), WHS-38 (Product Import), WHS-39 (Product Export)
- **Planned Endpoints:**
  - `GET /api/v1/products/export` - Export sản phẩm ra Excel
  - `POST /api/v1/products/import` - Import sản phẩm từ Excel
  - `POST /api/v1/locations/bulk` - Import vị trí hàng loạt từ CSV
- **Boundary:** Async processing, validation, error reporting.

## 3) Data and control flow boundaries

### Implemented flow (Auth/Product/Warehouse/Location/UOM/Email)
`HTTP -> Security filters -> Controller -> Service -> Repository -> MySQL`  
`Service -> Mapper -> DTO response`

### Async integration flow (Email)
`EmailController -> EmailServiceImpl(save EmailLog) -> EmailProducerService -> RabbitMQ`  
`RabbitMQ -> EmailConsumerService -> JavaMailSender -> update EmailLog`

### Caching/rate-limit side flows
- UOM list/entity caching through Redis cache manager.
- Rate-limit counters and TTL state persisted in Redis via Lua script.

## 4) Module boundaries to keep in mind
- Security module is cross-cutting and should remain independent from domain services.
- Master data modules (warehouse/location/product/UOM/category/partner) provide reference data for operational modules.
- Operational modules (inbound/outbound/inventory/stock movement) already have DB schema and entities, but implementation depth is currently uneven across services/controllers.

