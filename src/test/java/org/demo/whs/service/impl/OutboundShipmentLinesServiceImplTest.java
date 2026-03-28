package org.demo.whs.service.impl;

import org.demo.whs.entity.*;
import org.demo.whs.entity.dto.request.OutboundShipmentLines.OutboundShipmentLinesRequest;
import org.demo.whs.entity.dto.request.OutboundShipmentLines.UpdateOutboundShipmentLinesRequest;
import org.demo.whs.entity.dto.response.OutboundShipmentLines.OutboundShipmentLinesResponse;
import org.demo.whs.entity.enums.*;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.NotFoundException;
import org.demo.whs.mapper.OutboundShipmentLinesMapper;
import org.demo.whs.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OutboundShipmentLinesServiceImpl Unit Tests")
class OutboundShipmentLinesServiceImplTest {

    private static final String USERNAME = "line-tester";
    private static final String ACTOR_ID = "acc-line-001";
    private static final String SHIPMENT_ID = "ship-001";
    private static final String LINE_ID = "line-001";
    private static final String SALES_ORDER_LINE_ID = "so-line-001";
    private static final String PRODUCT_ID = "prod-001";
    private static final String LOCATION_ID = "loc-001";
    private static final String WAREHOUSE_ID = "wh-001";

    @Mock private OutboundShipmentLinesRepository outboundShipmentLinesRepository;
    @Mock private OutboundShipmentsRepository outboundShipmentsRepository;
    @Mock private SalesOrderLinesRepository salesOrderLinesRepository;
    @Mock private ProductRepository productRepository;
    @Mock private LocationRepository locationRepository;
    @Mock private BatchRepository batchRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private OutboundShipmentLinesMapper outboundShipmentLinesMapper;

    private OutboundShipmentLinesServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OutboundShipmentLinesServiceImpl(
                outboundShipmentLinesRepository,
                outboundShipmentsRepository,
                salesOrderLinesRepository,
                productRepository,
                locationRepository,
                batchRepository,
                accountRepository,
                outboundShipmentLinesMapper
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
    @DisplayName("should_CreateLine_When_RequestIsValidAndShipmentIsDraft")
    void should_CreateLine_When_RequestIsValidAndShipmentIsDraft() {
        OutboundShipmentLinesRequest request = OutboundShipmentLinesRequest.builder()
                .outboundShipmentId(SHIPMENT_ID)
                .salesOrderLineId(SALES_ORDER_LINE_ID)
                .productId(PRODUCT_ID)
                .locationId(LOCATION_ID)
                .batchId(null)
                .quantityShipped(new BigDecimal("5.00"))
                .notes("Pick note")
                .build();

        OutboundShipments shipment = buildShipment(SHIPMENT_ID, OutboundShipmentsStatus.DRAFT);
        SalesOrderLines soLine = buildSalesOrderLine(SALES_ORDER_LINE_ID, "prod-001", "10.00", "0.00");
        Products product = buildProduct(PRODUCT_ID);
        Locations location = buildLocation(LOCATION_ID);
        OutboundShipmentLines entity = buildLine(LINE_ID, SHIPMENT_ID);
        OutboundShipmentLinesResponse response = buildResponse(LINE_ID, SHIPMENT_ID);

        when(outboundShipmentsRepository.findByIdWithLock(SHIPMENT_ID)).thenReturn(Optional.of(shipment));
        when(salesOrderLinesRepository.findById(SALES_ORDER_LINE_ID)).thenReturn(Optional.of(soLine));
        when(outboundShipmentLinesRepository.findByOutboundShipmentIdAndSalesOrderLineIdAndLocationIdAndBatchId(
                anyString(), anyString(), anyString(), any())).thenReturn(List.of());
        when(outboundShipmentLinesRepository.sumShippedForSoLine(anyString(), anyString())).thenReturn(null);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(locationRepository.findById(LOCATION_ID)).thenReturn(Optional.of(location));
        when(outboundShipmentLinesMapper.toEntity(request)).thenReturn(entity);
        when(outboundShipmentLinesRepository.findMaxLineNumber(SHIPMENT_ID)).thenReturn(null);
        when(outboundShipmentLinesRepository.save(any(OutboundShipmentLines.class))).thenAnswer(inv -> {
            OutboundShipmentLines e = inv.getArgument(0);
            e.setId(LINE_ID);
            e.setCreatedAt(LocalDateTime.now());
            e.setUpdatedAt(LocalDateTime.now());
            return e;
        });
        when(outboundShipmentLinesMapper.toResponse(any(OutboundShipmentLines.class))).thenReturn(response);

        OutboundShipmentLinesResponse result = service.create(request);

        assertThat(result.getId()).isEqualTo(LINE_ID);
        assertThat(result.getOutboundShipmentId()).isEqualTo(SHIPMENT_ID);
        verify(outboundShipmentLinesRepository).save(any(OutboundShipmentLines.class));
    }

    @Test
    @DisplayName("should_CreateLineWithBatch_When_BatchIdIsProvided")
    void should_CreateLineWithBatch_When_BatchIdIsProvided() {
        String batchId = "batch-001";
        OutboundShipmentLinesRequest request = OutboundShipmentLinesRequest.builder()
                .outboundShipmentId(SHIPMENT_ID)
                .salesOrderLineId(SALES_ORDER_LINE_ID)
                .productId(PRODUCT_ID)
                .locationId(LOCATION_ID)
                .batchId(batchId)
                .quantityShipped(new BigDecimal("3.00"))
                .build();

        OutboundShipments shipment = buildShipment(SHIPMENT_ID, OutboundShipmentsStatus.DRAFT);
        SalesOrderLines soLine = buildSalesOrderLine(SALES_ORDER_LINE_ID, PRODUCT_ID, "10.00", "0.00");
        Products product = buildProduct(PRODUCT_ID);
        Locations location = buildLocation(LOCATION_ID);
        Batch batch = buildBatch(batchId);
        OutboundShipmentLines entity = buildLine(LINE_ID, SHIPMENT_ID);
        entity.setBatchId(batchId);
        OutboundShipmentLinesResponse response = buildResponse(LINE_ID, SHIPMENT_ID);

        when(outboundShipmentsRepository.findByIdWithLock(SHIPMENT_ID)).thenReturn(Optional.of(shipment));
        when(salesOrderLinesRepository.findById(SALES_ORDER_LINE_ID)).thenReturn(Optional.of(soLine));
        when(outboundShipmentLinesRepository.findByOutboundShipmentIdAndSalesOrderLineIdAndLocationIdAndBatchId(
                anyString(), anyString(), anyString(), anyString())).thenReturn(List.of());
        when(outboundShipmentLinesRepository.sumShippedForSoLine(anyString(), anyString())).thenReturn(null);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(locationRepository.findById(LOCATION_ID)).thenReturn(Optional.of(location));
        when(batchRepository.findById(batchId)).thenReturn(Optional.of(batch));
        when(outboundShipmentLinesMapper.toEntity(request)).thenReturn(entity);
        when(outboundShipmentLinesRepository.findMaxLineNumber(SHIPMENT_ID)).thenReturn(1);
        when(outboundShipmentLinesRepository.save(any(OutboundShipmentLines.class))).thenAnswer(inv -> {
            OutboundShipmentLines e = inv.getArgument(0);
            e.setId(LINE_ID);
            e.setCreatedAt(LocalDateTime.now());
            e.setUpdatedAt(LocalDateTime.now());
            return e;
        });
        when(outboundShipmentLinesMapper.toResponse(any(OutboundShipmentLines.class))).thenReturn(response);

        OutboundShipmentLinesResponse result = service.create(request);

        assertThat(result.getId()).isEqualTo(LINE_ID);
        verify(batchRepository, atLeastOnce()).findById(batchId);
    }

    @Test
    @DisplayName("should_ThrowNotFound_When_ShipmentNotFound")
    void should_ThrowNotFound_When_ShipmentNotFound() {
        OutboundShipmentLinesRequest request = OutboundShipmentLinesRequest.builder()
                .outboundShipmentId("ship-missing")
                .salesOrderLineId(SALES_ORDER_LINE_ID)
                .productId(PRODUCT_ID)
                .locationId(LOCATION_ID)
                .quantityShipped(new BigDecimal("5.00"))
                .build();

        when(outboundShipmentsRepository.findByIdWithLock("ship-missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Outbound shipment not found");
    }

    @Test
    @DisplayName("should_ThrowBadRequest_When_ShipmentIsNotDraft")
    void should_ThrowBadRequest_When_ShipmentIsNotDraft() {
        OutboundShipmentLinesRequest request = OutboundShipmentLinesRequest.builder()
                .outboundShipmentId(SHIPMENT_ID)
                .salesOrderLineId(SALES_ORDER_LINE_ID)
                .productId(PRODUCT_ID)
                .locationId(LOCATION_ID)
                .quantityShipped(new BigDecimal("5.00"))
                .build();

        OutboundShipments shipment = buildShipment(SHIPMENT_ID, OutboundShipmentsStatus.PICKING);

        when(outboundShipmentsRepository.findByIdWithLock(SHIPMENT_ID)).thenReturn(Optional.of(shipment));

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Only DRAFT shipments can have lines added");
    }

    @Test
    @DisplayName("should_ThrowNotFound_When_SalesOrderLineNotFound")
    void should_ThrowNotFound_When_SalesOrderLineNotFound() {
        OutboundShipmentLinesRequest request = OutboundShipmentLinesRequest.builder()
                .outboundShipmentId(SHIPMENT_ID)
                .salesOrderLineId("so-line-missing")
                .productId(PRODUCT_ID)
                .locationId(LOCATION_ID)
                .quantityShipped(new BigDecimal("5.00"))
                .build();

        OutboundShipments shipment = buildShipment(SHIPMENT_ID, OutboundShipmentsStatus.DRAFT);

        when(outboundShipmentsRepository.findByIdWithLock(SHIPMENT_ID)).thenReturn(Optional.of(shipment));
        when(salesOrderLinesRepository.findById("so-line-missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Sales order line not found");
    }

    @Test
    @DisplayName("should_ThrowBadRequest_When_SalesOrderLineDoesNotBelongToShipment")
    void should_ThrowBadRequest_When_SalesOrderLineDoesNotBelongToShipment() {
        OutboundShipmentLinesRequest request = OutboundShipmentLinesRequest.builder()
                .outboundShipmentId(SHIPMENT_ID)
                .salesOrderLineId(SALES_ORDER_LINE_ID)
                .productId(PRODUCT_ID)
                .locationId(LOCATION_ID)
                .quantityShipped(new BigDecimal("5.00"))
                .build();

        OutboundShipments shipment = buildShipment(SHIPMENT_ID, OutboundShipmentsStatus.DRAFT);
        SalesOrderLines soLine = buildSalesOrderLine(SALES_ORDER_LINE_ID, PRODUCT_ID, "10.00", "0.00");
        soLine.setSalesOrderId("so-other");

        when(outboundShipmentsRepository.findByIdWithLock(SHIPMENT_ID)).thenReturn(Optional.of(shipment));
        when(salesOrderLinesRepository.findById(SALES_ORDER_LINE_ID)).thenReturn(Optional.of(soLine));

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("does not belong to the shipment's sales order");
    }

    @Test
    @DisplayName("should_ThrowBadRequest_When_ProductDoesNotMatchSalesOrderLine")
    void should_ThrowBadRequest_When_ProductDoesNotMatchSalesOrderLine() {
        OutboundShipmentLinesRequest request = OutboundShipmentLinesRequest.builder()
                .outboundShipmentId(SHIPMENT_ID)
                .salesOrderLineId(SALES_ORDER_LINE_ID)
                .productId("prod-wrong")
                .locationId(LOCATION_ID)
                .quantityShipped(new BigDecimal("5.00"))
                .build();

        OutboundShipments shipment = buildShipment(SHIPMENT_ID, OutboundShipmentsStatus.DRAFT);
        SalesOrderLines soLine = buildSalesOrderLine(SALES_ORDER_LINE_ID, PRODUCT_ID, "10.00", "0.00");

        when(outboundShipmentsRepository.findByIdWithLock(SHIPMENT_ID)).thenReturn(Optional.of(shipment));
        when(salesOrderLinesRepository.findById(SALES_ORDER_LINE_ID)).thenReturn(Optional.of(soLine));

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Product ID must match Sales Order Line product");
    }

    @Test
    @DisplayName("should_ThrowBadRequest_When_DuplicateLineExists")
    void should_ThrowBadRequest_When_DuplicateLineExists() {
        OutboundShipmentLinesRequest request = OutboundShipmentLinesRequest.builder()
                .outboundShipmentId(SHIPMENT_ID)
                .salesOrderLineId(SALES_ORDER_LINE_ID)
                .productId(PRODUCT_ID)
                .locationId(LOCATION_ID)
                .batchId(null)
                .quantityShipped(new BigDecimal("5.00"))
                .build();

        OutboundShipments shipment = buildShipment(SHIPMENT_ID, OutboundShipmentsStatus.DRAFT);
        SalesOrderLines soLine = buildSalesOrderLine(SALES_ORDER_LINE_ID, PRODUCT_ID, "10.00", "0.00");
        OutboundShipmentLines existingLine = buildLine("existing-line", SHIPMENT_ID);

        when(outboundShipmentsRepository.findByIdWithLock(SHIPMENT_ID)).thenReturn(Optional.of(shipment));
        when(salesOrderLinesRepository.findById(SALES_ORDER_LINE_ID)).thenReturn(Optional.of(soLine));
        when(outboundShipmentLinesRepository.findByOutboundShipmentIdAndSalesOrderLineIdAndLocationIdAndBatchId(
                anyString(), anyString(), anyString(), any())).thenReturn(List.of(existingLine));

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("should_ThrowBadRequest_When_TotalQuantityExceedsOrdered")
    void should_ThrowBadRequest_When_TotalQuantityExceedsOrdered() {
        OutboundShipmentLinesRequest request = OutboundShipmentLinesRequest.builder()
                .outboundShipmentId(SHIPMENT_ID)
                .salesOrderLineId(SALES_ORDER_LINE_ID)
                .productId(PRODUCT_ID)
                .locationId(LOCATION_ID)
                .batchId(null)
                .quantityShipped(new BigDecimal("15.00"))
                .build();

        OutboundShipments shipment = buildShipment(SHIPMENT_ID, OutboundShipmentsStatus.DRAFT);
        SalesOrderLines soLine = buildSalesOrderLine(SALES_ORDER_LINE_ID, PRODUCT_ID, "10.00", "0.00");

        when(outboundShipmentsRepository.findByIdWithLock(SHIPMENT_ID)).thenReturn(Optional.of(shipment));
        when(salesOrderLinesRepository.findById(SALES_ORDER_LINE_ID)).thenReturn(Optional.of(soLine));
        when(outboundShipmentLinesRepository.findByOutboundShipmentIdAndSalesOrderLineIdAndLocationIdAndBatchId(
                anyString(), anyString(), anyString(), any())).thenReturn(List.of());
        when(outboundShipmentLinesRepository.sumShippedForSoLine(anyString(), anyString())).thenReturn(BigDecimal.ZERO);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("exceeds ordered quantity");
    }

    @Test
    @DisplayName("should_ThrowNotFound_When_ProductNotFound")
    void should_ThrowNotFound_When_ProductNotFound() {
        OutboundShipmentLinesRequest request = OutboundShipmentLinesRequest.builder()
                .outboundShipmentId(SHIPMENT_ID)
                .salesOrderLineId(SALES_ORDER_LINE_ID)
                .productId("prod-missing")
                .locationId(LOCATION_ID)
                .batchId(null)
                .quantityShipped(new BigDecimal("5.00"))
                .build();

        OutboundShipments shipment = buildShipment(SHIPMENT_ID, OutboundShipmentsStatus.DRAFT);
        SalesOrderLines soLine = buildSalesOrderLine(SALES_ORDER_LINE_ID, "prod-missing", "10.00", "0.00");

        when(outboundShipmentsRepository.findByIdWithLock(SHIPMENT_ID)).thenReturn(Optional.of(shipment));
        when(salesOrderLinesRepository.findById(SALES_ORDER_LINE_ID)).thenReturn(Optional.of(soLine));
        when(outboundShipmentLinesRepository.findByOutboundShipmentIdAndSalesOrderLineIdAndLocationIdAndBatchId(
                anyString(), anyString(), anyString(), any())).thenReturn(List.of());
        when(outboundShipmentLinesRepository.sumShippedForSoLine(anyString(), anyString())).thenReturn(null);
        when(productRepository.findById("prod-missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Product not found");
    }

    @Test
    @DisplayName("should_ThrowNotFound_When_LocationNotFound")
    void should_ThrowNotFound_When_LocationNotFound() {
        OutboundShipmentLinesRequest request = OutboundShipmentLinesRequest.builder()
                .outboundShipmentId(SHIPMENT_ID)
                .salesOrderLineId(SALES_ORDER_LINE_ID)
                .productId(PRODUCT_ID)
                .locationId("loc-missing")
                .batchId(null)
                .quantityShipped(new BigDecimal("5.00"))
                .build();

        OutboundShipments shipment = buildShipment(SHIPMENT_ID, OutboundShipmentsStatus.DRAFT);
        SalesOrderLines soLine = buildSalesOrderLine(SALES_ORDER_LINE_ID, PRODUCT_ID, "10.00", "0.00");
        Products product = buildProduct(PRODUCT_ID);

        when(outboundShipmentsRepository.findByIdWithLock(SHIPMENT_ID)).thenReturn(Optional.of(shipment));
        when(salesOrderLinesRepository.findById(SALES_ORDER_LINE_ID)).thenReturn(Optional.of(soLine));
        when(outboundShipmentLinesRepository.findByOutboundShipmentIdAndSalesOrderLineIdAndLocationIdAndBatchId(
                anyString(), anyString(), anyString(), any())).thenReturn(List.of());
        when(outboundShipmentLinesRepository.sumShippedForSoLine(anyString(), anyString())).thenReturn(null);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(locationRepository.findById("loc-missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Location not found");
    }

    @Test
    @DisplayName("should_ThrowBadRequest_When_LocationDoesNotBelongToWarehouse")
    void should_ThrowBadRequest_When_LocationDoesNotBelongToWarehouse() {
        OutboundShipmentLinesRequest request = OutboundShipmentLinesRequest.builder()
                .outboundShipmentId(SHIPMENT_ID)
                .salesOrderLineId(SALES_ORDER_LINE_ID)
                .productId(PRODUCT_ID)
                .locationId("loc-other-wh")
                .batchId(null)
                .quantityShipped(new BigDecimal("5.00"))
                .build();

        OutboundShipments shipment = buildShipment(SHIPMENT_ID, OutboundShipmentsStatus.DRAFT);
        SalesOrderLines soLine = buildSalesOrderLine(SALES_ORDER_LINE_ID, PRODUCT_ID, "10.00", "0.00");
        Products product = buildProduct(PRODUCT_ID);
        Locations location = buildLocationWithDifferentWarehouse("loc-other-wh");

        when(outboundShipmentsRepository.findByIdWithLock(SHIPMENT_ID)).thenReturn(Optional.of(shipment));
        when(salesOrderLinesRepository.findById(SALES_ORDER_LINE_ID)).thenReturn(Optional.of(soLine));
        when(outboundShipmentLinesRepository.findByOutboundShipmentIdAndSalesOrderLineIdAndLocationIdAndBatchId(
                anyString(), anyString(), anyString(), any())).thenReturn(List.of());
        when(outboundShipmentLinesRepository.sumShippedForSoLine(anyString(), anyString())).thenReturn(null);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(locationRepository.findById("loc-other-wh")).thenReturn(Optional.of(location));

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Location does not belong to the shipment's warehouse");
    }

    @Test
    @DisplayName("should_ThrowNotFound_When_BatchNotFound")
    void should_ThrowNotFound_When_BatchNotFound() {
        OutboundShipmentLinesRequest request = OutboundShipmentLinesRequest.builder()
                .outboundShipmentId(SHIPMENT_ID)
                .salesOrderLineId(SALES_ORDER_LINE_ID)
                .productId(PRODUCT_ID)
                .locationId(LOCATION_ID)
                .batchId("batch-missing")
                .quantityShipped(new BigDecimal("5.00"))
                .build();

        OutboundShipments shipment = buildShipment(SHIPMENT_ID, OutboundShipmentsStatus.DRAFT);
        SalesOrderLines soLine = buildSalesOrderLine(SALES_ORDER_LINE_ID, PRODUCT_ID, "10.00", "0.00");
        Products product = buildProduct(PRODUCT_ID);
        Locations location = buildLocation(LOCATION_ID);

        when(outboundShipmentsRepository.findByIdWithLock(SHIPMENT_ID)).thenReturn(Optional.of(shipment));
        when(salesOrderLinesRepository.findById(SALES_ORDER_LINE_ID)).thenReturn(Optional.of(soLine));
        when(outboundShipmentLinesRepository.findByOutboundShipmentIdAndSalesOrderLineIdAndLocationIdAndBatchId(
                anyString(), anyString(), anyString(), anyString())).thenReturn(List.of());
        when(outboundShipmentLinesRepository.sumShippedForSoLine(anyString(), anyString())).thenReturn(null);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(locationRepository.findById(LOCATION_ID)).thenReturn(Optional.of(location));
        when(batchRepository.findById("batch-missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Batch not found");
    }

    // ==================== GET BY SHIPMENT ID ====================

    @Test
    @DisplayName("should_ReturnLines_When_ShipmentHasLines")
    void should_ReturnLines_When_ShipmentHasLines() {
        OutboundShipmentLines line = buildLine(LINE_ID, SHIPMENT_ID);
        Locations location = buildLocation(LOCATION_ID);
        OutboundShipmentLinesResponse response = buildResponse(LINE_ID, SHIPMENT_ID);

        when(outboundShipmentLinesRepository.findByOutboundShipmentId(SHIPMENT_ID)).thenReturn(List.of(line));
        lenient().when(locationRepository.findAllById(any())).thenReturn(List.of(location));
        when(outboundShipmentLinesMapper.toResponse(any(OutboundShipmentLines.class))).thenReturn(response);

        var result = service.getByShipmentId(SHIPMENT_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(LINE_ID);
    }

    @Test
    @DisplayName("should_ReturnEmptyList_When_ShipmentHasNoLines")
    void should_ReturnEmptyList_When_ShipmentHasNoLines() {
        when(outboundShipmentLinesRepository.findByOutboundShipmentId(SHIPMENT_ID)).thenReturn(List.of());

        var result = service.getByShipmentId(SHIPMENT_ID);

        assertThat(result).isEmpty();
    }

    // ==================== GET BY ID ====================

    @Test
    @DisplayName("should_ReturnLineById_When_Exists")
    void should_ReturnLineById_When_Exists() {
        OutboundShipmentLines line = buildLine(LINE_ID, SHIPMENT_ID);
        Products product = buildProduct(PRODUCT_ID);
        Locations location = buildLocation(LOCATION_ID);
        OutboundShipmentLinesResponse response = buildResponse(LINE_ID, SHIPMENT_ID);

        when(outboundShipmentLinesRepository.findById(LINE_ID)).thenReturn(Optional.of(line));
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(locationRepository.findById(LOCATION_ID)).thenReturn(Optional.of(location));
        when(outboundShipmentLinesMapper.toResponse(line)).thenReturn(response);

        OutboundShipmentLinesResponse result = service.getById(LINE_ID);

        assertThat(result.getId()).isEqualTo(LINE_ID);
    }

    @Test
    @DisplayName("should_ReturnLineById_When_LocationIsNull")
    void should_ReturnLineById_When_LocationIsNull() {
        OutboundShipmentLines line = buildLine(LINE_ID, SHIPMENT_ID);
        line.setLocationId(null);
        Products product = buildProduct(PRODUCT_ID);
        OutboundShipmentLinesResponse response = buildResponse(LINE_ID, SHIPMENT_ID);

        when(outboundShipmentLinesRepository.findById(LINE_ID)).thenReturn(Optional.of(line));
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(outboundShipmentLinesMapper.toResponse(line)).thenReturn(response);

        OutboundShipmentLinesResponse result = service.getById(LINE_ID);

        assertThat(result.getId()).isEqualTo(LINE_ID);
        verify(locationRepository, never()).findById(anyString());
    }

    @Test
    @DisplayName("should_ThrowNotFound_When_GetByIdNotExists")
    void should_ThrowNotFound_When_GetByIdNotExists() {
        when(outboundShipmentLinesRepository.findById("line-missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById("line-missing"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Outbound shipment line not found");
    }

    // ==================== UPDATE ====================

    @Test
    @DisplayName("should_UpdateLine_When_ShipmentIsDraftAndRequestIsValid")
    void should_UpdateLine_When_ShipmentIsDraftAndRequestIsValid() {
        OutboundShipmentLines line = buildLine(LINE_ID, SHIPMENT_ID);
        line.setSalesOrderLineId(SALES_ORDER_LINE_ID);
        line.setQuantityShipped(new BigDecimal("5.00"));
        UpdateOutboundShipmentLinesRequest request = UpdateOutboundShipmentLinesRequest.builder()
                .quantityShipped(new BigDecimal("8.00"))
                .locationId(LOCATION_ID)
                .build();

        OutboundShipments shipment = buildShipment(SHIPMENT_ID, OutboundShipmentsStatus.DRAFT);
        SalesOrderLines soLine = buildSalesOrderLine(SALES_ORDER_LINE_ID, PRODUCT_ID, "10.00", "0.00");
        Locations location = buildLocation(LOCATION_ID);
        OutboundShipmentLinesResponse response = buildResponse(LINE_ID, SHIPMENT_ID);
        Products product = buildProduct(PRODUCT_ID);

        when(outboundShipmentLinesRepository.findById(LINE_ID)).thenReturn(Optional.of(line));
        when(outboundShipmentsRepository.findByIdWithLock(SHIPMENT_ID)).thenReturn(Optional.of(shipment));
        when(salesOrderLinesRepository.findById(SALES_ORDER_LINE_ID)).thenReturn(Optional.of(soLine));
        when(outboundShipmentLinesRepository.sumShippedForSoLine(anyString(), anyString()))
                .thenReturn(new BigDecimal("5.00"));
        when(locationRepository.findById(LOCATION_ID)).thenReturn(Optional.of(location));
        doNothing().when(outboundShipmentLinesMapper).updateEntity(any(), any());
        when(outboundShipmentLinesRepository.save(any(OutboundShipmentLines.class))).thenReturn(line);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(outboundShipmentLinesMapper.toResponse(any(OutboundShipmentLines.class))).thenReturn(response);

        OutboundShipmentLinesResponse result = service.update(LINE_ID, request);

        assertThat(result.getId()).isEqualTo(LINE_ID);
        verify(outboundShipmentLinesMapper).updateEntity(line, request);
    }

    @Test
    @DisplayName("should_ThrowBadRequest_When_UpdateLineOnNonDraftShipment")
    void should_ThrowBadRequest_When_UpdateLineOnNonDraftShipment() {
        OutboundShipmentLines line = buildLine(LINE_ID, SHIPMENT_ID);
        UpdateOutboundShipmentLinesRequest request = UpdateOutboundShipmentLinesRequest.builder()
                .quantityShipped(new BigDecimal("8.00"))
                .build();

        OutboundShipments shipment = buildShipment(SHIPMENT_ID, OutboundShipmentsStatus.PICKING);

        when(outboundShipmentLinesRepository.findById(LINE_ID)).thenReturn(Optional.of(line));
        when(outboundShipmentsRepository.findByIdWithLock(SHIPMENT_ID)).thenReturn(Optional.of(shipment));

        assertThatThrownBy(() -> service.update(LINE_ID, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Only DRAFT shipments can have lines updated");
    }

    @Test
    @DisplayName("should_ThrowBadRequest_When_UpdateQuantityExceedsOrdered")
    void should_ThrowBadRequest_When_UpdateQuantityExceedsOrdered() {
        OutboundShipmentLines line = buildLine(LINE_ID, SHIPMENT_ID);
        line.setSalesOrderLineId(SALES_ORDER_LINE_ID);
        line.setQuantityShipped(new BigDecimal("2.00"));
        UpdateOutboundShipmentLinesRequest request = UpdateOutboundShipmentLinesRequest.builder()
                .quantityShipped(new BigDecimal("15.00"))
                .build();

        OutboundShipments shipment = buildShipment(SHIPMENT_ID, OutboundShipmentsStatus.DRAFT);
        SalesOrderLines soLine = buildSalesOrderLine(SALES_ORDER_LINE_ID, PRODUCT_ID, "10.00", "0.00");

        when(outboundShipmentLinesRepository.findById(LINE_ID)).thenReturn(Optional.of(line));
        when(outboundShipmentsRepository.findByIdWithLock(SHIPMENT_ID)).thenReturn(Optional.of(shipment));
        when(salesOrderLinesRepository.findById(SALES_ORDER_LINE_ID)).thenReturn(Optional.of(soLine));
        when(outboundShipmentLinesRepository.sumShippedForSoLine(anyString(), anyString()))
                .thenReturn(new BigDecimal("2.00"));

        assertThatThrownBy(() -> service.update(LINE_ID, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("exceeds ordered quantity");
    }

    @Test
    @DisplayName("should_ThrowBadRequest_When_UpdateLocationNotInWarehouse")
    void should_ThrowBadRequest_When_UpdateLocationNotInWarehouse() {
        OutboundShipmentLines line = buildLine(LINE_ID, SHIPMENT_ID);
        line.setSalesOrderLineId(SALES_ORDER_LINE_ID);
        UpdateOutboundShipmentLinesRequest request = UpdateOutboundShipmentLinesRequest.builder()
                .locationId("loc-other-wh")
                .build();

        OutboundShipments shipment = buildShipment(SHIPMENT_ID, OutboundShipmentsStatus.DRAFT);
        Locations location = buildLocationWithDifferentWarehouse("loc-other-wh");

        when(outboundShipmentLinesRepository.findById(LINE_ID)).thenReturn(Optional.of(line));
        when(outboundShipmentsRepository.findByIdWithLock(SHIPMENT_ID)).thenReturn(Optional.of(shipment));
        when(locationRepository.findById("loc-other-wh")).thenReturn(Optional.of(location));

        assertThatThrownBy(() -> service.update(LINE_ID, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Location does not belong to the shipment's warehouse");
    }

    // ==================== REMOVE ====================

    @Test
    @DisplayName("should_RemoveLine_When_ShipmentIsDraft")
    void should_RemoveLine_When_ShipmentIsDraft() {
        OutboundShipmentLines line = buildLine(LINE_ID, SHIPMENT_ID);
        OutboundShipments shipment = buildShipment(SHIPMENT_ID, OutboundShipmentsStatus.DRAFT);

        when(outboundShipmentLinesRepository.findById(LINE_ID)).thenReturn(Optional.of(line));
        when(outboundShipmentsRepository.findByIdWithLock(SHIPMENT_ID)).thenReturn(Optional.of(shipment));
        doNothing().when(outboundShipmentLinesRepository).delete(line);

        service.remove(LINE_ID);

        verify(outboundShipmentLinesRepository).delete(line);
    }

    @Test
    @DisplayName("should_ThrowBadRequest_When_RemoveLineFromNonDraftShipment")
    void should_ThrowBadRequest_When_RemoveLineFromNonDraftShipment() {
        OutboundShipmentLines line = buildLine(LINE_ID, SHIPMENT_ID);
        OutboundShipments shipment = buildShipment(SHIPMENT_ID, OutboundShipmentsStatus.PICKING);

        when(outboundShipmentLinesRepository.findById(LINE_ID)).thenReturn(Optional.of(line));
        when(outboundShipmentsRepository.findByIdWithLock(SHIPMENT_ID)).thenReturn(Optional.of(shipment));

        assertThatThrownBy(() -> service.remove(LINE_ID))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Only DRAFT shipments can have lines removed");
    }

    @Test
    @DisplayName("should_ThrowNotFound_When_RemoveLineNotFound")
    void should_ThrowNotFound_When_RemoveLineNotFound() {
        when(outboundShipmentLinesRepository.findById("line-missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.remove("line-missing"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Outbound shipment line not found");
    }

    // ==================== HELPER METHODS ====================

    private OutboundShipments buildShipment(String id, OutboundShipmentsStatus status) {
        OutboundShipments entity = OutboundShipments.builder()
                .shipmentNumber("SHIP-001")
                .salesOrderId("so-001")
                .warehouseId(WAREHOUSE_ID)
                .shipmentDate(java.time.LocalDate.of(2026, 3, 22))
                .status(status)
                .carrier("GHN")
                .build();
        if (id != null) entity.setId(id);
        entity.setCreatedAt(LocalDateTime.of(2026, 3, 22, 10, 0, 0));
        entity.setUpdatedAt(LocalDateTime.of(2026, 3, 22, 11, 0, 0));
        return entity;
    }

    private OutboundShipmentLines buildLine(String id, String shipmentId) {
        OutboundShipmentLines line = OutboundShipmentLines.builder()
                .outboundShipmentId(shipmentId)
                .salesOrderLineId(SALES_ORDER_LINE_ID)
                .productId(PRODUCT_ID)
                .locationId(LOCATION_ID)
                .batchId(null)
                .lineNumber(1)
                .quantityShipped(new BigDecimal("5.00"))
                .build();
        if (id != null) line.setId(id);
        line.setCreatedAt(LocalDateTime.of(2026, 3, 22, 10, 0, 0));
        line.setUpdatedAt(LocalDateTime.of(2026, 3, 22, 11, 0, 0));
        return line;
    }

    private SalesOrderLines buildSalesOrderLine(String id, String productId, String qtyOrdered, String qtyShipped) {
        SalesOrderLines line = SalesOrderLines.builder()
                .salesOrderId("so-001")
                .productId(productId)
                .lineNumber(1)
                .quantityOrdered(new BigDecimal(qtyOrdered))
                .quantityShipped(new BigDecimal(qtyShipped))
                .unitPrice(new BigDecimal("100.00"))
                .lineTotal(new BigDecimal("1000.00"))
                .build();
        if (id != null) line.setId(id);
        line.setCreatedAt(LocalDateTime.of(2026, 3, 20, 10, 0, 0));
        line.setUpdatedAt(LocalDateTime.of(2026, 3, 20, 11, 0, 0));
        return line;
    }

    private Products buildProduct(String id) {
        Products product = Products.builder()
                .sku("SKU-001")
                .name("Product A")
                .status(ProductStatus.ACTIVE)
                .categoryId("cat-1")
                .uomId("uom-1")
                .build();
        product.setId(id);
        product.setCreatedAt(LocalDateTime.of(2026, 3, 1, 10, 0, 0));
        product.setUpdatedAt(LocalDateTime.of(2026, 3, 1, 10, 0, 0));
        return product;
    }

    private Locations buildLocation(String id) {
        Locations location = Locations.builder()
                .warehouseId(WAREHOUSE_ID)
                .code("LOC-001")
                .name("Location A")
                .type(LocationType.PICKING)
                .status(LocationStatus.ACTIVE)
                .build();
        location.setId(id);
        location.setCreatedAt(LocalDateTime.of(2026, 3, 1, 10, 0, 0));
        location.setUpdatedAt(LocalDateTime.of(2026, 3, 1, 10, 0, 0));
        return location;
    }

    private Locations buildLocationWithDifferentWarehouse(String id) {
        Locations location = Locations.builder()
                .warehouseId("wh-other")
                .code("LOC-002")
                .name("Location B")
                .type(LocationType.PICKING)
                .status(LocationStatus.ACTIVE)
                .build();
        location.setId(id);
        location.setCreatedAt(LocalDateTime.of(2026, 3, 1, 10, 0, 0));
        location.setUpdatedAt(LocalDateTime.of(2026, 3, 1, 10, 0, 0));
        return location;
    }

    private Batch buildBatch(String id) {
        Batch batch = Batch.builder()
                .batchNumber("BATCH-001")
                .productId(PRODUCT_ID)
                .manufacturingDate(java.time.LocalDate.of(2026, 1, 1))
                .status(BatchStatus.AVAILABLE)
                .build();
        batch.setId(id);
        batch.setCreatedAt(LocalDateTime.of(2026, 3, 1, 10, 0, 0));
        batch.setUpdatedAt(LocalDateTime.of(2026, 3, 1, 10, 0, 0));
        return batch;
    }

    private OutboundShipmentLinesResponse buildResponse(String id, String shipmentId) {
        return OutboundShipmentLinesResponse.builder()
                .id(id)
                .outboundShipmentId(shipmentId)
                .salesOrderLineId(SALES_ORDER_LINE_ID)
                .productId(PRODUCT_ID)
                .sku("SKU-001")
                .productName("Product A")
                .locationId(LOCATION_ID)
                .locationName("Location A")
                .batchId(null)
                .batchNumber(null)
                .lineNumber(1)
                .quantityShipped(new BigDecimal("5.00"))
                .createdAt(LocalDateTime.of(2026, 3, 22, 10, 0, 0))
                .updatedAt(LocalDateTime.of(2026, 3, 22, 11, 0, 0))
                .build();
    }
}
