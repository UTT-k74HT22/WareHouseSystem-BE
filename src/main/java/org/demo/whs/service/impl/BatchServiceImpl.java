package org.demo.whs.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.Batch;
import org.demo.whs.entity.dto.request.Batch.UpdateBatchRequest;
import org.demo.whs.exception.NotFoundException;
import org.demo.whs.repository.BatchRepository;
import org.demo.whs.service.BatchService;
import org.demo.whs.entity.dto.request.Batch.CreateBatchRequest;
import org.demo.whs.entity.dto.request.Batch.CreateBatchRequest;
import org.demo.whs.entity.dto.response.Batch.BatchResponse;
import org.demo.whs.mapper.BatchMapper;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.enums.BatchStatus;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.ErrorCode;
import org.springframework.stereotype.Service;

import java.beans.Transient;

/**
 * Implementation of the BatchService interface.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class BatchServiceImpl implements BatchService {

    private final BatchRepository batchRepository;
    private final BatchMapper batchMapper;

    @Override
    @Transactional
    public BatchResponse createBatch (CreateBatchRequest request) {

        if (batchRepository.existsByBatchNumber(request.getBatchNumber())) {
            log.info("Create batch already exists for batch number: {}", request.getBatchNumber());
            throw new BadRequestException(ErrorCode.BATCH_001);
        }

        Batch batch = batchMapper.toEntity(request);

        Batch batchSave =  batchRepository.save(batch);
        log.info("Batch created successfully with ID={}", batch.getId());

        return batchMapper.toResponse(batchSave);
    }

    @Override
    @Transactional
    public BatchResponse updateBatch(String id, UpdateBatchRequest request) {

        log.info("Updating batch with id={}", id);

        Batch batch = batchRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("",ErrorCode.BATCH_001));

        validateBatchNumber(request.getBatchNumber(), batch);

        updateBatchFields(batch, request);

        Batch updated = batchRepository.save(batch);

        log.info("Batch updated successfully with id={}", id);

        return batchMapper.toResponse(updated);
    }

    private void validateBatchNumber(String newBatchNumber, Batch currentBatch) {

        if (newBatchNumber == null ||
                newBatchNumber.equals(currentBatch.getBatchNumber())) {
            return;
        }

        Batch existing = batchRepository
                .findByBatchNumber(newBatchNumber)
                .orElse(null);

        if (existing != null && !existing.getId().equals(currentBatch.getId())) {
            log.warn("Batch number {} already exists", newBatchNumber);
            throw new BadRequestException(ErrorCode.BATCH_001);
        }
    }

    private void updateBatchFields(Batch batch, UpdateBatchRequest request) {

        if (request.getBatchNumber() != null)
            batch.setBatchNumber(request.getBatchNumber());

        if (request.getManufacturingDate() != null)
            batch.setManufacturingDate(request.getManufacturingDate());

        if (request.getExpiryDate() != null)
            batch.setExpiryDate(request.getExpiryDate());

        if (request.getSupplierBatchNumber() != null)
            batch.setSupplierBatchNumber(request.getSupplierBatchNumber());

        if (request.getNotes() != null)
            batch.setNotes(request.getNotes());
    }
}
