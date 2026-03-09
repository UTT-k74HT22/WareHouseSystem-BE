package org.demo.whs.entity.dto.request.InboundReceipts;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class InboundReceiptsFilterRequest {

    private String receiptNumber;
    private String purchaseOrderId;
    private String warehouseId;
    private String status;
    private LocalDate receiptDateFrom;
    private LocalDate receiptDateTo;
}
