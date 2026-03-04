package org.demo.whs.entity.dto.request.StockAdjustments;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import org.demo.whs.entity.enums.ReasonType;
import java.math.BigDecimal;

@Getter
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class StockAdjustmentsRequest {

    @NotBlank(message = "Inventory ID is required")
    private String inventoryId;

    @NotBlank(message = "Product ID is required")
    private String productId;

    @NotBlank(message = "Warehouse ID is required")
    private String warehouseId;

    private String locationId;

    private String batchId;

    @NotNull(message = "Quantity before is required")
    @DecimalMin(value = "0.00", message = "Quantity before must be greater than or equal to 0")
    private BigDecimal quantityBefore;

    @NotNull(message = "Quantity after is required")
    @DecimalMin(value = "0.00", message = "Quantity after must be greater than or equal to 0")
    private BigDecimal quantityAfter;

    @NotNull(message = "Reason is required")
    private ReasonType reason;

    @Size(max = 2000, message = "Notes must not exceed 2000 characters")
    private String notes;

    private Boolean requiresApproval;
}
