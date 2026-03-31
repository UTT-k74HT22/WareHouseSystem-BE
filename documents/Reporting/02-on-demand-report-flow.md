# On-Demand Report Flow - Chi Tiết

> Module: WHS-67
> Updated: 2026-03-22

---

## 1. Tổng Quan On-Demand Reports

On-demand reports được tạo ngay lập tức khi có request từ client. Phù hợp cho báo cáo với dữ liệu nhỏ (< 10,000 rows).

### 1.1 Supported Report Types

| Report | Endpoint | Description |
|--------|----------|-------------|
| Current Stock | POST /api/v1/reports/current-stock | Tồn kho hiện tại |
| Stock Valuation | POST /api/v1/reports/stock-valuation | Định giá tồn kho |
| Movements | POST /api/v1/reports/movements | Lịch sử di chuyển |
| Batch Traceability | POST /api/v1/reports/batch-traceability | Truy xuất lô hàng |
| Low Stock | POST /api/v1/reports/low-stock | Tồn kho thấp |
| Expiring Batches | POST /api/v1/reports/expiring-batches | Lô sắp hết hạn |

---

## 2. Request/Response Flow

### 2.1 Request Flow

```
Client                    Controller              Service                Repository
  │                           │                      │                      │
  │  1. POST /reports/xxx     │                      │                      │
  │  Body: { filters }        │                      │                      │
  │──────────────────────────►│                      │                      │
  │                           │                      │                      │
  │                           │ 2. @Valid            │                      │
  │                           │ Validate Request     │                      │
  │                           │─────┐                │                      │
  │                           │     │ Check:         │                      │
  │                           │     │ - Required     │                      │
  │                           │     │ - Format       │                      │
  │                           │     │ - Range        │                      │
  │                           │◄────┘                │                      │
  │                           │                      │                      │
  │                           │ 3. generateReport()  │                      │
  │                           │─────────────────────►│                      │
  │                           │                      │                      │
  │                           │                      │ 4. Check Permission  │
  │                           │                      │─────┐                │
  │                           │                      │     │ - User role    │
  │                           │                      │     │ - Warehouse    │
  │                           │                      │◄────┘ access        │
  │                           │                      │                      │
  │                           │                      │ 5. Build Query      │
  │                           │                      │─────────────────────►│
  │                           │                      │                      │
  │                           │                      │ 6. Execute Query    │
  │                           │                      │                      │
  │                           │                      │ 7. Return Entities  │
  │                           │                      │◄─────────────────────│
  │                           │                      │                      │
  │                           │                      │ 8. Map to DTO       │
  │                           │                      │─────┐                │
  │                           │                      │     │ - Enrich       │
  │                           │                      │     │ - Calculate    │
  │                           │                      │◄────┘                │
  │                           │                      │                      │
  │                           │ 9. Return Response   │                      │
  │                           │◄─────────────────────│                      │
  │                           │                      │                      │
  │ 10. 200 OK                │                      │                      │
  │     { data: [...] }       │                      │                      │
  │◄──────────────────────────│                      │                      │
  │                           │                      │                      │
```

### 2.2 Response Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          Response Flow                                      │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  1. Repository Layer                                                        │
│     ├── Execute JPA CriteriaQuery or Native SQL                             │
│     ├── Return List<Entity> or Page<Entity>                                │
│     └── Apply fetch joins to avoid N+1                                      │
│                                                                             │
│  2. Service Layer                                                           │
│     ├── Convert Entity to Response DTO (using Mapper)                       │
│     ├── Calculate derived fields                                            │
│     │   ├── available_quantity = on_hand - reserved - quarantine            │
│     │   ├── stock_value = quantity * unit_cost                              │
│     │   └── days_until_expiry = expiry_date - today                         │
│     ├── Apply business rules                                                │
│     │   ├── Filter based on user permissions                               │
│     │   └── Apply currency conversion if needed                            │
│     └── Return Response DTO                                                 │
│                                                                             │
│  3. Controller Layer                                                        │
│     ├── Wrap in BaseResponse<T>                                             │
│     ├── Add metadata (timestamp, pagination)                                │
│     └── Return ResponseEntity<BaseResponse<T>>                              │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Chi Tiết Từng Report Type

### 3.1 Current Stock Report

#### Request

```json
POST /api/v1/reports/current-stock

{
  "warehouse_id": "uuid-optional",
  "category_id": "uuid-optional",
  "product_ids": ["uuid1", "uuid2"],
  "location_id": "uuid-optional",
  "include_zero_stock": false,
  "group_by": "PRODUCT",
  "sort_by": "product_name",
  "sort_direction": "ASC"
}
```

#### Query Flow

```sql
-- Step 1: Base query
SELECT 
    p.id as product_id,
    p.sku as product_sku,
    p.name as product_name,
    c.name as category_name,
    w.id as warehouse_id,
    w.name as warehouse_name,
    l.id as location_id,
    l.code as location_code,
    i.on_hand_quantity,
    i.reserved_quantity,
    i.quarantine_quantity,
    (i.on_hand_quantity - i.reserved_quantity - i.quarantine_quantity) as available_quantity,
    b.batch_number,
    b.expiry_date
FROM inventory i
INNER JOIN products p ON i.product_id = p.id
INNER JOIN warehouses w ON i.warehouse_id = w.id
LEFT JOIN locations l ON i.location_id = l.id
LEFT JOIN categories c ON p.category_id = c.id
LEFT JOIN batches b ON i.batch_id = b.id
WHERE 1=1
    AND (:warehouseId IS NULL OR i.warehouse_id = :warehouseId)
    AND (:categoryId IS NULL OR p.category_id = :categoryId)
    AND (:productIds IS NULL OR p.id IN (:productIds))
    AND (:locationId IS NULL OR i.location_id = :locationId)
    AND (:includeZeroStock = TRUE OR i.on_hand_quantity > 0)
ORDER BY p.name ASC, w.name ASC;
```

#### Response

```json
{
  "success": true,
  "message": "Success",
  "data": {
    "content": [
      {
        "product_id": "uuid",
        "product_sku": "SP001",
        "product_name": "Sản phẩm A",
        "category_name": "Điện tử",
        "warehouse_id": "uuid",
        "warehouse_name": "Kho Hà Nội",
        "location_id": "uuid",
        "location_code": "A-01-01",
        "on_hand_quantity": 100.00,
        "reserved_quantity": 10.00,
        "quarantine_quantity": 5.00,
        "available_quantity": 85.00,
        "batch_number": "BATCH-001",
        "expiry_date": "2026-12-31",
        "stock_value": 8500000.00
      }
    ],
    "page": 0,
    "size": 20,
    "total_elements": 150,
    "total_pages": 8
  },
  "timestamp": "2026-03-22T10:30:00Z"
}
```

### 3.2 Stock Valuation Report

#### Request

```json
POST /api/v1/reports/stock-valuation

{
  "warehouse_id": "uuid-optional",
  "valuation_method": "WEIGHTED_AVERAGE",
  "as_of_date": "2026-03-22",
  "currency": "VND",
  "category_id": "uuid-optional"
}
```

#### Calculation Logic

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    Stock Valuation Calculation                               │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  Method: WEIGHTED_AVERAGE                                                   │
│                                                                             │
│  For each product:                                                          │
│  1. Get all inbound movements (positive quantity_change)                    │
│  2. Calculate weighted average cost:                                        │
│     total_cost = SUM(quantity_change * unit_cost)                           │
│     total_quantity = SUM(quantity_change)                                   │
│     weighted_avg_cost = total_cost / total_quantity                         │
│                                                                             │
│  3. Calculate stock value:                                                  │
│     stock_value = on_hand_quantity * weighted_avg_cost                      │
│                                                                             │
│  Method: FIFO                                                               │
│                                                                             │
│  For each product:                                                          │
│  1. Get movements ordered by date (oldest first)                            │
│  2. Allocate current stock to oldest batches first                          │
│  3. Calculate value using batch-specific costs                              │
│                                                                             │
│  Method: LIFO                                                               │
│                                                                             │
│  For each product:                                                          │
│  1. Get movements ordered by date (newest first)                            │
│  2. Allocate current stock to newest batches first                          │
│  3. Calculate value using batch-specific costs                              │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 3.3 Movements Report

#### Request

```json
POST /api/v1/reports/movements

{
  "warehouse_id": "uuid-optional",
  "product_id": "uuid-optional",
  "movement_type": "INBOUND",
  "date_from": "2026-03-01",
  "date_to": "2026-03-22",
  "reference_type": "PURCHASE_ORDER",
  "page": 0,
  "size": 50
}
```

#### Query Flow

```sql
SELECT 
    sm.id,
    sm.movement_type,
    sm.movement_date,
    p.sku as product_sku,
    p.name as product_name,
    w.name as warehouse_name,
    l.code as location_code,
    b.batch_number,
    sm.quantity_change,
    sm.quantity_before,
    sm.quantity_after,
    sm.reference_type,
    sm.reference_number,
    sm.notes
FROM stock_movements sm
INNER JOIN products p ON sm.product_id = p.id
INNER JOIN warehouses w ON sm.warehouse_id = w.id
LEFT JOIN locations l ON sm.location_id = l.id
LEFT JOIN batches b ON sm.batch_id = b.id
WHERE sm.movement_date BETWEEN :dateFrom AND :dateTo
    AND (:warehouseId IS NULL OR sm.warehouse_id = :warehouseId)
    AND (:productId IS NULL OR sm.product_id = :productId)
    AND (:movementType IS NULL OR sm.movement_type = :movementType)
    AND (:referenceType IS NULL OR sm.reference_type = :referenceType)
ORDER BY sm.movement_date DESC, sm.created_at DESC
LIMIT :limit OFFSET :offset;
```

### 3.4 Batch Traceability Report

#### Request

```json
POST /api/v1/reports/batch-traceability

{
  "batch_id": "uuid",
  "include_movements": true,
  "include_documents": true
}
```

#### Response Structure

```json
{
  "success": true,
  "data": {
    "batch": {
      "id": "uuid",
      "batch_number": "BATCH-001",
      "product_id": "uuid",
      "product_name": "Sản phẩm A",
      "manufacture_date": "2026-01-01",
      "expiry_date": "2026-12-31",
      "supplier_id": "uuid",
      "supplier_name": "Nhà cung cấp X",
      "status": "ACTIVE"
    },
    "current_locations": [
      {
        "warehouse_id": "uuid",
        "warehouse_name": "Kho Hà Nội",
        "location_id": "uuid",
        "location_code": "A-01-01",
        "quantity": 50.00
      }
    ],
    "movements": [
      {
        "id": "uuid",
        "movement_type": "INBOUND",
        "movement_date": "2026-01-15T08:00:00Z",
        "quantity_change": 100.00,
        "reference_type": "PURCHASE_ORDER",
        "reference_id": "uuid",
        "reference_number": "PO-001"
      }
    ],
    "documents": {
      "purchase_orders": [
        {
          "id": "uuid",
          "po_number": "PO-001",
          "supplier_name": "Nhà cung cấp X",
          "order_date": "2026-01-10",
          "quantity": 100.00
        }
      ],
      "inbound_receipts": [
        {
          "id": "uuid",
          "receipt_number": "IR-001",
          "receipt_date": "2026-01-15",
          "quantity": 100.00
        }
      ],
      "sales_orders": [],
      "outbound_shipments": []
    },
    "summary": {
      "total_inbound": 100.00,
      "total_outbound": 50.00,
      "current_stock": 50.00
    }
  }
}
```

### 3.5 Low Stock Report

#### Request

```json
POST /api/v1/reports/low-stock

{
  "warehouse_id": "uuid-optional",
  "category_id": "uuid-optional",
  "below_reorder_point": true,
  "below_min_stock": true
}
```

#### Query Logic

```sql
SELECT 
    p.id as product_id,
    p.sku,
    p.name as product_name,
    p.min_stock_level,
    p.reorder_point,
    p.cost_price,
    p.selling_price,
    w.id as warehouse_id,
    w.name as warehouse_name,
    SUM(i.on_hand_quantity) as total_on_hand,
    SUM(i.reserved_quantity) as total_reserved,
    SUM(i.on_hand_quantity - i.reserved_quantity - i.quarantine_quantity) as available_quantity,
    (p.reorder_point - SUM(i.on_hand_quantity - i.reserved_quantity - i.quarantine_quantity)) as quantity_to_reorder
FROM products p
INNER JOIN inventory i ON p.id = i.product_id
INNER JOIN warehouses w ON i.warehouse_id = w.id
WHERE p.status = 'ACTIVE'
    AND (:warehouseId IS NULL OR i.warehouse_id = :warehouseId)
    AND (:categoryId IS NULL OR p.category_id = :categoryId)
GROUP BY p.id, w.id
HAVING 
    (:belowReorderPoint = TRUE AND available_quantity < p.reorder_point)
    OR (:belowMinStock = TRUE AND available_quantity < p.min_stock_level)
ORDER BY (available_quantity / p.reorder_point) ASC;
```

### 3.6 Expiring Batches Report

#### Request

```json
POST /api/v1/reports/expiring-batches

{
  "warehouse_id": "uuid-optional",
  "days_until_expiry": 30,
  "include_expired": false,
  "category_id": "uuid-optional"
}
```

#### Query Logic

```sql
SELECT 
    b.id as batch_id,
    b.batch_number,
    b.manufacture_date,
    b.expiry_date,
    DATEDIFF(b.expiry_date, CURDATE()) as days_remaining,
    p.id as product_id,
    p.sku,
    p.name as product_name,
    w.id as warehouse_id,
    w.name as warehouse_name,
    l.code as location_code,
    i.on_hand_quantity,
    i.quarantine_quantity
FROM batches b
INNER JOIN inventory i ON b.id = i.batch_id
INNER JOIN products p ON i.product_id = p.id
INNER JOIN warehouses w ON i.warehouse_id = w.id
LEFT JOIN locations l ON i.location_id = l.id
WHERE b.expiry_date IS NOT NULL
    AND i.on_hand_quantity > 0
    AND (:warehouseId IS NULL OR i.warehouse_id = :warehouseId)
    AND (:categoryId IS NULL OR p.category_id = :categoryId)
    AND (
        (:includeExpired = TRUE AND b.expiry_date <= DATE_ADD(CURDATE(), INTERVAL :daysUntilExpiry DAY))
        OR (:includeExpired = FALSE AND b.expiry_date BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL :daysUntilExpiry DAY))
    )
ORDER BY b.expiry_date ASC, p.name ASC;
```

---

## 4. Export Flow (PDF/Excel/CSV)

### 4.1 Export Request

```
POST /api/v1/reports/current-stock/export?format=pdf

Body: {
  "warehouse_id": "uuid",
  "include_zero_stock": false
}
```

### 4.2 Export Processing Flow

```
┌────────┐     ┌────────────┐     ┌────────────┐     ┌────────────┐     ┌────────┐
│ Client │────►│ Controller │────►│  Service   │────►│ Generator  │────►│ Response│
└────────┘     └────────────┘     └────────────┘     └────────────┘     └────────┘
    │               │                   │                   │                 │
    │ 1. Request    │                   │                   │                 │
    │──────────────►│                   │                   │                 │
    │               │ 2. Validate       │                   │                 │
    │               │──────────────────►│                   │                 │
    │               │                   │ 3. Query Data     │                 │
    │               │                   │─────┐             │                 │
    │               │                   │     │ Same as     │                 │
    │               │                   │◄────┘ on-demand   │                 │
    │               │                   │                   │                 │
    │               │                   │ 4. Generate       │                 │
    │               │                   │    Report         │                 │
    │               │                   │──────────────────►│                 │
    │               │                   │                   │                 │
    │               │                   │                   │ 5. PDF:         │
    │               │                   │                   │ - Fill template │
    │               │                   │                   │ - Export to byte│
    │               │                   │                   │                 │
    │               │                   │                   │ 6. Excel:       │
    │               │                   │                   │ - Create sheet  │
    │               │                   │                   │ - Add rows      │
    │               │                   │                   │ - Write to byte │
    │               │                   │                   │                 │
    │               │                   │                   │ 7. CSV:         │
    │               │                   │                   │ - Write header  │
    │               │                   │                   │ - Write rows    │
    │               │                   │                   │ - To byte[]     │
    │               │                   │                   │                 │
    │               │                   │ 8. Return byte[]  │                 │
    │               │                   │◄──────────────────│                 │
    │               │ 9. Return File    │                   │                 │
    │               │◄──────────────────│                   │                 │
    │ 10. File      │                   │                   │                 │
    │◄──────────────│                   │                   │                 │
    │               │                   │                   │                 │
```

### 4.3 Response Headers

```http
HTTP/1.1 200 OK
Content-Type: application/pdf
Content-Disposition: attachment; filename="current_stock_report_20260322.pdf"
Content-Length: 123456

[binary data]
```

---

## 5. Error Handling

### 5.1 Validation Errors

```json
{
  "success": false,
  "error_code": "VALIDATION_ERROR",
  "message": "Invalid request parameters",
  "field_errors": [
    {
      "field": "date_from",
      "message": "Date from must be before date to"
    },
    {
      "field": "warehouse_id",
      "message": "Warehouse not found"
    }
  ],
  "timestamp": "2026-03-22T10:30:00Z"
}
```

### 5.2 Business Errors

```json
{
  "success": false,
  "error_code": "NO_DATA_FOUND",
  "message": "No inventory data found for the specified criteria",
  "timestamp": "2026-03-22T10:30:00Z"
}
```

### 5.3 System Errors

```json
{
  "success": false,
  "error_code": "REPORT_GENERATION_FAILED",
  "message": "Failed to generate report due to system error",
  "timestamp": "2026-03-22T10:30:00Z"
}
```

---

## 6. Implementation Notes

### 6.1 Service Implementation Pattern

```java
@Service
@Slf4j
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ReportRepositoryCustom reportRepository;
    private final ReportMapper reportMapper;
    private final ReportValidator reportValidator;
    private final ReportGenerator reportGenerator;

    @Override
    @Transactional(readOnly = true)
    public CurrentStockReportResponse generateCurrentStockReport(
            CurrentStockReportRequest request) {
        
        log.info("Generating current stock report with filters: {}", request);
        
        // 1. Validate request
        reportValidator.validateCurrentStockRequest(request);
        
        // 2. Query data
        List<Inventory> inventories = reportRepository.findCurrentStock(request);
        
        // 3. Map to DTO
        List<CurrentStockItem> items = reportMapper.toCurrentStockItems(inventories);
        
        // 4. Calculate totals
        CurrentStockSummary summary = calculateSummary(items);
        
        // 5. Build response
        return CurrentStockReportResponse.builder()
                .items(items)
                .summary(summary)
                .generatedAt(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportCurrentStockReport(
            CurrentStockReportRequest request, 
            ExportFormat format) {
        
        log.info("Exporting current stock report in {} format", format);
        
        // 1. Generate report data
        CurrentStockReportResponse reportData = generateCurrentStockReport(request);
        
        // 2. Export to format
        return switch (format) {
            case PDF -> reportGenerator.generatePdf(reportData, "current_stock");
            case EXCEL -> reportGenerator.generateExcel(reportData, "current_stock");
            case CSV -> reportGenerator.generateCsv(reportData, "current_stock");
        };
    }
}
```

### 6.2 Repository Implementation Pattern

```java
@Repository
public class ReportRepositoryCustomImpl implements ReportRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<Inventory> findCurrentStock(CurrentStockReportRequest request) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Inventory> query = cb.createQuery(Inventory.class);
        Root<Inventory> root = query.from(Inventory.class);
        
        List<Predicate> predicates = new ArrayList<>();
        
        if (request.getWarehouseId() != null) {
            predicates.add(cb.equal(root.get("warehouseId"), request.getWarehouseId()));
        }
        
        if (request.getProductId() != null) {
            predicates.add(cb.equal(root.get("productId"), request.getProductId()));
        }
        
        if (!request.isIncludeZeroStock()) {
            predicates.add(cb.greaterThan(root.get("onHandQuantity"), BigDecimal.ZERO));
        }
        
        query.where(predicates.toArray(new Predicate[0]));
        query.orderBy(cb.asc(root.get("productId")));
        
        return entityManager.createQuery(query).getResultList();
    }
}
```

---

## 7. Testing Strategy

### 7.1 Unit Tests

```java
@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    @Mock
    private ReportRepositoryCustom reportRepository;
    
    @Mock
    private ReportMapper reportMapper;
    
    @InjectMocks
    private ReportServiceImpl reportService;

    @Test
    void should_GenerateCurrentStockReport_When_ValidRequest() {
        // Given
        CurrentStockReportRequest request = new CurrentStockReportRequest();
        request.setWarehouseId("uuid");
        
        List<Inventory> mockInventories = Arrays.asList(new Inventory());
        when(reportRepository.findCurrentStock(any())).thenReturn(mockInventories);
        
        // When
        CurrentStockReportResponse response = reportService.generateCurrentStockReport(request);
        
        // Then
        assertNotNull(response);
        verify(reportRepository).findCurrentStock(any());
    }
}
```

### 7.2 Integration Tests

```java
@SpringBootTest
@AutoConfigureMockMvc
class ReportControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void should_ReturnCurrentStockReport_When_ValidRequest() throws Exception {
        CurrentStockReportRequest request = new CurrentStockReportRequest();
        
        mockMvc.perform(post("/api/v1/reports/current-stock")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isArray());
    }
}
```

---

## 8. Documentation References

- JasperReports Template Design: [JasperSoft Studio](https://community.jaspersoft.com/project/jaspersoft-studio)
- Spring Data JPA Criteria API: [Spring Docs](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#specifications)
- Jackson JSON Processing: [Jackson Docs](https://github.com/FasterXML/jackson)
