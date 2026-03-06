package org.demo.whs.entity.dto.response.Inventory;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class InventoryByLocationResponse {
    private String locationId;
    private String locationCode;
    private String locationName;
    private String warehouseId;
    private String warehouseName;
    private List<LocationInventoryItem> items;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class LocationInventoryItem {
        private String productId;
        private String productSku;
        private String productName;
        private String batchId;
        private String batchNumber;
        private BigDecimal onHandQuantity;
        private BigDecimal reservedQuantity;
        private BigDecimal availableQuantity;

    }
}
