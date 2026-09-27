package org.demo.whs.entity.dto.response.OutboundShipments;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.demo.whs.entity.dto.response.OutboundShipmentLines.OutboundShipmentLinesResponse;
import org.demo.whs.entity.enums.OutboundShipmentsStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
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
    private List<OutboundShipmentLinesResponse> lines;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
}
