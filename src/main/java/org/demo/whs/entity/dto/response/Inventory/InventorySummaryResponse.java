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
    private BigDecimal totalReservedQuantity;
    private BigDecimal totalAvailableQuantity;
    private Long warehouseCount;
    private Long locationCount;

    public InventorySummaryResponse(String productId, BigDecimal totalOnHandQuantity, BigDecimal totalReservedQuantity, Long warehouseCount, Long locationCount) {
        this.productId = productId;
        this.totalOnHandQuantity = totalOnHandQuantity != null ? totalOnHandQuantity : BigDecimal.ZERO;
        this.totalReservedQuantity = totalReservedQuantity != null ? totalReservedQuantity : BigDecimal.ZERO;
        this.totalAvailableQuantity = this.totalOnHandQuantity.subtract(this.totalReservedQuantity);
        this.warehouseCount = warehouseCount != null ? warehouseCount : 0L;
        this.locationCount = locationCount != null ? locationCount : 0L;
    }

    public InventorySummaryResponse(String productId, String productSku, String productName, Object totalOnHandQuantity, Object totalReservedQuantity, Object warehouseCount, Object locationCount) {
        this.productId = productId;
        this.productSku = productSku;
        this.productName = productName;
        this.totalOnHandQuantity = totalOnHandQuantity instanceof BigDecimal ? (BigDecimal) totalOnHandQuantity :
                (totalOnHandQuantity != null ? new BigDecimal(totalOnHandQuantity.toString()) : BigDecimal.ZERO);
        this.totalReservedQuantity = totalReservedQuantity instanceof BigDecimal ? (BigDecimal) totalReservedQuantity :
                (totalReservedQuantity != null ? new BigDecimal(totalReservedQuantity.toString()) : BigDecimal.ZERO);
        this.totalAvailableQuantity = this.totalOnHandQuantity.subtract(this.totalReservedQuantity);
        this.warehouseCount = warehouseCount instanceof Long ? (Long) warehouseCount :
                (warehouseCount != null ? Long.valueOf(warehouseCount.toString()) : 0L);
        this.locationCount = locationCount instanceof Long ? (Long) locationCount :
                (locationCount != null ? Long.valueOf(locationCount.toString()) : 0L);
    }
}
