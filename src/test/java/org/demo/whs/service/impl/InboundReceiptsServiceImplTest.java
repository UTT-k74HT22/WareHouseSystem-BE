package org.demo.whs.service.impl;

import org.demo.whs.entity.Account;
import org.demo.whs.entity.Batch;
import org.demo.whs.entity.InboundReceiptLines;
import org.demo.whs.entity.InboundReceipts;
import org.demo.whs.entity.Inventory;
import org.demo.whs.entity.Locations;
import org.demo.whs.entity.PurchaseOrders;
import org.demo.whs.entity.PurchaseOrderLines;
import org.demo.whs.entity.Products;
import org.demo.whs.entity.StockMovements;
import org.demo.whs.entity.Warehouses;
import org.demo.whs.entity.dto.request.InboundReceipts.InboundReceiptsFilterRequest;
import org.demo.whs.entity.dto.request.InboundReceipts.InboundReceiptsRequest;
import org.demo.whs.entity.dto.response.InboundReceiptLines.InboundReceiptLinesResponse;
import org.demo.whs.entity.dto.response.InboundReceipts.InboundReceiptsResponse;
import org.demo.whs.entity.enums.BatchStatus;
import org.demo.whs.entity.enums.InboundReceiptsStatus;
import org.demo.whs.entity.enums.LocationStatus;
import org.demo.whs.entity.enums.ProductStatus;
import org.demo.whs.entity.enums.PurchaseOrdersStatus;
import org.demo.whs.entity.enums.QualityStatus;
import org.demo.whs.entity.enums.ReferenceType;
import org.demo.whs.entity.enums.StockMovementsType;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.mapper.InboundReceiptLinesMapper;
import org.demo.whs.mapper.InboundReceiptsMapper;
import org.demo.whs.mapper.StockMovementsMapper;
import org.demo.whs.repository.AccountRepository;
import org.demo.whs.repository.BatchRepository;
import org.demo.whs.repository.InboundReceiptLinesRepository;
import org.demo.whs.repository.InboundReceiptsRepository;
import org.demo.whs.repository.InventoryRepository;
import org.demo.whs.repository.LocationRepository;
import org.demo.whs.repository.ProductRepository;
import org.demo.whs.repository.PurchaseOrderLinesRepository;
import org.demo.whs.repository.PurchaseOrdersRepository;
import org.demo.whs.repository.StockMovementsRepository;
import org.demo.whs.repository.WareHouseRepository;
import org.demo.whs.security.SecurityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InboundReceiptsServiceImplTest {

    @Mock
    private InboundReceiptsRepository inboundReceiptsRepository;
    @Mock
    private InboundReceiptLinesRepository inboundReceiptLinesRepository;
    @Mock
    private PurchaseOrdersRepository purchaseOrdersRepository;
    @Mock
    private PurchaseOrderLinesRepository purchaseOrderLinesRepository;
    @Mock
    private WareHouseRepository wareHouseRepository;
    @Mock
    private LocationRepository locationRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private InventoryRepository inventoryRepository;
    @Mock
    private StockMovementsRepository stockMovementsRepository;
    @Mock
    private BatchRepository batchRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private InboundReceiptsMapper inboundReceiptsMapper;
    @Mock
    private InboundReceiptLinesMapper inboundReceiptLinesMapper;
    @Mock
    private StockMovementsMapper stockMovementsMapper;

    @InjectMocks
    private InboundReceiptsServiceImpl inboundReceiptsService;

    private MockedStatic<SecurityUtils> mockedSecurityUtils;

    @BeforeEach
    void setUp() {
        mockedSecurityUtils = mockStatic(SecurityUtils.class);
    }

    @AfterEach
    void tearDown() {
        mockedSecurityUtils.close();
    }

    @Test
    @DisplayName("should_CreateInboundReceipt_When_ValidRequest")
    void should_CreateInboundReceipt_When_ValidRequest() {
        // Given
        InboundReceiptsRequest request = new InboundReceiptsRequest();
        request.setPurchaseOrderId("po-123");

        PurchaseOrders po = PurchaseOrders.builder()
                .status(PurchaseOrdersStatus.CONFIRMED)
                .warehouseId("wh-1")
                .build();
        po.setId("po-123");

        Warehouses wh = Warehouses.builder()
                .name("Main WH")
                .build();
        wh.setId("wh-1");

        Account actor = Account.builder().username("admin").build();
        actor.setId("user-1");

        InboundReceipts entity = new InboundReceipts();
        entity.setId("receipt-1");
        entity.setReceiptNumber("GR-2026-001");

        InboundReceiptsResponse expectedResponse = InboundReceiptsResponse.builder()
                .id("receipt-1")
                .receiptNumber("GR-2026-001")
                .build();

        when(purchaseOrdersRepository.findByIdForUpdate("po-123")).thenReturn(Optional.of(po));
        when(wareHouseRepository.findById("wh-1")).thenReturn(Optional.of(wh));
        when(SecurityUtils.getCurrentUsername()).thenReturn("admin");
        when(accountRepository.findByUsername("admin")).thenReturn(Optional.of(actor));
        when(inboundReceiptsMapper.toEntity(any())).thenReturn(entity);
        when(inboundReceiptsRepository.existsByReceiptNumber(anyString())).thenReturn(false);
        when(inboundReceiptsRepository.save(any())).thenReturn(entity);
        when(inboundReceiptsMapper.toResponse(any(), eq(po), eq(wh), anyList())).thenReturn(expectedResponse);

        // When
        InboundReceiptsResponse response = inboundReceiptsService.create(request);

        // Then
        assertNotNull(response);
        assertEquals("receipt-1", response.getId());
        verify(inboundReceiptsRepository).save(any());
    }

    @Test
    @DisplayName("should_ThrowBadRequest_When_POStatusInvalid")
    void should_ThrowBadRequest_When_POStatusInvalid() {
        // Given
        InboundReceiptsRequest request = new InboundReceiptsRequest();
        request.setPurchaseOrderId("po-123");

        PurchaseOrders po = PurchaseOrders.builder()
                .status(PurchaseOrdersStatus.DRAFT) // Invalid status
                .build();
        po.setId("po-123");

        when(purchaseOrdersRepository.findByIdForUpdate("po-123")).thenReturn(Optional.of(po));

        // When & Then
        assertThrows(BadRequestException.class, () -> inboundReceiptsService.create(request));
    }

    @Test
    @DisplayName("should_GetAllInboundReceipts_When_Filtered")
    void should_GetAllInboundReceipts_When_Filtered() {
        // Given
        InboundReceiptsFilterRequest filter = new InboundReceiptsFilterRequest();
        Pageable pageable = mock(Pageable.class);
        when(pageable.getPageNumber()).thenReturn(0);
        when(pageable.getPageSize()).thenReturn(10);

        InboundReceipts receipt = new InboundReceipts();
        receipt.setPurchaseOrderId("po-1");
        receipt.setWarehouseId("wh-1");

        Page<InboundReceipts> page = new PageImpl<>(List.of(receipt));

        PurchaseOrders po = PurchaseOrders.builder().purchaseOrderNumber("PO-001").build();
        po.setId("po-1");
        Warehouses wh = Warehouses.builder().name("Main WH").build();
        wh.setId("wh-1");

        when(inboundReceiptsRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(purchaseOrdersRepository.findAllById(anySet())).thenReturn(List.of(po));
        when(wareHouseRepository.findByIdIn(anySet())).thenReturn(List.of(wh));
        when(inboundReceiptsMapper.toResponse(any(), eq(po), eq(wh), anyList())).thenReturn(new InboundReceiptsResponse());

        // When
        var response = inboundReceiptsService.getAll(filter, pageable);

        // Then
        assertNotNull(response);
        assertEquals(1, response.getContent().size());
    }

    @Test
    @DisplayName("should_ConfirmInboundReceipt_When_ValidDraftReceipt")
    void should_ConfirmInboundReceipt_When_ValidDraftReceipt() {
        InboundReceipts receipt = InboundReceipts.builder()
                .purchaseOrderId("po-1")
                .warehouseId("wh-1")
                .receiptNumber("GR-001")
                .status(InboundReceiptsStatus.DRAFT)
                .build();
        receipt.setId("receipt-1");

        InboundReceiptLines receiptLine = InboundReceiptLines.builder()
                .inboundReceiptId("receipt-1")
                .purchaseOrderLineId("pol-1")
                .productId("prod-1")
                .batchId("batch-1")
                .locationId("loc-1")
                .lineNumber(1)
                .quantityReceived(new BigDecimal("10.00"))
                .qualityStatus(QualityStatus.PASS)
                .notes("received")
                .build();
        receiptLine.setId("line-1");

        PurchaseOrders purchaseOrder = PurchaseOrders.builder()
                .purchaseOrderNumber("PO-001")
                .warehouseId("wh-1")
                .status(PurchaseOrdersStatus.CONFIRMED)
                .build();
        purchaseOrder.setId("po-1");

        PurchaseOrderLines poLine = PurchaseOrderLines.builder()
                .purchaseOrderId("po-1")
                .productId("prod-1")
                .quantityOrdered(new BigDecimal("30.00"))
                .quantityReceived(new BigDecimal("20.00"))
                .build();
        poLine.setId("pol-1");

        Products product = Products.builder()
                .status(ProductStatus.ACTIVE)
                .requiresBatchTracking(true)
                .build();
        product.setId("prod-1");

        Batch batch = Batch.builder()
                .productId("prod-1")
                .status(BatchStatus.AVAILABLE)
                .build();
        batch.setId("batch-1");

        Locations location = Locations.builder()
                .warehouseId("wh-1")
                .status(LocationStatus.ACTIVE)
                .build();
        location.setId("loc-1");

        Inventory inventory = Inventory.builder()
                .productId("prod-1")
                .warehouseId("wh-1")
                .locationId("loc-1")
                .batchId("batch-1")
                .onHandQuantity(new BigDecimal("5.00"))
                .reservedQuantity(BigDecimal.ZERO)
                .version(0)
                .build();
        inventory.setId("inv-1");

        Account actor = Account.builder().username("admin").build();
        actor.setId("user-1");

        Warehouses warehouse = Warehouses.builder().name("Main WH").build();
        warehouse.setId("wh-1");

        List<InboundReceiptLines> receiptLines = List.of(receiptLine);
        List<InboundReceiptLinesResponse> lineResponses = List.of(new InboundReceiptLinesResponse());
        InboundReceiptsResponse expectedResponse = InboundReceiptsResponse.builder()
                .id("receipt-1")
                .status(InboundReceiptsStatus.CONFIRMED.name())
                .build();

        mockedSecurityUtils.when(SecurityUtils::getCurrentUsername).thenReturn("admin");
        when(accountRepository.findByUsername("admin")).thenReturn(Optional.of(actor));
        when(inboundReceiptsRepository.findByIdForUpdate("receipt-1")).thenReturn(Optional.of(receipt));
        when(inboundReceiptLinesRepository.findByInboundReceiptIdOrderByLineNumberAsc("receipt-1")).thenReturn(receiptLines);
        when(purchaseOrdersRepository.findByIdForUpdate("po-1")).thenReturn(Optional.of(purchaseOrder));
        when(purchaseOrderLinesRepository.findByPurchaseOrderIdForUpdate("po-1")).thenReturn(List.of(poLine));
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(product));
        when(locationRepository.findById("loc-1")).thenReturn(Optional.of(location));
        when(batchRepository.findById("batch-1")).thenReturn(Optional.of(batch));
        when(inventoryRepository.findByDimensionForUpdate("prod-1", "wh-1", "loc-1", "batch-1"))
                .thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(stockMovementsMapper.toEntity(
                eq(StockMovementsType.INBOUND),
                eq("prod-1"),
                eq("wh-1"),
                eq("loc-1"),
                eq("batch-1"),
                eq(new BigDecimal("10.00")),
                eq(new BigDecimal("5.00")),
                eq(new BigDecimal("15.00")),
                eq(ReferenceType.INBOUND_RECEIPT),
                eq("receipt-1"),
                eq("GR-001"),
                eq("received"),
                eq("user-1")
        )).thenReturn(new StockMovements());
        when(purchaseOrderLinesRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(purchaseOrdersRepository.save(any(PurchaseOrders.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(inboundReceiptsRepository.save(any(InboundReceipts.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(wareHouseRepository.findById("wh-1")).thenReturn(Optional.of(warehouse));
        when(inboundReceiptLinesMapper.toResponses(receiptLines)).thenReturn(lineResponses);
        when(inboundReceiptsMapper.toResponse(any(InboundReceipts.class), eq(purchaseOrder), eq(warehouse), eq(lineResponses)))
                .thenReturn(expectedResponse);

        InboundReceiptsResponse response = inboundReceiptsService.confirm("receipt-1");

        assertNotNull(response);
        assertEquals(InboundReceiptsStatus.CONFIRMED, receipt.getStatus());
        assertEquals("user-1", receipt.getConfirmedBy());
        assertEquals(PurchaseOrdersStatus.COMPLETED, purchaseOrder.getStatus());
        assertEquals(new BigDecimal("30.00"), poLine.getQuantityReceived());
        assertEquals(new BigDecimal("15.00"), inventory.getOnHandQuantity());
        verify(stockMovementsRepository).save(any(StockMovements.class));
    }

    @Test
    @DisplayName("should_ThrowBadRequest_When_ReceiptQuantityExceedsRemaining")
    void should_ThrowBadRequest_When_ReceiptQuantityExceedsRemaining() {
        InboundReceipts receipt = InboundReceipts.builder()
                .purchaseOrderId("po-1")
                .warehouseId("wh-1")
                .status(InboundReceiptsStatus.DRAFT)
                .build();
        receipt.setId("receipt-1");

        InboundReceiptLines receiptLine = InboundReceiptLines.builder()
                .purchaseOrderLineId("pol-1")
                .productId("prod-1")
                .locationId("loc-1")
                .quantityReceived(new BigDecimal("10.00"))
                .qualityStatus(QualityStatus.PASS)
                .build();
        receiptLine.setId("line-1");

        PurchaseOrders purchaseOrder = PurchaseOrders.builder()
                .status(PurchaseOrdersStatus.CONFIRMED)
                .warehouseId("wh-1")
                .build();
        purchaseOrder.setId("po-1");

        PurchaseOrderLines poLine = PurchaseOrderLines.builder()
                .purchaseOrderId("po-1")
                .productId("prod-1")
                .quantityOrdered(new BigDecimal("30.00"))
                .quantityReceived(new BigDecimal("25.00"))
                .build();
        poLine.setId("pol-1");

        Products product = Products.builder()
                .status(ProductStatus.ACTIVE)
                .requiresBatchTracking(false)
                .build();
        product.setId("prod-1");

        Locations location = Locations.builder()
                .warehouseId("wh-1")
                .status(LocationStatus.ACTIVE)
                .build();
        location.setId("loc-1");

        mockedSecurityUtils.when(SecurityUtils::getCurrentUsername).thenReturn("admin");
        when(accountRepository.findByUsername("admin")).thenReturn(Optional.of(account("user-1", "admin")));
        when(inboundReceiptsRepository.findByIdForUpdate("receipt-1")).thenReturn(Optional.of(receipt));
        when(inboundReceiptLinesRepository.findByInboundReceiptIdOrderByLineNumberAsc("receipt-1"))
                .thenReturn(List.of(receiptLine));
        when(purchaseOrdersRepository.findByIdForUpdate("po-1")).thenReturn(Optional.of(purchaseOrder));
        when(purchaseOrderLinesRepository.findByPurchaseOrderIdForUpdate("po-1")).thenReturn(List.of(poLine));
        lenient().when(productRepository.findById("prod-1")).thenReturn(Optional.of(product));
        lenient().when(locationRepository.findById("loc-1")).thenReturn(Optional.of(location));

        assertThrows(BadRequestException.class, () -> inboundReceiptsService.confirm("receipt-1"));
        verifyNoInteractions(inventoryRepository, stockMovementsRepository);
    }

    @Test
    @DisplayName("should_ThrowBadRequest_When_QuarantineLineMissingNotes")
    void should_ThrowBadRequest_When_QuarantineLineMissingNotes() {
        InboundReceipts receipt = InboundReceipts.builder()
                .purchaseOrderId("po-1")
                .warehouseId("wh-1")
                .status(InboundReceiptsStatus.DRAFT)
                .build();
        receipt.setId("receipt-1");

        InboundReceiptLines receiptLine = InboundReceiptLines.builder()
                .purchaseOrderLineId("pol-1")
                .productId("prod-1")
                .locationId("loc-1")
                .quantityReceived(new BigDecimal("5.00"))
                .qualityStatus(QualityStatus.QUARANTINE)
                .notes(" ")
                .build();
        receiptLine.setId("line-1");

        PurchaseOrders purchaseOrder = PurchaseOrders.builder()
                .status(PurchaseOrdersStatus.CONFIRMED)
                .warehouseId("wh-1")
                .build();
        purchaseOrder.setId("po-1");

        PurchaseOrderLines poLine = PurchaseOrderLines.builder()
                .purchaseOrderId("po-1")
                .productId("prod-1")
                .quantityOrdered(new BigDecimal("30.00"))
                .quantityReceived(BigDecimal.ZERO)
                .build();
        poLine.setId("pol-1");

        Products product = Products.builder()
                .status(ProductStatus.ACTIVE)
                .requiresBatchTracking(false)
                .build();
        product.setId("prod-1");

        Locations location = Locations.builder()
                .warehouseId("wh-1")
                .status(LocationStatus.ACTIVE)
                .build();
        location.setId("loc-1");

        mockedSecurityUtils.when(SecurityUtils::getCurrentUsername).thenReturn("admin");
        when(accountRepository.findByUsername("admin")).thenReturn(Optional.of(account("user-1", "admin")));
        lenient().when(inboundReceiptsRepository.findByIdForUpdate("receipt-1")).thenReturn(Optional.of(receipt));
        lenient().when(inboundReceiptLinesRepository.findByInboundReceiptIdOrderByLineNumberAsc("receipt-1"))
                .thenReturn(List.of(receiptLine));
        lenient().when(purchaseOrdersRepository.findByIdForUpdate("po-1")).thenReturn(Optional.of(purchaseOrder));
        lenient().when(purchaseOrderLinesRepository.findByPurchaseOrderIdForUpdate("po-1")).thenReturn(List.of(poLine));
        lenient().when(productRepository.findById("prod-1")).thenReturn(Optional.of(product));
        lenient().when(locationRepository.findById("loc-1")).thenReturn(Optional.of(location));

        assertThrows(BadRequestException.class, () -> inboundReceiptsService.confirm("receipt-1"));
        verifyNoInteractions(inventoryRepository, stockMovementsRepository);
    }

    private Account account(String id, String username) {
        Account account = Account.builder().username(username).build();
        account.setId(id);
        return account;
    }
}
