package org.demo.whs.entity.dto.response.Dashboard;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class DashboardResponse {

    private DashboardOverviewResponse overview;
    private List<DashboardAlertResponse> alerts;
    private List<DashboardTrendPointResponse> trend;
    private List<DashboardWarehouseCapacityResponse> warehouseCapacities;
    private List<DashboardActivityResponse> recentActivities;
    private List<DashboardJobResponse> recentJobs;
    private LocalDateTime generatedAt;
}
