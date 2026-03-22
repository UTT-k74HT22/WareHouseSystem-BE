package org.demo.whs.entity.dto.response.OutboundShipments;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.demo.whs.entity.enums.OutboundShipmentsStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboundShipmentsResponse {
    private String id;
    private String shipmentNumber;
    private String salesOrderId;
    private String warehouseId;
    private LocalDate shipmentDate;
    private OutboundShipmentsStatus status;
    private String trackingNumber;
    private String carrier;
    private LocalDateTime shippedAt;
    private String confirmedBy;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
}
