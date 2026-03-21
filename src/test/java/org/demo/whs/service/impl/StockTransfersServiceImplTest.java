package org.demo.whs.service.impl;

import org.demo.whs.entity.Account;
import org.demo.whs.entity.Batch;
import org.demo.whs.entity.Employee;
import org.demo.whs.entity.Inventory;
import org.demo.whs.entity.Locations;
import org.demo.whs.entity.StockMovements;
import org.demo.whs.entity.StockTransfers;
import org.demo.whs.entity.dto.request.StockTransfers.StockTransfersRequest;
import org.demo.whs.entity.dto.response.StockTransfers.StockTransfersResponse;
import org.demo.whs.entity.enums.AccountStatus;
import org.demo.whs.entity.enums.BatchStatus;
import org.demo.whs.entity.enums.LocationStatus;
import org.demo.whs.entity.enums.LocationType;
import org.demo.whs.entity.enums.StockMovementsType;
import org.demo.whs.entity.enums.StockTransfersReason;
import org.demo.whs.entity.enums.StockTransfersStatus;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.NotFoundException;
import org.demo.whs.mapper.StockMovementsMapper;
import org.demo.whs.mapper.StockTransfersMapper;
import org.demo.whs.repository.AccountRepository;
import org.demo.whs.repository.BatchRepository;
import org.demo.whs.repository.EmployeeRepository;
import org.demo.whs.repository.InventoryRepository;
import org.demo.whs.repository.LocationRepository;
import org.demo.whs.repository.ProductRepository;
import org.demo.whs.repository.StockMovementsRepository;
import org.demo.whs.repository.StockTransfersRepository;
import org.demo.whs.utils.IdentifierGenerator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.inOrder;

@ExtendWith(MockitoExtension.class)
@DisplayName("StockTransfersServiceImpl Unit Tests")
class StockTransfersServiceImplTest {

    @Mock
    private StockTransfersRepository stockTransfersRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private StockMovementsRepository stockMovementsRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private BatchRepository batchRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    private StockTransfersServiceImpl stockTransfersService;

    @BeforeEach
    void setUp() {
        stockTransfersService = new StockTransfersServiceImpl(
                stockTransfersRepository,
                inventoryRepository,
                stockMovementsRepository,
                productRepository,
                locationRepository,
                batchRepository,
                accountRepository,
                employeeRepository,
                new StockTransfersMapper(),
                new StockMovementsMapper(),
                new IdentifierGenerator()
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

        Employee employee = Employee.builder()
                .accountId("acc-1")
                .warehouseId("wh-1")
                .build();
        lenient().when(employeeRepository.findByAccountId("acc-1")).thenReturn(Optional.of(employee));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void should_CreateDraftTransfer_When_RequestValid() {
        StockTransfersRequest request = buildTransferRequest("10.00");

        Locations from = new Locations();
        from.setId("loc-1");
        from.setWarehouseId("wh-1");
        from.setStatus(LocationStatus.ACTIVE);
        from.setType(LocationType.STORAGE);

        Locations to = new Locations();
        to.setId("loc-2");
        to.setWarehouseId("wh-1");
        to.setStatus(LocationStatus.ACTIVE);
        to.setType(LocationType.STORAGE);

        when(productRepository.existsById("prod-1")).thenReturn(true);
        when(locationRepository.findById("loc-1")).thenReturn(Optional.of(from));
        when(locationRepository.findById("loc-2")).thenReturn(Optional.of(to));
        when(stockTransfersRepository.existsByTransferNumber(anyString())).thenReturn(false);
        when(stockTransfersRepository.save(any(StockTransfers.class))).thenAnswer(invocation -> {
            StockTransfers transfer = invocation.getArgument(0);
            transfer.setId("trf-1");
            return transfer;
        });

        StockTransfersResponse response = stockTransfersService.createTransfer(request);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(StockTransfersStatus.DRAFT);
        assertThat(response.getQuantity()).isEqualByComparingTo("10.00");
    }

    @Test
    void should_CompleteTransferAndCreateTwoMovements_When_TransferDraftAndStockAvailable() {
        StockTransfers transfer = buildDraftTransfer("trf-2", "loc-1", "loc-2", "20.00");

        Inventory sourceInventory = buildInventory("loc-1", "100.00", "10.00");
        sourceInventory.setId("inv-src");

        Inventory destinationInventory = buildInventory("loc-2", "5.00", "0.00");
        destinationInventory.setId("inv-dst");

        when(stockTransfersRepository.findByIdForUpdate("trf-2")).thenReturn(Optional.of(transfer));
        when(inventoryRepository.findByDimensionForUpdate("prod-1", "wh-1", "loc-1", "batch-1"))
                .thenReturn(Optional.of(sourceInventory));
        when(inventoryRepository.findByDimensionForUpdate("prod-1", "wh-1", "loc-2", "batch-1"))
                .thenReturn(Optional.of(destinationInventory));
        when(stockTransfersRepository.save(any(StockTransfers.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StockTransfersResponse response = stockTransfersService.complete("trf-2");

        assertThat(response.getStatus()).isEqualTo(StockTransfersStatus.COMPLETED);
        assertThat(response.getCompletedAt()).isNotNull();
        assertThat(response.getUpdatedBy()).isEqualTo("acc-1");
        assertThat(sourceInventory.getOnHandQuantity()).isEqualByComparingTo("80.00");
        assertThat(destinationInventory.getOnHandQuantity()).isEqualByComparingTo("25.00");
        assertThat(sourceInventory.getUpdatedBy()).isEqualTo("acc-1");
        assertThat(destinationInventory.getUpdatedBy()).isEqualTo("acc-1");
        assertThat(sourceInventory.getLastMovementAt()).isNotNull();
        assertThat(destinationInventory.getLastMovementAt()).isNotNull();

        ArgumentCaptor<StockMovements> movementCaptor = ArgumentCaptor.forClass(StockMovements.class);
        verify(stockMovementsRepository, times(2)).save(movementCaptor.capture());

        assertThat(movementCaptor.getAllValues())
                .extracting(StockMovements::getMovementType)
                .containsExactlyInAnyOrder(StockMovementsType.TRANSFER_OUT, StockMovementsType.TRANSFER_IN);
    }

    @Test
    void should_ThrowBadRequest_When_CompleteTransferWithInsufficientAvailableStock() {
        StockTransfers transfer = buildDraftTransfer("trf-3", "loc-1", "loc-2", "10.00");

        Inventory sourceInventory = buildInventory("loc-1", "15.00", "10.00");

        when(stockTransfersRepository.findByIdForUpdate("trf-3")).thenReturn(Optional.of(transfer));
        when(inventoryRepository.findByDimensionForUpdate("prod-1", "wh-1", "loc-1", "batch-1"))
                .thenReturn(Optional.of(sourceInventory));
        when(inventoryRepository.findByDimensionForUpdate("prod-1", "wh-1", "loc-2", "batch-1"))
                .thenReturn(Optional.of(buildInventory("loc-2", "0.00", "0.00")));

        assertThatThrownBy(() -> stockTransfersService.complete("trf-3"))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("errorCode", "INV_004");

        verify(stockMovementsRepository, never()).save(any(StockMovements.class));
    }

    @Test
    void should_ThrowBadRequest_When_CompleteTransferWithQuarantineReducingAvailableStock() {
        StockTransfers transfer = buildDraftTransfer("trf-quarantine", "loc-1", "loc-2", "10.00");

        Inventory sourceInventory = buildInventory("loc-1", "15.00", "0.00");
        sourceInventory.setQuarantineQuantity(new BigDecimal("8.00"));

        when(stockTransfersRepository.findByIdForUpdate("trf-quarantine")).thenReturn(Optional.of(transfer));
        when(inventoryRepository.findByDimensionForUpdate("prod-1", "wh-1", "loc-1", "batch-1"))
                .thenReturn(Optional.of(sourceInventory));
        when(inventoryRepository.findByDimensionForUpdate("prod-1", "wh-1", "loc-2", "batch-1"))
                .thenReturn(Optional.of(buildInventory("loc-2", "0.00", "0.00")));

        assertThatThrownBy(() -> stockTransfersService.complete("trf-quarantine"))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("errorCode", "INV_004");

        verify(stockMovementsRepository, never()).save(any(StockMovements.class));
    }

    @Test
    void should_ThrowBadRequest_When_CompleteTransferWithNonPositiveQuantity() {
        StockTransfers transfer = buildDraftTransfer("trf-qty", "loc-1", "loc-2", "0.00");

        when(stockTransfersRepository.findByIdForUpdate("trf-qty")).thenReturn(Optional.of(transfer));

        assertThatThrownBy(() -> stockTransfersService.complete("trf-qty"))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("errorCode", "STF_003");

        verifyNoInteractions(inventoryRepository, stockMovementsRepository);
    }

    @Test
    void should_CancelDraftTransfer_When_StatusIsDraft() {
        StockTransfers transfer = StockTransfers.builder()
                .status(StockTransfersStatus.DRAFT)
                .build();
        transfer.setId("trf-4");

        when(stockTransfersRepository.findByIdForUpdate("trf-4")).thenReturn(Optional.of(transfer));
        when(stockTransfersRepository.save(any(StockTransfers.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StockTransfersResponse response = stockTransfersService.cancel("trf-4");

        assertThat(response.getStatus()).isEqualTo(StockTransfersStatus.CANCELLED);
        verify(stockMovementsRepository, never()).save(any(StockMovements.class));
    }

    @Test
    void should_ThrowBadRequest_When_CompleteTransferInInvalidStatus() {
        StockTransfers transfer = StockTransfers.builder()
                .status(StockTransfersStatus.COMPLETED)
                .build();
        transfer.setId("trf-5");

        when(stockTransfersRepository.findByIdForUpdate("trf-5")).thenReturn(Optional.of(transfer));

        assertThatThrownBy(() -> stockTransfersService.complete("trf-5"))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("errorCode", "STF_002");
    }

    @Test
    void should_CreateDestinationInventory_When_DestinationInventoryMissingAndSortedFirst() {
        StockTransfers transfer = buildDraftTransfer("trf-6", "loc-2", "loc-1", "10.00");

        Inventory sourceInventory = buildInventory("loc-2", "50.00", "5.00");
        Inventory createdDestinationInventory = buildInventory("loc-1", "0.00", "0.00");
        createdDestinationInventory.setId("inv-dst");

        when(stockTransfersRepository.findByIdForUpdate("trf-6")).thenReturn(Optional.of(transfer));
        when(inventoryRepository.findByDimensionForUpdate("prod-1", "wh-1", "loc-1", "batch-1"))
                .thenReturn(Optional.empty());
        when(inventoryRepository.findByDimensionForUpdate("prod-1", "wh-1", "loc-2", "batch-1"))
                .thenReturn(Optional.of(sourceInventory));
        when(inventoryRepository.saveAndFlush(any(Inventory.class))).thenReturn(createdDestinationInventory);
        when(stockTransfersRepository.save(any(StockTransfers.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StockTransfersResponse response = stockTransfersService.complete("trf-6");

        assertThat(response.getStatus()).isEqualTo(StockTransfersStatus.COMPLETED);
        assertThat(sourceInventory.getOnHandQuantity()).isEqualByComparingTo("40.00");
        assertThat(createdDestinationInventory.getOnHandQuantity()).isEqualByComparingTo("10.00");

        InOrder inOrder = inOrder(inventoryRepository);
        inOrder.verify(inventoryRepository).findByDimensionForUpdate("prod-1", "wh-1", "loc-1", "batch-1");
        inOrder.verify(inventoryRepository).findByDimensionForUpdate("prod-1", "wh-1", "loc-2", "batch-1");
    }

    @Test
    void should_ReloadDestinationInventory_When_ConcurrentCreationOccurs() {
        StockTransfers transfer = buildDraftTransfer("trf-7", "loc-2", "loc-1", "10.00");

        Inventory sourceInventory = buildInventory("loc-2", "60.00", "5.00");
        Inventory destinationInventory = buildInventory("loc-1", "15.00", "0.00");
        destinationInventory.setId("inv-dst");

        when(stockTransfersRepository.findByIdForUpdate("trf-7")).thenReturn(Optional.of(transfer));
        when(inventoryRepository.findByDimensionForUpdate("prod-1", "wh-1", "loc-1", "batch-1"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(destinationInventory));
        when(inventoryRepository.findByDimensionForUpdate("prod-1", "wh-1", "loc-2", "batch-1"))
                .thenReturn(Optional.of(sourceInventory));
        when(inventoryRepository.saveAndFlush(any(Inventory.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate inventory"));
        when(stockTransfersRepository.save(any(StockTransfers.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StockTransfersResponse response = stockTransfersService.complete("trf-7");

        assertThat(response.getStatus()).isEqualTo(StockTransfersStatus.COMPLETED);
        assertThat(sourceInventory.getOnHandQuantity()).isEqualByComparingTo("50.00");
        assertThat(destinationInventory.getOnHandQuantity()).isEqualByComparingTo("25.00");
    }

    @Test
    void should_ThrowNotFound_When_SourceInventoryMissing() {
        StockTransfers transfer = buildDraftTransfer("trf-8", "loc-2", "loc-1", "10.00");

        when(stockTransfersRepository.findByIdForUpdate("trf-8")).thenReturn(Optional.of(transfer));
        when(inventoryRepository.findByDimensionForUpdate("prod-1", "wh-1", "loc-1", "batch-1"))
                .thenReturn(Optional.of(buildInventory("loc-1", "5.00", "0.00")));
        when(inventoryRepository.findByDimensionForUpdate("prod-1", "wh-1", "loc-2", "batch-1"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> stockTransfersService.complete("trf-8"))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("errorCode", "INV_001");

        verify(stockMovementsRepository, never()).save(any(StockMovements.class));
    }

    @Test
    void should_ThrowBadRequest_When_BatchDoesNotBelongToProductOnCreate() {
        StockTransfersRequest request = buildTransferRequest("5.00");
        setField(request, "batchId", "batch-1");

        Locations from = new Locations();
        from.setWarehouseId("wh-1");
        from.setStatus(LocationStatus.ACTIVE);
        from.setType(LocationType.STORAGE);

        Locations to = new Locations();
        to.setWarehouseId("wh-1");
        to.setStatus(LocationStatus.ACTIVE);
        to.setType(LocationType.STORAGE);

        Batch batch = Batch.builder()
                .productId("another-product")
                .status(BatchStatus.AVAILABLE)
                .build();
        batch.setId("batch-1");

        when(productRepository.existsById("prod-1")).thenReturn(true);
        when(locationRepository.findById("loc-1")).thenReturn(Optional.of(from));
        when(locationRepository.findById("loc-2")).thenReturn(Optional.of(to));
        when(batchRepository.findById("batch-1")).thenReturn(Optional.of(batch));

        assertThatThrownBy(() -> stockTransfersService.createTransfer(request))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("errorCode", "STF_002");
    }

    @Test
    void should_ThrowBadRequest_When_UserAccessWarehouseTheyDoNotBelongTo() {
        StockTransfersRequest request = buildTransferRequest("5.00");
        setField(request, "warehouseId", "wh-different");

        assertThatThrownBy(() -> stockTransfersService.createTransfer(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("You do not have permission to access this warehouse")
                .hasFieldOrPropertyWithValue("errorCode", "AUTH_003");
    }

    @Test
    void should_ThrowBadRequest_When_EmployeeNotFoundForCurrentUser() {
        when(employeeRepository.findByAccountId("acc-1")).thenReturn(Optional.empty());

        StockTransfersRequest request = buildTransferRequest("5.00");

        assertThatThrownBy(() -> stockTransfersService.createTransfer(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Employee not found for current user")
                .hasFieldOrPropertyWithValue("errorCode", "AUTH_003");
    }

    @Test
    void should_ThrowBadRequest_When_FromLocationIsInactive() {
        StockTransfersRequest request = buildTransferRequest("5.00");

        Locations from = new Locations();
        from.setId("loc-1");
        from.setWarehouseId("wh-1");
        from.setStatus(LocationStatus.INACTIVE);
        from.setType(LocationType.STORAGE);

        Locations to = new Locations();
        to.setId("loc-2");
        to.setWarehouseId("wh-1");
        to.setStatus(LocationStatus.ACTIVE);
        to.setType(LocationType.STORAGE);

        when(productRepository.existsById("prod-1")).thenReturn(true);
        when(locationRepository.findById("loc-1")).thenReturn(Optional.of(from));
        when(locationRepository.findById("loc-2")).thenReturn(Optional.of(to));

        assertThatThrownBy(() -> stockTransfersService.createTransfer(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Source location is not active")
                .hasFieldOrPropertyWithValue("errorCode", "LOC_007");
    }

    @Test
    void should_ThrowBadRequest_When_ToLocationIsInactive() {
        StockTransfersRequest request = buildTransferRequest("5.00");

        Locations from = new Locations();
        from.setId("loc-1");
        from.setWarehouseId("wh-1");
        from.setStatus(LocationStatus.ACTIVE);
        from.setType(LocationType.STORAGE);

        Locations to = new Locations();
        to.setId("loc-2");
        to.setWarehouseId("wh-1");
        to.setStatus(LocationStatus.MAINTENANCE);
        to.setType(LocationType.STORAGE);

        when(productRepository.existsById("prod-1")).thenReturn(true);
        when(locationRepository.findById("loc-1")).thenReturn(Optional.of(from));
        when(locationRepository.findById("loc-2")).thenReturn(Optional.of(to));

        assertThatThrownBy(() -> stockTransfersService.createTransfer(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Destination location is not active")
                .hasFieldOrPropertyWithValue("errorCode", "LOC_007");
    }

    @Test
    void should_ThrowBadRequest_When_FromLocationTypeIsNotValidForTransfer() {
        StockTransfersRequest request = buildTransferRequest("5.00");

        Locations from = new Locations();
        from.setId("loc-1");
        from.setWarehouseId("wh-1");
        from.setStatus(LocationStatus.ACTIVE);
        from.setType(LocationType.RETURN);

        Locations to = new Locations();
        to.setId("loc-2");
        to.setWarehouseId("wh-1");
        to.setStatus(LocationStatus.ACTIVE);
        to.setType(LocationType.STORAGE);

        when(productRepository.existsById("prod-1")).thenReturn(true);
        when(locationRepository.findById("loc-1")).thenReturn(Optional.of(from));
        when(locationRepository.findById("loc-2")).thenReturn(Optional.of(to));

        assertThatThrownBy(() -> stockTransfersService.createTransfer(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Source location type is not valid")
                .hasFieldOrPropertyWithValue("errorCode", "LOC_008");
    }

    @Test
    void should_ThrowBadRequest_When_BatchIsQuarantined() {
        StockTransfersRequest request = buildTransferRequest("5.00");
        setField(request, "batchId", "batch-1");

        Locations from = new Locations();
        from.setId("loc-1");
        from.setWarehouseId("wh-1");
        from.setStatus(LocationStatus.ACTIVE);
        from.setType(LocationType.STORAGE);

        Locations to = new Locations();
        to.setId("loc-2");
        to.setWarehouseId("wh-1");
        to.setStatus(LocationStatus.ACTIVE);
        to.setType(LocationType.STORAGE);

        Batch batch = Batch.builder()
                .productId("prod-1")
                .status(BatchStatus.QUARANTINE)
                .build();
        batch.setId("batch-1");

        when(productRepository.existsById("prod-1")).thenReturn(true);
        when(locationRepository.findById("loc-1")).thenReturn(Optional.of(from));
        when(locationRepository.findById("loc-2")).thenReturn(Optional.of(to));
        when(batchRepository.findById("batch-1")).thenReturn(Optional.of(batch));

        assertThatThrownBy(() -> stockTransfersService.createTransfer(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Batch is not available")
                .hasFieldOrPropertyWithValue("errorCode", "BATCH_012");
    }

    @Test
    void should_ThrowBadRequest_When_BatchIsExpired() {
        StockTransfersRequest request = buildTransferRequest("5.00");
        setField(request, "batchId", "batch-1");

        Locations from = new Locations();
        from.setId("loc-1");
        from.setWarehouseId("wh-1");
        from.setStatus(LocationStatus.ACTIVE);
        from.setType(LocationType.STORAGE);

        Locations to = new Locations();
        to.setId("loc-2");
        to.setWarehouseId("wh-1");
        to.setStatus(LocationStatus.ACTIVE);
        to.setType(LocationType.STORAGE);

        Batch batch = Batch.builder()
                .productId("prod-1")
                .status(BatchStatus.AVAILABLE)
                .expiryDate(LocalDate.now().minusDays(1))
                .build();
        batch.setId("batch-1");

        when(productRepository.existsById("prod-1")).thenReturn(true);
        when(locationRepository.findById("loc-1")).thenReturn(Optional.of(from));
        when(locationRepository.findById("loc-2")).thenReturn(Optional.of(to));
        when(batchRepository.findById("batch-1")).thenReturn(Optional.of(batch));

        assertThatThrownBy(() -> stockTransfersService.createTransfer(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Batch has expired")
                .hasFieldOrPropertyWithValue("errorCode", "BATCH_020");
    }

    @Test
    void should_ThrowBadRequest_When_BatchIsRecalled() {
        StockTransfersRequest request = buildTransferRequest("5.00");
        setField(request, "batchId", "batch-1");

        Locations from = new Locations();
        from.setId("loc-1");
        from.setWarehouseId("wh-1");
        from.setStatus(LocationStatus.ACTIVE);
        from.setType(LocationType.STORAGE);

        Locations to = new Locations();
        to.setId("loc-2");
        to.setWarehouseId("wh-1");
        to.setStatus(LocationStatus.ACTIVE);
        to.setType(LocationType.STORAGE);

        Batch batch = Batch.builder()
                .productId("prod-1")
                .status(BatchStatus.RECALLED)
                .build();
        batch.setId("batch-1");

        when(productRepository.existsById("prod-1")).thenReturn(true);
        when(locationRepository.findById("loc-1")).thenReturn(Optional.of(from));
        when(locationRepository.findById("loc-2")).thenReturn(Optional.of(to));
        when(batchRepository.findById("batch-1")).thenReturn(Optional.of(batch));

        assertThatThrownBy(() -> stockTransfersService.createTransfer(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Batch is not available")
                .hasFieldOrPropertyWithValue("errorCode", "BATCH_012");
    }

    @Test
    void should_ThrowBadRequest_When_LocationIsFull() {
        StockTransfersRequest request = buildTransferRequest("5.00");

        Locations from = new Locations();
        from.setId("loc-1");
        from.setWarehouseId("wh-1");
        from.setStatus(LocationStatus.FULL);
        from.setType(LocationType.STORAGE);

        Locations to = new Locations();
        to.setId("loc-2");
        to.setWarehouseId("wh-1");
        to.setStatus(LocationStatus.ACTIVE);
        to.setType(LocationType.STORAGE);

        when(productRepository.existsById("prod-1")).thenReturn(true);
        when(locationRepository.findById("loc-1")).thenReturn(Optional.of(from));
        when(locationRepository.findById("loc-2")).thenReturn(Optional.of(to));

        assertThatThrownBy(() -> stockTransfersService.createTransfer(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Source location is not active")
                .hasFieldOrPropertyWithValue("errorCode", "LOC_007");
    }

    private StockTransfersRequest buildTransferRequest(String quantity) {
        StockTransfersRequest request = new StockTransfersRequest();
        setField(request, "productId", "prod-1");
        setField(request, "warehouseId", "wh-1");
        setField(request, "fromLocationId", "loc-1");
        setField(request, "toLocationId", "loc-2");
        setField(request, "quantity", new BigDecimal(quantity));
        setField(request, "reason", StockTransfersReason.REORG);
        setField(request, "notes", "Move stock");
        return request;
    }

    private StockTransfers buildDraftTransfer(String id, String fromLocationId, String toLocationId, String quantity) {
        StockTransfers transfer = StockTransfers.builder()
                .transferNumber("TRF-" + id)
                .productId("prod-1")
                .warehouseId("wh-1")
                .fromLocationId(fromLocationId)
                .toLocationId(toLocationId)
                .batchId("batch-1")
                .quantity(new BigDecimal(quantity))
                .reason(StockTransfersReason.REORG)
                .status(StockTransfersStatus.DRAFT)
                .build();
        transfer.setId(id);
        transfer.setCreatedAt(LocalDateTime.now());
        return transfer;
    }

    private Inventory buildInventory(String locationId, String onHand, String reserved) {
        return Inventory.builder()
                .productId("prod-1")
                .warehouseId("wh-1")
                .locationId(locationId)
                .batchId("batch-1")
                .onHandQuantity(new BigDecimal(onHand))
                .reservedQuantity(new BigDecimal(reserved))
                .version(0)
                .build();
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

