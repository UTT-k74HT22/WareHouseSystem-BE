package org.demo.whs.service.impl;

import org.demo.whs.entity.Batch;
import org.demo.whs.entity.BusinessPartners;
import org.demo.whs.entity.InboundReceiptLines;
import org.demo.whs.entity.InboundReceipts;
import org.demo.whs.entity.Inventory;
import org.demo.whs.entity.Locations;
import org.demo.whs.entity.OutboundShipmentLines;
import org.demo.whs.entity.OutboundShipments;
import org.demo.whs.entity.Products;
import org.demo.whs.entity.PurchaseOrders;
import org.demo.whs.entity.SalesOrders;
import org.demo.whs.entity.StockMovements;
import org.demo.whs.entity.Warehouses;
import org.demo.whs.entity.dto.response.Batch.BatchByProductResponse;
import org.demo.whs.entity.dto.response.Batch.BatchExpiringResponse;
import org.demo.whs.entity.dto.response.Batch.BatchFifoRecommendationResponse;
import org.demo.whs.entity.dto.response.Batch.BatchTraceabilityResponse;
import org.demo.whs.entity.enums.BatchStatus;
import org.demo.whs.entity.enums.InboundReceiptsStatus;
import org.demo.whs.entity.enums.OutboundShipmentsStatus;
import org.demo.whs.entity.enums.QualityStatus;
import org.demo.whs.entity.enums.ReferenceType;
import org.demo.whs.entity.enums.StockMovementsType;
import org.demo.whs.mapper.BatchMapper;
import org.demo.whs.repository.AccountRepository;
import org.demo.whs.repository.BatchRepository;
import org.demo.whs.repository.BusinessPartnersRepository;
import org.demo.whs.repository.InboundReceiptLinesRepository;
import org.demo.whs.repository.InboundReceiptsRepository;
import org.demo.whs.repository.InventoryRepository;
import org.demo.whs.repository.LocationRepository;
import org.demo.whs.repository.OutboundShipmentLinesRepository;
import org.demo.whs.repository.OutboundShipmentsRepository;
import org.demo.whs.repository.ProductRepository;
import org.demo.whs.repository.PurchaseOrdersRepository;
import org.demo.whs.repository.SalesOrdersRepository;
import org.demo.whs.repository.StockMovementsRepository;
import org.demo.whs.repository.WareHouseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BatchQueryServiceImplTest {

    @Mock private AccountRepository accountRepository;
    @Mock private InventoryRepository inventoryRepository;
    @Mock private ProductRepository productRepository;
    @Mock private BatchRepository batchRepository;
    @Mock private BatchMapper batchMapper;
    @Mock private InboundReceiptLinesRepository inboundReceiptLinesRepository;
    @Mock private InboundReceiptsRepository inboundReceiptsRepository;
    @Mock private OutboundShipmentLinesRepository outboundShipmentLinesRepository;
    @Mock private OutboundShipmentsRepository outboundShipmentsRepository;
    @Mock private StockMovementsRepository stockMovementsRepository;
    @Mock private PurchaseOrdersRepository purchaseOrdersRepository;
    @Mock private SalesOrdersRepository salesOrdersRepository;
    @Mock private BusinessPartnersRepository businessPartnersRepository;
    @Mock private LocationRepository locationRepository;
    @Mock private WareHouseRepository wareHouseRepository;

    @InjectMocks
    private BatchServiceImpl batchService;

    @Test
    void should_GetBatchTraceability_When_BatchHasLinkedTransactions() {
        Batch batch = batch("B1", "P1", "BATCH-001", BatchStatus.AVAILABLE, 20, 10);
        batch.setNotes("Manual note\n[QUARANTINE] reason=Quality hold");
        Products product = product("P1", "SKU-1", "Paracetamol");
        Warehouses warehouse = warehouse("W1", "WH-01", "Main Warehouse");
        Locations location = location("L1", "A-01", "Rack A-01");
        Inventory inventory = inventory("B1", "P1", "W1", "L1", "10", "1", "2");

        InboundReceiptLines inboundLine = new InboundReceiptLines();
        inboundLine.setInboundReceiptId("IR1");
        inboundLine.setLocationId("L1");
        inboundLine.setBatchId("B1");
        inboundLine.setQuantityReceived(new BigDecimal("5"));
        inboundLine.setQualityStatus(QualityStatus.PASS);
        inboundLine.setCreatedAt(LocalDateTime.now().minusDays(3));

        InboundReceipts inboundReceipt = new InboundReceipts();
        inboundReceipt.setId("IR1");
        inboundReceipt.setReceiptNumber("IR-001");
        inboundReceipt.setReceiptDate(LocalDate.now().minusDays(3));
        inboundReceipt.setStatus(InboundReceiptsStatus.CONFIRMED);
        inboundReceipt.setPurchaseOrderId("PO1");
        inboundReceipt.setWarehouseId("W1");

        PurchaseOrders purchaseOrder = new PurchaseOrders();
        purchaseOrder.setId("PO1");
        purchaseOrder.setPurchaseOrderNumber("PO-001");
        purchaseOrder.setSupplierId("SUP1");

        BusinessPartners supplier = new BusinessPartners();
        supplier.setId("SUP1");
        supplier.setCode("SUP-01");
        supplier.setName("Supplier A");

        OutboundShipmentLines outboundLine = new OutboundShipmentLines();
        outboundLine.setOutboundShipmentId("OS1");
        outboundLine.setLocationId("L1");
        outboundLine.setBatchId("B1");
        outboundLine.setQuantityShipped(new BigDecimal("3"));
        outboundLine.setPickedAt(LocalDateTime.now().minusDays(1));

        OutboundShipments outboundShipment = new OutboundShipments();
        outboundShipment.setId("OS1");
        outboundShipment.setShipmentNumber("OS-001");
        outboundShipment.setShipmentDate(LocalDate.now().minusDays(1));
        outboundShipment.setStatus(OutboundShipmentsStatus.SHIPPED);
        outboundShipment.setSalesOrderId("SO1");
        outboundShipment.setWarehouseId("W1");

        SalesOrders salesOrder = new SalesOrders();
        salesOrder.setId("SO1");
        salesOrder.setSoNumber("SO-001");
        salesOrder.setCustomerId("CUS1");

        BusinessPartners customer = new BusinessPartners();
        customer.setId("CUS1");
        customer.setCode("CUS-01");
        customer.setName("Customer A");

        StockMovements movement = new StockMovements();
        movement.setId("SM1");
        movement.setMovementType(StockMovementsType.INBOUND);
        movement.setMovementDate(LocalDateTime.now().minusDays(3));
        movement.setWarehouseId("W1");
        movement.setLocationId("L1");
        movement.setQuantityChange(new BigDecimal("5"));
        movement.setQuantityBefore(BigDecimal.ZERO);
        movement.setQuantityAfter(new BigDecimal("5"));
        movement.setReferenceType(ReferenceType.INBOUND_RECEIPT);
        movement.setReferenceId("IR1");
        movement.setReferenceNumber("IR-001");

        when(batchRepository.findById("B1")).thenReturn(Optional.of(batch));
        when(productRepository.findById("P1")).thenReturn(Optional.of(product));
        when(inventoryRepository.findByBatchIdOrderByLastMovementAtDesc("B1")).thenReturn(List.of(inventory));
        when(inboundReceiptLinesRepository.findByBatchIdOrderByCreatedAtDesc("B1")).thenReturn(List.of(inboundLine));
        when(inboundReceiptsRepository.findAllById(any())).thenReturn(List.of(inboundReceipt));
        when(purchaseOrdersRepository.findAllById(any())).thenReturn(List.of(purchaseOrder));
        when(outboundShipmentLinesRepository.findByBatchIdOrderByCreatedAtDesc("B1")).thenReturn(List.of(outboundLine));
        when(outboundShipmentsRepository.findAllById(any())).thenReturn(List.of(outboundShipment));
        when(salesOrdersRepository.findAllById(any())).thenReturn(List.of(salesOrder));
        when(stockMovementsRepository.findByBatchIdOrderByMovementDateDesc("B1")).thenReturn(List.of(movement));
        when(businessPartnersRepository.findAllById(any())).thenReturn(List.of(supplier, customer));
        when(wareHouseRepository.findAllById(any())).thenReturn(List.of(warehouse));
        when(locationRepository.findAllById(any())).thenReturn(List.of(location));

        BatchTraceabilityResponse response = batchService.getBatchTraceability("B1");

        assertThat(response.getBatchId()).isEqualTo("B1");
        assertThat(response.getProductSku()).isEqualTo("SKU-1");
        assertThat(response.getInboundReceipts()).hasSize(1);
        assertThat(response.getOutboundShipments()).hasSize(1);
        assertThat(response.getStockMovements()).hasSize(1);
        assertThat(response.getWorkflowNotes()).containsExactly("[QUARANTINE] reason=Quality hold");
        assertThat(response.getInventorySnapshot().getTotalAvailableQuantity()).isEqualByComparingTo("7");
    }

    @Test
    void should_GetExpiringBatches_When_BatchesStillHaveStock() {
        Batch expiring = batch("B1", "P1", "BATCH-001", BatchStatus.AVAILABLE, 12, 5);
        Batch noStock = batch("B2", "P1", "BATCH-002", BatchStatus.AVAILABLE, 15, 6);
        Products product = product("P1", "SKU-1", "Paracetamol");
        Warehouses warehouse = warehouse("W1", "WH-01", "Main Warehouse");
        Locations location = location("L1", "A-01", "Rack A-01");

        when(batchRepository.findExpiringBatches(any(), any(), any())).thenReturn(List.of(expiring, noStock));
        when(productRepository.findAllById(any())).thenReturn(List.of(product));
        when(inventoryRepository.findByBatchIdIn(any())).thenReturn(List.of(inventory("B1", "P1", "W1", "L1", "8", "0", "1")));
        when(wareHouseRepository.findAllById(any())).thenReturn(List.of(warehouse));
        when(locationRepository.findAllById(any())).thenReturn(List.of(location));

        List<BatchExpiringResponse> responses = batchService.getExpiringBatches(30, null);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getBatchId()).isEqualTo("B1");
        assertThat(responses.get(0).getInventorySnapshot().getTotalOnHandQuantity()).isEqualByComparingTo("8");
    }

    @Test
    void should_GetFifoRecommendations_When_OnlyEligibleBatchesRemain() {
        Products product = product("P1", "SKU-1", "Paracetamol");
        Warehouses warehouse = warehouse("W1", "WH-01", "Main Warehouse");
        Locations location = location("L1", "A-01", "Rack A-01");
        Batch oldest = batch("B1", "P1", "BATCH-001", BatchStatus.AVAILABLE, 30, 10);
        Batch newer = batch("B2", "P1", "BATCH-002", BatchStatus.AVAILABLE, 10, 20);
        Batch quarantine = batch("B3", "P1", "BATCH-003", BatchStatus.QUARANTINE, 20, 15);

        when(productRepository.findById("P1")).thenReturn(Optional.of(product));
        when(wareHouseRepository.findById("W1")).thenReturn(Optional.of(warehouse));
        when(batchRepository.findByProductIdOrderByManufacturingDateAscExpiryDateAscCreatedAtAsc("P1"))
                .thenReturn(List.of(oldest, newer, quarantine));
        when(inventoryRepository.findByProductIdAndWarehouseIdAndBatchIdIn(any(), any(), any())).thenReturn(List.of(
                inventory("B1", "P1", "W1", "L1", "5", "0", "0"),
                inventory("B2", "P1", "W1", "L1", "4", "0", "1"),
                inventory("B3", "P1", "W1", "L1", "7", "0", "0")
        ));
        when(locationRepository.findAllById(any())).thenReturn(List.of(location));

        List<BatchFifoRecommendationResponse> responses = batchService.getFifoRecommendations("P1", "W1", 5);

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getBatchId()).isEqualTo("B1");
        assertThat(responses.get(0).getRecommendationRank()).isEqualTo(1);
        assertThat(responses.get(1).getBatchId()).isEqualTo("B2");
    }

    @Test
    void should_GetBatchesByProduct_When_WarehouseFilterProvided() {
        Products product = product("P1", "SKU-1", "Paracetamol");
        Warehouses warehouse = warehouse("W1", "WH-01", "Main Warehouse");
        Locations location = location("L1", "A-01", "Rack A-01");
        Batch available = batch("B1", "P1", "BATCH-001", BatchStatus.AVAILABLE, 20, 10);
        Batch hidden = batch("B2", "P1", "BATCH-002", BatchStatus.AVAILABLE, 15, 8);

        when(productRepository.findById("P1")).thenReturn(Optional.of(product));
        when(wareHouseRepository.findById("W1")).thenReturn(Optional.of(warehouse));
        when(batchRepository.findByProductIdOrderByManufacturingDateAscExpiryDateAscCreatedAtAsc("P1"))
                .thenReturn(List.of(available, hidden));
        when(inventoryRepository.findByProductIdAndWarehouseIdAndBatchIdIn(any(), any(), any()))
                .thenReturn(List.of(inventory("B1", "P1", "W1", "L1", "6", "1", "0")));
        when(wareHouseRepository.findAllById(any())).thenReturn(List.of(warehouse));
        when(locationRepository.findAllById(any())).thenReturn(List.of(location));

        List<BatchByProductResponse> responses = batchService.getBatchesByProduct("P1", "W1");

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getBatchId()).isEqualTo("B1");
        assertThat(responses.get(0).getInventorySnapshot().getTotalAvailableQuantity()).isEqualByComparingTo("5");
    }

    private Products product(String id, String sku, String name) {
        Products product = new Products();
        product.setId(id);
        product.setSku(sku);
        product.setName(name);
        product.setRequiresBatchTracking(true);
        return product;
    }

    private Batch batch(String id, String productId, String batchNumber, BatchStatus status, long expiryInDays, long manufacturingDaysAgo) {
        Batch batch = new Batch();
        batch.setId(id);
        batch.setProductId(productId);
        batch.setBatchNumber(batchNumber);
        batch.setStatus(status);
        batch.setExpiryDate(LocalDate.now().plusDays(expiryInDays));
        batch.setManufacturingDate(LocalDate.now().minusDays(manufacturingDaysAgo));
        return batch;
    }

    private Inventory inventory(String batchId, String productId, String warehouseId, String locationId,
                                String onHand, String quarantine, String reserved) {
        Inventory inventory = new Inventory();
        inventory.setBatchId(batchId);
        inventory.setProductId(productId);
        inventory.setWarehouseId(warehouseId);
        inventory.setLocationId(locationId);
        inventory.setOnHandQuantity(new BigDecimal(onHand));
        inventory.setQuarantineQuantity(new BigDecimal(quarantine));
        inventory.setReservedQuantity(new BigDecimal(reserved));
        inventory.setLastMovementAt(LocalDateTime.now().minusHours(2));
        return inventory;
    }

    private Warehouses warehouse(String id, String code, String name) {
        Warehouses warehouse = new Warehouses();
        warehouse.setId(id);
        warehouse.setCode(code);
        warehouse.setName(name);
        return warehouse;
    }

    private Locations location(String id, String code, String name) {
        Locations location = new Locations();
        location.setId(id);
        location.setCode(code);
        location.setName(name);
        return location;
    }
}
