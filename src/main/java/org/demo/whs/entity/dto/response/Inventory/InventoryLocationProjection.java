package org.demo.whs.entity.dto.response.Inventory;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryLocationProjection {

    private String locationId;
    private String locationCode;
    private String locationName;

    private String warehouseId;
    private String warehouseName;

    private String productId;
    private String productSku;
    private String productName;

    private String batchId;
    private String batchNumber;

    private BigDecimal onHandQuantity;
    private BigDecimal reservedQuantity;
}
