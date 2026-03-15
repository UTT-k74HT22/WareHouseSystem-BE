package org.demo.whs.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.Account;
import org.demo.whs.entity.Batch;
import org.demo.whs.entity.Products;
import org.demo.whs.entity.dto.request.Batch.ChangeBatchStatusRequest;
import org.demo.whs.entity.dto.request.Batch.CreateBatchRequest;
import org.demo.whs.entity.dto.request.Batch.QuarantineBatchRequest;
import org.demo.whs.entity.dto.request.Batch.ReleaseBatchRequest;
import org.demo.whs.entity.dto.request.Batch.UpdateBatchRequest;
import org.demo.whs.entity.dto.response.Batch.BatchResponse;
import org.demo.whs.entity.dto.response.PageResponse;
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
import org.demo.whs.service.BatchService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of the BatchService interface.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class BatchServiceImpl implements BatchService {

    private final AccountRepository accountRepository;
    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;
    private final BatchRepository batchRepository;
    private final BatchMapper batchMapper;

    @Override
    @Transactional
    public BatchResponse createBatch(CreateBatchRequest request) {

        Products product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.PROD_001));

        if (!product.getRequiresBatchTracking()) {
            throw new BadRequestException(ErrorCode.BATCH_009);
        }

        if (batchRepository.existsByProductIdAndBatchNumber(
                request.getProductId(),
                request.getBatchNumber())) {

            log.warn("Batch already exists: productId={}, batchNumber={}",
                    request.getProductId(), request.getBatchNumber());

            throw new BadRequestException(ErrorCode.BATCH_002);
        }

        if (request.getManufacturingDate() != null
                && request.getManufacturingDate().isAfter(LocalDate.now())) {
            throw new BadRequestException(ErrorCode.BATCH_005);
        }

        if (request.getManufacturingDate() != null
                && request.getExpiryDate() != null
                && request.getExpiryDate().isBefore(request.getManufacturingDate())) {
            throw new BadRequestException(ErrorCode.BATCH_006);
        }

        Batch batch = batchMapper.createEntity(request);
        batch.setStatus(BatchStatus.AVAILABLE);

        Batch savedBatch = batchRepository.save(batch);

        log.info("Batch created successfully with ID={}", savedBatch.getId());

        return batchMapper.toResponse(savedBatch);
    }

    @Override
    @Transactional
    public BatchResponse getBatchById(String id) {
        log.info("Fetching batch by ID: {}", id);
        Batch batch = batchRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Batch not found with id={}", id);
                    return new NotFoundException(ErrorCode.BATCH_001);
                });

        return batchMapper.toResponse(batch);
    }

    @Override
    @Transactional
    public PageResponse<BatchResponse> getAllBatches(Integer page, Integer size) {
        log.info("Fetching all batches - page={}, size={}", page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Batch> batchPage = batchRepository.findAll(pageable);
        List<BatchResponse> responses = batchPage.getContent()
                .stream()
                .map(batchMapper::toResponse)
                .collect(Collectors.toList());

        return PageResponse.from(batchPage, responses);
    }

    @Override
    @Transactional
    public BatchResponse changeBatchStatus(String id, ChangeBatchStatusRequest request) {
        log.warn("Blocked generic batch status change for batchId={} targetStatus={}", id, request.getStatus());
        throw new BadRequestException(ErrorCode.BATCH_011);
    }

    @Override
    @Transactional
    public BatchResponse updateBatch(String id, UpdateBatchRequest request) {
        Batch batch = batchRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorCode.BATCH_001));

        if (request.getBatchNumber() != null && batchRepository.existsByProductIdAndBatchNumberAndIdNot(
                batch.getProductId(),
                request.getBatchNumber(),
                id
        )) {
            throw new BadRequestException(ErrorCode.BATCH_002);
        }

        LocalDate manufacturingDate = request.getManufacturingDate() != null
                ? request.getManufacturingDate()
                : batch.getManufacturingDate();

        LocalDate expiryDate = request.getExpiryDate() != null
                ? request.getExpiryDate()
                : batch.getExpiryDate();

        if (manufacturingDate != null && manufacturingDate.isAfter(LocalDate.now())) {
            throw new BadRequestException(ErrorCode.BATCH_005);
        }

        if (expiryDate != null && manufacturingDate != null && expiryDate.isBefore(manufacturingDate)) {
            throw new BadRequestException(ErrorCode.BATCH_006);
        }

        batchMapper.updateEntity(request, batch);

        Batch updateBatch = batchRepository.save(batch);

        return batchMapper.toResponse(updateBatch);
    }

    @Override
    @Transactional
    public BatchResponse quarantineBatch(String id, QuarantineBatchRequest request) {
        Batch batch = batchRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorCode.BATCH_001));

        validateQuarantine(batch);

        Account currentUser = getCurrentUser();

        batch.setStatus(BatchStatus.QUARANTINE);
        batch.setNotes(appendWorkflowNote(batch.getNotes(), buildQuarantineAuditNote(request)));

        setAuditFieldsForUpdate(batch, currentUser);

        Batch saveBatch = batchRepository.save(batch);

        log.info("Batch {} moved to QUARANTINE by user {}",
                batch.getBatchNumber(), currentUser.getUsername());

        return batchMapper.toResponse(saveBatch);
    }

    @Override
    @Transactional
    public BatchResponse releaseBatch(String id, ReleaseBatchRequest request) {

        Batch batch = batchRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorCode.BATCH_001));

        validateRelease(batch);

        Account user = getCurrentUser();

        batch.setStatus(BatchStatus.AVAILABLE);
        batch.setNotes(appendWorkflowNote(batch.getNotes(), buildReleaseAuditNote(request)));

        setAuditFieldsForUpdate(batch, user);

        Batch savedBatch = batchRepository.save(batch);

        return batchMapper.toResponse(savedBatch);
    }

    /**
     * Validate business rules for quarantine
     */
    private void validateQuarantine(Batch batch) {

        if (batch.getStatus() == BatchStatus.QUARANTINE) {
            throw new BadRequestException(ErrorCode.BATCH_013);
        }

        if (batch.getStatus() == BatchStatus.EXPIRED) {
            throw new BadRequestException(ErrorCode.BATCH_004);
        }

        if (batch.getStatus() == BatchStatus.RECALLED) {
            throw new BadRequestException(ErrorCode.BATCH_014);
        }

        if (batch.getStatus() != BatchStatus.AVAILABLE) {
            throw new BadRequestException(ErrorCode.BATCH_015);
        }

        boolean hasReservedStock = inventoryRepository.existsReservedStockByBatchId(batch.getId());

        if (hasReservedStock) {
            throw new BadRequestException(ErrorCode.BATCH_007);
        }
    }

    /**
     * Validate business rules for release
     */
    private void validateRelease(Batch batch) {

        if (batch.getStatus() == BatchStatus.RECALLED) {
            throw new BadRequestException(ErrorCode.BATCH_018);
        }

        if (batch.getStatus() != BatchStatus.QUARANTINE) {
            throw new BadRequestException(ErrorCode.BATCH_016);
        }

        if (batch.getExpiryDate() != null
                && !batch.getExpiryDate().isAfter(LocalDate.now())) {
            throw new BadRequestException(ErrorCode.BATCH_017);
        }
    }

    private String appendWorkflowNote(String existingNotes, String workflowNote) {
        if (workflowNote == null || workflowNote.isBlank()) {
            return existingNotes;
        }
        if (existingNotes == null || existingNotes.isBlank()) {
            return workflowNote;
        }
        return existingNotes + System.lineSeparator() + workflowNote;
    }

    private String buildQuarantineAuditNote(QuarantineBatchRequest request) {
        StringBuilder note = new StringBuilder("[QUARANTINE] reason=")
                .append(request.getReason());

        if (request.getExpectedResolutionDate() != null) {
            note.append("; expected_resolution_date=").append(request.getExpectedResolutionDate());
        }

        note.append("; notify_manager=").append(Boolean.TRUE.equals(request.getNotifyManager()));
        return note.toString();
    }

    private String buildReleaseAuditNote(ReleaseBatchRequest request) {
        return "[RELEASE] release_notes=" + request.getReleaseNotes();
    }

    /**
     * Set audit fields when updating batch
     */
    private void setAuditFieldsForUpdate(Batch batch, Account user) {
        batch.setUpdatedBy(user.getId());
        batch.setUpdatedAt(LocalDateTime.now());
    }

    /**
     * Get current authenticated user
     */
    private Account getCurrentUser() {

        String username = SecurityUtils.getCurrentUsername();

        return accountRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("No authenticated user found with username: {}", username);
                    return new BadRequestException(ErrorCode.AUTH_002);
                });
    }
}
