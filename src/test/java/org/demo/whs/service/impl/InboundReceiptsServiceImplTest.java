package org.demo.whs.service.impl;

import org.demo.whs.entity.Account;
import org.demo.whs.entity.Batch;
import org.demo.whs.entity.InboundReceiptLines;
import org.demo.whs.entity.InboundReceipts;
import org.demo.whs.entity.Inventory;
import org.demo.whs.entity.Locations;
import org.demo.whs.entity.PurchaseOrderLines;
import org.demo.whs.entity.PurchaseOrders;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
        when(inboundReceiptsRepository.existsByReceiptNumber(any())).thenReturn(false);
        when(inboundReceiptsRepository.save(any())).thenReturn(entity);
        when(inboundReceiptsMapper.toResponse(any(), eq(po), eq(wh), anyList())).thenReturn(expectedResponse);

        InboundReceiptsResponse response = inboundReceiptsService.create(request);

        assertNotNull(response);
        assertEquals("receipt-1", response.getId());
        verify(inboundReceiptsRepository).save(any());
    }

    @Test
    @DisplayName("should_ThrowBadRequest_When_POStatusInvalid")
    void should_ThrowBadRequest_When_POStatusInvalid() {
        InboundReceiptsRequest request = new InboundReceiptsRequest();
        request.setPurchaseOrderId("po-123");

        PurchaseOrders po = PurchaseOrders.builder()
                .status(PurchaseOrdersStatus.DRAFT)
                .build();
        po.setId("po-123");

        when(purchaseOrdersRepository.findByIdForUpdate("po-123")).thenReturn(Optional.of(po));

        assertThrows(BadRequestException.class, () -> inboundReceiptsService.create(request));
    }

    @Test
    @DisplayName("should_GetAllInboundReceipts_When_Filtered")
    void should_GetAllInboundReceipts_When_Filtered() {
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

        var response = inboundReceiptsService.getAll(filter, pageable);

        assertNotNull(response);
        assertEquals(1, response.getContent().size());
    }

    @Test
    @DisplayName("should_ConfirmInboundReceipt_When_ValidDraftReceipt")
    void should_ConfirmInboundReceipt_When_ValidDraftReceipt() {
        InboundReceipts receipt = draftReceipt();
        InboundReceiptLines receiptLine = receiptLine(QualityStatus.PASS, new BigDecimal("10.00"), "batch-1", "received");
        PurchaseOrders purchaseOrder = purchaseOrder(PurchaseOrdersStatus.CONFIRMED, "wh-1");
        PurchaseOrderLines poLine = purchaseOrderLine(new BigDecimal("30.00"), new BigDecimal("20.00"));
        Products product = product(true, ProductStatus.ACTIVE);
        Batch batch = batch(BatchStatus.AVAILABLE);
        Locations location = location("wh-1", LocationStatus.ACTIVE);
        Inventory inventory = inventory("batch-1", new BigDecimal("5.00"), BigDecimal.ZERO, BigDecimal.ZERO);
        Warehouses warehouse = warehouse("wh-1");
        List<InboundReceiptLines> receiptLines = List.of(receiptLine);
        List<InboundReceiptLinesResponse> lineResponses = List.of(new InboundReceiptLinesResponse());
        InboundReceiptsResponse expectedResponse = InboundReceiptsResponse.builder()
                .id("receipt-1")
                .status(InboundReceiptsStatus.CONFIRMED.name())
                .build();

        mockActor();
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
        assertEquals(BigDecimal.ZERO, inventory.getQuarantineQuantity());
        verify(batchRepository, never()).save(any(Batch.class));
        verify(stockMovementsRepository).save(any(StockMovements.class));
    }

    @Test
    @DisplayName("should_ConfirmInboundReceipt_When_QuarantineLine_ShouldIncreaseOnHandAndQuarantineAndMarkBatchQuarantine")
    void should_ConfirmInboundReceipt_When_QuarantineLine_ShouldIncreaseOnHandAndQuarantineAndMarkBatchQuarantine() {
        InboundReceipts receipt = draftReceipt();
        InboundReceiptLines receiptLine = receiptLine(QualityStatus.QUARANTINE, new BigDecimal("5.00"), "batch-1", "damaged box");
        PurchaseOrders purchaseOrder = purchaseOrder(PurchaseOrdersStatus.CONFIRMED, "wh-1");
        PurchaseOrderLines poLine = purchaseOrderLine(new BigDecimal("10.00"), BigDecimal.ZERO);
        Products product = product(true, ProductStatus.ACTIVE);
        Batch batch = batch(BatchStatus.AVAILABLE);
        Locations location = location("wh-1", LocationStatus.ACTIVE);
        Inventory inventory = inventory("batch-1", new BigDecimal("10.00"), new BigDecimal("2.00"), BigDecimal.ZERO);
        Warehouses warehouse = warehouse("wh-1");
        List<InboundReceiptLines> receiptLines = List.of(receiptLine);
        List<InboundReceiptLinesResponse> lineResponses = List.of(new InboundReceiptLinesResponse());

        mockActor();
        when(inboundReceiptsRepository.findByIdForUpdate("receipt-1")).thenReturn(Optional.of(receipt));
        when(inboundReceiptLinesRepository.findByInboundReceiptIdOrderByLineNumberAsc("receipt-1")).thenReturn(receiptLines);
        when(purchaseOrdersRepository.findByIdForUpdate("po-1")).thenReturn(Optional.of(purchaseOrder));
        when(purchaseOrderLinesRepository.findByPurchaseOrderIdForUpdate("po-1")).thenReturn(List.of(poLine));
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(product));
        when(locationRepository.findById("loc-1")).thenReturn(Optional.of(location));
        when(batchRepository.findById("batch-1")).thenReturn(Optional.of(batch));
        when(batchRepository.save(any(Batch.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(inventoryRepository.findByDimensionForUpdate("prod-1", "wh-1", "loc-1", "batch-1"))
                .thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(stockMovementsMapper.toEntity(
                eq(StockMovementsType.INBOUND),
                eq("prod-1"),
                eq("wh-1"),
                eq("loc-1"),
                eq("batch-1"),
                eq(new BigDecimal("5.00")),
                eq(new BigDecimal("10.00")),
                eq(new BigDecimal("15.00")),
                eq(ReferenceType.INBOUND_RECEIPT),
                eq("receipt-1"),
                eq("GR-001"),
                eq("damaged box"),
                eq("user-1")
        )).thenReturn(new StockMovements());
        when(purchaseOrderLinesRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(purchaseOrdersRepository.save(any(PurchaseOrders.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(inboundReceiptsRepository.save(any(InboundReceipts.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(wareHouseRepository.findById("wh-1")).thenReturn(Optional.of(warehouse));
        when(inboundReceiptLinesMapper.toResponses(receiptLines)).thenReturn(lineResponses);
        when(inboundReceiptsMapper.toResponse(any(InboundReceipts.class), eq(purchaseOrder), eq(warehouse), eq(lineResponses)))
                .thenReturn(InboundReceiptsResponse.builder().id("receipt-1").status(InboundReceiptsStatus.CONFIRMED.name()).build());

        InboundReceiptsResponse response = inboundReceiptsService.confirm("receipt-1");

        assertNotNull(response);
        assertEquals(InboundReceiptsStatus.CONFIRMED, receipt.getStatus());
        assertEquals(PurchaseOrdersStatus.PARTIALLY_RECEIVED, purchaseOrder.getStatus());
        assertEquals(new BigDecimal("5.00"), poLine.getQuantityReceived());
        assertEquals(new BigDecimal("15.00"), inventory.getOnHandQuantity());
        assertEquals(new BigDecimal("7.00"), inventory.getQuarantineQuantity());
        assertEquals(BatchStatus.QUARANTINE, batch.getStatus());
        verify(batchRepository).save(batch);
        verify(stockMovementsRepository).save(any(StockMovements.class));
    }

    @Test
    @DisplayName("should_ConfirmInboundReceipt_When_QuarantineBatchAlreadyQuarantined")
    void should_ConfirmInboundReceipt_When_QuarantineBatchAlreadyQuarantined() {
        InboundReceipts receipt = draftReceipt();
        InboundReceiptLines receiptLine = receiptLine(QualityStatus.QUARANTINE, new BigDecimal("3.00"), "batch-1", "quality hold");
        PurchaseOrders purchaseOrder = purchaseOrder(PurchaseOrdersStatus.CONFIRMED, "wh-1");
        PurchaseOrderLines poLine = purchaseOrderLine(new BigDecimal("3.00"), BigDecimal.ZERO);
        Products product = product(true, ProductStatus.ACTIVE);
        Batch batch = batch(BatchStatus.QUARANTINE);
        Locations location = location("wh-1", LocationStatus.ACTIVE);
        Inventory inventory = inventory("batch-1", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        Warehouses warehouse = warehouse("wh-1");
        List<InboundReceiptLines> receiptLines = List.of(receiptLine);
        List<InboundReceiptLinesResponse> lineResponses = List.of(new InboundReceiptLinesResponse());

        mockActor();
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
        when(stockMovementsMapper.toEntity(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new StockMovements());
        when(purchaseOrderLinesRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(purchaseOrdersRepository.save(any(PurchaseOrders.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(inboundReceiptsRepository.save(any(InboundReceipts.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(wareHouseRepository.findById("wh-1")).thenReturn(Optional.of(warehouse));
        when(inboundReceiptLinesMapper.toResponses(receiptLines)).thenReturn(lineResponses);
        when(inboundReceiptsMapper.toResponse(any(InboundReceipts.class), eq(purchaseOrder), eq(warehouse), eq(lineResponses)))
                .thenReturn(InboundReceiptsResponse.builder().id("receipt-1").status(InboundReceiptsStatus.CONFIRMED.name()).build());

        InboundReceiptsResponse response = inboundReceiptsService.confirm("receipt-1");

        assertNotNull(response);
        assertEquals(new BigDecimal("3.00"), inventory.getOnHandQuantity());
        assertEquals(new BigDecimal("3.00"), inventory.getQuarantineQuantity());
        verify(batchRepository, never()).save(any(Batch.class));
    }

    @Test
    @DisplayName("should_ThrowBadRequest_When_ReceiptQuantityExceedsRemaining")
    void should_ThrowBadRequest_When_ReceiptQuantityExceedsRemaining() {
        InboundReceipts receipt = draftReceipt();
        InboundReceiptLines receiptLine = receiptLine(QualityStatus.PASS, new BigDecimal("10.00"), null, null);
        PurchaseOrders purchaseOrder = purchaseOrder(PurchaseOrdersStatus.CONFIRMED, "wh-1");
        PurchaseOrderLines poLine = purchaseOrderLine(new BigDecimal("30.00"), new BigDecimal("25.00"));
        Products product = product(false, ProductStatus.ACTIVE);
        Locations location = location("wh-1", LocationStatus.ACTIVE);

        mockActor();
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
        InboundReceipts receipt = draftReceipt();
        InboundReceiptLines receiptLine = receiptLine(QualityStatus.QUARANTINE, new BigDecimal("5.00"), null, " ");
        PurchaseOrders purchaseOrder = purchaseOrder(PurchaseOrdersStatus.CONFIRMED, "wh-1");
        PurchaseOrderLines poLine = purchaseOrderLine(new BigDecimal("30.00"), BigDecimal.ZERO);
        Products product = product(false, ProductStatus.ACTIVE);
        Locations location = location("wh-1", LocationStatus.ACTIVE);

        mockActor();
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
    @DisplayName("should_ThrowBadRequest_When_ReceiptHasNoLines")
    void should_ThrowBadRequest_When_ReceiptHasNoLines() {
        mockActor();
        when(inboundReceiptsRepository.findByIdForUpdate("receipt-1")).thenReturn(Optional.of(draftReceipt()));
        when(inboundReceiptLinesRepository.findByInboundReceiptIdOrderByLineNumberAsc("receipt-1"))
                .thenReturn(Collections.emptyList());

        assertThrows(BadRequestException.class, () -> inboundReceiptsService.confirm("receipt-1"));
        verifyNoInteractions(purchaseOrdersRepository, inventoryRepository, stockMovementsRepository);
    }

    @Test
    @DisplayName("should_ThrowBadRequest_When_ReceiptAlreadyConfirmed")
    void should_ThrowBadRequest_When_ReceiptAlreadyConfirmed() {
        InboundReceipts receipt = draftReceipt();
        receipt.setStatus(InboundReceiptsStatus.CONFIRMED);

        mockActor();
        when(inboundReceiptsRepository.findByIdForUpdate("receipt-1")).thenReturn(Optional.of(receipt));

        assertThrows(BadRequestException.class, () -> inboundReceiptsService.confirm("receipt-1"));
        verifyNoInteractions(inboundReceiptLinesRepository, purchaseOrdersRepository, inventoryRepository);
    }

    @Test
    @DisplayName("should_ThrowBadRequest_When_PurchaseOrderWarehouseDiffersFromReceiptWarehouse")
    void should_ThrowBadRequest_When_PurchaseOrderWarehouseDiffersFromReceiptWarehouse() {
        InboundReceipts receipt = draftReceipt();
        InboundReceiptLines receiptLine = receiptLine(QualityStatus.PASS, new BigDecimal("5.00"), null, null);
        PurchaseOrders purchaseOrder = purchaseOrder(PurchaseOrdersStatus.CONFIRMED, "wh-2");

        mockActor();
        when(inboundReceiptsRepository.findByIdForUpdate("receipt-1")).thenReturn(Optional.of(receipt));
        when(inboundReceiptLinesRepository.findByInboundReceiptIdOrderByLineNumberAsc("receipt-1"))
                .thenReturn(List.of(receiptLine));
        when(purchaseOrdersRepository.findByIdForUpdate("po-1")).thenReturn(Optional.of(purchaseOrder));

        assertThrows(BadRequestException.class, () -> inboundReceiptsService.confirm("receipt-1"));
        verifyNoInteractions(purchaseOrderLinesRepository, inventoryRepository, stockMovementsRepository);
    }

    @Test
    @DisplayName("should_ThrowBadRequest_When_BatchTrackedProductMissingBatch")
    void should_ThrowBadRequest_When_BatchTrackedProductMissingBatch() {
        InboundReceipts receipt = draftReceipt();
        InboundReceiptLines receiptLine = receiptLine(QualityStatus.PASS, new BigDecimal("5.00"), null, "received");
        PurchaseOrders purchaseOrder = purchaseOrder(PurchaseOrdersStatus.CONFIRMED, "wh-1");
        PurchaseOrderLines poLine = purchaseOrderLine(new BigDecimal("30.00"), BigDecimal.ZERO);
        Products product = product(true, ProductStatus.ACTIVE);
        Locations location = location("wh-1", LocationStatus.ACTIVE);

        mockActor();
        when(inboundReceiptsRepository.findByIdForUpdate("receipt-1")).thenReturn(Optional.of(receipt));
        when(inboundReceiptLinesRepository.findByInboundReceiptIdOrderByLineNumberAsc("receipt-1"))
                .thenReturn(List.of(receiptLine));
        when(purchaseOrdersRepository.findByIdForUpdate("po-1")).thenReturn(Optional.of(purchaseOrder));
        when(purchaseOrderLinesRepository.findByPurchaseOrderIdForUpdate("po-1")).thenReturn(List.of(poLine));
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(product));
        when(locationRepository.findById("loc-1")).thenReturn(Optional.of(location));

        assertThrows(BadRequestException.class, () -> inboundReceiptsService.confirm("receipt-1"));
        verify(batchRepository, never()).findById(any());
        verifyNoInteractions(inventoryRepository, stockMovementsRepository);
    }

    @Test
    @DisplayName("should_ThrowBadRequest_When_PassLineUsesNonAvailableBatch")
    void should_ThrowBadRequest_When_PassLineUsesNonAvailableBatch() {
        InboundReceipts receipt = draftReceipt();
        InboundReceiptLines receiptLine = receiptLine(QualityStatus.PASS, new BigDecimal("5.00"), "batch-1", "received");
        PurchaseOrders purchaseOrder = purchaseOrder(PurchaseOrdersStatus.CONFIRMED, "wh-1");
        PurchaseOrderLines poLine = purchaseOrderLine(new BigDecimal("30.00"), BigDecimal.ZERO);
        Products product = product(true, ProductStatus.ACTIVE);
        Batch batch = batch(BatchStatus.QUARANTINE);
        Locations location = location("wh-1", LocationStatus.ACTIVE);

        mockActor();
        when(inboundReceiptsRepository.findByIdForUpdate("receipt-1")).thenReturn(Optional.of(receipt));
        when(inboundReceiptLinesRepository.findByInboundReceiptIdOrderByLineNumberAsc("receipt-1"))
                .thenReturn(List.of(receiptLine));
        when(purchaseOrdersRepository.findByIdForUpdate("po-1")).thenReturn(Optional.of(purchaseOrder));
        when(purchaseOrderLinesRepository.findByPurchaseOrderIdForUpdate("po-1")).thenReturn(List.of(poLine));
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(product));
        when(locationRepository.findById("loc-1")).thenReturn(Optional.of(location));
        when(batchRepository.findById("batch-1")).thenReturn(Optional.of(batch));

        assertThrows(BadRequestException.class, () -> inboundReceiptsService.confirm("receipt-1"));
        verifyNoInteractions(inventoryRepository, stockMovementsRepository);
    }

    @Test
    @DisplayName("should_ThrowBadRequest_When_LocationOutsideReceiptWarehouse")
    void should_ThrowBadRequest_When_LocationOutsideReceiptWarehouse() {
        InboundReceipts receipt = draftReceipt();
        InboundReceiptLines receiptLine = receiptLine(QualityStatus.PASS, new BigDecimal("5.00"), null, null);
        PurchaseOrders purchaseOrder = purchaseOrder(PurchaseOrdersStatus.CONFIRMED, "wh-1");
        PurchaseOrderLines poLine = purchaseOrderLine(new BigDecimal("30.00"), BigDecimal.ZERO);
        Products product = product(false, ProductStatus.ACTIVE);
        Locations location = location("wh-2", LocationStatus.ACTIVE);

        mockActor();
        when(inboundReceiptsRepository.findByIdForUpdate("receipt-1")).thenReturn(Optional.of(receipt));
        when(inboundReceiptLinesRepository.findByInboundReceiptIdOrderByLineNumberAsc("receipt-1"))
                .thenReturn(List.of(receiptLine));
        when(purchaseOrdersRepository.findByIdForUpdate("po-1")).thenReturn(Optional.of(purchaseOrder));
        when(purchaseOrderLinesRepository.findByPurchaseOrderIdForUpdate("po-1")).thenReturn(List.of(poLine));
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(product));
        when(locationRepository.findById("loc-1")).thenReturn(Optional.of(location));

        assertThrows(BadRequestException.class, () -> inboundReceiptsService.confirm("receipt-1"));
        verifyNoInteractions(inventoryRepository, stockMovementsRepository);
    }

    @Test
    @DisplayName("should_ThrowBadRequest_When_ProductInactive")
    void should_ThrowBadRequest_When_ProductInactive() {
        InboundReceipts receipt = draftReceipt();
        InboundReceiptLines receiptLine = receiptLine(QualityStatus.PASS, new BigDecimal("5.00"), null, null);
        PurchaseOrders purchaseOrder = purchaseOrder(PurchaseOrdersStatus.CONFIRMED, "wh-1");
        PurchaseOrderLines poLine = purchaseOrderLine(new BigDecimal("30.00"), BigDecimal.ZERO);
        Products product = product(false, ProductStatus.INACTIVE);

        mockActor();
        when(inboundReceiptsRepository.findByIdForUpdate("receipt-1")).thenReturn(Optional.of(receipt));
        when(inboundReceiptLinesRepository.findByInboundReceiptIdOrderByLineNumberAsc("receipt-1"))
                .thenReturn(List.of(receiptLine));
        when(purchaseOrdersRepository.findByIdForUpdate("po-1")).thenReturn(Optional.of(purchaseOrder));
        when(purchaseOrderLinesRepository.findByPurchaseOrderIdForUpdate("po-1")).thenReturn(List.of(poLine));
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(product));

        assertThrows(BadRequestException.class, () -> inboundReceiptsService.confirm("receipt-1"));
        verifyNoInteractions(inventoryRepository, stockMovementsRepository);
    }

    @Test
    @DisplayName("should_ThrowBadRequest_When_QuarantineLineUsesExpiredBatch")
    void should_ThrowBadRequest_When_QuarantineLineUsesExpiredBatch() {
        InboundReceipts receipt = draftReceipt();
        InboundReceiptLines receiptLine = receiptLine(QualityStatus.QUARANTINE, new BigDecimal("5.00"), "batch-1", "quality issue");
        PurchaseOrders purchaseOrder = purchaseOrder(PurchaseOrdersStatus.CONFIRMED, "wh-1");
        PurchaseOrderLines poLine = purchaseOrderLine(new BigDecimal("30.00"), BigDecimal.ZERO);
        Products product = product(true, ProductStatus.ACTIVE);
        Batch batch = batch(BatchStatus.EXPIRED);
        Locations location = location("wh-1", LocationStatus.ACTIVE);

        mockActor();
        when(inboundReceiptsRepository.findByIdForUpdate("receipt-1")).thenReturn(Optional.of(receipt));
        when(inboundReceiptLinesRepository.findByInboundReceiptIdOrderByLineNumberAsc("receipt-1"))
                .thenReturn(List.of(receiptLine));
        when(purchaseOrdersRepository.findByIdForUpdate("po-1")).thenReturn(Optional.of(purchaseOrder));
        when(purchaseOrderLinesRepository.findByPurchaseOrderIdForUpdate("po-1")).thenReturn(List.of(poLine));
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(product));
        when(locationRepository.findById("loc-1")).thenReturn(Optional.of(location));
        when(batchRepository.findById("batch-1")).thenReturn(Optional.of(batch));

        assertThrows(BadRequestException.class, () -> inboundReceiptsService.confirm("receipt-1"));
        verify(inventoryRepository, never()).save(any(Inventory.class));
    }

    private void mockActor() {
        mockedSecurityUtils.when(SecurityUtils::getCurrentUsername).thenReturn("admin");
        when(accountRepository.findByUsername("admin")).thenReturn(Optional.of(account("user-1", "admin")));
    }

    private InboundReceipts draftReceipt() {
        InboundReceipts receipt = InboundReceipts.builder()
                .purchaseOrderId("po-1")
                .warehouseId("wh-1")
                .receiptNumber("GR-001")
                .status(InboundReceiptsStatus.DRAFT)
                .build();
        receipt.setId("receipt-1");
        return receipt;
    }

    private InboundReceiptLines receiptLine(QualityStatus qualityStatus, BigDecimal quantity, String batchId, String notes) {
        InboundReceiptLines receiptLine = InboundReceiptLines.builder()
                .inboundReceiptId("receipt-1")
                .purchaseOrderLineId("pol-1")
                .productId("prod-1")
                .batchId(batchId)
                .locationId("loc-1")
                .lineNumber(1)
                .quantityReceived(quantity)
                .qualityStatus(qualityStatus)
                .notes(notes)
                .build();
        receiptLine.setId("line-1");
        return receiptLine;
    }

    private PurchaseOrders purchaseOrder(PurchaseOrdersStatus status, String warehouseId) {
        PurchaseOrders purchaseOrder = PurchaseOrders.builder()
                .purchaseOrderNumber("PO-001")
                .warehouseId(warehouseId)
                .status(status)
                .build();
        purchaseOrder.setId("po-1");
        return purchaseOrder;
    }

    private PurchaseOrderLines purchaseOrderLine(BigDecimal quantityOrdered, BigDecimal quantityReceived) {
        PurchaseOrderLines poLine = PurchaseOrderLines.builder()
                .purchaseOrderId("po-1")
                .productId("prod-1")
                .quantityOrdered(quantityOrdered)
                .quantityReceived(quantityReceived)
                .build();
        poLine.setId("pol-1");
        return poLine;
    }

    private Products product(boolean requiresBatchTracking, ProductStatus status) {
        Products product = Products.builder()
                .status(status)
                .requiresBatchTracking(requiresBatchTracking)
                .build();
        product.setId("prod-1");
        return product;
    }

    private Batch batch(BatchStatus status) {
        Batch batch = Batch.builder()
                .productId("prod-1")
                .batchNumber("BATCH-001")
                .status(status)
                .build();
        batch.setId("batch-1");
        return batch;
    }

    private Locations location(String warehouseId, LocationStatus status) {
        Locations location = Locations.builder()
                .warehouseId(warehouseId)
                .status(status)
                .build();
        location.setId("loc-1");
        return location;
    }

    private Inventory inventory(String batchId, BigDecimal onHand, BigDecimal quarantine, BigDecimal reserved) {
        Inventory inventory = Inventory.builder()
                .productId("prod-1")
                .warehouseId("wh-1")
                .locationId("loc-1")
                .batchId(batchId)
                .onHandQuantity(onHand)
                .quarantineQuantity(quarantine)
                .reservedQuantity(reserved)
                .version(0)
                .build();
        inventory.setId("inv-1");
        return inventory;
    }

    private Warehouses warehouse(String id) {
        Warehouses warehouse = Warehouses.builder().name("Main WH").build();
        warehouse.setId(id);
        return warehouse;
    }

    private Account account(String id, String username) {
        Account account = Account.builder().username(username).build();
        account.setId(id);
        return account;
    }
}
