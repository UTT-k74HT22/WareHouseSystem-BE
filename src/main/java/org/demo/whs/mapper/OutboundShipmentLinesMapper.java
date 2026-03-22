package org.demo.whs.mapper;

import org.demo.whs.entity.OutboundShipmentLines;
import org.demo.whs.entity.dto.request.OutboundShipmentLines.OutboundShipmentLinesRequest;
import org.demo.whs.entity.dto.request.OutboundShipmentLines.UpdateOutboundShipmentLinesRequest;
import org.demo.whs.entity.dto.response.OutboundShipmentLines.OutboundShipmentLinesResponse;
import org.springframework.stereotype.Component;

/**
 * Mapper class for Outbound Shipment Lines.
 */
@Component
public class OutboundShipmentLinesMapper {

    public OutboundShipmentLines toEntity(OutboundShipmentLinesRequest request) {
        if (request == null) {
            return null;
        }

        return OutboundShipmentLines.builder()
                .outboundShipmentId(request.getOutboundShipmentId())
                .salesOrderLineId(request.getSalesOrderLineId())
                .productId(request.getProductId())
                .batchId(request.getBatchId())
                .locationId(request.getLocationId())
                .quantityShipped(request.getQuantityShipped())
                .notes(request.getNotes())
                .build();
    }

    public OutboundShipmentLinesResponse toResponse(OutboundShipmentLines entity) {
        if (entity == null) {
            return null;
        }

        return OutboundShipmentLinesResponse.builder()
                .id(entity.getId())
                .outboundShipmentId(entity.getOutboundShipmentId())
                .salesOrderLineId(entity.getSalesOrderLineId())
                .productId(entity.getProductId())
                .batchId(entity.getBatchId())
                .locationId(entity.getLocationId())
                .lineNumber(entity.getLineNumber())
                .quantityShipped(entity.getQuantityShipped())
                .pickedAt(entity.getPickedAt())
                .pickedBy(entity.getPickedBy())
                .notes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public void updateEntity(OutboundShipmentLines entity, UpdateOutboundShipmentLinesRequest request) {
        if (entity == null || request == null) {
            return;
        }

        if (request.getBatchId() != null) {
            entity.setBatchId(request.getBatchId());
        }
        if (request.getLocationId() != null) {
            entity.setLocationId(request.getLocationId());
        }
        if (request.getQuantityShipped() != null) {
            entity.setQuantityShipped(request.getQuantityShipped());
        }
        if (request.getNotes() != null) {
            entity.setNotes(request.getNotes());
        }
    }
}
