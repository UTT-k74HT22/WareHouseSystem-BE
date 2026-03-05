package org.demo.whs.entity.dto.request.StockAdjustments;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ApproveStockAdjustmentRequest {

    @Size(max = 500, message = "Approval note must not exceed 500 characters")
    private String approvalNote;
}
