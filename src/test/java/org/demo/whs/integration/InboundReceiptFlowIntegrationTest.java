package org.demo.whs.integration;

import com.jayway.jsonpath.JsonPath;
import org.demo.whs.entity.*;
import org.demo.whs.entity.dto.request.InboundReceiptLines.InboundReceiptLinesRequest;
import org.demo.whs.entity.dto.request.InboundReceipts.InboundReceiptsRequest;
import org.demo.whs.entity.dto.request.PurchaseOrderLines.PurchaseOrderLinesRequest;
import org.demo.whs.entity.dto.request.PurchaseOrders.PurchaseOrdersRequest;
import org.demo.whs.entity.enums.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Inbound Receipt Flow Integration Tests")
class InboundReceiptFlowIntegrationTest extends BaseIntegrationTest {

    private Warehouses warehouse;
    private Locations storageLocation;
    private Products product;
    private BusinessPartners supplier;

    private static final String[] INBOUND_PERMS = {
            "PERM_PURCHASE_ORDER_CREATE",
            "PERM_PURCHASE_ORDER_READ",
            "PERM_PURCHASE_ORDER_UPDATE",
            "PERM_PURCHASE_ORDER_DELETE",
            "PERM_PURCHASE_ORDER_LINE_CREATE",
            "PERM_PURCHASE_ORDER_LINE_READ",
            "PERM_PURCHASE_ORDER_LINE_UPDATE",
            "PERM_PURCHASE_ORDER_LINE_DELETE",
            "PERM_INBOUND_RECEIPT_CREATE",
            "PERM_INBOUND_RECEIPT_READ",
            "PERM_INBOUND_RECEIPT_UPDATE",
            "PERM_INBOUND_RECEIPT_DELETE",
            "PERM_INBOUND_RECEIPT_LINE_CREATE",
            "PERM_INBOUND_RECEIPT_LINE_READ",
            "PERM_INBOUND_RECEIPT_LINE_UPDATE",
            "PERM_INBOUND_RECEIPT_LINE_DELETE"
    };

    @BeforeEach
    void setUpMasterData() {
        String suffix = UUID.randomUUID().toString().substring(0, 4);

        warehouse = dataFactory.createWarehouse("WH-" + suffix, "Inbound Warehouse " + suffix);
        storageLocation = dataFactory.createLocation(warehouse.getId(), "LOC-STR-" + suffix, "Storage " + suffix, LocationType.STORAGE);

        Category category = dataFactory.createCategory("CAT-" + suffix, "Category " + suffix);
        UnitsOfMeasure uom = dataFactory.createUom("U-" + suffix, "Box", UnitsOfMeasureType.COUNT);
        product = dataFactory.createProduct("SKU-INB-" + suffix, "Inbound Product " + suffix, category.getId(), uom.getId());

        supplier = dataFactory.createPartner("SUP-" + suffix, "Supplier " + suffix, BusinessPartnerType.SUPPLIER);
    }

    @Test
    @DisplayName("INB-01: Full Inbound Lifecycle - Create PO -> Add Lines -> Confirm PO -> Create Receipt -> Add Lines -> Confirm Receipt -> Verify Inventory & Stock Movement")
    void should_CompleteInboundLifecycle_When_ValidPurchaseOrderAndReceiptConfirmed() throws Exception {
        // Step 1: Create Purchase Order (Draft)
        PurchaseOrdersRequest poRequest = PurchaseOrdersRequest.builder()
                .supplierId(supplier.getId())
                .warehouseId(warehouse.getId())
                .orderDate(LocalDate.now())
                .expectedDeliveryDate(LocalDate.now().plusDays(5))
                .currency("VND")
                .paymentTerms("Net 30")
                .notes("Integration test PO")
                .build();

        MvcResult poResult = performPost("/api/v1/purchase-orders", poRequest, DEFAULT_ADMIN_USER, INBOUND_PERMS)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andReturn();

        String poId = JsonPath.read(poResult.getResponse().getContentAsString(), "$.data.id");

        // Step 2: Add Line to PO (100 items)
        PurchaseOrderLinesRequest lineRequest = PurchaseOrderLinesRequest.builder()
                .purchaseOrderId(poId)
                .productId(product.getId())
                .quantityOrdered(new BigDecimal("100.00"))
                .unitPrice(new BigDecimal("50000.00"))
                .notes("100 items for PO")
                .build();

        performPost("/api/v1/purchase-order-lines", lineRequest, DEFAULT_ADMIN_USER, INBOUND_PERMS)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));

        // Get PO line ID
        List<PurchaseOrderLines> poLines = purchaseOrderLinesRepository.findByPurchaseOrderIdOrderByLineNumberAsc(poId);
        assertThat(poLines).hasSize(1);
        String poLineId = poLines.get(0).getId();

        // Step 3: Confirm Purchase Order
        performPut("/api/v1/purchase-orders/" + poId + "/confirm", null, DEFAULT_ADMIN_USER, INBOUND_PERMS)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));

        // Step 4: Create Inbound Receipt Draft
        InboundReceiptsRequest receiptRequest = new InboundReceiptsRequest();
        receiptRequest.setPurchaseOrderId(poId);
        receiptRequest.setReceiptDate(LocalDate.now());
        receiptRequest.setDeliveryNoteNumber("DN-TEST-001");
        receiptRequest.setNotes("Integration test inbound receipt");

        MvcResult receiptResult = performPost("/api/v1/inbound-receipts", receiptRequest, DEFAULT_ADMIN_USER, INBOUND_PERMS)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andReturn();

        String receiptId = JsonPath.read(receiptResult.getResponse().getContentAsString(), "$.data.id");

        // Step 5: Add Line to Inbound Receipt (Receive full 100 items into Storage Location)
        InboundReceiptLinesRequest receiptLineRequest = InboundReceiptLinesRequest.builder()
                .inboundReceiptId(receiptId)
                .purchaseOrderLineId(poLineId)
                .locationId(storageLocation.getId())
                .quantityReceived(new BigDecimal("100.00"))
                .qualityStatus(QualityStatus.PASS)
                .notes("Received full 100 items in good condition")
                .build();

        performPost("/api/v1/inbound-receipt-lines", receiptLineRequest, DEFAULT_ADMIN_USER, INBOUND_PERMS)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));

        // Step 6: Confirm Inbound Receipt (Executes stock increase and movement write)
        performPut("/api/v1/inbound-receipts/" + receiptId + "/confirm", null, DEFAULT_ADMIN_USER, INBOUND_PERMS)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));

        // ========== ASSERTIONS: Verify Data Integrity across tables ==========

        // 1. Inbound Receipt status
        InboundReceipts confirmedReceipt = inboundReceiptsRepository.findById(receiptId).orElseThrow();
        assertThat(confirmedReceipt.getStatus()).isEqualTo(InboundReceiptsStatus.CONFIRMED);

        // 2. Purchase Order status transition: Should be COMPLETED because all 100 items were received
        PurchaseOrders completedPO = purchaseOrdersRepository.findById(poId).orElseThrow();
        assertThat(completedPO.getStatus()).isEqualTo(PurchaseOrdersStatus.COMPLETED);

        // 3. Purchase Order Line quantity received updated
        PurchaseOrderLines updatedPOLine = purchaseOrderLinesRepository.findById(poLineId).orElseThrow();
        assertThat(updatedPOLine.getQuantityReceived()).isEqualByComparingTo("100.00");

        // 4. Inventory increased: On hand quantity must be 100.00
        List<Inventory> inventories = inventoryRepository.findAll().stream()
                .filter(i -> product.getId().equals(i.getProductId()) && warehouse.getId().equals(i.getWarehouseId()))
                .toList();
        assertThat(inventories).isNotEmpty();
        Inventory inv = inventories.get(0);
        assertThat(inv.getOnHandQuantity()).isEqualByComparingTo("100.00");
        assertThat(inv.getLocationId()).isEqualTo(storageLocation.getId());

        // 5. Stock Movement audit record created with INBOUND type
        List<StockMovements> movements = stockMovementsRepository.findAll().stream()
                .filter(m -> receiptId.equals(m.getReferenceId()))
                .toList();
        assertThat(movements).isNotEmpty();
        StockMovements movement = movements.get(0);
        assertThat(movement.getMovementType()).isEqualTo(StockMovementsType.INBOUND);
        assertThat(movement.getQuantityChange()).isEqualByComparingTo("100.00");
        assertThat(movement.getQuantityBefore()).isEqualByComparingTo("0.00");
        assertThat(movement.getQuantityAfter()).isEqualByComparingTo("100.00");
        assertThat(movement.getReferenceType()).isEqualTo(ReferenceType.INBOUND_RECEIPT);
    }

    @Test
    @DisplayName("INB-02: Partial Receipts - Receive in multiple shipments -> PO status transitions DRAFT -> CONFIRMED -> PARTIALLY_RECEIVED -> COMPLETED")
    void should_SupportPartialReceipt_When_MultipleReceiptsForSinglePO() throws Exception {
        // Step 1: Create and Confirm PO with 100 items
        PurchaseOrdersRequest poRequest = PurchaseOrdersRequest.builder()
                .supplierId(supplier.getId())
                .warehouseId(warehouse.getId())
                .orderDate(LocalDate.now())
                .expectedDeliveryDate(LocalDate.now().plusDays(7))
                .currency("VND")
                .paymentTerms("Net 30")
                .build();

        MvcResult poResult = performPost("/api/v1/purchase-orders", poRequest, DEFAULT_ADMIN_USER, INBOUND_PERMS)
                .andExpect(status().isCreated())
                .andReturn();
        String poId = JsonPath.read(poResult.getResponse().getContentAsString(), "$.data.id");

        PurchaseOrderLinesRequest lineRequest = PurchaseOrderLinesRequest.builder()
                .purchaseOrderId(poId)
                .productId(product.getId())
                .quantityOrdered(new BigDecimal("100.00"))
                .unitPrice(new BigDecimal("50000.00"))
                .build();
        performPost("/api/v1/purchase-order-lines", lineRequest, DEFAULT_ADMIN_USER, INBOUND_PERMS)
                .andExpect(status().isCreated());

        String poLineId = purchaseOrderLinesRepository.findByPurchaseOrderIdOrderByLineNumberAsc(poId).get(0).getId();

        performPut("/api/v1/purchase-orders/" + poId + "/confirm", null, DEFAULT_ADMIN_USER, INBOUND_PERMS)
                .andExpect(status().isOk());

        // Step 2: First Receipt - Receive 40 items
        InboundReceiptsRequest receipt1Req = new InboundReceiptsRequest();
        receipt1Req.setPurchaseOrderId(poId);
        receipt1Req.setReceiptDate(LocalDate.now());

        MvcResult r1Result = performPost("/api/v1/inbound-receipts", receipt1Req, DEFAULT_ADMIN_USER, INBOUND_PERMS)
                .andExpect(status().isCreated())
                .andReturn();
        String receipt1Id = JsonPath.read(r1Result.getResponse().getContentAsString(), "$.data.id");

        InboundReceiptLinesRequest r1LineReq = InboundReceiptLinesRequest.builder()
                .inboundReceiptId(receipt1Id)
                .purchaseOrderLineId(poLineId)
                .locationId(storageLocation.getId())
                .quantityReceived(new BigDecimal("40.00"))
                .qualityStatus(QualityStatus.PASS)
                .build();
        performPost("/api/v1/inbound-receipt-lines", r1LineReq, DEFAULT_ADMIN_USER, INBOUND_PERMS)
                .andExpect(status().isCreated());

        // Confirm First Receipt
        performPut("/api/v1/inbound-receipts/" + receipt1Id + "/confirm", null, DEFAULT_ADMIN_USER, INBOUND_PERMS)
                .andExpect(status().isOk());

        // Verify intermediate state: PO is PARTIALLY_RECEIVED, Inventory is 40
        PurchaseOrders partiallyReceivedPO = purchaseOrdersRepository.findById(poId).orElseThrow();
        assertThat(partiallyReceivedPO.getStatus()).isEqualTo(PurchaseOrdersStatus.PARTIALLY_RECEIVED);

        Inventory invAfterFirst = inventoryRepository.findAll().stream()
                .filter(i -> product.getId().equals(i.getProductId()) && warehouse.getId().equals(i.getWarehouseId()))
                .findFirst().orElseThrow();
        assertThat(invAfterFirst.getOnHandQuantity()).isEqualByComparingTo("40.00");

        // Step 3: Second Receipt - Receive remaining 60 items
        InboundReceiptsRequest receipt2Req = new InboundReceiptsRequest();
        receipt2Req.setPurchaseOrderId(poId);
        receipt2Req.setReceiptDate(LocalDate.now());

        MvcResult r2Result = performPost("/api/v1/inbound-receipts", receipt2Req, DEFAULT_ADMIN_USER, INBOUND_PERMS)
                .andExpect(status().isCreated())
                .andReturn();
        String receipt2Id = JsonPath.read(r2Result.getResponse().getContentAsString(), "$.data.id");

        InboundReceiptLinesRequest r2LineReq = InboundReceiptLinesRequest.builder()
                .inboundReceiptId(receipt2Id)
                .purchaseOrderLineId(poLineId)
                .locationId(storageLocation.getId())
                .quantityReceived(new BigDecimal("60.00"))
                .qualityStatus(QualityStatus.PASS)
                .build();
        performPost("/api/v1/inbound-receipt-lines", r2LineReq, DEFAULT_ADMIN_USER, INBOUND_PERMS)
                .andExpect(status().isCreated());

        // Confirm Second Receipt
        performPut("/api/v1/inbound-receipts/" + receipt2Id + "/confirm", null, DEFAULT_ADMIN_USER, INBOUND_PERMS)
                .andExpect(status().isOk());

        // Verify final state: PO is COMPLETED, total inventory is 100
        PurchaseOrders completedPO = purchaseOrdersRepository.findById(poId).orElseThrow();
        assertThat(completedPO.getStatus()).isEqualTo(PurchaseOrdersStatus.COMPLETED);

        Inventory invAfterSecond = inventoryRepository.findAll().stream()
                .filter(i -> product.getId().equals(i.getProductId()) && warehouse.getId().equals(i.getWarehouseId()))
                .findFirst().orElseThrow();
        assertThat(invAfterSecond.getOnHandQuantity()).isEqualByComparingTo("100.00");

        // Verify two separate stock movements exist
        List<StockMovements> m1 = stockMovementsRepository.findAll().stream()
                .filter(m -> receipt1Id.equals(m.getReferenceId()))
                .toList();
        List<StockMovements> m2 = stockMovementsRepository.findAll().stream()
                .filter(m -> receipt2Id.equals(m.getReferenceId()))
                .toList();
        assertThat(m1).hasSize(1);
        assertThat(m2).hasSize(1);
        assertThat(m1.get(0).getQuantityChange()).isEqualByComparingTo("40.00");
        assertThat(m2.get(0).getQuantityChange()).isEqualByComparingTo("60.00");
    }

    @Test
    @DisplayName("INB-03: Validation Error - Cannot receive quantity exceeding remaining PO quantity")
    void should_ThrowBadRequest_When_ReceiptQuantityExceedsPurchaseOrder() throws Exception {
        // Setup PO with 50 items
        PurchaseOrdersRequest poRequest = PurchaseOrdersRequest.builder()
                .supplierId(supplier.getId())
                .warehouseId(warehouse.getId())
                .orderDate(LocalDate.now())
                .expectedDeliveryDate(LocalDate.now().plusDays(5))
                .currency("VND")
                .paymentTerms("Net 30")
                .build();

        MvcResult poResult = performPost("/api/v1/purchase-orders", poRequest, DEFAULT_ADMIN_USER, INBOUND_PERMS)
                .andExpect(status().isCreated())
                .andReturn();
        String poId = JsonPath.read(poResult.getResponse().getContentAsString(), "$.data.id");

        PurchaseOrderLinesRequest lineRequest = PurchaseOrderLinesRequest.builder()
                .purchaseOrderId(poId)
                .productId(product.getId())
                .quantityOrdered(new BigDecimal("50.00"))
                .unitPrice(new BigDecimal("50000.00"))
                .build();
        performPost("/api/v1/purchase-order-lines", lineRequest, DEFAULT_ADMIN_USER, INBOUND_PERMS)
                .andExpect(status().isCreated());

        performPut("/api/v1/purchase-orders/" + poId + "/confirm", null, DEFAULT_ADMIN_USER, INBOUND_PERMS)
                .andExpect(status().isOk());

        String poLineId = purchaseOrderLinesRepository.findByPurchaseOrderIdOrderByLineNumberAsc(poId).get(0).getId();

        // Attempt to create receipt with 60 items (> 50 items ordered)
        InboundReceiptsRequest receiptRequest = new InboundReceiptsRequest();
        receiptRequest.setPurchaseOrderId(poId);
        receiptRequest.setReceiptDate(LocalDate.now());

        MvcResult receiptResult = performPost("/api/v1/inbound-receipts", receiptRequest, DEFAULT_ADMIN_USER, INBOUND_PERMS)
                .andExpect(status().isCreated())
                .andReturn();
        String receiptId = JsonPath.read(receiptResult.getResponse().getContentAsString(), "$.data.id");

        InboundReceiptLinesRequest receiptLineRequest = InboundReceiptLinesRequest.builder()
                .inboundReceiptId(receiptId)
                .purchaseOrderLineId(poLineId)
                .locationId(storageLocation.getId())
                .quantityReceived(new BigDecimal("60.00"))
                .qualityStatus(QualityStatus.PASS)
                .build();
        // Adding line with 60 items (> 50 ordered) must immediately fail with 400 Bad Request
        performPost("/api/v1/inbound-receipt-lines", receiptLineRequest, DEFAULT_ADMIN_USER, INBOUND_PERMS)
                .andExpect(status().isBadRequest());

        // Verify inventory was NOT created/updated
        List<Inventory> inventories = inventoryRepository.findAll().stream()
                .filter(i -> product.getId().equals(i.getProductId()) && warehouse.getId().equals(i.getWarehouseId()))
                .toList();
        assertThat(inventories).isEmpty();
    }

    @Test
    @DisplayName("INB-04: Sequence Validation - Cannot create receipt for unconfirmed (DRAFT) Purchase Order")
    void should_ThrowBadRequest_When_POIsNotConfirmed() throws Exception {
        // Create PO but do NOT confirm it (remains DRAFT)
        PurchaseOrdersRequest poRequest = PurchaseOrdersRequest.builder()
                .supplierId(supplier.getId())
                .warehouseId(warehouse.getId())
                .orderDate(LocalDate.now())
                .expectedDeliveryDate(LocalDate.now().plusDays(5))
                .currency("VND")
                .paymentTerms("Net 30")
                .build();

        MvcResult poResult = performPost("/api/v1/purchase-orders", poRequest, DEFAULT_ADMIN_USER, INBOUND_PERMS)
                .andExpect(status().isCreated())
                .andReturn();
        String poId = JsonPath.read(poResult.getResponse().getContentAsString(), "$.data.id");

        // Attempt to create receipt from DRAFT PO
        InboundReceiptsRequest receiptRequest = new InboundReceiptsRequest();
        receiptRequest.setPurchaseOrderId(poId);
        receiptRequest.setReceiptDate(LocalDate.now());

        performPost("/api/v1/inbound-receipts", receiptRequest, DEFAULT_ADMIN_USER, INBOUND_PERMS)
                .andExpect(status().isBadRequest());
    }
}
