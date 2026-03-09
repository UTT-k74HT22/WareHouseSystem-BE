package org.demo.whs.entity.dto.response.InboundReceiptLines;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class InboundReceiptLinesResponse {

    private String id;
    private String inboundReceiptId;
    private String purchaseOrderLineId;
    private String productId;
    private String productSku;
    private String productName;
    private String batchId;
    private String batchNumber;
    private String locationId;
    private String locationCode;
    private String locationName;
    private Integer lineNumber;
    private BigDecimal quantityReceived;
    private String qualityStatus;
    private String notes;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
