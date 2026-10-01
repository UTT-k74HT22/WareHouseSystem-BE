package org.demo.whs.entity.dto.response.Inventory;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Aggregate stock of one product in one warehouse.
 * One row per (warehouse, product) that has inventory records.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class InventoryByProductResponse {

    private String warehouseId;

    private String warehouseName;

    private String productId;

    private String productSku;

    private String productName;

    private BigDecimal totalOnHandQuantity;

    private BigDecimal totalQuarantineQuantity;

    private BigDecimal totalReservedQuantity;

    private Long locationCount;

    public BigDecimal getTotalAvailableQuantity() {
        if (totalOnHandQuantity == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal quarantine = totalQuarantineQuantity == null ? BigDecimal.ZERO : totalQuarantineQuantity;
        BigDecimal reserved = totalReservedQuantity == null ? BigDecimal.ZERO : totalReservedQuantity;
        return totalOnHandQuantity.subtract(quarantine).subtract(reserved);
    }
}
