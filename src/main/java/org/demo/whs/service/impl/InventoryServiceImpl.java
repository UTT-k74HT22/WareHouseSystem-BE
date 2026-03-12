package org.demo.whs.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.Batch;
import org.demo.whs.entity.Inventory;
import org.demo.whs.entity.InventoryReservation;
import org.demo.whs.entity.Locations;
import org.demo.whs.entity.Products;
import org.demo.whs.entity.Warehouses;
import org.demo.whs.entity.dto.request.Inventory.CheckAvailabilityRequest;
import org.demo.whs.entity.dto.request.Inventory.InventoryFilterRequest;
import org.demo.whs.entity.dto.request.Inventory.InventoryReserveRequest;
import org.demo.whs.entity.dto.request.Inventory.InventoryUnreserveRequest;
import org.demo.whs.entity.dto.response.Inventory.CheckAvailabilityResponse;
import org.demo.whs.entity.dto.response.Inventory.InventoryByLocationResponse;
import org.demo.whs.entity.dto.response.Inventory.InventoryLocationProjection;
import org.demo.whs.entity.dto.response.Inventory.InventoryReserveResponse;
import org.demo.whs.entity.dto.response.Inventory.InventoryResponse;
import org.demo.whs.entity.dto.response.Inventory.InventorySummaryResponse;
import org.demo.whs.entity.dto.response.Inventory.InventoryUnreserveResponse;
import org.demo.whs.entity.dto.response.Inventory.LocationInventoryItemResponse;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.StockMovements;
import org.demo.whs.entity.enums.InventoryReservationStatus;
import org.demo.whs.entity.enums.ReferenceType;
import org.demo.whs.entity.enums.StockMovementsType;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.ConflictException;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.exception.NotFoundException;
import org.demo.whs.mapper.InventoryMapper;
import org.demo.whs.mapper.StockMovementsMapper;
import org.demo.whs.repository.BatchRepository;
import org.demo.whs.repository.InventoryRepository;
import org.demo.whs.repository.InventoryReservationRepository;
import org.demo.whs.repository.LocationRepository;
import org.demo.whs.repository.ProductRepository;
import org.demo.whs.repository.WareHouseRepository;
import org.demo.whs.repository.specification.InventorySpecification;
import org.demo.whs.service.InventoryService;
import org.demo.whs.service.StockMovementsService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.demo.whs.exception.ErrorCode.PROD_001;


@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryReservationRepository inventoryReservationRepository;
    private final InventoryMapper inventoryMapper;
    private final ProductRepository productRepository;
    private final WareHouseRepository wareHouseRepository;
    private final LocationRepository locationRepository;
    private final BatchRepository batchRepository;
    private final StockMovementsService stockMovementsService;
    private final StockMovementsMapper stockMovementsMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<InventoryResponse> getInventories(InventoryFilterRequest filter, Pageable pageable) {

        log.info("Fetching inventories with filter: {}, pageable: {}", filter, pageable);

        validatePageable(pageable);

        Page<Inventory> inventoryPage = inventoryRepository.findAll(
                InventorySpecification.withFilter(filter),
                pageable
        );

        List<Inventory> content = inventoryPage.getContent();

        if (content.isEmpty()) {
            return PageResponse.from(inventoryPage, List.of());
        }

        Set<String> productIds = content.stream()
                .map(Inventory::getProductId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<String> warehouseIds = content.stream()
                .map(Inventory::getWarehouseId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<String> locationIds = content.stream()
                .map(Inventory::getLocationId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<String> batchIds = content.stream()
                .map(Inventory::getBatchId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<String, Products> productMap = productRepository.findAllById(productIds)
                .stream()
                .collect(Collectors.toMap(Products::getId, p -> p));

        Map<String, Warehouses> warehouseMap = wareHouseRepository.findAllById(warehouseIds)
                .stream()
                .collect(Collectors.toMap(Warehouses::getId, w -> w));

        Map<String, Locations> locationMap = locationRepository.findAllById(locationIds)
                .stream()
                .collect(Collectors.toMap(Locations::getId, l -> l));

        Map<String, Batch> batchMap = batchRepository.findAllById(batchIds)
                .stream()
                .collect(Collectors.toMap(Batch::getId, b -> b));

        List<InventoryResponse> responses = inventoryMapper.toResponses(
                content,
                productMap,
                warehouseMap,
                locationMap,
                batchMap
        );

        return PageResponse.from(inventoryPage, responses);
    }

    @Override
    @Transactional(readOnly = true)
    public InventorySummaryResponse getSummaryByProduct(String productId) {
        log.info("Getting inventory summary for product ID: {}", productId);

        return inventoryRepository.getSummaryByProductId(productId)
                .orElseThrow(() -> new NotFoundException(PROD_001));
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryByLocationResponse> getInventoryByLocation(InventoryFilterRequest filter) {
        log.info("Getting inventory grouped by location for filters: {}", filter);

        List<InventoryLocationProjection> projections = inventoryRepository.getInventoryByLocation(filter);

        Map<String, InventoryByLocationResponse> responseMap = new LinkedHashMap<>();

        for (InventoryLocationProjection p : projections) {
            String locationGroupKey = buildLocationGroupKey(p);
            InventoryByLocationResponse locationResponse = responseMap.computeIfAbsent(locationGroupKey, id ->
                InventoryByLocationResponse.builder()
                    .locationId(p.getLocationId())
                    .locationCode(p.getLocationCode())
                    .locationName(p.getLocationName() != null ? p.getLocationName() : "Unassigned")
                    .warehouseId(p.getWarehouseId())
                    .warehouseName(p.getWarehouseName())
                    .items(new ArrayList<>())
                    .build()
            );

            BigDecimal onHand = p.getOnHandQuantity() != null ? p.getOnHandQuantity() : BigDecimal.ZERO;
            BigDecimal quarantine = p.getQuarantineQuantity() != null ? p.getQuarantineQuantity() : BigDecimal.ZERO;
            BigDecimal reserved = p.getReservedQuantity() != null ? p.getReservedQuantity() : BigDecimal.ZERO;

            locationResponse.getItems().add(
                LocationInventoryItemResponse.builder()
                    .productId(p.getProductId())
                    .productSku(p.getProductSku())
                    .productName(p.getProductName())
                    .batchId(p.getBatchId())
                    .batchNumber(p.getBatchNumber())
                    .onHandQuantity(onHand)
                    .reservedQuantity(reserved)
                    .availableQuantity(onHand.subtract(quarantine).subtract(reserved))
                    .build()
            );
        }

        return new ArrayList<>(responseMap.values());
    }

    /* ----------PRIVATE METHOD--------------*/
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
    @Override
    @Transactional(readOnly = true)
    public CheckAvailabilityResponse checkAvailability(CheckAvailabilityRequest request) {

        log.info("Checking inventory availability for request: {}", request);

        // 1. Validate product
        productRepository.findById(request.getProductId())
                .orElseThrow(() -> new NotFoundException(PROD_001));

        // 2. Validate warehouse (optional)
        Warehouses warehouse = null;
        if (request.getWarehouseId() != null) {
            warehouse = wareHouseRepository.findById(request.getWarehouseId())
                    .orElseThrow(() -> new NotFoundException(ErrorCode.WHS_001));
        }

        // 3. Validate location (optional)
        Locations location = null;
        if (request.getLocationId() != null) {
            location = locationRepository.findById(request.getLocationId())
                    .orElseThrow(() -> new NotFoundException(ErrorCode.LOC_001));
        }

        if (warehouse != null && location != null
                && !warehouse.getId().equals(location.getWarehouseId())) {
            throw new BadRequestException(ErrorCode.COM_001);
        }

        // 4. Aggregate inventory
        CheckAvailabilityResponse availability = inventoryRepository.getAvailability(
                request.getProductId(),
                request.getWarehouseId(),
                request.getLocationId()
        );

        BigDecimal availableQuantity = availability.getAvailableQuantity();
        boolean isAvailable = availableQuantity.compareTo(request.getQuantity()) >= 0;

        return inventoryMapper.toCheckAvailabilityResponse(
                request,
                availableQuantity,
                isAvailable
        );
    }

    @Override
    @Transactional
    public InventoryReserveResponse reserve(InventoryReserveRequest request) {
        log.info("Reserving inventory for request: {}", request);

        // 0. Validate quantity
        if (request.getQuantity() == null || request.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException(ErrorCode.COM_001);
        }

        // 1. Idempotency Check: Prioritize orderLineId
        Optional<InventoryReservation> existing = findExistingReservation(request);
        if (existing.isPresent()) {
            return handleExistingReservation(existing.get());
        }

        // 2. Optimized Database Selection (Allocation Strategy)
        Inventory inventory = inventoryRepository.findBestSuitableForUpdate(
                request.getProductId(),
                request.getWarehouseId(),
                request.getLocationId(),
                request.getBatchId(),
                request.getQuantity()
        ).orElseThrow(() -> {
            log.warn("No suitable inventory row found for reservation. Requested: {}", request.getQuantity());
            return new ConflictException(ErrorCode.INV_004);
        });

        // Capture available quantity BEFORE update
        BigDecimal availableBefore = inventory.getAvailableQuantity();

        // 3. Update Inventory (Reserved Quantity)
        inventory.setReservedQuantity(inventory.getReservedQuantity().add(request.getQuantity()));
        inventory.setLastMovementAt(LocalDateTime.now());
        inventoryRepository.save(inventory);

        // 4. Record Stock Movement (Audit Trail)
        StockMovements movement = stockMovementsMapper.toEntity(
                StockMovementsType.RESERVE,
                inventory.getProductId(),
                inventory.getWarehouseId(),
                inventory.getLocationId(),
                inventory.getBatchId(),
                request.getQuantity().negate(), // Reduced available stock
                availableBefore,
                inventory.getAvailableQuantity(),
                ReferenceType.SALES_ORDER,
                request.getOrderLineId(),
                null, // Reference number if available
                "System Reservation for Order Line: " + request.getOrderLineId(),
                null // Current user ID if context available
        );
        stockMovementsService.recordMovement(movement);

        // 5. Persist Reservation Ledger with Uniqueness enforcement
        InventoryReservation reservation = InventoryReservation.builder()
                .inventoryId(inventory.getId())
                .productId(inventory.getProductId())
                .warehouseId(inventory.getWarehouseId())
                .locationId(inventory.getLocationId())
                .batchId(inventory.getBatchId())
                .quantity(request.getQuantity())
                .orderLineId(request.getOrderLineId())
                .requestKey(request.getRequestKey())
                .status(InventoryReservationStatus.RESERVED)
                .build();
        
        try {
            inventoryReservationRepository.saveAndFlush(reservation);
        } catch (DataIntegrityViolationException ex) {
            log.warn("Race condition or Duplicate detected for orderLineId: {}. Fetching existing record.", request.getOrderLineId());
            return findExistingReservation(request)
                    .map(this::handleExistingReservation)
                    .orElseThrow(() -> ex);
        }

        log.info("Successfully reserved {} for order line: {} (inventory id: {})", 
                request.getQuantity(), request.getOrderLineId(), inventory.getId());

        return inventoryMapper.toReserveResponse(reservation, inventory);
    }

    @Override
    @Transactional
    public InventoryUnreserveResponse unreserve(InventoryUnreserveRequest request) {
        log.info("Unreserving inventory for request: {}", request);

        // 0. Validate quantity
        BigDecimal unreserveQty = request.getQuantity();
        if (unreserveQty == null || unreserveQty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException(ErrorCode.COM_001);
        }

        // 1. Find the existing reservation (ledger)
        InventoryReservation reservation = inventoryReservationRepository.findByOrderLineId(request.getOrderLineId())
                .orElseThrow(() -> {
                    log.warn("No reservation found for order line: {}", request.getOrderLineId());
                    return new NotFoundException(ErrorCode.INV_001);
                });

        // 2. Validate Reservation Context (ProductId/WarehouseId must match)
        if (!reservation.getProductId().equals(request.getProductId()) ||
            !reservation.getWarehouseId().equals(request.getWarehouseId())) {
            log.warn("Request dimensions do not match reservation. Request: {} vs Ledger: {}", 
                    request.getProductId(), reservation.getProductId());
            throw new BadRequestException(ErrorCode.COM_001);
        }

        // 3. IDEMPOTENCY: If already released, return success with 0 unreserved
        if (reservation.getStatus() == InventoryReservationStatus.RELEASED || 
            reservation.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            log.info("Reservation already fully released for order line: {}", request.getOrderLineId());
            return inventoryMapper.toUnreserveResponse(reservation, BigDecimal.ZERO);
        }

        // 4. Check if unreserve quantity is valid (Cannot unreserve more than held)
        if (unreserveQty.compareTo(reservation.getQuantity()) > 0) {
            log.warn("Unreserve quantity {} exceeds reserved quantity {}", unreserveQty, reservation.getQuantity());
            throw new ConflictException(ErrorCode.INV_002);
        }

        // 5. Lock and Update Inventory Record
        Inventory inventory = inventoryRepository.findByIdForUpdate(reservation.getInventoryId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.INV_001));

        // Capture available quantity BEFORE update
        BigDecimal availableBefore = inventory.getAvailableQuantity();

        // 6. FINAL GUARD: Ensure Inventory record itself has enough reserved stock to subtract
        if (inventory.getReservedQuantity().compareTo(unreserveQty) < 0) {
            log.error("Data inconsistency: Inventory reservedQty {} < request {}", 
                    inventory.getReservedQuantity(), unreserveQty);
            throw new ConflictException(ErrorCode.INV_002);
        }

        // 7. Update Inventory (Decrease reserved quantity)
        inventory.setReservedQuantity(inventory.getReservedQuantity().subtract(unreserveQty));
        inventory.setLastMovementAt(LocalDateTime.now());
        inventoryRepository.save(inventory);

        // 8. Record Stock Movement (Audit Trail)
        StockMovements movement = stockMovementsMapper.toEntity(
                StockMovementsType.UNRESERVE,
                inventory.getProductId(),
                inventory.getWarehouseId(),
                inventory.getLocationId(),
                inventory.getBatchId(),
                unreserveQty, // Increased available stock
                availableBefore,
                inventory.getAvailableQuantity(),
                ReferenceType.SALES_ORDER,
                request.getOrderLineId(),
                null,
                "System Unreservation for Order Line: " + request.getOrderLineId(),
                null
        );
        stockMovementsService.recordMovement(movement);

        // 9. Update or Delete Reservation Record (Ledger)
        BigDecimal newQuantity = reservation.getQuantity().subtract(unreserveQty);
        reservation.setQuantity(newQuantity); // Update the object first for the response mapper
        
        if (newQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            log.info("Reservation fully released. Deleting record for order line: {}", request.getOrderLineId());
            inventoryReservationRepository.delete(reservation);
        } else {
            inventoryReservationRepository.save(reservation);
        }

        log.info("Successfully unreserved {} for order line: {} (inventory id: {})", 
                unreserveQty, request.getOrderLineId(), inventory.getId());

        return inventoryMapper.toUnreserveResponse(reservation, unreserveQty);
    }

    private Optional<InventoryReservation> findExistingReservation(InventoryReserveRequest request) {
        if (request.getOrderLineId() != null) {
            return inventoryReservationRepository.findByOrderLineId(request.getOrderLineId());
        }
        if (request.getRequestKey() != null) {
            return inventoryReservationRepository.findByRequestKey(request.getRequestKey());
        }
        return Optional.empty();
    }

    private InventoryReserveResponse handleExistingReservation(InventoryReservation reservation) {
        log.info("Returning existing reservation: {}", reservation.getId());
        Inventory inv = inventoryRepository.findById(reservation.getInventoryId())
                .orElseThrow(() -> new ConflictException(ErrorCode.INV_001));
        return inventoryMapper.toReserveResponse(reservation, inv);
    }

    private String buildLocationGroupKey(InventoryLocationProjection projection) {
        return projection.getWarehouseId() + ":" +
                (projection.getLocationId() != null ? projection.getLocationId() : "UNASSIGNED");
    }
}
