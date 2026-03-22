package org.demo.whs.entity.dto.request.OutboundShipmentLines;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class UpdateOutboundShipmentLinesRequest {
    private String batchId;
    private String locationId;
    
    @DecimalMin(value = "0.01", message = "Quantity shipped must be greater than zero")
    private BigDecimal quantityShipped;
    
    private String notes;
}
