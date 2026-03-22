package org.demo.whs.entity.dto.request.OutboundShipments;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class OutboundShipmentsRequest {
    @NotBlank(message = "Sales order ID is required")
    private String salesOrderId;

    @NotBlank(message = "Warehouse ID is required")
    private String warehouseId;

    @NotNull(message = "Shipment date is required")
    private LocalDate shipmentDate;

    private String trackingNumber;
    private String carrier;
    private String notes;
}
