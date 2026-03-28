package org.demo.whs.service.impl;

import lombok.RequiredArgsConstructor;
import org.demo.whs.entity.BackgroundJob;
import org.demo.whs.entity.dto.response.Dashboard.DashboardAlertResponse;
import org.demo.whs.entity.dto.response.Dashboard.DashboardJobResponse;
import org.demo.whs.entity.dto.response.Dashboard.DashboardOverviewResponse;
import org.demo.whs.entity.dto.response.Dashboard.DashboardResponse;
import org.demo.whs.entity.dto.response.Dashboard.DashboardTrendPointResponse;
import org.demo.whs.entity.enums.BackgroundJobStatus;
import org.demo.whs.repository.BackgroundJobRepository;
import org.demo.whs.repository.custom.DashboardRepositoryCustom;
import org.demo.whs.security.SecurityUtils;
import org.demo.whs.service.DashboardService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private static final DateTimeFormatter TREND_LABEL_FORMATTER = DateTimeFormatter.ofPattern("dd/MM");
    private static final Set<BackgroundJobStatus> CANCELLABLE_STATUSES = Set.of(
            BackgroundJobStatus.PENDING,
            BackgroundJobStatus.VALIDATING,
            BackgroundJobStatus.PROCESSING,
            BackgroundJobStatus.GENERATING_FILE
    );

    private final DashboardRepositoryCustom dashboardRepository;
    private final BackgroundJobRepository backgroundJobRepository;

    @Override
    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(String warehouseId, int days, int activityLimit, int jobLimit) {
        int normalizedDays = Math.max(days, 1);
        int normalizedActivityLimit = Math.max(activityLimit, 1);
        int normalizedJobLimit = Math.max(jobLimit, 1);
        DashboardOverviewResponse overview = dashboardRepository.getOverview(warehouseId);

        return DashboardResponse.builder()
                .overview(overview)
                .alerts(buildAlerts(overview))
                .trend(fillMissingTrendPoints(dashboardRepository.getMovementTrend(warehouseId, normalizedDays), normalizedDays))
                .warehouseCapacities(dashboardRepository.getWarehouseCapacities(warehouseId, 10))
                .recentActivities(dashboardRepository.getRecentActivities(warehouseId, normalizedActivityLimit))
                .recentJobs(loadRecentJobs(normalizedJobLimit))
                .generatedAt(LocalDateTime.now())
                .build();
    }

    private List<DashboardJobResponse> loadRecentJobs(int jobLimit) {
        String currentAccountId = SecurityUtils.getCurrentAccountId();
        return backgroundJobRepository.findByRequestedByOrderByCreatedAtDesc(currentAccountId, PageRequest.of(0, jobLimit))
                .getContent()
                .stream()
                .map(this::toDashboardJobResponse)
                .toList();
    }

    private DashboardJobResponse toDashboardJobResponse(BackgroundJob job) {
        return DashboardJobResponse.builder()
                .id(job.getId())
                .jobCode(job.getJobCode())
                .jobType(job.getJobType())
                .businessType(job.getBusinessType())
                .status(job.getStatus())
                .currentStep(job.getCurrentStep())
                .progressPercent(job.getProgressPercent())
                .processedRows(job.getProcessedRows())
                .totalRows(job.getTotalRows())
                .resultFileName(job.getResultFileName())
                .errorCode(job.getErrorCode())
                .createdAt(job.getCreatedAt())
                .finishedAt(job.getFinishedAt())
                .cancellable(CANCELLABLE_STATUSES.contains(job.getStatus()))
                .retryable(job.getStatus() == BackgroundJobStatus.FAILED || job.getStatus() == BackgroundJobStatus.CANCELLED)
                .build();
    }

    private List<DashboardAlertResponse> buildAlerts(DashboardOverviewResponse overview) {
        List<DashboardAlertResponse> alerts = new ArrayList<>();

        alerts.add(DashboardAlertResponse.builder()
                .code("LOW_STOCK")
                .severity(overview.getLowStockSkuCount() > 0 ? "HIGH" : "INFO")
                .title("SKU dưới mức reorder")
                .description("Sản phẩm cần bổ sung hoặc theo dõi sát.")
                .value(overview.getLowStockSkuCount())
                .build());

        alerts.add(DashboardAlertResponse.builder()
                .code("EXPIRING_BATCH")
                .severity(overview.getExpiringBatchCount() > 0 ? "MEDIUM" : "INFO")
                .title("Lô sắp hết hạn trong 7 ngày")
                .description("Cần ưu tiên xuất hoặc xử lý tồn gần hạn.")
                .value(overview.getExpiringBatchCount())
                .build());

        alerts.add(DashboardAlertResponse.builder()
                .code("WAREHOUSE_CAPACITY")
                .severity(overview.getNearCapacityWarehouseCount() > 0 ? "MEDIUM" : "INFO")
                .title("Kho gần đầy")
                .description("Kho có tỷ lệ vị trí sử dụng từ 80% trở lên.")
                .value(overview.getNearCapacityWarehouseCount())
                .build());

        alerts.add(DashboardAlertResponse.builder()
                .code("FAILED_JOBS")
                .severity(overview.getFailedJobsToday() > 0 ? "HIGH" : "INFO")
                .title("Background jobs lỗi hôm nay")
                .description("Theo dõi các job export/import cần retry.")
                .value(overview.getFailedJobsToday())
                .build());

        return alerts;
    }

    private List<DashboardTrendPointResponse> fillMissingTrendPoints(List<DashboardTrendPointResponse> rawPoints, int days) {
        Map<LocalDate, DashboardTrendPointResponse> pointMap = new LinkedHashMap<>();

        for (DashboardTrendPointResponse rawPoint : rawPoints) {
            LocalDate pointDate = LocalDate.parse(rawPoint.getLabel());
            pointMap.put(pointDate, DashboardTrendPointResponse.builder()
                    .label(pointDate.format(TREND_LABEL_FORMATTER))
                    .inboundCount(rawPoint.getInboundCount())
                    .outboundCount(rawPoint.getOutboundCount())
                    .build());
        }

        List<DashboardTrendPointResponse> normalized = new ArrayList<>();
        LocalDate startDate = LocalDate.now().minusDays(Math.max(days - 1L, 0L));

        for (int i = 0; i < days; i++) {
            LocalDate date = startDate.plusDays(i);
            normalized.add(pointMap.getOrDefault(date, DashboardTrendPointResponse.builder()
                    .label(date.format(TREND_LABEL_FORMATTER))
                    .inboundCount(0L)
                    .outboundCount(0L)
                    .build()));
        }

        return normalized;
    }
}
