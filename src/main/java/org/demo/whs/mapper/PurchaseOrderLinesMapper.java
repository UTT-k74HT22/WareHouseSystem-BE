package org.demo.whs.mapper;

import org.demo.whs.entity.PurchaseOrderLines;
import org.demo.whs.entity.dto.request.PurchaseOrderLines.PurchaseOrderLinesRequest;
import org.demo.whs.entity.dto.request.PurchaseOrderLines.UpdatePurchaseOrderLinesRequest;
import org.demo.whs.entity.dto.response.PurchaseOrderLines.PurchaseOrderLinesResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Mapper class for Purchase Order Lines.
 */
@Component
public class PurchaseOrderLinesMapper {

    public PurchaseOrderLines toEntity(PurchaseOrderLinesRequest request) {
        if (request == null) {
            return null;
        }

        return PurchaseOrderLines.builder()
                .purchaseOrderId(request.getPurchaseOrderId())
                .productId(request.getProductId())
                .quantityOrdered(request.getQuantityOrdered())
                .quantityReceived(BigDecimal.ZERO)
                .unitPrice(request.getUnitPrice())
                .notes(request.getNotes())
                .build();
    }

    public void updateEntity(PurchaseOrderLines entity, UpdatePurchaseOrderLinesRequest request) {
        if (entity == null || request == null) {
            return;
        }

        if (request.getProductId() != null) {
            entity.setProductId(request.getProductId());
        }
        if (request.getQuantityOrdered() != null) {
            entity.setQuantityOrdered(request.getQuantityOrdered());
        }
        if (request.getUnitPrice() != null) {
            entity.setUnitPrice(request.getUnitPrice());
        }
        if (request.getNotes() != null) {
            entity.setNotes(request.getNotes());
        }
    }

    public PurchaseOrderLinesResponse toResponse(PurchaseOrderLines entity) {
        if (entity == null) {
            return null;
        }

        return PurchaseOrderLinesResponse.builder()
                .id(entity.getId())
                .purchaseOrderId(entity.getPurchaseOrderId())
                .productId(entity.getProductId())
                .lineNumber(entity.getLineNumber())
                .quantityOrdered(defaultZero(entity.getQuantityOrdered()))
                .quantityReceived(defaultZero(entity.getQuantityReceived()))
                .unitPrice(defaultZero(entity.getUnitPrice()))
                .lineTotal(defaultZero(entity.getLineTotal()))
                .notes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
