package org.demo.whs.service.impl;

import org.demo.whs.entity.*;
import org.demo.whs.entity.dto.request.Inventory.InventoryReserveRequest;
import org.demo.whs.entity.dto.request.Inventory.InventoryUnreserveRequest;
import org.demo.whs.entity.dto.request.SalesOrders.SalesOrdersFilterRequest;
import org.demo.whs.entity.dto.request.SalesOrders.SalesOrdersRequest;
import org.demo.whs.entity.dto.request.SalesOrderLines.SalesOrderLinesRequest;
import org.demo.whs.entity.dto.request.SalesOrders.UpdateSalesOrdersRequest;
import org.demo.whs.entity.dto.response.Inventory.InventoryReserveResponse;
import org.demo.whs.entity.dto.response.SalesOrders.SalesOrdersResponse;
import org.demo.whs.entity.enums.*;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.NotFoundException;
import org.demo.whs.mapper.SalesOrderLinesMapper;
import org.demo.whs.mapper.SalesOrdersMapper;
import org.demo.whs.repository.*;
import org.demo.whs.service.InventoryService;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SalesOrdersServiceImpl Unit Tests")
class SalesOrdersServiceImplTest {

    private static final String USERNAME = "so-tester";
    private static final String ACTOR_ID = "acc-so-001";

    @Mock private SalesOrdersRepository salesOrdersRepository;
    @Mock private SalesOrderLinesRepository salesOrderLinesRepository;
    @Mock private BusinessPartnersRepository businessPartnersRepository;
    @Mock private WareHouseRepository wareHouseRepository;
    @Mock private ProductRepository productRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private OutboundShipmentsRepository outboundShipmentsRepository;
    @Mock private InventoryReservationRepository inventoryReservationRepository;
    @Mock private InventoryService inventoryService;
    @Mock private SalesOrdersMapper salesOrdersMapper;
    @Mock private SalesOrderLinesMapper salesOrderLinesMapper;
    @Mock private IdentifierGenerator identifierGenerator;

    private SalesOrdersServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SalesOrdersServiceImpl(
                salesOrdersRepository, salesOrderLinesRepository, businessPartnersRepository,
                wareHouseRepository, productRepository, accountRepository, outboundShipmentsRepository,
                inventoryReservationRepository, inventoryService, salesOrdersMapper,
                salesOrderLinesMapper, identifierGenerator
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

    @Test
    @DisplayName("should_CreateDraftSalesOrder_When_RequestIsValid")
    void should_CreateDraftSalesOrder_When_RequestIsValid() {
        SalesOrderLinesRequest lineReq = SalesOrderLinesRequest.builder()
                .productId("prod-1").quantityOrdered(new BigDecimal("5.00"))
                .unitPrice(new BigDecimal("100.00")).notes("Line note").build();
        SalesOrdersRequest request = SalesOrdersRequest.builder()
                .customerId("bp-1").warehouseId("wh-1")
                .orderDate(LocalDate.of(2026, 3, 20))
                .requestedDeliveryDate(LocalDate.of(2026, 3, 25))
                .currency("VND").lines(List.of(lineReq)).build();

        when(businessPartnersRepository.findById("bp-1")).thenReturn(Optional.of(buildCustomer("bp-1", BusinessPartnerStatus.ACTIVE, BusinessPartnerType.CUSTOMER)));
        when(wareHouseRepository.findById("wh-1")).thenReturn(Optional.of(buildWarehouse("wh-1", WareHouseStatus.ACTIVE)));
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(buildProduct("prod-1", ProductStatus.ACTIVE)));
        when(identifierGenerator.generate(anyString(), anyInt(), any())).thenReturn("SO-001");
        when(salesOrdersMapper.toEntity(request)).thenReturn(buildSalesOrder(null, null, SalesOrdersStatus.DRAFT));
        when(salesOrdersRepository.save(any(SalesOrders.class))).thenAnswer(inv -> {
            SalesOrders e = inv.getArgument(0);
            e.setId("so-1"); e.setCreatedAt(LocalDateTime.now()); e.setUpdatedAt(LocalDateTime.now());
            return e;
        });
        when(salesOrderLinesMapper.toEntity(lineReq)).thenReturn(
                SalesOrderLines.builder().productId("prod-1").quantityOrdered(new BigDecimal("5.00"))
                        .unitPrice(new BigDecimal("100.00")).lineTotal(new BigDecimal("500.00")).build()
        );
        when(salesOrderLinesRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(salesOrdersMapper.toResponse(any(SalesOrders.class))).thenReturn(
                SalesOrdersResponse.builder().id("so-1").soNumber("SO-001").status("DRAFT")
                        .customerId("bp-1").warehouseId("wh-1").subTotal(new BigDecimal("500.00"))
                        .taxAmount(BigDecimal.ZERO).totalAmount(new BigDecimal("500.00")).currency("VND")
                        .lines(List.of()).build()
        );
        when(salesOrderLinesMapper.toResponse(any(SalesOrderLines.class))).thenReturn(
                org.demo.whs.entity.dto.response.SalesOrderLines.SalesOrderLinesResponse.builder()
                        .id("line-1").productId("prod-1").lineNumber(1)
                        .quantityOrdered(new BigDecimal("5.00")).unitPrice(new BigDecimal("100.00"))
                        .lineTotal(new BigDecimal("500.00")).build()
        );

        SalesOrdersResponse response = service.create(request);

        assertThat(response.getId()).isEqualTo("so-1");
        assertThat(response.getStatus()).isEqualTo("DRAFT");
        verify(salesOrderLinesRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("should_ThrowNotFound_When_CustomerNotFound")
    void should_ThrowNotFound_When_CustomerNotFound() {
        SalesOrdersRequest request = SalesOrdersRequest.builder()
                .customerId("bp-missing").warehouseId("wh-1")
                .orderDate(LocalDate.of(2026, 3, 20))
                .requestedDeliveryDate(LocalDate.of(2026, 3, 25))
                .currency("VND").lines(List.of()).build();

        when(businessPartnersRepository.findById("bp-missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Customer not found");
    }

    @Test
    @DisplayName("should_ThrowBadRequest_When_CustomerIsNotActive")
    void should_ThrowBadRequest_When_CustomerIsNotActive() {
        BusinessPartners customer = buildCustomer("bp-1", BusinessPartnerStatus.INACTIVE, BusinessPartnerType.CUSTOMER);
        SalesOrdersRequest request = SalesOrdersRequest.builder()
                .customerId("bp-1").warehouseId("wh-1")
                .orderDate(LocalDate.of(2026, 3, 20))
                .requestedDeliveryDate(LocalDate.of(2026, 3, 25))
                .currency("VND").lines(List.of()).build();

        when(businessPartnersRepository.findById("bp-1")).thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not active");
    }

    @Test
    @DisplayName("should_ThrowBadRequest_When_WarehouseNotActive")
    void should_ThrowBadRequest_When_WarehouseNotActive() {
        when(businessPartnersRepository.findById("bp-1")).thenReturn(Optional.of(
                buildCustomer("bp-1", BusinessPartnerStatus.ACTIVE, BusinessPartnerType.CUSTOMER)));
        when(wareHouseRepository.findById("wh-1")).thenReturn(Optional.of(
                buildWarehouse("wh-1", WareHouseStatus.INACTIVE)));

        SalesOrdersRequest request = SalesOrdersRequest.builder()
                .customerId("bp-1").warehouseId("wh-1")
                .orderDate(LocalDate.of(2026, 3, 20))
                .requestedDeliveryDate(LocalDate.of(2026, 3, 25))
                .currency("VND").lines(List.of()).build();

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Warehouse is not active");
    }

    @Test
    @DisplayName("should_ThrowBadRequest_When_LineQuantityIsInvalid")
    void should_ThrowBadRequest_When_LineQuantityIsInvalid() {
        SalesOrderLinesRequest lineReq = SalesOrderLinesRequest.builder()
                .productId("prod-1").quantityOrdered(BigDecimal.ZERO)
                .unitPrice(new BigDecimal("100.00")).build();
        SalesOrdersRequest request = SalesOrdersRequest.builder()
                .customerId("bp-1").warehouseId("wh-1")
                .orderDate(LocalDate.of(2026, 3, 20))
                .requestedDeliveryDate(LocalDate.of(2026, 3, 25))
                .currency("VND").lines(List.of(lineReq)).build();

        when(businessPartnersRepository.findById("bp-1")).thenReturn(Optional.of(
                buildCustomer("bp-1", BusinessPartnerStatus.ACTIVE, BusinessPartnerType.CUSTOMER)));
        when(wareHouseRepository.findById("wh-1")).thenReturn(Optional.of(
                buildWarehouse("wh-1", WareHouseStatus.ACTIVE)));
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(
                buildProduct("prod-1", ProductStatus.ACTIVE)));
        when(salesOrdersMapper.toEntity(request)).thenReturn(buildSalesOrder(null, null, SalesOrdersStatus.DRAFT));
        when(identifierGenerator.generate(anyString(), anyInt(), any())).thenReturn("SO-001");

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("greater than zero");
    }

    @Test
    @DisplayName("should_ReturnSalesOrderById_When_Exists")
    void should_ReturnSalesOrderById_When_Exists() {
        SalesOrders entity = buildSalesOrder("so-1", "SO-001", SalesOrdersStatus.DRAFT);
        SalesOrderLines line = buildSalesOrderLine("line-1", "so-1", 1, "5.00", "100.00", "500.00");

        when(salesOrdersRepository.findById("so-1")).thenReturn(Optional.of(entity));
        when(salesOrderLinesRepository.findBySalesOrderId("so-1")).thenReturn(List.of(line));
        when(salesOrdersMapper.toResponse(entity)).thenReturn(
                SalesOrdersResponse.builder().id("so-1").soNumber("SO-001").status("DRAFT").build()
        );
        when(salesOrderLinesMapper.toResponse(line)).thenReturn(
                org.demo.whs.entity.dto.response.SalesOrderLines.SalesOrderLinesResponse.builder()
                        .id("line-1").salesOrderId("so-1").lineNumber(1)
                        .quantityOrdered(new BigDecimal("5.00")).unitPrice(new BigDecimal("100.00"))
                        .lineTotal(new BigDecimal("500.00")).build()
        );

        SalesOrdersResponse response = service.getById("so-1");

        assertThat(response.getId()).isEqualTo("so-1");
        assertThat(response.getStatus()).isEqualTo("DRAFT");
    }

    @Test
    @DisplayName("should_ThrowNotFound_When_GetByIdNotExists")
    void should_ThrowNotFound_When_GetByIdNotExists() {
        when(salesOrdersRepository.findById("so-missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById("so-missing"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Sales order not found");
    }

    @Test
    @DisplayName("should_UpdateDraftSalesOrder_When_RequestIsValid")
    void should_UpdateDraftSalesOrder_When_RequestIsValid() {
        SalesOrders existing = buildSalesOrder("so-1", "SO-001", SalesOrdersStatus.DRAFT);
        UpdateSalesOrdersRequest request = UpdateSalesOrdersRequest.builder()
                .notes("Updated notes").currency("USD").build();

        when(salesOrdersRepository.findByIdForUpdate("so-1")).thenReturn(Optional.of(existing));
        doNothing().when(salesOrdersMapper).updateEntity(any(), any());
        when(salesOrdersRepository.save(any(SalesOrders.class))).thenReturn(existing);
        when(salesOrdersMapper.toResponse(existing)).thenReturn(
                SalesOrdersResponse.builder().id("so-1").status("DRAFT")
                        .notes("Updated notes").currency("USD").build()
        );

        SalesOrdersResponse response = service.update("so-1", request);

        assertThat(response.getNotes()).isEqualTo("Updated notes");
        verify(salesOrdersMapper).updateEntity(existing, request);
    }

    @Test
    @DisplayName("should_ThrowBadRequest_When_UpdateNonDraftSalesOrder")
    void should_ThrowBadRequest_When_UpdateNonDraftSalesOrder() {
        SalesOrders confirmed = buildSalesOrder("so-1", "SO-001", SalesOrdersStatus.CONFIRMED);
        UpdateSalesOrdersRequest request = UpdateSalesOrdersRequest.builder().notes("Updated").build();

        when(salesOrdersRepository.findByIdForUpdate("so-1")).thenReturn(Optional.of(confirmed));

        assertThatThrownBy(() -> service.update("so-1", request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Only draft sales orders can be updated");
    }

    @Test
    @DisplayName("should_ConfirmSalesOrder_When_DraftHasValidLines")
    void should_ConfirmSalesOrder_When_DraftHasValidLines() {
        SalesOrders so = buildSalesOrder("so-1", "SO-001", SalesOrdersStatus.DRAFT);
        SalesOrderLines line = buildSalesOrderLine("line-1", "so-1", 1, "5.00", "100.00", "500.00");

        when(salesOrdersRepository.findByIdForUpdate("so-1")).thenReturn(Optional.of(so));
        when(salesOrderLinesRepository.findBySalesOrderId("so-1")).thenReturn(List.of(line));
        when(inventoryService.reserve(any(InventoryReserveRequest.class))).thenReturn(
                InventoryReserveResponse.builder().inventoryId("inv-1").status("RESERVED").build()
        );
        when(salesOrdersRepository.save(any(SalesOrders.class))).thenAnswer(inv -> inv.getArgument(0));
        when(salesOrderLinesMapper.toResponse(line)).thenReturn(
                org.demo.whs.entity.dto.response.SalesOrderLines.SalesOrderLinesResponse.builder()
                        .id("line-1").lineNumber(1).quantityOrdered(new BigDecimal("5.00"))
                        .unitPrice(new BigDecimal("100.00")).lineTotal(new BigDecimal("500.00")).build()
        );
        when(salesOrdersMapper.toResponse(any(SalesOrders.class))).thenReturn(
                SalesOrdersResponse.builder().id("so-1").status("CONFIRMED").build()
        );

        SalesOrdersResponse response = service.confirm("so-1");

        assertThat(so.getStatus()).isEqualTo(SalesOrdersStatus.CONFIRMED);
        assertThat(so.getConfirmedBy()).isEqualTo(ACTOR_ID);
        verify(inventoryService).reserve(any(InventoryReserveRequest.class));
    }

    @Test
    @DisplayName("should_ThrowBadRequest_When_ConfirmNonDraftSalesOrder")
    void should_ThrowBadRequest_When_ConfirmNonDraftSalesOrder() {
        SalesOrders completed = buildSalesOrder("so-1", "SO-001", SalesOrdersStatus.COMPLETED);

        when(salesOrdersRepository.findByIdForUpdate("so-1")).thenReturn(Optional.of(completed));

        assertThatThrownBy(() -> service.confirm("so-1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Only draft sales orders can be confirmed");
    }

    @Test
    @DisplayName("should_ThrowBadRequest_When_ConfirmSalesOrderHasNoLines")
    void should_ThrowBadRequest_When_ConfirmSalesOrderHasNoLines() {
        SalesOrders so = buildSalesOrder("so-1", "SO-001", SalesOrdersStatus.DRAFT);

        when(salesOrdersRepository.findByIdForUpdate("so-1")).thenReturn(Optional.of(so));
        when(salesOrderLinesRepository.findBySalesOrderId("so-1")).thenReturn(List.of());

        assertThatThrownBy(() -> service.confirm("so-1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("at least one line");
    }

    @Test
    @DisplayName("should_CancelDraftSalesOrder_When_DraftExists")
    void should_CancelDraftSalesOrder_When_DraftExists() {
        SalesOrders so = buildSalesOrder("so-1", "SO-001", SalesOrdersStatus.DRAFT);

        when(salesOrdersRepository.findById("so-1")).thenReturn(Optional.of(so));
        when(salesOrdersRepository.findByIdForUpdate("so-1")).thenReturn(Optional.of(so));
        when(salesOrderLinesRepository.findBySalesOrderId("so-1")).thenReturn(List.of());
        when(salesOrdersRepository.save(any(SalesOrders.class))).thenAnswer(inv -> inv.getArgument(0));
        when(salesOrdersMapper.toResponse(any(SalesOrders.class))).thenReturn(
                SalesOrdersResponse.builder().id("so-1").status("CANCELLED").build()
        );

        SalesOrdersResponse response = service.cancel("so-1");

        assertThat(so.getStatus()).isEqualTo(SalesOrdersStatus.CANCELLED);
    }

    @Test
    @DisplayName("should_CancelConfirmedSalesOrder_AndUnreserveInventory")
    void should_CancelConfirmedSalesOrder_AndUnreserveInventory() {
        SalesOrders so = buildSalesOrder("so-1", "SO-001", SalesOrdersStatus.CONFIRMED);
        SalesOrderLines line = buildSalesOrderLine("line-1", "so-1", 1, "5.00", "100.00", "500.00");
        InventoryReservation reservation = InventoryReservation.builder()
                .id("res-1").inventoryId("inv-1").orderLineId("line-1")
                .quantity(new BigDecimal("5.00")).status(InventoryReservationStatus.RESERVED).build();

        when(salesOrdersRepository.findById("so-1")).thenReturn(Optional.of(so));
        when(salesOrdersRepository.findByIdForUpdate("so-1")).thenReturn(Optional.of(so));
        when(outboundShipmentsRepository.findBySalesOrderId("so-1")).thenReturn(List.of());
        when(salesOrderLinesRepository.findBySalesOrderId("so-1")).thenReturn(List.of(line));
        when(inventoryReservationRepository.findByOrderLineId("line-1")).thenReturn(Optional.of(reservation));
        when(salesOrdersRepository.save(any(SalesOrders.class))).thenAnswer(inv -> inv.getArgument(0));
        when(salesOrdersMapper.toResponse(any(SalesOrders.class))).thenReturn(
                SalesOrdersResponse.builder().id("so-1").status("CANCELLED").build()
        );

        service.cancel("so-1");

        assertThat(so.getStatus()).isEqualTo(SalesOrdersStatus.CANCELLED);
        verify(inventoryService).unreserve(any(InventoryUnreserveRequest.class));
    }

    @Test
    @DisplayName("should_ThrowBadRequest_When_CancelSalesOrderWithActiveShipments")
    void should_ThrowBadRequest_When_CancelSalesOrderWithActiveShipments() {
        SalesOrders so = buildSalesOrder("so-1", "SO-001", SalesOrdersStatus.CONFIRMED);
        OutboundShipments shipment = OutboundShipments.builder()
                .salesOrderId("so-1").status(OutboundShipmentsStatus.PICKING).build();
        shipment.setId("ship-1");

        when(salesOrdersRepository.findByIdForUpdate("so-1")).thenReturn(Optional.of(so));
        when(outboundShipmentsRepository.findBySalesOrderId("so-1")).thenReturn(List.of(shipment));

        assertThatThrownBy(() -> service.cancel("so-1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("active shipments");
    }

    @Test
    @DisplayName("should_ReturnFilteredSalesOrders_When_GetAllCalled")
    void should_ReturnFilteredSalesOrders_When_GetAllCalled() {
        SalesOrdersFilterRequest filter = SalesOrdersFilterRequest.builder()
                .soNumber("SO-001").status("DRAFT").build();
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "updatedAt"));
        SalesOrders entity = buildSalesOrder("so-1", "SO-001", SalesOrdersStatus.DRAFT);

        when(salesOrdersRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(entity), pageable, 1));
        when(salesOrdersMapper.toResponse(entity))
                .thenReturn(SalesOrdersResponse.builder().id("so-1").soNumber("SO-001").status("DRAFT").build());

        var response = service.getAll(filter, pageable);

        assertThat(response.getTotalElements()).isEqualTo(1);
        assertThat(response.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("should_ReturnEmptyPage_When_GetAllWithNoResults")
    void should_ReturnEmptyPage_When_GetAllWithNoResults() {
        SalesOrdersFilterRequest filter = SalesOrdersFilterRequest.builder().build();
        Pageable pageable = PageRequest.of(0, 10);

        when(salesOrdersRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        var response = service.getAll(filter, pageable);

        assertThat(response.getTotalElements()).isEqualTo(0);
        assertThat(response.getContent()).isEmpty();
    }

    private SalesOrders buildSalesOrder(String id, String soNumber, SalesOrdersStatus status) {
        SalesOrders entity = SalesOrders.builder()
                .soNumber(soNumber).customerId("bp-1").warehouseId("wh-1")
                .orderDate(LocalDate.of(2026, 3, 20))
                .requestedDeliveryDate(LocalDate.of(2026, 3, 25))
                .status(status).subTotal(new BigDecimal("500.00"))
                .taxAmount(BigDecimal.ZERO).totalAmount(new BigDecimal("500.00"))
                .currency(CurrencyType.VND).build();
        if (id != null) entity.setId(id);
        entity.setCreatedAt(LocalDateTime.of(2026, 3, 20, 10, 0, 0));
        entity.setUpdatedAt(LocalDateTime.of(2026, 3, 20, 11, 0, 0));
        return entity;
    }

    private SalesOrderLines buildSalesOrderLine(String id, String soId, Integer lineNumber,
                                                String qtyOrdered, String unitPrice, String lineTotal) {
        SalesOrderLines line = SalesOrderLines.builder()
                .salesOrderId(soId).productId("prod-1").lineNumber(lineNumber)
                .quantityOrdered(new BigDecimal(qtyOrdered)).quantityShipped(BigDecimal.ZERO)
                .unitPrice(new BigDecimal(unitPrice)).lineTotal(new BigDecimal(lineTotal)).build();
        line.setId(id);
        return line;
    }

    private BusinessPartners buildCustomer(String id, BusinessPartnerStatus status, BusinessPartnerType type) {
        BusinessPartners bp = BusinessPartners.builder()
                .code("BP-001").name("Customer A").type(type).status(status).build();
        bp.setId(id);
        return bp;
    }

    private Warehouses buildWarehouse(String id, WareHouseStatus status) {
        Warehouses wh = Warehouses.builder()
                .code("WH-001").name("Main WH").status(status).build();
        wh.setId(id);
        return wh;
    }

    private Products buildProduct(String id, ProductStatus status) {
        Products p = Products.builder()
                .name("Product A").status(status).sku("SKU-001")
                .categoryId("cat-1").uomId("uom-1").build();
        p.setId(id);
        return p;
    }
}
