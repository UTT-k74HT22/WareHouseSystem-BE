package org.demo.whs.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.*;
import org.demo.whs.entity.dto.request.Inventory.InventoryFilterRequest;
import org.demo.whs.entity.dto.response.Inventory.InventoryByLocationResponse;
import org.demo.whs.entity.dto.response.Inventory.InventoryResponse;
import org.demo.whs.entity.dto.response.Inventory.InventorySummaryResponse;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.exception.NotFoundException;
import org.demo.whs.mapper.InventoryMapper;
import org.demo.whs.repository.*;
import org.demo.whs.repository.projection.InventorySummaryProjection;
import org.demo.whs.repository.specification.InventorySpecification;
import org.demo.whs.service.InventoryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.demo.whs.exception.ErrorCode.PROD_001;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryMapper inventoryMapper;
    private final ProductRepository productRepository;
    private final WareHouseRepository wareHouseRepository;
    private final LocationRepository locationRepository;
    private final BatchRepository batchRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<InventoryResponse> getInventories(InventoryFilterRequest filter, Pageable pageable) {
        log.info("Fetching inventories with filter: {}, pageable: {}", filter, pageable);
        validatePageable(pageable);

        // 1. Fetch Inventory page (Thin Entity)
        Page<Inventory> inventoryPage = inventoryRepository.findAll(
                InventorySpecification.withFilter(filter),
                pageable
        );

        List<Inventory> content = inventoryPage.getContent();
        if (content.isEmpty()) {
            return PageResponse.from(inventoryPage, List.of());
        }

        // 2. Collect Unique IDs
        Set<String> productIds = content.stream().map(Inventory::getProductId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<String> warehouseIds = content.stream().map(Inventory::getWarehouseId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<String> locationIds = content.stream().map(Inventory::getLocationId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<String> batchIds = content.stream().map(Inventory::getBatchId).filter(Objects::nonNull).collect(Collectors.toSet());

        // 3. Bulk Fetch related entities
        Map<String, Products> productMap = productRepository.findAllById(productIds)
                .stream().collect(Collectors.toMap(Products::getId, p -> p));

        Map<String, Warehouses> warehouseMap = wareHouseRepository.findAllById(warehouseIds)
                .stream().collect(Collectors.toMap(Warehouses::getId, w -> w));

        Map<String, Locations> locationMap = locationRepository.findAllById(locationIds)
                .stream().collect(Collectors.toMap(Locations::getId, l -> l));

        Map<String, Batch> batchMap = batchRepository.findAllById(batchIds)
                .stream().collect(Collectors.toMap(Batch::getId, b -> b));

        // 4. Map to Responses with related data
        List<InventoryResponse> responses = inventoryMapper.toResponses(content, productMap, warehouseMap, locationMap, batchMap);

        return PageResponse.from(inventoryPage, responses);
    }

    @Override
    @Transactional(readOnly = true)
    public InventorySummaryResponse getSummaryByProduct(String productId) {
        log.info("Getting inventory summary for product ID: {}", productId);

        InventorySummaryProjection projection =
                inventoryRepository.getSummaryByProductId(productId)
                        .orElseThrow(() -> new NotFoundException(PROD_001));

        return InventorySummaryResponse.builder()
                .productId(projection.getProductId())
                .productSku(projection.getProductSku())
                .productName(projection.getProductName())
                .totalOnHandQuantity(projection.getTotalOnHandQuantity())
                .totalReservedQuantity(projection.getTotalReservedQuantity())
                .totalAvailableQuantity(projection.getTotalOnHandQuantity().subtract(projection.getTotalReservedQuantity()))
                .warehouseCount(projection.getWarehouseCount())
                .locationCount(projection.getLocationCount())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryByLocationResponse> getInventoryByLocation(InventoryFilterRequest filter) {
        log.info("Getting inventory grouped by location with filter: {}", filter);

        // 1. Fetch all matching inventories
        List<Inventory> allInventories = inventoryRepository.findAll(InventorySpecification.withFilter(filter));
        if (allInventories.isEmpty()) {
            return List.of();
        }

        // 2. Collect Unique IDs for bulk fetching
        Set<String> productIds = allInventories.stream().map(Inventory::getProductId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<String> warehouseIds = allInventories.stream().map(Inventory::getWarehouseId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<String> locationIds = allInventories.stream().map(Inventory::getLocationId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<String> batchIds = allInventories.stream().map(Inventory::getBatchId).filter(Objects::nonNull).collect(Collectors.toSet());

        // 3. Bulk Fetch - Using (p1, p2) -> p1 to handle potential duplicates in non-unique ID queries (though IDs should be unique)
        Map<String, Products> productMap = productRepository.findAllById(productIds)
                .stream().collect(Collectors.toMap(Products::getId, p -> p, (p1, p2) -> p1));
        Map<String, Warehouses> warehouseMap = wareHouseRepository.findAllById(warehouseIds)
                .stream().collect(Collectors.toMap(Warehouses::getId, w -> w, (w1, w2) -> w1));
        Map<String, Locations> locationMap = locationRepository.findAllById(locationIds)
                .stream().collect(Collectors.toMap(Locations::getId, l -> l, (l1, l2) -> l1));
        Map<String, Batch> batchMap = batchRepository.findAllById(batchIds)
                .stream().collect(Collectors.toMap(Batch::getId, b -> b, (b1, b2) -> b1));

        // 4. Group by locationId
        Map<String, List<Inventory>> groupedByLocation = allInventories.stream()
                .collect(Collectors.groupingBy(i -> i.getLocationId() != null ? i.getLocationId() : "unassigned"));

        // 5. Build response
        List<InventoryByLocationResponse> responses = new ArrayList<>();
        groupedByLocation.forEach((locId, inventories) -> {
            Locations loc = locationMap.get(locId);
            
            // It's possible that different items in the same location (or unassigned) belong to different warehouses
            // but in a typical warehouse management system, one location belongs to one warehouse.
            // For "unassigned", they might span multiple warehouses if filter is broad.
            // Grouping by warehouseId as well if needed, but for now we follow the location-first approach.
            
            String warehouseId = inventories.get(0).getWarehouseId();
            Warehouses wh = warehouseMap.get(warehouseId);

            List<InventoryByLocationResponse.LocationInventoryItem> items = inventories.stream().map(inv -> {
                Products prod = productMap.get(inv.getProductId());
                Batch batch = inv.getBatchId() != null ? batchMap.get(inv.getBatchId()) : null;
                
                return InventoryByLocationResponse.LocationInventoryItem.builder()
                        .productId(inv.getProductId())
                        .productSku(prod != null ? prod.getSku() : null)
                        .productName(prod != null ? prod.getName() : null)
                        .batchId(inv.getBatchId())
                        .batchNumber(batch != null ? batch.getBatchNumber() : null)
                        .onHandQuantity(inv.getOnHandQuantity())
                        .reservedQuantity(inv.getReservedQuantity())
                        .availableQuantity(inv.getAvailableQuantity())
                        .build();
            }).collect(Collectors.toList());

            responses.add(InventoryByLocationResponse.builder()
                    .locationId(locId.equals("unassigned") ? null : locId)
                    .locationCode(loc != null ? loc.getCode() : "N/A")
                    .locationName(loc != null ? loc.getName() : "Unassigned")
                    .warehouseId(warehouseId)
                    .warehouseName(wh != null ? wh.getName() : "Unknown")
                    .items(items)
                    .build());
        });

        return responses;
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
}
