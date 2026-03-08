package org.demo.whs.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.*;
import org.demo.whs.entity.dto.request.Inventory.*;
import org.demo.whs.entity.dto.response.Inventory.*;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.enums.InventoryReservationStatus;
import org.demo.whs.exception.*;
import org.demo.whs.mapper.InventoryMapper;
import org.demo.whs.repository.*;
import org.demo.whs.repository.specification.InventorySpecification;
import org.demo.whs.service.InventoryService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.demo.whs.exception.ErrorCode.PROD_001;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryServiceImpl implements InventoryService {

    /* ===================== DEPENDENCIES ===================== */

    private final InventoryRepository inventoryRepository;
    private final InventoryReservationRepository inventoryReservationRepository;
    private final InventoryMapper inventoryMapper;

    private final ProductRepository productRepository;
    private final WareHouseRepository wareHouseRepository;
    private final LocationRepository locationRepository;
    private final BatchRepository batchRepository;


    /**
     * Lấy danh sách inventory theo filter và phân trang.
     */
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

        Set<String> productIds = content.stream().map(Inventory::getProductId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<String> warehouseIds = content.stream().map(Inventory::getWarehouseId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<String> locationIds = content.stream().map(Inventory::getLocationId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<String> batchIds = content.stream().map(Inventory::getBatchId).filter(Objects::nonNull).collect(Collectors.toSet());

        Map<String, Products> productMap = productRepository.findAllById(productIds)
                .stream().collect(Collectors.toMap(Products::getId, p -> p));

        Map<String, Warehouses> warehouseMap = wareHouseRepository.findAllById(warehouseIds)
                .stream().collect(Collectors.toMap(Warehouses::getId, w -> w));

        Map<String, Locations> locationMap = locationRepository.findAllById(locationIds)
                .stream().collect(Collectors.toMap(Locations::getId, l -> l));

        Map<String, Batch> batchMap = batchRepository.findAllById(batchIds)
                .stream().collect(Collectors.toMap(Batch::getId, b -> b));

        List<InventoryResponse> responses = inventoryMapper.toResponses(
                content, productMap, warehouseMap, locationMap, batchMap
        );

        return PageResponse.from(inventoryPage, responses);
    }


    /**
     * Lấy tổng tồn kho theo product.
     */
    @Override
    @Transactional(readOnly = true)
    public InventorySummaryResponse getSummaryByProduct(String productId) {
        log.info("Getting inventory summary for product ID: {}", productId);

        return inventoryRepository.getSummaryByProductId(productId)
                .orElseThrow(() -> new NotFoundException(PROD_001));
    }
    /**
     * Lấy tồn kho theo từng location.
     */
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

            BigDecimal onHand = Optional.ofNullable(p.getOnHandQuantity()).orElse(BigDecimal.ZERO);
            BigDecimal reserved = Optional.ofNullable(p.getReservedQuantity()).orElse(BigDecimal.ZERO);

            locationResponse.getItems().add(
                    LocationInventoryItemResponse.builder()
                            .productId(p.getProductId())
                            .productSku(p.getProductSku())
                            .productName(p.getProductName())
                            .batchId(p.getBatchId())
                            .batchNumber(p.getBatchNumber())
                            .onHandQuantity(onHand)
                            .reservedQuantity(reserved)
                            .availableQuantity(onHand.subtract(reserved))
                            .build()
            );
        }

        return new ArrayList<>(responseMap.values());
    }
    /**
     * Kiểm tra tồn kho khả dụng.
     */
    @Override
    @Transactional(readOnly = true)
    public CheckAvailabilityResponse checkAvailability(CheckAvailabilityRequest request) {

        log.info("Checking inventory availability for request: {}", request);

        productRepository.findById(request.getProductId())
                .orElseThrow(() -> new NotFoundException(PROD_001));

        Warehouses warehouse = null;
        if (request.getWarehouseId() != null) {
            warehouse = wareHouseRepository.findById(request.getWarehouseId())
                    .orElseThrow(() -> new NotFoundException(ErrorCode.WHS_001));
        }

        Locations location = null;
        if (request.getLocationId() != null) {
            location = locationRepository.findById(request.getLocationId())
                    .orElseThrow(() -> new NotFoundException(ErrorCode.LOC_001));
        }

        if (warehouse != null && location != null &&
                !warehouse.getId().equals(location.getWarehouseId())) {
            throw new BadRequestException(ErrorCode.COM_001);
        }

        CheckAvailabilityResponse availability = inventoryRepository.getAvailability(
                request.getProductId(),
                request.getWarehouseId(),
                request.getLocationId()
        );

        BigDecimal availableQuantity = availability.getAvailableQuantity();
        boolean isAvailable = availableQuantity.compareTo(request.getQuantity()) >= 0;

        return inventoryMapper.toCheckAvailabilityResponse(
                request, availableQuantity, isAvailable
        );
    }
    /**
     * Reserve inventory cho một order line.
     * Hỗ trợ idempotency và chống race condition.
     */
    @Override
    @Transactional
    public InventoryReserveResponse reserve(InventoryReserveRequest request) {

        log.info("Reserving inventory for request: {}", request);

        if (request.getQuantity() == null || request.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException(ErrorCode.COM_001);
        }

        Optional<InventoryReservation> existing = findExistingReservation(request);
        if (existing.isPresent()) {
            return handleExistingReservation(existing.get());
        }

        Inventory inventory = inventoryRepository.findBestSuitableForUpdate(
                request.getProductId(),
                request.getWarehouseId(),
                request.getLocationId(),
                request.getBatchId(),
                request.getQuantity()
        ).orElseThrow(() -> new ConflictException(ErrorCode.INV_004));

        BigDecimal reserved = Optional.ofNullable(inventory.getReservedQuantity())
                .orElse(BigDecimal.ZERO);

        inventory.setReservedQuantity(reserved.add(request.getQuantity()));
        inventory.setLastMovementAt(LocalDateTime.now());
        inventoryRepository.save(inventory);

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

            log.warn("Duplicate reservation detected for orderLineId: {}", request.getOrderLineId());

            return findExistingReservation(request)
                    .map(this::handleExistingReservation)
                    .orElseThrow(() -> ex);
        }

        return inventoryMapper.toReserveResponse(reservation, inventory);
    }
    /* ===================== PRIVATE METHODS ===================== */

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

        Inventory inv = inventoryRepository.findById(reservation.getInventoryId())
                .orElseThrow(() -> new ConflictException(ErrorCode.INV_001));

        return inventoryMapper.toReserveResponse(reservation, inv);
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

    private String buildLocationGroupKey(InventoryLocationProjection projection) {
        return projection.getWarehouseId() + ":" +
                (projection.getLocationId() != null ? projection.getLocationId() : "UNASSIGNED");
    }
}