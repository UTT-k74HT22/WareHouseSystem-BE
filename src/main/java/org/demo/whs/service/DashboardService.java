package org.demo.whs.service;

import org.demo.whs.entity.dto.response.Dashboard.DashboardResponse;

public interface DashboardService {

    DashboardResponse getDashboard(String warehouseId, int days, int activityLimit, int jobLimit);
}
