package org.demo.whs.service.impl;

import org.demo.whs.entity.Account;
import org.demo.whs.entity.Employee;
import org.demo.whs.entity.Inventory;
import org.demo.whs.entity.StockAdjustments;
import org.demo.whs.entity.StockMovements;
import org.demo.whs.entity.dto.request.StockAdjustments.ApproveStockAdjustmentRequest;
import org.demo.whs.entity.dto.request.StockAdjustments.RejectStockAdjustmentRequest;
import org.demo.whs.entity.dto.request.StockAdjustments.SearchStockAdjustmentsRequest;
import org.demo.whs.entity.dto.request.StockAdjustments.StockAdjustmentsRequest;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.dto.response.StockAdjustments.StockAdjustmentsResponse;
import org.demo.whs.entity.enums.*;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.NotFoundException;
import org.demo.whs.mapper.StockAdjustmentsMapper;
import org.demo.whs.mapper.StockMovementsMapper;
import org.demo.whs.repository.*;
import org.demo.whs.utils.IdentifierGenerator;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private RoleRepository roleRepository;

    private StockAdjustmentsServiceImpl stockAdjustmentsService;

    private static final String ACTOR_ID = "acc-1";
    private static final String USERNAME = "tester";

    @BeforeEach
    void setUp() {
        stockAdjustmentsService = new StockAdjustmentsServiceImpl(
                stockAdjustmentsRepository,
                inventoryRepository,
                stockMovementsRepository,
                accountRepository,
                employeeRepository,
                new StockAdjustmentsMapper(),
                new StockMovementsMapper(),
                roleRepository,
                new IdentifierGenerator()
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

        Employee employee = Employee.builder()
                .accountId(ACTOR_ID)
                .employeeCode("EMP-001")
                .warehouseId("wh-1")
                .build();
        lenient().when(employeeRepository.findByAccountId(ACTOR_ID)).thenReturn(Optional.of(employee));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // =========================================================================
    // createAdjustment() — Happy paths
    // =========================================================================

    @Nested
    @DisplayName("createAdjustment() — Happy paths")
    class CreateAdjustmentHappyPaths {

        @Test
        @DisplayName("Should create PENDING_APPROVAL adjustment when role requires approval")
        void should_CreatePendingAdjustment_When_RoleRequiresApproval() {
            Inventory inventory = buildInventory("inv-1", "100.00", "10.00");
            StockAdjustmentsRequest request = buildAdjustmentRequest("inv-1", "120.00", ReasonType.COUNT_ERROR);

            when(inventoryRepository.findByIdForUpdate("inv-1")).thenReturn(Optional.of(inventory));
            when(roleRepository.findRoleNamesByAccountId(ACTOR_ID)).thenReturn(List.of("USER"));
            when(stockAdjustmentsRepository.existsByAdjustmentNumber(anyString())).thenReturn(false);
            when(stockAdjustmentsRepository.save(any(StockAdjustments.class))).thenAnswer(inv -> {
                StockAdjustments adj = inv.getArgument(0);
                adj.setId("adj-1");
                return adj;
            });

            StockAdjustmentsResponse response = stockAdjustmentsService.createAdjustment(request);

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo(StockAdjustmentsStatus.PENDING_APPROVAL);
            assertThat(response.getRequiresApproval()).isTrue();
            assertThat(response.getAdjustmentQuantity()).isEqualByComparingTo("20.00");
            assertThat(response.getQuantityBefore()).isEqualByComparingTo("100.00");
            assertThat(response.getQuantityAfter()).isEqualByComparingTo("120.00");

            verify(inventoryRepository, never()).save(any(Inventory.class));
            verify(stockMovementsRepository, never()).save(any(StockMovements.class));
        }

        @Test
        @DisplayName("Should auto-approve and create movement when ADMIN creates adjustment")
        void should_AutoApproveAndCreateMovement_When_AdminCreates() {
            Inventory inventory = buildInventory("inv-1", "100.00", "10.00");
            StockAdjustmentsRequest request = buildAdjustmentRequest("inv-1", "80.00", ReasonType.COUNT_ERROR);

            when(inventoryRepository.findByIdForUpdate("inv-1")).thenReturn(Optional.of(inventory));
            when(roleRepository.findRoleNamesByAccountId(ACTOR_ID)).thenReturn(List.of("ADMIN"));
            when(stockAdjustmentsRepository.existsByAdjustmentNumber(anyString())).thenReturn(false);
            when(stockAdjustmentsRepository.save(any(StockAdjustments.class))).thenAnswer(inv -> {
                StockAdjustments adj = inv.getArgument(0);
                adj.setId("adj-2");
                return adj;
            });

            StockAdjustmentsResponse response = stockAdjustmentsService.createAdjustment(request);

            assertThat(response.getStatus()).isEqualTo(StockAdjustmentsStatus.APPROVED);
            assertThat(response.getRequiresApproval()).isFalse();

            ArgumentCaptor<Inventory> inventoryCaptor = ArgumentCaptor.forClass(Inventory.class);
            verify(inventoryRepository).save(inventoryCaptor.capture());
            assertThat(inventoryCaptor.getValue().getOnHandQuantity()).isEqualByComparingTo("80.00");

            ArgumentCaptor<StockMovements> movementCaptor = ArgumentCaptor.forClass(StockMovements.class);
            verify(stockMovementsRepository).save(movementCaptor.capture());
            StockMovements movement = movementCaptor.getValue();
            assertThat(movement.getMovementType()).isEqualTo(StockMovementsType.ADJUSTMENT_DECREASE);
            assertThat(movement.getQuantityChange()).isEqualByComparingTo("-20.00");
            assertThat(movement.getQuantityBefore()).isEqualByComparingTo("100.00");
            assertThat(movement.getQuantityAfter()).isEqualByComparingTo("80.00");
            assertThat(movement.getReferenceType()).isEqualTo(ReferenceType.STOCK_ADJUSTMENT);
        }

        @Test
        @DisplayName("Should require approval for SYSTEM_ERROR reason when role is USER")
        void should_RequireApproval_When_ReasonIsSystemErrorAndRoleIsUser() {
            Inventory inventory = buildInventory("inv-1", "50.00", "5.00");
            StockAdjustmentsRequest request = buildAdjustmentRequest("inv-1", "60.00", ReasonType.SYSTEM_ERROR);

            when(inventoryRepository.findByIdForUpdate("inv-1")).thenReturn(Optional.of(inventory));
            when(roleRepository.findRoleNamesByAccountId(ACTOR_ID)).thenReturn(List.of("USER"));
            when(stockAdjustmentsRepository.existsByAdjustmentNumber(anyString())).thenReturn(false);
            when(stockAdjustmentsRepository.save(any(StockAdjustments.class))).thenAnswer(inv -> {
                StockAdjustments adj = inv.getArgument(0);
                adj.setId("adj-3");
                return adj;
            });

            StockAdjustmentsResponse response = stockAdjustmentsService.createAdjustment(request);

            assertThat(response.getStatus()).isEqualTo(StockAdjustmentsStatus.PENDING_APPROVAL);
            assertThat(response.getRequiresApproval()).isTrue();
            verify(inventoryRepository, never()).save(any(Inventory.class));
            verify(stockMovementsRepository, never()).save(any(StockMovements.class));
        }

        @Test
        @DisplayName("Should auto-approve SYSTEM_ERROR when ADMIN creates it")
        void should_AutoApprove_When_AdminCreatesSystemError() {
            Inventory inventory = buildInventory("inv-1", "50.00", "5.00");
            StockAdjustmentsRequest request = buildAdjustmentRequest("inv-1", "60.00", ReasonType.SYSTEM_ERROR);

            when(inventoryRepository.findByIdForUpdate("inv-1")).thenReturn(Optional.of(inventory));
            when(roleRepository.findRoleNamesByAccountId(ACTOR_ID)).thenReturn(List.of("ADMIN"));
            when(stockAdjustmentsRepository.existsByAdjustmentNumber(anyString())).thenReturn(false);
            when(stockAdjustmentsRepository.save(any(StockAdjustments.class))).thenAnswer(inv -> {
                StockAdjustments adj = inv.getArgument(0);
                adj.setId("adj-3-admin");
                return adj;
            });

            StockAdjustmentsResponse response = stockAdjustmentsService.createAdjustment(request);

            assertThat(response.getStatus()).isEqualTo(StockAdjustmentsStatus.APPROVED);
            verify(inventoryRepository).save(any(Inventory.class));
            verify(stockMovementsRepository).save(any(StockMovements.class));
        }

        @Test
        @DisplayName("Should create ADJUSTMENT_INCREASE movement when quantity increases")
        void should_CreateIncreaseMovement_When_QuantityIncreases() {
            Inventory inventory = buildInventory("inv-1", "100.00", "10.00");
            StockAdjustmentsRequest request = buildAdjustmentRequest("inv-1", "150.00", ReasonType.COUNT_ERROR);

            when(inventoryRepository.findByIdForUpdate("inv-1")).thenReturn(Optional.of(inventory));
            when(roleRepository.findRoleNamesByAccountId(ACTOR_ID)).thenReturn(List.of("ADMIN"));
            when(stockAdjustmentsRepository.existsByAdjustmentNumber(anyString())).thenReturn(false);
            when(stockAdjustmentsRepository.save(any(StockAdjustments.class))).thenAnswer(inv -> {
                StockAdjustments adj = inv.getArgument(0);
                adj.setId("adj-inc");
                return adj;
            });

            stockAdjustmentsService.createAdjustment(request);

            ArgumentCaptor<StockMovements> cap = ArgumentCaptor.forClass(StockMovements.class);
            verify(stockMovementsRepository).save(cap.capture());
            assertThat(cap.getValue().getMovementType()).isEqualTo(StockMovementsType.ADJUSTMENT_INCREASE);
            assertThat(cap.getValue().getQuantityChange()).isEqualByComparingTo("50.00");
        }

        @Test
        @DisplayName("Should generate unique adjustment number with ADJ- prefix")
        void should_GenerateUniqueAdjustmentNumber() {
            Inventory inventory = buildInventory("inv-1", "100.00", "0.00");
            StockAdjustmentsRequest request = buildAdjustmentRequest("inv-1", "50.00", ReasonType.DAMAGE);

            when(inventoryRepository.findByIdForUpdate("inv-1")).thenReturn(Optional.of(inventory));
            when(roleRepository.findRoleNamesByAccountId(ACTOR_ID)).thenReturn(List.of("ADMIN"));
            when(stockAdjustmentsRepository.existsByAdjustmentNumber(anyString())).thenReturn(false);
            when(stockAdjustmentsRepository.save(any(StockAdjustments.class))).thenAnswer(inv -> {
                StockAdjustments adj = inv.getArgument(0);
                adj.setId("adj-num");
                return adj;
            });

            StockAdjustmentsResponse response = stockAdjustmentsService.createAdjustment(request);

            assertThat(response.getAdjustmentNumber()).startsWith("ADJ-");
            assertThat(response.getAdjustmentNumber()).hasSizeLessThanOrEqualTo(50);
        }
    }

    // =========================================================================
    // createAdjustment() — Validation failures
    // =========================================================================

    @Nested
    @DisplayName("createAdjustment() — Validation failures")
    class CreateAdjustmentValidation {

        @Test
        @DisplayName("Should throw BadRequest when adjustment quantity is zero")
        void should_ThrowBadRequest_When_AdjustmentQuantityIsZero() {
            Inventory inventory = buildInventory("inv-1", "100.00", "10.00");
            StockAdjustmentsRequest request = buildAdjustmentRequest("inv-1", "100.00", ReasonType.COUNT_ERROR);

            when(inventoryRepository.findByIdForUpdate("inv-1")).thenReturn(Optional.of(inventory));

            assertThatThrownBy(() -> stockAdjustmentsService.createAdjustment(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Adjustment quantity cannot be zero")
                    .hasFieldOrPropertyWithValue("errorCode", "STA_001");

            verify(stockAdjustmentsRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw BadRequest when quantity after is negative")
        void should_ThrowBadRequest_When_QuantityAfterIsNegative() {
            Inventory inventory = buildInventory("inv-1", "100.00", "10.00");
            StockAdjustmentsRequest request = buildAdjustmentRequest("inv-1", "-1.00", ReasonType.DAMAGE);

            when(inventoryRepository.findByIdForUpdate("inv-1")).thenReturn(Optional.of(inventory));

            assertThatThrownBy(() -> stockAdjustmentsService.createAdjustment(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Quantity after must be non-negative")
                    .hasFieldOrPropertyWithValue("errorCode", "STA_001");
        }

        @Test
        @DisplayName("Should throw BadRequest when quantity after < reserved quantity")
        void should_ThrowBadRequest_When_QuantityAfterLessThanReserved() {
            Inventory inventory = buildInventory("inv-1", "100.00", "30.00");
            StockAdjustmentsRequest request = buildAdjustmentRequest("inv-1", "20.00", ReasonType.DAMAGE);

            when(inventoryRepository.findByIdForUpdate("inv-1")).thenReturn(Optional.of(inventory));

            assertThatThrownBy(() -> stockAdjustmentsService.createAdjustment(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Quantity after cannot be lower than reserved quantity")
                    .hasFieldOrPropertyWithValue("errorCode", "STA_001");
        }

        @Test
        @DisplayName("Should throw NotFoundException when inventory not found")
        void should_ThrowNotFound_When_InventoryNotFound() {
            StockAdjustmentsRequest request = buildAdjustmentRequest("inv-999", "50.00", ReasonType.DAMAGE);

            when(inventoryRepository.findByIdForUpdate("inv-999")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> stockAdjustmentsService.createAdjustment(request))
                    .isInstanceOf(NotFoundException.class)
                    .hasFieldOrPropertyWithValue("errorCode", "INV_001");
        }

        @Test
        @DisplayName("Should throw BadRequest when unauthenticated")
        void should_ThrowBadRequest_When_Unauthenticated() {
            SecurityContextHolder.clearContext();

            StockAdjustmentsRequest request = buildAdjustmentRequest("inv-1", "50.00", ReasonType.DAMAGE);

            assertThatThrownBy(() -> stockAdjustmentsService.createAdjustment(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasFieldOrPropertyWithValue("errorCode", "AUTH_002");
        }

        @Test
        @DisplayName("Should throw BadRequest when account not found")
        void should_ThrowBadRequest_When_AccountNotFound() {
            when(accountRepository.findByUsername(USERNAME)).thenReturn(Optional.empty());

            StockAdjustmentsRequest request = buildAdjustmentRequest("inv-1", "50.00", ReasonType.DAMAGE);

            assertThatThrownBy(() -> stockAdjustmentsService.createAdjustment(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("User account not found")
                    .hasFieldOrPropertyWithValue("errorCode", "AUTH_002");
        }

        @Test
        @DisplayName("Should throw BadRequest when save fails")
        void should_ThrowBadRequest_When_SaveFails() {
            Inventory inventory = buildInventory("inv-1", "100.00", "10.00");
            StockAdjustmentsRequest request = buildAdjustmentRequest("inv-1", "120.00", ReasonType.COUNT_ERROR);

            when(inventoryRepository.findByIdForUpdate("inv-1")).thenReturn(Optional.of(inventory));
            when(roleRepository.findRoleNamesByAccountId(ACTOR_ID)).thenReturn(List.of("USER"));
            when(stockAdjustmentsRepository.existsByAdjustmentNumber(anyString())).thenReturn(false);
            when(stockAdjustmentsRepository.save(any(StockAdjustments.class)))
                    .thenThrow(new DataIntegrityViolationException("DB constraint violated"));

            assertThatThrownBy(() -> stockAdjustmentsService.createAdjustment(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Failed to create stock adjustment")
                    .hasFieldOrPropertyWithValue("errorCode", "STA_001");
        }

        @Test
        @DisplayName("Should throw BadRequest when user accesses inventory from different warehouse")
        void should_ThrowBadRequest_When_UserAccessDifferentWarehouseInventory() {
            Inventory inventoryFromOtherWarehouse = buildInventory("inv-other", "100.00", "10.00");
            setField(inventoryFromOtherWarehouse, "warehouseId", "wh-other");
            StockAdjustmentsRequest request = buildAdjustmentRequest("inv-other", "120.00", ReasonType.COUNT_ERROR);

            when(inventoryRepository.findByIdForUpdate("inv-other")).thenReturn(Optional.of(inventoryFromOtherWarehouse));

            assertThatThrownBy(() -> stockAdjustmentsService.createAdjustment(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("You do not have permission to access this warehouse")
                    .hasFieldOrPropertyWithValue("errorCode", "AUTH_003");
        }
    }

    // =========================================================================
    // getById()
    // =========================================================================

    @Nested
    @DisplayName("getById()")
    class GetByIdTests {

        @Test
        @DisplayName("Should return adjustment response when found")
        void should_ReturnResponse_When_Found() {
            StockAdjustments adjustment = buildPendingAdjustment("adj-1", "inv-1", "100.00", "90.00");

            when(stockAdjustmentsRepository.findById("adj-1")).thenReturn(Optional.of(adjustment));

            StockAdjustmentsResponse response = stockAdjustmentsService.getById("adj-1");

            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo("adj-1");
        }

        @Test
        @DisplayName("Should throw NotFoundException when adjustment not found")
        void should_ThrowNotFound_When_AdjustmentNotFound() {
            when(stockAdjustmentsRepository.findById("adj-999")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> stockAdjustmentsService.getById("adj-999"))
                    .isInstanceOf(NotFoundException.class)
                    .hasFieldOrPropertyWithValue("errorCode", "STA_404");
        }
    }

    // =========================================================================
    // approve() — Happy paths
    // =========================================================================

    @Nested
    @DisplayName("approve() — Happy paths")
    class ApproveHappyPaths {

        @Test
        @DisplayName("Should approve pending adjustment with increase")
        void should_ApprovePendingAdjustment_When_IncreaseQuantity() {
            Inventory inventory = buildInventory("inv-1", "100.00", "10.00");
            StockAdjustments adjustment = buildPendingAdjustment("adj-3", "inv-1", "100.00", "130.00");

            ApproveStockAdjustmentRequest approveRequest = new ApproveStockAdjustmentRequest();
            setField(approveRequest, "approvalNote", "Count verified");

            when(roleRepository.findRoleNamesByAccountId(ACTOR_ID)).thenReturn(List.of("ADMIN"));
            when(stockAdjustmentsRepository.findByIdForUpdate("adj-3")).thenReturn(Optional.of(adjustment));
            when(inventoryRepository.findByIdForUpdate("inv-1")).thenReturn(Optional.of(inventory));
            when(stockAdjustmentsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            StockAdjustmentsResponse response = stockAdjustmentsService.approve("adj-3", approveRequest);

            assertThat(response.getStatus()).isEqualTo(StockAdjustmentsStatus.APPROVED);
            assertThat(response.getApprovedAt()).isNotNull();
            assertThat(inventory.getOnHandQuantity()).isEqualByComparingTo("130.00");

            ArgumentCaptor<StockMovements> cap = ArgumentCaptor.forClass(StockMovements.class);
            verify(stockMovementsRepository).save(cap.capture());
            assertThat(cap.getValue().getMovementType()).isEqualTo(StockMovementsType.ADJUSTMENT_INCREASE);
        }

        @Test
        @DisplayName("Should approve pending adjustment with decrease")
        void should_ApprovePendingAdjustment_When_DecreaseQuantity() {
            Inventory inventory = buildInventory("inv-1", "100.00", "10.00");
            StockAdjustments adjustment = buildPendingAdjustment("adj-dec", "inv-1", "100.00", "50.00");

            when(roleRepository.findRoleNamesByAccountId(ACTOR_ID)).thenReturn(List.of("ADMIN"));
            when(stockAdjustmentsRepository.findByIdForUpdate("adj-dec")).thenReturn(Optional.of(adjustment));
            when(inventoryRepository.findByIdForUpdate("inv-1")).thenReturn(Optional.of(inventory));
            when(stockAdjustmentsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            StockAdjustmentsResponse response = stockAdjustmentsService.approve("adj-dec", null);

            assertThat(response.getStatus()).isEqualTo(StockAdjustmentsStatus.APPROVED);

            ArgumentCaptor<StockMovements> cap = ArgumentCaptor.forClass(StockMovements.class);
            verify(stockMovementsRepository).save(cap.capture());
            assertThat(cap.getValue().getMovementType()).isEqualTo(StockMovementsType.ADJUSTMENT_DECREASE);
        }
    }

    // =========================================================================
    // approve() — Validation failures
    // =========================================================================

    @Nested
    @DisplayName("approve() — Validation failures")
    class ApproveValidation {

        @Test
        @DisplayName("Should throw NotFoundException when adjustment not found")
        void should_ThrowNotFound_When_AdjustmentNotFoundForApproval() {
            when(roleRepository.findRoleNamesByAccountId(ACTOR_ID)).thenReturn(List.of("ADMIN"));
            when(stockAdjustmentsRepository.findByIdForUpdate("adj-999")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> stockAdjustmentsService.approve("adj-999", null))
                    .isInstanceOf(NotFoundException.class)
                    .hasFieldOrPropertyWithValue("errorCode", "STA_404");
        }

        @Test
        @DisplayName("Should throw BadRequest when adjustment already APPROVED")
        void should_ThrowBadRequest_When_AlreadyApproved() {
            StockAdjustments adjustment = StockAdjustments.builder()
                    .inventoryId("inv-1")
                    .status(StockAdjustmentsStatus.APPROVED)
                    .build();
            adjustment.setId("adj-approved");

            when(roleRepository.findRoleNamesByAccountId(ACTOR_ID)).thenReturn(List.of("ADMIN"));
            when(stockAdjustmentsRepository.findByIdForUpdate("adj-approved")).thenReturn(Optional.of(adjustment));

            assertThatThrownBy(() -> stockAdjustmentsService.approve("adj-approved", null))
                    .isInstanceOf(BadRequestException.class)
                    .hasFieldOrPropertyWithValue("errorCode", "STA_002");
        }

        @Test
        @DisplayName("Should throw BadRequest when non-ADMIN tries to approve")
        void should_ThrowBadRequest_When_NonAdminTriesToApprove() {
            StockAdjustments adjustment = buildPendingAdjustment("adj-user", "inv-1", "100.00", "110.00");

            when(roleRepository.findRoleNamesByAccountId(ACTOR_ID)).thenReturn(List.of("USER"));

            assertThatThrownBy(() -> stockAdjustmentsService.approve("adj-user", null))
                    .isInstanceOf(BadRequestException.class)
                    .hasFieldOrPropertyWithValue("errorCode", "AUTH_002");
        }

        @Test
        @DisplayName("Should throw BadRequest when user approves inventory from different warehouse")
        void should_ThrowBadRequest_When_UserApprovesDifferentWarehouseInventory() {
            Inventory inventoryFromOtherWarehouse = buildInventory("inv-other", "100.00", "10.00");
            setField(inventoryFromOtherWarehouse, "warehouseId", "wh-other");
            StockAdjustments adjustment = buildPendingAdjustment("adj-wh", "inv-other", "100.00", "130.00");

            when(roleRepository.findRoleNamesByAccountId(ACTOR_ID)).thenReturn(List.of("ADMIN"));
            when(stockAdjustmentsRepository.findByIdForUpdate("adj-wh")).thenReturn(Optional.of(adjustment));
            when(inventoryRepository.findByIdForUpdate("inv-other")).thenReturn(Optional.of(inventoryFromOtherWarehouse));

            assertThatThrownBy(() -> stockAdjustmentsService.approve("adj-wh", null))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("You do not have permission to access this warehouse")
                    .hasFieldOrPropertyWithValue("errorCode", "AUTH_003");
        }
    }

    // =========================================================================
    // reject()
    // =========================================================================

    @Nested
    @DisplayName("reject()")
    class RejectTests {

        @Test
        @DisplayName("Should reject pending adjustment with reason")
        void should_RejectPendingAdjustment_When_ReasonProvided() {
            StockAdjustments adjustment = buildPendingAdjustment("adj-r1", "inv-1", "100.00", "110.00");

            RejectStockAdjustmentRequest rejectRequest = new RejectStockAdjustmentRequest();
            setField(rejectRequest, "rejectionReason", "Not verified");

            when(roleRepository.findRoleNamesByAccountId(ACTOR_ID)).thenReturn(List.of("ADMIN"));
            when(stockAdjustmentsRepository.findByIdForUpdate("adj-r1")).thenReturn(Optional.of(adjustment));
            when(stockAdjustmentsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            StockAdjustmentsResponse response = stockAdjustmentsService.reject("adj-r1", rejectRequest);

            assertThat(response.getStatus()).isEqualTo(StockAdjustmentsStatus.REJECTED);
            assertThat(response.getRejectionReason()).isEqualTo("Not verified");
        }

        @Test
        @DisplayName("Should throw BadRequest when rejection reason is blank")
        void should_ThrowBadRequest_When_RejectionReasonIsBlank() {
            StockAdjustments adjustment = buildPendingAdjustment("adj-r2", "inv-1", "100.00", "110.00");

            RejectStockAdjustmentRequest rejectRequest = new RejectStockAdjustmentRequest();

            when(roleRepository.findRoleNamesByAccountId(ACTOR_ID)).thenReturn(List.of("ADMIN"));
            when(stockAdjustmentsRepository.findByIdForUpdate("adj-r2")).thenReturn(Optional.of(adjustment));

            assertThatThrownBy(() -> stockAdjustmentsService.reject("adj-r2", rejectRequest))
                    .isInstanceOf(BadRequestException.class)
                    .hasFieldOrPropertyWithValue("errorCode", "STA_003");
        }
    }

    // =========================================================================
    // Helper methods
    // =========================================================================

    private Inventory buildInventory(String inventoryId, String onHandQuantity, String reservedQuantity) {
        Inventory inventory = Inventory.builder()
                .productId("prod-1")
                .warehouseId("wh-1")
                .locationId("loc-1")
                .batchId("batch-1")
                .onHandQuantity(new BigDecimal(onHandQuantity))
                .reservedQuantity(new BigDecimal(reservedQuantity))
                .version(0)
                .build();
        inventory.setId(inventoryId);
        return inventory;
    }

    private StockAdjustmentsRequest buildAdjustmentRequest(String inventoryId, String quantityAfter, ReasonType reason) {
        StockAdjustmentsRequest request = new StockAdjustmentsRequest();
        setField(request, "inventoryId", inventoryId);
        setField(request, "quantityAfter", new BigDecimal(quantityAfter));
        setField(request, "reason", reason);
        setField(request, "notes", "Test adjustment");
        return request;
    }

    private StockAdjustments buildPendingAdjustment(String id, String inventoryId, String quantityBefore, String quantityAfter) {
        StockAdjustments adjustment = StockAdjustments.builder()
                .adjustmentNumber("ADJ-" + id)
                .inventoryId(inventoryId)
                .productId("prod-1")
                .warehouseId("wh-1")
                .locationId("loc-1")
                .batchId("batch-1")
                .quantityBefore(new BigDecimal(quantityBefore))
                .quantityAfter(new BigDecimal(quantityAfter))
                .adjustmentQuantity(new BigDecimal(quantityAfter).subtract(new BigDecimal(quantityBefore)))
                .reason(ReasonType.COUNT_ERROR)
                .requiresApproval(true)
                .status(StockAdjustmentsStatus.PENDING_APPROVAL)
                .build();
        adjustment.setId(id);
        return adjustment;
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