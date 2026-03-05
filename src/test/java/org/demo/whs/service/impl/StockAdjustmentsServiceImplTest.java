package org.demo.whs.service.impl;

import org.demo.whs.entity.Account;
import org.demo.whs.entity.Inventory;
import org.demo.whs.entity.StockAdjustments;
import org.demo.whs.entity.StockMovements;
import org.demo.whs.entity.dto.request.StockAdjustments.ApproveStockAdjustmentRequest;
import org.demo.whs.entity.dto.request.StockAdjustments.RejectStockAdjustmentRequest;
import org.demo.whs.entity.dto.request.StockAdjustments.StockAdjustmentsRequest;
import org.demo.whs.entity.dto.response.StockAdjustments.StockAdjustmentsResponse;
import org.demo.whs.entity.enums.AccountStatus;
import org.demo.whs.entity.enums.ReasonType;
import org.demo.whs.entity.enums.StockAdjustmentsStatus;
import org.demo.whs.entity.enums.StockMovementsType;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.mapper.StockAdjustmentsMapper;
import org.demo.whs.mapper.StockMovementsMapper;
import org.demo.whs.repository.AccountRepository;
import org.demo.whs.repository.InventoryRepository;
import org.demo.whs.repository.StockAdjustmentsRepository;
import org.demo.whs.repository.StockMovementsRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("StockAdjustmentsServiceImpl Unit Tests")
class StockAdjustmentsServiceImplTest {

    @Mock
    private StockAdjustmentsRepository stockAdjustmentsRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private StockMovementsRepository stockMovementsRepository;

    @Mock
    private AccountRepository accountRepository;

    private StockAdjustmentsServiceImpl stockAdjustmentsService;

    @BeforeEach
    void setUp() {
        stockAdjustmentsService = new StockAdjustmentsServiceImpl(
                stockAdjustmentsRepository,
                inventoryRepository,
                stockMovementsRepository,
                accountRepository,
                new StockAdjustmentsMapper(),
                new StockMovementsMapper()
        );

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("tester", "password", List.of())
        );

        Account account = Account.builder()
                .username("tester")
                .password("secret")
                .status(AccountStatus.ACTIVE)
                .build();
        account.setId("acc-1");

        lenient().when(accountRepository.findByUsername("tester")).thenReturn(Optional.of(account));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void should_CreatePendingAdjustment_When_RequestRequiresApproval() {
        Inventory inventory = buildInventory("inv-1", "100.00", "10.00");
        StockAdjustmentsRequest request = buildAdjustmentRequest("inv-1", "120.00", true);

        when(inventoryRepository.findByIdForUpdate("inv-1")).thenReturn(Optional.of(inventory));
        when(stockAdjustmentsRepository.existsByAdjustmentNumber(anyString())).thenReturn(false);
        when(stockAdjustmentsRepository.save(any(StockAdjustments.class))).thenAnswer(invocation -> {
            StockAdjustments adjustment = invocation.getArgument(0);
            adjustment.setId("adj-1");
            return adjustment;
        });

        StockAdjustmentsResponse response = stockAdjustmentsService.createAdjustment(request);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(StockAdjustmentsStatus.PENDING_APPROVAL);
        assertThat(response.getAdjustmentQuantity()).isEqualByComparingTo("20.00");

        verify(inventoryRepository, never()).save(any(Inventory.class));
        verify(stockMovementsRepository, never()).save(any(StockMovements.class));
    }

    @Test
    void should_CreateAutoApprovedAdjustmentAndMovement_When_RequestDoesNotRequireApproval() {
        Inventory inventory = buildInventory("inv-1", "100.00", "10.00");
        StockAdjustmentsRequest request = buildAdjustmentRequest("inv-1", "80.00", false);

        when(inventoryRepository.findByIdForUpdate("inv-1")).thenReturn(Optional.of(inventory));
        when(stockAdjustmentsRepository.existsByAdjustmentNumber(anyString())).thenReturn(false);
        when(stockAdjustmentsRepository.save(any(StockAdjustments.class))).thenAnswer(invocation -> {
            StockAdjustments adjustment = invocation.getArgument(0);
            adjustment.setId("adj-2");
            return adjustment;
        });

        StockAdjustmentsResponse response = stockAdjustmentsService.createAdjustment(request);

        assertThat(response.getStatus()).isEqualTo(StockAdjustmentsStatus.APPROVED);

        ArgumentCaptor<Inventory> inventoryCaptor = ArgumentCaptor.forClass(Inventory.class);
        verify(inventoryRepository).save(inventoryCaptor.capture());
        assertThat(inventoryCaptor.getValue().getOnHandQuantity()).isEqualByComparingTo("80.00");

        ArgumentCaptor<StockMovements> movementCaptor = ArgumentCaptor.forClass(StockMovements.class);
        verify(stockMovementsRepository).save(movementCaptor.capture());
        assertThat(movementCaptor.getValue().getMovementType()).isEqualTo(StockMovementsType.ADJUSTMENT_DECREASE);
        assertThat(movementCaptor.getValue().getQuantityChange()).isEqualByComparingTo("-20.00");
    }

    @Test
    void should_ThrowBadRequest_When_AdjustmentQuantityIsZero() {
        Inventory inventory = buildInventory("inv-1", "100.00", "10.00");
        StockAdjustmentsRequest request = buildAdjustmentRequest("inv-1", "100.00", true);

        when(inventoryRepository.findByIdForUpdate("inv-1")).thenReturn(Optional.of(inventory));

        assertThatThrownBy(() -> stockAdjustmentsService.createAdjustment(request))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("errorCode", "STA_001");

        verify(stockAdjustmentsRepository, never()).save(any(StockAdjustments.class));
    }

    @Test
    void should_ApproveAdjustmentAndCreateMovement_When_StatusIsPendingApproval() {
        Inventory inventory = buildInventory("inv-1", "100.00", "10.00");

        StockAdjustments adjustment = StockAdjustments.builder()
                .inventoryId("inv-1")
                .productId("prod-1")
                .warehouseId("wh-1")
                .locationId("loc-1")
                .batchId("batch-1")
                .quantityBefore(new BigDecimal("100.00"))
                .quantityAfter(new BigDecimal("130.00"))
                .adjustmentQuantity(new BigDecimal("30.00"))
                .reason(ReasonType.COUNT_ERROR)
                .status(StockAdjustmentsStatus.PENDING_APPROVAL)
                .requiresApproval(true)
                .build();
        adjustment.setId("adj-3");
        adjustment.setAdjustmentNumber("ADJ-001");

        ApproveStockAdjustmentRequest approveRequest = new ApproveStockAdjustmentRequest();
        setField(approveRequest, "approvalNote", "Count verified");

        when(stockAdjustmentsRepository.findByIdForUpdate("adj-3")).thenReturn(Optional.of(adjustment));
        when(inventoryRepository.findByIdForUpdate("inv-1")).thenReturn(Optional.of(inventory));
        when(stockAdjustmentsRepository.save(any(StockAdjustments.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StockAdjustmentsResponse response = stockAdjustmentsService.approve("adj-3", approveRequest);

        assertThat(response.getStatus()).isEqualTo(StockAdjustmentsStatus.APPROVED);
        assertThat(inventory.getOnHandQuantity()).isEqualByComparingTo("130.00");

        ArgumentCaptor<StockMovements> movementCaptor = ArgumentCaptor.forClass(StockMovements.class);
        verify(stockMovementsRepository).save(movementCaptor.capture());
        assertThat(movementCaptor.getValue().getMovementType()).isEqualTo(StockMovementsType.ADJUSTMENT_INCREASE);
        assertThat(movementCaptor.getValue().getQuantityChange()).isEqualByComparingTo("30.00");
    }

    @Test
    void should_ThrowBadRequest_When_ApproveAdjustmentInInvalidStatus() {
        StockAdjustments adjustment = StockAdjustments.builder()
                .inventoryId("inv-1")
                .status(StockAdjustmentsStatus.APPROVED)
                .build();
        adjustment.setId("adj-4");

        when(stockAdjustmentsRepository.findByIdForUpdate("adj-4")).thenReturn(Optional.of(adjustment));

        assertThatThrownBy(() -> stockAdjustmentsService.approve("adj-4", new ApproveStockAdjustmentRequest()))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("errorCode", "STA_002");

        verify(inventoryRepository, never()).findByIdForUpdate(anyString());
    }

    @Test
    void should_RejectAdjustmentWithoutInventorySideEffects_When_StatusIsPendingApproval() {
        StockAdjustments adjustment = StockAdjustments.builder()
                .inventoryId("inv-1")
                .status(StockAdjustmentsStatus.PENDING_APPROVAL)
                .quantityBefore(new BigDecimal("100.00"))
                .quantityAfter(new BigDecimal("90.00"))
                .adjustmentQuantity(new BigDecimal("-10.00"))
                .reason(ReasonType.DAMAGE)
                .requiresApproval(true)
                .build();
        adjustment.setId("adj-5");

        RejectStockAdjustmentRequest rejectRequest = new RejectStockAdjustmentRequest();
        setField(rejectRequest, "rejectionReason", "Evidence insufficient");

        when(stockAdjustmentsRepository.findByIdForUpdate("adj-5")).thenReturn(Optional.of(adjustment));
        when(stockAdjustmentsRepository.save(any(StockAdjustments.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StockAdjustmentsResponse response = stockAdjustmentsService.reject("adj-5", rejectRequest);

        assertThat(response.getStatus()).isEqualTo(StockAdjustmentsStatus.REJECTED);
        assertThat(response.getRejectionReason()).isEqualTo("Evidence insufficient");

        verify(inventoryRepository, never()).save(any(Inventory.class));
        verify(stockMovementsRepository, never()).save(any(StockMovements.class));
    }

    private Inventory buildInventory(String id, String onHand, String reserved) {
        Inventory inventory = Inventory.builder()
                .productId("prod-1")
                .warehouseId("wh-1")
                .locationId("loc-1")
                .batchId("batch-1")
                .onHandQuantity(new BigDecimal(onHand))
                .reservedQuantity(new BigDecimal(reserved))
                .version(0)
                .build();
        inventory.setId(id);
        return inventory;
    }

    private StockAdjustmentsRequest buildAdjustmentRequest(String inventoryId, String quantityAfter, boolean requiresApproval) {
        StockAdjustmentsRequest request = new StockAdjustmentsRequest();
        setField(request, "inventoryId", inventoryId);
        setField(request, "quantityAfter", new BigDecimal(quantityAfter));
        setField(request, "reason", ReasonType.COUNT_ERROR);
        setField(request, "notes", "Cycle count");
        setField(request, "requiresApproval", requiresApproval);
        return request;
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Failed to set field " + fieldName, ex);
        }
    }
}
