package org.demo.whs.entity.dto.response.Batch;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class BatchInventorySnapshotResponse {

    private BigDecimal totalOnHandQuantity;
    private BigDecimal totalQuarantineQuantity;
    private BigDecimal totalReservedQuantity;
    private BigDecimal totalAvailableQuantity;
    private Long warehouseCount;
    private Long locationCount;
    private List<WarehouseInventoryResponse> warehouses;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class WarehouseInventoryResponse {
        private String warehouseId;
        private String warehouseCode;
        private String warehouseName;
        private BigDecimal onHandQuantity;
        private BigDecimal quarantineQuantity;
        private BigDecimal reservedQuantity;
        private BigDecimal availableQuantity;
        private Long locationCount;
        private List<LocationInventoryResponse> locations;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class LocationInventoryResponse {
        private String locationId;
        private String locationCode;
        private String locationName;
        private BigDecimal onHandQuantity;
        private BigDecimal quarantineQuantity;
        private BigDecimal reservedQuantity;
        private BigDecimal availableQuantity;
        private LocalDateTime lastMovementAt;
    }
}
