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
public class LocationInventoryItemResponse {

    private String productId;
    private String productSku;
    private String productName;

    private String batchId;
    private String batchNumber;

    private BigDecimal onHandQuantity;
    private BigDecimal reservedQuantity;
    private BigDecimal availableQuantity;
}