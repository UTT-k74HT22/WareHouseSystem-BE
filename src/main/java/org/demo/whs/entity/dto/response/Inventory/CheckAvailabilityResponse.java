package org.demo.whs.entity.dto.response.Inventory;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Response DTO for inventory availability check.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class CheckAvailabilityResponse {

    private String productId;
    private String warehouseId;
    private String locationId;
    private BigDecimal requestedQuantity;
    private BigDecimal availableQuantity;
    private boolean isAvailable;
    private String message;
}
