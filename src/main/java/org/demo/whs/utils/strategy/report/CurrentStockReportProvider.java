package org.demo.whs.utils.strategy.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.BackgroundJob;
import org.demo.whs.entity.Batch;
import org.demo.whs.entity.Inventory;
import org.demo.whs.entity.Locations;
import org.demo.whs.entity.Products;
import org.demo.whs.entity.Warehouses;
import org.demo.whs.entity.dto.ReportExportJobPayload;
import org.demo.whs.entity.dto.request.Inventory.InventoryFilterRequest;
import org.demo.whs.entity.dto.response.Inventory.InventoryResponse;
import org.demo.whs.entity.enums.ReportType;
import org.demo.whs.mapper.InventoryMapper;
import org.demo.whs.repository.BatchRepository;
import org.demo.whs.repository.InventoryRepository;
import org.demo.whs.repository.LocationRepository;
import org.demo.whs.repository.ProductRepository;
import org.demo.whs.repository.WareHouseRepository;
import org.demo.whs.repository.specification.InventorySpecification;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class CurrentStockReportProvider implements ReportDataProvider {

    private static final List<String> HEADERS = List.of(
            "sku",
            "productName",
            "warehouseName",
            "locationCode",
            "batchNumber",
            "onHandQuantity",
            "reservedQuantity",
            "availableQuantity"
    );

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;
    private final WareHouseRepository wareHouseRepository;
    private final LocationRepository locationRepository;
    private final BatchRepository batchRepository;
    private final InventoryMapper inventoryMapper;
    private final ObjectMapper objectMapper;

    @Override
    public ReportType getReportType() {
        return ReportType.CURRENT_STOCK;
    }

    @Override
    public ReportDataSet loadDataSet(BackgroundJob job, ReportExportJobPayload payload) {
        InventoryFilterRequest filter = resolveFilter(payload);
        log.info("Loading current stock report data jobId={} filter={}", job == null ? null : job.getId(), filter);

        List<Inventory> inventories = inventoryRepository.findAll(
                InventorySpecification.withFilter(filter),
                Sort.by(
                        Sort.Order.asc("warehouseId"),
                        Sort.Order.asc("productId"),
                        Sort.Order.asc("locationId"),
                        Sort.Order.asc("batchId")
                )
        );

        if (inventories.isEmpty()) {
            return ReportDataSet.builder()
                    .reportName("current_stock_report")
                    .headers(HEADERS)
                    .rows(List.of())
                    .parameters(buildParameters(payload, 0))
                    .build();
        }

        Map<String, Products> productMap = productRepository.findAllById(extractIds(inventories, Inventory::getProductId))
                .stream()
                .collect(Collectors.toMap(Products::getId, product -> product));

        Map<String, Warehouses> warehouseMap = wareHouseRepository.findAllById(extractIds(inventories, Inventory::getWarehouseId))
                .stream()
                .collect(Collectors.toMap(Warehouses::getId, warehouse -> warehouse));

        Map<String, Locations> locationMap = locationRepository.findAllById(extractIds(inventories, Inventory::getLocationId))
                .stream()
                .collect(Collectors.toMap(Locations::getId, location -> location));

        Map<String, Batch> batchMap = batchRepository.findAllById(extractIds(inventories, Inventory::getBatchId))
                .stream()
                .collect(Collectors.toMap(Batch::getId, batch -> batch));

        List<InventoryResponse> inventoryResponses = inventoryMapper.toResponses(
                inventories,
                productMap,
                warehouseMap,
                locationMap,
                batchMap
        );

        List<Map<String, Object>> rows = inventoryResponses.stream()
                .map(this::toRow)
                .toList();

        return ReportDataSet.builder()
                .reportName("current_stock_report")
                .headers(HEADERS)
                .rows(rows)
                .parameters(buildParameters(payload, rows.size()))
                .build();
    }

    private InventoryFilterRequest resolveFilter(ReportExportJobPayload payload) {
        if (payload == null || payload.getFilters() == null || payload.getFilters().isEmpty()) {
            return new InventoryFilterRequest();
        }

        return objectMapper.convertValue(payload.getFilters(), InventoryFilterRequest.class);
    }

    private Map<String, Object> buildParameters(ReportExportJobPayload payload, int totalRows) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("reportTitle", "Current Stock Report");
        parameters.put("totalRows", totalRows);

        if (payload != null && payload.getParameters() != null && !payload.getParameters().isEmpty()) {
            parameters.putAll(payload.getParameters());
        }

        return parameters;
    }

    private Map<String, Object> toRow(InventoryResponse response) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("sku", response.getProductSku());
        row.put("productName", response.getProductName());
        row.put("warehouseName", response.getWarehouseName());
        row.put("locationCode", response.getLocationCode());
        row.put("batchNumber", response.getBatchNumber());
        row.put("onHandQuantity", response.getOnHandQuantity());
        row.put("reservedQuantity", response.getReservedQuantity());
        row.put("availableQuantity", response.getAvailableQuantity());
        return row;
    }

    private <T> Set<String> extractIds(List<Inventory> inventories, java.util.function.Function<Inventory, String> extractor) {
        return inventories.stream()
                .map(extractor)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }
}
