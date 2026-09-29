package org.demo.whs.entity.dto.request.StockAdjustments;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.demo.whs.entity.enums.StockAdjustmentsStatus;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SearchStockAdjustmentsRequest {

    private StockAdjustmentsStatus status;
    private String productId;
    private String warehouseId;
    private String inventoryId;
    private String adjustmentNumber;
    private LocalDateTime createdFrom;
    private LocalDateTime createdTo;
}
