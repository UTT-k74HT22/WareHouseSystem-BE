package org.demo.whs.service.impl;

import org.demo.whs.entity.Account;
import org.demo.whs.entity.Batch;
import org.demo.whs.entity.Inventory;
import org.demo.whs.entity.Locations;
import org.demo.whs.entity.StockMovements;
import org.demo.whs.entity.StockTransfers;
import org.demo.whs.entity.dto.request.StockTransfers.StockTransfersRequest;
import org.demo.whs.entity.dto.response.StockTransfers.StockTransfersResponse;
import org.demo.whs.entity.enums.AccountStatus;
import org.demo.whs.entity.enums.StockMovementsType;
import org.demo.whs.entity.enums.StockTransfersReason;
import org.demo.whs.entity.enums.StockTransfersStatus;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.mapper.StockMovementsMapper;
import org.demo.whs.mapper.StockTransfersMapper;
import org.demo.whs.repository.AccountRepository;
import org.demo.whs.repository.BatchRepository;
import org.demo.whs.repository.InventoryRepository;
import org.demo.whs.repository.LocationRepository;
import org.demo.whs.repository.ProductRepository;
import org.demo.whs.repository.StockMovementsRepository;
import org.demo.whs.repository.StockTransfersRepository;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
                new StockTransfersMapper(),
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
    void should_CreateDraftTransfer_When_RequestValid() {
        StockTransfersRequest request = buildTransferRequest("10.00");

        Locations from = new Locations();
        from.setId("loc-1");
        from.setWarehouseId("wh-1");

        Locations to = new Locations();
        to.setId("loc-2");
        to.setWarehouseId("wh-1");

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
        StockTransfers transfer = StockTransfers.builder()
                .transferNumber("TRF-001")
                .productId("prod-1")
                .warehouseId("wh-1")
                .fromLocationId("loc-1")
                .toLocationId("loc-2")
                .batchId("batch-1")
                .quantity(new BigDecimal("20.00"))
                .reason(StockTransfersReason.REORG)
                .status(StockTransfersStatus.DRAFT)
                .build();
        transfer.setId("trf-2");

        Inventory sourceInventory = Inventory.builder()
                .productId("prod-1")
                .warehouseId("wh-1")
                .locationId("loc-1")
                .batchId("batch-1")
                .onHandQuantity(new BigDecimal("100.00"))
                .reservedQuantity(new BigDecimal("10.00"))
                .version(0)
                .build();
        sourceInventory.setId("inv-src");

        Inventory destinationInventory = Inventory.builder()
                .productId("prod-1")
                .warehouseId("wh-1")
                .locationId("loc-2")
                .batchId("batch-1")
                .onHandQuantity(new BigDecimal("5.00"))
                .reservedQuantity(BigDecimal.ZERO)
                .version(0)
                .build();
        destinationInventory.setId("inv-dst");

        when(stockTransfersRepository.findByIdForUpdate("trf-2")).thenReturn(Optional.of(transfer));
        when(inventoryRepository.findByDimensionForUpdate("prod-1", "wh-1", "loc-1", "batch-1"))
                .thenReturn(Optional.of(sourceInventory));
        when(inventoryRepository.findByDimensionForUpdate("prod-1", "wh-1", "loc-2", "batch-1"))
                .thenReturn(Optional.of(destinationInventory));
        when(stockTransfersRepository.save(any(StockTransfers.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StockTransfersResponse response = stockTransfersService.complete("trf-2");

        assertThat(response.getStatus()).isEqualTo(StockTransfersStatus.COMPLETED);
        assertThat(sourceInventory.getOnHandQuantity()).isEqualByComparingTo("80.00");
        assertThat(destinationInventory.getOnHandQuantity()).isEqualByComparingTo("25.00");

        ArgumentCaptor<StockMovements> movementCaptor = ArgumentCaptor.forClass(StockMovements.class);
        verify(stockMovementsRepository, times(2)).save(movementCaptor.capture());

        assertThat(movementCaptor.getAllValues())
                .extracting(StockMovements::getMovementType)
                .containsExactlyInAnyOrder(StockMovementsType.TRANSFER_OUT, StockMovementsType.TRANSFER_IN);
    }

    @Test
    void should_ThrowBadRequest_When_CompleteTransferWithInsufficientAvailableStock() {
        StockTransfers transfer = StockTransfers.builder()
                .productId("prod-1")
                .warehouseId("wh-1")
                .fromLocationId("loc-1")
                .toLocationId("loc-2")
                .batchId("batch-1")
                .quantity(new BigDecimal("10.00"))
                .status(StockTransfersStatus.DRAFT)
                .build();
        transfer.setId("trf-3");

        Inventory sourceInventory = Inventory.builder()
                .productId("prod-1")
                .warehouseId("wh-1")
                .locationId("loc-1")
                .batchId("batch-1")
                .onHandQuantity(new BigDecimal("15.00"))
                .reservedQuantity(new BigDecimal("10.00"))
                .version(0)
                .build();

        when(stockTransfersRepository.findByIdForUpdate("trf-3")).thenReturn(Optional.of(transfer));
        when(inventoryRepository.findByDimensionForUpdate("prod-1", "wh-1", "loc-1", "batch-1"))
                .thenReturn(Optional.of(sourceInventory));
        when(inventoryRepository.findByDimensionForUpdate("prod-1", "wh-1", "loc-2", "batch-1"))
                .thenReturn(Optional.of(Inventory.builder()
                        .productId("prod-1")
                        .warehouseId("wh-1")
                        .locationId("loc-2")
                        .batchId("batch-1")
                        .onHandQuantity(BigDecimal.ZERO)
                        .reservedQuantity(BigDecimal.ZERO)
                        .version(0)
                        .build()));

        assertThatThrownBy(() -> stockTransfersService.complete("trf-3"))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("errorCode", "INV_004");

        verify(stockMovementsRepository, never()).save(any(StockMovements.class));
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
    void should_ThrowBadRequest_When_BatchDoesNotBelongToProductOnCreate() {
        StockTransfersRequest request = buildTransferRequest("5.00");
        setField(request, "batchId", "batch-1");

        Locations from = new Locations();
        from.setWarehouseId("wh-1");

        Locations to = new Locations();
        to.setWarehouseId("wh-1");

        Batch batch = Batch.builder()
                .productId("another-product")
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
