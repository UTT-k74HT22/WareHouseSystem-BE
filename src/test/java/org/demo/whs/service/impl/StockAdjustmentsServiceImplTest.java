package org.demo.whs.service.impl;

import org.demo.whs.entity.Account;
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
                new StockAdjustmentsMapper(),
                new StockMovementsMapper(),
                roleRepository
        );

        // Set up security context with authenticated user
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

            // No inventory update or movement for pending
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

            // Inventory updated
            ArgumentCaptor<Inventory> inventoryCaptor = ArgumentCaptor.forClass(Inventory.class);
            verify(inventoryRepository).save(inventoryCaptor.capture());
            assertThat(inventoryCaptor.getValue().getOnHandQuantity()).isEqualByComparingTo("80.00");

            // Movement created with ADJUSTMENT_DECREASE
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
        @DisplayName("Should require approval for SYSTEM_ERROR reason when role is USER (sensitive reason)")
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

            // SYSTEM_ERROR + non-ADMIN → requires approval (sensitive reason)
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
            assertThat(response.getAdjustmentNumber()).hasSize(27); // "ADJ-" + 14 timestamp + "-" + 8 UUID
        }
    }

    // =========================================================================
    // createAdjustment() — Validation failures
    // =========================================================================

    @Nested
    @DisplayName("createAdjustment() — Validation failures")
    class CreateAdjustmentValidation {

        @Test
        @DisplayName("Should throw BadRequest when adjustment quantity is zero (same quantity)")
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
        @DisplayName("Should throw BadRequest when unauthenticated (no security context)")
        void should_ThrowBadRequest_When_Unauthenticated() {
            SecurityContextHolder.clearContext();

            StockAdjustmentsRequest request = buildAdjustmentRequest("inv-1", "50.00", ReasonType.DAMAGE);

            assertThatThrownBy(() -> stockAdjustmentsService.createAdjustment(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasFieldOrPropertyWithValue("errorCode", "AUTH_002");
        }

        @Test
        @DisplayName("Should throw BadRequest when account not found by username")
        void should_ThrowBadRequest_When_AccountNotFound() {
            when(accountRepository.findByUsername(USERNAME)).thenReturn(Optional.empty());

            StockAdjustmentsRequest request = buildAdjustmentRequest("inv-1", "50.00", ReasonType.DAMAGE);

            assertThatThrownBy(() -> stockAdjustmentsService.createAdjustment(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("User account not found")
                    .hasFieldOrPropertyWithValue("errorCode", "AUTH_002");
        }

        @Test
        @DisplayName("Should throw BadRequest when save fails with data integrity violation")
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
        @DisplayName("Should throw BadRequest when adjustment number generation exhausts retries")
        void should_ThrowBadRequest_When_AdjustmentNumberGenerationFails() {
            Inventory inventory = buildInventory("inv-1", "100.00", "10.00");
            StockAdjustmentsRequest request = buildAdjustmentRequest("inv-1", "120.00", ReasonType.COUNT_ERROR);

            when(inventoryRepository.findByIdForUpdate("inv-1")).thenReturn(Optional.of(inventory));
            when(roleRepository.findRoleNamesByAccountId(ACTOR_ID)).thenReturn(List.of("USER"));
            // All 5 candidates are taken
            when(stockAdjustmentsRepository.existsByAdjustmentNumber(anyString())).thenReturn(true);

            assertThatThrownBy(() -> stockAdjustmentsService.createAdjustment(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Unable to generate unique adjustment number");
        }

        @Test
        @DisplayName("Should succeed on second attempt when first adjustment number exists")
        void should_SucceedOnRetry_When_FirstAdjustmentNumberExists() {
            Inventory inventory = buildInventory("inv-1", "100.00", "10.00");
            StockAdjustmentsRequest request = buildAdjustmentRequest("inv-1", "120.00", ReasonType.COUNT_ERROR);

            when(inventoryRepository.findByIdForUpdate("inv-1")).thenReturn(Optional.of(inventory));
            when(roleRepository.findRoleNamesByAccountId(ACTOR_ID)).thenReturn(List.of("USER"));
            // First attempt → exists, second attempt → available
            when(stockAdjustmentsRepository.existsByAdjustmentNumber(anyString()))
                    .thenReturn(true)
                    .thenReturn(false);
            when(stockAdjustmentsRepository.save(any(StockAdjustments.class))).thenAnswer(inv -> {
                StockAdjustments adj = inv.getArgument(0);
                adj.setId("adj-retry");
                return adj;
            });

            StockAdjustmentsResponse response = stockAdjustmentsService.createAdjustment(request);

            assertThat(response).isNotNull();
            verify(stockAdjustmentsRepository, times(2)).existsByAdjustmentNumber(anyString());
        }
    }

    // =========================================================================
    // createAdjustment() — requiresApproval logic
    // =========================================================================

    @Nested
    @DisplayName("createAdjustment() — requiresApproval logic")
    class CreateAdjustmentApprovalLogic {

        @Test
        @DisplayName("Should NOT require approval when role is ADMIN")
        void should_NotRequireApproval_When_RoleIsAdmin() {
            Inventory inventory = buildInventory("inv-1", "100.00", "0.00");
            StockAdjustmentsRequest request = buildAdjustmentRequest("inv-1", "200.00", ReasonType.DAMAGE);

            when(inventoryRepository.findByIdForUpdate("inv-1")).thenReturn(Optional.of(inventory));
            when(roleRepository.findRoleNamesByAccountId(ACTOR_ID)).thenReturn(List.of("ADMIN"));
            when(stockAdjustmentsRepository.existsByAdjustmentNumber(anyString())).thenReturn(false);
            when(stockAdjustmentsRepository.save(any())).thenAnswer(inv -> {
                StockAdjustments adj = inv.getArgument(0);
                adj.setId("adj-a");
                return adj;
            });

            StockAdjustmentsResponse response = stockAdjustmentsService.createAdjustment(request);

            assertThat(response.getRequiresApproval()).isFalse();
            assertThat(response.getStatus()).isEqualTo(StockAdjustmentsStatus.APPROVED);
        }

        @Test
        @DisplayName("Should require approval when reason is SYSTEM_ERROR and role is not ADMIN")
        void should_RequireApproval_When_ReasonIsSystemErrorAndNotAdmin() {
            Inventory inventory = buildInventory("inv-1", "100.00", "0.00");
            StockAdjustmentsRequest request = buildAdjustmentRequest("inv-1", "90.00", ReasonType.SYSTEM_ERROR);

            when(inventoryRepository.findByIdForUpdate("inv-1")).thenReturn(Optional.of(inventory));
            when(roleRepository.findRoleNamesByAccountId(ACTOR_ID)).thenReturn(List.of("MANAGER"));
            when(stockAdjustmentsRepository.existsByAdjustmentNumber(anyString())).thenReturn(false);
            when(stockAdjustmentsRepository.save(any())).thenAnswer(inv -> {
                StockAdjustments adj = inv.getArgument(0);
                adj.setId("adj-se");
                return adj;
            });

            StockAdjustmentsResponse response = stockAdjustmentsService.createAdjustment(request);

            // SYSTEM_ERROR is a sensitive reason → requires approval for non-ADMIN
            assertThat(response.getRequiresApproval()).isTrue();
            assertThat(response.getStatus()).isEqualTo(StockAdjustmentsStatus.PENDING_APPROVAL);
        }

        @Test
        @DisplayName("Should require approval when MANAGER creates non-SYSTEM_ERROR adjustment")
        void should_RequireApproval_When_ManagerCreatesNonSystemError() {
            Inventory inventory = buildInventory("inv-1", "100.00", "0.00");
            StockAdjustmentsRequest request = buildAdjustmentRequest("inv-1", "90.00", ReasonType.DAMAGE);

            when(inventoryRepository.findByIdForUpdate("inv-1")).thenReturn(Optional.of(inventory));
            when(roleRepository.findRoleNamesByAccountId(ACTOR_ID)).thenReturn(List.of("MANAGER"));
            when(stockAdjustmentsRepository.existsByAdjustmentNumber(anyString())).thenReturn(false);
            when(stockAdjustmentsRepository.save(any())).thenAnswer(inv -> {
                StockAdjustments adj = inv.getArgument(0);
                adj.setId("adj-mgr");
                return adj;
            });

            StockAdjustmentsResponse response = stockAdjustmentsService.createAdjustment(request);

            assertThat(response.getRequiresApproval()).isTrue();
            assertThat(response.getStatus()).isEqualTo(StockAdjustmentsStatus.PENDING_APPROVAL);
        }

        @Test
        @DisplayName("Should require approval when USER creates DAMAGE adjustment")
        void should_RequireApproval_When_UserCreatesDamageAdjustment() {
            Inventory inventory = buildInventory("inv-1", "100.00", "5.00");
            StockAdjustmentsRequest request = buildAdjustmentRequest("inv-1", "80.00", ReasonType.DAMAGE);

            when(inventoryRepository.findByIdForUpdate("inv-1")).thenReturn(Optional.of(inventory));
            when(roleRepository.findRoleNamesByAccountId(ACTOR_ID)).thenReturn(List.of("USER"));
            when(stockAdjustmentsRepository.existsByAdjustmentNumber(anyString())).thenReturn(false);
            when(stockAdjustmentsRepository.save(any())).thenAnswer(inv -> {
                StockAdjustments adj = inv.getArgument(0);
                adj.setId("adj-u");
                return adj;
            });

            StockAdjustmentsResponse response = stockAdjustmentsService.createAdjustment(request);

            assertThat(response.getRequiresApproval()).isTrue();
            verify(inventoryRepository, never()).save(any());
            verify(stockMovementsRepository, never()).save(any());
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
            assertThat(response.getInventoryId()).isEqualTo("inv-1");
            assertThat(response.getQuantityBefore()).isEqualByComparingTo("100.00");
            assertThat(response.getQuantityAfter()).isEqualByComparingTo("90.00");
            assertThat(response.getAdjustmentQuantity()).isEqualByComparingTo("-10.00");
        }

        @Test
        @DisplayName("Should throw NotFoundException when adjustment not found")
        void should_ThrowNotFound_When_AdjustmentNotFound() {
            when(stockAdjustmentsRepository.findById("adj-999")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> stockAdjustmentsService.getById("adj-999"))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Stock adjustment not found")
                    .hasFieldOrPropertyWithValue("errorCode", "STA_404");
        }
    }

    // =========================================================================
    // getAll()
    // =========================================================================

    @Nested
    @DisplayName("getAll()")
    class GetAllTests {

        @Test
        @DisplayName("Should return paginated adjustments with default pagination")
        void should_ReturnPaginated_When_DefaultPagination() {
            StockAdjustments adj1 = buildPendingAdjustment("adj-1", "inv-1", "100.00", "90.00");
            StockAdjustments adj2 = buildPendingAdjustment("adj-2", "inv-2", "200.00", "180.00");
            Page<StockAdjustments> page = new PageImpl<>(List.of(adj1, adj2));

            when(stockAdjustmentsRepository.findAll(any(Pageable.class))).thenReturn(page);

            PageResponse<StockAdjustmentsResponse> response = stockAdjustmentsService.getAll(0, 10);

            assertThat(response.getContent()).hasSize(2);
            assertThat(response.getContent().get(0).getId()).isEqualTo("adj-1");
            assertThat(response.getContent().get(1).getId()).isEqualTo("adj-2");
        }

        @Test
        @DisplayName("Should return empty page when no adjustments exist")
        void should_ReturnEmptyPage_When_NoAdjustments() {
            Page<StockAdjustments> emptyPage = new PageImpl<>(Collections.emptyList());

            when(stockAdjustmentsRepository.findAll(any(Pageable.class))).thenReturn(emptyPage);

            PageResponse<StockAdjustmentsResponse> response = stockAdjustmentsService.getAll(0, 10);

            assertThat(response.getContent()).isEmpty();
        }

        @Test
        @DisplayName("Should use default page=0 and size=10 when null params")
        void should_UseDefaults_When_NullParams() {
            Page<StockAdjustments> page = new PageImpl<>(Collections.emptyList());
            when(stockAdjustmentsRepository.findAll(any(Pageable.class))).thenReturn(page);

            stockAdjustmentsService.getAll(null, null);

            ArgumentCaptor<Pageable> cap = ArgumentCaptor.forClass(Pageable.class);
            verify(stockAdjustmentsRepository).findAll(cap.capture());
            assertThat(cap.getValue().getPageNumber()).isEqualTo(0);
            assertThat(cap.getValue().getPageSize()).isEqualTo(10);
        }

        @Test
        @DisplayName("Should throw BadRequest when page is negative")
        void should_ThrowBadRequest_When_PageIsNegative() {
            assertThatThrownBy(() -> stockAdjustmentsService.getAll(-1, 10))
                    .isInstanceOf(BadRequestException.class)
                    .hasFieldOrPropertyWithValue("errorCode", "COM_001");
        }

        @Test
        @DisplayName("Should throw BadRequest when size is zero")
        void should_ThrowBadRequest_When_SizeIsZero() {
            assertThatThrownBy(() -> stockAdjustmentsService.getAll(0, 0))
                    .isInstanceOf(BadRequestException.class)
                    .hasFieldOrPropertyWithValue("errorCode", "COM_001");
        }

        @Test
        @DisplayName("Should throw BadRequest when size exceeds 100")
        void should_ThrowBadRequest_When_SizeExceeds100() {
            assertThatThrownBy(() -> stockAdjustmentsService.getAll(0, 101))
                    .isInstanceOf(BadRequestException.class)
                    .hasFieldOrPropertyWithValue("errorCode", "COM_001");
        }

        @Test
        @DisplayName("Should accept size=100 as maximum valid size")
        void should_AcceptSize100_When_MaxValidSize() {
            Page<StockAdjustments> page = new PageImpl<>(Collections.emptyList());
            when(stockAdjustmentsRepository.findAll(any(Pageable.class))).thenReturn(page);

            stockAdjustmentsService.getAll(0, 100);

            ArgumentCaptor<Pageable> cap = ArgumentCaptor.forClass(Pageable.class);
            verify(stockAdjustmentsRepository).findAll(cap.capture());
            assertThat(cap.getValue().getPageSize()).isEqualTo(100);
        }
    }

    // =========================================================================
    // search()
    // =========================================================================

    @Nested
    @DisplayName("search()")
    class SearchTests {

        @Test
        @DisplayName("Should pass all search criteria to repository")
        void should_PassAllCriteria_When_SearchWithFilters() {
            SearchStockAdjustmentsRequest request = new SearchStockAdjustmentsRequest();
            request.setStatus(StockAdjustmentsStatus.PENDING_APPROVAL);
            request.setProductId("prod-1");
            request.setWarehouseId("wh-1");
            request.setInventoryId("inv-1");
            request.setAdjustmentNumber("ADJ-001");
            request.setCreatedFrom(LocalDateTime.of(2026, 1, 1, 0, 0));
            request.setCreatedTo(LocalDateTime.of(2026, 12, 31, 23, 59));

            Page<StockAdjustments> page = new PageImpl<>(Collections.emptyList());
            when(stockAdjustmentsRepository.search(
                    eq(StockAdjustmentsStatus.PENDING_APPROVAL),
                    eq("prod-1"), eq("wh-1"), eq("inv-1"), eq("ADJ-001"),
                    any(LocalDateTime.class), any(LocalDateTime.class),
                    any(Pageable.class)
            )).thenReturn(page);

            PageResponse<StockAdjustmentsResponse> response = stockAdjustmentsService.search(request, 0, 10);

            assertThat(response.getContent()).isEmpty();
            verify(stockAdjustmentsRepository).search(
                    eq(StockAdjustmentsStatus.PENDING_APPROVAL),
                    eq("prod-1"), eq("wh-1"), eq("inv-1"), eq("ADJ-001"),
                    any(LocalDateTime.class), any(LocalDateTime.class),
                    any(Pageable.class)
            );
        }

        @Test
        @DisplayName("Should handle null search criteria gracefully")
        void should_HandleNullCriteria_When_SearchWithNoFilters() {
            SearchStockAdjustmentsRequest request = new SearchStockAdjustmentsRequest();
            Page<StockAdjustments> page = new PageImpl<>(Collections.emptyList());

            when(stockAdjustmentsRepository.search(
                    isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                    any(Pageable.class)
            )).thenReturn(page);

            PageResponse<StockAdjustmentsResponse> response = stockAdjustmentsService.search(request, 0, 10);

            assertThat(response).isNotNull();
        }

        @Test
        @DisplayName("Should throw BadRequest for invalid pagination in search")
        void should_ThrowBadRequest_When_InvalidPaginationInSearch() {
            SearchStockAdjustmentsRequest request = new SearchStockAdjustmentsRequest();

            assertThatThrownBy(() -> stockAdjustmentsService.search(request, -1, 10))
                    .isInstanceOf(BadRequestException.class)
                    .hasFieldOrPropertyWithValue("errorCode", "COM_001");
        }
    }

    // =========================================================================
    // approve() — Happy paths
    // =========================================================================

    @Nested
    @DisplayName("approve() — Happy paths")
    class ApproveHappyPaths {

        @Test
        @DisplayName("Should approve pending adjustment, update inventory, create movement (increase)")
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

            // Assert status
            assertThat(response.getStatus()).isEqualTo(StockAdjustmentsStatus.APPROVED);
            assertThat(response.getApprovedBy()).isEqualTo(ACTOR_ID);
            assertThat(response.getApprovedAt()).isNotNull();
            assertThat(response.getRejectionReason()).isNull();

            // Assert inventory updated
            assertThat(inventory.getOnHandQuantity()).isEqualByComparingTo("130.00");

            // Assert movement created
            ArgumentCaptor<StockMovements> cap = ArgumentCaptor.forClass(StockMovements.class);
            verify(stockMovementsRepository).save(cap.capture());
            assertThat(cap.getValue().getMovementType()).isEqualTo(StockMovementsType.ADJUSTMENT_INCREASE);
            assertThat(cap.getValue().getQuantityChange()).isEqualByComparingTo("30.00");
            assertThat(cap.getValue().getQuantityBefore()).isEqualByComparingTo("100.00");
            assertThat(cap.getValue().getQuantityAfter()).isEqualByComparingTo("130.00");
        }

        @Test
        @DisplayName("Should approve pending adjustment with decrease quantity")
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
            assertThat(cap.getValue().getQuantityChange()).isEqualByComparingTo("-50.00");
        }

        @Test
        @DisplayName("Should append approval note to existing notes")
        void should_AppendApprovalNote_When_NotesExist() {
            Inventory inventory = buildInventory("inv-1", "100.00", "10.00");
            StockAdjustments adjustment = buildPendingAdjustment("adj-note", "inv-1", "100.00", "120.00");
            adjustment.setNotes("Original note");

            ApproveStockAdjustmentRequest approveRequest = new ApproveStockAdjustmentRequest();
            setField(approveRequest, "approvalNote", "Verified by manager");

            when(roleRepository.findRoleNamesByAccountId(ACTOR_ID)).thenReturn(List.of("ADMIN"));
            when(stockAdjustmentsRepository.findByIdForUpdate("adj-note")).thenReturn(Optional.of(adjustment));
            when(inventoryRepository.findByIdForUpdate("inv-1")).thenReturn(Optional.of(inventory));
            when(stockAdjustmentsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            StockAdjustmentsResponse response = stockAdjustmentsService.approve("adj-note", approveRequest);

            assertThat(response.getNotes()).contains("Original note");
            assertThat(response.getNotes()).contains("APPROVAL_NOTE: Verified by manager");
        }

        @Test
        @DisplayName("Should approve without modifying notes when request has null note")
        void should_ApproveWithoutNote_When_NullApprovalNote() {
            Inventory inventory = buildInventory("inv-1", "100.00", "10.00");
            StockAdjustments adjustment = buildPendingAdjustment("adj-nn", "inv-1", "100.00", "120.00");
            adjustment.setNotes("Existing note");

            when(roleRepository.findRoleNamesByAccountId(ACTOR_ID)).thenReturn(List.of("ADMIN"));
            when(stockAdjustmentsRepository.findByIdForUpdate("adj-nn")).thenReturn(Optional.of(adjustment));
            when(inventoryRepository.findByIdForUpdate("inv-1")).thenReturn(Optional.of(inventory));
            when(stockAdjustmentsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            StockAdjustmentsResponse response = stockAdjustmentsService.approve("adj-nn", null);

            // Notes should remain unchanged
            assertThat(response.getNotes()).isEqualTo("Existing note");
        }
    }

    // =========================================================================
    // approve() — Validation failures
    // =========================================================================

    @Nested
    @DisplayName("approve() — Validation failures")
    class ApproveValidation {

        @Test
        @DisplayName("Should throw NotFoundException when adjustment not found for approval")
        void should_ThrowNotFound_When_AdjustmentNotFoundForApproval() {
            when(roleRepository.findRoleNamesByAccountId(ACTOR_ID)).thenReturn(List.of("ADMIN"));
            when(stockAdjustmentsRepository.findByIdForUpdate("adj-999")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> stockAdjustmentsService.approve("adj-999", null))
                    .isInstanceOf(NotFoundException.class)
                    .hasFieldOrPropertyWithValue("errorCode", "STA_404");
        }

        @Test
        @DisplayName("Should throw BadRequest when adjustment is already APPROVED")
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
                    .hasMessageContaining("Adjustment is not in pending status")
                    .hasFieldOrPropertyWithValue("errorCode", "STA_002");

            verify(inventoryRepository, never()).findByIdForUpdate(anyString());
        }

        @Test
        @DisplayName("Should throw BadRequest when adjustment is already REJECTED")
        void should_ThrowBadRequest_When_AlreadyRejected() {
            StockAdjustments adjustment = StockAdjustments.builder()
                    .inventoryId("inv-1")
                    .status(StockAdjustmentsStatus.REJECTED)
                    .build();
            adjustment.setId("adj-rejected");

            when(roleRepository.findRoleNamesByAccountId(ACTOR_ID)).thenReturn(List.of("ADMIN"));
            when(stockAdjustmentsRepository.findByIdForUpdate("adj-rejected")).thenReturn(Optional.of(adjustment));

            assertThatThrownBy(() -> stockAdjustmentsService.approve("adj-rejected", null))
                    .isInstanceOf(BadRequestException.class)
                    .hasFieldOrPropertyWithValue("errorCode", "STA_002");
        }

        @Test
        @DisplayName("Should throw BadRequest when inventory on-hand changed since adjustment was created")
        void should_ThrowBadRequest_When_InventoryQuantityChangedSinceCreation() {
            Inventory inventory = buildInventory("inv-1", "80.00", "10.00");
            StockAdjustments adjustment = buildPendingAdjustment("adj-stale", "inv-1", "100.00", "130.00");

            when(roleRepository.findRoleNamesByAccountId(ACTOR_ID)).thenReturn(List.of("ADMIN"));
            when(stockAdjustmentsRepository.findByIdForUpdate("adj-stale")).thenReturn(Optional.of(adjustment));
            when(inventoryRepository.findByIdForUpdate("inv-1")).thenReturn(Optional.of(inventory));

            assertThatThrownBy(() -> stockAdjustmentsService.approve("adj-stale", null))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Inventory on-hand quantity has changed")
                    .hasFieldOrPropertyWithValue("errorCode", "STA_001");

            verify(inventoryRepository, never()).save(any());
            verify(stockMovementsRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw BadRequest when resulting quantityAfter < reserved (state changed)")
        void should_ThrowBadRequest_When_QuantityAfterLessThanReserved() {
            Inventory inventory = buildInventory("inv-1", "100.00", "60.00");
            StockAdjustments adjustment = buildPendingAdjustment("adj-res", "inv-1", "100.00", "50.00");

            when(roleRepository.findRoleNamesByAccountId(ACTOR_ID)).thenReturn(List.of("ADMIN"));
            when(stockAdjustmentsRepository.findByIdForUpdate("adj-res")).thenReturn(Optional.of(adjustment));
            when(inventoryRepository.findByIdForUpdate("inv-1")).thenReturn(Optional.of(inventory));

            assertThatThrownBy(() -> stockAdjustmentsService.approve("adj-res", null))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Inventory state changed and adjustment is no longer valid");
        }

        @Test
        @DisplayName("Should throw NotFoundException when inventory not found during approval")
        void should_ThrowNotFound_When_InventoryNotFoundDuringApproval() {
            StockAdjustments adjustment = buildPendingAdjustment("adj-inv", "inv-gone", "100.00", "120.00");

            when(roleRepository.findRoleNamesByAccountId(ACTOR_ID)).thenReturn(List.of("ADMIN"));
            when(stockAdjustmentsRepository.findByIdForUpdate("adj-inv")).thenReturn(Optional.of(adjustment));
            when(inventoryRepository.findByIdForUpdate("inv-gone")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> stockAdjustmentsService.approve("adj-inv", null))
                    .isInstanceOf(NotFoundException.class)
                    .hasFieldOrPropertyWithValue("errorCode", "INV_001");
        }

        @Test
        @DisplayName("Should throw BadRequest when non-ADMIN user tries to approve")
        void should_ThrowBadRequest_When_NonAdminTriesToApprove() {
            when(roleRepository.findRoleNamesByAccountId(ACTOR_ID)).thenReturn(List.of("USER"));

            assertThatThrownBy(() -> stockAdjustmentsService.approve("adj-1", null))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("You do not have permission")
                    .hasFieldOrPropertyWithValue("errorCode", "AUTH_002");

            verify(stockAdjustmentsRepository, never()).findByIdForUpdate(anyString());
        }

        @Test
        @DisplayName("Should throw BadRequest when role is null (user has no role)")
        void should_ThrowBadRequest_When_RoleIsNullForApprove() {
            when(roleRepository.findRoleNamesByAccountId(ACTOR_ID)).thenReturn(Collections.emptyList());

            assertThatThrownBy(() -> stockAdjustmentsService.approve("adj-1", null))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("User role not found")
                    .hasFieldOrPropertyWithValue("errorCode", "AUTH_002");
        }
    }

    // =========================================================================
    // reject() — Happy paths
    // =========================================================================

    @Nested
    @DisplayName("reject() — Happy paths")
    class RejectHappyPaths {

        @Test
        @DisplayName("Should reject pending adjustment without inventory side effects")
        void should_RejectAdjustment_When_PendingApproval() {
            StockAdjustments adjustment = buildPendingAdjustment("adj-5", "inv-1", "100.00", "90.00");

            RejectStockAdjustmentRequest rejectRequest = new RejectStockAdjustmentRequest();
            setField(rejectRequest, "rejectionReason", "Evidence insufficient");

            when(roleRepository.findRoleNamesByAccountId(ACTOR_ID)).thenReturn(List.of("ADMIN"));
            when(stockAdjustmentsRepository.findByIdForUpdate("adj-5")).thenReturn(Optional.of(adjustment));
            when(stockAdjustmentsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            StockAdjustmentsResponse response = stockAdjustmentsService.reject("adj-5", rejectRequest);

            assertThat(response.getStatus()).isEqualTo(StockAdjustmentsStatus.REJECTED);
            assertThat(response.getRejectionReason()).isEqualTo("Evidence insufficient");
            assertThat(response.getApprovedBy()).isEqualTo(ACTOR_ID);
            assertThat(response.getApprovedAt()).isNotNull();

            // No inventory or movement changes
            verify(inventoryRepository, never()).save(any());
            verify(inventoryRepository, never()).findByIdForUpdate(anyString());
            verify(stockMovementsRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should append rejection reason to notes for audit trail")
        void should_AppendRejectionToNotes_When_Rejected() {
            StockAdjustments adjustment = buildPendingAdjustment("adj-note-rej", "inv-1", "100.00", "90.00");
            adjustment.setNotes("Initial note");

            RejectStockAdjustmentRequest rejectRequest = new RejectStockAdjustmentRequest();
            setField(rejectRequest, "rejectionReason", "Photos unclear");

            when(roleRepository.findRoleNamesByAccountId(ACTOR_ID)).thenReturn(List.of("ADMIN"));
            when(stockAdjustmentsRepository.findByIdForUpdate("adj-note-rej")).thenReturn(Optional.of(adjustment));
            when(stockAdjustmentsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            StockAdjustmentsResponse response = stockAdjustmentsService.reject("adj-note-rej", rejectRequest);

            assertThat(response.getNotes()).contains("Initial note");
            assertThat(response.getNotes()).contains("REJECTION_REASON: Photos unclear");
        }

        @Test
        @DisplayName("Should set rejection reason as notes when no existing notes")
        void should_SetRejectionAsNotes_When_NoExistingNotes() {
            StockAdjustments adjustment = buildPendingAdjustment("adj-no-note", "inv-1", "100.00", "90.00");
            adjustment.setNotes(null);

            RejectStockAdjustmentRequest rejectRequest = new RejectStockAdjustmentRequest();
            setField(rejectRequest, "rejectionReason", "Not justified");

            when(roleRepository.findRoleNamesByAccountId(ACTOR_ID)).thenReturn(List.of("ADMIN"));
            when(stockAdjustmentsRepository.findByIdForUpdate("adj-no-note")).thenReturn(Optional.of(adjustment));
            when(stockAdjustmentsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            StockAdjustmentsResponse response = stockAdjustmentsService.reject("adj-no-note", rejectRequest);

            assertThat(response.getNotes()).isEqualTo("REJECTION_REASON: Not justified");
        }
    }

    // =========================================================================
    // reject() — Validation failures
    // =========================================================================

    @Nested
    @DisplayName("reject() — Validation failures")
    class RejectValidation {

        @Test
        @DisplayName("Should throw NotFoundException when adjustment not found for rejection")
        void should_ThrowNotFound_When_AdjustmentNotFoundForRejection() {
            RejectStockAdjustmentRequest rejectRequest = new RejectStockAdjustmentRequest();
            setField(rejectRequest, "rejectionReason", "Some reason");

            when(roleRepository.findRoleNamesByAccountId(ACTOR_ID)).thenReturn(List.of("ADMIN"));
            when(stockAdjustmentsRepository.findByIdForUpdate("adj-999")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> stockAdjustmentsService.reject("adj-999", rejectRequest))
                    .isInstanceOf(NotFoundException.class)
                    .hasFieldOrPropertyWithValue("errorCode", "STA_404");
        }

        @Test
        @DisplayName("Should throw BadRequest when rejecting already APPROVED adjustment")
        void should_ThrowBadRequest_When_RejectingApproved() {
            StockAdjustments adjustment = StockAdjustments.builder()
                    .inventoryId("inv-1")
                    .status(StockAdjustmentsStatus.APPROVED)
                    .build();
            adjustment.setId("adj-app");

            RejectStockAdjustmentRequest rejectRequest = new RejectStockAdjustmentRequest();
            setField(rejectRequest, "rejectionReason", "Too late");

            when(roleRepository.findRoleNamesByAccountId(ACTOR_ID)).thenReturn(List.of("ADMIN"));
            when(stockAdjustmentsRepository.findByIdForUpdate("adj-app")).thenReturn(Optional.of(adjustment));

            assertThatThrownBy(() -> stockAdjustmentsService.reject("adj-app", rejectRequest))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Adjustment is not in pending status")
                    .hasFieldOrPropertyWithValue("errorCode", "STA_002");
        }

        @Test
        @DisplayName("Should throw BadRequest when rejecting already REJECTED adjustment")
        void should_ThrowBadRequest_When_RejectingAlreadyRejected() {
            StockAdjustments adjustment = StockAdjustments.builder()
                    .inventoryId("inv-1")
                    .status(StockAdjustmentsStatus.REJECTED)
                    .build();
            adjustment.setId("adj-rej");

            RejectStockAdjustmentRequest rejectRequest = new RejectStockAdjustmentRequest();
            setField(rejectRequest, "rejectionReason", "Again");

            when(roleRepository.findRoleNamesByAccountId(ACTOR_ID)).thenReturn(List.of("ADMIN"));
            when(stockAdjustmentsRepository.findByIdForUpdate("adj-rej")).thenReturn(Optional.of(adjustment));

            assertThatThrownBy(() -> stockAdjustmentsService.reject("adj-rej", rejectRequest))
                    .isInstanceOf(BadRequestException.class)
                    .hasFieldOrPropertyWithValue("errorCode", "STA_002");
        }

        @Test
        @DisplayName("Should throw BadRequest when rejection reason is null")
        void should_ThrowBadRequest_When_RejectionReasonIsNull() {
            StockAdjustments adjustment = buildPendingAdjustment("adj-null-rej", "inv-1", "100.00", "90.00");

            RejectStockAdjustmentRequest rejectRequest = new RejectStockAdjustmentRequest();
            // Don't set rejectionReason → it will be null

            when(roleRepository.findRoleNamesByAccountId(ACTOR_ID)).thenReturn(List.of("ADMIN"));
            when(stockAdjustmentsRepository.findByIdForUpdate("adj-null-rej")).thenReturn(Optional.of(adjustment));

            assertThatThrownBy(() -> stockAdjustmentsService.reject("adj-null-rej", rejectRequest))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Rejection reason is required")
                    .hasFieldOrPropertyWithValue("errorCode", "STA_003");
        }

        @Test
        @DisplayName("Should throw BadRequest when rejection reason is blank")
        void should_ThrowBadRequest_When_RejectionReasonIsBlank() {
            StockAdjustments adjustment = buildPendingAdjustment("adj-blank-rej", "inv-1", "100.00", "90.00");

            RejectStockAdjustmentRequest rejectRequest = new RejectStockAdjustmentRequest();
            setField(rejectRequest, "rejectionReason", "   ");

            when(roleRepository.findRoleNamesByAccountId(ACTOR_ID)).thenReturn(List.of("ADMIN"));
            when(stockAdjustmentsRepository.findByIdForUpdate("adj-blank-rej")).thenReturn(Optional.of(adjustment));

            assertThatThrownBy(() -> stockAdjustmentsService.reject("adj-blank-rej", rejectRequest))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Rejection reason is required")
                    .hasFieldOrPropertyWithValue("errorCode", "STA_003");
        }

        @Test
        @DisplayName("Should throw BadRequest when non-ADMIN user tries to reject")
        void should_ThrowBadRequest_When_NonAdminTriesToReject() {
            RejectStockAdjustmentRequest rejectRequest = new RejectStockAdjustmentRequest();
            setField(rejectRequest, "rejectionReason", "Some reason");

            when(roleRepository.findRoleNamesByAccountId(ACTOR_ID)).thenReturn(List.of("MANAGER"));

            assertThatThrownBy(() -> stockAdjustmentsService.reject("adj-1", rejectRequest))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("You do not have permission")
                    .hasFieldOrPropertyWithValue("errorCode", "AUTH_002");

            verify(stockAdjustmentsRepository, never()).findByIdForUpdate(anyString());
        }
    }

    // =========================================================================
    // Edge cases & boundary tests
    // =========================================================================

    @Nested
    @DisplayName("Edge cases & boundary tests")
    class EdgeCases {

        @Test
        @DisplayName("Should handle quantity after equals reserved quantity exactly")
        void should_HandleQuantityAfterEqualsReserved() {
            // onHand=100, reserved=50, quantityAfter=50 → exactly at reserved boundary
            Inventory inventory = buildInventory("inv-1", "100.00", "50.00");
            StockAdjustmentsRequest request = buildAdjustmentRequest("inv-1", "50.00", ReasonType.DAMAGE);

            when(inventoryRepository.findByIdForUpdate("inv-1")).thenReturn(Optional.of(inventory));
            when(roleRepository.findRoleNamesByAccountId(ACTOR_ID)).thenReturn(List.of("ADMIN"));
            when(stockAdjustmentsRepository.existsByAdjustmentNumber(anyString())).thenReturn(false);
            when(stockAdjustmentsRepository.save(any())).thenAnswer(inv -> {
                StockAdjustments adj = inv.getArgument(0);
                adj.setId("adj-edge");
                return adj;
            });

            StockAdjustmentsResponse response = stockAdjustmentsService.createAdjustment(request);

            // Should succeed because quantityAfter == reservedQuantity (not less)
            assertThat(response.getStatus()).isEqualTo(StockAdjustmentsStatus.APPROVED);
            assertThat(inventory.getOnHandQuantity()).isEqualByComparingTo("50.00");
        }

        @Test
        @DisplayName("Should handle zero reserved quantity with quantity decrease to zero")
        void should_HandleDecreaseToZero_When_NoReserved() {
            Inventory inventory = buildInventory("inv-1", "50.00", "0.00");
            StockAdjustmentsRequest request = buildAdjustmentRequest("inv-1", "0.00", ReasonType.EXPIRED);

            when(inventoryRepository.findByIdForUpdate("inv-1")).thenReturn(Optional.of(inventory));
            when(roleRepository.findRoleNamesByAccountId(ACTOR_ID)).thenReturn(List.of("ADMIN"));
            when(stockAdjustmentsRepository.existsByAdjustmentNumber(anyString())).thenReturn(false);
            when(stockAdjustmentsRepository.save(any())).thenAnswer(inv -> {
                StockAdjustments adj = inv.getArgument(0);
                adj.setId("adj-zero");
                return adj;
            });

            StockAdjustmentsResponse response = stockAdjustmentsService.createAdjustment(request);

            assertThat(response.getQuantityAfter()).isEqualByComparingTo("0.00");
            assertThat(response.getAdjustmentQuantity()).isEqualByComparingTo("-50.00");
        }

        @Test
        @DisplayName("Should handle large quantity values (precision 15,2)")
        void should_HandleLargeQuantities() {
            Inventory inventory = buildInventory("inv-1", "9999999999999.99", "0.00");
            StockAdjustmentsRequest request = buildAdjustmentRequest("inv-1", "9999999999998.99", ReasonType.DAMAGE);

            when(inventoryRepository.findByIdForUpdate("inv-1")).thenReturn(Optional.of(inventory));
            when(roleRepository.findRoleNamesByAccountId(ACTOR_ID)).thenReturn(List.of("ADMIN"));
            when(stockAdjustmentsRepository.existsByAdjustmentNumber(anyString())).thenReturn(false);
            when(stockAdjustmentsRepository.save(any())).thenAnswer(inv -> {
                StockAdjustments adj = inv.getArgument(0);
                adj.setId("adj-large");
                return adj;
            });

            StockAdjustmentsResponse response = stockAdjustmentsService.createAdjustment(request);

            assertThat(response.getAdjustmentQuantity()).isEqualByComparingTo("-1.00");
        }

        @Test
        @DisplayName("Should handle very small quantity adjustment (0.01)")
        void should_HandleSmallestAdjustment() {
            Inventory inventory = buildInventory("inv-1", "100.00", "0.00");
            StockAdjustmentsRequest request = buildAdjustmentRequest("inv-1", "100.01", ReasonType.COUNT_ERROR);

            when(inventoryRepository.findByIdForUpdate("inv-1")).thenReturn(Optional.of(inventory));
            when(roleRepository.findRoleNamesByAccountId(ACTOR_ID)).thenReturn(List.of("ADMIN"));
            when(stockAdjustmentsRepository.existsByAdjustmentNumber(anyString())).thenReturn(false);
            when(stockAdjustmentsRepository.save(any())).thenAnswer(inv -> {
                StockAdjustments adj = inv.getArgument(0);
                adj.setId("adj-small");
                return adj;
            });

            StockAdjustmentsResponse response = stockAdjustmentsService.createAdjustment(request);

            assertThat(response.getAdjustmentQuantity()).isEqualByComparingTo("0.01");
        }

        @Test
        @DisplayName("Should correctly map all inventory fields to adjustment")
        void should_MapAllInventoryFields_When_Creating() {
            Inventory inventory = Inventory.builder()
                    .productId("prod-42")
                    .warehouseId("wh-7")
                    .locationId("loc-A3")
                    .batchId("batch-99")
                    .onHandQuantity(new BigDecimal("200.00"))
                    .reservedQuantity(new BigDecimal("20.00"))
                    .version(0)
                    .build();
            inventory.setId("inv-special");

            StockAdjustmentsRequest request = buildAdjustmentRequest("inv-special", "250.00", ReasonType.OTHER);

            when(inventoryRepository.findByIdForUpdate("inv-special")).thenReturn(Optional.of(inventory));
            when(roleRepository.findRoleNamesByAccountId(ACTOR_ID)).thenReturn(List.of("ADMIN"));
            when(stockAdjustmentsRepository.existsByAdjustmentNumber(anyString())).thenReturn(false);
            when(stockAdjustmentsRepository.save(any())).thenAnswer(inv -> {
                StockAdjustments adj = inv.getArgument(0);
                adj.setId("adj-map");
                return adj;
            });

            StockAdjustmentsResponse response = stockAdjustmentsService.createAdjustment(request);

            assertThat(response.getProductId()).isEqualTo("prod-42");
            assertThat(response.getWarehouseId()).isEqualTo("wh-7");
            assertThat(response.getLocationId()).isEqualTo("loc-A3");
            assertThat(response.getBatchId()).isEqualTo("batch-99");
            assertThat(response.getInventoryId()).isEqualTo("inv-special");
            assertThat(response.getReason()).isEqualTo(ReasonType.OTHER);
        }
    }

    // =========================================================================
    // Concurrency & Locking verification
    // =========================================================================

    @Nested
    @DisplayName("Concurrency & Locking")
    class ConcurrencyTests {

        @Test
        @DisplayName("Should use pessimistic lock when fetching inventory for create")
        void should_UsePessimisticLock_When_FetchingInventoryForCreate() {
            Inventory inventory = buildInventory("inv-1", "100.00", "0.00");
            StockAdjustmentsRequest request = buildAdjustmentRequest("inv-1", "110.00", ReasonType.COUNT_ERROR);

            when(inventoryRepository.findByIdForUpdate("inv-1")).thenReturn(Optional.of(inventory));
            when(roleRepository.findRoleNamesByAccountId(ACTOR_ID)).thenReturn(List.of("USER"));
            when(stockAdjustmentsRepository.existsByAdjustmentNumber(anyString())).thenReturn(false);
            when(stockAdjustmentsRepository.save(any())).thenAnswer(inv -> {
                StockAdjustments adj = inv.getArgument(0);
                adj.setId("adj-lock");
                return adj;
            });

            stockAdjustmentsService.createAdjustment(request);

            // Verify pessimistic lock method was called (not regular findById)
            verify(inventoryRepository).findByIdForUpdate("inv-1");
            verify(inventoryRepository, never()).findById(anyString());
        }

        @Test
        @DisplayName("Should use pessimistic lock when fetching adjustment for approve")
        void should_UsePessimisticLock_When_FetchingAdjustmentForApprove() {
            Inventory inventory = buildInventory("inv-1", "100.00", "10.00");
            StockAdjustments adjustment = buildPendingAdjustment("adj-lock", "inv-1", "100.00", "120.00");

            when(roleRepository.findRoleNamesByAccountId(ACTOR_ID)).thenReturn(List.of("ADMIN"));
            when(stockAdjustmentsRepository.findByIdForUpdate("adj-lock")).thenReturn(Optional.of(adjustment));
            when(inventoryRepository.findByIdForUpdate("inv-1")).thenReturn(Optional.of(inventory));
            when(stockAdjustmentsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            stockAdjustmentsService.approve("adj-lock", null);

            verify(stockAdjustmentsRepository).findByIdForUpdate("adj-lock");
            verify(inventoryRepository).findByIdForUpdate("inv-1");
        }

        @Test
        @DisplayName("Should use pessimistic lock when fetching adjustment for reject")
        void should_UsePessimisticLock_When_FetchingAdjustmentForReject() {
            StockAdjustments adjustment = buildPendingAdjustment("adj-rej-lock", "inv-1", "100.00", "90.00");

            RejectStockAdjustmentRequest rejectRequest = new RejectStockAdjustmentRequest();
            setField(rejectRequest, "rejectionReason", "Locked");

            when(roleRepository.findRoleNamesByAccountId(ACTOR_ID)).thenReturn(List.of("ADMIN"));
            when(stockAdjustmentsRepository.findByIdForUpdate("adj-rej-lock")).thenReturn(Optional.of(adjustment));
            when(stockAdjustmentsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            stockAdjustmentsService.reject("adj-rej-lock", rejectRequest);

            verify(stockAdjustmentsRepository).findByIdForUpdate("adj-rej-lock");
        }
    }

    // =========================================================================
    // Reason type variations
    // =========================================================================

    @Nested
    @DisplayName("All ReasonType variations for create")
    class ReasonTypeTests {

        @ParameterizedTest(name = "Should require approval for USER with reason {0}")
        @ValueSource(strings = {"DAMAGE", "THEFT", "COUNT_ERROR", "EXPIRED", "QUALITY_ISSUE", "OTHER"})
        @DisplayName("Should require approval for USER with any non-SYSTEM_ERROR reason")
        void should_RequireApproval_ForEachReasonType(String reasonName) {
            ReasonType reason = ReasonType.valueOf(reasonName);
            Inventory inventory = buildInventory("inv-1", "100.00", "0.00");
            StockAdjustmentsRequest request = buildAdjustmentRequest("inv-1", "80.00", reason);

            when(inventoryRepository.findByIdForUpdate("inv-1")).thenReturn(Optional.of(inventory));
            when(roleRepository.findRoleNamesByAccountId(ACTOR_ID)).thenReturn(List.of("USER"));
            when(stockAdjustmentsRepository.existsByAdjustmentNumber(anyString())).thenReturn(false);
            when(stockAdjustmentsRepository.save(any())).thenAnswer(inv -> {
                StockAdjustments adj = inv.getArgument(0);
                adj.setId("adj-" + reasonName);
                return adj;
            });

            StockAdjustmentsResponse response = stockAdjustmentsService.createAdjustment(request);

            assertThat(response.getRequiresApproval()).isTrue();
            assertThat(response.getReason()).isEqualTo(reason);
        }
    }

    // =========================================================================
    // Helper methods
    // =========================================================================

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

    private StockAdjustmentsRequest buildAdjustmentRequest(String inventoryId, String quantityAfter, ReasonType reason) {
        StockAdjustmentsRequest request = new StockAdjustmentsRequest();
        setField(request, "inventoryId", inventoryId);
        setField(request, "quantityAfter", new BigDecimal(quantityAfter));
        setField(request, "reason", reason);
        setField(request, "notes", "Test adjustment note");
        return request;
    }

    private StockAdjustments buildPendingAdjustment(String id, String inventoryId, String qtyBefore, String qtyAfter) {
        BigDecimal before = new BigDecimal(qtyBefore);
        BigDecimal after = new BigDecimal(qtyAfter);
        StockAdjustments adjustment = StockAdjustments.builder()
                .inventoryId(inventoryId)
                .productId("prod-1")
                .warehouseId("wh-1")
                .locationId("loc-1")
                .batchId("batch-1")
                .quantityBefore(before)
                .quantityAfter(after)
                .adjustmentQuantity(after.subtract(before))
                .reason(ReasonType.COUNT_ERROR)
                .status(StockAdjustmentsStatus.PENDING_APPROVAL)
                .requiresApproval(true)
                .build();
        adjustment.setId(id);
        adjustment.setAdjustmentNumber("ADJ-TEST-" + id);
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
