package org.demo.whs.mapper;

import org.demo.whs.entity.StockAdjustments;
import org.demo.whs.entity.dto.request.StockAdjustments.StockAdjustmentsRequest;
import org.demo.whs.entity.dto.response.StockAdjustments.StockAdjustmentsResponse;
import org.demo.whs.entity.enums.StockAdjustmentsStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
public class StockAdjustmentsMapper {

    public StockAdjustments toEntity(
            StockAdjustmentsRequest request,
            String adjustmentNumber,
            String actorId,
            boolean requiresApproval,
            StockAdjustmentsStatus status
    ) {
        if (request == null) {
            return null;
        }

        BigDecimal adjustmentQty = request.getQuantityAfter().subtract(request.getQuantityBefore());

        StockAdjustments entity = StockAdjustments.builder()
                .adjustmentNumber(adjustmentNumber)
                .inventoryId(request.getInventoryId())
                .productId(request.getProductId())
                .warehouseId(request.getWarehouseId())
                .locationId(request.getLocationId())
                .batchId(request.getBatchId())
                .quantityBefore(request.getQuantityBefore())
                .quantityAfter(request.getQuantityAfter())
                .adjustmentQuantity(adjustmentQty)
                .reason(request.getReason())
                .status(status)
                .notes(request.getNotes())
                .requiresApproval(requiresApproval)
                .build();

        if (actorId != null && !actorId.isBlank()) {
            entity.setCreatedBy(actorId);
            entity.setUpdatedBy(actorId);
            if (!requiresApproval) {
                entity.setApprovedBy(actorId);
                entity.setApprovedAt(LocalDateTime.now());
            }
        }

        return entity;
    }

    public StockAdjustmentsResponse toResponse(StockAdjustments entity) {
        if (entity == null) {
            return null;
        }

        return StockAdjustmentsResponse.builder()
                .id(entity.getId())
                .adjustmentNumber(entity.getAdjustmentNumber())
                .inventoryId(entity.getInventoryId())
                .productId(entity.getProductId())
                .warehouseId(entity.getWarehouseId())
                .locationId(entity.getLocationId())
                .batchId(entity.getBatchId())
                .quantityBefore(entity.getQuantityBefore())
                .quantityAfter(entity.getQuantityAfter())
                .adjustmentQuantity(entity.getAdjustmentQuantity())
                .reason(entity.getReason())
                .status(entity.getStatus())
                .notes(entity.getNotes())
                .requiresApproval(entity.getRequiresApproval())
                .approvedBy(entity.getApprovedBy())
                .approvedAt(entity.getApprovedAt())
                .rejectionReason(entity.getRejectionReason())
                .createdBy(entity.getCreatedBy())
                .createdAt(entity.getCreatedAt())
                .updatedBy(entity.getUpdatedBy())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
