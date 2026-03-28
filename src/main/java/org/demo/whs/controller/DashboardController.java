package org.demo.whs.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.demo.whs.entity.dto.response.BaseResponse;
import org.demo.whs.entity.dto.response.Dashboard.DashboardResponse;
import org.demo.whs.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/dashboard")
@PreAuthorize("isAuthenticated()")
@Tag(name = "Dashboard", description = "Operational dashboard endpoints")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    @Operation(summary = "Get dashboard snapshot")
    public ResponseEntity<BaseResponse<DashboardResponse>> getDashboard(
            @RequestParam(required = false) String warehouseId,
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "5") int activityLimit,
            @RequestParam(defaultValue = "5") int jobLimit) {
        DashboardResponse response = dashboardService.getDashboard(warehouseId, days, activityLimit, jobLimit);
        return ResponseEntity.ok(BaseResponse.success(response, "Dashboard data retrieved successfully"));
    }
}
