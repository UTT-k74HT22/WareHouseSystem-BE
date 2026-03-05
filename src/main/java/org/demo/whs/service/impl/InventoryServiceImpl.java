package org.demo.whs.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.*;
import org.demo.whs.entity.dto.request.Inventory.InventoryFilterRequest;
import org.demo.whs.entity.dto.response.Inventory.InventoryResponse;
import org.demo.whs.repository.projection.InventorySummaryProjection;
import org.demo.whs.entity.dto.response.Inventory.InventorySummaryResponse;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.exception.NotFoundException;
import org.demo.whs.mapper.InventoryMapper;
import org.demo.whs.repository.*;
import org.demo.whs.repository.specification.InventorySpecification;
import org.demo.whs.service.InventoryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        InventorySummaryResponse response = InventorySummaryResponse.builder()
                .productId(projection.getProductId())
                .productSku(projection.getProductSku())
                .productName(projection.getProductName())
                .totalOnHandQuantity(projection.getTotalOnHandQuantity())
                .totalReservedQuantity(projection.getTotalReservedQuantity())
                .warehouseCount(projection.getWarehouseCount())
                .locationCount(projection.getLocationCount())
                .build();
        return response;
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
