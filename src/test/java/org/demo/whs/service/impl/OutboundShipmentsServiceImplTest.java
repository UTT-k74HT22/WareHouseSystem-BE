package org.demo.whs.service.impl;

import org.demo.whs.entity.*;
import org.demo.whs.entity.dto.request.Inventory.InventoryDecreaseRequest;
import org.demo.whs.entity.dto.request.Inventory.InventoryUnreserveRequest;
import org.demo.whs.entity.dto.request.OutboundShipments.OutboundShipmentsFilterRequest;
import org.demo.whs.entity.dto.request.OutboundShipments.OutboundShipmentsRequest;
import org.demo.whs.entity.dto.request.OutboundShipments.UpdateOutboundShipmentsRequest;
import org.demo.whs.entity.dto.response.OutboundShipments.OutboundShipmentsResponse;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.enums.*;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.NotFoundException;
import org.demo.whs.mapper.OutboundShipmentsMapper;
import org.demo.whs.repository.*;
import org.demo.whs.service.InventoryService;
import org.demo.whs.service.LocationService;
import org.demo.whs.utils.IdentifierGenerator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OutboundShipmentsServiceImpl Unit Tests")
class OutboundShipmentsServiceImplTest {

    private static final String USERNAME = "shipment-tester";
    private static final String ACTOR_ID = "acc-ship-001";
    private static final String SALES_ORDER_ID = "so-001";
    private static final String WAREHOUSE_ID = "wh-001";
    private static final String SHIPMENT_ID = "ship-001";

    @Mock private OutboundShipmentsRepository outboundShipmentsRepository;
    @Mock private OutboundShipmentLinesRepository outboundShipmentLinesRepository;
    @Mock private SalesOrdersRepository salesOrdersRepository;
    @Mock private SalesOrderLinesRepository salesOrderLinesRepository;
    @Mock private WareHouseRepository wareHouseRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private InventoryService inventoryService;
    @Mock private InventoryRepository inventoryRepository;
    @Mock private InventoryReservationRepository inventoryReservationRepository;
    @Mock private StockMovementsRepository stockMovementsRepository;
    @Mock private LocationRepository locationRepository;
    @Mock private LocationService locationService;
    @Mock private OutboundShipmentsMapper outboundShipmentsMapper;
    @Mock private IdentifierGenerator identifierGenerator;

    private OutboundShipmentsServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OutboundShipmentsServiceImpl(
                outboundShipmentsRepository,
                outboundShipmentLinesRepository,
                salesOrdersRepository,
                salesOrderLinesRepository,
                wareHouseRepository,
                accountRepository,
                inventoryService,
                inventoryRepository,
                inventoryReservationRepository,
                stockMovementsRepository,
                locationRepository,
                locationService,
                outboundShipmentsMapper,
                identifierGenerator
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(USERNAME, "password", List.of())
        );
        Account account = Account.builder().username(USERNAME).password("secret").status(AccountStatus.ACTIVE).build();
        account.setId(ACTOR_ID);
        lenient().when(accountRepository.findByUsername(USERNAME)).thenReturn(Optional.of(account));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ==================== CREATE ====================

    @Test
    @DisplayName("should_CreateDraftShipment_When_SalesOrderIsConfirmedAndWarehouseMatches")
    void should_CreateDraftShipment_When_SalesOrderIsConfirmedAndWarehouseMatches() {
        OutboundShipmentsRequest request = OutboundShipmentsRequest.builder()
                .salesOrderId(SALES_ORDER_ID)
                .warehouseId(WAREHOUSE_ID)
                .shipmentDate(LocalDate.of(2026, 3, 22))
                .carrier("GHN")
                .notes("Shipment notes")
                .build();

        SalesOrders salesOrder = buildSalesOrder(SALES_ORDER_ID, SalesOrdersStatus.CONFIRMED);
        OutboundShipments entity = buildShipment(null, "SHIP-001", OutboundShipmentsStatus.DRAFT);
        OutboundShipmentsResponse response = buildResponse(SHIPMENT_ID, "SHIP-001", OutboundShipmentsStatus.DRAFT);

        when(salesOrdersRepository.findById(SALES_ORDER_ID)).thenReturn(Optional.of(salesOrder));
        when(wareHouseRepository.findById(WAREHOUSE_ID)).thenReturn(Optional.of(buildWarehouse(WAREHOUSE_ID)));
        when(identifierGenerator.generate(anyString(), anyInt(), any())).thenReturn("SHIP-001");
        when(outboundShipmentsMapper.toEntity(request)).thenReturn(entity);
        when(outboundShipmentsRepository.save(any(OutboundShipments.class))).thenAnswer(inv -> {
            OutboundShipments e = inv.getArgument(0);
            e.setId(SHIPMENT_ID);
            e.setCreatedAt(LocalDateTime.now());
            e.setUpdatedAt(LocalDateTime.now());
            return e;
        });
        when(outboundShipmentsMapper.toResponse(any(OutboundShipments.class))).thenReturn(response);

        OutboundShipmentsResponse result = service.create(request);

        assertThat(result.getId()).isEqualTo(SHIPMENT_ID);
        assertThat(result.getStatus()).isEqualTo(OutboundShipmentsStatus.DRAFT);
        verify(outboundShipmentsRepository).save(any(OutboundShipments.class));
    }

    @Test
    @DisplayName("should_CreateDraftShipment_When_SalesOrderIsPartiallyShipped")
    void should_CreateDraftShipment_When_SalesOrderIsPartiallyShipped() {
        OutboundShipmentsRequest request = OutboundShipmentsRequest.builder()
                .salesOrderId(SALES_ORDER_ID)
                .warehouseId(WAREHOUSE_ID)
                .shipmentDate(LocalDate.of(2026, 3, 22))
                .build();

        SalesOrders salesOrder = buildSalesOrder(SALES_ORDER_ID, SalesOrdersStatus.PARTIALLY_SHIPPED);
        OutboundShipments entity = buildShipment(null, "SHIP-001", OutboundShipmentsStatus.DRAFT);
        OutboundShipmentsResponse response = buildResponse(SHIPMENT_ID, "SHIP-001", OutboundShipmentsStatus.DRAFT);

        when(salesOrdersRepository.findById(SALES_ORDER_ID)).thenReturn(Optional.of(salesOrder));
        when(wareHouseRepository.findById(WAREHOUSE_ID)).thenReturn(Optional.of(buildWarehouse(WAREHOUSE_ID)));
        when(identifierGenerator.generate(anyString(), anyInt(), any())).thenReturn("SHIP-001");
        when(outboundShipmentsMapper.toEntity(request)).thenReturn(entity);
        when(outboundShipmentsRepository.save(any(OutboundShipments.class))).thenAnswer(inv -> {
            OutboundShipments e = inv.getArgument(0);
            e.setId(SHIPMENT_ID);
            e.setCreatedAt(LocalDateTime.now());
            e.setUpdatedAt(LocalDateTime.now());
            return e;
        });
        when(outboundShipmentsMapper.toResponse(any(OutboundShipments.class))).thenReturn(response);

        OutboundShipmentsResponse result = service.create(request);

        assertThat(result.getStatus()).isEqualTo(OutboundShipmentsStatus.DRAFT);
    }

    @Test
    @DisplayName("should_ThrowNotFound_When_SalesOrderNotFound")
    void should_ThrowNotFound_When_SalesOrderNotFound() {
        OutboundShipmentsRequest request = OutboundShipmentsRequest.builder()
                .salesOrderId("so-missing")
                .warehouseId(WAREHOUSE_ID)
                .shipmentDate(LocalDate.of(2026, 3, 22))
                .build();

        when(salesOrdersRepository.findById("so-missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Sales order not found");
    }

    @Test
    @DisplayName("should_ThrowBadRequest_When_SalesOrderStatusIsDraft")
    void should_ThrowBadRequest_When_SalesOrderStatusIsDraft() {
        OutboundShipmentsRequest request = OutboundShipmentsRequest.builder()
                .salesOrderId(SALES_ORDER_ID)
                .warehouseId(WAREHOUSE_ID)
                .shipmentDate(LocalDate.of(2026, 3, 22))
                .build();

        SalesOrders salesOrder = buildSalesOrder(SALES_ORDER_ID, SalesOrdersStatus.DRAFT);

        when(salesOrdersRepository.findById(SALES_ORDER_ID)).thenReturn(Optional.of(salesOrder));

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("CONFIRMED or PARTIALLY_SHIPPED");
    }

    @Test
    @DisplayName("should_ThrowBadRequest_When_WarehouseDoesNotMatchSalesOrder")
    void should_ThrowBadRequest_When_WarehouseDoesNotMatchSalesOrder() {
        OutboundShipmentsRequest request = OutboundShipmentsRequest.builder()
                .salesOrderId(SALES_ORDER_ID)
                .warehouseId("wh-other")
                .shipmentDate(LocalDate.of(2026, 3, 22))
                .build();

        SalesOrders salesOrder = buildSalesOrder(SALES_ORDER_ID, SalesOrdersStatus.CONFIRMED);

        when(salesOrdersRepository.findById(SALES_ORDER_ID)).thenReturn(Optional.of(salesOrder));

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("warehouse must match");
    }

    @Test
    @DisplayName("should_ThrowNotFound_When_WarehouseNotFound")
    void should_ThrowNotFound_When_WarehouseNotFound() {
        OutboundShipmentsRequest request = OutboundShipmentsRequest.builder()
                .salesOrderId(SALES_ORDER_ID)
                .warehouseId(WAREHOUSE_ID)
                .shipmentDate(LocalDate.of(2026, 3, 22))
                .build();

        SalesOrders salesOrder = buildSalesOrder(SALES_ORDER_ID, SalesOrdersStatus.CONFIRMED);

        when(salesOrdersRepository.findById(SALES_ORDER_ID)).thenReturn(Optional.of(salesOrder));
        when(wareHouseRepository.findById(WAREHOUSE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Warehouse not found");
    }

    // ==================== GET BY ID ====================

    @Test
    @DisplayName("should_ReturnShipmentById_When_Exists")
    void should_ReturnShipmentById_When_Exists() {
        OutboundShipments entity = buildShipment(SHIPMENT_ID, "SHIP-001", OutboundShipmentsStatus.DRAFT);
        OutboundShipmentsResponse response = buildResponse(SHIPMENT_ID, "SHIP-001", OutboundShipmentsStatus.DRAFT);

        when(outboundShipmentsRepository.findById(SHIPMENT_ID)).thenReturn(Optional.of(entity));
        when(outboundShipmentLinesRepository.findByOutboundShipmentId(SHIPMENT_ID)).thenReturn(List.of());
        when(outboundShipmentsMapper.toResponse(entity, List.of())).thenReturn(response);

        OutboundShipmentsResponse result = service.getById(SHIPMENT_ID);

        assertThat(result.getId()).isEqualTo(SHIPMENT_ID);
        assertThat(result.getShipmentNumber()).isEqualTo("SHIP-001");
    }

    @Test
    @DisplayName("should_ThrowNotFound_When_GetByIdNotExists")
    void should_ThrowNotFound_When_GetByIdNotExists() {
        when(outboundShipmentsRepository.findById("ship-missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById("ship-missing"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Outbound shipment not found");
    }

    // ==================== GET ALL ====================

    @Test
    @DisplayName("should_ReturnFilteredShipments_When_GetAllCalled")
    void should_ReturnFilteredShipments_When_GetAllCalled() {
        OutboundShipmentsFilterRequest filter = OutboundShipmentsFilterRequest.builder()
                .status(OutboundShipmentsStatus.DRAFT)
                .build();
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "updatedAt"));
        OutboundShipments entity = buildShipment(SHIPMENT_ID, "SHIP-001", OutboundShipmentsStatus.DRAFT);

        when(outboundShipmentsRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(entity), pageable, 1));
        when(outboundShipmentsMapper.toResponse(entity))
                .thenReturn(buildResponse(SHIPMENT_ID, "SHIP-001", OutboundShipmentsStatus.DRAFT));

        PageResponse<OutboundShipmentsResponse> result = service.getAll(filter, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("should_ReturnEmptyPage_When_GetAllWithNoResults")
    void should_ReturnEmptyPage_When_GetAllWithNoResults() {
        OutboundShipmentsFilterRequest filter = OutboundShipmentsFilterRequest.builder().build();
        Pageable pageable = PageRequest.of(0, 10);

        when(outboundShipmentsRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        PageResponse<OutboundShipmentsResponse> result = service.getAll(filter, pageable);

        assertThat(result.getTotalElements()).isEqualTo(0);
        assertThat(result.getContent()).isEmpty();
    }

    // ==================== UPDATE ====================

    @Test
    @DisplayName("should_UpdateDraftShipment_When_RequestIsValid")
    void should_UpdateDraftShipment_When_RequestIsValid() {
        OutboundShipments existing = buildShipment(SHIPMENT_ID, "SHIP-001", OutboundShipmentsStatus.DRAFT);
        UpdateOutboundShipmentsRequest request = UpdateOutboundShipmentsRequest.builder()
                .shipmentDate(LocalDate.of(2026, 3, 25))
                .carrier("GHTK")
                .notes("Updated notes")
                .build();
        OutboundShipmentsResponse response = buildResponse(SHIPMENT_ID, "SHIP-001", OutboundShipmentsStatus.DRAFT);

        when(outboundShipmentsRepository.findById(SHIPMENT_ID)).thenReturn(Optional.of(existing));
        doNothing().when(outboundShipmentsMapper).updateEntity(any(), any());
        when(outboundShipmentsRepository.save(any(OutboundShipments.class))).thenReturn(existing);
        when(outboundShipmentsMapper.toResponse(existing)).thenReturn(response);

        OutboundShipmentsResponse result = service.update(SHIPMENT_ID, request);

        assertThat(result.getId()).isEqualTo(SHIPMENT_ID);
        verify(outboundShipmentsMapper).updateEntity(existing, request);
    }

    @Test
    @DisplayName("should_ThrowBadRequest_When_UpdateNonDraftShipment")
    void should_ThrowBadRequest_When_UpdateNonDraftShipment() {
        OutboundShipments picking = buildShipment(SHIPMENT_ID, "SHIP-001", OutboundShipmentsStatus.PICKING);
        UpdateOutboundShipmentsRequest request = UpdateOutboundShipmentsRequest.builder()
                .notes("Try to update")
                .build();

        when(outboundShipmentsRepository.findById(SHIPMENT_ID)).thenReturn(Optional.of(picking));

        assertThatThrownBy(() -> service.update(SHIPMENT_ID, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Only DRAFT shipments can be updated");
    }

    @Test
    @DisplayName("should_ThrowBadRequest_When_UpdatePackedShipment")
    void should_ThrowBadRequest_When_UpdatePackedShipment() {
        OutboundShipments packed = buildShipment(SHIPMENT_ID, "SHIP-001", OutboundShipmentsStatus.PACKED);
        UpdateOutboundShipmentsRequest request = UpdateOutboundShipmentsRequest.builder()
                .notes("Try to update")
                .build();

        when(outboundShipmentsRepository.findById(SHIPMENT_ID)).thenReturn(Optional.of(packed));

        assertThatThrownBy(() -> service.update(SHIPMENT_ID, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Only DRAFT shipments can be updated");
    }

    // ==================== START PICKING ====================

    @Test
    @DisplayName("should_StartPicking_When_ShipmentIsDraftWithLines")
    void should_StartPicking_When_ShipmentIsDraftWithLines() {
        OutboundShipments shipment = buildShipment(SHIPMENT_ID, "SHIP-001", OutboundShipmentsStatus.DRAFT);
        OutboundShipmentLines line = buildShipmentLine("line-1", SHIPMENT_ID);
        OutboundShipmentsResponse response = buildResponse(SHIPMENT_ID, "SHIP-001", OutboundShipmentsStatus.PICKING);
        Locations pickingLocation = buildLocation("loc-picking", LocationType.PICKING);
        InventoryReservation reservation = buildReservation("res-1", "so-line-1", "prod-1", "loc-reserved", "wh-1", "5.00");

        when(outboundShipmentsRepository.findByIdWithLock(SHIPMENT_ID)).thenReturn(Optional.of(shipment));
        when(outboundShipmentLinesRepository.findByOutboundShipmentId(SHIPMENT_ID)).thenReturn(List.of(line));
        when(locationService.resolveLocationByType(WAREHOUSE_ID, LocationType.PICKING)).thenReturn(pickingLocation);
        when(locationService.increaseUsedCapacity("loc-picking", new BigDecimal("5.00"))).thenReturn(1);
        when(inventoryReservationRepository.findByOrderLineId(line.getSalesOrderLineId())).thenReturn(Optional.of(reservation));
        lenient().doNothing().when(inventoryService).moveInventory(anyString(), anyString(), anyString(), any(), any(), any(), anyString(), anyString(), anyBoolean());
        lenient().when(outboundShipmentLinesRepository.save(any(OutboundShipmentLines.class))).thenReturn(line);
        when(outboundShipmentsRepository.save(any(OutboundShipments.class))).thenReturn(shipment);
        lenient().when(outboundShipmentsMapper.toResponse(any(OutboundShipments.class), any())).thenReturn(response);

        OutboundShipmentsResponse result = service.startPicking(SHIPMENT_ID);

        assertThat(result.getStatus()).isEqualTo(OutboundShipmentsStatus.PICKING);
        assertThat(shipment.getStatus()).isEqualTo(OutboundShipmentsStatus.PICKING);
        verify(locationService).increaseUsedCapacity("loc-picking", new BigDecimal("5.00"));
        verify(locationService, never()).decreaseUsedCapacity("loc-reserved", new BigDecimal("5.00"));
    }

    @Test
    @DisplayName("should_ReturnPicking_When_ShipmentAlreadyPicking")
    void should_ReturnPicking_When_ShipmentAlreadyPicking() {
        OutboundShipments shipment = buildShipment(SHIPMENT_ID, "SHIP-001", OutboundShipmentsStatus.PICKING);
        OutboundShipmentLines line = buildShipmentLine("line-1", SHIPMENT_ID);
        OutboundShipmentsResponse response = buildResponse(SHIPMENT_ID, "SHIP-001", OutboundShipmentsStatus.PICKING);
        Locations pickingLocation = buildLocation("loc-picking", LocationType.PICKING);

        when(outboundShipmentsRepository.findByIdWithLock(SHIPMENT_ID)).thenReturn(Optional.of(shipment));
        when(locationService.resolveLocationByType(WAREHOUSE_ID, LocationType.PICKING)).thenReturn(pickingLocation);
        when(outboundShipmentLinesRepository.findByOutboundShipmentId(SHIPMENT_ID)).thenReturn(List.of(line));
        when(outboundShipmentsMapper.toResponse(shipment, List.of(line))).thenReturn(response);

        OutboundShipmentsResponse result = service.startPicking(SHIPMENT_ID);

        assertThat(result.getStatus()).isEqualTo(OutboundShipmentsStatus.PICKING);
        verify(outboundShipmentsRepository, never()).save(any());
    }

    @Test
    @DisplayName("should_ThrowBadRequest_When_StartPickingFromNonDraftStatus")
    void should_ThrowBadRequest_When_StartPickingFromNonDraftStatus() {
        OutboundShipments shipment = buildShipment(SHIPMENT_ID, "SHIP-001", OutboundShipmentsStatus.PACKED);

        when(outboundShipmentsRepository.findByIdWithLock(SHIPMENT_ID)).thenReturn(Optional.of(shipment));

        assertThatThrownBy(() -> service.startPicking(SHIPMENT_ID))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("DRAFT status");
    }

    @Test
    @DisplayName("should_ThrowBadRequest_When_StartPickingWithNoLines")
    void should_ThrowBadRequest_When_StartPickingWithNoLines() {
        OutboundShipments shipment = buildShipment(SHIPMENT_ID, "SHIP-001", OutboundShipmentsStatus.DRAFT);

        when(outboundShipmentsRepository.findByIdWithLock(SHIPMENT_ID)).thenReturn(Optional.of(shipment));
        when(outboundShipmentLinesRepository.findByOutboundShipmentId(SHIPMENT_ID)).thenReturn(List.of());

        assertThatThrownBy(() -> service.startPicking(SHIPMENT_ID))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("at least one line");
    }

    @Test
    @DisplayName("should_ThrowNotFound_When_StartPickingShipmentNotFound")
    void should_ThrowNotFound_When_StartPickingShipmentNotFound() {
        when(outboundShipmentsRepository.findByIdWithLock("ship-missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.startPicking("ship-missing"))
                .isInstanceOf(NotFoundException.class);
    }

    // ==================== MARK AS PACKED ====================

    @Test
    @DisplayName("should_MarkAsPacked_When_ShipmentIsPicking")
    void should_MarkAsPacked_When_ShipmentIsPicking() {
        OutboundShipments shipment = buildShipment(SHIPMENT_ID, "SHIP-001", OutboundShipmentsStatus.PICKING);
        OutboundShipmentLines line = buildShipmentLine("line-1", SHIPMENT_ID);
        line.setLocationId("loc-picking");
        OutboundShipmentsResponse response = buildResponse(SHIPMENT_ID, "SHIP-001", OutboundShipmentsStatus.PACKED);
        Locations pickingLoc = buildLocation("loc-picking", LocationType.PICKING);
        Locations packingLoc = buildLocation("loc-packing", LocationType.PACKING);

        when(outboundShipmentsRepository.findByIdWithLock(SHIPMENT_ID)).thenReturn(Optional.of(shipment));
        when(outboundShipmentLinesRepository.findByOutboundShipmentId(SHIPMENT_ID)).thenReturn(List.of(line));
        when(locationService.resolveLocationByType(WAREHOUSE_ID, LocationType.PICKING)).thenReturn(pickingLoc);
        when(locationService.resolveLocationByType(WAREHOUSE_ID, LocationType.PACKING)).thenReturn(packingLoc);
        when(locationService.decreaseUsedCapacity("loc-picking", new BigDecimal("5.00"))).thenReturn(1);
        when(locationService.increaseUsedCapacity("loc-packing", new BigDecimal("5.00"))).thenReturn(1);
        when(outboundShipmentLinesRepository.saveAll(anyList())).thenReturn(List.of(line));
        when(outboundShipmentsRepository.save(any(OutboundShipments.class))).thenReturn(shipment);
        lenient().when(outboundShipmentsMapper.toResponse(any(OutboundShipments.class), anyList())).thenReturn(response);

        OutboundShipmentsResponse result = service.markAsPacked(SHIPMENT_ID);

        assertThat(result.getStatus()).isEqualTo(OutboundShipmentsStatus.PACKED);
        assertThat(shipment.getStatus()).isEqualTo(OutboundShipmentsStatus.PACKED);
        verify(locationService).decreaseUsedCapacity("loc-picking", new BigDecimal("5.00"));
        verify(locationService).increaseUsedCapacity("loc-packing", new BigDecimal("5.00"));
    }

    @Test
    @DisplayName("should_ReturnPacked_When_ShipmentAlreadyPacked")
    void should_ReturnPacked_When_ShipmentAlreadyPacked() {
        OutboundShipments shipment = buildShipment(SHIPMENT_ID, "SHIP-001", OutboundShipmentsStatus.PACKED);
        OutboundShipmentLines line = buildShipmentLine("line-1", SHIPMENT_ID);
        OutboundShipmentsResponse response = buildResponse(SHIPMENT_ID, "SHIP-001", OutboundShipmentsStatus.PACKED);
        Locations packingLoc = buildLocation("loc-packing", LocationType.PACKING);

        when(outboundShipmentsRepository.findByIdWithLock(SHIPMENT_ID)).thenReturn(Optional.of(shipment));
        when(locationService.resolveLocationByType(WAREHOUSE_ID, LocationType.PACKING)).thenReturn(packingLoc);
        when(outboundShipmentLinesRepository.findByOutboundShipmentId(SHIPMENT_ID)).thenReturn(List.of(line));
        when(outboundShipmentsMapper.toResponse(shipment, List.of(line))).thenReturn(response);

        OutboundShipmentsResponse result = service.markAsPacked(SHIPMENT_ID);

        assertThat(result.getStatus()).isEqualTo(OutboundShipmentsStatus.PACKED);
        verify(outboundShipmentsRepository, never()).save(any());
    }

    @Test
    @DisplayName("should_ThrowBadRequest_When_MarkAsPackedFromDraft")
    void should_ThrowBadRequest_When_MarkAsPackedFromDraft() {
        OutboundShipments shipment = buildShipment(SHIPMENT_ID, "SHIP-001", OutboundShipmentsStatus.DRAFT);

        when(outboundShipmentsRepository.findByIdWithLock(SHIPMENT_ID)).thenReturn(Optional.of(shipment));

        assertThatThrownBy(() -> service.markAsPacked(SHIPMENT_ID))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("PICKING status");
    }

    @Test
    @DisplayName("should_ThrowBadRequest_When_MarkAsPackedFromShipped")
    void should_ThrowBadRequest_When_MarkAsPackedFromShipped() {
        OutboundShipments shipment = buildShipment(SHIPMENT_ID, "SHIP-001", OutboundShipmentsStatus.SHIPPED);

        when(outboundShipmentsRepository.findByIdWithLock(SHIPMENT_ID)).thenReturn(Optional.of(shipment));

        assertThatThrownBy(() -> service.markAsPacked(SHIPMENT_ID))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("PICKING status");
    }

    // ==================== SHIP ====================

    @Test
    @DisplayName("should_Ship_When_ShipmentIsPackedWithLines")
    void should_Ship_When_ShipmentIsPackedWithLines() {
        OutboundShipments shipment = buildShipment(SHIPMENT_ID, "SHIP-001", OutboundShipmentsStatus.PACKED);
        shipment.setSalesOrderId(SALES_ORDER_ID);
        shipment.setWarehouseId(WAREHOUSE_ID);

        OutboundShipmentLines line = buildShipmentLine("line-1", SHIPMENT_ID);
        line.setLocationId("loc-packing");

        OutboundShipmentsResponse response = buildResponse(SHIPMENT_ID, "SHIP-001", OutboundShipmentsStatus.STAGING);
        Locations packingLoc = buildLocation("loc-packing", LocationType.PACKING);
        Locations stagingLoc = buildLocation("loc-staging", LocationType.STAGING);

        when(outboundShipmentsRepository.findByIdWithLock(SHIPMENT_ID)).thenReturn(Optional.of(shipment));
        when(outboundShipmentLinesRepository.findByOutboundShipmentId(SHIPMENT_ID)).thenReturn(List.of(line));
        when(locationService.resolveLocationByType(WAREHOUSE_ID, LocationType.PACKING)).thenReturn(packingLoc);
        when(locationService.resolveLocationByType(WAREHOUSE_ID, LocationType.STAGING)).thenReturn(stagingLoc);
        when(locationService.decreaseUsedCapacity("loc-packing", new BigDecimal("5.00"))).thenReturn(1);
        when(locationService.increaseUsedCapacity("loc-staging", new BigDecimal("5.00"))).thenReturn(1);
        doNothing().when(inventoryService).moveInventory(anyString(), anyString(), anyString(), any(), any(), any(), anyString(), anyString(), anyBoolean());
        when(outboundShipmentsRepository.save(any())).thenReturn(shipment);
        when(outboundShipmentsMapper.toResponse(any(OutboundShipments.class), anyList())).thenReturn(response);

        OutboundShipmentsResponse result = service.ship(SHIPMENT_ID);

        assertThat(result.getStatus()).isEqualTo(OutboundShipmentsStatus.STAGING);
        assertThat(line.getLocationId()).isEqualTo("loc-staging");
        verify(inventoryService).moveInventory(anyString(), anyString(), anyString(), any(), any(), any(), anyString(), anyString(), anyBoolean());
        verify(inventoryService, never()).unreserve(any(InventoryUnreserveRequest.class));
        verify(inventoryService, never()).decrease(any(InventoryDecreaseRequest.class));
        verify(locationService).decreaseUsedCapacity("loc-packing", new BigDecimal("5.00"));
        verify(locationService).increaseUsedCapacity("loc-staging", new BigDecimal("5.00"));
        verify(outboundShipmentLinesRepository).saveAll(List.of(line));
    }

    @Test
    @DisplayName("should_ReturnStaging_When_ShipmentAlreadyInStaging")
    void should_ReturnStaging_When_ShipmentAlreadyInStaging() {
        OutboundShipments shipment = buildShipment(SHIPMENT_ID, "SHIP-001", OutboundShipmentsStatus.STAGING);
        OutboundShipmentLines line = buildShipmentLine("line-1", SHIPMENT_ID);
        OutboundShipmentsResponse response = buildResponse(SHIPMENT_ID, "SHIP-001", OutboundShipmentsStatus.STAGING);

        when(outboundShipmentsRepository.findByIdWithLock(SHIPMENT_ID)).thenReturn(Optional.of(shipment));
        when(outboundShipmentLinesRepository.findByOutboundShipmentId(SHIPMENT_ID)).thenReturn(List.of(line));
        when(outboundShipmentsMapper.toResponse(shipment, List.of(line))).thenReturn(response);

        OutboundShipmentsResponse result = service.ship(SHIPMENT_ID);

        assertThat(result.getStatus()).isEqualTo(OutboundShipmentsStatus.STAGING);
        verify(inventoryService, never()).decrease(any());
    }

    @Test
    @DisplayName("should_ThrowBadRequest_When_ShipFromDraft")
    void should_ThrowBadRequest_When_ShipFromDraft() {
        OutboundShipments shipment = buildShipment(SHIPMENT_ID, "SHIP-001", OutboundShipmentsStatus.DRAFT);

        when(outboundShipmentsRepository.findByIdWithLock(SHIPMENT_ID)).thenReturn(Optional.of(shipment));

        assertThatThrownBy(() -> service.ship(SHIPMENT_ID))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("PACKED");
    }

    @Test
    @DisplayName("should_ThrowBadRequest_When_ShipFromPicking")
    void should_ThrowBadRequest_When_ShipFromPicking() {
        OutboundShipments shipment = buildShipment(SHIPMENT_ID, "SHIP-001", OutboundShipmentsStatus.PICKING);

        when(outboundShipmentsRepository.findByIdWithLock(SHIPMENT_ID)).thenReturn(Optional.of(shipment));

        assertThatThrownBy(() -> service.ship(SHIPMENT_ID))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("PACKED");
    }

    @Test
    @DisplayName("should_ThrowBadRequest_When_ShipWithNoLines")
    void should_ThrowBadRequest_When_ShipWithNoLines() {
        OutboundShipments shipment = buildShipment(SHIPMENT_ID, "SHIP-001", OutboundShipmentsStatus.PACKED);

        when(outboundShipmentsRepository.findByIdWithLock(SHIPMENT_ID)).thenReturn(Optional.of(shipment));
        when(outboundShipmentLinesRepository.findByOutboundShipmentId(SHIPMENT_ID)).thenReturn(List.of());

        assertThatThrownBy(() -> service.ship(SHIPMENT_ID))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("at least one line");
    }

    @Test
    @DisplayName("should_ThrowBadRequest_When_ConfirmDispatchOverShippedQuantity")
    void should_ThrowBadRequest_When_ConfirmDispatchOverShippedQuantity() {
        OutboundShipments shipment = buildShipment(SHIPMENT_ID, "SHIP-001", OutboundShipmentsStatus.STAGING);
        shipment.setSalesOrderId(SALES_ORDER_ID);
        shipment.setWarehouseId(WAREHOUSE_ID);

        OutboundShipmentLines line = buildShipmentLine("line-1", SHIPMENT_ID);
        line.setLocationId("loc-staging");
        line.setBatchId("batch-1");
        line.setQuantityShipped(new BigDecimal("20.00"));
        SalesOrderLines soLine = buildSalesOrderLine("so-line-1", SALES_ORDER_ID, "10.00", "5.00");
        Locations stagingLoc = buildLocation("loc-staging", LocationType.STAGING);
        InventoryReservation reservation = buildReservation("res-1", "so-line-1", "prod-1", "loc-storage", WAREHOUSE_ID, "20.00");
        reservation.setBatchId("batch-1");

        when(outboundShipmentsRepository.findByIdWithLock(SHIPMENT_ID)).thenReturn(Optional.of(shipment));
        when(outboundShipmentLinesRepository.findByOutboundShipmentId(SHIPMENT_ID)).thenReturn(List.of(line));
        when(salesOrderLinesRepository.findById(line.getSalesOrderLineId())).thenReturn(Optional.of(soLine));
        when(locationService.resolveLocationByType(WAREHOUSE_ID, LocationType.STAGING)).thenReturn(stagingLoc);
        when(locationService.decreaseUsedCapacity("loc-staging", new BigDecimal("20.00"))).thenReturn(1);
        when(inventoryReservationRepository.findByOrderLineId("so-line-1")).thenReturn(Optional.of(reservation));
        when(inventoryService.decrease(any(InventoryDecreaseRequest.class))).thenReturn(null);

        assertThatThrownBy(() -> service.confirmDispatch(SHIPMENT_ID))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Over shipped");
    }

    @Test
    @DisplayName("should_ConfirmDispatch_When_ShipmentIsInStaging")
    void should_ConfirmDispatch_When_ShipmentIsInStaging() {
        OutboundShipments shipment = buildShipment(SHIPMENT_ID, "SHIP-001", OutboundShipmentsStatus.STAGING);
        shipment.setSalesOrderId(SALES_ORDER_ID);
        shipment.setWarehouseId(WAREHOUSE_ID);

        OutboundShipmentLines line = buildShipmentLine("line-1", SHIPMENT_ID);
        line.setLocationId("loc-staging");
        line.setBatchId("batch-1");
        SalesOrderLines soLine = buildSalesOrderLine("so-line-1", SALES_ORDER_ID, "10.00", "5.00");
        InventoryReservation reservation = buildReservation("res-1", "so-line-1", "prod-1", "loc-storage", WAREHOUSE_ID, "5.00");
        reservation.setBatchId("batch-1");

        OutboundShipmentsResponse response = buildResponse(SHIPMENT_ID, "SHIP-001", OutboundShipmentsStatus.SHIPPED);
        Locations stagingLoc = buildLocation("loc-staging", LocationType.STAGING);

        when(outboundShipmentsRepository.findByIdWithLock(SHIPMENT_ID)).thenReturn(Optional.of(shipment));
        when(outboundShipmentLinesRepository.findByOutboundShipmentId(SHIPMENT_ID)).thenReturn(List.of(line));
        when(locationService.resolveLocationByType(WAREHOUSE_ID, LocationType.STAGING)).thenReturn(stagingLoc);
        when(locationService.decreaseUsedCapacity("loc-staging", new BigDecimal("5.00"))).thenReturn(1);
        when(inventoryReservationRepository.findByOrderLineId("so-line-1")).thenReturn(Optional.of(reservation));
        lenient().when(inventoryService.decrease(any(InventoryDecreaseRequest.class))).thenReturn(null);
        when(salesOrderLinesRepository.findById("so-line-1")).thenReturn(Optional.of(soLine));
        when(salesOrdersRepository.findById(SALES_ORDER_ID)).thenReturn(Optional.of(buildSalesOrder(SALES_ORDER_ID, SalesOrdersStatus.CONFIRMED)));
        lenient().when(salesOrderLinesRepository.findBySalesOrderId(SALES_ORDER_ID)).thenReturn(List.of(soLine));
        lenient().when(outboundShipmentsRepository.save(any())).thenReturn(shipment);
        when(outboundShipmentsMapper.toResponse(any(OutboundShipments.class), anyList())).thenReturn(response);

        OutboundShipmentsResponse result = service.confirmDispatch(SHIPMENT_ID);

        assertThat(result.getStatus()).isEqualTo(OutboundShipmentsStatus.SHIPPED);
        assertThat(line.getLocationId()).isNull();
        verify(inventoryService).decrease(argThat(request ->
                request.isConsumeReserved()
                        && "so-line-1".equals(request.getOrderLineId())
                        && "loc-staging".equals(request.getLocationId())
                        && new BigDecimal("5.00").compareTo(request.getQuantity()) == 0
        ));
        verify(locationService).decreaseUsedCapacity("loc-staging", new BigDecimal("5.00"));
    }

    // ==================== CANCEL ====================

    @Test
    @DisplayName("should_CancelDraftShipment_When_DraftExists")
    void should_CancelDraftShipment_When_DraftExists() {
        OutboundShipments shipment = buildShipment(SHIPMENT_ID, "SHIP-001", OutboundShipmentsStatus.DRAFT);
        OutboundShipmentsResponse response = buildResponse(SHIPMENT_ID, "SHIP-001", OutboundShipmentsStatus.CANCELLED);

        when(outboundShipmentsRepository.findByIdWithLock(SHIPMENT_ID)).thenReturn(Optional.of(shipment));
        when(outboundShipmentsRepository.save(any(OutboundShipments.class))).thenReturn(shipment);
        when(outboundShipmentsMapper.toResponse(any(OutboundShipments.class))).thenReturn(response);

        OutboundShipmentsResponse result = service.cancel(SHIPMENT_ID);

        assertThat(result.getStatus()).isEqualTo(OutboundShipmentsStatus.CANCELLED);
        assertThat(shipment.getStatus()).isEqualTo(OutboundShipmentsStatus.CANCELLED);
    }

    @Test
    @DisplayName("should_CancelPickingShipment_AndMoveInventoryBack")
    void should_CancelPickingShipment_AndMoveInventoryBack() {
        OutboundShipments shipment = buildShipment(SHIPMENT_ID, "SHIP-001", OutboundShipmentsStatus.PICKING);
        shipment.setWarehouseId(WAREHOUSE_ID);
        OutboundShipmentLines line = buildShipmentLine("line-1", SHIPMENT_ID);
        line.setLocationId("loc-picking");
        OutboundShipmentsResponse response = buildResponse(SHIPMENT_ID, "SHIP-001", OutboundShipmentsStatus.CANCELLED);
        Locations pickingLoc = buildLocation("loc-picking", LocationType.PICKING);
        InventoryReservation reservation = buildReservation("res-1", "so-line-1", "prod-1", "loc-storage", WAREHOUSE_ID, "5.00");

        when(outboundShipmentsRepository.findByIdWithLock(SHIPMENT_ID)).thenReturn(Optional.of(shipment));
        when(outboundShipmentLinesRepository.findByOutboundShipmentId(SHIPMENT_ID)).thenReturn(List.of(line));
        when(locationService.resolveLocationByType(WAREHOUSE_ID, LocationType.PICKING)).thenReturn(pickingLoc);
        when(locationService.decreaseUsedCapacity("loc-picking", new BigDecimal("5.00"))).thenReturn(1);
        when(inventoryReservationRepository.findByOrderLineId("so-line-1")).thenReturn(Optional.of(reservation));
        doNothing().when(inventoryService).moveInventory(anyString(), anyString(), anyString(), any(), any(), any(), anyString(), anyString(), anyBoolean());
        when(outboundShipmentsRepository.save(any(OutboundShipments.class))).thenReturn(shipment);
        when(outboundShipmentsMapper.toResponse(any(OutboundShipments.class))).thenReturn(response);

        OutboundShipmentsResponse result = service.cancel(SHIPMENT_ID);

        assertThat(result.getStatus()).isEqualTo(OutboundShipmentsStatus.CANCELLED);
        assertThat(line.getLocationId()).isEqualTo("loc-storage");
        verify(inventoryService).moveInventory(anyString(), anyString(), anyString(), any(), any(), any(), anyString(), anyString(), anyBoolean());
        verify(inventoryService, never()).unreserve(any(InventoryUnreserveRequest.class));
        verify(locationService).decreaseUsedCapacity("loc-picking", new BigDecimal("5.00"));
        verify(locationService, never()).increaseUsedCapacity("loc-storage", new BigDecimal("5.00"));
        verify(outboundShipmentLinesRepository).saveAll(List.of(line));
    }

    @Test
    @DisplayName("should_CancelPackedShipment_AndMoveInventoryBack")
    void should_CancelPackedShipment_AndMoveInventoryBack() {
        OutboundShipments shipment = buildShipment(SHIPMENT_ID, "SHIP-001", OutboundShipmentsStatus.PACKED);
        shipment.setWarehouseId(WAREHOUSE_ID);
        OutboundShipmentLines line = buildShipmentLine("line-1", SHIPMENT_ID);
        line.setLocationId("loc-packing");
        OutboundShipmentsResponse response = buildResponse(SHIPMENT_ID, "SHIP-001", OutboundShipmentsStatus.CANCELLED);
        Locations pickingLoc = buildLocation("loc-picking", LocationType.PICKING);
        Locations packingLoc = buildLocation("loc-packing", LocationType.PACKING);

        when(outboundShipmentsRepository.findByIdWithLock(SHIPMENT_ID)).thenReturn(Optional.of(shipment));
        when(outboundShipmentLinesRepository.findByOutboundShipmentId(SHIPMENT_ID)).thenReturn(List.of(line));
        when(locationService.resolveLocationByType(WAREHOUSE_ID, LocationType.PICKING)).thenReturn(pickingLoc);
        when(locationService.resolveLocationByType(WAREHOUSE_ID, LocationType.PACKING)).thenReturn(packingLoc);
        when(locationService.decreaseUsedCapacity("loc-packing", new BigDecimal("5.00"))).thenReturn(1);
        when(locationService.increaseUsedCapacity("loc-picking", new BigDecimal("5.00"))).thenReturn(1);
        doNothing().when(inventoryService).moveInventory(anyString(), anyString(), anyString(), any(), any(), any(), anyString(), anyString(), anyBoolean());
        when(outboundShipmentsRepository.save(any(OutboundShipments.class))).thenReturn(shipment);
        when(outboundShipmentsMapper.toResponse(any(OutboundShipments.class))).thenReturn(response);

        OutboundShipmentsResponse result = service.cancel(SHIPMENT_ID);

        assertThat(result.getStatus()).isEqualTo(OutboundShipmentsStatus.CANCELLED);
        assertThat(line.getLocationId()).isEqualTo("loc-picking");
        verify(inventoryService).moveInventory(anyString(), anyString(), anyString(), any(), any(), any(), anyString(), anyString(), anyBoolean());
        verify(inventoryService, never()).unreserve(any(InventoryUnreserveRequest.class));
        verify(locationService).decreaseUsedCapacity("loc-packing", new BigDecimal("5.00"));
        verify(locationService).increaseUsedCapacity("loc-picking", new BigDecimal("5.00"));
        verify(outboundShipmentLinesRepository).saveAll(List.of(line));
    }

    @Test
    @DisplayName("should_ReturnCancelled_When_ShipmentAlreadyCancelled")
    void should_ReturnCancelled_When_ShipmentAlreadyCancelled() {
        OutboundShipments shipment = buildShipment(SHIPMENT_ID, "SHIP-001", OutboundShipmentsStatus.CANCELLED);
        OutboundShipmentsResponse response = buildResponse(SHIPMENT_ID, "SHIP-001", OutboundShipmentsStatus.CANCELLED);

        when(outboundShipmentsRepository.findByIdWithLock(SHIPMENT_ID)).thenReturn(Optional.of(shipment));
        when(outboundShipmentsMapper.toResponse(shipment)).thenReturn(response);

        OutboundShipmentsResponse result = service.cancel(SHIPMENT_ID);

        assertThat(result.getStatus()).isEqualTo(OutboundShipmentsStatus.CANCELLED);
        verify(outboundShipmentsRepository, never()).save(any());
    }

    @Test
    @DisplayName("should_ThrowBadRequest_When_CancelShippedShipment")
    void should_ThrowBadRequest_When_CancelShippedShipment() {
        OutboundShipments shipment = buildShipment(SHIPMENT_ID, "SHIP-001", OutboundShipmentsStatus.SHIPPED);

        when(outboundShipmentsRepository.findByIdWithLock(SHIPMENT_ID)).thenReturn(Optional.of(shipment));

        assertThatThrownBy(() -> service.cancel(SHIPMENT_ID))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot cancel a SHIPPED shipment");
    }

    @Test
    @DisplayName("should_CancelPickingShipment_EvenWhenMovementsExist")
    void should_CancelPickingShipment_EvenWhenMovementsExist() {
        OutboundShipments shipment = buildShipment(SHIPMENT_ID, "SHIP-001", OutboundShipmentsStatus.PICKING);
        shipment.setWarehouseId(WAREHOUSE_ID);
        OutboundShipmentLines line = buildShipmentLine("line-1", SHIPMENT_ID);
        line.setLocationId("loc-picking");
        OutboundShipmentsResponse response = buildResponse(SHIPMENT_ID, "SHIP-001", OutboundShipmentsStatus.CANCELLED);
        Locations pickingLoc = buildLocation("loc-picking", LocationType.PICKING);
        InventoryReservation reservation = buildReservation("res-1", "so-line-1", "prod-1", "loc-storage", WAREHOUSE_ID, "5.00");

        when(outboundShipmentsRepository.findByIdWithLock(SHIPMENT_ID)).thenReturn(Optional.of(shipment));
        when(outboundShipmentLinesRepository.findByOutboundShipmentId(SHIPMENT_ID)).thenReturn(List.of(line));
        when(locationService.resolveLocationByType(WAREHOUSE_ID, LocationType.PICKING)).thenReturn(pickingLoc);
        when(locationService.decreaseUsedCapacity("loc-picking", new BigDecimal("5.00"))).thenReturn(1);
        when(inventoryReservationRepository.findByOrderLineId("so-line-1")).thenReturn(Optional.of(reservation));
        doNothing().when(inventoryService).moveInventory(anyString(), anyString(), anyString(), any(), any(), any(), anyString(), anyString(), anyBoolean());
        when(outboundShipmentsRepository.save(any(OutboundShipments.class))).thenReturn(shipment);
        when(outboundShipmentsMapper.toResponse(any(OutboundShipments.class))).thenReturn(response);

        OutboundShipmentsResponse result = service.cancel(SHIPMENT_ID);

        assertThat(result.getStatus()).isEqualTo(OutboundShipmentsStatus.CANCELLED);
        verify(inventoryService).moveInventory(anyString(), anyString(), anyString(), any(), any(), any(), anyString(), anyString(), anyBoolean());
        verify(locationService, never()).increaseUsedCapacity("loc-storage", new BigDecimal("5.00"));
    }

    // ==================== HELPER METHODS ====================

    private OutboundShipments buildShipment(String id, String shipmentNumber, OutboundShipmentsStatus status) {
        OutboundShipments entity = OutboundShipments.builder()
                .shipmentNumber(shipmentNumber)
                .salesOrderId(SALES_ORDER_ID)
                .warehouseId(WAREHOUSE_ID)
                .shipmentDate(LocalDate.of(2026, 3, 22))
                .status(status)
                .carrier("GHN")
                .notes("Test shipment")
                .build();
        if (id != null) entity.setId(id);
        entity.setCreatedAt(LocalDateTime.of(2026, 3, 22, 10, 0, 0));
        entity.setUpdatedAt(LocalDateTime.of(2026, 3, 22, 11, 0, 0));
        return entity;
    }

    private OutboundShipmentLines buildShipmentLine(String id, String shipmentId) {
        OutboundShipmentLines line = OutboundShipmentLines.builder()
                .outboundShipmentId(shipmentId)
                .salesOrderLineId("so-line-1")
                .productId("prod-1")
                .locationId("loc-1")
                .batchId(null)
                .lineNumber(1)
                .quantityShipped(new BigDecimal("5.00"))
                .build();
        if (id != null) line.setId(id);
        line.setCreatedAt(LocalDateTime.of(2026, 3, 22, 10, 0, 0));
        line.setUpdatedAt(LocalDateTime.of(2026, 3, 22, 11, 0, 0));
        return line;
    }

    private SalesOrders buildSalesOrder(String id, SalesOrdersStatus status) {
        SalesOrders entity = SalesOrders.builder()
                .soNumber("SO-001")
                .customerId("bp-1")
                .warehouseId(WAREHOUSE_ID)
                .orderDate(LocalDate.of(2026, 3, 20))
                .requestedDeliveryDate(LocalDate.of(2026, 3, 25))
                .status(status)
                .subTotal(new BigDecimal("500.00"))
                .taxAmount(BigDecimal.ZERO)
                .totalAmount(new BigDecimal("500.00"))
                .currency(CurrencyType.VND)
                .build();
        if (id != null) entity.setId(id);
        entity.setCreatedAt(LocalDateTime.of(2026, 3, 20, 10, 0, 0));
        entity.setUpdatedAt(LocalDateTime.of(2026, 3, 20, 11, 0, 0));
        return entity;
    }

    private SalesOrderLines buildSalesOrderLine(String id, String soId, String qtyOrdered, String qtyShipped) {
        SalesOrderLines line = SalesOrderLines.builder()
                .salesOrderId(soId)
                .productId("prod-1")
                .lineNumber(1)
                .quantityOrdered(new BigDecimal(qtyOrdered))
                .quantityShipped(new BigDecimal(qtyShipped))
                .unitPrice(new BigDecimal("100.00"))
                .lineTotal(new BigDecimal(qtyOrdered).multiply(new BigDecimal("100.00")))
                .build();
        if (id != null) line.setId(id);
        line.setCreatedAt(LocalDateTime.of(2026, 3, 20, 10, 0, 0));
        line.setUpdatedAt(LocalDateTime.of(2026, 3, 20, 11, 0, 0));
        return line;
    }

    private Warehouses buildWarehouse(String id) {
        Warehouses wh = Warehouses.builder()
                .code("WH-001")
                .name("Main WH")
                .status(WareHouseStatus.ACTIVE)
                .type(WareHouseType.MAIN)
                .build();
        if (id != null) wh.setId(id);
        wh.setCreatedAt(LocalDateTime.of(2026, 3, 1, 10, 0, 0));
        wh.setUpdatedAt(LocalDateTime.of(2026, 3, 1, 10, 0, 0));
        return wh;
    }

    private Locations buildLocation(String id, LocationType type) {
        Locations loc = Locations.builder()
                .code("LOC-001")
                .name("Location 1")
                .type(type)
                .status(LocationStatus.ACTIVE)
                .warehouseId(WAREHOUSE_ID)
                .build();
        if (id != null) loc.setId(id);
        return loc;
    }

    private InventoryReservation buildReservation(String id, String orderLineId, String productId, 
                                                     String locationId, String warehouseId, String quantity) {
        InventoryReservation res = InventoryReservation.builder()
                .orderLineId(orderLineId)
                .productId(productId)
                .locationId(locationId)
                .warehouseId(warehouseId)
                .quantity(new BigDecimal(quantity))
                .status(InventoryReservationStatus.RESERVED)
                .build();
        if (id != null) res.setId(id);
        return res;
    }

    private OutboundShipmentsResponse buildResponse(String id, String shipmentNumber, OutboundShipmentsStatus status) {
        return OutboundShipmentsResponse.builder()
                .id(id)
                .shipmentNumber(shipmentNumber)
                .salesOrderId(SALES_ORDER_ID)
                .warehouseId(WAREHOUSE_ID)
                .shipmentDate(LocalDate.of(2026, 3, 22))
                .status(status)
                .carrier("GHN")
                .notes("Test shipment")
                .createdAt(LocalDateTime.of(2026, 3, 22, 10, 0, 0))
                .updatedAt(LocalDateTime.of(2026, 3, 22, 11, 0, 0))
                .build();
    }
}
