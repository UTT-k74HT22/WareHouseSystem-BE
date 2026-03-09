package org.demo.whs.entity.dto.request.PurchaseOrderLines;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class UpdatePurchaseOrderLinesRequest {

    private String productId;

    @DecimalMin(value = "0.01", message = "Quantity ordered must be greater than zero")
    private BigDecimal quantityOrdered;

    @DecimalMin(value = "0.00", message = "Unit price must be greater than or equal to zero")
    private BigDecimal unitPrice;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;
}
