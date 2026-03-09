package org.demo.whs.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.Products;
import org.demo.whs.entity.PurchaseOrderLines;
import org.demo.whs.entity.PurchaseOrders;
import org.demo.whs.entity.dto.request.PurchaseOrderLines.PurchaseOrderLinesRequest;
import org.demo.whs.entity.dto.request.PurchaseOrderLines.UpdatePurchaseOrderLinesRequest;
import org.demo.whs.entity.dto.response.PurchaseOrderLines.PurchaseOrderLinesResponse;
import org.demo.whs.entity.enums.ProductStatus;
import org.demo.whs.entity.enums.PurchaseOrdersStatus;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.exception.NotFoundException;
import org.demo.whs.mapper.PurchaseOrderLinesMapper;
import org.demo.whs.repository.ProductRepository;
import org.demo.whs.repository.PurchaseOrderLinesRepository;
import org.demo.whs.repository.PurchaseOrdersRepository;
import org.demo.whs.service.PurchaseOrderLinesService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Service implementation for managing purchase order lines.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PurchaseOrderLinesServiceImpl implements PurchaseOrderLinesService {

    private final PurchaseOrderLinesRepository purchaseOrderLinesRepository;
    private final PurchaseOrdersRepository purchaseOrdersRepository;
    private final ProductRepository productRepository;
    private final PurchaseOrderLinesMapper purchaseOrderLinesMapper;

    @Override
    @Transactional
    public PurchaseOrderLinesResponse create(PurchaseOrderLinesRequest request) {
        log.info("Create purchase order line, purchaseOrderId={}", request.getPurchaseOrderId());

        //Step 1: Validate the parent purchase order exists and is in DRAFT status
        PurchaseOrders po = getDraftPurchaseOrder(request.getPurchaseOrderId());

        //Step 2: Validate the product exists and is ACTIVE
        Products product = getActiveProduct(request.getProductId());

        //Step 3: Ensure no duplicate product line exists within the same purchase order
        validateNoDuplicateLine(po.getId(), product.getId(), null);

        //Step 4: Determine the next line number for the purchase order
        Integer nextLineNumber = purchaseOrderLinesRepository
                .findTopByPurchaseOrderIdOrderByLineNumberDesc(po.getId())
                .map(line -> line.getLineNumber() + 1)
                .orElse(1);

        //Step 5: Create the purchase order line entity, calculate line total, and save
        PurchaseOrderLines line = purchaseOrderLinesMapper.toEntity(request);
        validateOrderLineAmounts(line);
        line.setLineNumber(nextLineNumber);
        line.setQuantityReceived(BigDecimal.ZERO);
        line.setLineTotal(calculateLineTotal(request.getQuantityOrdered(), request.getUnitPrice()));

        //Step 6: Return the created line as a response
        return purchaseOrderLinesMapper.toResponse(purchaseOrderLinesRepository.save(line));
    }

    @Override
    @Transactional
    public PurchaseOrderLinesResponse update(String id, UpdatePurchaseOrderLinesRequest request) {
        log.info("Update purchase order line, id={}", id);

        //Step 1: Fetch the existing line with a lock and validate the parent purchase order is in DRAFT status
        PurchaseOrderLines line = getLockedOrderLine(id);

        //Step 2: If the product is changing, validate the new product exists and is ACTIVE, and check for duplicates
        getDraftPurchaseOrder(line.getPurchaseOrderId());

        // Only validate product and duplicates if the update request includes a different productId than the existing line
        if (isProductChanging(request, line)) {
            getActiveProduct(request.getProductId());
            validateNoDuplicateLine(line.getPurchaseOrderId(), request.getProductId(), id);
        }

        //Step 3: Merge the updates from the request into the existing entity, validate amounts, recalculate line total, and save
        purchaseOrderLinesMapper.updateEntity(line, request);
        validateOrderLineAmounts(line);

        // Recalculate line total after any changes to quantity or unit price
        line.setLineTotal(calculateLineTotal(line.getQuantityOrdered(), line.getUnitPrice()));

        //Step 4: Return the updated line as a response
        purchaseOrderLinesRepository.save(line);
        return purchaseOrderLinesMapper.toResponse(line);
    }

    @Override
    @Transactional
    public void delete(String id) {
        log.info("Delete purchase order line, id={}", id);

        PurchaseOrderLines line = getLockedOrderLine(id);
        getDraftPurchaseOrder(line.getPurchaseOrderId());
        purchaseOrderLinesRepository.delete(line);
    }

    // Private helpers
    /** Fetches a PO with a pessimistic lock and validates its status is DRAFT. */
    private PurchaseOrders getDraftPurchaseOrder(String purchaseOrderId) {
        PurchaseOrders po = purchaseOrdersRepository.findByIdForUpdate(purchaseOrderId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.PO_001));

        if (po.getStatus() != PurchaseOrdersStatus.DRAFT) {
            throw new BadRequestException(ErrorCode.PO_002);
        }
        return po;
    }

    /** Fetches a purchase order line with a pessimistic lock. */
    private PurchaseOrderLines getLockedOrderLine(String id) {
        return purchaseOrderLinesRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new NotFoundException(ErrorCode.POL_001));
    }

    /** Fetches a product and validates it is ACTIVE. */
    private Products getActiveProduct(String productId) {
        Products product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.PROD_001));

        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new BadRequestException(ErrorCode.POL_004);
        }
        return product;
    }

    /**
     * Ensures no duplicate product exists within the same PO.
     *
     * @param excludeLineId the current line id to exclude during update checks; {@code null} on create.
     */
    private void validateNoDuplicateLine(String purchaseOrderId, String productId, String excludeLineId) {
        boolean isDuplicate = (excludeLineId == null)
                ? purchaseOrderLinesRepository.existsByPurchaseOrderIdAndProductId(purchaseOrderId, productId)
                : purchaseOrderLinesRepository.existsByPurchaseOrderIdAndProductIdAndIdNot(purchaseOrderId, productId, excludeLineId);

        if (isDuplicate) {
            log.info("Duplicate product in purchase order, purchaseOrderId={}, productId={}", purchaseOrderId, productId);
            throw new BadRequestException(ErrorCode.POL_005);
        }
    }

    /** Validates business rules on order line amounts after a mapper merge. */
    private void validateOrderLineAmounts(PurchaseOrderLines line) {
        if (line.getQuantityOrdered() == null || line.getQuantityOrdered().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException(ErrorCode.POL_008);
        }
        if (line.getUnitPrice() == null || line.getUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException(ErrorCode.POL_007);
        }
        if (line.getQuantityReceived().compareTo(line.getQuantityOrdered()) > 0) {
            throw new BadRequestException(ErrorCode.POL_006);
        }
    }

    /** Calculates the line total, rounded to 2 decimal places. */
    private BigDecimal calculateLineTotal(BigDecimal quantityOrdered, BigDecimal unitPrice) {
        return quantityOrdered.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);
    }

    /** Returns {@code true} when the update request carries a different product than the existing line. */
    private boolean isProductChanging(UpdatePurchaseOrderLinesRequest request, PurchaseOrderLines line) {
        return request.getProductId() != null && !request.getProductId().equals(line.getProductId());
    }
}