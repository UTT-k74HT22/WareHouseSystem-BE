package org.demo.whs.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.*;
import org.demo.whs.entity.dto.request.Inventory.InventoryDecreaseRequest;
import org.demo.whs.entity.dto.request.Inventory.InventoryUnreserveRequest;
import org.demo.whs.entity.dto.request.OutboundShipments.OutboundShipmentsFilterRequest;
import org.demo.whs.entity.dto.request.OutboundShipments.OutboundShipmentsRequest;
import org.demo.whs.entity.dto.request.OutboundShipments.UpdateOutboundShipmentsRequest;
import org.demo.whs.entity.dto.response.OutboundShipments.OutboundShipmentsResponse;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.enums.OutboundShipmentsStatus;
import org.demo.whs.entity.enums.ReferenceType;
import org.demo.whs.entity.enums.SalesOrdersStatus;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.exception.NotFoundException;
import org.demo.whs.mapper.OutboundShipmentsMapper;
import org.demo.whs.repository.*;
import org.demo.whs.repository.specification.OutboundShipmentsSpecification;
import org.demo.whs.security.SecurityUtils;
import org.demo.whs.service.InventoryService;
import org.demo.whs.service.OutboundShipmentsService;
import org.demo.whs.utils.IdentifierGenerator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of the OutboundShipmentsService interface.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OutboundShipmentsServiceImpl implements OutboundShipmentsService {

    private final OutboundShipmentsRepository outboundShipmentsRepository;
    private final OutboundShipmentLinesRepository outboundShipmentLinesRepository;
    private final SalesOrdersRepository salesOrdersRepository;
    private final SalesOrderLinesRepository salesOrderLinesRepository;
    private final WareHouseRepository wareHouseRepository;
    private final AccountRepository accountRepository;
    private final InventoryService inventoryService;
    private final InventoryRepository inventoryRepository;
    private final StockMovementsRepository stockMovementsRepository;
    private final OutboundShipmentsMapper outboundShipmentsMapper;
    private final IdentifierGenerator identifierGenerator;

    @Override
    @Transactional
    public OutboundShipmentsResponse create(OutboundShipmentsRequest request) {
        log.info("Create outbound shipment draft, request={}", request);

        // Validate Sales Order
        SalesOrders salesOrder = salesOrdersRepository.findById(request.getSalesOrderId()).orElseThrow(() -> new NotFoundException("Sales order not found", ErrorCode.COM_001));

        if (salesOrder.getStatus() != SalesOrdersStatus.CONFIRMED && salesOrder.getStatus() != SalesOrdersStatus.PARTIALLY_SHIPPED) {
            throw new BadRequestException("Sales order must be CONFIRMED or PARTIALLY_SHIPPED to create a shipment", ErrorCode.COM_001);
        }

        // Validate Warehouse
        if (!salesOrder.getWarehouseId().equals(request.getWarehouseId())) {
            throw new BadRequestException("Shipment warehouse must match Sales Order warehouse", ErrorCode.COM_001);
        }
        validateWarehouse(request.getWarehouseId());

        // Generate Shipment Number
        String shipmentNumber = identifierGenerator.generate("SHIP", 50, outboundShipmentsRepository::existsByShipmentNumber);

        OutboundShipments shipment = outboundShipmentsMapper.toEntity(request);
        shipment.setShipmentNumber(shipmentNumber);
        applyAuditFields(shipment, getCurrentActorId(), true);

        OutboundShipments savedShipment = outboundShipmentsRepository.save(shipment);
        return outboundShipmentsMapper.toResponse(savedShipment);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OutboundShipmentsResponse> getAll(OutboundShipmentsFilterRequest filter, Pageable pageable) {
        log.info("Get outbound shipments, filter={}, pageable={}", filter, pageable);
        Page<OutboundShipments> page = outboundShipmentsRepository.findAll(OutboundShipmentsSpecification.withFilter(filter), pageable);

        List<OutboundShipmentsResponse> responses = page.getContent().stream().map(outboundShipmentsMapper::toResponse).collect(Collectors.toList());

        return PageResponse.from(page, responses);
    }

    @Override
    @Transactional(readOnly = true)
    public OutboundShipmentsResponse getById(String id) {
        log.info("Get outbound shipment by id={}", id);
        OutboundShipments shipment = findById(id);
        return outboundShipmentsMapper.toResponse(shipment);
    }

    @Override
    @Transactional
    public OutboundShipmentsResponse update(String id, UpdateOutboundShipmentsRequest request) {
        log.info("Update outbound shipment, id={}, request={}", id, request);
        OutboundShipments shipment = findById(id);

        if (shipment.getStatus() != OutboundShipmentsStatus.DRAFT) {
            throw new BadRequestException("Only DRAFT shipments can be updated", ErrorCode.COM_001);
        }

        outboundShipmentsMapper.updateEntity(shipment, request);
        applyAuditFields(shipment, getCurrentActorId(), false);

        OutboundShipments updatedShipment = outboundShipmentsRepository.save(shipment);
        return outboundShipmentsMapper.toResponse(updatedShipment);
    }

    // ==================== WORKFLOW ====================

    @Override
    @Transactional
    public OutboundShipmentsResponse startPicking(String id) {
        log.info("Start picking for shipment, id={}", id);
        // Dùng pessimistic lock để tránh nhiều request update trạng thái cùng lúc
        OutboundShipments shipment = outboundShipmentsRepository.findByIdWithLock(id).orElseThrow(() -> new NotFoundException("Outbound shipment not found", ErrorCode.COM_001));

        if (shipment.getStatus() == OutboundShipmentsStatus.PICKING) {
            return outboundShipmentsMapper.toResponse(shipment);
        }

        if (shipment.getStatus() != OutboundShipmentsStatus.DRAFT) {
            throw new BadRequestException("Shipment must be in DRAFT status to start picking", ErrorCode.COM_001);
        }

        List<OutboundShipmentLines> lines = outboundShipmentLinesRepository.findByOutboundShipmentId(id);
        if (lines.isEmpty()) {
            throw new BadRequestException("Shipment must have at least one line to start picking", ErrorCode.COM_001);
        }

        shipment.setStatus(OutboundShipmentsStatus.PICKING);
        applyAuditFields(shipment, getCurrentActorId(), false);

        OutboundShipments updatedShipment = outboundShipmentsRepository.save(shipment);
        return outboundShipmentsMapper.toResponse(updatedShipment);
    }

    @Override
    @Transactional
    public OutboundShipmentsResponse markAsPacked(String id) {
        log.info("Mark shipment as packed, id={}", id);
        // Use pessimistic lock to prevent concurrent status updates
        OutboundShipments shipment = outboundShipmentsRepository.findByIdWithLock(id).orElseThrow(() -> new NotFoundException("Outbound shipment not found", ErrorCode.COM_001));

        if (shipment.getStatus() == OutboundShipmentsStatus.PACKED) {
            return outboundShipmentsMapper.toResponse(shipment);
        }

        if (shipment.getStatus() != OutboundShipmentsStatus.PICKING) {
            throw new BadRequestException("Shipment must be in PICKING status to mark as packed", ErrorCode.COM_001);
        }

        shipment.setStatus(OutboundShipmentsStatus.PACKED);
        applyAuditFields(shipment, getCurrentActorId(), false);

        OutboundShipments updatedShipment = outboundShipmentsRepository.save(shipment);
        return outboundShipmentsMapper.toResponse(updatedShipment);
    }

    @Override
    @Transactional
    public OutboundShipmentsResponse ship(String id) {
        log.info("Ship shipment, id={}", id);
        // Lock shipment để tránh double ship (2 request chạy cùng lúc)
        OutboundShipments shipment = outboundShipmentsRepository.findByIdWithLock(id).orElseThrow(() -> new NotFoundException("Outbound shipment not found", ErrorCode.COM_001));

        if (shipment.getStatus() == OutboundShipmentsStatus.SHIPPED) {
            return outboundShipmentsMapper.toResponse(shipment);
        }

        // Chỉ cho phép ship khi trạng thái = PACKED
        if (shipment.getStatus() != OutboundShipmentsStatus.PACKED) {
            throw new BadRequestException("Shipment must be PACKED to ship", ErrorCode.COM_001);
        }

        List<OutboundShipmentLines> shipmentLines = outboundShipmentLinesRepository.findByOutboundShipmentId(id);
        if (shipmentLines.isEmpty()) {
            throw new BadRequestException("Shipment must have at least one line to ship", ErrorCode.COM_001);
        }

        String actorId = getCurrentActorId();

        // Lấy tất cả SalesOrderLine 1 lần để tránh N+1 query
        List<String> soLineIds = shipmentLines.stream().map(OutboundShipmentLines::getSalesOrderLineId).distinct().collect(Collectors.toList());

        java.util.Map<String, SalesOrderLines> soLinesMap = salesOrderLinesRepository.findAllById(soLineIds).stream().collect(java.util.stream.Collectors.toMap(SalesOrderLines::getId, sol -> sol));

        // Nhóm các line theo bucket inventory:
        // (product + warehouse + location + batch)
        // → để xử lý inventory theo nhóm, tránh lock nhiều lần
        record InventoryBucket(String productId, String warehouseId, String locationId, String batchId) {
        }
        java.util.Map<InventoryBucket, BigDecimal> bucketTotals = shipmentLines.stream().collect(java.util.stream.Collectors.groupingBy(line -> new InventoryBucket(line.getProductId(), shipment.getWarehouseId(), line.getLocationId(), line.getBatchId()), java.util.stream.Collectors.reducing(BigDecimal.ZERO, OutboundShipmentLines::getQuantityShipped, BigDecimal::add)));


        // 1. Trừ kho theo từng bucket (đảm bảo atomic theo từng nhóm)
        // → inventoryService sẽ tự validate và ghi stock movement
        for (java.util.Map.Entry<InventoryBucket, BigDecimal> entry : bucketTotals.entrySet()) {
            InventoryBucket bucket = entry.getKey();
            BigDecimal totalQty = entry.getValue();
            inventoryService.decrease(InventoryDecreaseRequest.builder().warehouseId(bucket.warehouseId()).productId(bucket.productId()).locationId(bucket.locationId()).batchId(bucket.batchId()).quantity(totalQty).referenceType(ReferenceType.OUTBOUND_SHIPMENT).referenceId(shipment.getId()).referenceNumber(shipment.getShipmentNumber()).consumeReserved(true).build());
        }

        // 2. Cập nhật quantity_shipped cho từng SalesOrderLine
        // → kiểm tra không được vượt quá quantity_ordered (tránh over-ship)
        for (OutboundShipmentLines line : shipmentLines) {
            SalesOrderLines soLine = soLinesMap.get(line.getSalesOrderLineId());
            if (soLine == null) {
                throw new NotFoundException("Sales order line not found: " + line.getSalesOrderLineId(), ErrorCode.COM_001);
            }

            BigDecimal newShipped = soLine.getQuantityShipped().add(line.getQuantityShipped());
            if (newShipped.compareTo(soLine.getQuantityOrdered()) > 0) {
                throw new BadRequestException("Over shipped quantity for order line: " + soLine.getLineNumber(), ErrorCode.COM_001);
            }
            soLine.setQuantityShipped(newShipped);
        }

        salesOrderLinesRepository.saveAll(soLinesMap.values());

        // 3. Cập nhật trạng thái shipment → SHIPPED
        // → set thời gian ship và người xác nhận
        shipment.setStatus(OutboundShipmentsStatus.SHIPPED);
        shipment.setShippedAt(LocalDateTime.now());
        shipment.setConfirmedBy(actorId);
        applyAuditFields(shipment, actorId, false);
        outboundShipmentsRepository.save(shipment);

        // 4. Cập nhật trạng thái Sales Order:
        // → COMPLETED nếu ship hết
        // → PARTIALLY_SHIPPED nếu còn thiếu
        updateSalesOrderStatus(shipment.getSalesOrderId());

        return outboundShipmentsMapper.toResponse(shipment);
    }

    @Override
    @Transactional
    public OutboundShipmentsResponse cancel(String id) {
        log.info("Cancel outbound shipment, id={}", id);
        // Use pessimistic lock to prevent race condition with ship()
        OutboundShipments shipment = outboundShipmentsRepository.findByIdWithLock(id).orElseThrow(() -> new NotFoundException("Outbound shipment not found", ErrorCode.COM_001));

        if (shipment.getStatus() == OutboundShipmentsStatus.SHIPPED) {
            throw new BadRequestException("Cannot cancel a SHIPPED shipment", ErrorCode.COM_001);
        }

        if (shipment.getStatus() == OutboundShipmentsStatus.CANCELLED) {
            return outboundShipmentsMapper.toResponse(shipment);
        }

        // Integrity Check: Cannot cancel if inventory movements already recorded (e.g. ship() partially processed)
        boolean hasMovements = stockMovementsRepository.existsByReferenceTypeAndReferenceId(ReferenceType.OUTBOUND_SHIPMENT, id);
        if (hasMovements) {
            throw new BadRequestException("Cannot cancel shipment that has already triggered inventory movements", ErrorCode.COM_001);
        }

        // If shipment was in progress (PICKING/PACKED), release reservations for its lines
        if (shipment.getStatus() == OutboundShipmentsStatus.PICKING || shipment.getStatus() == OutboundShipmentsStatus.PACKED) {
            List<OutboundShipmentLines> lines = outboundShipmentLinesRepository.findByOutboundShipmentId(id);
            for (OutboundShipmentLines line : lines) {
                inventoryService.unreserve(InventoryUnreserveRequest.builder().productId(line.getProductId()).warehouseId(shipment.getWarehouseId()).orderLineId(line.getSalesOrderLineId()).quantity(line.getQuantityShipped()).build());
            }
        }

        shipment.setStatus(OutboundShipmentsStatus.CANCELLED);
        applyAuditFields(shipment, getCurrentActorId(), false);

        OutboundShipments updatedShipment = outboundShipmentsRepository.save(shipment);
        return outboundShipmentsMapper.toResponse(updatedShipment);
    }

    // ==================== HELPER METHODS ====================

    private OutboundShipments findById(String id) {
        return outboundShipmentsRepository.findById(id).orElseThrow(() -> new NotFoundException("Outbound shipment not found", ErrorCode.COM_001));
    }

    private void validateWarehouse(String warehouseId) {
        wareHouseRepository.findById(warehouseId).orElseThrow(() -> new NotFoundException("Warehouse not found", ErrorCode.COM_001));
    }

    private String getCurrentActorId() {
        String username = SecurityUtils.getCurrentUsername();
        if (username == null || username.isBlank()) {
            throw new BadRequestException("Unauthenticated request", ErrorCode.AUTH_002);
        }

        Account account = accountRepository.findByUsername(username).orElseThrow(() -> new BadRequestException("User account not found", ErrorCode.AUTH_002));

        return account.getId();
    }

    private void applyAuditFields(OutboundShipments shipment, String actorId, boolean isCreate) {
        if (isCreate) {
            shipment.setCreatedBy(actorId);
        }
        shipment.setUpdatedBy(actorId);
    }

    private void updateSalesOrderStatus(String soId) {
        SalesOrders salesOrder = salesOrdersRepository.findById(soId).orElseThrow(() -> new NotFoundException("Sales order not found", ErrorCode.COM_001));

        List<SalesOrderLines> lines = salesOrderLinesRepository.findBySalesOrderId(soId);

        boolean allShipped = lines.stream().allMatch(line -> line.getQuantityShipped().compareTo(line.getQuantityOrdered()) >= 0);

        if (allShipped) {
            salesOrder.setStatus(SalesOrdersStatus.COMPLETED);
        } else {
            salesOrder.setStatus(SalesOrdersStatus.PARTIALLY_SHIPPED);
        }

        salesOrdersRepository.save(salesOrder);
    }
}
