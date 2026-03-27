package org.demo.whs.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.*;
import org.demo.whs.entity.dto.request.Inventory.InventoryDecreaseRequest;
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
        List<OutboundShipmentLines> lines = outboundShipmentLinesRepository.findByOutboundShipmentId(id);
        return outboundShipmentsMapper.toResponse(shipment, lines);
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
        OutboundShipments shipment = outboundShipmentsRepository.findByIdWithLock(id)
                .orElseThrow(() -> new NotFoundException("Outbound shipment not found", ErrorCode.COM_001));

        if (shipment.getStatus() == OutboundShipmentsStatus.PICKING) {
            log.info("Shipment already in PICKING status, reconciling transit capacity");
            Locations pickingLoc = locationService.resolveLocationByType(shipment.getWarehouseId(), LocationType.PICKING);
            syncTransitLocationUsedCapacity(pickingLoc.getId(), List.of(OutboundShipmentsStatus.PICKING));
            List<OutboundShipmentLines> lines = outboundShipmentLinesRepository.findByOutboundShipmentId(id);
            return outboundShipmentsMapper.toResponse(shipment, lines);
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
                    false,
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
        syncTransitLocationUsedCapacity(pickingLoc.getId(), List.of(OutboundShipmentsStatus.PICKING));
        List<OutboundShipmentLines> updatedLines = outboundShipmentLinesRepository.findByOutboundShipmentId(id);
        return outboundShipmentsMapper.toResponse(updatedShipment, updatedLines);
    }

    @Override
    @Transactional
    public OutboundShipmentsResponse markAsPacked(String id) {
        log.info("Mark shipment as packed, id={}", id);
        OutboundShipments shipment = outboundShipmentsRepository.findByIdWithLock(id)
                .orElseThrow(() -> new NotFoundException("Outbound shipment not found", ErrorCode.COM_001));

        if (shipment.getStatus() == OutboundShipmentsStatus.PACKED) {
            Locations packingLoc = locationService.resolveLocationByType(shipment.getWarehouseId(), LocationType.PACKING);
            syncTransitLocationUsedCapacity(packingLoc.getId(), List.of(OutboundShipmentsStatus.PACKED));
            List<OutboundShipmentLines> lines = outboundShipmentLinesRepository.findByOutboundShipmentId(id);
            return outboundShipmentsMapper.toResponse(shipment, lines);
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
                .forEach(locationId -> syncTransitLocationUsedCapacity(locationId, List.of(OutboundShipmentsStatus.PICKING)));

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
        syncTransitLocationUsedCapacity(pickingLoc.getId(), List.of(OutboundShipmentsStatus.PICKING));
        syncTransitLocationUsedCapacity(packingLoc.getId(), List.of(OutboundShipmentsStatus.PACKED));
        List<OutboundShipmentLines> updatedLines = outboundShipmentLinesRepository.findByOutboundShipmentId(id);
        return outboundShipmentsMapper.toResponse(updatedShipment, updatedLines);
    }

    @Override
    @Transactional
    public OutboundShipmentsResponse ship(String id) {
        log.info("Ship shipment, id={}", id);
        OutboundShipments shipment = outboundShipmentsRepository.findByIdWithLock(id)
                .orElseThrow(() -> new NotFoundException("Outbound shipment not found", ErrorCode.COM_001));

        if (shipment.getStatus() == OutboundShipmentsStatus.SHIPPED || shipment.getStatus() == OutboundShipmentsStatus.STAGING) {
            List<OutboundShipmentLines> lines = outboundShipmentLinesRepository.findByOutboundShipmentId(id);
            return outboundShipmentsMapper.toResponse(shipment, lines);
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
                .forEach(locationId -> syncTransitLocationUsedCapacity(locationId, List.of(OutboundShipmentsStatus.PACKED)));

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
        syncTransitLocationUsedCapacity(packingLoc.getId(), List.of(OutboundShipmentsStatus.PACKED));
        syncTransitLocationUsedCapacity(stagingLoc.getId(), List.of(OutboundShipmentsStatus.STAGING));

        List<OutboundShipmentLines> updatedLines = outboundShipmentLinesRepository.findByOutboundShipmentId(id);
        return outboundShipmentsMapper.toResponse(shipment, updatedLines);
    }

    @Override
    @Transactional
    public OutboundShipmentsResponse confirmDispatch(String id) {
        log.info("Confirm dispatch for shipment, id={}", id);
        OutboundShipments shipment = outboundShipmentsRepository.findByIdWithLock(id)
                .orElseThrow(() -> new NotFoundException("Outbound shipment not found", ErrorCode.COM_001));

        if (shipment.getStatus() == OutboundShipmentsStatus.SHIPPED) {
            List<OutboundShipmentLines> lines = outboundShipmentLinesRepository.findByOutboundShipmentId(id);
            return outboundShipmentsMapper.toResponse(shipment, lines);
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
        syncTransitLocationUsedCapacity(stagingLoc.getId(), List.of(OutboundShipmentsStatus.STAGING));

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
        salesOrderLinesRepository.saveAll(updatedSalesOrderLines.stream().distinct().collect(Collectors.toList()));
        outboundShipmentsRepository.saveAndFlush(shipment);
        locationRepository.forceUpdateUsedCapacity(stagingLoc.getId(), BigDecimal.ZERO);

        updateSalesOrderStatus(shipment.getSalesOrderId());

        return outboundShipmentsMapper.toResponse(shipment, lines);
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
            return outboundShipmentsMapper.toResponse(shipment);
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
                                        false
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
                syncTransitLocationUsedCapacity(pickingLoc.getId(), List.of(OutboundShipmentsStatus.PICKING));
            } else if (shipment.getStatus() == OutboundShipmentsStatus.PACKED) {
                syncTransitLocationUsedCapacity(packingLoc.getId(), List.of(OutboundShipmentsStatus.PACKED));
            } else {
                syncTransitLocationUsedCapacity(stagingLoc.getId(), List.of(OutboundShipmentsStatus.STAGING));
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

    private void validateIntegerQuantity(BigDecimal quantity) {
        if (quantity.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) != 0) {
            throw new BadRequestException("Quantity must be integer", ErrorCode.COM_001);
        }
    }

    private void syncTransitLocationUsedCapacity(String locationId, List<OutboundShipmentsStatus> statuses) {
        BigDecimal expected = outboundShipmentLinesRepository
                .sumQuantityByLocationIdAndShipmentStatuses(locationId, statuses);
        locationRepository.forceUpdateUsedCapacity(locationId, expected == null ? BigDecimal.ZERO : expected);
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
                InventoryDecreaseRequest.builder()
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
