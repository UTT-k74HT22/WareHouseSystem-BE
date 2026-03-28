package org.demo.whs.entity.dto.response.Dashboard;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class DashboardOverviewResponse {

    private BigDecimal totalOnHandQuantity;
    private BigDecimal totalReservedQuantity;
    private BigDecimal totalAvailableQuantity;
    private Integer lowStockSkuCount;
    private Integer expiringBatchCount;
    private Integer activeWarehouseCount;
    private Integer nearCapacityWarehouseCount;
    private Integer pendingInboundReceipts;
    private Integer pendingOutboundShipments;
    private Integer openPurchaseOrders;
    private Integer openSalesOrders;
    private Integer runningJobs;
    private Integer failedJobsToday;
}
