package org.demo.whs.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.*;
import org.demo.whs.entity.dto.request.Inventory.InventoryMutationRequest;
import org.demo.whs.entity.dto.request.OutboundShipments.OutboundShipmentsFilterRequest;
import org.demo.whs.entity.dto.request.OutboundShipments.OutboundShipmentsRequest;
import org.demo.whs.entity.dto.request.OutboundShipments.UpdateOutboundShipmentsRequest;
import org.demo.whs.entity.dto.response.OutboundShipments.OutboundShipmentsResponse;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.enums.LocationType;
import org.demo.whs.exception.ConflictException;
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
import org.demo.whs.service.LocationService;
import org.demo.whs.service.OutboundShipmentsService;
import org.demo.whs.utils.IdentifierGenerator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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
    private final InventoryReservationRepository inventoryReservationRepository;
    private final StockMovementsRepository stockMovementsRepository;
    private final LocationRepository locationRepository;
    private final LocationService locationService;
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
        return withActorNames(outboundShipmentsMapper.toResponse(savedShipment));
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
        List<OutboundShipmentLines> lines = outboundShipmentLinesRepository.findByOutboundShipmentId(id);
        return withActorNames(outboundShipmentsMapper.toResponse(shipment, lines));
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Map<String, Long> getStats() {
        java.util.Map<String, Long> stats = new java.util.LinkedHashMap<>();
        long total = 0;
        for (OutboundShipmentsStatus status : OutboundShipmentsStatus.values()) {
            long count = outboundShipmentsRepository.countByStatus(status);
            stats.put(status.name().toLowerCase(), count);
            total += count;
        }
        stats.put("total", total);
        return stats;
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
        return withActorNames(outboundShipmentsMapper.toResponse(updatedShipment));
    }

    // ==================== WORKFLOW ====================

    @Override
    @Transactional
    public OutboundShipmentsResponse startPicking(String id) {
        log.info("Start picking for shipment, id={}", id);
        OutboundShipments shipment = outboundShipmentsRepository.findByIdWithLock(id)
                .orElseThrow(() -> new NotFoundException("Outbound shipment not found", ErrorCode.COM_001));

        if (shipment.getStatus() == OutboundShipmentsStatus.PICKING) {
            log.info("Shipment already in PICKING status, reconciling transit capacity");
            Locations pickingLoc = locationService.resolveLocationByType(shipment.getWarehouseId(), LocationType.PICKING);
            syncLocationUsedCapacity(pickingLoc.getId());
            List<OutboundShipmentLines> lines = outboundShipmentLinesRepository.findByOutboundShipmentId(id);
            return withActorNames(outboundShipmentsMapper.toResponse(shipment, lines));
        }
        if (shipment.getStatus() != OutboundShipmentsStatus.DRAFT) {
            throw new BadRequestException("Shipment must be in DRAFT status to start picking", ErrorCode.COM_001);
        }

        List<OutboundShipmentLines> lines = outboundShipmentLinesRepository.findByOutboundShipmentId(id);
        if (lines.isEmpty()) {
            throw new BadRequestException("Shipment must have at least one line to start picking", ErrorCode.COM_001);
        }

        Locations pickingLoc = locationService.resolveLocationByType(shipment.getWarehouseId(), LocationType.PICKING);
        String actorId = getCurrentActorId();
        log.info("Picking location resolved: {}", pickingLoc.getId());

        for (OutboundShipmentLines line : lines) {
            log.info("Processing line: lineNumber={}, salesOrderLineId={}", line.getLineNumber(), line.getSalesOrderLineId());

            InventoryReservation reservation = inventoryReservationRepository
                    .findByOrderLineId(line.getSalesOrderLineId())
                    .orElseThrow(() -> new NotFoundException("No reservation found for line: " + line.getLineNumber(), ErrorCode.INV_001));

            log.info("Reservation found: locationId={}, productId={}, qty={}",
                    reservation.getLocationId(), reservation.getProductId(), reservation.getQuantity());

            if (reservation.getQuantity().compareTo(line.getQuantityShipped()) < 0) {
                log.error("Insufficient reserved quantity for line {}: reserved={}, requested={}",
                        line.getLineNumber(), reservation.getQuantity(), line.getQuantityShipped());
                throw new ConflictException("Insufficient reserved quantity to pick for line " + line.getLineNumber(), ErrorCode.INV_004);
            }

            log.info("Calling moveAndUpdateCapacity: from={}, to={}, qty={}",
                    reservation.getLocationId(), pickingLoc.getId(), line.getQuantityShipped());

            moveAndUpdateCapacity(
                    reservation.getLocationId(),
                    pickingLoc.getId(),
                    reservation.getProductId(),
                    reservation.getBatchId(),
                    line.getQuantityShipped(),
                    shipment.getId(),
                    line.getSalesOrderLineId(),
                    false,
                    true,
                    true
            );

            line.setLocationId(pickingLoc.getId());
            line.setBatchId(reservation.getBatchId());
            line.setPickedAt(LocalDateTime.now());
            line.setPickedBy(actorId);
        }

        shipment.setStatus(OutboundShipmentsStatus.PICKING);
        applyAuditFields(shipment, actorId, false);

        outboundShipmentLinesRepository.saveAll(lines);
        OutboundShipments updatedShipment = outboundShipmentsRepository.save(shipment);
        syncLocationUsedCapacity(pickingLoc.getId());
        List<OutboundShipmentLines> updatedLines = outboundShipmentLinesRepository.findByOutboundShipmentId(id);
        return withActorNames(outboundShipmentsMapper.toResponse(updatedShipment, updatedLines));
    }

    @Override
    @Transactional
    public OutboundShipmentsResponse markAsPacked(String id) {
        log.info("Mark shipment as packed, id={}", id);
        OutboundShipments shipment = outboundShipmentsRepository.findByIdWithLock(id)
                .orElseThrow(() -> new NotFoundException("Outbound shipment not found", ErrorCode.COM_001));

        if (shipment.getStatus() == OutboundShipmentsStatus.PACKED) {
            Locations packingLoc = locationService.resolveLocationByType(shipment.getWarehouseId(), LocationType.PACKING);
            syncLocationUsedCapacity(packingLoc.getId());
            List<OutboundShipmentLines> lines = outboundShipmentLinesRepository.findByOutboundShipmentId(id);
            return withActorNames(outboundShipmentsMapper.toResponse(shipment, lines));
        }
        if (shipment.getStatus() != OutboundShipmentsStatus.PICKING) {
            throw new BadRequestException("Shipment must be in PICKING status to mark as packed", ErrorCode.COM_001);
        }

        List<OutboundShipmentLines> lines = outboundShipmentLinesRepository.findByOutboundShipmentId(id);
        Locations pickingLoc = locationService.resolveLocationByType(shipment.getWarehouseId(), LocationType.PICKING);
        Locations packingLoc = locationService.resolveLocationByType(shipment.getWarehouseId(), LocationType.PACKING);
        lines.stream()
                .map(line -> line.getLocationId() != null ? line.getLocationId() : pickingLoc.getId())
                .distinct()
                .forEach(this::syncLocationUsedCapacity);

        for (OutboundShipmentLines line : lines) {
            String sourceLocationId = line.getLocationId() != null ? line.getLocationId() : pickingLoc.getId();
            moveAndUpdateCapacity(
                    sourceLocationId,
                    packingLoc.getId(),
                    line.getProductId(),
                    line.getBatchId(),
                    line.getQuantityShipped(),
                    shipment.getId(),
                    line.getSalesOrderLineId(),
                    false,
                    true,
                    true
            );

            line.setLocationId(packingLoc.getId());
        }

        shipment.setStatus(OutboundShipmentsStatus.PACKED);
        applyAuditFields(shipment, getCurrentActorId(), false);

        outboundShipmentLinesRepository.saveAll(lines);
        OutboundShipments updatedShipment = outboundShipmentsRepository.save(shipment);
        syncLocationUsedCapacity(pickingLoc.getId());
        syncLocationUsedCapacity(packingLoc.getId());
        List<OutboundShipmentLines> updatedLines = outboundShipmentLinesRepository.findByOutboundShipmentId(id);
        return withActorNames(outboundShipmentsMapper.toResponse(updatedShipment, updatedLines));
    }

    @Override
    @Transactional
    public OutboundShipmentsResponse ship(String id) {
        log.info("Ship shipment, id={}", id);
        OutboundShipments shipment = outboundShipmentsRepository.findByIdWithLock(id)
                .orElseThrow(() -> new NotFoundException("Outbound shipment not found", ErrorCode.COM_001));

        if (shipment.getStatus() == OutboundShipmentsStatus.SHIPPED || shipment.getStatus() == OutboundShipmentsStatus.STAGING) {
            List<OutboundShipmentLines> lines = outboundShipmentLinesRepository.findByOutboundShipmentId(id);
            return withActorNames(outboundShipmentsMapper.toResponse(shipment, lines));
        }
        if (shipment.getStatus() != OutboundShipmentsStatus.PACKED) {
            throw new BadRequestException("Shipment must be PACKED to ship", ErrorCode.COM_001);
        }

        List<OutboundShipmentLines> lines = outboundShipmentLinesRepository.findByOutboundShipmentId(id);
        if (lines.isEmpty()) {
            throw new BadRequestException("Shipment must have at least one line to ship", ErrorCode.COM_001);
        }

        Locations packingLoc = locationService.resolveLocationByType(shipment.getWarehouseId(), LocationType.PACKING);
        Locations stagingLoc = locationService.resolveLocationByType(shipment.getWarehouseId(), LocationType.STAGING);
        String actorId = getCurrentActorId();
        lines.stream()
                .map(line -> line.getLocationId() != null ? line.getLocationId() : packingLoc.getId())
                .distinct()
                .forEach(this::syncLocationUsedCapacity);

        for (OutboundShipmentLines line : lines) {
            // Move PACKING → STAGING
            String sourceLocationId = line.getLocationId() != null ? line.getLocationId() : packingLoc.getId();
            moveAndUpdateCapacity(
                    sourceLocationId,
                    stagingLoc.getId(),
                    line.getProductId(),
                    line.getBatchId(),
                    line.getQuantityShipped(),
                    shipment.getId(),
                    line.getSalesOrderLineId(),
                    false,
                    true,
                    true
            );
            line.setLocationId(stagingLoc.getId());
        }

        shipment.setStatus(OutboundShipmentsStatus.STAGING);
        applyAuditFields(shipment, actorId, false);
        outboundShipmentLinesRepository.saveAll(lines);
        outboundShipmentsRepository.save(shipment);
        syncLocationUsedCapacity(packingLoc.getId());
        syncLocationUsedCapacity(stagingLoc.getId());

        List<OutboundShipmentLines> updatedLines = outboundShipmentLinesRepository.findByOutboundShipmentId(id);
        return withActorNames(outboundShipmentsMapper.toResponse(shipment, updatedLines));
    }

    @Override
    @Transactional
    public OutboundShipmentsResponse confirmDispatch(String id) {
        log.info("Confirm dispatch for shipment, id={}", id);
        OutboundShipments shipment = outboundShipmentsRepository.findByIdWithLock(id)
                .orElseThrow(() -> new NotFoundException("Outbound shipment not found", ErrorCode.COM_001));

        if (shipment.getStatus() == OutboundShipmentsStatus.SHIPPED) {
            List<OutboundShipmentLines> lines = outboundShipmentLinesRepository.findByOutboundShipmentId(id);
            return withActorNames(outboundShipmentsMapper.toResponse(shipment, lines));
        }
        if (shipment.getStatus() != OutboundShipmentsStatus.STAGING) {
            throw new BadRequestException("Shipment must be in STAGING status to confirm dispatch", ErrorCode.COM_001);
        }

        List<OutboundShipmentLines> lines = outboundShipmentLinesRepository.findByOutboundShipmentId(id);
        if (lines.isEmpty()) {
            throw new BadRequestException("Shipment must have at least one line to confirm dispatch", ErrorCode.COM_001);
        }

        Locations stagingLoc = locationService.resolveLocationByType(shipment.getWarehouseId(), LocationType.STAGING);
        String actorId = getCurrentActorId();
        List<SalesOrderLines> updatedSalesOrderLines = new java.util.ArrayList<>(lines.size());
        syncLocationUsedCapacity(stagingLoc.getId());

        for (OutboundShipmentLines line : lines) {
            InventoryReservation reservation = inventoryReservationRepository.findByOrderLineId(line.getSalesOrderLineId())
                    .orElseThrow(() -> new NotFoundException("Reservation not found for order line", ErrorCode.INV_001));

            confirmDispatchFromSource(
                    line.getLocationId() != null ? line.getLocationId() : stagingLoc.getId(),
                    shipment,
                    line,
                    reservation
            );
            line.setLocationId(null);

            SalesOrderLines soLine = salesOrderLinesRepository.findById(line.getSalesOrderLineId())
                    .orElseThrow(() -> new NotFoundException("Sales order line not found", ErrorCode.COM_001));
            BigDecimal newShipped = soLine.getQuantityShipped().add(line.getQuantityShipped());
            if (newShipped.compareTo(soLine.getQuantityOrdered()) > 0) {
                throw new BadRequestException("Over shipped quantity for order line", ErrorCode.COM_001);
            }
            soLine.setQuantityShipped(newShipped);
            updatedSalesOrderLines.add(soLine);
        }

        shipment.setStatus(OutboundShipmentsStatus.SHIPPED);
        shipment.setShippedAt(LocalDateTime.now());
        shipment.setConfirmedBy(actorId);
        applyAuditFields(shipment, actorId, false);
        outboundShipmentLinesRepository.saveAllAndFlush(lines);
        // Ghi ngay số lượng đã xuất trước khi tính lại trạng thái đơn xuất hàng.
        // Nhờ đó tránh việc truy vấn trạng thái đọc dữ liệu cũ và đánh dấu nhầm hoàn thành.
        salesOrderLinesRepository.saveAllAndFlush(updatedSalesOrderLines.stream().distinct().collect(Collectors.toList()));
        outboundShipmentsRepository.saveAndFlush(shipment);
        syncLocationUsedCapacity(stagingLoc.getId());

        updateSalesOrderStatus(shipment.getSalesOrderId());

        return withActorNames(outboundShipmentsMapper.toResponse(shipment, lines));
    }

    @Override
    @Transactional
    public OutboundShipmentsResponse cancel(String id) {
        log.info("Cancel outbound shipment, id={}", id);
        OutboundShipments shipment = outboundShipmentsRepository.findByIdWithLock(id)
                .orElseThrow(() -> new NotFoundException("Outbound shipment not found", ErrorCode.COM_001));

        if (shipment.getStatus() == OutboundShipmentsStatus.SHIPPED) {
            throw new BadRequestException("Cannot cancel a SHIPPED shipment", ErrorCode.COM_001);
        }
        if (shipment.getStatus() == OutboundShipmentsStatus.CANCELLED) {
            return withActorNames(outboundShipmentsMapper.toResponse(shipment));
        }

        if (shipment.getStatus() == OutboundShipmentsStatus.PICKING
                || shipment.getStatus() == OutboundShipmentsStatus.PACKED
                || shipment.getStatus() == OutboundShipmentsStatus.STAGING) {

            List<OutboundShipmentLines> lines = outboundShipmentLinesRepository.findByOutboundShipmentId(id);
            Locations pickingLoc = locationService.resolveLocationByType(shipment.getWarehouseId(), LocationType.PICKING);
            Locations packingLoc = locationService.resolveLocationByType(shipment.getWarehouseId(), LocationType.PACKING);
            Locations stagingLoc = locationService.resolveLocationByType(shipment.getWarehouseId(), LocationType.STAGING);

            for (OutboundShipmentLines line : lines) {
                if (shipment.getStatus() == OutboundShipmentsStatus.PICKING) {
                    // Revert: PICKING → STORAGE (original reservation location)
                    inventoryReservationRepository.findByOrderLineId(line.getSalesOrderLineId())
                            .ifPresent(reservation -> {
                                moveAndUpdateCapacity(
                                        line.getLocationId() != null ? line.getLocationId() : pickingLoc.getId(),
                                        reservation.getLocationId(),
                                        line.getProductId(),
                                        line.getBatchId(),
                                        line.getQuantityShipped(),
                                        shipment.getId(),
                                        line.getSalesOrderLineId(),
                                        false,
                                        true,
                                        true
                                );
                                line.setLocationId(reservation.getLocationId());
                            });

                } else if (shipment.getStatus() == OutboundShipmentsStatus.PACKED) {
                    // Revert: PACKED → PICKING
                    moveAndUpdateCapacity(
                            line.getLocationId() != null ? line.getLocationId() : packingLoc.getId(),
                            pickingLoc.getId(),
                            line.getProductId(),
                            line.getBatchId(),
                            line.getQuantityShipped(),
                            shipment.getId(),
                            line.getSalesOrderLineId(),
                            false,
                            true,
                            true
                    );
                    line.setLocationId(pickingLoc.getId());
                } else {
                    moveAndUpdateCapacity(
                            line.getLocationId() != null ? line.getLocationId() : stagingLoc.getId(),
                            packingLoc.getId(),
                            line.getProductId(),
                            line.getBatchId(),
                            line.getQuantityShipped(),
                            shipment.getId(),
                            line.getSalesOrderLineId(),
                            false,
                            true,
                            true
                    );
                    line.setLocationId(packingLoc.getId());
                }
            }
            outboundShipmentLinesRepository.saveAll(lines);

            if (shipment.getStatus() == OutboundShipmentsStatus.PICKING) {
                syncLocationUsedCapacity(pickingLoc.getId());
            } else if (shipment.getStatus() == OutboundShipmentsStatus.PACKED) {
                syncLocationUsedCapacity(packingLoc.getId());
            } else {
                syncLocationUsedCapacity(stagingLoc.getId());
            }
        }

        shipment.setStatus(OutboundShipmentsStatus.CANCELLED);
        applyAuditFields(shipment, getCurrentActorId(), false);

        OutboundShipments updatedShipment = outboundShipmentsRepository.save(shipment);
        return withActorNames(outboundShipmentsMapper.toResponse(updatedShipment));
    }

    // ==================== HELPER METHODS ====================

    private OutboundShipmentsResponse withActorNames(OutboundShipmentsResponse response) {
        if (response == null) {
            return null;
        }

        response.setCreatedByName(resolveAccountUsername(response.getCreatedBy()));
        response.setUpdatedByName(resolveAccountUsername(response.getUpdatedBy()));
        response.setConfirmedByName(resolveAccountUsername(response.getConfirmedBy()));
        return response;
    }

    private String resolveAccountUsername(String accountId) {
        if (accountId == null || accountId.isBlank()) {
            return null;
        }
        return accountRepository.findById(accountId)
                .map(Account::getUsername)
                .orElse(null);
    }

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

        boolean hasRemainingQuantity = lines.stream().anyMatch(line -> {
            BigDecimal ordered = line.getQuantityOrdered() == null ? BigDecimal.ZERO : line.getQuantityOrdered();
            BigDecimal shipped = line.getQuantityShipped() == null ? BigDecimal.ZERO : line.getQuantityShipped();
            return shipped.compareTo(ordered) < 0;
        });

        salesOrder.setStatus(hasRemainingQuantity
                ? SalesOrdersStatus.PARTIALLY_SHIPPED
                : SalesOrdersStatus.COMPLETED);

        salesOrdersRepository.save(salesOrder);
    }

    private void validateIntegerQuantity(BigDecimal quantity) {
        if (quantity.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) != 0) {
            throw new BadRequestException("Quantity must be integer", ErrorCode.COM_001);
        }
    }

    private void syncLocationUsedCapacity(String locationId) {
        BigDecimal actualQuantity = inventoryRepository.sumTotalQuantityByLocationId(locationId);
        locationRepository.forceUpdateUsedCapacity(locationId,
                actualQuantity == null ? BigDecimal.ZERO : actualQuantity);
    }

    private void moveAndUpdateCapacity(
            String from,
            String to,
            String productId,
            String batchId,
            BigDecimal qty,
            String shipmentId,
            String lineId,
            boolean consumeReserved,
            boolean updateSourceUsedCapacity,
            boolean updateDestinationUsedCapacity
    ) {
        log.info("moveAndUpdateCapacity: from={}, to={}, product={}, qty={}", from, to, productId, qty);
        validateIntegerQuantity(qty);
        if (from.equals(to)) {
            return;
        }
        if (updateSourceUsedCapacity) {
            int decreased = locationService.decreaseUsedCapacity(from, qty);
            if (decreased == 0) {
                throw new ConflictException("Source location used capacity update failed", ErrorCode.COM_001);
            }
        }
        if (updateDestinationUsedCapacity) {
            int increased = locationService.increaseUsedCapacity(to, qty);
            if (increased == 0) {
                throw new ConflictException("Destination location used capacity update failed", ErrorCode.COM_001);
            }
        }

        inventoryService.moveInventory(
                from,
                to,
                productId,
                batchId,
                qty,
                ReferenceType.OUTBOUND_SHIPMENT,
                shipmentId,
                lineId,
                consumeReserved
        );
    }

    private void confirmDispatchFromSource(
            String stagingLocationId,
            OutboundShipments shipment,
            OutboundShipmentLines line,
            InventoryReservation reservation
    ) {
        validateIntegerQuantity(line.getQuantityShipped());

        String currentLocationId = line.getLocationId() != null ? line.getLocationId() : stagingLocationId;

        inventoryService.decrease(
                InventoryMutationRequest.builder()
                        .warehouseId(shipment.getWarehouseId())
                        .productId(line.getProductId())
                        .locationId(currentLocationId)
                        .batchId(line.getBatchId() != null ? line.getBatchId() : reservation.getBatchId())
                        .quantity(line.getQuantityShipped())
                        .referenceType(ReferenceType.OUTBOUND_SHIPMENT)
                        .referenceId(shipment.getId())
                        .referenceNumber(shipment.getShipmentNumber())
                        .consumeReserved(true)
                        .orderLineId(line.getSalesOrderLineId())
                        .build()
        );
        int decreased = locationService.decreaseUsedCapacity(stagingLocationId, line.getQuantityShipped());
        if (decreased == 0) {
            throw new ConflictException("Location used capacity update failed", ErrorCode.COM_001);
        }
    }

}
