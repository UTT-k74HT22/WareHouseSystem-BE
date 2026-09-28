package org.demo.whs.entity.dto.request.StockTransfers;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.demo.whs.entity.enums.StockTransfersReason;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class StockTransfersRequest {

    @NotBlank(message = "Product ID is required")
    private String productId;

    @NotBlank(message = "Warehouse ID is required")
    private String warehouseId;

    @NotBlank(message = "From location ID is required")
    private String fromLocationId;

    @NotBlank(message = "To location ID is required")
    private String toLocationId;

    private String batchId;

    @NotNull(message = "Transfer quantity is required")
    @DecimalMin(value = "0.01", message = "Transfer quantity must be greater than 0")
    @Digits(integer = 13, fraction = 2, message = "Transfer quantity must have up to 13 integer digits and 2 decimal places")
    private BigDecimal quantity;

    @NotNull(message = "Transfer reason is required")
    private StockTransfersReason reason;

    @Size(max = 2000, message = "Notes must not exceed 2000 characters")
    private String notes;
}
