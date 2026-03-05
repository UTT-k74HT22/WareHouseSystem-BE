package org.demo.whs.mapper;

import org.demo.whs.entity.StockMovements;
import org.demo.whs.entity.dto.response.StockMovements.StockMovementsResponse;
import org.demo.whs.entity.enums.ReferenceType;
import org.demo.whs.entity.enums.StockMovementsType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Mapper for StockMovements entity to DTOs and vice versa.
 */
@Component
public class StockMovementsMapper {

    public StockMovements toEntity(
            StockMovementsType movementType,
            String productId,
            String warehouseId,
            String locationId,
            String batchId,
            BigDecimal quantityChange,
            BigDecimal quantityBefore,
            BigDecimal quantityAfter,
            ReferenceType referenceType,
            String referenceId,
            String referenceNumber,
            String notes,
            String actorId
    ) {
        StockMovements movement = StockMovements.builder()
                .movementType(movementType)
                .productId(productId)
                .warehouseId(warehouseId)
                .locationId(locationId)
                .batchId(batchId)
                .quantityChange(quantityChange)
                .quantityBefore(quantityBefore)
                .quantityAfter(quantityAfter)
                .movementDate(LocalDateTime.now())
                .referenceType(referenceType)
                .referenceId(referenceId)
                .referenceNumber(referenceNumber)
                .notes(notes)
                .build();

        movement.setCreatedBy(actorId);
        movement.setUpdatedBy(actorId);
        return movement;
    }

    public StockMovementsResponse toResponse(StockMovements entity) {
        if (entity == null) {
            return null;
        }

        return StockMovementsResponse.builder()
                .id(entity.getId())
                .movementType(entity.getMovementType())
                .productId(entity.getProductId())
                .warehouseId(entity.getWarehouseId())
                .locationId(entity.getLocationId())
                .batchId(entity.getBatchId())
                .quantityChange(entity.getQuantityChange())
                .quantityBefore(entity.getQuantityBefore())
                .quantityAfter(entity.getQuantityAfter())
                .movementDate(entity.getMovementDate())
                .referenceType(entity.getReferenceType())
                .referenceId(entity.getReferenceId())
                .referenceNumber(entity.getReferenceNumber())
                .notes(entity.getNotes())
                .createdBy(entity.getCreatedBy())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
