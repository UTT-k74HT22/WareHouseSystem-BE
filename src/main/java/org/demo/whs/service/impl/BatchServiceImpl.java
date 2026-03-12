package org.demo.whs.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.Batch;
import org.demo.whs.entity.Products;
import org.demo.whs.entity.dto.request.Batch.ChangeBatchStatusRequest;
import org.demo.whs.exception.NotFoundException;
import org.demo.whs.repository.BatchRepository;
import org.demo.whs.repository.ProductRepository;
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
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of the BatchService interface.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class BatchServiceImpl implements BatchService {

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

        if (request.getManufacturingDate() != null &&
                request.getManufacturingDate().isAfter(LocalDate.now())) {
            throw new BadRequestException(ErrorCode.BATCH_005);
        }

        if (request.getManufacturingDate() != null &&
                request.getExpiryDate() != null &&
                request.getExpiryDate().isBefore(request.getManufacturingDate())) {
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
    public BatchResponse getBatchById(String Id) {
        log.info("Fetching batch by ID: {}", Id);
        Batch batch = batchRepository.findById(Id)
                .orElseThrow(() -> {
                    log.warn("Fetching batch by ID: {}", Id);
                    return new BadRequestException(ErrorCode.BATCH_001);
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
                .stream().map(batchMapper::toResponse)
                .collect(Collectors.toList());

        return PageResponse.from(batchPage, responses);
    }

    @Override
    @Transactional
    public BatchResponse changeBatchStatus(String id, ChangeBatchStatusRequest request) {

        Batch batch = batchRepository.findById(id)
                .orElseThrow(() ->
                        new BadRequestException(ErrorCode.BATCH_001));

        batch.setStatus(request.getStatus());
        Batch updateStatus = batchRepository.save(batch);
        log.info("Batch status changed successfully with ID={}", updateStatus.getId());
        return batchMapper.toResponse(updateStatus);
    }
}
