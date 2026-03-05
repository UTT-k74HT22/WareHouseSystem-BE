package org.demo.whs.entity.dto.response.StockMovements;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Getter;
import org.demo.whs.entity.enums.ReferenceType;
import org.demo.whs.entity.enums.StockMovementsType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class StockMovementsResponse {
    private String id;
    private StockMovementsType movementType;
    private String productId;
    private String warehouseId;
    private String locationId;
    private String batchId;
    private BigDecimal quantityChange;
    private BigDecimal quantityBefore;
    private BigDecimal quantityAfter;
    private LocalDateTime movementDate;
    private ReferenceType referenceType;
    private String referenceId;
    private String referenceNumber;
    private String notes;
    private String createdBy;
    private LocalDateTime createdAt;
}
