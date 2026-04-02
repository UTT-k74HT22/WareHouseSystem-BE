package org.demo.whs.entity.dto.response.Inventory;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class InventorySummaryResponse {

    private String productId;
    private String productSku;
    private String productName;

    private BigDecimal totalOnHandQuantity;
    private BigDecimal totalQuarantineQuantity;
    private BigDecimal totalReservedQuantity;

    private Long warehouseCount;
    private Long locationCount;

    public BigDecimal getTotalAvailableQuantity() {
        if (totalOnHandQuantity == null) return BigDecimal.ZERO;
        BigDecimal quarantine = totalQuarantineQuantity == null ? BigDecimal.ZERO : totalQuarantineQuantity;
        BigDecimal reserved = totalReservedQuantity == null ? BigDecimal.ZERO : totalReservedQuantity;
        return totalOnHandQuantity.subtract(quarantine).subtract(reserved);
    }
}
