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

    /**
     * Backward-compatibility field. The authoritative value is loaded from inventory_id.
     */
    private String productId;

    /**
     * Backward-compatibility field. The authoritative value is loaded from inventory_id.
     */
    private String warehouseId;

    /**
     * Backward-compatibility field. The authoritative value is loaded from inventory_id.
     */
    private String locationId;

    /**
     * Backward-compatibility field. The authoritative value is loaded from inventory_id.
     */
    private String batchId;

    /**
     * Backward-compatibility field. The authoritative value is loaded from inventory_id.
     */
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
