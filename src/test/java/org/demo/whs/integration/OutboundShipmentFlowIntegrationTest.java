package org.demo.whs.integration;

import com.jayway.jsonpath.JsonPath;
import org.demo.whs.entity.*;
import org.demo.whs.entity.dto.request.OutboundShipmentLines.OutboundShipmentLinesRequest;
import org.demo.whs.entity.dto.request.OutboundShipments.OutboundShipmentsRequest;
import org.demo.whs.entity.dto.request.SalesOrderLines.SalesOrderLinesRequest;
import org.demo.whs.entity.dto.request.SalesOrders.SalesOrdersRequest;
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

@DisplayName("Outbound Shipment Flow Integration Tests")
class OutboundShipmentFlowIntegrationTest extends BaseIntegrationTest {

    private Warehouses warehouse;
    private Locations storageLocation;
    private Locations pickingLocation;
    private Locations packingLocation;
    private Locations stagingLocation;
    private Products product;
    private BusinessPartners customer;

    private static final String[] OUTBOUND_PERMS = {
            "PERM_SALES_ORDER_CREATE",
            "PERM_SALES_ORDER_READ",
            "PERM_SALES_ORDER_UPDATE",
            "PERM_SALES_ORDER_DELETE",
            "PERM_SALES_ORDER_LINE_CREATE",
            "PERM_SALES_ORDER_LINE_READ",
            "PERM_SALES_ORDER_LINE_UPDATE",
            "PERM_SALES_ORDER_LINE_DELETE",
            "PERM_OUTBOUND_SHIPMENT_CREATE",
            "PERM_OUTBOUND_SHIPMENT_READ",
            "PERM_OUTBOUND_SHIPMENT_UPDATE",
            "PERM_OUTBOUND_SHIPMENT_DELETE",
            "PERM_OUTBOUND_SHIPMENT_LINE_CREATE",
            "PERM_OUTBOUND_SHIPMENT_LINE_READ",
            "PERM_OUTBOUND_SHIPMENT_LINE_UPDATE",
            "PERM_OUTBOUND_SHIPMENT_LINE_DELETE"
    };

    @BeforeEach
    void setUpMasterData() {
        String suffix = UUID.randomUUID().toString().substring(0, 4);

        warehouse = dataFactory.createWarehouse("WH-" + suffix, "Outbound Warehouse " + suffix);

        // Standard 4 warehouse location zones required by WMS lifecycle
        storageLocation = dataFactory.createLocation(warehouse.getId(), "LOC-STR-" + suffix, "Storage " + suffix, LocationType.STORAGE);
        pickingLocation = dataFactory.createLocation(warehouse.getId(), "LOC-PIK-" + suffix, "Picking " + suffix, LocationType.PICKING);
        packingLocation = dataFactory.createLocation(warehouse.getId(), "LOC-PAK-" + suffix, "Packing " + suffix, LocationType.PACKING);
        stagingLocation = dataFactory.createLocation(warehouse.getId(), "LOC-STG-" + suffix, "Staging " + suffix, LocationType.STAGING);

        Category category = dataFactory.createCategory("CAT-" + suffix, "Category " + suffix);
        UnitsOfMeasure uom = dataFactory.createUom("U-" + suffix, "Piece", UnitsOfMeasureType.COUNT);
        product = dataFactory.createProduct("SKU-OUT-" + suffix, "Outbound Product " + suffix, category.getId(), uom.getId());

        customer = dataFactory.createPartner("CUS-" + suffix, "Customer " + suffix, BusinessPartnerType.CUSTOMER);
    }

    @Test
    @DisplayName("OUT-01: Full Outbound Lifecycle - Create SO -> Confirm (Reserve) -> Create Shipment -> Picking -> Packing -> Staging -> Dispatch -> Verify Inventory & Stock Movements")
    void should_CompleteOutboundLifecycle_When_ValidSalesOrderAndShipmentDispatched() throws Exception {
        // Step 0: Seed initial stock: 100 items in STORAGE location
        dataFactory.seedInventory(product.getId(), warehouse.getId(), storageLocation.getId(), null,
                new BigDecimal("100.00"), BigDecimal.ZERO);

        // Step 1: Create Sales Order (Draft) with 1 line for 30 items
        SalesOrderLinesRequest soLineReq = SalesOrderLinesRequest.builder()
                .productId(product.getId())
                .quantityOrdered(new BigDecimal("30.00"))
                .unitPrice(new BigDecimal("80000.00"))
                .notes("Order line for 30 items")
                .build();

        SalesOrdersRequest soRequest = SalesOrdersRequest.builder()
                .customerId(customer.getId())
                .warehouseId(warehouse.getId())
                .orderDate(LocalDate.now())
                .requestedDeliveryDate(LocalDate.now().plusDays(3))
                .currency("VND")
                .notes("Integration test SO")
                .lines(List.of(soLineReq))
                .build();

        MvcResult soResult = performPost("/api/v1/sales-orders", soRequest, DEFAULT_ADMIN_USER, OUTBOUND_PERMS)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andReturn();

        String soId = JsonPath.read(soResult.getResponse().getContentAsString(), "$.data.id");

        List<SalesOrderLines> soLines = salesOrderLinesRepository.findBySalesOrderId(soId);
        assertThat(soLines).hasSize(1);
        String soLineId = soLines.get(0).getId();

        // Step 2: Confirm Sales Order (Triggers Inventory Reservation)
        performPut("/api/v1/sales-orders/" + soId + "/confirm", null, DEFAULT_ADMIN_USER, OUTBOUND_PERMS)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));

        // Verify stock reservation: 30 reserved, 70 available
        Inventory invAfterReserve = inventoryRepository.findAll().stream()
                .filter(i -> product.getId().equals(i.getProductId()) && warehouse.getId().equals(i.getWarehouseId()) && storageLocation.getId().equals(i.getLocationId()))
                .findFirst().orElseThrow();
        assertThat(invAfterReserve.getReservedQuantity()).isEqualByComparingTo("30.00");
        assertThat(invAfterReserve.getOnHandQuantity()).isEqualByComparingTo("100.00");
        assertThat(invAfterReserve.getAvailableQuantity()).isEqualByComparingTo("70.00");

        // Step 3: Create Outbound Shipment (Draft)
        OutboundShipmentsRequest shipmentRequest = OutboundShipmentsRequest.builder()
                .salesOrderId(soId)
                .warehouseId(warehouse.getId())
                .shipmentDate(LocalDate.now())
                .carrier("FastExpress")
                .notes("Integration test shipment")
                .build();

        MvcResult shipmentResult = performPost("/api/v1/outbound-shipments", shipmentRequest, DEFAULT_ADMIN_USER, OUTBOUND_PERMS)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andReturn();

        String shipmentId = JsonPath.read(shipmentResult.getResponse().getContentAsString(), "$.data.id");

        // Step 4: Add Shipment Line (30 items)
        OutboundShipmentLinesRequest shipmentLineRequest = OutboundShipmentLinesRequest.builder()
                .outboundShipmentId(shipmentId)
                .salesOrderLineId(soLineId)
                .productId(product.getId())
                .locationId(storageLocation.getId())
                .quantityShipped(new BigDecimal("30.00"))
                .notes("Shipment line for 30 items")
                .build();

        performPost("/api/v1/outbound-shipment-lines", shipmentLineRequest, DEFAULT_ADMIN_USER, OUTBOUND_PERMS)
                .andExpect(status().isCreated());

        // Step 5: Transition DRAFT -> PICKING (Moves reserved stock from STORAGE to PICKING)
        performPut("/api/v1/outbound-shipments/" + shipmentId + "/start-picking", null, DEFAULT_ADMIN_USER, OUTBOUND_PERMS)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PICKING"));

        // Step 6: Transition PICKING -> PACKED (Moves stock from PICKING to PACKING)
        performPut("/api/v1/outbound-shipments/" + shipmentId + "/mark-as-packed", null, DEFAULT_ADMIN_USER, OUTBOUND_PERMS)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PACKED"));

        // Step 7: Transition PACKED -> STAGING (Moves stock from PACKING to STAGING)
        performPut("/api/v1/outbound-shipments/" + shipmentId + "/ship", null, DEFAULT_ADMIN_USER, OUTBOUND_PERMS)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("STAGING"));

        // Step 8: Confirm Dispatch: STAGING -> SHIPPED (Decreases actual on-hand and consumes reservation)
        performPut("/api/v1/outbound-shipments/" + shipmentId + "/confirm-dispatch", null, DEFAULT_ADMIN_USER, OUTBOUND_PERMS)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SHIPPED"));

        // ========== ASSERTIONS: Verify Data Integrity across tables ==========

        // 1. Shipment Status
        OutboundShipments completedShipment = outboundShipmentsRepository.findById(shipmentId).orElseThrow();
        assertThat(completedShipment.getStatus()).isEqualTo(OutboundShipmentsStatus.SHIPPED);

        // 2. Sales Order Status: COMPLETED
        SalesOrders completedSO = salesOrdersRepository.findById(soId).orElseThrow();
        assertThat(completedSO.getStatus()).isEqualTo(SalesOrdersStatus.COMPLETED);

        // 3. Sales Order Line: quantity shipped updated to 30.00
        SalesOrderLines updatedSOLine = salesOrderLinesRepository.findById(soLineId).orElseThrow();
        assertThat(updatedSOLine.getQuantityShipped()).isEqualByComparingTo("30.00");

        // 4. Inventory: Total on hand across warehouse is now 70.00, reserved is 0.00
        BigDecimal totalOnHand = inventoryRepository.findAll().stream()
                .filter(i -> product.getId().equals(i.getProductId()) && warehouse.getId().equals(i.getWarehouseId()))
                .map(Inventory::getOnHandQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalReserved = inventoryRepository.findAll().stream()
                .filter(i -> product.getId().equals(i.getProductId()) && warehouse.getId().equals(i.getWarehouseId()))
                .map(Inventory::getReservedQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(totalOnHand).isEqualByComparingTo("70.00");
        assertThat(totalReserved).isEqualByComparingTo("0.00");

        // 5. Stock Movements: Contains OUTBOUND movement record
        List<StockMovements> outboundMovements = stockMovementsRepository.findAll().stream()
                .filter(m -> shipmentId.equals(m.getReferenceId()) && m.getMovementType() == StockMovementsType.OUTBOUND)
                .toList();
        assertThat(outboundMovements).isNotEmpty();
        StockMovements movement = outboundMovements.get(0);
        assertThat(movement.getMovementType()).isEqualTo(StockMovementsType.OUTBOUND);
        assertThat(movement.getQuantityChange()).isEqualByComparingTo("-30.00");
    }

    @Test
    @DisplayName("OUT-02: Insufficient Stock - Confirming SO fails when requested quantity exceeds available stock")
    void should_ThrowConflict_When_InsufficientStockOnSalesOrderConfirm() throws Exception {
        // Seed only 10 items in stock
        dataFactory.seedInventory(product.getId(), warehouse.getId(), storageLocation.getId(), null,
                new BigDecimal("10.00"), BigDecimal.ZERO);

        // Create SO requesting 25 items (> 10 items in stock)
        SalesOrderLinesRequest soLineReq = SalesOrderLinesRequest.builder()
                .productId(product.getId())
                .quantityOrdered(new BigDecimal("25.00"))
                .unitPrice(new BigDecimal("80000.00"))
                .build();

        SalesOrdersRequest soRequest = SalesOrdersRequest.builder()
                .customerId(customer.getId())
                .warehouseId(warehouse.getId())
                .orderDate(LocalDate.now())
                .requestedDeliveryDate(LocalDate.now().plusDays(3))
                .currency("VND")
                .lines(List.of(soLineReq))
                .build();

        MvcResult soResult = performPost("/api/v1/sales-orders", soRequest, DEFAULT_ADMIN_USER, OUTBOUND_PERMS)
                .andExpect(status().isCreated())
                .andReturn();
        String soId = JsonPath.read(soResult.getResponse().getContentAsString(), "$.data.id");

        // Confirming must fail because stock is insufficient
        performPut("/api/v1/sales-orders/" + soId + "/confirm", null, DEFAULT_ADMIN_USER, OUTBOUND_PERMS)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error_code").value("INV_004"));

        // Verify SO remains DRAFT
        SalesOrders so = salesOrdersRepository.findById(soId).orElseThrow();
        assertThat(so.getStatus()).isEqualTo(SalesOrdersStatus.DRAFT);

        // Verify reserved quantity did NOT change
        Inventory inv = inventoryRepository.findAll().stream()
                .filter(i -> product.getId().equals(i.getProductId()) && warehouse.getId().equals(i.getWarehouseId()))
                .findFirst().orElseThrow();
        assertThat(inv.getReservedQuantity()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("OUT-03: Order Cancellation - Cancelling confirmed SO unreserves inventory back to available")
    void should_UnreserveInventory_When_SalesOrderCancelled() throws Exception {
        // Seed 100 items in stock
        dataFactory.seedInventory(product.getId(), warehouse.getId(), storageLocation.getId(), null,
                new BigDecimal("100.00"), BigDecimal.ZERO);

        // Create and confirm SO for 20 items
        SalesOrderLinesRequest soLineReq = SalesOrderLinesRequest.builder()
                .productId(product.getId())
                .quantityOrdered(new BigDecimal("20.00"))
                .unitPrice(new BigDecimal("80000.00"))
                .build();

        SalesOrdersRequest soRequest = SalesOrdersRequest.builder()
                .customerId(customer.getId())
                .warehouseId(warehouse.getId())
                .orderDate(LocalDate.now())
                .requestedDeliveryDate(LocalDate.now().plusDays(3))
                .currency("VND")
                .lines(List.of(soLineReq))
                .build();

        MvcResult soResult = performPost("/api/v1/sales-orders", soRequest, DEFAULT_ADMIN_USER, OUTBOUND_PERMS)
                .andExpect(status().isCreated())
                .andReturn();
        String soId = JsonPath.read(soResult.getResponse().getContentAsString(), "$.data.id");

        performPut("/api/v1/sales-orders/" + soId + "/confirm", null, DEFAULT_ADMIN_USER, OUTBOUND_PERMS)
                .andExpect(status().isOk());

        // Verify stock is reserved
        Inventory invReserved = inventoryRepository.findAll().stream()
                .filter(i -> product.getId().equals(i.getProductId()) && warehouse.getId().equals(i.getWarehouseId()))
                .findFirst().orElseThrow();
        assertThat(invReserved.getReservedQuantity()).isEqualByComparingTo("20.00");

        // Cancel Sales Order
        performPut("/api/v1/sales-orders/" + soId + "/cancel", null, DEFAULT_ADMIN_USER, OUTBOUND_PERMS)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        // Verify stock is unreserved back to 0.00
        Inventory invUnreserved = inventoryRepository.findAll().stream()
                .filter(i -> product.getId().equals(i.getProductId()) && warehouse.getId().equals(i.getWarehouseId()))
                .findFirst().orElseThrow();
        assertThat(invUnreserved.getReservedQuantity()).isEqualByComparingTo("0.00");
        assertThat(invUnreserved.getAvailableQuantity()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("OUT-04: Workflow Enforcement - Cannot skip stages (e.g. DRAFT cannot jump directly to SHIPPED)")
    void should_ThrowBadRequest_When_InvalidStateTransitionAttempted() throws Exception {
        // Seed 50 items in stock
        dataFactory.seedInventory(product.getId(), warehouse.getId(), storageLocation.getId(), null,
                new BigDecimal("50.00"), BigDecimal.ZERO);

        SalesOrderLinesRequest soLineReq = SalesOrderLinesRequest.builder()
                .productId(product.getId())
                .quantityOrdered(new BigDecimal("10.00"))
                .unitPrice(new BigDecimal("80000.00"))
                .build();

        SalesOrdersRequest soRequest = SalesOrdersRequest.builder()
                .customerId(customer.getId())
                .warehouseId(warehouse.getId())
                .orderDate(LocalDate.now())
                .requestedDeliveryDate(LocalDate.now().plusDays(3))
                .currency("VND")
                .lines(List.of(soLineReq))
                .build();

        MvcResult soResult = performPost("/api/v1/sales-orders", soRequest, DEFAULT_ADMIN_USER, OUTBOUND_PERMS)
                .andExpect(status().isCreated())
                .andReturn();
        String soId = JsonPath.read(soResult.getResponse().getContentAsString(), "$.data.id");

        performPut("/api/v1/sales-orders/" + soId + "/confirm", null, DEFAULT_ADMIN_USER, OUTBOUND_PERMS)
                .andExpect(status().isOk());

        // Create Shipment in DRAFT
        OutboundShipmentsRequest shipmentRequest = OutboundShipmentsRequest.builder()
                .salesOrderId(soId)
                .warehouseId(warehouse.getId())
                .shipmentDate(LocalDate.now())
                .build();

        MvcResult shipmentResult = performPost("/api/v1/outbound-shipments", shipmentRequest, DEFAULT_ADMIN_USER, OUTBOUND_PERMS)
                .andExpect(status().isCreated())
                .andReturn();
        String shipmentId = JsonPath.read(shipmentResult.getResponse().getContentAsString(), "$.data.id");

        // Attempting confirm-dispatch directly from DRAFT must be rejected with 400 Bad Request
        performPut("/api/v1/outbound-shipments/" + shipmentId + "/confirm-dispatch", null, DEFAULT_ADMIN_USER, OUTBOUND_PERMS)
                .andExpect(status().isBadRequest());
    }
}
