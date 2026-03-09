package org.demo.whs.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.dto.request.PurchaseOrderLines.PurchaseOrderLinesRequest;
import org.demo.whs.entity.dto.request.PurchaseOrderLines.UpdatePurchaseOrderLinesRequest;
import org.demo.whs.entity.dto.response.PurchaseOrderLines.PurchaseOrderLinesResponse;
import org.demo.whs.exception.ConflictException;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.mapper.PurchaseOrderLinesMapper;
import org.demo.whs.repository.ProductRepository;
import org.demo.whs.repository.PurchaseOrderLinesRepository;
import org.demo.whs.repository.PurchaseOrdersRepository;
import org.demo.whs.service.PurchaseOrderLinesService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for managing purchase order lines.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PurchaseOrderLinesServiceImpl implements PurchaseOrderLinesService {
    private final PurchaseOrderLinesRepository purchaseOrderLinesRepository;
    private final PurchaseOrdersRepository purchaseOrdersRepository;
    private final ProductRepository productRepository;
    private final PurchaseOrderLinesMapper purchaseOrderLinesMapper;

    @Override
    @Transactional
    public PurchaseOrderLinesResponse create(PurchaseOrderLinesRequest request) {
        log.info("Scaffold purchase order line create, purchaseOrderId={}", request.getPurchaseOrderId());
        throw scaffoldOnly("create");
    }

    @Override
    @Transactional
    public PurchaseOrderLinesResponse update(String id, UpdatePurchaseOrderLinesRequest request) {
        log.info("Scaffold purchase order line update, id={}", id);
        throw scaffoldOnly("update");
    }

    @Override
    @Transactional
    public void delete(String id) {
        log.info("Scaffold purchase order line delete, id={}", id);
        throw scaffoldOnly("delete");
    }

    private ConflictException scaffoldOnly(String operation) {
        return new ConflictException(
                String.format(
                        "Purchase order line %s flow has been scaffolded and is pending business implementation for WHS-55",
                        operation
                ),
                ErrorCode.COM_001
        );
    }
}
