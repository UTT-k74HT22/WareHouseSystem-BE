package org.demo.whs.repository.custom;

import org.demo.whs.entity.dto.response.Dashboard.DashboardActivityResponse;
import org.demo.whs.entity.dto.response.Dashboard.DashboardOverviewResponse;
import org.demo.whs.entity.dto.response.Dashboard.DashboardTrendPointResponse;
import org.demo.whs.entity.dto.response.Dashboard.DashboardWarehouseCapacityResponse;

import java.util.List;

public interface DashboardRepositoryCustom {

    DashboardOverviewResponse getOverview(String warehouseId);

    List<DashboardTrendPointResponse> getMovementTrend(String warehouseId, int days);

    List<DashboardWarehouseCapacityResponse> getWarehouseCapacities(String warehouseId, int limit);

    List<DashboardActivityResponse> getRecentActivities(String warehouseId, int limit);
}
