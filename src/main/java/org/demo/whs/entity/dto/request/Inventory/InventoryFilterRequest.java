package org.demo.whs.entity.dto.request.Inventory;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class InventoryFilterRequest {

    private String productId;
    private String productSku;
    private String productName;
    private String warehouseId;
    private String locationId;
    private String batchId;
    private String batchNumber;

}
