package org.demo.whs.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.StockMovements;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.dto.response.StockMovements.StockMovementsResponse;
import org.demo.whs.entity.enums.ReferenceType;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.exception.NotFoundException;
import org.demo.whs.mapper.StockMovementsMapper;
import org.demo.whs.repository.StockMovementsRepository;
import org.demo.whs.service.StockMovementsService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementation of StockMovementsService.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class StockMovementsServiceImpl implements StockMovementsService {

    private final StockMovementsRepository stockMovementsRepository;
    private final StockMovementsMapper stockMovementsMapper;

    @Override
    @Transactional(readOnly = true)
    public StockMovementsResponse getById(String id) {
        StockMovements movement = stockMovementsRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Stock movement not found", ErrorCode.COM_004));
        return stockMovementsMapper.toResponse(movement);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<StockMovementsResponse> getAll(Integer page, Integer size) {
        Pageable pageable = buildPageable(page, size);
        Page<StockMovements> movementPage = stockMovementsRepository.findAll(pageable);
        List<StockMovementsResponse> content = movementPage.getContent().stream()
                .map(stockMovementsMapper::toResponse)
                .toList();
        return PageResponse.from(movementPage, content);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<StockMovementsResponse> getByReference(
            ReferenceType referenceType,
            String referenceId,
            Integer page,
            Integer size
    ) {
        Pageable pageable = buildPageable(page, size);
        Page<StockMovements> movementPage = stockMovementsRepository.findByReferenceTypeAndReferenceId(
                referenceType,
                referenceId,
                pageable
        );

        List<StockMovementsResponse> content = movementPage.getContent().stream()
                .map(stockMovementsMapper::toResponse)
                .toList();
        return PageResponse.from(movementPage, content);
    }

    private Pageable buildPageable(Integer page, Integer size) {
        int targetPage = page == null ? 0 : page;
        int targetSize = size == null ? 20 : size;
        if (targetPage < 0 || targetSize <= 0 || targetSize > 200) {
            throw new BadRequestException("Invalid pagination parameters", ErrorCode.COM_001);
        }
        return PageRequest.of(targetPage, targetSize, Sort.by(Sort.Direction.DESC, "movementDate"));
    }
}
