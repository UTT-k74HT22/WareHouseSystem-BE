package org.demo.whs.entity.dto.response.StockTransfers;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Getter;
import org.demo.whs.entity.enums.StockTransfersReason;
import org.demo.whs.entity.enums.StockTransfersStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class StockTransfersResponse {
    private String id;
    private String transferNumber;
    private String productId;
    private String warehouseId;
    private String fromLocationId;
    private String toLocationId;
    private String batchId;
    private BigDecimal quantity;
    private StockTransfersReason reason;
    private String notes;
    private StockTransfersStatus status;
    private LocalDateTime completedAt;
    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;
}
