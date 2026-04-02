package org.demo.whs.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.*;
import org.demo.whs.entity.dto.request.Inventory.CheckAvailabilityRequest;
import org.demo.whs.entity.dto.request.Inventory.InventoryFilterRequest;
import org.demo.whs.entity.dto.request.Inventory.InventoryIncreaseRequest;
import org.demo.whs.entity.dto.request.Inventory.InventoryReserveRequest;
import org.demo.whs.entity.dto.request.Inventory.InventoryUnreserveRequest;
import org.demo.whs.entity.dto.request.Inventory.InventoryDecreaseRequest;
import org.demo.whs.entity.dto.response.Inventory.CheckAvailabilityResponse;
import org.demo.whs.entity.dto.response.Inventory.InventoryByLocationResponse;
import org.demo.whs.entity.dto.response.Inventory.InventoryLocationProjection;
import org.demo.whs.entity.dto.response.Inventory.InventoryReserveResponse;
import org.demo.whs.entity.dto.response.Inventory.InventoryResponse;
import org.demo.whs.entity.dto.response.Inventory.InventorySummaryResponse;
import org.demo.whs.entity.dto.response.Inventory.InventoryUnreserveResponse;
import org.demo.whs.entity.dto.response.Inventory.LocationInventoryItemResponse;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.enums.InventoryReservationStatus;
import org.demo.whs.entity.enums.LocationStatus;
import org.demo.whs.entity.enums.ReferenceType;
import org.demo.whs.entity.enums.StockMovementsType;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.ConflictException;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.exception.NotFoundException;
import org.demo.whs.mapper.InventoryMapper;
import org.demo.whs.mapper.StockMovementsMapper;
import org.demo.whs.repository.*;
import org.demo.whs.repository.specification.InventorySpecification;
import org.demo.whs.service.InventoryService;
import org.demo.whs.service.LocationService;
import org.demo.whs.service.StockMovementsService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
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
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.demo.whs.entity.enums.InventoryReservationStatus.RELEASED;
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
    private final RedissonClient redissonClient;
    private final SalesOrderLinesRepository salesOrderLinesRepository;
    private final LocationService locationService;

    private static final String LOCK_PREFIX = "lock:inventory:reference:";

    private String buildLockKey(ReferenceType type, String referenceId, String referenceNumber) {
        if (referenceId != null && !referenceId.isBlank()) {
            return LOCK_PREFIX + type + ":" + referenceId;
        }
        if (referenceNumber != null && !referenceNumber.isBlank()) {
            return LOCK_PREFIX + type + ":" + referenceNumber;
        }

        throw new BadRequestException("Missing idempotency key", ErrorCode.COM_001);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<InventoryResponse> getInventories(InventoryFilterRequest filter, Pageable pageable) {

        log.info("Fetching inventories with filter: {}, pageable: {}", filter, pageable);

        validatePageable(pageable);

        Page<Inventory> inventoryPage = inventoryRepository.findAll(InventorySpecification.withFilter(filter), pageable);

        List<Inventory> content = inventoryPage.getContent();

        if (content.isEmpty()) {
            return PageResponse.from(inventoryPage, List.of());
        }

        Set<String> productIds = content.stream().map(Inventory::getProductId).filter(Objects::nonNull).collect(Collectors.toSet());

        Set<String> warehouseIds = content.stream().map(Inventory::getWarehouseId).filter(Objects::nonNull).collect(Collectors.toSet());

        Set<String> locationIds = content.stream().map(Inventory::getLocationId).filter(Objects::nonNull).collect(Collectors.toSet());

        Set<String> batchIds = content.stream().map(Inventory::getBatchId).filter(Objects::nonNull).collect(Collectors.toSet());

        Map<String, Products> productMap = productRepository.findAllById(productIds).stream().collect(Collectors.toMap(Products::getId, p -> p));

        Map<String, Warehouses> warehouseMap = wareHouseRepository.findAllById(warehouseIds).stream().collect(Collectors.toMap(Warehouses::getId, w -> w));

        Map<String, Locations> locationMap = locationRepository.findAllById(locationIds).stream().collect(Collectors.toMap(Locations::getId, l -> l));

        Map<String, Batch> batchMap = batchRepository.findAllById(batchIds).stream().collect(Collectors.toMap(Batch::getId, b -> b));

        List<InventoryResponse> responses = inventoryMapper.toResponses(content, productMap, warehouseMap, locationMap, batchMap);

        return PageResponse.from(inventoryPage, responses);
    }

    @Override
    @Transactional(readOnly = true)
    public InventorySummaryResponse getSummaryByProduct(String productId) {
        log.info("Getting inventory summary for product ID: {}", productId);

        return inventoryRepository.getSummaryByProductId(productId).orElseThrow(() -> new NotFoundException(PROD_001));
    }

    @Override
    @Transactional(readOnly = true)
    public InventorySummaryResponse getSummaryByProductAndWarehouse(String productId, String warehouseId) {
        log.info("Getting inventory summary for product ID: {} in warehouse: {}", productId, warehouseId);

        return inventoryRepository.getSummaryByProductIdAndWarehouseId(productId, warehouseId)
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
            InventoryByLocationResponse locationResponse = responseMap.computeIfAbsent(locationGroupKey, id -> InventoryByLocationResponse.builder().locationId(p.getLocationId()).locationCode(p.getLocationCode()).locationName(p.getLocationName() != null ? p.getLocationName() : "Unassigned").warehouseId(p.getWarehouseId()).warehouseName(p.getWarehouseName()).items(new ArrayList<>()).build());

            BigDecimal onHand = p.getOnHandQuantity() != null ? p.getOnHandQuantity() : BigDecimal.ZERO;
            BigDecimal quarantine = p.getQuarantineQuantity() != null ? p.getQuarantineQuantity() : BigDecimal.ZERO;
            BigDecimal reserved = p.getReservedQuantity() != null ? p.getReservedQuantity() : BigDecimal.ZERO;

            locationResponse.getItems().add(LocationInventoryItemResponse.builder().productId(p.getProductId()).productSku(p.getProductSku()).productName(p.getProductName()).batchId(p.getBatchId()).batchNumber(p.getBatchNumber()).onHandQuantity(onHand).reservedQuantity(reserved).availableQuantity(onHand.subtract(quarantine).subtract(reserved)).build());
        }

        return new ArrayList<>(responseMap.values());
    }

    @Override
    @Transactional(readOnly = true)
    public CheckAvailabilityResponse checkAvailability(CheckAvailabilityRequest request) {

        log.info("Checking inventory availability for request: {}", request);

        // 1. Validate product
        productRepository.findById(request.getProductId()).orElseThrow(() -> new NotFoundException(PROD_001));

        // 2. Validate warehouse (optional)
        Warehouses warehouse = null;
        if (request.getWarehouseId() != null) {
            warehouse = wareHouseRepository.findById(request.getWarehouseId()).orElseThrow(() -> new NotFoundException(ErrorCode.WHS_001));
        }

        // 3. Validate location (optional)
        Locations location = null;
        if (request.getLocationId() != null) {
            location = locationRepository.findById(request.getLocationId()).orElseThrow(() -> new NotFoundException(ErrorCode.LOC_001));
        }

        if (warehouse != null && location != null && !warehouse.getId().equals(location.getWarehouseId())) {
            throw new BadRequestException(ErrorCode.COM_001);
        }

        // 4. Aggregate inventory
        CheckAvailabilityResponse availability = inventoryRepository.getAvailability(request.getProductId(), request.getWarehouseId(), request.getLocationId());

        BigDecimal availableQuantity = availability.getAvailableQuantity();
        boolean isAvailable = availableQuantity.compareTo(request.getQuantity()) >= 0;

        return inventoryMapper.toCheckAvailabilityResponse(request, availableQuantity, isAvailable);
    }

    @Override
    @Transactional
    public InventoryReserveResponse reserve(InventoryReserveRequest request) {

        log.info("Reserving inventory for request: {}", request);

        if (request.getOrderLineId() == null || request.getOrderLineId().isBlank()) {
            throw new BadRequestException("orderLineId is required", ErrorCode.COM_001);
        }

        if (request.getQuantity() == null || request.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException(ErrorCode.COM_001);
        }
        SalesOrderLines orderLine = salesOrderLinesRepository.findById(request.getOrderLineId()).orElseThrow(() -> new NotFoundException(ErrorCode.ORDER_001));

        if (!orderLine.getProductId().equals(request.getProductId())) {
            log.error("Product ID mismatch: request={}, line={}", request.getProductId(), orderLine.getProductId());
            throw new BadRequestException("Product ID mismatch between request and order line", ErrorCode.COM_001);
        }

        String lockKey = "lock:reserve:" + request.getOrderLineId();
        RLock lock = redissonClient.getLock(lockKey);

        try {
            if (!lock.tryLock(10, 30, TimeUnit.SECONDS)) {
                throw new ConflictException(ErrorCode.COM_009);
            }

            try {
                Optional<InventoryReservation> existing = inventoryReservationRepository.findByOrderLineId(request.getOrderLineId());

                if (existing.isPresent()) {
                    Inventory inv = inventoryRepository.findById(existing.get().getInventoryId()).orElse(null);

                    return inventoryMapper.toReserveResponse(existing.get(), inv);
                }

                Inventory inventory = inventoryRepository.findBestSuitableForUpdate(orderLine.getProductId(), request.getWarehouseId(), request.getLocationId(), request.getBatchId(), request.getQuantity()).orElseThrow(() -> new ConflictException(ErrorCode.INV_004));

                BigDecimal availableBefore = inventory.getAvailableQuantity();

                if (inventory.getAvailableQuantity().compareTo(request.getQuantity()) < 0) {
                    throw new ConflictException(ErrorCode.INV_004);
                }

                inventory.setReservedQuantity(inventory.getReservedQuantity().add(request.getQuantity()));
                inventory.setLastMovementAt(LocalDateTime.now());
                inventoryRepository.save(inventory);

                InventoryReservation reservation = InventoryReservation.builder().inventoryId(inventory.getId()).productId(inventory.getProductId()).warehouseId(inventory.getWarehouseId()).locationId(inventory.getLocationId()).batchId(inventory.getBatchId()).quantity(request.getQuantity()).orderLineId(request.getOrderLineId()).status(InventoryReservationStatus.RESERVED).build();

                inventoryReservationRepository.save(reservation);

                StockMovements movement = stockMovementsMapper.toEntity(StockMovementsType.RESERVE, inventory.getProductId(), inventory.getWarehouseId(), inventory.getLocationId(), inventory.getBatchId(), request.getQuantity().negate(), availableBefore, inventory.getAvailableQuantity(), ReferenceType.SALES_ORDER, request.getOrderLineId(), null, "Reservation for orderLine: " + request.getOrderLineId(), null);

                stockMovementsService.recordMovement(movement);
                return inventoryMapper.toReserveResponse(reservation, inventory);

            } finally {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ConflictException(ErrorCode.COM_010);
        }
    }

    @Override
    @Transactional
    public InventoryUnreserveResponse unreserve(InventoryUnreserveRequest request) {

        log.info("Unreserving inventory for orderLineId={}, qty={}", request.getOrderLineId(), request.getQuantity());

        if (request.getOrderLineId() == null || request.getOrderLineId().isBlank()) {
            throw new BadRequestException("orderLineId is required", ErrorCode.COM_001);
        }

        BigDecimal unreserveQty = request.getQuantity();
        if (unreserveQty == null || unreserveQty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException(ErrorCode.COM_001);
        }
        InventoryReservation reservation = inventoryReservationRepository.findByOrderLineId(request.getOrderLineId()).orElseThrow(() -> {
            log.warn("No reservation found for order line: {}", request.getOrderLineId());
            return new NotFoundException(ErrorCode.INV_001);
        });

        // Cross-validate with request to ensure we are unreserving the right thing
        if (request.getProductId() != null && !request.getProductId().equals(reservation.getProductId())) {
            throw new BadRequestException("Product ID mismatch", ErrorCode.COM_001);
        }
        if (request.getWarehouseId() != null && !request.getWarehouseId().equals(reservation.getWarehouseId())) {
            throw new BadRequestException("Warehouse ID mismatch", ErrorCode.COM_001);
        }

        if (reservation.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            log.info("Nothing to unreserve for orderLineId={}", request.getOrderLineId());
            return inventoryMapper.toUnreserveResponse(reservation, BigDecimal.ZERO);
        }

        if (unreserveQty.compareTo(reservation.getQuantity()) > 0) {
            log.warn("Requested unreserve {} > reserved {}, clamping", unreserveQty, reservation.getQuantity());
            unreserveQty = reservation.getQuantity();
        }

        Inventory inventory = inventoryRepository.findByIdForUpdate(reservation.getInventoryId()).orElseThrow(() -> new NotFoundException(ErrorCode.INV_001));
        BigDecimal expectedReserved = inventoryReservationRepository.sumQuantityByInventoryIdAndStatus(
                inventory.getId(),
                InventoryReservationStatus.RESERVED
        );

        if (inventory.getReservedQuantity().compareTo(expectedReserved) != 0) {
            log.warn("Healing reserved aggregate before unreserve: inventoryId={}, reserved={} -> {}",
                    inventory.getId(), inventory.getReservedQuantity(), expectedReserved);
            inventory.setReservedQuantity(expectedReserved);
        }

        BigDecimal availableBefore = inventory.getAvailableQuantity();

        if (inventory.getReservedQuantity().compareTo(unreserveQty) < 0) {
            log.error("Data inconsistency: Inventory reserved {} < unreserve request {}", 
                inventory.getReservedQuantity(), unreserveQty);
            throw new ConflictException(ErrorCode.INV_002);
        }

        inventory.setReservedQuantity(inventory.getReservedQuantity().subtract(unreserveQty));
        inventory.setLastMovementAt(LocalDateTime.now());
        inventoryRepository.save(inventory);

        StockMovements movement = stockMovementsMapper.toEntity(StockMovementsType.UNRESERVE, inventory.getProductId(), inventory.getWarehouseId(), inventory.getLocationId(), inventory.getBatchId(), unreserveQty, availableBefore, inventory.getAvailableQuantity(), ReferenceType.SALES_ORDER, request.getOrderLineId(), null, "Unreserve for orderLine: " + request.getOrderLineId(), null);

        stockMovementsService.recordMovement(movement);

        BigDecimal newQty = reservation.getQuantity().subtract(unreserveQty);
        reservation.setQuantity(newQty);

        if (newQty.compareTo(BigDecimal.ZERO) <= 0) {
            log.info("Reservation fully released for orderLineId={}", request.getOrderLineId());
            inventoryReservationRepository.delete(reservation);
        } else {
            inventoryReservationRepository.save(reservation);
        }

        log.info("Unreserved {} for orderLineId={}, remaining={}", unreserveQty, request.getOrderLineId(), newQty);

        return inventoryMapper.toUnreserveResponse(reservation, unreserveQty);
    }

    @Override
    @Transactional
    public InventoryResponse increase(InventoryIncreaseRequest request) {
        String lockKey = buildLockKey(request.getReferenceType(), request.getReferenceId(), request.getReferenceNumber());
        RLock lock = redissonClient.getLock(lockKey);

        try {
            if (lock.tryLock(10, 30, TimeUnit.SECONDS)) {
                try {
                    log.info("Increasing inventory product={} warehouse={} qty={}", request.getProductId(), request.getWarehouseId(), request.getQuantity());

                    // 1. Validate quantity
                    if (request.getQuantity() == null || request.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                        throw new BadRequestException(ErrorCode.COM_001);
                    }

                    // 2. Validate Dimensions
                    Products product = productRepository.findById(request.getProductId()).orElseThrow(() -> new NotFoundException(ErrorCode.PROD_001));
                    Warehouses warehouse = wareHouseRepository.findById(request.getWarehouseId()).orElseThrow(() -> new NotFoundException(ErrorCode.WHS_001));

                    Locations location = null;
                    if (request.getLocationId() != null) {
                        location = locationRepository.findById(request.getLocationId()).orElseThrow(() -> new NotFoundException(ErrorCode.LOC_001));
                        if (!warehouse.getId().equals(location.getWarehouseId())) {
                            throw new BadRequestException("Location does not belong to warehouse", ErrorCode.COM_001);
                        }
                    }

                    Batch batch = null;
                    if (request.getBatchId() != null) {
                        batch = batchRepository.findById(request.getBatchId()).orElseThrow(() -> new NotFoundException(ErrorCode.BATCH_001));
                        if (!batch.getProductId().equals(request.getProductId())) {
                            throw new ConflictException(String.format("Batch %s belongs to product %s, but request is for product %s", batch.getId(), batch.getProductId(), request.getProductId()), ErrorCode.COM_001);
                        }
                    }

                    // 3. Find or Create Inventory Record with Locking
                    Inventory inventory = findOrCreateInventoryWithLock(request);

                    // 4. Update Inventory
                    BigDecimal onHandBefore = inventory.getOnHandQuantity();
                    inventory.setOnHandQuantity(onHandBefore.add(request.getQuantity()));
                    inventory.setLastMovementAt(LocalDateTime.now());
                    inventory = inventoryRepository.save(inventory);

                    // 5. Record Stock Movement with Atomic Idempotency (Catch DB Unique Constraint)
                    try {
                        stockMovementsService.recordIncrease(request, onHandBefore, inventory.getOnHandQuantity());
                    } catch (DataIntegrityViolationException e) {
                        log.warn("Duplicate request detected at DB level for reference: {}", request.getReferenceId());
                        throw new ConflictException("Duplicate request", ErrorCode.COM_001);
                    }

                    log.info("Successfully increased inventory. New on-hand: {}", inventory.getOnHandQuantity());
                    return inventoryMapper.toResponse(inventory, product, warehouse, location, batch);

                } finally {
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                }
            } else {
                throw new ConflictException(ErrorCode.COM_009);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ConflictException(ErrorCode.COM_010);
        }
    }

    @Override
    @Transactional
    public InventoryResponse decrease(InventoryDecreaseRequest request) {
        String lockKey = buildLockKey(request.getReferenceType(), request.getReferenceId(), request.getReferenceNumber());
        RLock lock = redissonClient.getLock(lockKey);

        try {
            if (lock.tryLock(10, 30, TimeUnit.SECONDS)) {
                try {
                    log.info("Decreasing inventory product={} warehouse={} qty={} consumeReserved={}", request.getProductId(), request.getWarehouseId(), request.getQuantity(), request.isConsumeReserved());

                    // 1. Validate quantity
                    if (request.getQuantity() == null || request.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                        throw new BadRequestException(ErrorCode.COM_001);
                    }

                    // 2. Find Inventory with Lock
                    Inventory inventory = inventoryRepository.findByDimensionForUpdate(request.getProductId(), request.getWarehouseId(), request.getLocationId(), request.getBatchId()).orElseThrow(() -> new NotFoundException(ErrorCode.INV_001));

                    // 3. Handle Scenarios
                    BigDecimal onHandBefore = inventory.getOnHandQuantity();
                    BigDecimal reservedBefore = inventory.getReservedQuantity();
                    InventoryReservation reservation = null;

                    if (request.isConsumeReserved()) {
                        if (request.getOrderLineId() != null && !request.getOrderLineId().isBlank()) {
                            reservation = inventoryReservationRepository.findByOrderLineId(request.getOrderLineId())
                                    .orElseThrow(() -> new NotFoundException(ErrorCode.INV_001));

                            BigDecimal expectedReserved = inventoryReservationRepository.sumQuantityByInventoryIdAndStatus(
                                    inventory.getId(),
                                    InventoryReservationStatus.RESERVED
                            );
                            BigDecimal healedReserved = expectedReserved.max(reservation.getQuantity());

                            if (inventory.getReservedQuantity().compareTo(healedReserved) != 0) {
                                log.warn("Healing reserved aggregate before consumeReserved decrease: inventoryId={}, reserved={} -> {}",
                                        inventory.getId(), inventory.getReservedQuantity(), healedReserved);
                                inventory.setReservedQuantity(healedReserved);
                            }

                            if (!inventory.getId().equals(reservation.getInventoryId())
                                    || !java.util.Objects.equals(inventory.getLocationId(), reservation.getLocationId())) {
                                log.warn("Healing reservation pointer before consumeReserved decrease: orderLineId={}, inventory {} -> {}, location {} -> {}",
                                        request.getOrderLineId(), reservation.getInventoryId(), inventory.getId(),
                                        reservation.getLocationId(), inventory.getLocationId());
                                reservation.setInventoryId(inventory.getId());
                                reservation.setLocationId(inventory.getLocationId());
                            }

                            if (reservation.getQuantity().compareTo(request.getQuantity()) < 0) {
                                throw new ConflictException(ErrorCode.INV_004);
                            }

                            reservedBefore = inventory.getReservedQuantity();
                        }

                        if (reservedBefore.compareTo(request.getQuantity()) < 0) {
                            throw new ConflictException(ErrorCode.INV_004);
                        }
                        inventory.setOnHandQuantity(onHandBefore.subtract(request.getQuantity()));
                        inventory.setReservedQuantity(reservedBefore.subtract(request.getQuantity()));

                        // Release reservation if orderLineId is provided
                        if (reservation != null) {
                            BigDecimal newResQty = reservation.getQuantity().subtract(request.getQuantity());
                            if (newResQty.compareTo(BigDecimal.ZERO) <= 0) {
                                inventoryReservationRepository.delete(reservation);
                            } else {
                                reservation.setQuantity(newResQty);
                                inventoryReservationRepository.save(reservation);
                            }
                        } else if (request.getOrderLineId() != null && !request.getOrderLineId().isBlank()) {
                            inventoryReservationRepository.findByOrderLineId(request.getOrderLineId())
                                    .ifPresent(existingReservation -> {
                                        BigDecimal newResQty = existingReservation.getQuantity().subtract(request.getQuantity());
                                        if (newResQty.compareTo(BigDecimal.ZERO) <= 0) {
                                            inventoryReservationRepository.delete(existingReservation);
                                        } else {
                                            existingReservation.setQuantity(newResQty);
                                            inventoryReservationRepository.save(existingReservation);
                                        }
                                    });
                        }
                    } else {
                        if (inventory.getAvailableQuantity().compareTo(request.getQuantity()) < 0) {
                            throw new ConflictException(ErrorCode.INV_004);
                        }
                        inventory.setOnHandQuantity(onHandBefore.subtract(request.getQuantity()));
                    }

                    // 4. Update
                    inventory.setLastMovementAt(LocalDateTime.now());
                    inventory = inventoryRepository.save(inventory);

                    // 5. Record Stock Movement with Atomic Idempotency (Catch DB Unique Constraint)
                    try {
                        stockMovementsService.recordDecrease(request, onHandBefore, inventory.getOnHandQuantity());
                    } catch (DataIntegrityViolationException e) {
                        log.warn("Duplicate request detected at DB level for reference: {}", request.getReferenceId());
                        throw new ConflictException("Duplicate request", ErrorCode.COM_001);
                    }

                    log.info("Successfully decreased inventory. New on-hand: {}, New reserved: {}", inventory.getOnHandQuantity(), inventory.getReservedQuantity());

                    // 6. Response
                    Products product = productRepository.findById(inventory.getProductId()).orElse(null);
                    Warehouses warehouse = wareHouseRepository.findById(inventory.getWarehouseId()).orElse(null);
                    Locations location = inventory.getLocationId() != null ? locationRepository.findById(inventory.getLocationId()).orElse(null) : null;
                    Batch batch = inventory.getBatchId() != null ? batchRepository.findById(inventory.getBatchId()).orElse(null) : null;

                    return inventoryMapper.toResponse(inventory, product, warehouse, location, batch);
                } finally {
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                }
            } else {
                throw new ConflictException(ErrorCode.COM_009);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ConflictException(ErrorCode.COM_010);
        }
    }

    @Override
    @Transactional
    public void moveInventory(
            String fromLocationId,
            String toLocationId,
            String productId,
            String batchId,
            BigDecimal quantity,
            ReferenceType referenceType,
            String referenceId,
            String orderLineId,
            boolean consumeReserved
    ) {
        log.info("Moving inventory from {} to {} product {} qty {} consumeReserved={} orderLineId={}",
                fromLocationId, toLocationId, productId, quantity, consumeReserved, orderLineId);

        // 1. Validate
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Quantity must be > 0", ErrorCode.COM_001);
        }

        if (fromLocationId == null || toLocationId == null || fromLocationId.equals(toLocationId)) {
            throw new BadRequestException("Invalid locations", ErrorCode.COM_001);
        }

        // 2. Lock locations
        Locations fromLoc = locationRepository.findByIdForUpdate(fromLocationId)
                .orElseThrow(() -> new NotFoundException("Source location not found", ErrorCode.LOC_001));

        Locations toLoc = locationRepository.findByIdForUpdate(toLocationId)
                .orElseThrow(() -> new NotFoundException("Destination location not found", ErrorCode.LOC_001));

        String warehouseId = fromLoc.getWarehouseId();
        if (!warehouseId.equals(toLoc.getWarehouseId())) {
            throw new BadRequestException("Locations must belong to the same warehouse", ErrorCode.COM_001);
        }

        // 3. Validate location status
        if (fromLoc.getStatus() != LocationStatus.ACTIVE) {
            throw new ConflictException("Source location not active", ErrorCode.LOC_002);
        }
        if (toLoc.getStatus() != LocationStatus.ACTIVE) {
            throw new ConflictException("Destination location not active", ErrorCode.LOC_002);
        }

        // 4. Lock inventory
        Inventory sourceInv = inventoryRepository.findByDimensionForUpdate(
                productId, warehouseId, fromLocationId, batchId
        ).orElseThrow(() -> new NotFoundException("Inventory not found at source", ErrorCode.INV_001));

        Inventory destInv = inventoryRepository.findByDimensionForUpdate(
                productId, warehouseId, toLocationId, batchId
        ).orElseGet(() -> {
            Inventory newInv = Inventory.builder()
                    .productId(productId)
                    .warehouseId(warehouseId)
                    .locationId(toLocationId)
                    .batchId(batchId)
                    .onHandQuantity(BigDecimal.ZERO)
                    .reservedQuantity(BigDecimal.ZERO)
                    .version(0)
                    .build();
            return inventoryRepository.saveAndFlush(newInv);
        });

        // 5. Validate stock and handle reservation
        InventoryReservation reservation = null;
        if (consumeReserved || (orderLineId != null && !orderLineId.isBlank())) {
            if (orderLineId != null && !orderLineId.isBlank()) {
                reservation = inventoryReservationRepository.findByOrderLineId(orderLineId)
                        .orElseThrow(() -> new NotFoundException("Reservation not found for order line", ErrorCode.INV_001));

                boolean sameProduct = reservation.getProductId().equals(productId);
                boolean sameWarehouse = reservation.getWarehouseId().equals(warehouseId);
                boolean sameBatch = java.util.Objects.equals(reservation.getBatchId(), batchId);
                if (!sameProduct || !sameWarehouse || !sameBatch) {
                    log.error("Reservation dimension mismatch: orderLineId={}, reservationProduct={}, requestProduct={}, reservationWarehouse={}, requestWarehouse={}, reservationBatch={}, requestBatch={}",
                            orderLineId, reservation.getProductId(), productId, reservation.getWarehouseId(), warehouseId, reservation.getBatchId(), batchId);
                    throw new ConflictException("Reservation mismatch with source inventory", ErrorCode.COM_001);
                }

                if (!sourceInv.getId().equals(reservation.getInventoryId())
                        || !java.util.Objects.equals(fromLocationId, reservation.getLocationId())) {
                    log.warn("Healing reservation pointer for orderLineId={}: inventory {} -> {}, location {} -> {}",
                            orderLineId, reservation.getInventoryId(), sourceInv.getId(), reservation.getLocationId(), fromLocationId);
                    reservation.setInventoryId(sourceInv.getId());
                    reservation.setLocationId(fromLocationId);
                }

                if (reservation.getQuantity().compareTo(quantity) < 0) {
                    log.error("Insufficient reservation quantity for orderLineId={}: reserved={}, requested={}",
                            orderLineId, reservation.getQuantity(), quantity);
                    throw new ConflictException("Not enough reserved stock", ErrorCode.INV_004);
                }

                if (sourceInv.getReservedQuantity().compareTo(quantity) < 0) {
                    BigDecimal healedReserved = sourceInv.getReservedQuantity().max(reservation.getQuantity());
                    log.warn("Healing source reserved quantity for orderLineId={}: inventory {} reserved {} -> {}",
                            orderLineId, sourceInv.getId(), sourceInv.getReservedQuantity(), healedReserved);
                    sourceInv.setReservedQuantity(healedReserved);
                }

                // Do NOT delete the reservation here. Only the ship method can delete it.
                // Just update quantities and location if needed in step 6.
            } else if (sourceInv.getReservedQuantity().compareTo(quantity) < 0) {
                log.error("Insufficient reserved stock at {}: reserved={}, requested={}",
                        fromLocationId, sourceInv.getReservedQuantity(), quantity);
                throw new ConflictException("Not enough reserved stock", ErrorCode.INV_004);
            }

            if (sourceInv.getReservedQuantity().compareTo(quantity) < 0) {
                log.error("Insufficient reserved stock at {} after reconciliation: reserved={}, requested={}",
                        fromLocationId, sourceInv.getReservedQuantity(), quantity);
                throw new ConflictException("Not enough reserved stock", ErrorCode.INV_004);
            }
        } else {
            if (sourceInv.getAvailableQuantity().compareTo(quantity) < 0) {
                log.error("Insufficient available stock at {}: available={}, requested={}", 
                        fromLocationId, sourceInv.getAvailableQuantity(), quantity);
                throw new ConflictException("Not enough available stock", ErrorCode.INV_004);
            }
        }

        BigDecimal sourceBefore = sourceInv.getOnHandQuantity();
        BigDecimal destBefore = destInv.getOnHandQuantity();

        // 6. Update Inventory quantities
        sourceInv.setOnHandQuantity(sourceBefore.subtract(quantity));
        destInv.setOnHandQuantity(destBefore.add(quantity));

        // If moving stock for a specific order line, move the reserved quantity too
        if (orderLineId != null && !orderLineId.isBlank()) {
            sourceInv.setReservedQuantity(sourceInv.getReservedQuantity().subtract(quantity));
            destInv.setReservedQuantity(destInv.getReservedQuantity().add(quantity));

            // Update the reservation record to point to the new location
            reservation.setLocationId(toLocationId);
            reservation.setInventoryId(destInv.getId());
        } else if (consumeReserved) {
            sourceInv.setReservedQuantity(sourceInv.getReservedQuantity().subtract(quantity));
        }

        sourceInv.setLastMovementAt(LocalDateTime.now());
        destInv.setLastMovementAt(LocalDateTime.now());

        inventoryRepository.save(sourceInv);
        inventoryRepository.save(destInv);
        if (reservation != null) {
            inventoryReservationRepository.save(reservation);
        }

        // 7. Record movements
        StockMovements sourceMovement = StockMovements.builder()
                .movementType(StockMovementsType.INTERNAL_MOVE)
                .productId(productId)
                .warehouseId(warehouseId)
                .locationId(fromLocationId)
                .toLocationId(toLocationId)
                .batchId(batchId)
                .quantityChange(quantity.negate())
                .quantityBefore(sourceBefore)
                .quantityAfter(sourceInv.getOnHandQuantity())
                .movementDate(LocalDateTime.now())
                .referenceType(referenceType)
                .referenceId(referenceId)
                .notes("Internal move OUT" + (consumeReserved ? " (Consumed Reserved)" : ""))
                .build();

        StockMovements destMovement = StockMovements.builder()
                .movementType(StockMovementsType.INTERNAL_MOVE)
                .productId(productId)
                .warehouseId(warehouseId)
                .locationId(toLocationId)
                .toLocationId(fromLocationId)
                .batchId(batchId)
                .quantityChange(quantity)
                .quantityBefore(destBefore)
                .quantityAfter(destInv.getOnHandQuantity())
                .movementDate(LocalDateTime.now())
                .referenceType(referenceType)
                .referenceId(referenceId)
                .notes("Internal move IN")
                .build();

        stockMovementsService.recordMovement(sourceMovement);
        stockMovementsService.recordMovement(destMovement);
    }


    private Inventory findOrCreateInventoryWithLock(InventoryIncreaseRequest request) {
        Optional<Inventory> existing = inventoryRepository.findByDimensionForUpdate(request.getProductId(), request.getWarehouseId(), request.getLocationId(), request.getBatchId());

        if (existing.isPresent()) {
            return existing.get();
        }

        Inventory newInventory = inventoryMapper.toEntity(request);
        try {
            return inventoryRepository.saveAndFlush(newInventory);
        } catch (DataIntegrityViolationException e) {
            return inventoryRepository.findByDimensionForUpdate(request.getProductId(), request.getWarehouseId(), request.getLocationId(), request.getBatchId()).orElseThrow(() -> new ConflictException("Concurrent inventory creation failed", ErrorCode.COM_001));
        }
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

    private String buildLocationGroupKey(InventoryLocationProjection projection) {
        return projection.getWarehouseId() + ":" + (projection.getLocationId() != null ? projection.getLocationId() : "UNASSIGNED");
    }
}
