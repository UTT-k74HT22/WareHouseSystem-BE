package org.demo.whs.mapper;

import org.demo.whs.entity.StockTransfers;
import org.demo.whs.entity.dto.request.StockTransfers.StockTransfersRequest;
import org.demo.whs.entity.dto.response.StockTransfers.StockTransfersResponse;
import org.demo.whs.entity.enums.StockTransfersStatus;
import org.springframework.stereotype.Component;

@Component
public class StockTransfersMapper {

    public StockTransfers toEntity(
            StockTransfersRequest request,
            String transferNumber,
            String actorId
    ) {
        StockTransfers entity = StockTransfers.builder()
                .transferNumber(transferNumber)
                .productId(request.getProductId())
                .warehouseId(request.getWarehouseId())
                .fromLocationId(request.getFromLocationId())
                .toLocationId(request.getToLocationId())
                .batchId(request.getBatchId())
                .quantity(request.getQuantity())
                .reason(request.getReason())
                .notes(request.getNotes())
                .status(StockTransfersStatus.DRAFT)
                .build();

        entity.setCreatedBy(actorId);
        entity.setUpdatedBy(actorId);
        return entity;
    }

    public StockTransfersResponse toResponse(StockTransfers entity) {
        if (entity == null) {
            return null;
        }

        return StockTransfersResponse.builder()
                .id(entity.getId())
                .transferNumber(entity.getTransferNumber())
                .productId(entity.getProductId())
                .warehouseId(entity.getWarehouseId())
                .fromLocationId(entity.getFromLocationId())
                .toLocationId(entity.getToLocationId())
                .batchId(entity.getBatchId())
                .quantity(entity.getQuantity())
                .reason(entity.getReason())
                .notes(entity.getNotes())
                .status(entity.getStatus())
                .completedAt(entity.getCompletedAt())
                .createdBy(entity.getCreatedBy())
                .createdAt(entity.getCreatedAt())
                .updatedBy(entity.getUpdatedBy())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
