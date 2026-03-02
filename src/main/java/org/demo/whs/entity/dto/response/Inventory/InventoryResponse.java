package org.demo.whs.entity.dto.response.Inventory;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class InventoryResponse {
    private String id;
    private String productId;
    private String productSku;
    private String productName;
    private String warehouseId;
    private String warehouseName;
    private String locationId;
    private String locationCode;
    private String batchId;
    private String batchNumber;
    private BigDecimal onHandQuantity;
    private BigDecimal reservedQuantity;
    private BigDecimal availableQuantity;
    private Integer version;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastMovementAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
