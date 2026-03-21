package org.demo.whs.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.*;
import org.demo.whs.entity.dto.request.Inventory.CheckAvailabilityRequest;
import org.demo.whs.entity.dto.request.Inventory.InventoryReserveRequest;
import org.demo.whs.entity.dto.request.Inventory.InventoryUnreserveRequest;
import org.demo.whs.entity.dto.request.SalesOrders.SalesOrdersFilterRequest;
import org.demo.whs.entity.dto.request.SalesOrders.SalesOrdersRequest;
import org.demo.whs.entity.dto.request.SalesOrders.UpdateSalesOrdersRequest;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.dto.response.SalesOrders.SalesOrdersResponse;
import org.demo.whs.entity.enums.*;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.exception.NotFoundException;
import org.demo.whs.mapper.SalesOrderLinesMapper;
import org.demo.whs.mapper.SalesOrdersMapper;
import org.demo.whs.repository.*;
import org.demo.whs.repository.specification.SalesOrdersSpecification;
import org.demo.whs.security.SecurityUtils;
import org.demo.whs.service.InventoryService;
import org.demo.whs.service.SalesOrdersService;
import org.demo.whs.utils.IdentifierGenerator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of the SalesOrdersService interface.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SalesOrdersServiceImpl implements SalesOrdersService {

    private final SalesOrdersRepository salesOrdersRepository;
    private final SalesOrderLinesRepository salesOrderLinesRepository;
    private final BusinessPartnersRepository businessPartnersRepository;
    private final WareHouseRepository wareHouseRepository;
    private final ProductRepository productRepository;
    private final AccountRepository accountRepository;
    private final OutboundShipmentsRepository outboundShipmentsRepository;
    private final InventoryReservationRepository inventoryReservationRepository;
    private final InventoryService inventoryService;
    private final SalesOrdersMapper salesOrdersMapper;
    private final SalesOrderLinesMapper salesOrderLinesMapper;
    private final IdentifierGenerator identifierGenerator;

    @Override
    @Transactional
    public SalesOrdersResponse create(SalesOrdersRequest request) {
        log.info("Create sales order draft, request={}", request);

        // Step 1: Validate related entities
        BusinessPartners customer = validateCustomer(request.getCustomerId());
        validateWarehouse(request.getWarehouseId());

        // Step 2: Create sales order entity
        String soNumber = identifierGenerator.generate(
                "SO",
                50,
                salesOrdersRepository::existsBySoNumber
        );

        SalesOrders salesOrder = salesOrdersMapper.toEntity(request);
        salesOrder.setSoNumber(soNumber);
        applyAuditFields(salesOrder, getCurrentActorId(), true);

        // Step 3: Save SO header first to get ID
        SalesOrders savedSO = salesOrdersRepository.save(salesOrder);

        // Step 4: Create lines and compute totals
        BigDecimal subTotal = BigDecimal.ZERO;
        List<SalesOrderLines> lines = request.getLines().stream().map(lineRequest -> {
            validateProduct(lineRequest.getProductId());
            
            // Validation: Quantity must be positive
            if (lineRequest.getQuantityOrdered() == null || lineRequest.getQuantityOrdered().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException(
                    String.format("Invalid quantity for product %s. Quantity must be greater than zero.", lineRequest.getProductId()),
                    ErrorCode.COM_001
                );
            }

            SalesOrderLines line = salesOrderLinesMapper.toEntity(lineRequest);
            line.setSalesOrderId(savedSO.getId());
            
            // Explicitly recalculate lineTotal to prevent client-side tampering
            line.setLineTotal(line.getUnitPrice().multiply(line.getQuantityOrdered()));
            
            return line;
        }).collect(Collectors.toList());

        // Assign line numbers and add to subtotal
        for (int i = 0; i < lines.size(); i++) {
            SalesOrderLines line = lines.get(i);
            line.setLineNumber(i + 1);
            subTotal = subTotal.add(line.getLineTotal());
        }

        salesOrderLinesRepository.saveAll(lines);

        // Step 5: Update SO totals
        savedSO.setSubTotal(subTotal);
        savedSO.setTaxAmount(BigDecimal.ZERO);
        savedSO.setTotalAmount(subTotal.add(savedSO.getTaxAmount()));
        salesOrdersRepository.save(savedSO);

        SalesOrdersResponse response = salesOrdersMapper.toResponse(savedSO);
        response.setLines(lines.stream().map(salesOrderLinesMapper::toResponse).collect(Collectors.toList()));
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SalesOrdersResponse> getAll(SalesOrdersFilterRequest filter, Pageable pageable) {
        log.info("Get sales orders, filter={}, pageable={}", filter, pageable);
        validatePageable(pageable);

        Page<SalesOrders> page = salesOrdersRepository.findAll(
                SalesOrdersSpecification.withFilter(filter),
                pageable
        );

        List<SalesOrdersResponse> responses = page.getContent().stream()
                .map(salesOrdersMapper::toResponse)
                .toList();

        return PageResponse.from(page, responses);
    }

    @Override
    @Transactional(readOnly = true)
    public SalesOrdersResponse getById(String id) {
        log.info("Get sales order by id={}", id);
        SalesOrders salesOrder = salesOrdersRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Sales order not found", ErrorCode.COM_001));

        List<SalesOrderLines> lines = salesOrderLinesRepository.findBySalesOrderId(id);
        SalesOrdersResponse response = salesOrdersMapper.toResponse(salesOrder);
        response.setLines(lines.stream().map(salesOrderLinesMapper::toResponse).collect(Collectors.toList()));
        return response;
    }

    @Override
    @Transactional
    public SalesOrdersResponse update(String id, UpdateSalesOrdersRequest request) {
        log.info("Update sales order draft, id={}, request={}", id, request);

        SalesOrders salesOrder = findByIdForMutation(id);
        validateDraftStatus(salesOrder, "updated");

        if (request.getCustomerId() != null) {
            validateCustomer(request.getCustomerId());
        }
        if (request.getWarehouseId() != null) {
            validateWarehouse(request.getWarehouseId());
        }

        salesOrdersMapper.updateEntity(salesOrder, request);
        applyAuditFields(salesOrder, getCurrentActorId(), false);

        SalesOrders updatedSO = salesOrdersRepository.save(salesOrder);
        return salesOrdersMapper.toResponse(updatedSO);
    }

    @Override
    @Transactional
    public SalesOrdersResponse confirm(String id) {
        log.info("Confirm sales order, id={}", id);

        SalesOrders salesOrder = findByIdForMutation(id);
        
        // Idempotency check: If already confirmed, return current state
        if (salesOrder.getStatus() == SalesOrdersStatus.CONFIRMED) {
            log.info("Sales order already confirmed, id={}", id);
            return getById(id);
        }

        validateDraftStatus(salesOrder, "confirmed");

        List<SalesOrderLines> lines = salesOrderLinesRepository.findAllBySalesOrderIdForUpdate(id);
        if (lines.isEmpty()) {
            throw new BadRequestException("Sales order must have at least one line", ErrorCode.COM_001);
        }

        // Step 0: Pre-validate quantities to fail fast
        for (SalesOrderLines line : lines) {
            if (line.getQuantityOrdered() == null || line.getQuantityOrdered().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException(
                    String.format("Invalid quantity for product %s. Quantity must be greater than zero.", line.getProductId()),
                    ErrorCode.COM_001
                );
            }
        }

        // Step 1: Reserve inventory for all lines (Atomic Check & Reserve)
        for (SalesOrderLines line : lines) {
            inventoryService.reserve(InventoryReserveRequest.builder()
                    .warehouseId(salesOrder.getWarehouseId())
                    .productId(line.getProductId())
                    .quantity(line.getQuantityOrdered())
                    .orderLineId(line.getId())
                    .build());
        }

        // Step 2: Update SO status
        String actorId = getCurrentActorId();
        salesOrder.setStatus(SalesOrdersStatus.CONFIRMED);
        salesOrder.setConfirmedAt(LocalDateTime.now());
        salesOrder.setConfirmedBy(actorId);
        applyAuditFields(salesOrder, actorId, false);

        SalesOrders confirmedSO = salesOrdersRepository.save(salesOrder);
        
        SalesOrdersResponse response = salesOrdersMapper.toResponse(confirmedSO);
        response.setLines(lines.stream().map(salesOrderLinesMapper::toResponse).collect(Collectors.toList()));
        return response;
    }

    @Override
    @Transactional
    public SalesOrdersResponse cancel(String id) {
        log.info("Cancel sales order, id={}", id);

        SalesOrders salesOrder = findByIdForMutation(id);

        // Idempotency check: If already cancelled, return current state
        if (salesOrder.getStatus() == SalesOrdersStatus.CANCELLED) {
            log.info("Sales order already cancelled, id={}", id);
            return getById(id);
        }

        if (salesOrder.getStatus() != SalesOrdersStatus.CONFIRMED && salesOrder.getStatus() != SalesOrdersStatus.DRAFT) {
            throw new BadRequestException("Only draft or confirmed sales orders can be cancelled", ErrorCode.COM_001);
        }

        // Check for active shipments if it was confirmed
        if (salesOrder.getStatus() == SalesOrdersStatus.CONFIRMED) {
            List<OutboundShipments> shipments = outboundShipmentsRepository.findBySalesOrderId(id);
            for (OutboundShipments shipment : shipments) {
                if (shipment.getStatus() != OutboundShipmentsStatus.CANCELLED) {
                    throw new BadRequestException("Cannot cancel sales order with active shipments", ErrorCode.COM_001);
                }
            }

            List<SalesOrderLines> lines = salesOrderLinesRepository.findAllBySalesOrderIdForUpdate(id);

            // Unreserve inventory based on actual reservation records
            for (SalesOrderLines line : lines) {
                inventoryReservationRepository.findByOrderLineId(line.getId()).ifPresent(reservation -> {
                    if (reservation.getQuantity().compareTo(BigDecimal.ZERO) > 0) {
                        inventoryService.unreserve(InventoryUnreserveRequest.builder()
                                .productId(reservation.getProductId())
                                .warehouseId(reservation.getWarehouseId())
                                .orderLineId(line.getId())
                                .quantity(reservation.getQuantity())
                                .build());
                    }
                });
            }
        }

        salesOrder.setStatus(SalesOrdersStatus.CANCELLED);
        applyAuditFields(salesOrder, getCurrentActorId(), false);

        SalesOrders cancelledSO = salesOrdersRepository.save(salesOrder);
        return getById(id);
    }

    private BusinessPartners validateCustomer(String customerId) {
        BusinessPartners customer = businessPartnersRepository.findById(customerId)
                .orElseThrow(() -> new NotFoundException("Customer not found", ErrorCode.COM_001));

        boolean validType = customer.getType() == BusinessPartnerType.CUSTOMER
                || customer.getType() == BusinessPartnerType.BOTH|| customer.getType() == BusinessPartnerType.SUPPLIER;
        if (customer.getStatus() != BusinessPartnerStatus.ACTIVE || !validType) {
            throw new BadRequestException("Customer is not active or not a customer", ErrorCode.COM_001);
        }

        return customer;
    }

    private void validateWarehouse(String warehouseId) {
        Warehouses warehouse = wareHouseRepository.findById(warehouseId)
                .orElseThrow(() -> new NotFoundException("Warehouse not found", ErrorCode.COM_001));

        if (warehouse.getStatus() != WareHouseStatus.ACTIVE) {
            throw new BadRequestException("Warehouse is not active", ErrorCode.COM_001);
        }
    }

    private void validateProduct(String productId) {
        Products product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found", ErrorCode.COM_001));

        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new BadRequestException("Product is not active", ErrorCode.COM_001);
        }
    }

    private void validatePageable(Pageable pageable) {
        if (pageable.getPageNumber() < 0) {
            throw new BadRequestException(ErrorCode.COM_006);
        }
        if (pageable.getPageSize() <= 0) {
            throw new BadRequestException(ErrorCode.COM_007);
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

    private void applyAuditFields(SalesOrders salesOrder, String actorId, boolean isCreate) {
        if (isCreate) {
            salesOrder.setCreatedBy(actorId);
        }
        salesOrder.setUpdatedBy(actorId);
    }

    private SalesOrders findByIdForMutation(String id) {
        return salesOrdersRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new NotFoundException("Sales order not found", ErrorCode.COM_001));
    }

    private void validateDraftStatus(SalesOrders salesOrder, String operation) {
        if (salesOrder.getStatus() != SalesOrdersStatus.DRAFT) {
            throw new BadRequestException(
                    String.format("Only draft sales orders can be %s", operation),
                    ErrorCode.COM_001
            );
        }
    }
}
