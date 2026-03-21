package org.demo.whs.entity.dto.response.SalesOrderLines;

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
public class SalesOrderLinesResponse {
    private String id;
    private String salesOrderId;
    private String productId;
    private Integer lineNumber;
    private BigDecimal quantityOrdered;
    private BigDecimal quantityShipped;
    private BigDecimal unitPrice;
    private BigDecimal lineTotal;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
