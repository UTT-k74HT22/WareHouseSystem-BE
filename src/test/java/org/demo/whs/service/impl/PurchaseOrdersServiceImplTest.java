package org.demo.whs.service.impl;

import org.demo.whs.entity.Account;
import org.demo.whs.entity.BusinessPartners;
import org.demo.whs.entity.PurchaseOrders;
import org.demo.whs.entity.Warehouses;
import org.demo.whs.entity.dto.request.PurchaseOrders.PurchaseOrdersFilterRequest;
import org.demo.whs.entity.dto.request.PurchaseOrders.PurchaseOrdersRequest;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.dto.response.PurchaseOrders.PurchaseOrdersResponse;
import org.demo.whs.entity.enums.AccountStatus;
import org.demo.whs.entity.enums.BusinessPartnerStatus;
import org.demo.whs.entity.enums.BusinessPartnerType;
import org.demo.whs.entity.enums.CurrencyType;
import org.demo.whs.entity.enums.PurchaseOrdersStatus;
import org.demo.whs.entity.enums.WareHouseStatus;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.NotFoundException;
import org.demo.whs.mapper.PurchaseOrdersMapper;
import org.demo.whs.repository.AccountRepository;
import org.demo.whs.repository.BusinessPartnersRepository;
import org.demo.whs.repository.PurchaseOrderLinesRepository;
import org.demo.whs.repository.PurchaseOrdersRepository;
import org.demo.whs.repository.WareHouseRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PurchaseOrdersServiceImpl Unit Tests")
class PurchaseOrdersServiceImplTest {

    private static final String USERNAME = "po-tester";
    private static final String ACTOR_ID = "acc-001";

    @Mock
    private PurchaseOrdersRepository purchaseOrdersRepository;

    @Mock
    private PurchaseOrderLinesRepository purchaseOrderLinesRepository;

    @Mock
    private BusinessPartnersRepository businessPartnersRepository;

    @Mock
    private WareHouseRepository wareHouseRepository;

    @Mock
    private AccountRepository accountRepository;

    private PurchaseOrdersServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PurchaseOrdersServiceImpl(
                purchaseOrdersRepository,
                purchaseOrderLinesRepository,
                businessPartnersRepository,
                wareHouseRepository,
                accountRepository,
                new PurchaseOrdersMapper()
        );

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(USERNAME, "password", List.of())
        );

        Account account = Account.builder()
                .username(USERNAME)
                .password("secret")
                .status(AccountStatus.ACTIVE)
                .build();
        account.setId(ACTOR_ID);

        lenient().when(accountRepository.findByUsername(USERNAME)).thenReturn(Optional.of(account));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("should_CreateDraftPurchaseOrder_When_RequestIsValid")
    void should_CreateDraftPurchaseOrder_When_RequestIsValid() {
        PurchaseOrdersRequest request = PurchaseOrdersRequest.builder()
                .supplierId("sup-001")
                .warehouseId("wh-001")
                .orderDate(LocalDate.of(2026, 3, 7))
                .expectedDeliveryDate(LocalDate.of(2026, 3, 10))
                .currency("USD")
                .paymentTerms("  ")
                .notes("PO draft")
                .build();

        when(businessPartnersRepository.findById("sup-001"))
                .thenReturn(Optional.of(buildSupplier("sup-001", BusinessPartnerType.BOTH, BusinessPartnerStatus.ACTIVE, "NET 30")));
        when(wareHouseRepository.findById("wh-001"))
                .thenReturn(Optional.of(buildWarehouse("wh-001", WareHouseStatus.ACTIVE)));
        when(purchaseOrdersRepository.existsByPurchaseOrderNumber(anyString())).thenReturn(false);
        when(purchaseOrdersRepository.save(any(PurchaseOrders.class))).thenAnswer(invocation -> {
            PurchaseOrders entity = invocation.getArgument(0);
            entity.setId("po-001");
            entity.setCreatedAt(LocalDateTime.of(2026, 3, 7, 10, 0, 0));
            entity.setUpdatedAt(LocalDateTime.of(2026, 3, 7, 10, 0, 0));
            return entity;
        });

        PurchaseOrdersResponse response = service.create(request);

        ArgumentCaptor<PurchaseOrders> entityCaptor = ArgumentCaptor.forClass(PurchaseOrders.class);
        verify(purchaseOrdersRepository).save(entityCaptor.capture());

        PurchaseOrders savedEntity = entityCaptor.getValue();
        assertThat(savedEntity.getStatus()).isEqualTo(PurchaseOrdersStatus.DRAFT);
        assertThat(savedEntity.getCurrency()).isEqualTo(CurrencyType.USD);
        assertThat(savedEntity.getCreatedBy()).isEqualTo(ACTOR_ID);
        assertThat(savedEntity.getUpdatedBy()).isEqualTo(ACTOR_ID);
        assertThat(savedEntity.getSubTotal()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(savedEntity.getTaxAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(savedEntity.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(savedEntity.getPurchaseOrderNumber()).startsWith("PO-");
        assertThat(savedEntity.getPaymentTerms()).isEqualTo("NET 30");
        assertThat(savedEntity.getNotes()).isEqualTo("PO draft");

        assertThat(response.getId()).isEqualTo("po-001");
        assertThat(response.getStatus()).isEqualTo(PurchaseOrdersStatus.DRAFT.name());
        assertThat(response.getPurchaseOrderNumber()).startsWith("PO-");
        assertThat(response.getPaymentTerms()).isEqualTo("NET 30");
    }

    @Test
    @DisplayName("should_ThrowBadRequest_When_ExpectedDeliveryDateIsBeforeOrderDate")
    void should_ThrowBadRequest_When_ExpectedDeliveryDateIsBeforeOrderDate() {
        PurchaseOrdersRequest request = PurchaseOrdersRequest.builder()
                .supplierId("sup-001")
                .warehouseId("wh-001")
                .orderDate(LocalDate.of(2026, 3, 10))
                .expectedDeliveryDate(LocalDate.of(2026, 3, 9))
                .currency("USD")
                .build();

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Expected delivery date must be on or after order date")
                .hasFieldOrPropertyWithValue("errorCode", "COM_001");

        verify(businessPartnersRepository, never()).findById(anyString());
        verify(purchaseOrdersRepository, never()).save(any(PurchaseOrders.class));
    }

    @Test
    @DisplayName("should_UseRequestedPaymentTerms_When_RequestProvidesValue")
    void should_UseRequestedPaymentTerms_When_RequestProvidesValue() {
        PurchaseOrdersRequest request = PurchaseOrdersRequest.builder()
                .supplierId("sup-001")
                .warehouseId("wh-001")
                .orderDate(LocalDate.of(2026, 3, 7))
                .expectedDeliveryDate(LocalDate.of(2026, 3, 10))
                .currency("USD")
                .paymentTerms("COD")
                .notes("  ")
                .build();

        when(businessPartnersRepository.findById("sup-001"))
                .thenReturn(Optional.of(buildSupplier("sup-001", BusinessPartnerType.SUPPLIER, BusinessPartnerStatus.ACTIVE, "NET 30")));
        when(wareHouseRepository.findById("wh-001"))
                .thenReturn(Optional.of(buildWarehouse("wh-001", WareHouseStatus.ACTIVE)));
        when(purchaseOrdersRepository.existsByPurchaseOrderNumber(anyString())).thenReturn(false);
        when(purchaseOrdersRepository.save(any(PurchaseOrders.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PurchaseOrdersResponse response = service.create(request);

        assertThat(response.getPaymentTerms()).isEqualTo("COD");
        assertThat(response.getNotes()).isNull();
    }

    @Test
    @DisplayName("should_ThrowNotFound_When_SupplierIsNotValidForPurchaseOrder")
    void should_ThrowNotFound_When_SupplierIsNotValidForPurchaseOrder() {
        PurchaseOrdersRequest request = PurchaseOrdersRequest.builder()
                .supplierId("bp-customer")
                .warehouseId("wh-001")
                .orderDate(LocalDate.of(2026, 3, 7))
                .currency("USD")
                .build();

        when(businessPartnersRepository.findById("bp-customer"))
                .thenReturn(Optional.of(buildSupplier("bp-customer", BusinessPartnerType.CUSTOMER, BusinessPartnerStatus.ACTIVE, null)));

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("errorCode", "BP_001");

        verify(wareHouseRepository, never()).findById(anyString());
        verify(purchaseOrdersRepository, never()).save(any(PurchaseOrders.class));
    }

    @Test
    @DisplayName("should_ReturnPurchaseOrderById_When_IdExists")
    void should_ReturnPurchaseOrderById_When_IdExists() {
        PurchaseOrders entity = buildPurchaseOrder("po-001", "PO-20260307101010123-ABC123", PurchaseOrdersStatus.CONFIRMED);
        when(purchaseOrdersRepository.findById("po-001")).thenReturn(Optional.of(entity));

        PurchaseOrdersResponse response = service.getById("po-001");

        assertThat(response.getId()).isEqualTo("po-001");
        assertThat(response.getPurchaseOrderNumber()).isEqualTo("PO-20260307101010123-ABC123");
        assertThat(response.getStatus()).isEqualTo(PurchaseOrdersStatus.CONFIRMED.name());
        assertThat(response.getCurrency()).isEqualTo(CurrencyType.USD.name());
    }

    @Test
    @DisplayName("should_ThrowNotFoundException_When_GetByIdWithUnknownId")
    void should_ThrowNotFoundException_When_GetByIdWithUnknownId() {
        when(purchaseOrdersRepository.findById("po-missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById("po-missing"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Purchase order not found")
                .hasFieldOrPropertyWithValue("errorCode", "PO_001");
    }

    @Test
    @DisplayName("should_ReturnFilteredPurchaseOrders_When_GetAllCalled")
    void should_ReturnFilteredPurchaseOrders_When_GetAllCalled() {
        PurchaseOrdersFilterRequest filter = PurchaseOrdersFilterRequest.builder()
                .purchaseOrderNumber("PO-2026")
                .status("draft")
                .supplierId("sup-001")
                .build();
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "updatedAt"));

        PurchaseOrders entity = buildPurchaseOrder("po-001", "PO-20260307101010123-ABC123", PurchaseOrdersStatus.DRAFT);
        when(purchaseOrdersRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(entity), pageable, 1));

        PageResponse<PurchaseOrdersResponse> response = service.getAll(filter, pageable);

        assertThat(filter.getStatus()).isEqualTo(PurchaseOrdersStatus.DRAFT.name());
        assertThat(response.getTotalElements()).isEqualTo(1);
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getId()).isEqualTo("po-001");
        assertThat(response.getContent().get(0).getStatus()).isEqualTo(PurchaseOrdersStatus.DRAFT.name());
    }

    @Test
    @DisplayName("should_ThrowBadRequest_When_GetAllStatusFilterIsInvalid")
    void should_ThrowBadRequest_When_GetAllStatusFilterIsInvalid() {
        PurchaseOrdersFilterRequest filter = PurchaseOrdersFilterRequest.builder()
                .status("NOT_A_STATUS")
                .build();
        Pageable pageable = PageRequest.of(0, 10);

        assertThatThrownBy(() -> service.getAll(filter, pageable))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid purchase order status")
                .hasFieldOrPropertyWithValue("errorCode", "COM_001");

        verify(purchaseOrdersRepository, never()).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    @DisplayName("should_ThrowBadRequest_When_GetAllOrderDateRangeIsInvalid")
    void should_ThrowBadRequest_When_GetAllOrderDateRangeIsInvalid() {
        PurchaseOrdersFilterRequest filter = PurchaseOrdersFilterRequest.builder()
                .orderDateFrom(LocalDate.of(2026, 3, 10))
                .orderDateTo(LocalDate.of(2026, 3, 7))
                .build();
        Pageable pageable = PageRequest.of(0, 10);

        assertThatThrownBy(() -> service.getAll(filter, pageable))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("orderDateFrom must be less than or equal to orderDateTo")
                .hasFieldOrPropertyWithValue("errorCode", "COM_001");

        verify(purchaseOrdersRepository, never()).findAll(any(Specification.class), eq(pageable));
    }

    private PurchaseOrders buildPurchaseOrder(String id, String number, PurchaseOrdersStatus status) {
        PurchaseOrders entity = PurchaseOrders.builder()
                .purchaseOrderNumber(number)
                .supplierId("sup-001")
                .warehouseId("wh-001")
                .orderDate(LocalDate.of(2026, 3, 7))
                .expectedDeliveryDate(LocalDate.of(2026, 3, 10))
                .status(status)
                .subTotal(new BigDecimal("100.00"))
                .taxAmount(new BigDecimal("10.00"))
                .totalAmount(new BigDecimal("110.00"))
                .currency(CurrencyType.USD)
                .paymentTerms("NET 30")
                .notes("Test PO")
                .confirmedAt(LocalDateTime.of(2026, 3, 7, 11, 0, 0))
                .confirmedBy("acc-002")
                .build();
        entity.setId(id);
        entity.setCreatedAt(LocalDateTime.of(2026, 3, 7, 10, 0, 0));
        entity.setUpdatedAt(LocalDateTime.of(2026, 3, 7, 11, 0, 0));
        return entity;
    }

    private BusinessPartners buildSupplier(
            String id,
            BusinessPartnerType type,
            BusinessPartnerStatus status,
            String paymentTerms
    ) {
        BusinessPartners supplier = BusinessPartners.builder()
                .code("BP-001")
                .name("Supplier A")
                .type(type)
                .status(status)
                .paymentTerms(paymentTerms)
                .build();
        supplier.setId(id);
        return supplier;
    }

    private Warehouses buildWarehouse(String id, WareHouseStatus status) {
        Warehouses warehouse = Warehouses.builder()
                .code("WH-001")
                .name("Main Warehouse")
                .status(status)
                .build();
        warehouse.setId(id);
        return warehouse;
    }
}
