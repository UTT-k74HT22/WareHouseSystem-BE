package org.demo.whs.mapper;

import org.demo.whs.entity.InboundReceiptLines;
import org.demo.whs.entity.Products;
import org.demo.whs.entity.dto.request.InboundReceiptLines.InboundReceiptLinesRequest;
import org.demo.whs.entity.dto.response.InboundReceiptLines.InboundReceiptLinesResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class InboundReceiptLinesMapper {

    /**
     * Convert an InboundReceiptLines entity to an InboundReceiptLinesResponse DTO.
     *
     * @param entity the InboundReceiptLines entity to convert
     * @return the corresponding InboundReceiptLinesResponse DTO
     */
    public InboundReceiptLinesResponse toResponse(InboundReceiptLines entity) {
        return toResponse(entity, null, null, null, null, null);
    }

    /**
     * Convert an InboundReceiptLines entity to an InboundReceiptLinesResponse DTO with product details.
     *
     * @param entity      the InboundReceiptLines entity to convert
     * @param productSku   the SKU of the product
     * @param productName  the name of the product
     * @return the corresponding InboundReceiptLinesResponse DTO with product details
     */
    public InboundReceiptLinesResponse toResponse(InboundReceiptLines entity, String productSku, String productName) {
        return toResponse(entity, productSku, productName, null, null, null);
    }

    /**
     * Convert an InboundReceiptLines entity to an InboundReceiptLinesResponse DTO with product and batch details.
     *
     * @param entity      the InboundReceiptLines entity to convert
     * @param productSku   the SKU of the product
     * @param productName  the name of the product
     * @param batchNumber  the batch number of the product
     * @return the corresponding InboundReceiptLinesResponse DTO with product and batch details
     */
    public InboundReceiptLinesResponse toResponse(InboundReceiptLines entity, String productSku, String productName, 
                                                  String batchNumber, String locationCode, String locationName) {
        if (entity == null) {
            return null;
        }

        return InboundReceiptLinesResponse.builder()
                .id(entity.getId())
                .inboundReceiptId(entity.getInboundReceiptId())
                .purchaseOrderLineId(entity.getPurchaseOrderLineId())
                .productId(entity.getProductId())
                .productSku(productSku)
                .productName(productName)
                .batchId(entity.getBatchId())
                .batchNumber(batchNumber)
                .locationId(entity.getLocationId())
                .locationCode(locationCode)
                .locationName(locationName)
                .lineNumber(entity.getLineNumber())
                .quantityReceived(entity.getQuantityReceived())
                .qualityStatus(entity.getQualityStatus() == null ? null : entity.getQualityStatus().name())
                .notes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /**
     * Convert a list of InboundReceiptLines entities to a list of InboundReceiptLinesResponse DTOs.
     *
     * @param entities the list of InboundReceiptLines entities to convert
     * @return the corresponding list of InboundReceiptLinesResponse DTOs
     */
    public List<InboundReceiptLinesResponse> toResponses(List<InboundReceiptLines> entities) {
        if (entities == null || entities.isEmpty()) {
            return List.of();
        }

        return entities.stream()
                .map(this::toResponse)
                .toList();
    }

    public InboundReceiptLines getInboundReceiptLines(InboundReceiptLinesRequest request, Products products, Integer nextLineNumber) {
        return InboundReceiptLines.builder()
                .inboundReceiptId(request.getInboundReceiptId())
                .purchaseOrderLineId(request.getPurchaseOrderLineId())
                .productId(products.getId())
                .locationId(request.getLocationId())
                .batchId(request.getBatchId())
                .lineNumber(nextLineNumber)
                .quantityReceived(request.getQuantityReceived())
                .qualityStatus(request.getQualityStatus())
                .notes(request.getNotes())
                .build();
    }
}
