package org.demo.whs.mapper;

import org.demo.whs.entity.InboundReceiptLines;
import org.demo.whs.entity.dto.response.InboundReceiptLines.InboundReceiptLinesResponse;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Mapper class for Inbound Receipt Lines.
 */
@Component
public class InboundReceiptLinesMapper {

    public InboundReceiptLinesResponse toResponse(InboundReceiptLines entity) {
        if (entity == null) {
            return null;
        }

        return InboundReceiptLinesResponse.builder()
                .id(entity.getId())
                .inboundReceiptId(entity.getInboundReceiptId())
                .purchaseOrderLineId(entity.getPurchaseOrderLineId())
                .productId(entity.getProductId())
                .batchId(entity.getBatchId())
                .locationId(entity.getLocationId())
                .lineNumber(entity.getLineNumber())
                .quantityReceived(entity.getQuantityReceived())
                .qualityStatus(entity.getQualityStatus() == null ? null : entity.getQualityStatus().name())
                .notes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public List<InboundReceiptLinesResponse> toResponses(List<InboundReceiptLines> entities) {
        if (entities == null || entities.isEmpty()) {
            return List.of();
        }

        return entities.stream()
                .map(this::toResponse)
                .toList();
    }
}
