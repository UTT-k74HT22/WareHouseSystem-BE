package org.demo.whs.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.dto.request.PurchaseOrders.PurchaseOrdersFilterRequest;
import org.demo.whs.entity.dto.request.PurchaseOrders.PurchaseOrdersRequest;
import org.demo.whs.entity.dto.request.PurchaseOrders.UpdatePurchaseOrdersRequest;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.dto.response.PurchaseOrders.PurchaseOrdersResponse;
import org.demo.whs.mapper.PurchaseOrdersMapper;
import org.demo.whs.repository.PurchaseOrderLinesRepository;
import org.demo.whs.repository.PurchaseOrdersRepository;
import org.demo.whs.service.PurchaseOrdersService;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of the PurchaseOrdersService.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PurchaseOrdersServiceImpl implements PurchaseOrdersService {

    private final PurchaseOrdersRepository purchaseOrdersRepository;
    private final PurchaseOrderLinesRepository purchaseOrderLinesRepository;
    private final PurchaseOrdersMapper purchaseOrdersMapper;

    @Override
    @Transactional
    public PurchaseOrdersResponse create(PurchaseOrdersRequest request) {
        log.warn("[SERVICE][PURCHASE_ORDERS][CREATE] WHS-53 service implementation is pending");
        throw new UnsupportedOperationException("WHS-53 create purchase order service is not implemented yet");
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PurchaseOrdersResponse> getAll(PurchaseOrdersFilterRequest filter, Pageable pageable) {
        log.warn("[SERVICE][PURCHASE_ORDERS][GET_ALL] WHS-53 service implementation is pending");
        throw new UnsupportedOperationException("WHS-53 get purchase orders service is not implemented yet");
    }

    @Override
    @Transactional(readOnly = true)
    public PurchaseOrdersResponse getById(String id) {
        log.warn("[SERVICE][PURCHASE_ORDERS][GET_BY_ID] WHS-53 service implementation is pending, id={}", id);
        throw new UnsupportedOperationException("WHS-53 get purchase order by id service is not implemented yet");
    }

    @Override
    @Transactional
    public PurchaseOrdersResponse update(String id, UpdatePurchaseOrdersRequest request) {
        log.warn("[SERVICE][PURCHASE_ORDERS][UPDATE] WHS-53 service implementation is pending, id={}", id);
        throw new UnsupportedOperationException("WHS-53 update purchase order service is not implemented yet");
    }

    @Override
    @Transactional
    public void delete(String id) {
        log.warn("[SERVICE][PURCHASE_ORDERS][DELETE] WHS-53 service implementation is pending, id={}", id);
        throw new UnsupportedOperationException("WHS-53 delete purchase order service is not implemented yet");
    }

    @Override
    @Transactional
    public PurchaseOrdersResponse confirm(String id) {
        log.warn("[SERVICE][PURCHASE_ORDERS][CONFIRM] WHS-54 service implementation is pending, id={}", id);
        throw new UnsupportedOperationException("WHS-54 confirm purchase order service is not implemented yet");
    }
}
