# Implementation Guide - Warehouse Management System
## Hướng Dẫn Triển Khai Chi Tiết

---

## 📋 Mục Lục
1. [Roadmap Implementation](#roadmap-implementation)
2. [Phase-by-Phase Guide](#phase-by-phase-guide)
3. [Module Implementation Details](#module-implementation-details)
4. [Best Practices](#best-practices)
5. [Testing Strategy](#testing-strategy)

---

## 🗺️ Roadmap Implementation

### Overview Timeline - 14 Weeks

```
Week 1-2:   ✅ Foundation Setup
Week 3-4:   🔄 Master Data Module
Week 5-6:   🔄 Inventory & Batch Module
Week 7-8:   ⏳ Inbound Module
Week 9-10:  ⏳ Outbound Module
Week 11:    ⏳ Reporting & Import
Week 12:    ⏳ Notifications & Integration
Week 13:    ⏳ Testing & Bug Fixing
Week 14:    ⏳ Deployment & Documentation
```

### Progress Tracker

| Phase | Status | Completion |
|-------|--------|------------|
| Phase 1: Foundation | ✅ Done | 100% |
| Phase 2: Master Data | 🔄 In Progress | 0% |
| Phase 3: Core Business | ⏳ Pending | 0% |
| Phase 4: Advanced Features | ⏳ Pending | 0% |
| Phase 5: Testing & Deploy | ⏳ Pending | 0% |

---

## 📅 Phase-by-Phase Guide

## Phase 1: Foundation Setup ✅ (COMPLETED)

### Week 1-2: Project Setup & Auth Module

#### ✅ Đã Hoàn Thành:
1. **Project Initialization**
   - Spring Boot 3.5.9 project setup
   - Maven dependencies configuration
   - Docker Compose for MySQL, Redis, RabbitMQ
   - Application properties configuration

2. **Auth & RBAC Module**
   - User authentication with JWT
   - Role-based access control
   - Token refresh mechanism
   - Rate limiting on auth endpoints
   - Password encryption with BCrypt

3. **Database Migration**
   - Flyway setup
   - Initial schema: accounts, roles, permissions
   - Sample data insertion

4. **Testing**
   - Unit tests for AuthService
   - Integration tests for AuthController
   - Rate limit tests

---

## Phase 2: Master Data Module 🔄 (NEXT PRIORITY)

### Week 3-4: Master Data Implementation

#### 📝 Tasks Checklist:

#### 1. Units of Measure (UOM)
- [ ] Create Entity: `UnitOfMeasure.java`
- [ ] Create Repository: `UnitOfMeasureRepository.java`
- [ ] Create DTOs: `UomRequest.java`, `UomResponse.java`
- [ ] Create Service: `UomService.java`
- [ ] Create Controller: `UomController.java`
- [ ] Add Validation
- [ ] Write Unit Tests
- [ ] Write Integration Tests

**Implementation Steps:**

```java
// Step 1: Create Entity
@Entity
@Table(name = "units_of_measure")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UnitOfMeasure {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false, length = 10)
    private String code;
    
    @Column(nullable = false, length = 50)
    private String name;
    
    private String description;
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
}

// Step 2: Create Repository
@Repository
public interface UnitOfMeasureRepository extends JpaRepository<UnitOfMeasure, Long> {
    Optional<UnitOfMeasure> findByCode(String code);
    boolean existsByCode(String code);
}

// Step 3: Create DTOs
@Data
@Builder
public class UomRequest {
    @NotBlank(message = "Code is required")
    @Size(min = 1, max = 10)
    private String code;
    
    @NotBlank(message = "Name is required")
    @Size(min = 3, max = 50)
    private String name;
    
    @Size(max = 255)
    private String description;
}

@Data
@Builder
public class UomResponse {
    private Long id;
    private String code;
    private String name;
    private String description;
    private LocalDateTime createdAt;
}

// Step 4: Create Service
@Service
@Transactional
public class UomServiceImpl implements UomService {
    
    @Autowired
    private UnitOfMeasureRepository uomRepository;
    
    @Autowired
    private UomMapper uomMapper;
    
    @Override
    public UomResponse create(UomRequest request) {
        if (uomRepository.existsByCode(request.getCode())) {
            throw new DuplicateException("UOM code already exists: " + request.getCode());
        }
        
        UnitOfMeasure uom = uomMapper.toEntity(request);
        UnitOfMeasure saved = uomRepository.save(uom);
        return uomMapper.toResponse(saved);
    }
    
    @Override
    public Page<UomResponse> findAll(Pageable pageable) {
        return uomRepository.findAll(pageable)
            .map(uomMapper::toResponse);
    }
    
    // ... other methods
}

// Step 5: Create Controller
@RestController
@RequestMapping("/api/uoms")
@RequiredArgsConstructor
public class UomController {
    
    private final UomService uomService;
    
    @PostMapping
    @PreAuthorize("hasPermission('UOM', 'CREATE')")
    public ResponseEntity<UomResponse> create(@Valid @RequestBody UomRequest request) {
        UomResponse response = uomService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping
    public ResponseEntity<Page<UomResponse>> findAll(
        @PageableDefault(size = 20, sort = "name") Pageable pageable
    ) {
        return ResponseEntity.ok(uomService.findAll(pageable));
    }
    
    // ... other endpoints
}
```

#### 2. Warehouses
- [ ] Create Entity: `Warehouse.java`
- [ ] Create Repository with custom queries
- [ ] Implement caching with Redis
- [ ] Add audit fields (created_by, updated_by)
- [ ] Implement soft delete with status
- [ ] Add business validation
- [ ] Write comprehensive tests

**Key Features:**
```java
@Entity
@Table(name = "warehouses")
@Data
@EntityListeners(AuditingEntityListener.class)
public class Warehouse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String code;
    
    private String name;
    private String address;
    private String city;
    private String country;
    
    @Enumerated(EnumType.STRING)
    private WarehouseType type; // MAIN, SATELLITE, TRANSIT
    
    @Enumerated(EnumType.STRING)
    private WarehouseStatus status; // ACTIVE, INACTIVE
    
    @ManyToOne
    @JoinColumn(name = "manager_id")
    private Account manager;
    
    @OneToMany(mappedBy = "warehouse", cascade = CascadeType.ALL)
    private List<Location> locations = new ArrayList<>();
    
    @CreatedBy
    @ManyToOne
    private Account createdBy;
    
    @LastModifiedBy
    @ManyToOne
    private Account updatedBy;
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
}

// Caching Strategy
@Cacheable(value = "warehouses", key = "#code")
public Optional<Warehouse> findByCode(String code) {
    return warehouseRepository.findByCode(code);
}

@CacheEvict(value = "warehouses", key = "#warehouse.code")
public void update(Warehouse warehouse) {
    warehouseRepository.save(warehouse);
}
```

#### 3. Locations
- [ ] Create Entity with composite unique key
- [ ] Link to Warehouse (parent-child)
- [ ] Implement capacity tracking
- [ ] Add zone management
- [ ] Create search by warehouse endpoint

#### 4. Products
- [ ] Create Entity with full-text search
- [ ] Implement SKU validation
- [ ] Add caching for frequently accessed products
- [ ] Implement min/max stock level tracking
- [ ] Add batch tracking flag
- [ ] Create bulk import preparation

**Advanced Features:**
```java
// Full-text search
@Query("SELECT p FROM Product p WHERE " +
       "LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
       "LOWER(p.sku) LIKE LOWER(CONCAT('%', :keyword, '%'))")
List<Product> searchByKeyword(@Param("keyword") String keyword);

// Cache frequently accessed products
@Cacheable(value = "products", key = "#sku")
public ProductResponse findBySku(String sku) {
    Product product = productRepository.findBySku(sku)
        .orElseThrow(() -> new NotFoundException("Product not found: " + sku));
    return productMapper.toResponse(product);
}
```

#### 5. Business Partners
- [ ] Create Entity with type (SUPPLIER/CUSTOMER)
- [ ] Implement contact information
- [ ] Add credit limit tracking
- [ ] Create search and filter endpoints

---

## Phase 3: Core Business Modules

### Week 5-6: Inventory & Batch Management

#### 1. Batch Module
```java
@Entity
@Table(name = "batches")
public class Batch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String batchNumber;
    
    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;
    
    private LocalDate manufactureDate;
    private LocalDate expiryDate;
    
    @Enumerated(EnumType.STRING)
    private BatchStatus status;
    
    // Business logic: Check if expired
    public boolean isExpired() {
        return expiryDate != null && 
               expiryDate.isBefore(LocalDate.now());
    }
    
    // Business logic: Check if expiring soon
    public boolean isExpiringSoon(int days) {
        return expiryDate != null && 
               expiryDate.isBefore(LocalDate.now().plusDays(days));
    }
}

// Service method: Find expiring batches
public List<BatchResponse> findExpiringSoon(int days) {
    LocalDate thresholdDate = LocalDate.now().plusDays(days);
    return batchRepository.findByExpiryDateBeforeAndStatus(
        thresholdDate, BatchStatus.ACTIVE
    ).stream()
     .map(batchMapper::toResponse)
     .collect(Collectors.toList());
}
```

**Tasks:**
- [ ] Implement Batch entity with expiry tracking
- [ ] Create batch validation (expiry dates)
- [ ] Add expiring soon alert endpoint
- [ ] Write batch lifecycle tests

#### 2. Inventory Module

**Core Inventory Logic:**
```java
@Service
@Transactional
public class InventoryServiceImpl implements InventoryService {
    
    @Autowired
    private InventoryRepository inventoryRepository;
    
    @Autowired
    private StockMovementService stockMovementService;
    
    /**
     * Increase stock (from inbound receipt)
     */
    @Override
    public void increaseStock(IncreaseStockRequest request) {
        // Find or create inventory record
        Inventory inventory = inventoryRepository
            .findByProductAndWarehouseAndLocationAndBatch(
                request.getProductId(),
                request.getWarehouseId(),
                request.getLocationId(),
                request.getBatchId()
            )
            .orElseGet(() -> createNewInventory(request));
        
        // Increase quantity
        BigDecimal newQuantity = inventory.getOnHandQuantity()
            .add(request.getQuantity());
        inventory.setOnHandQuantity(newQuantity);
        
        // Save with optimistic locking
        Inventory saved = inventoryRepository.save(inventory);
        
        // Create stock movement record
        stockMovementService.recordMovement(
            StockMovementRequest.builder()
                .movementType(MovementType.INBOUND)
                .productId(request.getProductId())
                .warehouseId(request.getWarehouseId())
                .locationId(request.getLocationId())
                .batchId(request.getBatchId())
                .quantityChange(request.getQuantity())
                .referenceType("inbound_receipt")
                .referenceId(request.getReceiptId())
                .build()
        );
    }
    
    /**
     * Reserve stock (from sales order confirmation)
     */
    @Override
    public void reserveStock(ReserveStockRequest request) {
        Inventory inventory = findInventory(request);
        
        // Check available quantity
        if (inventory.getAvailableQuantity()
                .compareTo(request.getQuantity()) < 0) {
            throw new InsufficientStockException(
                "Not enough stock available. Required: " + 
                request.getQuantity() + ", Available: " + 
                inventory.getAvailableQuantity()
            );
        }
        
        // Increase reserved quantity
        BigDecimal newReserved = inventory.getReservedQuantity()
            .add(request.getQuantity());
        inventory.setReservedQuantity(newReserved);
        
        inventoryRepository.save(inventory);
    }
    
    /**
     * Decrease stock (from outbound shipment)
     */
    @Override
    public void decreaseStock(DecreaseStockRequest request) {
        Inventory inventory = findInventory(request);
        
        // Decrease both on-hand and reserved
        BigDecimal newOnHand = inventory.getOnHandQuantity()
            .subtract(request.getQuantity());
        BigDecimal newReserved = inventory.getReservedQuantity()
            .subtract(request.getQuantity());
        
        // Validate not negative
        if (newOnHand.compareTo(BigDecimal.ZERO) < 0) {
            throw new NegativeStockException(
                "Stock cannot be negative"
            );
        }
        
        inventory.setOnHandQuantity(newOnHand);
        inventory.setReservedQuantity(newReserved);
        
        inventoryRepository.save(inventory);
        
        // Record movement
        stockMovementService.recordMovement(
            StockMovementRequest.builder()
                .movementType(MovementType.OUTBOUND)
                .productId(request.getProductId())
                .quantityChange(request.getQuantity().negate())
                .referenceType("outbound_shipment")
                .referenceId(request.getShipmentId())
                .build()
        );
    }
    
    /**
     * Manual adjustment with approval
     */
    @Override
    public AdjustmentResponse adjustStock(AdjustmentRequest request) {
        // Create adjustment record
        InventoryAdjustment adjustment = InventoryAdjustment.builder()
            .adjustmentNumber(generateAdjustmentNumber())
            .productId(request.getProductId())
            .warehouseId(request.getWarehouseId())
            .quantityChange(request.getQuantityChange())
            .reason(request.getReason())
            .status(AdjustmentStatus.PENDING_APPROVAL)
            .build();
        
        // Save adjustment (not applied yet)
        adjustment = adjustmentRepository.save(adjustment);
        
        // If small adjustment, auto-approve
        if (shouldAutoApprove(request)) {
            approveAdjustment(adjustment.getId());
        }
        
        return adjustmentMapper.toResponse(adjustment);
    }
}
```

**Tasks:**
- [ ] Implement Inventory entity with optimistic locking
- [ ] Create increase/decrease/reserve stock methods
- [ ] Add validation for negative stock
- [ ] Implement adjustment workflow with approval
- [ ] Add low stock alert detection
- [ ] Write concurrency tests

### Week 7-8: Inbound Module

**Purchase Order Workflow:**
```java
@Service
@Transactional
public class PurchaseOrderServiceImpl implements PurchaseOrderService {
    
    @Override
    public PurchaseOrderResponse create(PurchaseOrderRequest request) {
        // 1. Validate supplier and warehouse
        validateSupplierAndWarehouse(request);
        
        // 2. Create PO with DRAFT status
        PurchaseOrder po = PurchaseOrder.builder()
            .orderNumber(generateOrderNumber())
            .supplierId(request.getSupplierId())
            .warehouseId(request.getWarehouseId())
            .orderDate(LocalDate.now())
            .status(PurchaseOrderStatus.DRAFT)
            .build();
        
        // 3. Add lines
        List<PurchaseOrderLine> lines = request.getLines().stream()
            .map(lineReq -> createOrderLine(po, lineReq))
            .collect(Collectors.toList());
        po.setLines(lines);
        
        // 4. Calculate total
        po.setTotalAmount(calculateTotal(lines));
        
        // 5. Save
        PurchaseOrder saved = poRepository.save(po);
        
        return poMapper.toResponse(saved);
    }
    
    @Override
    public void confirm(Long poId) {
        PurchaseOrder po = findById(poId);
        
        // Validate can confirm
        if (po.getStatus() != PurchaseOrderStatus.DRAFT) {
            throw new InvalidStatusException(
                "Only DRAFT orders can be confirmed"
            );
        }
        
        // Update status
        po.setStatus(PurchaseOrderStatus.CONFIRMED);
        po.setConfirmedAt(LocalDateTime.now());
        po.setConfirmedBy(getCurrentUser());
        
        poRepository.save(po);
        
        // Send notification
        notificationService.notifyPOConfirmed(po);
    }
}

@Service
@Transactional
public class InboundReceiptServiceImpl implements InboundReceiptService {
    
    @Override
    public ReceiptResponse createReceipt(ReceiptRequest request) {
        // 1. Validate PO exists and confirmed
        PurchaseOrder po = validatePurchaseOrder(request.getPurchaseOrderId());
        
        // 2. Create receipt with DRAFT status
        InboundReceipt receipt = InboundReceipt.builder()
            .receiptNumber(generateReceiptNumber())
            .purchaseOrderId(request.getPurchaseOrderId())
            .warehouseId(request.getWarehouseId())
            .receiptDate(LocalDate.now())
            .status(ReceiptStatus.DRAFT)
            .build();
        
        // 3. Add lines with batch info
        List<InboundReceiptLine> lines = request.getLines().stream()
            .map(lineReq -> createReceiptLine(receipt, lineReq))
            .collect(Collectors.toList());
        receipt.setLines(lines);
        
        // 4. Save
        InboundReceipt saved = receiptRepository.save(receipt);
        
        return receiptMapper.toResponse(saved);
    }
    
    @Override
    public void confirmReceipt(Long receiptId) {
        InboundReceipt receipt = findById(receiptId);
        
        // Validate can confirm
        if (receipt.getStatus() != ReceiptStatus.DRAFT) {
            throw new InvalidStatusException(
                "Only DRAFT receipts can be confirmed"
            );
        }
        
        // Process each line: increase inventory
        for (InboundReceiptLine line : receipt.getLines()) {
            inventoryService.increaseStock(
                IncreaseStockRequest.builder()
                    .productId(line.getProductId())
                    .warehouseId(receipt.getWarehouseId())
                    .locationId(line.getLocationId())
                    .batchId(line.getBatchId())
                    .quantity(line.getReceivedQuantity())
                    .receiptId(receiptId)
                    .build()
            );
            
            // Update PO line received quantity
            updatePOLineReceived(line);
        }
        
        // Update receipt status
        receipt.setStatus(ReceiptStatus.CONFIRMED);
        receipt.setConfirmedAt(LocalDateTime.now());
        receipt.setConfirmedBy(getCurrentUser());
        
        receiptRepository.save(receipt);
        
        // Check if PO is completed
        checkAndCompletePO(receipt.getPurchaseOrderId());
        
        // Send notification
        notificationService.notifyReceiptConfirmed(receipt);
    }
}
```

**Tasks:**
- [ ] Implement PO creation and confirmation
- [ ] Implement receipt creation with batch tracking
- [ ] Link receipt confirmation to inventory increase
- [ ] Add validation for over-receiving
- [ ] Handle partial receipts
- [ ] Write integration tests for full flow

### Week 9-10: Outbound Module

**Sales Order & Shipment Flow:**
```java
@Service
@Transactional
public class SalesOrderServiceImpl implements SalesOrderService {
    
    @Override
    public void confirm(Long soId) {
        SalesOrder so = findById(soId);
        
        // Validate stock availability
        for (SalesOrderLine line : so.getLines()) {
            BigDecimal available = inventoryService
                .getAvailableStock(line.getProductId(), so.getWarehouseId());
            
            if (available.compareTo(line.getOrderedQuantity()) < 0) {
                throw new InsufficientStockException(
                    "Not enough stock for product: " + line.getProduct().getSku()
                );
            }
        }
        
        // Reserve stock for all lines
        for (SalesOrderLine line : so.getLines()) {
            inventoryService.reserveStock(
                ReserveStockRequest.builder()
                    .productId(line.getProductId())
                    .warehouseId(so.getWarehouseId())
                    .quantity(line.getOrderedQuantity())
                    .salesOrderId(soId)
                    .build()
            );
        }
        
        // Update status
        so.setStatus(SalesOrderStatus.CONFIRMED);
        so.setConfirmedAt(LocalDateTime.now());
        
        soRepository.save(so);
    }
}

@Service
@Transactional
public class OutboundShipmentServiceImpl implements OutboundShipmentService {
    
    @Override
    public void confirmShipment(Long shipmentId) {
        OutboundShipment shipment = findById(shipmentId);
        
        // Validate status
        if (shipment.getStatus() != ShipmentStatus.PICKED) {
            throw new InvalidStatusException(
                "Only PICKED shipments can be confirmed"
            );
        }
        
        // Decrease stock for each line
        for (OutboundShipmentLine line : shipment.getLines()) {
            inventoryService.decreaseStock(
                DecreaseStockRequest.builder()
                    .productId(line.getProductId())
                    .warehouseId(shipment.getWarehouseId())
                    .locationId(line.getLocationId())
                    .batchId(line.getBatchId())
                    .quantity(line.getShippedQuantity())
                    .shipmentId(shipmentId)
                    .build()
            );
            
            // Update SO line shipped quantity
            updateSOLineShipped(line);
        }
        
        // Update shipment status
        shipment.setStatus(ShipmentStatus.SHIPPED);
        shipment.setShippedAt(LocalDateTime.now());
        shipment.setShippedBy(getCurrentUser());
        
        shipmentRepository.save(shipment);
        
        // Check if SO is completed
        checkAndCompleteSO(shipment.getSalesOrderId());
        
        // Send notification
        notificationService.notifyShipmentConfirmed(shipment);
    }
}
```

**Tasks:**
- [ ] Implement SO creation and confirmation with stock reservation
- [ ] Implement shipment creation with picking workflow
- [ ] Link shipment confirmation to inventory decrease
- [ ] Handle cancellation and unreserve stock
- [ ] Support partial shipments
- [ ] Write comprehensive tests

---

## Phase 4: Advanced Features

### Week 11: Reporting & Import

#### Reporting with RabbitMQ
```java
@Service
public class ReportServiceImpl implements ReportService {
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    @Autowired
    private ReportJobRepository reportJobRepository;
    
    @Override
    public ReportJobResponse generateInventoryReport(ReportRequest request) {
        // 1. Create job record
        ReportJob job = ReportJob.builder()
            .jobId(UUID.randomUUID().toString())
            .reportType("INVENTORY_SNAPSHOT")
            .format(request.getFormat())
            .status(JobStatus.PENDING)
            .filters(objectMapper.writeValueAsString(request.getFilters()))
            .build();
        
        reportJobRepository.save(job);
        
        // 2. Send to RabbitMQ
        rabbitTemplate.convertAndSend(
            "report-generation-queue",
            job.getJobId()
        );
        
        return reportMapper.toResponse(job);
    }
}

// Worker to process reports
@Component
public class ReportWorker {
    
    @RabbitListener(queues = "report-generation-queue")
    public void processReport(String jobId) {
        ReportJob job = reportJobRepository.findByJobId(jobId)
            .orElseThrow();
        
        try {
            // Update status
            job.setStatus(JobStatus.PROCESSING);
            job.setStartedAt(LocalDateTime.now());
            reportJobRepository.save(job);
            
            // Generate report based on type
            byte[] reportData = generateReport(job);
            
            // Save file
            String filePath = saveReportFile(job, reportData);
            
            // Update job
            job.setStatus(JobStatus.COMPLETED);
            job.setFilePath(filePath);
            job.setFileSize((long) reportData.length);
            job.setCompletedAt(LocalDateTime.now());
            reportJobRepository.save(job);
            
            // Notify user
            notificationService.notifyReportReady(job);
            
        } catch (Exception e) {
            job.setStatus(JobStatus.FAILED);
            job.setErrorMessage(e.getMessage());
            reportJobRepository.save(job);
        }
    }
}
```

#### Excel Import with Validation
```java
@Service
public class ImportServiceImpl implements ImportService {
    
    @Override
    public ImportJobResponse importProducts(MultipartFile file) {
        // 1. Validate file
        validateExcelFile(file);
        
        // 2. Create job
        ImportJob job = ImportJob.builder()
            .jobId(UUID.randomUUID().toString())
            .importType("PRODUCTS")
            .fileName(file.getOriginalFilename())
            .status(JobStatus.PENDING)
            .build();
        
        importJobRepository.save(job);
        
        // 3. Save file temporarily
        String tempPath = saveTemporaryFile(file);
        
        // 4. Send to RabbitMQ
        rabbitTemplate.convertAndSend(
            "import-processing-queue",
            ImportMessage.builder()
                .jobId(job.getJobId())
                .filePath(tempPath)
                .build()
        );
        
        return importMapper.toResponse(job);
    }
}

// Worker to process imports
@Component
public class ImportWorker {
    
    @RabbitListener(queues = "import-processing-queue")
    public void processImport(ImportMessage message) {
        ImportJob job = importJobRepository.findByJobId(message.getJobId())
            .orElseThrow();
        
        try {
            // Read Excel
            List<ProductRow> rows = excelReader.read(message.getFilePath());
            job.setTotalRows(rows.size());
            job.setStatus(JobStatus.PROCESSING);
            reportJobRepository.save(job);
            
            // Process each row
            List<ImportError> errors = new ArrayList<>();
            int successCount = 0;
            
            for (int i = 0; i < rows.size(); i++) {
                try {
                    ProductRow row = rows.get(i);
                    validateAndSaveProduct(row);
                    successCount++;
                } catch (Exception e) {
                    errors.add(new ImportError(i + 2, e.getMessage()));
                }
            }
            
            // Update job
            job.setSuccessfulRows(successCount);
            job.setFailedRows(errors.size());
            job.setStatus(errors.isEmpty() ? 
                JobStatus.COMPLETED : JobStatus.COMPLETED_WITH_ERRORS);
            job.setErrorDetails(objectMapper.writeValueAsString(errors));
            job.setCompletedAt(LocalDateTime.now());
            importJobRepository.save(job);
            
        } catch (Exception e) {
            job.setStatus(JobStatus.FAILED);
            importJobRepository.save(job);
        }
    }
}
```

### Week 12: Notifications

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }
    
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/notifications")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}

@Service
public class NotificationService {
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    public void notifyReceiptConfirmed(InboundReceipt receipt) {
        Notification notification = Notification.builder()
            .eventType("INBOUND_COMPLETED")
            .timestamp(LocalDateTime.now())
            .message("Inbound receipt #" + receipt.getReceiptNumber() + " completed")
            .severity("INFO")
            .data(Map.of(
                "receiptId", receipt.getId(),
                "receiptNumber", receipt.getReceiptNumber(),
                "warehouseId", receipt.getWarehouseId()
            ))
            .build();
        
        // Send to all users
        messagingTemplate.convertAndSend(
            "/topic/notifications",
            notification
        );
        
        // Send to specific user (created_by)
        messagingTemplate.convertAndSendToUser(
            receipt.getCreatedBy().getUsername(),
            "/queue/notifications",
            notification
        );
    }
}
```

---

## 🧪 Testing Strategy

### Unit Tests
```java
@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {
    
    @Mock
    private InventoryRepository inventoryRepository;
    
    @Mock
    private StockMovementService stockMovementService;
    
    @InjectMocks
    private InventoryServiceImpl inventoryService;
    
    @Test
    void increaseStock_Success() {
        // Given
        Inventory inventory = createTestInventory();
        when(inventoryRepository.findByProductAndWarehouse(...))
            .thenReturn(Optional.of(inventory));
        
        // When
        inventoryService.increaseStock(request);
        
        // Then
        verify(inventoryRepository).save(any());
        verify(stockMovementService).recordMovement(any());
    }
    
    @Test
    void reserveStock_InsufficientStock_ThrowsException() {
        // Given
        Inventory inventory = createInventoryWithLowStock();
        
        // When & Then
        assertThrows(InsufficientStockException.class, () -> {
            inventoryService.reserveStock(request);
        });
    }
}
```

### Integration Tests
```java
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class InboundIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void completeInboundFlow_Success() throws Exception {
        // 1. Create PO
        String poResponse = mockMvc.perform(post("/api/purchase-orders")
            .contentType(MediaType.APPLICATION_JSON)
            .content(createPORequest()))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        
        Long poId = extractId(poResponse);
        
        // 2. Confirm PO
        mockMvc.perform(put("/api/purchase-orders/" + poId + "/confirm"))
            .andExpect(status().isOk());
        
        // 3. Create Receipt
        String receiptResponse = mockMvc.perform(post("/api/inbound-receipts")
            .contentType(MediaType.APPLICATION_JSON)
            .content(createReceiptRequest(poId)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        
        Long receiptId = extractId(receiptResponse);
        
        // 4. Confirm Receipt (should increase stock)
        mockMvc.perform(put("/api/inbound-receipts/" + receiptId + "/confirm"))
            .andExpect(status().isOk());
        
        // 5. Verify inventory increased
        mockMvc.perform(get("/api/inventory?productId=1&warehouseId=1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].onHandQuantity").value(50));
    }
}
```

---

## ✅ Best Practices

### 1. Transaction Management
```java
// Always use @Transactional for operations that modify multiple tables
@Transactional
public void confirmReceipt(Long receiptId) {
    // All operations in same transaction
    updateReceipt();
    increaseInventory();
    createStockMovement();
}
```

### 2. Error Handling
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientStock(
        InsufficientStockException ex
    ) {
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(ErrorResponse.builder()
                .message(ex.getMessage())
                .details(ex.getDetails())
                .build());
    }
}
```

### 3. Optimistic Locking
```java
@Entity
public class Inventory {
    @Version
    private Long version;
}

// Handle OptimisticLockException
try {
    inventoryRepository.save(inventory);
} catch (OptimisticLockException e) {
    // Retry or inform user
}
```

### 4. Logging
```java
@Slf4j
@Service
public class InventoryServiceImpl {
    
    public void increaseStock(IncreaseStockRequest request) {
        log.info("Increasing stock: product={}, quantity={}", 
                 request.getProductId(), request.getQuantity());
        
        try {
            // Logic
            log.debug("Stock increased successfully");
        } catch (Exception e) {
            log.error("Failed to increase stock", e);
            throw e;
        }
    }
}
```

---

**Cập nhật lần cuối:** 21/01/2026  
**Version:** 1.0

