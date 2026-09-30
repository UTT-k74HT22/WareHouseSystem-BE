package org.demo.whs.entity.dto.request.OutboundShipments;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class UpdateOutboundShipmentsRequest {
    private LocalDate shipmentDate;

    private LocalTime shipmentTime;

    @Size(max = 100, message = "Carrier must not exceed 100 characters")
    private String carrier;

    @Size(max = 100, message = "Tracking number must not exceed 100 characters")
    private String trackingNumber;

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;
}
