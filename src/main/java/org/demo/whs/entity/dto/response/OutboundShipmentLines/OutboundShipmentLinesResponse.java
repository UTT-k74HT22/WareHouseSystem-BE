package org.demo.whs.entity.dto.response.OutboundShipmentLines;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboundShipmentLinesResponse {
    private String id;
    private String outboundShipmentId;
    private String salesOrderLineId;
    private String productId;
    private String sku;
    private String productName;
    private String batchId;
    private String batchNumber;
    private String locationId;
    private String locationName;
    private Integer lineNumber;
    private BigDecimal quantityShipped;
    private LocalDateTime pickedAt;
    private String pickedBy;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
