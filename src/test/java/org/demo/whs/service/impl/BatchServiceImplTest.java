package org.demo.whs.service.impl;

import org.demo.whs.entity.Account;
import org.demo.whs.entity.Batch;
import org.demo.whs.entity.Products;
import org.demo.whs.entity.dto.request.Batch.CreateBatchRequest;
import org.demo.whs.entity.dto.response.Batch.BatchResponse;
import org.demo.whs.entity.enums.BatchStatus;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.NotFoundException;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.mapper.BatchMapper;
import org.demo.whs.repository.AccountRepository;
import org.demo.whs.repository.BatchRepository;
import org.demo.whs.repository.InventoryRepository;
import org.demo.whs.repository.ProductRepository;
import org.demo.whs.security.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BatchServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private BatchRepository batchRepository;

    @Mock
    private BatchMapper batchMapper;

    @InjectMocks
    private BatchServiceImpl batchService;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private AccountRepository accountRepository;

        @Test
        void createBatch_success() {

            CreateBatchRequest request = new CreateBatchRequest();
            request.setProductId("P1");
            request.setBatchNumber("B001");
            request.setManufacturingDate(LocalDate.now().minusDays(1));
            request.setExpiryDate(LocalDate.now().plusDays(10));

            Products product = new Products();
            product.setId("P1");
            product.setRequiresBatchTracking(true);

            Batch batch = new Batch();
            Batch savedBatch = new Batch();
            savedBatch.setId("BATCH_1");

            BatchResponse response = BatchResponse.builder()
                    .id("BATCH_1")
                    .build();

            when(productRepository.findById("P1")).thenReturn(Optional.of(product));
            when(batchRepository.existsByProductIdAndBatchNumber("P1", "B001")).thenReturn(false);
            when(batchMapper.createEntity(request)).thenReturn(batch);
            when(batchRepository.save(batch)).thenReturn(savedBatch);
            when(batchMapper.toResponse(savedBatch)).thenReturn(response);

            BatchResponse result = batchService.createBatch(request);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo("BATCH_1");

            verify(batchRepository).save(batch);
        }

    @Test
    void createBatch_productNotFound() {

        CreateBatchRequest request = new CreateBatchRequest();
        request.setProductId("P1");

        when(productRepository.findById("P1"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> batchService.createBatch(request))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining(ErrorCode.PROD_001.getMessage());
    }

    @Test
    void createBatch_productBatchTrackingDisabled() {

        CreateBatchRequest request = new CreateBatchRequest();
        request.setProductId("P1");

        Products product = new Products();
        product.setRequiresBatchTracking(false);

        when(productRepository.findById("P1"))
                .thenReturn(Optional.of(product));

        assertThatThrownBy(() -> batchService.createBatch(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining(ErrorCode.BATCH_009.getMessage());
    }

    @Test
    void createBatch_duplicateBatchNumber() {

        CreateBatchRequest request = new CreateBatchRequest();
        request.setProductId("P1");
        request.setBatchNumber("B001");

        Products product = new Products();
        product.setRequiresBatchTracking(true);

        when(productRepository.findById("P1"))
                .thenReturn(Optional.of(product));

        when(batchRepository.existsByProductIdAndBatchNumber("P1", "B001"))
                .thenReturn(true);

        assertThatThrownBy(() -> batchService.createBatch(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining(ErrorCode.BATCH_002.getMessage());
    }

    @Test
    void createBatch_invalidManufacturingDate() {

        CreateBatchRequest request = new CreateBatchRequest();
        request.setProductId("P1");
        request.setBatchNumber("B001");
        request.setManufacturingDate(LocalDate.now().plusDays(1));

        Products product = new Products();
        product.setRequiresBatchTracking(true);

        when(productRepository.findById("P1"))
                .thenReturn(Optional.of(product));

        when(batchRepository.existsByProductIdAndBatchNumber("P1", "B001"))
                .thenReturn(false);

        assertThatThrownBy(() -> batchService.createBatch(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining(ErrorCode.BATCH_005.getMessage());
    }

    @Test
    void createBatch_invalidExpiryDate() {

        CreateBatchRequest request = new CreateBatchRequest();
        request.setProductId("P1");
        request.setBatchNumber("B001");
        request.setManufacturingDate(LocalDate.now());
        request.setExpiryDate(LocalDate.now().minusDays(1));

        Products product = new Products();
        product.setRequiresBatchTracking(true);

        when(productRepository.findById("P1"))
                .thenReturn(Optional.of(product));

        when(batchRepository.existsByProductIdAndBatchNumber("P1", "B001"))
                .thenReturn(false);

        assertThatThrownBy(() -> batchService.createBatch(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining(ErrorCode.BATCH_006.getMessage());
    }

    @Test
    void quarantineBatch_success() {

        Batch batch = new Batch();
        batch.setId("B1");
        batch.setBatchNumber("BATCH_01");
        batch.setStatus(BatchStatus.AVAILABLE);

        Account account = new Account();
        account.setId("A1");
        account.setUsername("admin");

        BatchResponse response = BatchResponse.builder()
                .id("B1")
                .build();

        when(batchRepository.findById("B1")).thenReturn(Optional.of(batch));
        when(inventoryRepository.existsReservedStockByBatchId("B1")).thenReturn(false);

        try (var mocked = mockStatic(SecurityUtils.class)) {

            mocked.when(SecurityUtils::getCurrentUsername).thenReturn("admin");

            when(accountRepository.findByUsername("admin"))
                    .thenReturn(Optional.of(account));

            when(batchRepository.save(batch)).thenReturn(batch);
            when(batchMapper.toResponse(batch)).thenReturn(response);

            BatchResponse result = batchService.quarantineBatch("B1");

            assertThat(result).isNotNull();
            assertThat(batch.getStatus()).isEqualTo(BatchStatus.QUARANTINE);

            verify(batchRepository).save(batch);
        }
    }

    @Test
    void quarantineBatch_batchNotFound() {

        when(batchRepository.findById("B1"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> batchService.quarantineBatch("B1"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining(ErrorCode.BATCH_001.getMessage());
    }

    @Test
    void quarantineBatch_alreadyQuarantine() {

        Batch batch = new Batch();
        batch.setId("B1");
        batch.setStatus(BatchStatus.QUARANTINE);

        when(batchRepository.findById("B1"))
                .thenReturn(Optional.of(batch));

        assertThatThrownBy(() -> batchService.quarantineBatch("B1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining(ErrorCode.BATCH_013.getMessage());
    }

    @Test
    void quarantineBatch_hasReservedStock() {

        Batch batch = new Batch();
        batch.setId("B1");
        batch.setStatus(BatchStatus.AVAILABLE);

        when(batchRepository.findById("B1"))
                .thenReturn(Optional.of(batch));

        when(inventoryRepository.existsReservedStockByBatchId("B1"))
                .thenReturn(true);

        assertThatThrownBy(() -> batchService.quarantineBatch("B1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining(ErrorCode.BATCH_007.getMessage());
    }
}
