package org.demo.whs.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.Account;
import org.demo.whs.entity.BusinessPartners;
import org.demo.whs.entity.PurchaseOrders;
import org.demo.whs.entity.Warehouses;
import org.demo.whs.entity.dto.request.PurchaseOrders.PurchaseOrdersFilterRequest;
import org.demo.whs.entity.dto.request.PurchaseOrders.PurchaseOrdersRequest;
import org.demo.whs.entity.dto.request.PurchaseOrders.UpdatePurchaseOrdersRequest;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.dto.response.PurchaseOrders.PurchaseOrdersResponse;
import org.demo.whs.entity.enums.BusinessPartnerStatus;
import org.demo.whs.entity.enums.BusinessPartnerType;
import org.demo.whs.entity.enums.PurchaseOrdersStatus;
import org.demo.whs.entity.enums.WareHouseStatus;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.exception.NotFoundException;
import org.demo.whs.mapper.PurchaseOrdersMapper;
import org.demo.whs.repository.AccountRepository;
import org.demo.whs.repository.BusinessPartnersRepository;
import org.demo.whs.repository.PurchaseOrderLinesRepository;
import org.demo.whs.repository.PurchaseOrdersRepository;
import org.demo.whs.repository.WareHouseRepository;
import org.demo.whs.repository.specification.PurchaseOrdersSpecification;
import org.demo.whs.security.SecurityUtils;
import org.demo.whs.service.PurchaseOrdersService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

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
    private final AccountRepository accountRepository;
    private final PurchaseOrdersMapper purchaseOrdersMapper;

    @Override
    @Transactional
    public PurchaseOrdersResponse create(PurchaseOrdersRequest request) {
        log.info("Create purchase order draft, request={}", request);

        // Step 1: Validate and sanitize input
        sanitizeCreateRequest(request);
        validatePurchaseOrderDates(request.getOrderDate(), request.getExpectedDeliveryDate());

        // Step 2: Validate related entities
        BusinessPartners supplier = validateSupplier(request.getSupplierId());
        validateWarehouse(request.getWarehouseId());

        // Step 3: Create purchase order draft
        String purchaseOrderNumber = generatePurchaseOrderNumber();
        PurchaseOrders purchaseOrders = purchaseOrdersMapper.toEntity(request);
        purchaseOrders.setPurchaseOrderNumber(purchaseOrderNumber);
        purchaseOrders.setPaymentTerms(resolvePaymentTerms(request.getPaymentTerms(), supplier.getPaymentTerms()));
        purchaseOrders.setNotes(normalizeBlank(request.getNotes()));
        applyAuditFields(purchaseOrders, getCurrentActorId(), true);

        // Step 4: Save to database
        purchaseOrders = purchaseOrdersRepository.save(purchaseOrders);
        log.info("Purchase order draft created successfully, id={}, purchaseOrderNumber={}", purchaseOrders.getId(), purchaseOrders.getPurchaseOrderNumber());
        return purchaseOrdersMapper.toResponse(purchaseOrders);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PurchaseOrdersResponse> getAll(PurchaseOrdersFilterRequest filter, Pageable pageable) {
        log.info("Get purchase orders, filter={}, pageable={}", filter, pageable);
        validatePageable(pageable);
        normalizeAndValidateFilter(filter);

        Page<PurchaseOrders> purchaseOrderPage = purchaseOrdersRepository.findAll(
                PurchaseOrdersSpecification.withFilter(filter),
                pageable
        );

        List<PurchaseOrdersResponse> responses = purchaseOrderPage.getContent().stream()
                .map(purchaseOrdersMapper::toResponse)
                .toList();

        return PageResponse.from(purchaseOrderPage, responses);
    }

    @Override
    @Transactional(readOnly = true)
    public PurchaseOrdersResponse getById(String id) {
        log.info("Get purchase order by id={}", id);

        PurchaseOrders purchaseOrders = purchaseOrdersRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Purchase order not found", ErrorCode.PO_001));

        return purchaseOrdersMapper.toResponse(purchaseOrders);
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
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        while (true) {
            String suffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
            String poNumber = String.format("PO-%s-%s", datePart, suffix);
            if (!purchaseOrdersRepository.existsByPurchaseOrderNumber(poNumber)) {
                log.debug("Generated purchase order number: {}", poNumber);
                return poNumber;
            }
            log.warn("Generated purchase order number {} already exists, regenerating...", poNumber);
        }
    }

    private void normalizeAndValidateFilter(PurchaseOrdersFilterRequest filter) {
        if (filter == null) {
            return;
        }

        if (filter.getOrderDateFrom() != null
                && filter.getOrderDateTo() != null
                && filter.getOrderDateFrom().isAfter(filter.getOrderDateTo())) {
            throw new BadRequestException("orderDateFrom must be less than or equal to orderDateTo", ErrorCode.COM_001);
        }

        if (filter.getExpectedDeliveryDateFrom() != null
                && filter.getExpectedDeliveryDateTo() != null
                && filter.getExpectedDeliveryDateFrom().isAfter(filter.getExpectedDeliveryDateTo())) {
            throw new BadRequestException(
                    "expectedDeliveryDateFrom must be less than or equal to expectedDeliveryDateTo",
                    ErrorCode.COM_001
            );
        }

        if (filter.getStatus() != null && !filter.getStatus().isBlank()) {
            try {
                filter.setStatus(PurchaseOrdersStatus.valueOf(filter.getStatus().trim().toUpperCase()).name());
            } catch (IllegalArgumentException ex) {
                throw new BadRequestException("Invalid purchase order status", ErrorCode.COM_001);
            }
        }
    }

    private void validatePurchaseOrderDates(java.time.LocalDate orderDate, java.time.LocalDate expectedDeliveryDate) {
        if (orderDate != null
                && expectedDeliveryDate != null
                && expectedDeliveryDate.isBefore(orderDate)) {
            throw new BadRequestException(
                    "Expected delivery date must be on or after order date",
                    ErrorCode.COM_001
            );
        }
    }

    private BusinessPartners validateSupplier(String supplierId) {
        BusinessPartners supplier = businessPartnersRepository.findById(supplierId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.BP_001));

        boolean validType = supplier.getType() == BusinessPartnerType.SUPPLIER
                || supplier.getType() == BusinessPartnerType.BOTH;
        if (supplier.getStatus() != BusinessPartnerStatus.ACTIVE || !validType) {
            throw new NotFoundException(ErrorCode.BP_001);
        }

        return supplier;
    }

    private Warehouses validateWarehouse(String warehouseId) {
        Warehouses warehouse = wareHouseRepository.findById(warehouseId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.WHS_001));

        if (warehouse.getStatus() != WareHouseStatus.ACTIVE) {
            throw new NotFoundException(ErrorCode.WHS_001);
        }

        return warehouse;
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

    private String getCurrentActorId() {
        String username = SecurityUtils.getCurrentUsername();
        if (username == null || username.isBlank()) {
            throw new BadRequestException("Unauthenticated request", ErrorCode.AUTH_002);
        }

        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new BadRequestException("User account not found", ErrorCode.AUTH_002));

        return account.getId();
    }

    private void applyAuditFields(PurchaseOrders purchaseOrders, String actorId, boolean isCreate) {
        if (isCreate) {
            purchaseOrders.setCreatedBy(actorId);
        }
        purchaseOrders.setUpdatedBy(actorId);
    }

    private void sanitizeCreateRequest(PurchaseOrdersRequest request) {
        request.setSupplierId(normalizeRequired(request.getSupplierId()));
        request.setWarehouseId(normalizeRequired(request.getWarehouseId()));
        request.setCurrency(normalizeRequired(request.getCurrency()));
        request.setPaymentTerms(normalizeBlank(request.getPaymentTerms()));
        request.setNotes(normalizeBlank(request.getNotes()));
    }

    private String resolvePaymentTerms(String requestedPaymentTerms, String supplierPaymentTerms) {
        String normalizedRequestPaymentTerms = normalizeBlank(requestedPaymentTerms);
        if (normalizedRequestPaymentTerms != null) {
            return normalizedRequestPaymentTerms;
        }
        return normalizeBlank(supplierPaymentTerms);
    }

    private String normalizeRequired(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizeBlank(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
