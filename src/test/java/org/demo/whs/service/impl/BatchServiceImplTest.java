package org.demo.whs.service.impl;

import org.demo.whs.entity.Account;
import org.demo.whs.entity.Batch;
import org.demo.whs.entity.Products;
import org.demo.whs.entity.dto.request.Batch.ChangeBatchStatusRequest;
import org.demo.whs.entity.dto.request.Batch.CreateBatchRequest;
import org.demo.whs.entity.dto.request.Batch.QuarantineBatchRequest;
import org.demo.whs.entity.dto.request.Batch.ReleaseBatchRequest;
import org.demo.whs.entity.dto.request.Batch.UpdateBatchRequest;
import org.demo.whs.entity.dto.response.Batch.BatchResponse;
import org.demo.whs.entity.enums.BatchStatus;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.exception.NotFoundException;
import org.demo.whs.mapper.BatchMapper;
import org.demo.whs.repository.AccountRepository;
import org.demo.whs.repository.BatchRepository;
import org.demo.whs.repository.InventoryRepository;
import org.demo.whs.repository.ProductRepository;
import org.demo.whs.security.SecurityUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

        Account account = new Account();
        account.setId("A1");
        account.setUsername("admin");

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

        try (var mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUsername).thenReturn("admin");
            when(accountRepository.findByUsername("admin")).thenReturn(Optional.of(account));

            BatchResponse result = batchService.createBatch(request);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo("BATCH_1");
            assertThat(batch.getStatus()).isEqualTo(BatchStatus.AVAILABLE);
            assertThat(batch.getCreatedBy()).isEqualTo("A1");
            assertThat(batch.getUpdatedBy()).isEqualTo("A1");

            verify(batchRepository).save(batch);
        }
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
    void updateBatch_success() {
        Batch batch = new Batch();
        batch.setId("B1");
        batch.setProductId("P1");
        batch.setBatchNumber("B001");
        batch.setManufacturingDate(LocalDate.now().minusDays(2));
        batch.setExpiryDate(LocalDate.now().plusDays(20));

        UpdateBatchRequest request = new UpdateBatchRequest();
        request.setBatchNumber("B002");
        request.setSupplierBatchNumber("SUP-01");
        request.setNotes("Updated notes");

        Account account = new Account();
        account.setId("A1");
        account.setUsername("admin");

        BatchResponse response = BatchResponse.builder()
                .id("B1")
                .batchNumber("B002")
                .build();

        when(batchRepository.findById("B1")).thenReturn(Optional.of(batch));
        when(batchRepository.existsByProductIdAndBatchNumberAndIdNot("P1", "B002", "B1")).thenReturn(false);
        doAnswer(invocation -> {
            UpdateBatchRequest updateRequest = invocation.getArgument(0);
            Batch target = invocation.getArgument(1);
            target.setBatchNumber(updateRequest.getBatchNumber());
            target.setSupplierBatchNumber(updateRequest.getSupplierBatchNumber());
            target.setNotes(updateRequest.getNotes());
            return null;
        }).when(batchMapper).updateEntity(any(UpdateBatchRequest.class), any(Batch.class));
        when(batchRepository.save(batch)).thenReturn(batch);
        when(batchMapper.toResponse(batch)).thenReturn(response);

        try (var mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUsername).thenReturn("admin");
            when(accountRepository.findByUsername("admin")).thenReturn(Optional.of(account));

            BatchResponse result = batchService.updateBatch("B1", request);

            assertThat(result).isNotNull();
            assertThat(batch.getBatchNumber()).isEqualTo("B002");
            assertThat(batch.getSupplierBatchNumber()).isEqualTo("SUP-01");
            assertThat(batch.getNotes()).isEqualTo("Updated notes");
            assertThat(batch.getUpdatedBy()).isEqualTo("A1");
            assertThat(batch.getUpdatedAt()).isNotNull();
        }
    }

    @Test
    void quarantineBatch_success() {

        Batch batch = new Batch();
        batch.setId("B1");
        batch.setBatchNumber("BATCH_01");
        batch.setStatus(BatchStatus.AVAILABLE);
        batch.setNotes("Existing note");

        Account account = new Account();
        account.setId("A1");
        account.setUsername("admin");

        QuarantineBatchRequest request = new QuarantineBatchRequest();
        request.setReason("Quality issue");
        request.setExpectedResolutionDate(LocalDate.now().plusDays(7));
        request.setNotifyManager(Boolean.TRUE);

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

            BatchResponse result = batchService.quarantineBatch("B1", request);

            assertThat(result).isNotNull();
            assertThat(batch.getStatus()).isEqualTo(BatchStatus.QUARANTINE);
            assertThat(batch.getNotes()).contains("[QUARANTINE] reason=Quality issue");
            assertThat(batch.getNotes()).contains("notify_manager=true");

            verify(batchRepository).save(batch);
        }
    }

    @Test
    void quarantineBatch_batchNotFound() {

        QuarantineBatchRequest request = new QuarantineBatchRequest();
        request.setReason("Quality issue");

        when(batchRepository.findById("B1"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> batchService.quarantineBatch("B1", request))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining(ErrorCode.BATCH_001.getMessage());
    }

    @Test
    void quarantineBatch_alreadyQuarantine() {

        Batch batch = new Batch();
        batch.setId("B1");
        batch.setStatus(BatchStatus.QUARANTINE);

        QuarantineBatchRequest request = new QuarantineBatchRequest();
        request.setReason("Quality issue");

        when(batchRepository.findById("B1"))
                .thenReturn(Optional.of(batch));

        assertThatThrownBy(() -> batchService.quarantineBatch("B1", request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining(ErrorCode.BATCH_013.getMessage());
    }

    @Test
    void quarantineBatch_hasReservedStock() {

        Batch batch = new Batch();
        batch.setId("B1");
        batch.setStatus(BatchStatus.AVAILABLE);

        QuarantineBatchRequest request = new QuarantineBatchRequest();
        request.setReason("Quality issue");

        when(batchRepository.findById("B1"))
                .thenReturn(Optional.of(batch));

        when(inventoryRepository.existsReservedStockByBatchId("B1"))
                .thenReturn(true);

        assertThatThrownBy(() -> batchService.quarantineBatch("B1", request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining(ErrorCode.BATCH_007.getMessage());
    }

    @Test
    void changeBatchStatus_shouldRejectGenericTransition() {
        ChangeBatchStatusRequest request = new ChangeBatchStatusRequest();

        assertThatThrownBy(() -> batchService.changeBatchStatus("B1", request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining(ErrorCode.BATCH_011.getMessage());
    }

    @Test
    void releaseBatch_success() {
        Batch batch = new Batch();
        batch.setId("B1");
        batch.setBatchNumber("BATCH_01");
        batch.setStatus(BatchStatus.QUARANTINE);
        batch.setExpiryDate(LocalDate.now().plusDays(5));

        Account account = new Account();
        account.setId("A1");
        account.setUsername("admin");

        ReleaseBatchRequest request = new ReleaseBatchRequest();
        request.setReleaseNotes("Lab result passed");

        BatchResponse response = BatchResponse.builder()
                .id("B1")
                .status(BatchStatus.AVAILABLE)
                .build();

        when(batchRepository.findById("B1")).thenReturn(Optional.of(batch));

        try (var mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUsername).thenReturn("admin");

            when(accountRepository.findByUsername("admin"))
                    .thenReturn(Optional.of(account));
            when(batchRepository.save(batch)).thenReturn(batch);
            when(batchMapper.toResponse(batch)).thenReturn(response);

            BatchResponse result = batchService.releaseBatch("B1", request);

            assertThat(result).isNotNull();
            assertThat(batch.getStatus()).isEqualTo(BatchStatus.AVAILABLE);
            assertThat(batch.getNotes()).contains("[RELEASE] release_notes=Lab result passed");
        }
    }

    @Test
    void releaseBatch_shouldThrow_WhenExpiredOnCurrentDate() {
        Batch batch = new Batch();
        batch.setId("B1");
        batch.setStatus(BatchStatus.QUARANTINE);
        batch.setExpiryDate(LocalDate.now());

        ReleaseBatchRequest request = new ReleaseBatchRequest();
        request.setReleaseNotes("Attempt release");

        when(batchRepository.findById("B1")).thenReturn(Optional.of(batch));

        assertThatThrownBy(() -> batchService.releaseBatch("B1", request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining(ErrorCode.BATCH_017.getMessage());
    }
}
