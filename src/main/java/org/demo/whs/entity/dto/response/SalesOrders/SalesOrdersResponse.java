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
    private String customerName;
    private String warehouseId;
    private String warehouseName;
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
    private String confirmedByName;
    private LocalDateTime createdAt;
    private String createdBy;
    private String createdByName;
    private LocalDateTime updatedAt;
    private String updatedBy;
    private String updatedByName;
    private List<SalesOrderLinesResponse> lines;
}
