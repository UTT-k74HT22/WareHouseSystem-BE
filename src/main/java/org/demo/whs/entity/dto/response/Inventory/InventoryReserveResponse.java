package org.demo.whs.entity.dto.response.Inventory;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for inventory reservation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class InventoryReserveResponse {
    private String inventoryId;
    private String productId;
    private String warehouseId;
    private String locationId;
    private String batchId;
    private BigDecimal reservedQuantity;
    private BigDecimal onHandQuantity;
    private BigDecimal availableQuantity;
    private String orderLineId;
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime reservedAt;
}
