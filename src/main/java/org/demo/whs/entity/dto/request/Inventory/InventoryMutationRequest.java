package org.demo.whs.entity.dto.request.Inventory;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.demo.whs.entity.enums.ReferenceType;

import java.math.BigDecimal;

/**
 * Single request DTO for inventory on-hand mutations.
 * Used by both {@code POST /api/v1/inventories/increase} and
 * {@code POST /api/v1/inventories/decrease} — the endpoint decides
 * the direction. {@code consumeReserved} / {@code orderLineId} are
 * only meaningful for decrease and are ignored by increase.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class InventoryMutationRequest {

    @NotBlank(message = "Product ID is required")
    private String productId;

    @NotBlank(message = "Warehouse ID is required")
    private String warehouseId;

    private String locationId;

    private String batchId;

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.01", message = "Quantity must be greater than 0")
    @Digits(integer = 13, fraction = 2, message = "Quantity must have up to 13 integer digits and 2 decimal places")
    private BigDecimal quantity;

    @NotNull(message = "Reference type is required")
    private ReferenceType referenceType;

    private String referenceId;

    @NotBlank(message = "Reference number is required")
    private String referenceNumber;

    @Builder.Default
    private boolean consumeReserved = false;

    private String orderLineId;

    @Size(max = 2000, message = "Notes must not exceed 2000 characters")
    private String notes;
}
