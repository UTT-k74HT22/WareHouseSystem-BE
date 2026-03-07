package org.demo.whs.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.PurchaseOrders;
import org.demo.whs.entity.dto.request.PurchaseOrders.PurchaseOrdersFilterRequest;
import org.demo.whs.entity.dto.request.PurchaseOrders.PurchaseOrdersRequest;
import org.demo.whs.entity.dto.request.PurchaseOrders.UpdatePurchaseOrdersRequest;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.dto.response.PurchaseOrders.PurchaseOrdersResponse;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.exception.NotFoundException;
import org.demo.whs.mapper.PurchaseOrdersMapper;
import org.demo.whs.repository.BusinessPartnersRepository;
import org.demo.whs.repository.PurchaseOrderLinesRepository;
import org.demo.whs.repository.PurchaseOrdersRepository;
import org.demo.whs.repository.WareHouseRepository;
import org.demo.whs.service.PurchaseOrdersService;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Implementation of the PurchaseOrdersService.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PurchaseOrdersServiceImpl implements PurchaseOrdersService {

    private final PurchaseOrdersRepository purchaseOrdersRepository;
    private final PurchaseOrderLinesRepository purchaseOrderLinesRepository;
    private final BusinessPartnersRepository businessPartnersRepository;
    private final WareHouseRepository wareHouseRepository;
    private final PurchaseOrdersMapper purchaseOrdersMapper;

    @Override
    @Transactional
    public PurchaseOrdersResponse create(PurchaseOrdersRequest request) {
        log.info("Create purchase order draft, request={}", request);

        if (!businessPartnersRepository.existsActiveSupplierById(request.getSupplierId())) {
            log.debug("Supplier with id {} does not exist or is not active", request.getSupplierId());
            throw new NotFoundException(ErrorCode.BP_001);
        }

        if (!wareHouseRepository.existsActiveById(request.getWarehouseId())) {
            log.debug("Warehouse with id {} does not exist or is not active", request.getWarehouseId());
            throw new NotFoundException(ErrorCode.WHS_001);
        }

        String purchaseOrderNumber = generatePurchaseOrderNumber();
        PurchaseOrders purchaseOrders = purchaseOrdersMapper.toEntity(request);
        purchaseOrders.setPurchaseOrderNumber(purchaseOrderNumber);

        purchaseOrders = purchaseOrdersRepository.save(purchaseOrders);
        log.info("Purchase order draft created successfully, id={}, purchaseOrderNumber={}", purchaseOrders.getId(), purchaseOrders.getPurchaseOrderNumber());
        return purchaseOrdersMapper.toResponse(purchaseOrders);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PurchaseOrdersResponse> getAll(PurchaseOrdersFilterRequest filter, Pageable pageable) {
        log.info("Get purchase orders, filter={}, pageable={}", filter, pageable);
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

    private String generatePurchaseOrderNumber() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        long sequence = purchaseOrdersRepository.count() + 1; // tăng dần theo số lượng đơn hàng đã tạo, có thể thay bằng sequence generator nếu cần
        String poNumber = String.format("PO-%s-%04d", datePart, sequence); // PO-yyyyMMddHHmmss-xxxx
        log.debug("Generated purchase order number: {}", poNumber);
        //validate uniqueness
        while(purchaseOrdersRepository.existsByPurchaseOrderNumber(poNumber)) {
            log.warn("Generated purchase order number {} already exists, regenerating...", poNumber);
            sequence++;
            poNumber = String.format("PO-%s-%04d", datePart, sequence);
        }
        return poNumber;
    }
}
