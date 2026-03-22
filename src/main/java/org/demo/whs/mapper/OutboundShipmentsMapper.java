package org.demo.whs.mapper;

import org.demo.whs.entity.OutboundShipments;
import org.demo.whs.entity.dto.request.OutboundShipments.OutboundShipmentsRequest;
import org.demo.whs.entity.dto.request.OutboundShipments.UpdateOutboundShipmentsRequest;
import org.demo.whs.entity.dto.response.OutboundShipments.OutboundShipmentsResponse;
import org.demo.whs.entity.enums.OutboundShipmentsStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Mapper class for Outbound Shipments.
 */
@Component
public class OutboundShipmentsMapper {

    public OutboundShipments toEntity(OutboundShipmentsRequest request) {
        if (request == null) {
            return null;
        }

        return OutboundShipments.builder()
                .salesOrderId(request.getSalesOrderId())
                .warehouseId(request.getWarehouseId())
                .shipmentDate(request.getShipmentDate())
                .status(OutboundShipmentsStatus.DRAFT)
                .trackingNumber(request.getTrackingNumber())
                .carrier(request.getCarrier())
                .notes(request.getNotes())
                .build();
    }

    public OutboundShipmentsResponse toResponse(OutboundShipments entity) {
        if (entity == null) {
            return null;
        }

        return OutboundShipmentsResponse.builder()
                .id(entity.getId())
                .shipmentNumber(entity.getShipmentNumber())
                .salesOrderId(entity.getSalesOrderId())
                .warehouseId(entity.getWarehouseId())
                .shipmentDate(entity.getShipmentDate())
                .status(entity.getStatus())
                .trackingNumber(entity.getTrackingNumber())
                .carrier(entity.getCarrier())
                .shippedAt(entity.getShippedAt())
                .confirmedBy(entity.getConfirmedBy())
                .notes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedBy(entity.getUpdatedBy())
                .build();
    }

    public void updateEntity(OutboundShipments entity, UpdateOutboundShipmentsRequest request) {
        if (entity == null || request == null) {
            return;
        }

        if (request.getShipmentDate() != null) {
            entity.setShipmentDate(request.getShipmentDate());
        }
        if (request.getTrackingNumber() != null) {
            entity.setTrackingNumber(request.getTrackingNumber());
        }
        if (request.getCarrier() != null) {
            entity.setCarrier(request.getCarrier());
        }
        if (request.getNotes() != null) {
            entity.setNotes(request.getNotes());
        }
    }
}
