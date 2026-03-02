package org.demo.whs.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.Batch;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.beans.Transient;
import java.util.List;
import java.util.stream.Collectors;

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
    public BatchResponse getBatchById(String Id) {
        log.info("Fetching batch by ID: {}", Id);
        Batch batch = batchRepository.findById(Id)
                .orElseThrow(() -> {
                    log.warn("Fetching batch by ID: {}", Id);
                    return new NotFoundException("Batches not found", ErrorCode.BATCH_001);
                });

        return batchMapper.toResponse(batch);

    }

    @Override
    @Transactional
    public PageResponse<BatchResponse> getAllBatches(Integer page, Integer size) {
        log.info("Fetching all batches - page={}, size={}", page, size);

        validatePaginationParams(page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Batch> batchPage = batchRepository.findAll(pageable);

        List<BatchResponse> responses = batchPage.getContent()
                .stream().map(batchMapper::toResponse)
                .collect(Collectors.toList());

        return PageResponse.from(batchPage, responses);
    }

    /**
     * Validates pagination parameters.
     *
     * @param page the page number
     * @param size the page size
     */
    private void validatePaginationParams(Integer page, Integer size) {
        if (page < 0) {
            log.warn("Invalid page number: {}", page);
            throw new BadRequestException(ErrorCode.COM_003);
        }
        if (size <= 0 || size > 100) {
            log.warn("Invalid page size: {}", size);
            throw new BadRequestException(ErrorCode.COM_003);
        }
    }
}
