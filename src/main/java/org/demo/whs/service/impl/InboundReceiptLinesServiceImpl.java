package org.demo.whs.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.Batch;
import org.demo.whs.entity.InboundReceiptLines;
import org.demo.whs.entity.InboundReceipts;
import org.demo.whs.entity.Locations;
import org.demo.whs.entity.Products;
import org.demo.whs.entity.PurchaseOrderLines;
import org.demo.whs.entity.dto.request.InboundReceiptLines.InboundReceiptLineUpdateRequest;
import org.demo.whs.entity.dto.request.InboundReceiptLines.InboundReceiptLinesRequest;
import org.demo.whs.entity.dto.response.InboundReceiptLines.InboundReceiptLinesResponse;
import org.demo.whs.entity.enums.BatchStatus;
import org.demo.whs.entity.enums.InboundReceiptsStatus;
import org.demo.whs.entity.enums.LocationStatus;
import org.demo.whs.entity.enums.ProductStatus;
import org.demo.whs.entity.enums.QualityStatus;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.exception.NotFoundException;
import org.demo.whs.mapper.InboundReceiptLinesMapper;
import org.demo.whs.repository.BatchRepository;
import org.demo.whs.repository.InboundReceiptLinesRepository;
import org.demo.whs.repository.InboundReceiptsRepository;
import org.demo.whs.repository.LocationRepository;
import org.demo.whs.repository.ProductRepository;
import org.demo.whs.repository.PurchaseOrderLinesRepository;
import org.demo.whs.service.InboundReceiptLinesService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class InboundReceiptLinesServiceImpl implements InboundReceiptLinesService {

    private final InboundReceiptLinesRepository inboundReceiptLinesRepository;
    private final InboundReceiptsRepository inboundReceiptsRepository;
    private final PurchaseOrderLinesRepository purchaseOrderLinesRepository;
    private final ProductRepository productRepository;
    private final LocationRepository locationRepository;
    private final BatchRepository batchRepository;
    private final InboundReceiptLinesMapper inboundReceiptLinesMapper;

    /**
     * @param request the request body containing line details
     * @return the created line details
     */
    @Override
    @Transactional
    public InboundReceiptLinesResponse create(InboundReceiptLinesRequest request) {
        log.info("Creating inbound receipt line with request: {}", request);

        // Step 1: Load Phiếu nhập hàng (Inbound Receipt) in DRAFT status with pessimistic lock
        InboundReceipts inboundReceipt = loadDraftReceiptForLineMutation(request.getInboundReceiptId());

        // Step 2: Load chi tiết dòng hàng của đơn hàng mua (Purchase Order Line) with pessimistic lock and validate it belongs to the receipt's purchase order
        PurchaseOrderLines purchaseOrderLine = loadPurchaseOrderLineForReceipt(request.getPurchaseOrderLineId(), inboundReceipt.getPurchaseOrderId());
        // Step 3: Derive and validate product information for the line
        Products product = deriveAndValidateProductForLine(purchaseOrderLine);

        request.setBatchId(normalizeOptionalText(request.getBatchId()));
        request.setNotes(normalizeOptionalText(request.getNotes()));
        request.setQualityStatus(resolveQualityStatus(request.getQualityStatus(), null));

        // Step 4: Validate all line details (quantity, location, batch, quality rules, duplicate dimensions, remaining quantity for receipt)
        validateQuantityReceived(request.getQuantityReceived());

        // Step 5: Validate location chosen for the receipt line
        validateLocationForReceiptLine(request.getLocationId(), inboundReceipt.getWarehouseId());

        // Step 6: Validate batch details if batch tracking is required for the product
        validateBatchForReceiptLine(request.getBatchId(), product, request.getQualityStatus());
        // Step 7: Validate quality status and notes according to business rules
        validateQualityRulesForReceiptLine(request.getQualityStatus(), request.getNotes());
        // Step 8: Validate that there are no duplicate lines with the same split dimensions (location, batch, quality status) for the same purchase order line in the same receipt
        validateDuplicateSplitDimension(
                inboundReceipt.getId(),
                purchaseOrderLine.getId(),
                request.getLocationId(),
                request.getBatchId(),
                request.getQualityStatus(),
                null
        );
        // Step 9: Validate that the quantity received does not exceed the remaining quantity available for the purchase order line, considering other lines in DRAFT status for the same receipt
        validateRemainingQuantityForDraftReceipt(
                inboundReceipt.getId(),
                purchaseOrderLine,
                request.getQuantityReceived(),
                null
        );
        // Step 10: Create and save the new line, assigning the next line number for the receipt
        Integer nextLineNumber = getNextLineNumber(request.getInboundReceiptId());
        InboundReceiptLines line = inboundReceiptLinesMapper.getInboundReceiptLines(request, product, nextLineNumber);
        InboundReceiptLines savedLine = inboundReceiptLinesRepository.save(line);
        log.info("Created inbound receipt line with id: {}", savedLine.getId());

        return buildLineResponse(savedLine, product, request.getBatchId(), request.getLocationId());
    }

    /**
     * @param id      the ID of the line to update
     * @param request the request body containing updated line details
     * @return the updated line details
     */
    @Override
    @Transactional
    public InboundReceiptLinesResponse update(String id, InboundReceiptLineUpdateRequest request) {
        log.info("Updating inbound receipt line: {}", id);

        InboundReceiptLines currentLine = inboundReceiptLinesRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new NotFoundException("Inbound receipt line not found", ErrorCode.IRL_001));

        InboundReceipts receipt = loadDraftReceiptForLineMutation(currentLine.getInboundReceiptId());
        PurchaseOrderLines purchaseOrderLine = loadPurchaseOrderLineForReceipt(
                currentLine.getPurchaseOrderLineId(),
                receipt.getPurchaseOrderId()
        );
        validateCurrentLineProductConsistency(currentLine, purchaseOrderLine);
        Products product = deriveAndValidateProductForLine(purchaseOrderLine);

        request.setBatchId(request.getBatchId() == null
                ? currentLine.getBatchId()
                : normalizeOptionalText(request.getBatchId()));
        request.setNotes(request.getNotes() == null
                ? currentLine.getNotes()
                : normalizeOptionalText(request.getNotes()));
        request.setQualityStatus(resolveQualityStatus(request.getQualityStatus(), currentLine.getQualityStatus()));

        validateQuantityReceived(request.getQuantityReceived());
        validateLocationForReceiptLine(request.getLocationId(), receipt.getWarehouseId());
        validateBatchForReceiptLine(request.getBatchId(), product, request.getQualityStatus());
        validateQualityRulesForReceiptLine(request.getQualityStatus(), request.getNotes());
        validateDuplicateSplitDimension(
                currentLine.getInboundReceiptId(),
                currentLine.getPurchaseOrderLineId(),
                request.getLocationId(),
                request.getBatchId(),
                request.getQualityStatus(),
                id
        );
        validateRemainingQuantityForDraftReceipt(
                receipt.getId(),
                purchaseOrderLine,
                request.getQuantityReceived(),
                id
        );

        currentLine.setLocationId(request.getLocationId());
        currentLine.setBatchId(request.getBatchId());
        currentLine.setQuantityReceived(request.getQuantityReceived());
        currentLine.setQualityStatus(request.getQualityStatus());
        currentLine.setNotes(request.getNotes());

        InboundReceiptLines savedLine = inboundReceiptLinesRepository.save(currentLine);
        log.info("Updated inbound receipt line: {}", id);

        return buildLineResponse(savedLine, product, request.getBatchId(), request.getLocationId());
    }

    /**
     * @param id the ID of the line to delete
     */
    @Override
    @Transactional
    public void delete(String id) {
        log.info("Deleting inbound receipt line: {}", id);

        InboundReceiptLines line = inboundReceiptLinesRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new NotFoundException("Inbound receipt line not found", ErrorCode.IRL_001));

        InboundReceipts receipt = loadDraftReceiptForLineMutation(line.getInboundReceiptId());

        inboundReceiptLinesRepository.delete(line);
        log.info("Deleted inbound receipt line: {}", id);
    }

    /**
     * @param inboundReceiptId the ID of the inbound receipt
     * @return list of lines for the specified inbound receipt
     */
    @Override
    @Transactional(readOnly = true)
    public List<InboundReceiptLinesResponse> findByInboundReceiptId(String inboundReceiptId) {
        List<InboundReceiptLines> lines = inboundReceiptLinesRepository
                .findByInboundReceiptIdOrderByLineNumberAsc(inboundReceiptId);
        return lines.stream()
                .map(this::buildLineResponseFromEntity)
                .toList();
    }


    private InboundReceipts loadDraftReceiptForLineMutation(String inboundReceiptId) {
        InboundReceipts inboundReceipts = inboundReceiptsRepository.findByIdForUpdate(inboundReceiptId)
                .orElseThrow(() -> new NotFoundException("Inbound receipt not found", ErrorCode.IRL_002));

        if (inboundReceipts.getStatus() != InboundReceiptsStatus.DRAFT) {
            throw new BadRequestException(ErrorCode.IRL_003);
        }
        return inboundReceipts;
    }

    private PurchaseOrderLines loadPurchaseOrderLineForReceipt(String purchaseOrderLineId, String purchaseOrderId) {
        PurchaseOrderLines purchaseOrderLines = purchaseOrderLinesRepository.findByIdForUpdate(purchaseOrderLineId)
                .orElseThrow(() -> new NotFoundException("Purchase order line not found", ErrorCode.IRL_016));

        if (!Objects.equals(purchaseOrderId, purchaseOrderLines.getPurchaseOrderId())) {
            throw new BadRequestException(
                    "Purchase order line does not belong to receipt purchase order",
                    ErrorCode.IRL_004
            );
        }
        return purchaseOrderLines;
    }

    private Products deriveAndValidateProductForLine(PurchaseOrderLines purchaseOrderLine) {
        Products product = productRepository.findById(purchaseOrderLine.getProductId())
                .orElseThrow(() -> new NotFoundException("Product not found", ErrorCode.PROD_001));

        if (product.getStatus() == ProductStatus.INACTIVE) {
            throw new BadRequestException("Product is inactive", ErrorCode.IRL_005);
        }

        return product;
    }

    private void validateCurrentLineProductConsistency(InboundReceiptLines currentLine, PurchaseOrderLines purchaseOrderLine) {
        if (!Objects.equals(currentLine.getProductId(), purchaseOrderLine.getProductId())) {
            throw new BadRequestException(
                    "Inbound receipt line product does not match purchase order line product",
                    ErrorCode.IRL_004
            );
        }
    }

    private void validateLocationForReceiptLine(String locationId, String warehouseId) {
        Locations location = locationRepository.findById(locationId)
                .orElseThrow(() -> new NotFoundException("Location not found", ErrorCode.LOC_001));

        if (!location.getWarehouseId().equals(warehouseId)) {
            throw new BadRequestException("Location does not belong to receipt warehouse", ErrorCode.IRL_006);
        }

        if (location.getStatus() == LocationStatus.INACTIVE || location.getStatus() == LocationStatus.MAINTENANCE) {
            throw new BadRequestException("Location is not usable - status is INACTIVE or MAINTENANCE",
                    ErrorCode.IRL_007);
        }
    }

    private void validateBatchForReceiptLine(String batchId, Products products, QualityStatus qualityStatus) {
        boolean requiresBatch = Boolean.TRUE.equals(products.getRequiresBatchTracking());

        if (requiresBatch) {
            // Batch ID is required
            if (batchId == null || batchId.isBlank()) {
                throw new BadRequestException("Batch ID is required for products that require batch tracking", ErrorCode.IRL_008);
            }

            Batch batch = batchRepository.findById(batchId)
                    .orElseThrow(() -> new NotFoundException("Batch not found", ErrorCode.BATCH_001));

            if (!batch.getProductId().equals(products.getId())) {
                throw new BadRequestException("Batch does not belong to the product", ErrorCode.IRL_009);
            }

            // check batch status phù hợp với quality status của line item
            validateBatchStatusAndQuantity(batch, qualityStatus);

        } else {
            if (batchId != null && !batchId.isBlank()) {
                throw new BadRequestException("Batch is not allowed for non-batch tracked product", ErrorCode.IRL_009);
            }
        }
    }

    private void validateBatchStatusAndQuantity(Batch batch, QualityStatus qualityStatus) {
        BatchStatus batchStatus = batch.getStatus();

        if (batchStatus == BatchStatus.EXPIRED || batchStatus == BatchStatus.RECALLED) {
            throw new BadRequestException("Batch is EXPIRED or RECALLED - cannot be used", ErrorCode.IRL_011);
        }

        // nếu quality status là PASS thì batch phải ở trạng thái AVAILABLE
        if (qualityStatus == QualityStatus.PASS) {
            if (batchStatus != BatchStatus.AVAILABLE) {
                throw new BadRequestException("Batch is not compatible with quality status PASS", ErrorCode.IRL_010);
            }
        } else if (qualityStatus == QualityStatus.QUARANTINE) {
            if (batchStatus != BatchStatus.AVAILABLE && batchStatus != BatchStatus.QUARANTINE) {
                throw new BadRequestException("Batch is not compatible with quality status QUARANTINE",
                        ErrorCode.IRL_010);
            }
        }
    }

    private void validateQualityRulesForReceiptLine(QualityStatus status, String notes) {
        if (status == null) {
            throw new BadRequestException(ErrorCode.IRL_015);
        }

        if (notes != null && notes.length() > 500) {
            throw new BadRequestException("Notes cannot exceed 500 characters", ErrorCode.COM_001);
        }

        if (status == QualityStatus.QUARANTINE && !StringUtils.hasText(notes)) {
            throw new BadRequestException("Quarantine status requires notes", ErrorCode.IRL_014);
        }
    }

    private void validateRemainingQuantityForDraftReceipt(
            String inboundReceiptId,
            PurchaseOrderLines purchaseOrderLine,
            BigDecimal requestedQuantity,
            String excludeLineId
    ) {
        BigDecimal allocatedDraftQuantity = inboundReceiptLinesRepository.sumQuantityByReceiptAndPurchaseOrderLine(
                inboundReceiptId,
                purchaseOrderLine.getId(),
                excludeLineId
        );

        BigDecimal remainingQuantity = zeroIfNull(purchaseOrderLine.getQuantityOrdered())
                .subtract(zeroIfNull(purchaseOrderLine.getQuantityReceived()))
                .subtract(zeroIfNull(allocatedDraftQuantity));

        if (requestedQuantity.compareTo(remainingQuantity) > 0) {
            throw new BadRequestException(
                    String.format("Quantity exceeds remaining quantity available for this purchase order line. Remaining quantity: %s", remainingQuantity),
                    ErrorCode.IRL_012
            );
        }
    }

    private void validateDuplicateSplitDimension(String inboundReceiptId, String purchaseOrderLineId,
                                                 String locationId, String batchId,
                                                 QualityStatus qualityStatus, String excludeLineId) {
        boolean exists = inboundReceiptLinesRepository.existsByDuplicateDimension(
                inboundReceiptId, purchaseOrderLineId, locationId, batchId, qualityStatus, excludeLineId);

        if (exists) {
            throw new BadRequestException("Duplicate split dimension already exists in this receipt",
                    ErrorCode.IRL_013);
        }
    }

    private Integer getNextLineNumber(String inboundReceiptId) {
        Optional<InboundReceiptLines> lastLine = inboundReceiptLinesRepository
                .findTopByInboundReceiptIdOrderByLineNumberDesc(inboundReceiptId);

        return lastLine.map(line -> line.getLineNumber() + 1).orElse(1);
    }

    private InboundReceiptLinesResponse buildLineResponse(InboundReceiptLines line, Products product,
                                                          String batchId, String locationId) {
        String productSku = product.getSku();
        String productName = product.getName();

        String batchNumber = null;
        if (batchId != null) {
            batchNumber = batchRepository.findById(batchId)
                    .map(Batch::getBatchNumber)
                    .orElse(null);
        }

        String locationCode = null;
        String locationName = null;
        Locations location = locationRepository.findById(locationId).orElse(null);
        if (location != null) {
            locationCode = location.getCode();
            locationName = location.getName();
        }

        return inboundReceiptLinesMapper.toResponse(line, productSku, productName, batchNumber,
                locationCode, locationName);
    }

    private InboundReceiptLinesResponse buildLineResponseFromEntity(InboundReceiptLines line) {
        String productSku = null;
        String productName = null;
        Products product = productRepository.findById(line.getProductId()).orElse(null);
        if (product != null) {
            productSku = product.getSku();
            productName = product.getName();
        }

        String batchNumber = null;
        if (line.getBatchId() != null) {
            Batch batch = batchRepository.findById(line.getBatchId()).orElse(null);
            if (batch != null) {
                batchNumber = batch.getBatchNumber();
            }
        }

        String locationCode = null;
        String locationName = null;
        Locations location = locationRepository.findById(line.getLocationId()).orElse(null);
        if (location != null) {
            locationCode = location.getCode();
            locationName = location.getName();
        }

        return inboundReceiptLinesMapper.toResponse(line, productSku, productName, batchNumber,
                locationCode, locationName);
    }

    private QualityStatus resolveQualityStatus(QualityStatus requestStatus, QualityStatus currentStatus) {
        return requestStatus != null
                ? requestStatus
                : (currentStatus != null ? currentStatus : QualityStatus.PASS);
    }

    private String normalizeOptionalText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private void validateQuantityReceived(BigDecimal quantityReceived) {
        if (quantityReceived == null || quantityReceived.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Quantity received must be greater than zero", ErrorCode.IRL_012);
        }
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
