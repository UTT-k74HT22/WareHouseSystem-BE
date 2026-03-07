package org.demo.whs.entity.dto.response.StockAdjustments;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Getter;
import org.demo.whs.entity.enums.ReasonType;
import org.demo.whs.entity.enums.StockAdjustmentsStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class StockAdjustmentsResponse {
    private String id;
    private String adjustmentNumber;
    private String inventoryId;
    private String productId;
    private String warehouseId;
    private String locationId;
    private String batchId;
    private BigDecimal quantityBefore;
    private BigDecimal quantityAfter;
    private BigDecimal adjustmentQuantity;
    private ReasonType reason;
    private StockAdjustmentsStatus status;
    private String notes;
    private Boolean requiresApproval;
    private LocalDateTime approvedAt;
    private String rejectionReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
