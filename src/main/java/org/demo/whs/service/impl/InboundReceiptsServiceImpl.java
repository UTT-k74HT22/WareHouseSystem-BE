package org.demo.whs.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.InboundReceiptLines;
import org.demo.whs.entity.InboundReceipts;
import org.demo.whs.entity.dto.request.InboundReceipts.InboundReceiptsFilterRequest;
import org.demo.whs.entity.dto.request.InboundReceipts.InboundReceiptsRequest;
import org.demo.whs.entity.dto.request.InboundReceipts.UpdateInboundReceiptsRequest;
import org.demo.whs.entity.dto.response.InboundReceiptLines.InboundReceiptLinesResponse;
import org.demo.whs.entity.dto.response.InboundReceipts.InboundReceiptsResponse;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.enums.InboundReceiptsStatus;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.exception.NotFoundException;
import org.demo.whs.exception.NotImplementedException;
import org.demo.whs.mapper.InboundReceiptLinesMapper;
import org.demo.whs.mapper.InboundReceiptsMapper;
import org.demo.whs.repository.InboundReceiptLinesRepository;
import org.demo.whs.repository.InboundReceiptsRepository;
import org.demo.whs.repository.specification.InboundReceiptsSpecification;
import org.demo.whs.service.InboundReceiptsService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Implementation of the InboundReceiptsService.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class InboundReceiptsServiceImpl implements InboundReceiptsService {

    private final InboundReceiptsRepository inboundReceiptsRepository;
    private final InboundReceiptLinesRepository inboundReceiptLinesRepository;
    private final InboundReceiptsMapper inboundReceiptsMapper;
    private final InboundReceiptLinesMapper inboundReceiptLinesMapper;

    @Override
    @Transactional
    public InboundReceiptsResponse create(InboundReceiptsRequest request) {
        log.info("Create inbound receipt draft requested, purchaseOrderId={}", request.getPurchaseOrderId());
        throw new NotImplementedException("WHS-56 scaffold is ready. Implement draft receipt creation in InboundReceiptsServiceImpl.create.");
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<InboundReceiptsResponse> getAll(InboundReceiptsFilterRequest filter, Pageable pageable) {
        log.info("Get inbound receipts, filter={}, pageable={}", filter, pageable);
        validatePageable(pageable);
        normalizeAndValidateFilter(filter);

        Page<InboundReceipts> receiptPage = inboundReceiptsRepository.findAll(
                InboundReceiptsSpecification.withFilter(filter),
                pageable
        );

        List<InboundReceiptsResponse> responses = receiptPage.getContent().stream()
                .map(inboundReceiptsMapper::toSummaryResponse)
                .toList();

        return PageResponse.from(receiptPage, responses);
    }

    @Override
    @Transactional(readOnly = true)
    public InboundReceiptsResponse getById(String id) {
        log.info("Get inbound receipt by id={}", id);
        InboundReceipts receipt = inboundReceiptsRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Inbound receipt not found", ErrorCode.COM_004));

        return inboundReceiptsMapper.toResponse(receipt, getLineResponses(receipt.getId()));
    }

    @Override
    @Transactional
    public InboundReceiptsResponse update(String id, UpdateInboundReceiptsRequest request) {
        log.info("Update inbound receipt draft requested, id={}", id);
        throw new NotImplementedException("WHS-56 scaffold is ready. Implement draft receipt update in InboundReceiptsServiceImpl.update.");
    }

    @Override
    @Transactional
    public void delete(String id) {
        log.info("Delete inbound receipt draft requested, id={}", id);
        throw new NotImplementedException("WHS-56 scaffold is ready. Implement draft receipt deletion in InboundReceiptsServiceImpl.delete.");
    }

    @Override
    @Transactional(readOnly = true)
    public List<InboundReceiptsResponse> getByPurchaseOrderId(String purchaseOrderId) {
        log.info("Get inbound receipts by purchaseOrderId={}", purchaseOrderId);
        return inboundReceiptsRepository.findByPurchaseOrderIdOrderByCreatedAtDesc(purchaseOrderId)
                .stream()
                .map(inboundReceiptsMapper::toSummaryResponse)
                .toList();
    }

    private List<InboundReceiptLinesResponse> getLineResponses(String inboundReceiptId) {
        List<InboundReceiptLines> lines = inboundReceiptLinesRepository.findByInboundReceiptIdOrderByLineNumberAsc(inboundReceiptId);
        return inboundReceiptLinesMapper.toResponses(lines);
    }

    private void normalizeAndValidateFilter(InboundReceiptsFilterRequest filter) {
        if (filter == null) {
            return;
        }

        if (filter.getReceiptDateFrom() != null
                && filter.getReceiptDateTo() != null
                && filter.getReceiptDateFrom().isAfter(filter.getReceiptDateTo())) {
            throw new BadRequestException(
                    "receiptDateFrom must be less than or equal to receiptDateTo",
                    ErrorCode.COM_001
            );
        }

        if (StringUtils.hasText(filter.getStatus())) {
            try {
                filter.setStatus(InboundReceiptsStatus.valueOf(filter.getStatus().trim().toUpperCase()).name());
            } catch (IllegalArgumentException ex) {
                throw new BadRequestException("Invalid inbound receipt status", ErrorCode.COM_001);
            }
        }
    }

    private void validatePageable(Pageable pageable) {
        if (pageable.getPageNumber() < 0) {
            throw new BadRequestException(ErrorCode.COM_006);
        }
        if (pageable.getPageSize() <= 0) {
            throw new BadRequestException(ErrorCode.COM_007);
        }
        if (pageable.getPageSize() > 100) {
            throw new BadRequestException(ErrorCode.COM_008);
        }
    }
}
