package org.demo.whs.entity.dto.response.SalesOrders;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.demo.whs.entity.dto.response.SalesOrderLines.SalesOrderLinesResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SalesOrdersResponse {
    private String id;
    private String soNumber;
    private String customerId;
    private String warehouseId;
    private LocalDate orderDate;
    private LocalDate requestedDeliveryDate;
    private String status;
    private BigDecimal subTotal;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private String currency;
    private String notes;
    private LocalDateTime confirmedAt;
    private String confirmedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<SalesOrderLinesResponse> lines;
}
