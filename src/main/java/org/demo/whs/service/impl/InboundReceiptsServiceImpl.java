package org.demo.whs.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.*;
import org.demo.whs.entity.dto.request.InboundReceipts.InboundReceiptsFilterRequest;
import org.demo.whs.entity.dto.request.InboundReceipts.InboundReceiptsRequest;
import org.demo.whs.entity.dto.request.InboundReceipts.UpdateInboundReceiptsRequest;
import org.demo.whs.entity.dto.response.InboundReceiptLines.InboundReceiptLinesResponse;
import org.demo.whs.entity.dto.response.InboundReceipts.InboundReceiptsResponse;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.enums.InboundReceiptsStatus;
import org.demo.whs.entity.enums.ProductStatus;
import org.demo.whs.entity.enums.PurchaseOrdersStatus;
import org.demo.whs.entity.enums.QualityStatus;
import org.demo.whs.entity.enums.ReferenceType;
import org.demo.whs.entity.enums.StockMovementsType;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.exception.NotFoundException;
import org.demo.whs.mapper.InboundReceiptLinesMapper;
import org.demo.whs.mapper.InboundReceiptsMapper;
import org.demo.whs.mapper.StockMovementsMapper;
import org.demo.whs.repository.*;
import org.demo.whs.repository.specification.InboundReceiptsSpecification;
import org.demo.whs.security.SecurityUtils;
import org.demo.whs.service.InboundReceiptsService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of the InboundReceiptsService.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class InboundReceiptsServiceImpl implements InboundReceiptsService {

    private final InboundReceiptsRepository inboundReceiptsRepository;
    private final InboundReceiptLinesRepository inboundReceiptLinesRepository;
    private final PurchaseOrdersRepository purchaseOrdersRepository;
    private final PurchaseOrderLinesRepository purchaseOrderLinesRepository;
    private final WareHouseRepository wareHouseRepository;
    private final LocationRepository locationRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final StockMovementsRepository stockMovementsRepository;
    private final BatchRepository batchRepository;
    private final AccountRepository accountRepository;
    private final InboundReceiptsMapper inboundReceiptsMapper;
    private final InboundReceiptLinesMapper inboundReceiptLinesMapper;
    private final StockMovementsMapper stockMovementsMapper;

    @Override
    @Transactional
    public InboundReceiptsResponse create(InboundReceiptsRequest request) {
        log.info("Create inbound receipt draft requested, purchaseOrderId={}", request.getPurchaseOrderId());

        // Step 1: Validate Purchase Order
        PurchaseOrders purchaseOrders = purchaseOrdersRepository.findByIdForUpdate(request.getPurchaseOrderId())
                .orElseThrow(() -> new NotFoundException("Purchase order not found", ErrorCode.PO_001));

        if (purchaseOrders.getStatus() != PurchaseOrdersStatus.CONFIRMED
                && purchaseOrders.getStatus() != PurchaseOrdersStatus.PARTIALLY_RECEIVED) {
            throw new BadRequestException(
                    "Receipt can only be created from CONFIRMED or PARTIALLY_RECEIVED purchase orders",
                    ErrorCode.COM_001
            );
        }

        // Step 2: Fetch Warehouse info
        Warehouses warehouses = wareHouseRepository.findById(purchaseOrders.getWarehouseId())
                .orElseThrow(() -> new NotFoundException("Warehouse not found", ErrorCode.WHS_001));

        // Step 3: Create draft receipt
        String receiptNumber = generateInboundReceiptNumber();
        String actorId = getCurrentActorId();

        InboundReceipts receipt = inboundReceiptsMapper.toEntity(request);
        receipt.setReceiptNumber(receiptNumber);
        receipt.setWarehouseId(purchaseOrders.getWarehouseId());
        receipt.setCreatedBy(actorId);
        receipt.setUpdatedBy(actorId);

        receipt = inboundReceiptsRepository.save(receipt);

        log.info("Inbound receipt draft created successful, id={}, receiptNumber={}", receipt.getId(), receipt.getReceiptNumber());
        return inboundReceiptsMapper.toResponse(receipt, purchaseOrders, warehouses, Collections.emptyList());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<InboundReceiptsResponse> getAll(InboundReceiptsFilterRequest filter, Pageable pageable) {
        log.info("Get inbound receipts, filter={}, pageable={}", filter, pageable);
        validatePageable(pageable);
        normalizeAndValidateFilter(filter);

        Page<InboundReceipts> receiptPage = inboundReceiptsRepository.findAll(
                InboundReceiptsSpecification.withFilter(filter),
                pageable
        );

        List<InboundReceipts> receipts = receiptPage.getContent();
        if (receipts.isEmpty()) {
            return PageResponse.from(receiptPage, Collections.emptyList());
        }

        // Fetch related POs and Warehouses in bulk for mapping
        Set<String> poIds = receipts.stream().map(InboundReceipts::getPurchaseOrderId).collect(Collectors.toSet());
        Set<String> whIds = receipts.stream().map(InboundReceipts::getWarehouseId).collect(Collectors.toSet());

        Map<String, PurchaseOrders> poMap = purchaseOrdersRepository.findAllById(poIds).stream()
                .collect(Collectors.toMap(PurchaseOrders::getId, po -> po));
        Map<String, Warehouses> whMap = wareHouseRepository.findByIdIn(whIds).stream()
                .collect(Collectors.toMap(Warehouses::getId, wh -> wh));

        List<InboundReceiptsResponse> responses = receipts.stream()
                .map(receipt -> inboundReceiptsMapper.toResponse(
                        receipt,
                        poMap.get(receipt.getPurchaseOrderId()),
                        whMap.get(receipt.getWarehouseId()),
                        Collections.emptyList()
                ))
                .toList();

        return PageResponse.from(receiptPage, responses);
    }

    @Override
    @Transactional(readOnly = true)
    public InboundReceiptsResponse getById(String id) {
        log.info("Get inbound receipt by id={}", id);
        InboundReceipts receipt = inboundReceiptsRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Inbound receipt not found", ErrorCode.COM_004));

        PurchaseOrders po = purchaseOrdersRepository.findById(receipt.getPurchaseOrderId()).orElse(null);
        Warehouses wh = wareHouseRepository.findById(receipt.getWarehouseId()).orElse(null);

        return inboundReceiptsMapper.toResponse(receipt, po, wh, getLineResponses(receipt.getId()));
    }

    @Override
    @Transactional
    public InboundReceiptsResponse update(String id, UpdateInboundReceiptsRequest request) {
        log.info("Update inbound receipt draft requested, id={}", id);
        InboundReceipts receipt = inboundReceiptsRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new NotFoundException("Inbound receipt not found", ErrorCode.COM_004));

        if (receipt.getStatus() != InboundReceiptsStatus.DRAFT) {
            throw new BadRequestException("Only draft receipts can be updated", ErrorCode.COM_001);
        }

        inboundReceiptsMapper.updateEntity(receipt, request);
        receipt.setUpdatedBy(getCurrentActorId());

        receipt = inboundReceiptsRepository.save(receipt);

        PurchaseOrders po = purchaseOrdersRepository.findById(receipt.getPurchaseOrderId()).orElse(null);
        Warehouses wh = wareHouseRepository.findById(receipt.getWarehouseId()).orElse(null);

        return inboundReceiptsMapper.toResponse(receipt, po, wh, getLineResponses(receipt.getId()));
    }

    @Override
    @Transactional
    public void delete(String id) {
        log.info("Delete inbound receipt draft requested, id={}", id);
        InboundReceipts receipt = inboundReceiptsRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new NotFoundException("Inbound receipt not found", ErrorCode.COM_004));

        if (receipt.getStatus() != InboundReceiptsStatus.DRAFT) {
            throw new BadRequestException("Only draft receipts can be deleted", ErrorCode.COM_001);
        }

        long lineCount = inboundReceiptLinesRepository.countByInboundReceiptId(id);
        if (lineCount > 0) {
            throw new BadRequestException("Cannot delete receipt with lines", ErrorCode.COM_001);
        }

        inboundReceiptsRepository.delete(receipt);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InboundReceiptsResponse> getByPurchaseOrderId(String purchaseOrderId) {
        log.info("Get inbound receipts by purchaseOrderId={}", purchaseOrderId);
        List<InboundReceipts> receipts = inboundReceiptsRepository.findByPurchaseOrderIdOrderByCreatedAtDesc(purchaseOrderId);
        if (receipts.isEmpty()) {
            return Collections.emptyList();
        }

        PurchaseOrders po = purchaseOrdersRepository.findById(purchaseOrderId).orElse(null);
        Set<String> whIds = receipts.stream().map(InboundReceipts::getWarehouseId).collect(Collectors.toSet());
        Map<String, Warehouses> whMap = wareHouseRepository.findByIdIn(whIds).stream()
                .collect(Collectors.toMap(Warehouses::getId, wh -> wh));

        return receipts.stream()
                .map(receipt -> inboundReceiptsMapper.toResponse(
                        receipt,
                        po,
                        whMap.get(receipt.getWarehouseId()),
                        Collections.emptyList()
                ))
                .toList();
    }

    private String generateInboundReceiptNumber() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        while (true) {
            String suffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
            String receiptNumber = String.format("GR-%s-%s", datePart, suffix);
            if (!inboundReceiptsRepository.existsByReceiptNumber(receiptNumber)) {
                return receiptNumber;
            }
        }
    }

    private String getCurrentActorId() {
        String username = SecurityUtils.getCurrentUsername();
        if (username == null || username.isBlank()) {
            throw new BadRequestException("Unauthenticated request", ErrorCode.AUTH_002);
        }

        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new BadRequestException("User account not found", ErrorCode.AUTH_002));

        return account.getId();
    }

    @Override
    @Transactional
    public InboundReceiptsResponse confirm(String id) {
        log.info("Confirm inbound receipt requested, id={}", id);
        String actorId = getCurrentActorId();
        LocalDateTime now = LocalDateTime.now();

        // Step 1: Load receipt and lines with locks and check status
        InboundReceipts receipt = loadDraftReceiptForConfirm(id);

        // Step 2: Validate receipt lines and related entities, and prepare updates
        List<InboundReceiptLines> receiptLines = loadReceiptLinesForConfirm(id);

        // Step 3: Load purchase order and lines with locks
        PurchaseOrders purchaseOrder = validatePurchaseOrderForReceipt(receipt);

        // Step 4: Map purchase order lines for quick access
        List<PurchaseOrderLines> poLines = loadPurchaseOrderLinesForConfirm(purchaseOrder.getId());
        Map<String, PurchaseOrderLines> poLineMap = poLines.stream()
                .collect(Collectors.toMap(PurchaseOrderLines::getId, poLine -> poLine));

        // Step 5: Process each receipt line
        for (InboundReceiptLines receiptLine : receiptLines) {

            //Step 5.1: Validate receipt line against purchase order line and related entities
            PurchaseOrderLines purchaseOrderLine = validateReceiptLineAgainstPurchaseOrderLine(receipt, receiptLine, poLineMap);

            //Step 5.2: Validate product, location, quality rules, and resolve batch if needed
            Products product = validateProductForReceiptLine(receiptLine);
            validateLocationForReceiptLine(receipt, receiptLine);
            validateQualityRules(receiptLine);
            resolveBatchForReceiptLine(receiptLine, product);

            // Step 5.3: Increase inventory and write stock movement
            InventorySnapshot inventorySnapshot = increaseInventoryForReceiptLine(receipt, receiptLine, actorId, now);
            writeInboundMovement(receipt, receiptLine, inventorySnapshot, actorId);

            // Step 5.4: Update purchase order line received quantity
            BigDecimal updatedQuantityReceived = zeroIfNull(purchaseOrderLine.getQuantityReceived())
                    .add(receiptLine.getQuantityReceived());
            purchaseOrderLine.setQuantityReceived(updatedQuantityReceived);
            purchaseOrderLine.setUpdatedBy(actorId);
        }

        // Step 6: Save all updates in batch
        purchaseOrderLinesRepository.saveAll(poLines);

        purchaseOrder.setStatus(recomputePurchaseOrderStatus(poLines));
        purchaseOrder.setUpdatedBy(actorId);
        purchaseOrdersRepository.save(purchaseOrder);

        markReceiptConfirmed(receipt, actorId, now);

        // Step 7: Save confirmed receipt
        InboundReceipts confirmedReceipt = inboundReceiptsRepository.save(receipt);
        Warehouses warehouse = wareHouseRepository.findById(confirmedReceipt.getWarehouseId()).orElse(null);

        log.info(
                "Inbound receipt confirmed successfully, id={}, receiptNumber={}, purchaseOrderId={}",
                confirmedReceipt.getId(), confirmedReceipt.getReceiptNumber(), purchaseOrder.getId()
        );

        // Step 8: Map and return response
        return inboundReceiptsMapper.toResponse(
                confirmedReceipt,
                purchaseOrder,
                warehouse,
                getLineResponses(confirmedReceipt.getId())
        );
    }

    private InboundReceipts loadDraftReceiptForConfirm(String id) {
        InboundReceipts receipt = inboundReceiptsRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new NotFoundException("Inbound receipt not found", ErrorCode.COM_004));

        if (receipt.getStatus() != InboundReceiptsStatus.DRAFT) {
            throw new BadRequestException("Only draft receipts can be confirmed", ErrorCode.COM_001);
        }

        return receipt;
    }

    private List<InboundReceiptLines> loadReceiptLinesForConfirm(String receiptId) {
        List<InboundReceiptLines> receiptLines = inboundReceiptLinesRepository.findByInboundReceiptIdOrderByLineNumberAsc(receiptId);
        if (receiptLines.isEmpty()) {
            throw new BadRequestException("Cannot confirm receipt without lines", ErrorCode.COM_001);
        }
        return receiptLines;
    }

    private PurchaseOrders validatePurchaseOrderForReceipt(InboundReceipts receipt) {
        PurchaseOrders purchaseOrder = purchaseOrdersRepository.findByIdForUpdate(receipt.getPurchaseOrderId())
                .orElseThrow(() -> new NotFoundException("Purchase order not found", ErrorCode.PO_001));

        if (purchaseOrder.getStatus() != PurchaseOrdersStatus.CONFIRMED
                && purchaseOrder.getStatus() != PurchaseOrdersStatus.PARTIALLY_RECEIVED) {
            throw new BadRequestException(
                    "Receipt can only be confirmed from CONFIRMED or PARTIALLY_RECEIVED purchase orders",
                    ErrorCode.COM_001
            );
        }

        return purchaseOrder;
    }

    private List<PurchaseOrderLines> loadPurchaseOrderLinesForConfirm(String purchaseOrderId) {
        List<PurchaseOrderLines> poLines = purchaseOrderLinesRepository.findByPurchaseOrderIdForUpdate(purchaseOrderId);
        if (poLines.isEmpty()) {
            throw new NotFoundException("Purchase order lines not found", ErrorCode.PO_002);
        }
        return poLines;
    }

    private PurchaseOrderLines validateReceiptLineAgainstPurchaseOrderLine(InboundReceipts receipt,
                                                                           InboundReceiptLines receiptLine,
                                                                           Map<String, PurchaseOrderLines> poLineMap) {
        PurchaseOrderLines purchaseOrderLine = poLineMap.get(receiptLine.getPurchaseOrderLineId());
        if (purchaseOrderLine == null) {
            throw new BadRequestException(
                    "Receipt line with id " + receiptLine.getId() + " does not match any purchase order line",
                    ErrorCode.COM_001
            );
        }

        if (!Objects.equals(purchaseOrderLine.getPurchaseOrderId(), receipt.getPurchaseOrderId())) {
            throw new BadRequestException("Receipt line does not belong to the same purchase order", ErrorCode.COM_001);
        }

        if (!Objects.equals(purchaseOrderLine.getProductId(), receiptLine.getProductId())) {
            throw new BadRequestException("Receipt line product does not match purchase order line", ErrorCode.COM_001);
        }

        BigDecimal receiptQuantity = receiptLine.getQuantityReceived();
        if (receiptQuantity == null || receiptQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Receipt quantity must be greater than zero", ErrorCode.COM_001);
        }

        BigDecimal remaining = zeroIfNull(purchaseOrderLine.getQuantityOrdered())
                .subtract(zeroIfNull(purchaseOrderLine.getQuantityReceived()));
        if (receiptQuantity.compareTo(remaining) > 0) {
            throw new BadRequestException(
                    "Receipt line with id " + receiptLine.getId()
                            + " has quantity received greater than remaining quantity on purchase order line",
                    ErrorCode.POL_006
            );
        }

        return purchaseOrderLine;
    }

    private Products validateProductForReceiptLine(InboundReceiptLines receiptLine) {
        Products product = productRepository.findById(receiptLine.getProductId())
                .orElseThrow(() -> new NotFoundException(
                        "Product not found for receipt line with id " + receiptLine.getId(),
                        ErrorCode.PROD_001
                ));

        if (product.getStatus() == ProductStatus.INACTIVE) {
            throw new BadRequestException(
                    "Product with id " + product.getId() + " is inactive and cannot be received",
                    ErrorCode.COM_001
            );
        }

        return product;
    }

    private void validateLocationForReceiptLine(InboundReceipts receipt, InboundReceiptLines receiptLine) {
        Locations location = locationRepository.findById(receiptLine.getLocationId())
                .orElseThrow(() -> new NotFoundException(
                        "Location not found for receipt line with id " + receiptLine.getId(),
                        ErrorCode.LOC_001
                ));

        if (!Objects.equals(location.getWarehouseId(), receipt.getWarehouseId())) {
            throw new BadRequestException("Location not in receipt warehouse", ErrorCode.LOC_002);
        }
    }

    private void validateQualityRules(InboundReceiptLines receiptLine) {
        if (receiptLine.getQualityStatus() == QualityStatus.QUARANTINE
                && !StringUtils.hasText(receiptLine.getNotes())) {
            throw new BadRequestException("Quarantine receipt lines must include notes", ErrorCode.COM_001);
        }
    }

    private Batch resolveBatchForReceiptLine(InboundReceiptLines receiptLine, Products product) {
        if (!Boolean.TRUE.equals(product.getRequiresBatchTracking())) {
            return null;
        }

        if (!StringUtils.hasText(receiptLine.getBatchId())) {
            throw new BadRequestException("Batch is required for batch-tracked products", ErrorCode.COM_001);
        }

        Batch batch = batchRepository.findById(receiptLine.getBatchId())
                .orElseThrow(() -> new NotFoundException(
                        "Batch not found for receipt line with id " + receiptLine.getId(),
                        ErrorCode.BATCH_001
                ));

        if (!Objects.equals(batch.getProductId(), receiptLine.getProductId())) {
            throw new BadRequestException("Batch does not belong to receipt line product", ErrorCode.COM_001);
        }

        return batch;
    }

    private InventorySnapshot increaseInventoryForReceiptLine(
            InboundReceipts receipt,
            InboundReceiptLines receiptLine,
            String actorId,
            LocalDateTime now
    ) {
        Inventory inventory = inventoryRepository.findByDimensionForUpdate(
                receiptLine.getProductId(),
                receipt.getWarehouseId(),
                receiptLine.getLocationId(),
                receiptLine.getBatchId()
        ).orElseGet(() -> createOrReloadInventory(receipt, receiptLine, actorId));

        BigDecimal quantityBefore = zeroIfNull(inventory.getOnHandQuantity());
        BigDecimal quantityAfter = quantityBefore.add(receiptLine.getQuantityReceived());

        inventory.setOnHandQuantity(quantityAfter);
        inventory.setUpdatedBy(actorId);
        inventory.setLastMovementAt(now);
        Inventory savedInventory = inventoryRepository.save(inventory);

        return new InventorySnapshot(savedInventory, quantityBefore, quantityAfter);
    }

    private Inventory createOrReloadInventory(InboundReceipts receipt, InboundReceiptLines receiptLine, String actorId) {
        try {
            Inventory inventory = Inventory.builder()
                    .productId(receiptLine.getProductId())
                    .warehouseId(receipt.getWarehouseId())
                    .locationId(receiptLine.getLocationId())
                    .batchId(receiptLine.getBatchId())
                    .onHandQuantity(BigDecimal.ZERO)
                    .reservedQuantity(BigDecimal.ZERO)
                    .version(0)
                    .build();
            inventory.setCreatedBy(actorId);
            inventory.setUpdatedBy(actorId);
            return inventoryRepository.saveAndFlush(inventory);
        } catch (DataIntegrityViolationException ex) {
            log.warn(
                    "Inventory row already created concurrently, productId={}, warehouseId={}, locationId={}, batchId={}",
                    receiptLine.getProductId(),
                    receipt.getWarehouseId(),
                    receiptLine.getLocationId(),
                    receiptLine.getBatchId()
            );
            return inventoryRepository.findByDimensionForUpdate(
                    receiptLine.getProductId(),
                    receipt.getWarehouseId(),
                    receiptLine.getLocationId(),
                    receiptLine.getBatchId()
            ).orElseThrow(() -> new NotFoundException(
                    "Inventory not found after concurrent creation",
                    ErrorCode.INV_001
            ));
        }
    }

    private void writeInboundMovement(InboundReceipts receipt, InboundReceiptLines receiptLine, InventorySnapshot inventorySnapshot, String actorId) {
        StockMovements movement = stockMovementsMapper.toEntity(
                StockMovementsType.INBOUND,
                receiptLine.getProductId(),
                receipt.getWarehouseId(),
                receiptLine.getLocationId(),
                receiptLine.getBatchId(),
                receiptLine.getQuantityReceived(),
                inventorySnapshot.quantityBefore(),
                inventorySnapshot.quantityAfter(),
                ReferenceType.INBOUND_RECEIPT,
                receipt.getId(),
                receipt.getReceiptNumber(),
                receiptLine.getNotes(),
                actorId
        );
        stockMovementsRepository.save(movement);
    }

    private PurchaseOrdersStatus recomputePurchaseOrderStatus(List<PurchaseOrderLines> poLines) {
        boolean allReceived = poLines.stream().allMatch(poLine ->
                zeroIfNull(poLine.getQuantityReceived()).compareTo(zeroIfNull(poLine.getQuantityOrdered())) >= 0
        );
        return allReceived ? PurchaseOrdersStatus.COMPLETED : PurchaseOrdersStatus.PARTIALLY_RECEIVED;
    }

    private void markReceiptConfirmed(InboundReceipts receipt, String actorId, LocalDateTime now) {
        receipt.setStatus(InboundReceiptsStatus.CONFIRMED);
        receipt.setConfirmedAt(now);
        receipt.setConfirmedBy(actorId);
        receipt.setUpdatedBy(actorId);
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private record InventorySnapshot(Inventory inventory, BigDecimal quantityBefore, BigDecimal quantityAfter) {
    }

    private List<InboundReceiptLinesResponse> getLineResponses(String inboundReceiptId) {
        List<InboundReceiptLines> lines = inboundReceiptLinesRepository.findByInboundReceiptIdOrderByLineNumberAsc(inboundReceiptId);
        return inboundReceiptLinesMapper.toResponses(lines);
    }

    private void normalizeAndValidateFilter(InboundReceiptsFilterRequest filter) {
        if (filter == null) {
            return;
        }

        if (filter.getReceiptDateFrom() != null
                && filter.getReceiptDateTo() != null
                && filter.getReceiptDateFrom().isAfter(filter.getReceiptDateTo())) {
            throw new BadRequestException(
                    "receiptDateFrom must be less than or equal to receiptDateTo",
                    ErrorCode.COM_001
            );
        }

        if (StringUtils.hasText(filter.getStatus())) {
            try {
                filter.setStatus(InboundReceiptsStatus.valueOf(filter.getStatus().trim().toUpperCase()).name());
            } catch (IllegalArgumentException ex) {
                throw new BadRequestException("Invalid inbound receipt status", ErrorCode.COM_001);
            }
        }
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
