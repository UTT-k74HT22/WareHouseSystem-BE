package org.demo.whs.service.impl;

import org.demo.whs.entity.Account;
import org.demo.whs.entity.InboundReceipts;
import org.demo.whs.entity.PurchaseOrders;
import org.demo.whs.entity.Warehouses;
import org.demo.whs.entity.dto.request.InboundReceipts.InboundReceiptsFilterRequest;
import org.demo.whs.entity.dto.request.InboundReceipts.InboundReceiptsRequest;
import org.demo.whs.entity.dto.response.InboundReceipts.InboundReceiptsResponse;
import org.demo.whs.entity.enums.PurchaseOrdersStatus;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.mapper.InboundReceiptLinesMapper;
import org.demo.whs.mapper.InboundReceiptsMapper;
import org.demo.whs.repository.AccountRepository;
import org.demo.whs.repository.InboundReceiptLinesRepository;
import org.demo.whs.repository.InboundReceiptsRepository;
import org.demo.whs.repository.PurchaseOrdersRepository;
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
    private WareHouseRepository wareHouseRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private InboundReceiptsMapper inboundReceiptsMapper;
    @Mock
    private InboundReceiptLinesMapper inboundReceiptLinesMapper;

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
}
