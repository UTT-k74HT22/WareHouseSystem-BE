package org.demo.whs.entity.dto.request.Inventory;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for reserving inventory.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class InventoryReserveRequest {
    @NotBlank(message = "Warehouse ID is required")
    private String warehouseId;

    @NotBlank(message = "Product ID is required")
    private String productId;

    private String locationId;

    private String batchId;

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.01", message = "Quantity must be greater than zero")
    @Digits(integer = 13, fraction = 2, message = "Quantity must have up to 13 integer digits and 2 decimal places")
    private BigDecimal quantity;

    @NotBlank(message = "OrderLineId is required")
    private String orderLineId;
}
