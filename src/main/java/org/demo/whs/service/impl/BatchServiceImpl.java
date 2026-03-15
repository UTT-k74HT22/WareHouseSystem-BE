package org.demo.whs.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.Account;
import org.demo.whs.entity.BaseEntity;
import org.demo.whs.entity.Batch;
import org.demo.whs.entity.BusinessPartners;
import org.demo.whs.entity.InboundReceiptLines;
import org.demo.whs.entity.InboundReceipts;
import org.demo.whs.entity.Inventory;
import org.demo.whs.entity.Locations;
import org.demo.whs.entity.OutboundShipmentLines;
import org.demo.whs.entity.OutboundShipments;
import org.demo.whs.entity.Products;
import org.demo.whs.entity.PurchaseOrders;
import org.demo.whs.entity.SalesOrders;
import org.demo.whs.entity.StockMovements;
import org.demo.whs.entity.Warehouses;
import org.demo.whs.entity.dto.request.Batch.ChangeBatchStatusRequest;
import org.demo.whs.entity.dto.request.Batch.CreateBatchRequest;
import org.demo.whs.entity.dto.request.Batch.QuarantineBatchRequest;
import org.demo.whs.entity.dto.request.Batch.ReleaseBatchRequest;
import org.demo.whs.entity.dto.request.Batch.SearchBatchRequest;
import org.demo.whs.entity.dto.request.Batch.UpdateBatchRequest;
import org.demo.whs.entity.dto.response.Batch.BatchByProductResponse;
import org.demo.whs.entity.dto.response.Batch.BatchExpiringResponse;
import org.demo.whs.entity.dto.response.Batch.BatchFifoRecommendationResponse;
import org.demo.whs.entity.dto.response.Batch.BatchInventorySnapshotResponse;
import org.demo.whs.entity.dto.response.Batch.BatchResponse;
import org.demo.whs.entity.dto.response.Batch.BatchTraceabilityResponse;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.enums.BatchStatus;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.exception.NotFoundException;
import org.demo.whs.mapper.BatchMapper;
import org.demo.whs.repository.AccountRepository;
import org.demo.whs.repository.BatchRepository;
import org.demo.whs.repository.BusinessPartnersRepository;
import org.demo.whs.repository.InboundReceiptLinesRepository;
import org.demo.whs.repository.InboundReceiptsRepository;
import org.demo.whs.repository.InventoryRepository;
import org.demo.whs.repository.LocationRepository;
import org.demo.whs.repository.OutboundShipmentLinesRepository;
import org.demo.whs.repository.OutboundShipmentsRepository;
import org.demo.whs.repository.ProductRepository;
import org.demo.whs.repository.PurchaseOrdersRepository;
import org.demo.whs.repository.SalesOrdersRepository;
import org.demo.whs.repository.StockMovementsRepository;
import org.demo.whs.repository.WareHouseRepository;
import org.demo.whs.repository.specification.BatchSpecification;
import org.demo.whs.security.SecurityUtils;
import org.demo.whs.service.BatchService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class BatchServiceImpl implements BatchService {

    private static final List<BatchStatus> EXPIRING_BATCH_STATUSES = List.of(
            BatchStatus.AVAILABLE,
            BatchStatus.QUARANTINE
    );
    private static final String NO_LOCATION_KEY = "__NO_LOCATION__";

    private final AccountRepository accountRepository;
    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;
    private final BatchRepository batchRepository;
    private final BatchMapper batchMapper;
    private final InboundReceiptLinesRepository inboundReceiptLinesRepository;
    private final InboundReceiptsRepository inboundReceiptsRepository;
    private final OutboundShipmentLinesRepository outboundShipmentLinesRepository;
    private final OutboundShipmentsRepository outboundShipmentsRepository;
    private final StockMovementsRepository stockMovementsRepository;
    private final PurchaseOrdersRepository purchaseOrdersRepository;
    private final SalesOrdersRepository salesOrdersRepository;
    private final BusinessPartnersRepository businessPartnersRepository;
    private final LocationRepository locationRepository;
    private final WareHouseRepository wareHouseRepository;

    @Override
    @Transactional
    public BatchResponse createBatch(CreateBatchRequest request) {
        Products product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.PROD_001));
        if (!Boolean.TRUE.equals(product.getRequiresBatchTracking())) {
            throw new BadRequestException(ErrorCode.BATCH_009);
        }
        if (batchRepository.existsByProductIdAndBatchNumber(request.getProductId(), request.getBatchNumber())) {
            log.warn("Batch already exists: productId={}, batchNumber={}", request.getProductId(), request.getBatchNumber());
            throw new BadRequestException(ErrorCode.BATCH_002);
        }
        if (request.getManufacturingDate() != null && request.getManufacturingDate().isAfter(LocalDate.now())) {
            throw new BadRequestException(ErrorCode.BATCH_005);
        }
        if (request.getManufacturingDate() != null
                && request.getExpiryDate() != null
                && request.getExpiryDate().isBefore(request.getManufacturingDate())) {
            throw new BadRequestException(ErrorCode.BATCH_006);
        }
        Account currentUser = getCurrentUser();
        Batch batch = batchMapper.createEntity(request);
        batch.setStatus(BatchStatus.AVAILABLE);
        setAuditFieldsForCreate(batch, currentUser);
        Batch savedBatch = batchRepository.save(batch);
        log.info("Batch created successfully with ID={} by user={}", savedBatch.getId(), currentUser.getUsername());
        return enrichBatchResponse(savedBatch, List.of());
    }

    @Override
    @Transactional
    public BatchResponse getBatchById(String id) {
        log.info("Fetching batch by ID: {}", id);
        Batch batch = batchRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorCode.BATCH_001));
        List<Inventory> inventories = inventoryRepository.findByBatchIdOrderByLastMovementAtDesc(id);
        return enrichBatchResponse(batch, inventories);
    }

    @Override
    @Transactional
    public PageResponse<BatchResponse> getAllBatches(SearchBatchRequest request, Integer page, Integer size) {
        log.info("Fetching all batches - request={}, page={}, size={}", request, page, size);
        validateSearchRequest(request);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Batch> batchPage = batchRepository.findAll(BatchSpecification.withFilter(request), pageable);
        Set<String> batchIds = collectIds(batchPage.getContent(), Batch::getId);
        Map<String, List<Inventory>> inventoryByBatch = batchIds.isEmpty()
                ? Collections.emptyMap()
                : groupInventoriesByBatch(inventoryRepository.findByBatchIdIn(batchIds));
        List<BatchResponse> responses = batchPage.getContent().stream()
                .map(batch -> enrichBatchResponse(batch, inventoryByBatch.getOrDefault(batch.getId(), List.of())))
                .collect(Collectors.toList());
        return PageResponse.from(batchPage, responses);
    }
    @Override
    @Transactional
    public BatchResponse changeBatchStatus(String id, ChangeBatchStatusRequest request) {
        log.warn("Blocked generic batch status change for batchId={} targetStatus={}", id, request.getStatus());
        throw new BadRequestException(ErrorCode.BATCH_011);
    }

    @Override
    @Transactional
    public BatchResponse updateBatch(String id, UpdateBatchRequest request) {
        Batch batch = batchRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorCode.BATCH_001));
        if (request.getBatchNumber() != null
                && batchRepository.existsByProductIdAndBatchNumberAndIdNot(batch.getProductId(), request.getBatchNumber(), id)) {
            throw new BadRequestException(ErrorCode.BATCH_002);
        }
        LocalDate manufacturingDate = request.getManufacturingDate() != null
                ? request.getManufacturingDate()
                : batch.getManufacturingDate();
        LocalDate expiryDate = request.getExpiryDate() != null
                ? request.getExpiryDate()
                : batch.getExpiryDate();
        if (manufacturingDate != null && manufacturingDate.isAfter(LocalDate.now())) {
            throw new BadRequestException(ErrorCode.BATCH_005);
        }
        if (expiryDate != null && manufacturingDate != null && expiryDate.isBefore(manufacturingDate)) {
            throw new BadRequestException(ErrorCode.BATCH_006);
        }
        batchMapper.updateEntity(request, batch);
        Account currentUser = getCurrentUser();
        setAuditFieldsForUpdate(batch, currentUser);
        Batch updatedBatch = batchRepository.save(batch);
        log.info("Batch updated successfully with ID={} by user={}", updatedBatch.getId(), currentUser.getUsername());
        List<Inventory> inventories = inventoryRepository.findByBatchIdOrderByLastMovementAtDesc(updatedBatch.getId());
        return enrichBatchResponse(updatedBatch, inventories);
    }

    @Override
    @Transactional
    public BatchResponse quarantineBatch(String id, QuarantineBatchRequest request) {
        Batch batch = batchRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorCode.BATCH_001));
        validateQuarantine(batch);
        Account currentUser = getCurrentUser();
        batch.setStatus(BatchStatus.QUARANTINE);
        batch.setNotes(appendWorkflowNote(batch.getNotes(), buildQuarantineAuditNote(request)));
        setAuditFieldsForUpdate(batch, currentUser);
        Batch savedBatch = batchRepository.save(batch);
        log.info("Batch {} moved to QUARANTINE by user {}", batch.getBatchNumber(), currentUser.getUsername());
        List<Inventory> inventories = inventoryRepository.findByBatchIdOrderByLastMovementAtDesc(savedBatch.getId());
        return enrichBatchResponse(savedBatch, inventories);
    }

    @Override
    @Transactional
    public BatchResponse releaseBatch(String id, ReleaseBatchRequest request) {
        Batch batch = batchRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorCode.BATCH_001));
        validateRelease(batch);
        Account user = getCurrentUser();
        batch.setStatus(BatchStatus.AVAILABLE);
        batch.setNotes(appendWorkflowNote(batch.getNotes(), buildReleaseAuditNote(request)));
        setAuditFieldsForUpdate(batch, user);
        Batch savedBatch = batchRepository.save(batch);
        log.info("Batch {} released from QUARANTINE by user {}", batch.getBatchNumber(), user.getUsername());
        List<Inventory> inventories = inventoryRepository.findByBatchIdOrderByLastMovementAtDesc(savedBatch.getId());
        return enrichBatchResponse(savedBatch, inventories);
    }

    @Override
    @Transactional
    public BatchTraceabilityResponse getBatchTraceability(String id) {
        Batch batch = getBatchOrThrow(id);
        Products product = getProductOrThrow(batch.getProductId());
        List<Inventory> inventories = inventoryRepository.findByBatchIdOrderByLastMovementAtDesc(id);
        List<InboundReceiptLines> inboundLines = inboundReceiptLinesRepository.findByBatchIdOrderByCreatedAtDesc(id);
        List<OutboundShipmentLines> outboundLines = outboundShipmentLinesRepository.findByBatchIdOrderByCreatedAtDesc(id);
        List<StockMovements> stockMovements = stockMovementsRepository.findByBatchIdOrderByMovementDateDesc(id);

        Map<String, InboundReceipts> inboundReceiptsById = toEntityMap(
                inboundReceiptsRepository.findAllById(collectIds(inboundLines, InboundReceiptLines::getInboundReceiptId)));
        Map<String, PurchaseOrders> purchaseOrdersById = toEntityMap(
                purchaseOrdersRepository.findAllById(collectIds(inboundReceiptsById.values(), InboundReceipts::getPurchaseOrderId)));
        Map<String, OutboundShipments> outboundShipmentsById = toEntityMap(
                outboundShipmentsRepository.findAllById(collectIds(outboundLines, OutboundShipmentLines::getOutboundShipmentId)));
        Map<String, SalesOrders> salesOrdersById = toEntityMap(
                salesOrdersRepository.findAllById(collectIds(outboundShipmentsById.values(), OutboundShipments::getSalesOrderId)));

        Set<String> businessPartnerIds = new LinkedHashSet<>();
        businessPartnerIds.addAll(collectIds(purchaseOrdersById.values(), PurchaseOrders::getSupplierId));
        businessPartnerIds.addAll(collectIds(salesOrdersById.values(), SalesOrders::getCustomerId));
        Map<String, BusinessPartners> businessPartnerMap = toEntityMap(businessPartnersRepository.findAllById(businessPartnerIds));

        Set<String> warehouseIds = new LinkedHashSet<>();
        warehouseIds.addAll(collectIds(inventories, Inventory::getWarehouseId));
        warehouseIds.addAll(collectIds(inboundReceiptsById.values(), InboundReceipts::getWarehouseId));
        warehouseIds.addAll(collectIds(outboundShipmentsById.values(), OutboundShipments::getWarehouseId));
        warehouseIds.addAll(collectIds(stockMovements, StockMovements::getWarehouseId));
        Map<String, Warehouses> warehouseMap = toEntityMap(wareHouseRepository.findAllById(warehouseIds));

        Set<String> locationIds = new LinkedHashSet<>();
        locationIds.addAll(collectIds(inventories, Inventory::getLocationId));
        locationIds.addAll(collectIds(inboundLines, InboundReceiptLines::getLocationId));
        locationIds.addAll(collectIds(outboundLines, OutboundShipmentLines::getLocationId));
        locationIds.addAll(collectIds(stockMovements, StockMovements::getLocationId));
        Map<String, Locations> locationMap = toEntityMap(locationRepository.findAllById(locationIds));

        return BatchTraceabilityResponse.builder()
                .batchId(batch.getId())
                .batchNumber(batch.getBatchNumber())
                .productId(product.getId())
                .productSku(product.getSku())
                .productName(product.getName())
                .status(batch.getStatus())
                .manufacturingDate(batch.getManufacturingDate())
                .expiryDate(batch.getExpiryDate())
                .supplierBatchNumber(batch.getSupplierBatchNumber())
                .notes(batch.getNotes())
                .inventorySnapshot(buildInventorySnapshot(inventories, warehouseMap, locationMap))
                .inboundReceipts(buildInboundTraceabilityEvents(
                        inboundLines, inboundReceiptsById, purchaseOrdersById, businessPartnerMap, warehouseMap, locationMap))
                .outboundShipments(buildOutboundTraceabilityEvents(
                        outboundLines, outboundShipmentsById, salesOrdersById, businessPartnerMap, warehouseMap, locationMap))
                .stockMovements(buildStockMovementEvents(stockMovements, warehouseMap, locationMap))
                .workflowNotes(extractWorkflowNotes(batch.getNotes()))
                .build();
    }

    @Override
    @Transactional
    public List<BatchExpiringResponse> getExpiringBatches(Integer thresholdDays, String warehouseId) {
        int effectiveThreshold = thresholdDays == null ? 30 : thresholdDays;
        String normalizedWarehouseId = normalizeOptional(warehouseId);
        if (normalizedWarehouseId != null) {
            validateWarehouseExists(normalizedWarehouseId);
        }
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusDays(effectiveThreshold);
        List<Batch> batches = batchRepository.findExpiringBatches(today, endDate, EXPIRING_BATCH_STATUSES);
        if (batches.isEmpty()) {
            return List.of();
        }
        Map<String, Products> productMap = toEntityMap(productRepository.findAllById(collectIds(batches, Batch::getProductId)));
        List<Inventory> inventories = normalizedWarehouseId == null
                ? inventoryRepository.findByBatchIdIn(collectIds(batches, Batch::getId))
                : inventoryRepository.findByWarehouseIdAndBatchIdIn(normalizedWarehouseId, collectIds(batches, Batch::getId));
        Map<String, List<Inventory>> inventoryByBatch = groupInventoriesByBatch(inventories);
        Map<String, Warehouses> warehouseMap = toEntityMap(wareHouseRepository.findAllById(collectIds(inventories, Inventory::getWarehouseId)));
        Map<String, Locations> locationMap = toEntityMap(locationRepository.findAllById(collectIds(inventories, Inventory::getLocationId)));
        return batches.stream()
                .map(batch -> buildExpiringResponse(batch, productMap.get(batch.getProductId()), inventoryByBatch, warehouseMap, locationMap, today))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<BatchFifoRecommendationResponse> getFifoRecommendations(String productId, String warehouseId, Integer limit) {
        Products product = getBatchTrackedProductOrThrow(productId);
        Warehouses warehouse = validateWarehouseExists(warehouseId);
        int effectiveLimit = limit == null ? 5 : limit;
        LocalDate today = LocalDate.now();
        List<Batch> batches = batchRepository.findByProductIdOrderByManufacturingDateAscExpiryDateAscCreatedAtAsc(productId);
        if (batches.isEmpty()) {
            return List.of();
        }
        List<Inventory> inventories = inventoryRepository.findByProductIdAndWarehouseIdAndBatchIdIn(
                productId, warehouseId, collectIds(batches, Batch::getId));
        Map<String, List<Inventory>> inventoryByBatch = groupInventoriesByBatch(inventories);
        Map<String, Warehouses> warehouseMap = Map.of(warehouse.getId(), warehouse);
        Map<String, Locations> locationMap = toEntityMap(locationRepository.findAllById(collectIds(inventories, Inventory::getLocationId)));
        List<BatchFifoRecommendationResponse> responses = new ArrayList<>();
        int rank = 1;
        for (Batch batch : batches) {
            if (batch.getStatus() != BatchStatus.AVAILABLE) {
                continue;
            }
            if (batch.getExpiryDate() != null && !batch.getExpiryDate().isAfter(today)) {
                continue;
            }
            List<Inventory> batchInventories = inventoryByBatch.getOrDefault(batch.getId(), List.of());
            if (!hasPositiveAvailableQuantity(batchInventories)) {
                continue;
            }
            responses.add(BatchFifoRecommendationResponse.builder()
                    .recommendationRank(rank++)
                    .batchId(batch.getId())
                    .batchNumber(batch.getBatchNumber())
                    .productId(product.getId())
                    .productSku(product.getSku())
                    .productName(product.getName())
                    .warehouseId(warehouse.getId())
                    .warehouseCode(warehouse.getCode())
                    .warehouseName(warehouse.getName())
                    .status(batch.getStatus())
                    .manufacturingDate(batch.getManufacturingDate())
                    .expiryDate(batch.getExpiryDate())
                    .daysToExpiry(daysToExpiry(batch.getExpiryDate(), today))
                    .inventorySnapshot(buildInventorySnapshot(batchInventories, warehouseMap, locationMap))
                    .build());
            if (responses.size() >= effectiveLimit) {
                break;
            }
        }
        return responses;
    }

    @Override
    @Transactional
    public List<BatchByProductResponse> getBatchesByProduct(String productId, String warehouseId) {
        Products product = getBatchTrackedProductOrThrow(productId);
        String normalizedWarehouseId = normalizeOptional(warehouseId);
        if (normalizedWarehouseId != null) {
            validateWarehouseExists(normalizedWarehouseId);
        }
        List<Batch> batches = batchRepository.findByProductIdOrderByManufacturingDateAscExpiryDateAscCreatedAtAsc(productId);
        if (batches.isEmpty()) {
            return List.of();
        }
        List<Inventory> inventories = normalizedWarehouseId == null
                ? inventoryRepository.findByProductIdAndBatchIdIn(productId, collectIds(batches, Batch::getId))
                : inventoryRepository.findByProductIdAndWarehouseIdAndBatchIdIn(
                        productId, normalizedWarehouseId, collectIds(batches, Batch::getId));
        Map<String, List<Inventory>> inventoryByBatch = groupInventoriesByBatch(inventories);
        Map<String, Warehouses> warehouseMap = toEntityMap(wareHouseRepository.findAllById(collectIds(inventories, Inventory::getWarehouseId)));
        Map<String, Locations> locationMap = toEntityMap(locationRepository.findAllById(collectIds(inventories, Inventory::getLocationId)));
        return batches.stream()
                .filter(batch -> normalizedWarehouseId == null || !inventoryByBatch.getOrDefault(batch.getId(), List.of()).isEmpty())
                .map(batch -> BatchByProductResponse.builder()
                        .batchId(batch.getId())
                        .batchNumber(batch.getBatchNumber())
                        .productId(product.getId())
                        .productSku(product.getSku())
                        .productName(product.getName())
                        .status(batch.getStatus())
                        .manufacturingDate(batch.getManufacturingDate())
                        .expiryDate(batch.getExpiryDate())
                        .supplierBatchNumber(batch.getSupplierBatchNumber())
                        .notes(batch.getNotes())
                        .inventorySnapshot(buildInventorySnapshot(inventoryByBatch.getOrDefault(batch.getId(), List.of()), warehouseMap, locationMap))
                        .build())
                .collect(Collectors.toList());
    }

    private void validateQuarantine(Batch batch) {
        if (batch.getStatus() == BatchStatus.QUARANTINE) {
            throw new BadRequestException(ErrorCode.BATCH_013);
        }
        if (batch.getStatus() == BatchStatus.EXPIRED) {
            throw new BadRequestException(ErrorCode.BATCH_004);
        }
        if (batch.getStatus() == BatchStatus.RECALLED) {
            throw new BadRequestException(ErrorCode.BATCH_014);
        }
        if (batch.getStatus() != BatchStatus.AVAILABLE) {
            throw new BadRequestException(ErrorCode.BATCH_015);
        }
        if (inventoryRepository.existsReservedStockByBatchId(batch.getId())) {
            throw new BadRequestException(ErrorCode.BATCH_007);
        }
    }

    private void validateRelease(Batch batch) {
        if (batch.getStatus() == BatchStatus.RECALLED) {
            throw new BadRequestException(ErrorCode.BATCH_018);
        }
        if (batch.getStatus() != BatchStatus.QUARANTINE) {
            throw new BadRequestException(ErrorCode.BATCH_016);
        }
        if (batch.getExpiryDate() != null && !batch.getExpiryDate().isAfter(LocalDate.now())) {
            throw new BadRequestException(ErrorCode.BATCH_017);
        }
    }

    private BatchTraceabilityResponse.InboundTraceabilityEvent buildInboundTraceabilityEvent(
            InboundReceiptLines line,
            Map<String, InboundReceipts> inboundReceiptsById,
            Map<String, PurchaseOrders> purchaseOrdersById,
            Map<String, BusinessPartners> businessPartnerMap,
            Map<String, Warehouses> warehouseMap,
            Map<String, Locations> locationMap) {
        InboundReceipts receipt = inboundReceiptsById.get(line.getInboundReceiptId());
        PurchaseOrders purchaseOrder = receipt == null ? null : purchaseOrdersById.get(receipt.getPurchaseOrderId());
        BusinessPartners supplier = purchaseOrder == null ? null : businessPartnerMap.get(purchaseOrder.getSupplierId());
        Warehouses warehouse = receipt == null ? null : warehouseMap.get(receipt.getWarehouseId());
        Locations location = locationMap.get(line.getLocationId());
        return BatchTraceabilityResponse.InboundTraceabilityEvent.builder()
                .inboundReceiptId(line.getInboundReceiptId())
                .receiptNumber(receipt == null ? null : receipt.getReceiptNumber())
                .receiptDate(receipt == null ? null : receipt.getReceiptDate())
                .receiptStatus(receipt == null ? null : receipt.getStatus())
                .purchaseOrderId(receipt == null ? null : receipt.getPurchaseOrderId())
                .purchaseOrderNumber(purchaseOrder == null ? null : purchaseOrder.getPurchaseOrderNumber())
                .supplierId(purchaseOrder == null ? null : purchaseOrder.getSupplierId())
                .supplierCode(supplier == null ? null : supplier.getCode())
                .supplierName(supplier == null ? null : supplier.getName())
                .warehouseId(receipt == null ? null : receipt.getWarehouseId())
                .warehouseCode(warehouse == null ? null : warehouse.getCode())
                .warehouseName(warehouse == null ? null : warehouse.getName())
                .locationId(line.getLocationId())
                .locationCode(location == null ? null : location.getCode())
                .locationName(location == null ? null : location.getName())
                .quantityReceived(line.getQuantityReceived())
                .qualityStatus(line.getQualityStatus())
                .notes(line.getNotes())
                .createdAt(line.getCreatedAt())
                .confirmedAt(receipt == null ? null : receipt.getConfirmedAt())
                .build();
    }

    private List<BatchTraceabilityResponse.InboundTraceabilityEvent> buildInboundTraceabilityEvents(
            List<InboundReceiptLines> inboundLines,
            Map<String, InboundReceipts> inboundReceiptsById,
            Map<String, PurchaseOrders> purchaseOrdersById,
            Map<String, BusinessPartners> businessPartnerMap,
            Map<String, Warehouses> warehouseMap,
            Map<String, Locations> locationMap) {
        return inboundLines.stream()
                .map(line -> buildInboundTraceabilityEvent(line, inboundReceiptsById, purchaseOrdersById, businessPartnerMap, warehouseMap, locationMap))
                .collect(Collectors.toList());
    }

    private BatchTraceabilityResponse.OutboundTraceabilityEvent buildOutboundTraceabilityEvent(
            OutboundShipmentLines line,
            Map<String, OutboundShipments> outboundShipmentsById,
            Map<String, SalesOrders> salesOrdersById,
            Map<String, BusinessPartners> businessPartnerMap,
            Map<String, Warehouses> warehouseMap,
            Map<String, Locations> locationMap) {
        OutboundShipments shipment = outboundShipmentsById.get(line.getOutboundShipmentId());
        SalesOrders salesOrder = shipment == null ? null : salesOrdersById.get(shipment.getSalesOrderId());
        BusinessPartners customer = salesOrder == null ? null : businessPartnerMap.get(salesOrder.getCustomerId());
        Warehouses warehouse = shipment == null ? null : warehouseMap.get(shipment.getWarehouseId());
        Locations location = locationMap.get(line.getLocationId());
        return BatchTraceabilityResponse.OutboundTraceabilityEvent.builder()
                .outboundShipmentId(line.getOutboundShipmentId())
                .shipmentNumber(shipment == null ? null : shipment.getShipmentNumber())
                .shipmentDate(shipment == null ? null : shipment.getShipmentDate())
                .shipmentStatus(shipment == null ? null : shipment.getStatus())
                .salesOrderId(shipment == null ? null : shipment.getSalesOrderId())
                .salesOrderNumber(salesOrder == null ? null : salesOrder.getSoNumber())
                .customerId(salesOrder == null ? null : salesOrder.getCustomerId())
                .customerCode(customer == null ? null : customer.getCode())
                .customerName(customer == null ? null : customer.getName())
                .warehouseId(shipment == null ? null : shipment.getWarehouseId())
                .warehouseCode(warehouse == null ? null : warehouse.getCode())
                .warehouseName(warehouse == null ? null : warehouse.getName())
                .locationId(line.getLocationId())
                .locationCode(location == null ? null : location.getCode())
                .locationName(location == null ? null : location.getName())
                .quantityShipped(line.getQuantityShipped())
                .pickedAt(line.getPickedAt())
                .notes(line.getNotes())
                .createdAt(line.getCreatedAt())
                .shippedAt(shipment == null ? null : shipment.getShippedAt())
                .build();
    }

    private List<BatchTraceabilityResponse.OutboundTraceabilityEvent> buildOutboundTraceabilityEvents(
            List<OutboundShipmentLines> outboundLines,
            Map<String, OutboundShipments> outboundShipmentsById,
            Map<String, SalesOrders> salesOrdersById,
            Map<String, BusinessPartners> businessPartnerMap,
            Map<String, Warehouses> warehouseMap,
            Map<String, Locations> locationMap) {
        return outboundLines.stream()
                .map(line -> buildOutboundTraceabilityEvent(line, outboundShipmentsById, salesOrdersById, businessPartnerMap, warehouseMap, locationMap))
                .collect(Collectors.toList());
    }

    private List<BatchTraceabilityResponse.StockMovementTraceabilityEvent> buildStockMovementEvents(
            List<StockMovements> stockMovements,
            Map<String, Warehouses> warehouseMap,
            Map<String, Locations> locationMap) {
        return stockMovements.stream()
                .map(movement -> {
                    Warehouses warehouse = warehouseMap.get(movement.getWarehouseId());
                    Locations location = locationMap.get(movement.getLocationId());
                    return BatchTraceabilityResponse.StockMovementTraceabilityEvent.builder()
                            .stockMovementId(movement.getId())
                            .movementType(movement.getMovementType())
                            .movementDate(movement.getMovementDate())
                            .warehouseId(movement.getWarehouseId())
                            .warehouseCode(warehouse == null ? null : warehouse.getCode())
                            .warehouseName(warehouse == null ? null : warehouse.getName())
                            .locationId(movement.getLocationId())
                            .locationCode(location == null ? null : location.getCode())
                            .locationName(location == null ? null : location.getName())
                            .quantityChange(movement.getQuantityChange())
                            .quantityBefore(movement.getQuantityBefore())
                            .quantityAfter(movement.getQuantityAfter())
                            .referenceType(movement.getReferenceType())
                            .referenceId(movement.getReferenceId())
                            .referenceNumber(movement.getReferenceNumber())
                            .notes(movement.getNotes())
                            .build();
                })
                .collect(Collectors.toList());
    }

    private BatchExpiringResponse buildExpiringResponse(
            Batch batch,
            Products product,
            Map<String, List<Inventory>> inventoryByBatch,
            Map<String, Warehouses> warehouseMap,
            Map<String, Locations> locationMap,
            LocalDate today) {
        List<Inventory> inventories = inventoryByBatch.getOrDefault(batch.getId(), List.of());
        if (!hasPhysicalStock(inventories)) {
            return null;
        }
        return BatchExpiringResponse.builder()
                .batchId(batch.getId())
                .batchNumber(batch.getBatchNumber())
                .productId(batch.getProductId())
                .productSku(product == null ? null : product.getSku())
                .productName(product == null ? null : product.getName())
                .status(batch.getStatus())
                .manufacturingDate(batch.getManufacturingDate())
                .expiryDate(batch.getExpiryDate())
                .daysToExpiry(daysToExpiry(batch.getExpiryDate(), today))
                .urgency(resolveExpiryUrgency(batch.getExpiryDate(), today))
                .inventorySnapshot(buildInventorySnapshot(inventories, warehouseMap, locationMap))
                .build();
    }

    private BatchResponse enrichBatchResponse(Batch batch, List<Inventory> inventories) {
        BatchResponse baseResponse = batchMapper.toResponse(batch);
        List<Inventory> safeInventories = inventories == null ? List.of() : inventories;
        return baseResponse.toBuilder()
                .totalOnHandQuantity(sumInventories(safeInventories, Inventory::getOnHandQuantity))
                .totalQuarantineQuantity(sumInventories(safeInventories, Inventory::getQuarantineQuantity))
                .totalReservedQuantity(sumInventories(safeInventories, Inventory::getReservedQuantity))
                .totalAvailableQuantity(sumInventories(safeInventories, Inventory::getAvailableQuantity))
                .build();
    }

    private void validateSearchRequest(SearchBatchRequest request) {
        if (request == null) {
            return;
        }
        validateDateRange(request.getManufacturingDateFrom(), request.getManufacturingDateTo());
        validateDateRange(request.getExpiryDateFrom(), request.getExpiryDateTo());
    }

    private void validateDateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BadRequestException(ErrorCode.BATCH_019);
        }
    }

    private BatchInventorySnapshotResponse buildInventorySnapshot(
            List<Inventory> inventories,
            Map<String, Warehouses> warehouseMap,
            Map<String, Locations> locationMap) {
        List<Inventory> safeInventories = inventories == null ? List.of() : inventories;
        BigDecimal totalOnHandQuantity = sumInventories(safeInventories, Inventory::getOnHandQuantity);
        BigDecimal totalQuarantineQuantity = sumInventories(safeInventories, Inventory::getQuarantineQuantity);
        BigDecimal totalReservedQuantity = sumInventories(safeInventories, Inventory::getReservedQuantity);
        BigDecimal totalAvailableQuantity = sumInventories(safeInventories, Inventory::getAvailableQuantity);
        List<BatchInventorySnapshotResponse.WarehouseInventoryResponse> warehouseResponses = safeInventories.isEmpty()
                ? List.of()
                : groupInventoriesByWarehouse(safeInventories).entrySet().stream()
                        .sorted(Comparator.comparing(entry -> resolveWarehouseSortKey(entry.getKey(), warehouseMap)))
                        .map(entry -> buildWarehouseInventoryResponse(entry.getKey(), entry.getValue(), warehouseMap, locationMap))
                        .collect(Collectors.toList());
        return BatchInventorySnapshotResponse.builder()
                .totalOnHandQuantity(totalOnHandQuantity)
                .totalQuarantineQuantity(totalQuarantineQuantity)
                .totalReservedQuantity(totalReservedQuantity)
                .totalAvailableQuantity(totalAvailableQuantity)
                .warehouseCount(safeInventories.stream().map(Inventory::getWarehouseId).filter(Objects::nonNull).distinct().count())
                .locationCount(safeInventories.stream().map(Inventory::getLocationId).filter(Objects::nonNull).distinct().count())
                .warehouses(warehouseResponses)
                .build();
    }

    private BatchInventorySnapshotResponse.WarehouseInventoryResponse buildWarehouseInventoryResponse(
            String warehouseId,
            List<Inventory> warehouseInventories,
            Map<String, Warehouses> warehouseMap,
            Map<String, Locations> locationMap) {
        Warehouses warehouse = warehouseMap.get(warehouseId);
        List<BatchInventorySnapshotResponse.LocationInventoryResponse> locationResponses = groupInventoriesByLocation(warehouseInventories)
                .entrySet().stream()
                .sorted(Comparator.comparing(entry -> resolveLocationSortKey(entry.getKey(), locationMap)))
                .map(entry -> buildLocationInventoryResponse(entry.getKey(), entry.getValue(), locationMap))
                .collect(Collectors.toList());
        return BatchInventorySnapshotResponse.WarehouseInventoryResponse.builder()
                .warehouseId(warehouseId)
                .warehouseCode(warehouse == null ? null : warehouse.getCode())
                .warehouseName(warehouse == null ? null : warehouse.getName())
                .onHandQuantity(sumInventories(warehouseInventories, Inventory::getOnHandQuantity))
                .quarantineQuantity(sumInventories(warehouseInventories, Inventory::getQuarantineQuantity))
                .reservedQuantity(sumInventories(warehouseInventories, Inventory::getReservedQuantity))
                .availableQuantity(sumInventories(warehouseInventories, Inventory::getAvailableQuantity))
                .locationCount(warehouseInventories.stream().map(Inventory::getLocationId).filter(Objects::nonNull).distinct().count())
                .locations(locationResponses)
                .build();
    }

    private BatchInventorySnapshotResponse.LocationInventoryResponse buildLocationInventoryResponse(
            String locationKey,
            List<Inventory> locationInventories,
            Map<String, Locations> locationMap) {
        String locationId = NO_LOCATION_KEY.equals(locationKey) ? null : locationKey;
        Locations location = locationId == null ? null : locationMap.get(locationId);
        LocalDateTime lastMovementAt = locationInventories.stream()
                .map(Inventory::getLastMovementAt)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);
        return BatchInventorySnapshotResponse.LocationInventoryResponse.builder()
                .locationId(locationId)
                .locationCode(location == null ? null : location.getCode())
                .locationName(location == null ? null : location.getName())
                .onHandQuantity(sumInventories(locationInventories, Inventory::getOnHandQuantity))
                .quarantineQuantity(sumInventories(locationInventories, Inventory::getQuarantineQuantity))
                .reservedQuantity(sumInventories(locationInventories, Inventory::getReservedQuantity))
                .availableQuantity(sumInventories(locationInventories, Inventory::getAvailableQuantity))
                .lastMovementAt(lastMovementAt)
                .build();
    }

    private Map<String, List<Inventory>> groupInventoriesByBatch(List<Inventory> inventories) {
        return inventories.stream()
                .filter(inventory -> inventory.getBatchId() != null)
                .collect(Collectors.groupingBy(Inventory::getBatchId, LinkedHashMap::new, Collectors.toList()));
    }

    private Map<String, List<Inventory>> groupInventoriesByWarehouse(List<Inventory> inventories) {
        return inventories.stream()
                .collect(Collectors.groupingBy(Inventory::getWarehouseId, LinkedHashMap::new, Collectors.toList()));
    }

    private Map<String, List<Inventory>> groupInventoriesByLocation(List<Inventory> inventories) {
        Map<String, List<Inventory>> inventoryByLocation = new LinkedHashMap<>();
        for (Inventory inventory : inventories) {
            String locationKey = inventory.getLocationId() == null ? NO_LOCATION_KEY : inventory.getLocationId();
            inventoryByLocation.computeIfAbsent(locationKey, unused -> new ArrayList<>()).add(inventory);
        }
        return inventoryByLocation;
    }

    private boolean hasPhysicalStock(List<Inventory> inventories) {
        return sumInventories(inventories, Inventory::getOnHandQuantity).compareTo(BigDecimal.ZERO) > 0;
    }

    private boolean hasPositiveAvailableQuantity(List<Inventory> inventories) {
        return sumInventories(inventories, Inventory::getAvailableQuantity).compareTo(BigDecimal.ZERO) > 0;
    }

    private BigDecimal sumInventories(List<Inventory> inventories, Function<Inventory, BigDecimal> extractor) {
        return inventories.stream()
                .map(extractor)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String resolveWarehouseSortKey(String warehouseId, Map<String, Warehouses> warehouseMap) {
        Warehouses warehouse = warehouseMap.get(warehouseId);
        if (warehouse == null) {
            return warehouseId == null ? "" : warehouseId;
        }
        return warehouse.getCode() == null ? Optional.ofNullable(warehouse.getName()).orElse(warehouse.getId()) : warehouse.getCode();
    }

    private String resolveLocationSortKey(String locationId, Map<String, Locations> locationMap) {
        if (NO_LOCATION_KEY.equals(locationId)) {
            return "";
        }
        Locations location = locationMap.get(locationId);
        if (location == null) {
            return locationId == null ? "" : locationId;
        }
        return location.getCode() == null ? Optional.ofNullable(location.getName()).orElse(location.getId()) : location.getCode();
    }

    private Long daysToExpiry(LocalDate expiryDate, LocalDate today) {
        return expiryDate == null ? null : ChronoUnit.DAYS.between(today, expiryDate);
    }

    private String resolveExpiryUrgency(LocalDate expiryDate, LocalDate today) {
        Long daysToExpiry = daysToExpiry(expiryDate, today);
        if (daysToExpiry == null) {
            return null;
        }
        if (daysToExpiry <= 7) {
            return "CRITICAL";
        }
        if (daysToExpiry <= 14) {
            return "WARNING";
        }
        return "INFO";
    }

    private List<String> extractWorkflowNotes(String notes) {
        if (notes == null || notes.isBlank()) {
            return List.of();
        }
        return notes.lines()
                .map(String::trim)
                .filter(line -> line.startsWith("[QUARANTINE]") || line.startsWith("[RELEASE]"))
                .collect(Collectors.toList());
    }

    private Batch getBatchOrThrow(String batchId) {
        return batchRepository.findById(batchId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.BATCH_001));
    }

    private Products getProductOrThrow(String productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.PROD_001));
    }

    private Products getBatchTrackedProductOrThrow(String productId) {
        Products product = getProductOrThrow(productId);
        if (!Boolean.TRUE.equals(product.getRequiresBatchTracking())) {
            throw new BadRequestException(ErrorCode.BATCH_009);
        }
        return product;
    }

    private Warehouses validateWarehouseExists(String warehouseId) {
        return wareHouseRepository.findById(warehouseId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.WHS_001));
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private <T> Set<String> collectIds(Collection<T> items, Function<T, String> extractor) {
        if (items == null || items.isEmpty()) {
            return Collections.emptySet();
        }
        return items.stream()
                .map(extractor)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private <T extends BaseEntity> Map<String, T> toEntityMap(Collection<T> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyMap();
        }
        return entities.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(BaseEntity::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
    }

    private String appendWorkflowNote(String existingNotes, String workflowNote) {
        if (workflowNote == null || workflowNote.isBlank()) {
            return existingNotes;
        }
        if (existingNotes == null || existingNotes.isBlank()) {
            return workflowNote;
        }
        return existingNotes + System.lineSeparator() + workflowNote;
    }

    private String buildQuarantineAuditNote(QuarantineBatchRequest request) {
        StringBuilder note = new StringBuilder("[QUARANTINE] reason=").append(request.getReason());
        if (request.getExpectedResolutionDate() != null) {
            note.append("; expected_resolution_date=").append(request.getExpectedResolutionDate());
        }
        note.append("; notify_manager=").append(Boolean.TRUE.equals(request.getNotifyManager()));
        return note.toString();
    }

    private String buildReleaseAuditNote(ReleaseBatchRequest request) {
        return "[RELEASE] release_notes=" + request.getReleaseNotes();
    }

    private void setAuditFieldsForCreate(Batch batch, Account user) {
        batch.setCreatedBy(user.getId());
        batch.setUpdatedBy(user.getId());
    }

    private void setAuditFieldsForUpdate(Batch batch, Account user) {
        batch.setUpdatedBy(user.getId());
        batch.setUpdatedAt(LocalDateTime.now());
    }

    private Account getCurrentUser() {
        String username = SecurityUtils.getCurrentUsername();
        return accountRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("No authenticated user found with username: {}", username);
                    return new BadRequestException(ErrorCode.AUTH_002);
                });
    }
}
