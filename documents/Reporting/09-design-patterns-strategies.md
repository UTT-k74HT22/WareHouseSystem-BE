# Design Patterns & Architecture Strategies cho Module Report

> Cập nhật: 2026-03-22
> Mục đích: Hướng dẫn triển khai module Report với các pattern và chiến lược tối ưu

---

## 1. Design Patterns Nên Áp Dụng

### 1.1 Strategy Pattern (Quan Trọng Nhất)

**Mục đích:** Thay đổi thuật toán/chiến thuật生成 report và export format mà không thay đổi code chính.

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          Strategy Pattern                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│                         ┌─────────────────┐                                 │
│                         │ ReportGenerator │ (Interface)                     │
│                         │   <<interface>> │                                 │
│                         └────────┬────────┘                                 │
│                                  │                                          │
│            ┌─────────────────────┼─────────────────────┐                    │
│            │                     │                     │                    │
│            ▼                     ▼                     ▼                    │
│   ┌─────────────────┐   ┌─────────────────┐   ┌─────────────────┐         │
│   │ CurrentStock    │   │ StockValuation  │   │ BatchTrace      │         │
│   │ ReportGenerator │   │ ReportGenerator │   │ ReportGenerator │         │
│   └─────────────────┘   └─────────────────┘   └─────────────────┘         │
│                                                                             │
│                         ┌─────────────────┐                                 │
│                         │ ExportFormatter │ (Interface)                     │
│                         │   <<interface>> │                                 │
│                         └────────┬────────┘                                 │
│                                  │                                          │
│            ┌─────────────────────┼─────────────────────┐                    │
│            │                     │                     │                    │
│            ▼                     ▼                     ▼                    │
│   ┌─────────────────┐   ┌─────────────────┐   ┌─────────────────┐         │
│   │ PdfExport       │   │ ExcelExport     │   │ CsvExport       │         │
│   │ Formatter       │   │ Formatter       │   │ Formatter       │         │
│   └─────────────────┘   └─────────────────┘   └─────────────────┘         │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

**Triển khai:**

```java
// 1. Report Generator Strategy Interface
public interface ReportGenerator<T extends ReportData> {
    ReportType getReportType();
    T generate(ReportRequest request);
}

// 2. Concrete Strategies
@Component
public class CurrentStockReportGenerator implements ReportGenerator<CurrentStockData> {
    
    @Override
    public ReportType getReportType() {
        return ReportType.CURRENT_STOCK;
    }
    
    @Override
    public CurrentStockData generate(ReportRequest request) {
        // Query logic specific to current stock
        List<Inventory> inventories = inventoryRepository.findByFilters(request.getFilters());
        return CurrentStockData.builder()
                .items(mapToItems(inventories))
                .summary(calculateSummary(inventories))
                .build();
    }
}

@Component
public class StockValuationReportGenerator implements ReportGenerator<StockValuationData> {
    
    @Override
    public ReportType getReportType() {
        return ReportType.STOCK_VALUATION;
    }
    
    @Override
    public StockValuationData generate(ReportRequest request) {
        // Different query and calculation logic
        return calculateValuation(request);
    }
}

// 3. Export Formatter Strategy Interface
public interface ExportFormatter {
    ExportFormat getFormat();
    byte[] format(ReportData data, String templateName);
}

// 4. Concrete Formatters
@Component
public class PdfExportFormatter implements ExportFormatter {
    
    @Override
    public ExportFormat getFormat() {
        return ExportFormat.PDF;
    }
    
    @Override
    public byte[] format(ReportData data, String templateName) {
        JasperReport report = templateCache.get(templateName);
        JasperPrint print = JasperFillManager.fillReport(report, data.toParameters(), data.toDataSource());
        return JasperExportManager.exportReportToPdf(print);
    }
}

@Component
public class ExcelExportFormatter implements ExportFormatter {
    
    @Override
    public ExportFormat getFormat() {
        return ExportFormat.EXCEL;
    }
    
    @Override
    public byte[] format(ReportData data, String templateName) {
        // Apache POI logic
    }
}

// 5. Context - Report Service sử dụng Strategies
@Service
public class ReportServiceImpl implements ReportService {
    
    private final Map<ReportType, ReportGenerator<?>> generators;
    private final Map<ExportFormat, ExportFormatter> formatters;
    
    // Spring tự inject tất cả implementations
    public ReportServiceImpl(List<ReportGenerator<?>> generatorList,
                             List<ExportFormatter> formatterList) {
        this.generators = generatorList.stream()
                .collect(Collectors.toMap(ReportGenerator::getReportType, g -> g));
        this.formatters = formatterList.stream()
                .collect(Collectors.toMap(ExportFormatter::getFormat, f -> f));
    }
    
    @Override
    public byte[] generateReport(ReportRequest request) {
        // 1. Get appropriate generator
        ReportGenerator<?> generator = generators.get(request.getReportType());
        if (generator == null) {
            throw new UnsupportedReportTypeException(request.getReportType());
        }
        
        // 2. Generate report data
        ReportData data = generator.generate(request);
        
        // 3. Get appropriate formatter
        ExportFormatter formatter = formatters.get(request.getFormat());
        if (formatter == null) {
            throw new UnsupportedFormatException(request.getFormat());
        }
        
        // 4. Format and return
        return formatter.format(data, request.getReportType().getTemplateName());
    }
}
```

---

### 1.2 Factory Pattern

**Mục đích:** Tạo report request/response objects một cách linh hoạt.

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                            Factory Pattern                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│                    ┌─────────────────────┐                                  │
│                    │ ReportRequestFactory│                                  │
│                    │    <<interface>>    │                                  │
│                    └──────────┬──────────┘                                  │
│                               │                                             │
│            ┌──────────────────┼──────────────────┐                         │
│            │                  │                  │                         │
│            ▼                  ▼                  ▼                         │
│   ┌──────────────┐   ┌──────────────┐   ┌──────────────┐                  │
│   │ OnDemand     │   │ Async        │   │ Scheduled    │                  │
│   │ Request      │   │ Request      │   │ Request      │                  │
│   │ Factory      │   │ Factory      │   │ Factory      │                  │
│   └──────────────┘   └──────────────┘   └──────────────┘                  │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

**Triển khai:**

```java
// Abstract Factory
public interface ReportRequestFactory {
    ReportRequest create(ReportType type, Map<String, Object> parameters);
}

// Concrete Factories
@Component
public class OnDemandReportRequestFactory implements ReportRequestFactory {
    
    @Override
    public ReportRequest create(ReportType type, Map<String, Object> parameters) {
        return ReportRequest.builder()
                .id(UUID.randomUUID().toString())
                .reportType(type)
                .parameters(parameters)
                .requestType(RequestType.ON_DEMAND)
                .status(ReportStatus.PROCESSING)
                .requestedAt(LocalDateTime.now())
                .build();
    }
}

@Component
public class AsyncReportRequestFactory implements ReportRequestFactory {
    
    @Override
    public ReportRequest create(ReportType type, Map<String, Object> parameters) {
        return ReportRequest.builder()
                .id(UUID.randomUUID().toString())
                .reportType(type)
                .parameters(parameters)
                .requestType(RequestType.ASYNC)
                .status(ReportStatus.PENDING)
                .requestedAt(LocalDateTime.now())
                .retryCount(0)
                .maxRetries(3)
                .build();
    }
}

// Factory Registry
@Component
public class ReportRequestFactoryRegistry {
    
    private final Map<RequestType, ReportRequestFactory> factories;
    
    public ReportRequestFactoryRegistry(List<ReportRequestFactory> factoryList) {
        this.factories = factoryList.stream()
                .collect(Collectors.toMap(
                        f -> f.getClass().getAnnotation(ReportFactoryType.class).value(),
                        f -> f
                ));
    }
    
    public ReportRequestFactory getFactory(RequestType type) {
        return factories.get(type);
    }
}
```

---

### 1.3 Template Method Pattern

**Mục đích:** Định nghĩa skeleton của report processing, cho phép subclasses tùy chỉnh các bước cụ thể.

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        Template Method Pattern                              │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│                    ┌─────────────────────┐                                  │
│                    │ AbstractReport      │                                  │
│                    │ Processor           │                                  │
│                    │  <<abstract class>> │                                  │
│                    └──────────┬──────────┘                                  │
│                               │                                             │
│     ┌─────────────────────────┼─────────────────────────┐                  │
│     │                         │                         │                  │
│     │ final generate():       │ abstract methods:       │                  │
│     │ 1. validate()          │ - validate()            │                  │
│     │ 2. queryData()         │ - queryData()           │                  │
│     │ 3. processData()       │ - processData()         │                  │
│     │ 4. calculateTotals()   │ - calculateTotals()     │                  │
│     │ 5. formatResponse()    │                         │                  │
│     │                         │                         │                  │
│     └─────────────────────────┴─────────────────────────┘                  │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

**Triển khai:**

```java
// Abstract Class với Template Method
public abstract class AbstractReportProcessor<T extends ReportData> {
    
    // Template Method - KHÔNG cho override
    public final T process(ReportRequest request) {
        log.info("Processing report: {}", request.getReportType());
        
        // Step 1: Validate (abstract)
        validate(request);
        
        // Step 2: Query data (abstract)
        List<?> rawData = queryData(request);
        
        // Step 3: Process/transform data (abstract)
        T processedData = processData(rawData, request);
        
        // Step 4: Calculate totals (abstract)
        calculateTotals(processedData);
        
        // Step 5: Enrich with metadata (hook - có thể override)
        enrichWithMetadata(processedData, request);
        
        log.info("Report processed successfully: {}", request.getReportType());
        return processedData;
    }
    
    // Abstract methods - subclasses PHẢI implement
    protected abstract void validate(ReportRequest request);
    protected abstract List<?> queryData(ReportRequest request);
    protected abstract T processData(List<?> rawData, ReportRequest request);
    protected abstract void calculateTotals(T data);
    
    // Hook method - subclasses CÓ THỂ override
    protected void enrichWithMetadata(T data, ReportRequest request) {
        data.setGeneratedAt(LocalDateTime.now());
        data.setRequestId(request.getId());
    }
}

// Concrete Implementation
@Component
public class CurrentStockReportProcessor extends AbstractReportProcessor<CurrentStockData> {
    
    @Override
    protected void validate(ReportRequest request) {
        // Validation specific to current stock report
        if (request.getWarehouseId() == null) {
            throw new ValidationException("Warehouse ID is required");
        }
    }
    
    @Override
    protected List<Inventory> queryData(ReportRequest request) {
        return inventoryRepository.findByWarehouseId(request.getWarehouseId());
    }
    
    @Override
    protected CurrentStockData processData(List<Inventory> rawData, ReportRequest request) {
        return CurrentStockData.builder()
                .items(mapToItems(rawData))
                .build();
    }
    
    @Override
    protected void calculateTotals(CurrentStockData data) {
        data.setSummary(CurrentStockSummary.builder()
                .totalOnHand(data.getItems().stream()
                        .map(CurrentStockItem::getOnHandQuantity)
                        .reduce(BigDecimal.ZERO, BigDecimal::add))
                .build());
    }
}
```

---

### 1.4 Builder Pattern

**Mục đích:** Xây dựng complex queries một cách linh hoạt và readable.

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                            Builder Pattern                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   ReportQueryBuilder                                                        │
│   ├── .select("product_id", "product_name")                               │
│   ├── .from("inventory")                                                   │
│   ├── .join("products", "inventory.product_id = products.id")             │
│   ├── .where("warehouse_id = ?", warehouseId)                             │
│   ├── .where("on_hand_quantity > ?", 0)                                    │
│   ├── .groupBy("product_id")                                               │
│   ├── .orderBy("product_name", ASC)                                        │
│   └── .build() → Specification<Inventory>                                  │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

**Triển khai:**

```java
// Report Query Builder
@Component
public class ReportQueryBuilder {
    
    private final EntityManager entityManager;
    
    public <T> CriteriaQuery<T> buildQuery(Class<T> entityClass, 
                                           ReportFilterRequest filter) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> query = cb.createQuery(entityClass);
        Root<T> root = query.from(entityClass);
        
        List<Predicate> predicates = new ArrayList<>();
        
        // Build predicates based on filter
        if (filter.getWarehouseId() != null) {
            predicates.add(cb.equal(root.get("warehouseId"), filter.getWarehouseId()));
        }
        
        if (filter.getProductId() != null) {
            predicates.add(cb.equal(root.get("productId"), filter.getProductId()));
        }
        
        if (filter.getDateFrom() != null && filter.getDateTo() != null) {
            predicates.add(cb.between(root.get("createdAt"), 
                    filter.getDateFrom(), filter.getDateTo()));
        }
        
        query.where(predicates.toArray(new Predicate[0]));
        
        // Add ordering
        if (filter.getSortBy() != null) {
            if (filter.getSortDirection() == SortDirection.ASC) {
                query.orderBy(cb.asc(root.get(filter.getSortBy())));
            } else {
                query.orderBy(cb.desc(root.get(filter.getSortBy())));
            }
        }
        
        return query;
    }
}

// Specification Builder cho Spring Data JPA
public class ReportSpecificationBuilder<T> {
    
    private final List<Specification<T>> specifications = new ArrayList<>();
    
    public ReportSpecificationBuilder<T> withWarehouse(String warehouseId) {
        if (warehouseId != null) {
            specifications.add((root, query, cb) -> 
                    cb.equal(root.get("warehouseId"), warehouseId));
        }
        return this;
    }
    
    public ReportSpecificationBuilder<T> withProduct(String productId) {
        if (productId != null) {
            specifications.add((root, query, cb) -> 
                    cb.equal(root.get("productId"), productId));
        }
        return this;
    }
    
    public ReportSpecificationBuilder<T> withDateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null) {
            specifications.add((root, query, cb) -> 
                    cb.between(root.get("createdAt"), from, to));
        }
        return this;
    }
    
    public Specification<T> build() {
        return specifications.stream()
                .reduce(Specification::and)
                .orElse(null);
    }
}

// Usage
@Service
public class ReportServiceImpl {
    
    public Page<Inventory> getInventoryReport(InventoryReportFilter filter) {
        Specification<Inventory> spec = new ReportSpecificationBuilder<Inventory>()
                .withWarehouse(filter.getWarehouseId())
                .withProduct(filter.getProductId())
                .withDateRange(filter.getDateFrom(), filter.getDateTo())
                .build();
        
        return inventoryRepository.findAll(spec, filter.getPageable());
    }
}
```

---

### 1.5 Chain of Responsibility Pattern

**Mục đích:** Xây dựng validation pipeline cho report requests.

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    Chain of Responsibility Pattern                          │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   Request                                                                   │
│      │                                                                      │
│      ▼                                                                      │
│   ┌─────────────────┐                                                       │
│   │ Parameter       │──► Validate required fields                          │
│   │ Validator       │                                                       │
│   └────────┬────────┘                                                       │
│            │                                                                │
│            ▼                                                                │
│   ┌─────────────────┐                                                       │
│   │ Permission      │──► Check user permissions                            │
│   │ Validator       │                                                       │
│   └────────┬────────┘                                                       │
│            │                                                                │
│            ▼                                                                │
│   ┌─────────────────┐                                                       │
│   │ Business Rule   │──► Validate business constraints                      │
│   │ Validator       │                                                       │
│   └────────┬────────┘                                                       │
│            │                                                                │
│            ▼                                                                │
│   ┌─────────────────┐                                                       │
│   │ Rate Limit      │──► Check rate limiting                               │
│   │ Validator       │                                                       │
│   └────────┬────────┘                                                       │
│            │                                                                │
│            ▼                                                                │
│        Valid ✓                                                              │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

**Triển khai:**

```java
// Handler Interface
public interface ReportRequestValidator {
    void validate(ReportRequest request);
    int getOrder();
}

// Concrete Validators
@Component
@Order(1)
public class ParameterValidator implements ReportRequestValidator {
    
    @Override
    public void validate(ReportRequest request) {
        if (request.getReportType() == null) {
            throw new ValidationException("Report type is required");
        }
        if (request.getFormat() == null) {
            throw new ValidationException("Export format is required");
        }
        // Validate report-specific parameters
        validateReportParameters(request);
    }
    
    @Override
    public int getOrder() {
        return 1;
    }
}

@Component
@Order(2)
public class PermissionValidator implements ReportRequestValidator {
    
    private final PermissionService permissionService;
    
    @Override
    public void validate(ReportRequest request) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        
        if (!permissionService.hasPermission(userId, "VIEW_REPORTS")) {
            throw new AccessDeniedException("No permission to view reports");
        }
        
        if (request.getFormat() != ExportFormat.PDF && 
            !permissionService.hasPermission(userId, "EXPORT_REPORTS")) {
            throw new AccessDeniedException("No permission to export reports");
        }
    }
    
    @Override
    public int getOrder() {
        return 2;
    }
}

@Component
@Order(3)
public class BusinessRuleValidator implements ReportRequestValidator {
    
    @Override
    public void validate(ReportRequest request) {
        // Validate date range
        if (request.getDateFrom() != null && request.getDateTo() != null) {
            if (request.getDateFrom().isAfter(request.getDateTo())) {
                throw new ValidationException("Date from must be before date to");
            }
            
            long daysBetween = ChronoUnit.DAYS.between(request.getDateFrom(), request.getDateTo());
            if (daysBetween > 365) {
                throw new ValidationException("Date range cannot exceed 1 year");
            }
        }
    }
    
    @Override
    public int getOrder() {
        return 3;
    }
}

// Chain Executor
@Component
public class ReportRequestValidatorChain {
    
    private final List<ReportRequestValidator> validators;
    
    public ReportRequestValidatorChain(List<ReportRequestValidator> validators) {
        this.validators = validators.stream()
                .sorted(Comparator.comparingInt(ReportRequestValidator::getOrder))
                .collect(Collectors.toList());
    }
    
    public void validate(ReportRequest request) {
        for (ReportRequestValidator validator : validators) {
            validator.validate(request);
        }
    }
}

// Usage trong Service
@Service
public class ReportServiceImpl {
    
    private final ReportRequestValidatorChain validatorChain;
    
    public ReportResponse generateReport(ReportRequest request) {
        // Run validation chain
        validatorChain.validate(request);
        
        // Proceed with report generation
        // ...
    }
}
```

---

### 1.6 Observer Pattern

**Mục đích:** Thông báo khi report generation hoàn thành.

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           Observer Pattern                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│                    ┌─────────────────────┐                                  │
│                    │ ReportEventPublisher│                                  │
│                    │    (Subject)        │                                  │
│                    └──────────┬──────────┘                                  │
│                               │                                             │
│            publish(event)     │                                             │
│                               ▼                                             │
│                    ┌─────────────────────┐                                  │
│                    │ ReportCompletedEvent│                                  │
│                    └──────────┬──────────┘                                  │
│                               │                                             │
│            ┌──────────────────┼──────────────────┐                         │
│            │                  │                  │                         │
│            ▼                  ▼                  ▼                         │
│   ┌──────────────┐   ┌──────────────┐   ┌──────────────┐                  │
│   │ Email        │   │ Audit Log    │   │ Notification │                  │
│   │ Listener     │   │ Listener     │   │ Listener     │                  │
│   └──────────────┘   └──────────────┘   └──────────────┘                  │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

**Triển khai:**

```java
// Event
@Getter
@AllArgsConstructor
public class ReportCompletedEvent {
    private final String requestId;
    private final ReportType reportType;
    private final String userId;
    private final LocalDateTime completedAt;
    private final String fileUrl;
}

// Publisher
@Component
public class ReportEventPublisher {
    
    private final ApplicationEventPublisher eventPublisher;
    
    public void publishReportCompleted(ReportRequest request, String fileUrl) {
        ReportCompletedEvent event = new ReportCompletedEvent(
                request.getId(),
                request.getReportType(),
                request.getRequestedBy(),
                LocalDateTime.now(),
                fileUrl
        );
        eventPublisher.publishEvent(event);
    }
}

// Listeners
@Component
@Slf4j
public class ReportEmailListener {
    
    @Async
    @EventListener
    public void handleReportCompleted(ReportCompletedEvent event) {
        log.info("Sending email notification for report: {}", event.getRequestId());
        // Send email logic
    }
}

@Component
@Slf4j
public class ReportAuditLogListener {
    
    @EventListener
    public void handleReportCompleted(ReportCompletedEvent event) {
        log.info("Audit log - Report completed: {}", event.getRequestId());
        // Save audit log
    }
}

@Component
@Slf4j
public class ReportNotificationListener {
    
    @EventListener
    public void handleReportCompleted(ReportCompletedEvent event) {
        log.info("Push notification for report: {}", event.getRequestId());
        // WebSocket/SSE notification
    }
}
```

---

## 2. Database Strategies

### 2.1 Database Views cho Complex Queries

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                       Database Views Strategy                               │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  Thay vì query phức tạp mỗi lần, tạo VIEW một lần:                        │
│                                                                             │
│  CREATE VIEW v_inventory_summary AS                                        │
│  SELECT                                                                     │
│      p.id AS product_id,                                                   │
│      p.sku,                                                                │
│      p.name AS product_name,                                               │
│      c.name AS category_name,                                              │
│      w.id AS warehouse_id,                                                 │
│      w.name AS warehouse_name,                                             │
│      SUM(i.on_hand_quantity) AS total_on_hand,                             │
│      SUM(i.reserved_quantity) AS total_reserved,                           │
│      SUM(i.quarantine_quantity) AS total_quarantine,                       │
│      SUM(i.on_hand_quantity - i.reserved_quantity - i.quarantine_quantity) │
│          AS available_quantity,                                            │
│      p.cost_price,                                                         │
│      p.cost_price * SUM(i.on_hand_quantity - i.reserved_quantity -         │
│          i.quarantine_quantity) AS stock_value                             │
│  FROM inventory i                                                          │
│  INNER JOIN products p ON i.product_id = p.id                             │
│  INNER JOIN warehouses w ON i.warehouse_id = w.id                         │
│  LEFT JOIN categories c ON p.category_id = c.id                           │
│  WHERE p.status = 'ACTIVE'                                                 │
│  GROUP BY p.id, w.id;                                                      │
│                                                                             │
│  Lợi ích:                                                                  │
│  ├── Query đơn giản hơn                                                    │
│  ├── Performance tốt hơn (pre-computed joins)                              │
│  ├── Reusable cho nhiều reports                                            │
│  └── Dễ maintain                                                           │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

**Flyway Migration:**

```sql
-- V0XX__create_report_views.sql

-- View cho Current Stock Report
CREATE OR REPLACE VIEW v_inventory_summary AS
SELECT
    p.id AS product_id,
    p.sku,
    p.name AS product_name,
    c.name AS category_name,
    w.id AS warehouse_id,
    w.name AS warehouse_name,
    l.id AS location_id,
    l.code AS location_code,
    b.id AS batch_id,
    b.batch_number,
    b.expiry_date,
    i.on_hand_quantity,
    i.reserved_quantity,
    i.quarantine_quantity,
    (i.on_hand_quantity - i.reserved_quantity - i.quarantine_quantity) AS available_quantity,
    p.cost_price,
    p.selling_price,
    p.reorder_point,
    p.min_stock_level,
    p.cost_price * (i.on_hand_quantity - i.reserved_quantity - i.quarantine_quantity) AS stock_value
FROM inventory i
INNER JOIN products p ON i.product_id = p.id
INNER JOIN warehouses w ON i.warehouse_id = w.id
LEFT JOIN locations l ON i.location_id = l.id
LEFT JOIN categories c ON p.category_id = c.id
LEFT JOIN batches b ON i.batch_id = b.id
WHERE p.status = 'ACTIVE';

-- View cho Movements Report
CREATE OR REPLACE VIEW v_stock_movements_enriched AS
SELECT
    sm.id,
    sm.movement_type,
    sm.movement_date,
    sm.quantity_change,
    sm.quantity_before,
    sm.quantity_after,
    p.sku AS product_sku,
    p.name AS product_name,
    w.name AS warehouse_name,
    l.code AS location_code,
    b.batch_number,
    sm.reference_type,
    sm.reference_number,
    sm.notes
FROM stock_movements sm
INNER JOIN products p ON sm.product_id = p.id
INNER JOIN warehouses w ON sm.warehouse_id = w.id
LEFT JOIN locations l ON sm.location_id = l.id
LEFT JOIN batches b ON sm.batch_id = b.id;

-- View cho Low Stock Report
CREATE OR REPLACE VIEW v_low_stock_products AS
SELECT
    p.id AS product_id,
    p.sku,
    p.name AS product_name,
    c.name AS category_name,
    w.id AS warehouse_id,
    w.name AS warehouse_name,
    SUM(i.on_hand_quantity - i.reserved_quantity - i.quarantine_quantity) AS available_quantity,
    p.reorder_point,
    p.min_stock_level,
    p.cost_price,
    (p.reorder_point - SUM(i.on_hand_quantity - i.reserved_quantity - i.quarantine_quantity)) AS quantity_to_reorder
FROM products p
INNER JOIN inventory i ON p.id = i.product_id
INNER JOIN warehouses w ON i.warehouse_id = w.id
LEFT JOIN categories c ON p.category_id = c.id
WHERE p.status = 'ACTIVE'
GROUP BY p.id, w.id
HAVING available_quantity < p.reorder_point;

-- View cho Expiring Batches
CREATE OR REPLACE VIEW v_expiring_batches AS
SELECT
    b.id AS batch_id,
    b.batch_number,
    b.manufacture_date,
    b.expiry_date,
    DATEDIFF(b.expiry_date, CURDATE()) AS days_remaining,
    p.id AS product_id,
    p.sku,
    p.name AS product_name,
    w.id AS warehouse_id,
    w.name AS warehouse_name,
    l.code AS location_code,
    i.on_hand_quantity,
    i.quarantine_quantity
FROM batches b
INNER JOIN inventory i ON b.id = i.batch_id
INNER JOIN products p ON i.product_id = p.id
INNER JOIN warehouses w ON i.warehouse_id = w.id
LEFT JOIN locations l ON i.location_id = l.id
WHERE b.expiry_date IS NOT NULL
    AND i.on_hand_quantity > 0;
```

**JPA Entity Mapping cho View:**

```java
@Entity
@Table(name = "v_inventory_summary")
@Getter
@Setter
@Immutable  // Hibernate annotation cho read-only
public class InventorySummaryView {
    
    @Id
    @Column(name = "product_id")
    private String productId;
    
    @Column(name = "sku")
    private String sku;
    
    @Column(name = "product_name")
    private String productName;
    
    // ... other fields
}

// Repository
@Repository
public interface InventorySummaryViewRepository extends JpaRepository<InventorySummaryView, String> {
    
    @Query("SELECT v FROM InventorySummaryView v WHERE v.warehouseId = :warehouseId")
    List<InventorySummaryView> findByWarehouseId(@Param("warehouseId") String warehouseId);
    
    @Query("SELECT v FROM InventorySummaryView v WHERE v.availableQuantity < v.reorderPoint")
    List<InventorySummaryView> findLowStockItems();
}
```

---

### 2.2 Materialized Views (Nếu dùng PostgreSQL)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                      Materialized Views Strategy                            │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  Materialized View = View + Cached Results                                  │
│                                                                             │
│  CREATE MATERIALIZED VIEW mv_inventory_summary AS                          │
│  SELECT ... FROM ... GROUP BY ...;                                         │
│                                                                             │
│  -- Refresh định kỳ (dùng scheduler)                                       │
│  REFRESH MATERIALIZED VIEW mv_inventory_summary;                           │
│                                                                             │
│  Ưu điểm:                                                                  │
│  ├── Performance cực tốt (pre-computed và cached)                          │
│  ├── Không cần query real-time                                            │
│  └── Phù hợp cho reports phức tạp                                         │
│                                                                             │
│  Nhược điểm:                                                               │
│  ├── Data có thể stale                                                     │
│  ├── Cần refresh mechanism                                                │
│  └── MySQL không hỗ trợ native                                            │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

**MySQL Alternative - Summary Tables:**

```sql
-- Bảng tổng hợp được update bởi trigger hoặc scheduler
CREATE TABLE inventory_summary_cache (
    product_id CHAR(36) NOT NULL,
    warehouse_id CHAR(36) NOT NULL,
    total_on_hand DECIMAL(15,2),
    total_reserved DECIMAL(15,2),
    available_quantity DECIMAL(15,2),
    stock_value DECIMAL(15,2),
    last_updated TIMESTAMP,
    PRIMARY KEY (product_id, warehouse_id)
);

-- Refresh bởi scheduler
-- @Scheduled(fixedRate = 300000) // Every 5 minutes
public void refreshInventorySummary() {
    jdbcTemplate.execute("""
        INSERT INTO inventory_summary_cache
        SELECT 
            product_id,
            warehouse_id,
            SUM(on_hand_quantity),
            SUM(reserved_quantity),
            SUM(on_hand_quantity - reserved_quantity - quarantine_quantity),
            SUM(cost_price * (on_hand_quantity - reserved_quantity - quarantine_quantity)),
            NOW()
        FROM inventory
        GROUP BY product_id, warehouse_id
        ON DUPLICATE KEY UPDATE
            total_on_hand = VALUES(total_on_hand),
            total_reserved = VALUES(total_reserved),
            available_quantity = VALUES(available_quantity),
            stock_value = VALUES(stock_value),
            last_updated = NOW()
    """);
}
```

---

### 2.3 Stored Procedures cho Heavy Aggregations

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                      Stored Procedures Strategy                             │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  Sử dụng khi:                                                              │
│  ├── Query quá phức tạp, nhiều joins                                      │
│  ├── Cần tính toán aggregations nặng                                      │
│  ├── Cần xử lý business logic trong DB                                    │
│  └── Performance là ưu tiên hàng đầu                                      │
│                                                                             │
│  Không sử dụng khi:                                                        │
│  ├── Logic đơn giản                                                        │
│  ├── Cần maintainability                                                   │
│  ├── Team không strong về DB                                              │
│  └── Muốn test dễ dàng                                                    │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

**Ví dụ Stored Procedure:**

```sql
DELIMITER //

CREATE PROCEDURE sp_generate_stock_valuation(
    IN p_warehouse_id CHAR(36),
    IN p_valuation_method VARCHAR(20),
    IN p_as_of_date DATE
)
BEGIN
    DECLARE v_total_value DECIMAL(20,2);
    
    -- Temporary table để tính weighted average
    CREATE TEMPORARY TABLE tmp_weighted_avg AS
    SELECT 
        product_id,
        warehouse_id,
        SUM(quantity_change * unit_cost) / NULLIF(SUM(quantity_change), 0) AS avg_cost
    FROM stock_movements
    WHERE movement_type = 'INBOUND'
        AND (p_warehouse_id IS NULL OR warehouse_id = p_warehouse_id)
        AND movement_date <= p_as_of_date
    GROUP BY product_id, warehouse_id;
    
    -- Kết quả
    SELECT 
        p.id AS product_id,
        p.sku,
        p.name AS product_name,
        w.name AS warehouse_name,
        i.on_hand_quantity,
        COALESCE(twa.avg_cost, p.cost_price) AS unit_cost,
        i.on_hand_quantity * COALESCE(twa.avg_cost, p.cost_price) AS stock_value
    FROM inventory i
    INNER JOIN products p ON i.product_id = p.id
    INNER JOIN warehouses w ON i.warehouse_id = w.id
    LEFT JOIN tmp_weighted_avg twa ON i.product_id = twa.product_id 
        AND i.warehouse_id = twa.warehouse_id
    WHERE (p_warehouse_id IS NULL OR i.warehouse_id = p_warehouse_id)
        AND i.on_hand_quantity > 0
    ORDER BY p.name;
    
    -- Cleanup
    DROP TEMPORARY TABLE tmp_weighted_avg;
    
END //

DELIMITER ;
```

**Gọi từ Java:**

```java
@Repository
public class ReportRepositoryCustomImpl implements ReportRepositoryCustom {
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @Override
    public List<StockValuationItem> generateStockValuation(String warehouseId, 
                                                             String valuationMethod,
                                                             LocalDate asOfDate) {
        StoredProcedureQuery query = entityManager
                .createStoredProcedureQuery("sp_generate_stock_valuation")
                .registerStoredProcedureParameter("p_warehouse_id", String.class, ParameterMode.IN)
                .registerStoredProcedureParameter("p_valuation_method", String.class, ParameterMode.IN)
                .registerStoredProcedureParameter("p_as_of_date", LocalDate.class, ParameterMode.IN)
                .setParameter("p_warehouse_id", warehouseId)
                .setParameter("p_valuation_method", valuationMethod)
                .setParameter("p_as_of_date", asOfDate);
        
        query.execute();
        
        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();
        
        return results.stream()
                .map(this::mapToStockValuationItem)
                .collect(Collectors.toList());
    }
}
```

---

### 2.4 Read Replicas cho Report Queries

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                      Read Replicas Strategy                                 │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────┐         ┌─────────────────┐                          │
│  │   Primary DB    │────────►│   Read Replica  │                          │
│  │   (Write)       │  sync   │   (Read)        │                          │
│  │                 │         │                 │                          │
│  │ • CRUD ops     │         │ • Report queries│                          │
│  │ • Transactions │         │ • Analytics     │                          │
│  └─────────────────┘         └─────────────────┘                          │
│                                                                             │
│  Lợi ích:                                                                  │
│  ├── Report queries không ảnh hưởng đến write operations                   │
│  ├── Có thể optimize cho read-heavy workloads                              │
│  └── Scale horizontally                                                    │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

**Spring Configuration:**

```java
@Configuration
public class DataSourceConfig {
    
    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource.primary")
    public DataSource primaryDataSource() {
        return DataSourceBuilder.create().build();
    }
    
    @Bean
    @ConfigurationProperties("spring.datasource.replica")
    public DataSource replicaDataSource() {
        return DataSourceBuilder.create().build();
    }
    
    @Bean
    public RoutingDataSource routingDataSource(
            @Qualifier("primaryDataSource") DataSource primary,
            @Qualifier("replicaDataSource") DataSource replica) {
        
        RoutingDataSource routingDataSource = new RoutingDataSource();
        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put("primary", primary);
        targetDataSources.put("replica", replica);
        
        routingDataSource.setTargetDataSources(targetDataSources);
        routingDataSource.setDefaultTargetDataSource(primary);
        
        return routingDataSource;
    }
}

// Annotation để chỉ định datasource
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface UseReadReplica {}

// Aspect để switch datasource
@Aspect
@Component
public class ReadReplicaAspect {
    
    @Around("@annotation(useReadReplica)")
    public Object useReadReplica(ProceedingJoinPoint joinPoint, UseReadReplica useReadReplica) 
            throws Throwable {
        DataSourceContextHolder.setDataSourceType("replica");
        try {
            return joinPoint.proceed();
        } finally {
            DataSourceContextHolder.clearDataSourceType();
        }
    }
}

// Usage trong Report Service
@Service
public class ReportServiceImpl implements ReportService {
    
    @Override
    @UseReadReplica
    @Transactional(readOnly = true)
    public CurrentStockReportResponse generateCurrentStockReport(CurrentStockReportRequest request) {
        // Query sẽ dùng read replica
        return inventoryRepository.findByFilters(request.getFilters());
    }
}
```

---

## 3. Caching Strategies

### 3.1 Template Caching

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        Template Caching Strategy                            │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  Problem: JasperReports.compile() rất chậm                                │
│  Solution: Cache compiled templates trong memory                           │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                                                                     │   │
│  │   ┌─────────────┐    Cache Hit     ┌─────────────┐                 │   │
│  │   │   Request   │─────────────────►│  Compiled   │                 │   │
│  │   │   Template  │                  │  Template   │                 │   │
│  │   └─────────────┘                  │  (Memory)   │                 │   │
│  │         │                          └─────────────┘                 │   │
│  │         │ Cache Miss                                                │   │
│  │         ▼                                                          │   │
│  │   ┌─────────────┐    Load          ┌─────────────┐                 │   │
│  │   │   Load      │◄─────────────────│   .jrxml   │                 │   │
│  │   │   & Compile │                  │   Files    │                 │   │
│  │   └─────────────┘                  └─────────────┘                 │   │
│  │         │                                                          │   │
│  │         │ Store in Cache                                           │   │
│  │         ▼                                                          │   │
│  │   ┌─────────────┐                                                 │   │
│  │   │   Cache     │                                                 │   │
│  │   │   Update    │                                                 │   │
│  │   └─────────────┘                                                 │   │
│  │                                                                     │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

**Implementation:**

```java
@Component
@Slf4j
public class ReportTemplateCache {
    
    private final Map<String, JasperReport> cache = new ConcurrentHashMap<>();
    private final ResourceLoader resourceLoader;
    
    @PostConstruct
    public void init() {
        log.info("Initializing report template cache");
        // Pre-load all templates
        for (ReportType type : ReportType.values()) {
            loadTemplate(type.getTemplateName());
        }
    }
    
    public JasperReport getTemplate(String templateName) {
        return cache.computeIfAbsent(templateName, this::loadAndCompile);
    }
    
    private JasperReport loadAndCompile(String templateName) {
        log.info("Loading and compiling template: {}", templateName);
        long start = System.currentTimeMillis();
        
        try {
            Resource resource = resourceLoader.getResource(
                    "classpath:templates/reports/" + templateName + ".jrxml");
            
            try (InputStream is = resource.getInputStream()) {
                JasperReport report = JasperCompileManager.compileReport(is);
                
                long elapsed = System.currentTimeMillis() - start;
                log.info("Template {} compiled in {}ms", templateName, elapsed);
                
                return report;
            }
        } catch (Exception e) {
            throw new ReportTemplateException("Failed to load template: " + templateName, e);
        }
    }
    
    @Scheduled(fixedRate = 3600000) // Refresh every hour
    public void refreshCache() {
        log.info("Refreshing report template cache");
        cache.clear();
        init();
    }
    
    public void invalidate(String templateName) {
        cache.remove(templateName);
        log.info("Template cache invalidated: {}", templateName);
    }
}

// Usage trong Export Formatter
@Component
public class PdfExportFormatter implements ExportFormatter {
    
    private final ReportTemplateCache templateCache;
    
    @Override
    public byte[] format(ReportData data, String templateName) {
        // Get from cache - KHÔNG compile lại
        JasperReport report = templateCache.getTemplate(templateName);
        
        JasperPrint print = JasperFillManager.fillReport(
                report, 
                data.toParameters(), 
                data.toDataSource()
        );
        
        return JasperExportManager.exportReportToPdf(print);
    }
}
```

---

### 3.2 Report Result Caching (Redis)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    Report Result Caching Strategy                           │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  Cache Key: report:cache:{hash(request_params)}                            │
│  TTL: 5 minutes (configurable)                                             │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                                                                     │   │
│  │   Request ──► Hash Params ──► Check Redis ──► Cache Hit ──► Return │   │
│  │                                    │                                │   │
│  │                                    │ Cache Miss                     │   │
│  │                                    ▼                                │   │
│  │                              Generate Report                       │   │
│  │                                    │                                │   │
│  │                                    ▼                                │   │
│  │                              Store in Redis ──► Return              │   │
│  │                                                                     │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

**Implementation:**

```java
@Component
@Slf4j
public class ReportCacheService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    
    private static final String CACHE_PREFIX = "report:cache:";
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(5);
    
    public <T> Optional<T> get(String cacheKey, Class<T> type) {
        try {
            String key = CACHE_PREFIX + cacheKey;
            Object cached = redisTemplate.opsForValue().get(key);
            
            if (cached != null) {
                log.debug("Cache hit for key: {}", cacheKey);
                return Optional.of(objectMapper.convertValue(cached, type));
            }
        } catch (Exception e) {
            log.warn("Cache read error for key: {}", cacheKey, e);
        }
        return Optional.empty();
    }
    
    public void put(String cacheKey, Object value, Duration ttl) {
        try {
            String key = CACHE_PREFIX + cacheKey;
            redisTemplate.opsForValue().set(key, value, ttl);
            log.debug("Cached result for key: {}", cacheKey);
        } catch (Exception e) {
            log.warn("Cache write error for key: {}", cacheKey, e);
        }
    }
    
    public void evict(String cacheKey) {
        String key = CACHE_PREFIX + cacheKey;
        redisTemplate.delete(key);
        log.debug("Cache evicted for key: {}", cacheKey);
    }
    
    public String generateCacheKey(ReportRequest request) {
        try {
            String json = objectMapper.writeValueAsString(request);
            return DigestUtils.md5DigestAsHex(json.getBytes());
        } catch (JsonProcessingException e) {
            return UUID.randomUUID().toString();
        }
    }
}

// Usage trong Service
@Service
public class ReportServiceImpl implements ReportService {
    
    private final ReportCacheService cacheService;
    
    @Override
    public ReportResponse generateReport(ReportRequest request) {
        // 1. Generate cache key
        String cacheKey = cacheService.generateCacheKey(request);
        
        // 2. Check cache
        Optional<ReportResponse> cached = cacheService.get(cacheKey, ReportResponse.class);
        if (cached.isPresent()) {
            log.info("Returning cached report for request: {}", request.getId());
            return cached.get();
        }
        
        // 3. Generate report
        ReportResponse response = doGenerateReport(request);
        
        // 4. Store in cache
        cacheService.put(cacheKey, response, Duration.ofMinutes(5));
        
        return response;
    }
}
```

---

### 3.3 Reference Data Caching

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    Reference Data Caching Strategy                          │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  Cache các data ít thay đổi:                                               │
│  ├── Products (TTL: 1 hour)                                               │
│  ├── Warehouses (TTL: 1 hour)                                             │
│  ├── Categories (TTL: 1 hour)                                             │
│  ├── UOMs (TTL: 1 hour)                                                   │
│  └── Locations (TTL: 30 minutes)                                          │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                                                                     │   │
│  │   Report Query ──► Need Product Info ──► Check Cache               │   │
│  │                                            │                        │   │
│  │                        ┌───────────────────┘                        │   │
│  │                        │                                            │   │
│  │                   Cache Hit                                    Cache Miss
│  │                        │                                            │   │
│  │                        ▼                                            ▼   │
│  │                   Use Cached                                   Query DB │
│  │                                                                     │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

**Implementation:**

```java
@Service
@Slf4j
public class ReferenceDataCacheService {
    
    private final ProductRepository productRepository;
    private final WareHouseRepository warehouseRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    
    @Cacheable(value = "products", key = "#productId")
    public Product getProduct(String productId) {
        log.debug("Loading product from DB: {}", productId);
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
    }
    
    @Cacheable(value = "warehouses", key = "#warehouseId")
    public Warehouse getWarehouse(String warehouseId) {
        log.debug("Loading warehouse from DB: {}", warehouseId);
        return warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found"));
    }
    
    @CacheEvict(value = "products", key = "#productId")
    public void evictProduct(String productId) {
        log.debug("Evicting product cache: {}", productId);
    }
    
    @CacheEvict(value = "warehouses", key = "#warehouseId")
    public void evictWarehouse(String warehouseId) {
        log.debug("Evicting warehouse cache: {}", warehouseId);
    }
}

// Usage trong Report Generator
@Component
public class CurrentStockReportGenerator implements ReportGenerator<CurrentStockData> {
    
    private final ReferenceDataCacheService referenceCache;
    
    @Override
    public CurrentStockData generate(ReportRequest request) {
        List<Inventory> inventories = inventoryRepository.findByFilters(request.getFilters());
        
        return CurrentStockData.builder()
                .items(inventories.stream()
                        .map(inv -> {
                            // Get from cache - không query DB
                            Product product = referenceCache.getProduct(inv.getProductId());
                            Warehouse warehouse = referenceCache.getWarehouse(inv.getWarehouseId());
                            
                            return CurrentStockItem.builder()
                                    .productId(inv.getProductId())
                                    .productSku(product.getSku())
                                    .productName(product.getName())
                                    .warehouseName(warehouse.getName())
                                    // ...
                                    .build();
                        })
                        .collect(Collectors.toList()))
                .build();
    }
}
```

---

## 4. Performance Optimization Strategies

### 4.1 Pagination cho Large Datasets

```java
@Service
public class ReportServiceImpl implements ReportService {
    
    @Override
    public ReportResponse generateCurrentStockReport(CurrentStockReportRequest request) {
        // Nếu có pagination
        if (request.getPageable() != null) {
            Page<Inventory> page = inventoryRepository.findByFilters(
                    request.getFilters(), 
                    request.getPageable()
            );
            
            return CurrentStockReportResponse.builder()
                    .items(mapToItems(page.getContent()))
                    .pagination(PaginationInfo.builder()
                            .page(page.getNumber())
                            .size(page.getSize())
                            .totalElements(page.getTotalElements())
                            .totalPages(page.getTotalPages())
                            .build())
                    .build();
        }
        
        // Nếu không pagination (async export)
        List<Inventory> all = inventoryRepository.findByFilters(request.getFilters());
        return CurrentStockReportResponse.builder()
                .items(mapToItems(all))
                .build();
    }
}
```

### 4.2 Batch Processing cho Async

```java
@Component
public class ReportConsumerService {
    
    private static final int BATCH_SIZE = 1000;
    
    @RabbitListener(queues = "whs.report.generate.queue")
    public void processReportRequest(ReportMessage message) {
        log.info("Processing report request: {}", message.getRequestId());
        
        // Process in batches
        int offset = 0;
        List<ReportItem> allItems = new ArrayList<>();
        
        while (true) {
            List<Inventory> batch = inventoryRepository.findByFiltersWithLimit(
                    message.getFilters(), 
                    offset, 
                    BATCH_SIZE
            );
            
            if (batch.isEmpty()) {
                break;
            }
            
            allItems.addAll(mapToItems(batch));
            offset += BATCH_SIZE;
            
            // Update progress
            updateProgress(message.getRequestId(), offset);
            
            // Clear memory
            batch.clear();
        }
        
        // Generate report with all items
        generateReportFile(message, allItems);
    }
}
```

### 4.3 Connection Pooling

```yaml
# application.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      idle-timeout: 30000
      max-lifetime: 1800000
      connection-timeout: 30000
      
report:
  datasource:
    hikari:
      maximum-pool-size: 10  # Smaller pool for report queries
      minimum-idle: 2
```

---

## 5. Summary - Design Patterns Applied

| Pattern | Use Case | Benefit |
|---------|----------|---------|
| **Strategy** | Report generation & export formats | Flexible, extensible |
| **Factory** | Request creation | Clean object creation |
| **Template Method** | Report processing workflow | Consistent流程 |
| **Builder** | Query construction | Readable, flexible queries |
| **Chain of Responsibility** | Validation pipeline | Modular validation |
| **Observer** | Event notifications | Decoupled notifications |

| Strategy | Use Case | Benefit |
|----------|----------|---------|
| **DB Views** | Complex joins | Simplified queries |
| **Read Replicas** | Report queries | Performance isolation |
| **Template Caching** | JasperReports | Fast generation |
| **Result Caching** | Report data | Reduced DB load |
| **Reference Caching** | Products, Warehouses | Faster lookups |

---

## 6. Implementation Priority

1. **Phase 1:** Strategy Pattern + DB Views + Template Caching
2. **Phase 2:** Result Caching + Reference Caching
3. **Phase 3:** Chain of Responsibility + Observer Pattern
4. **Phase 4:** Read Replicas (if needed)
