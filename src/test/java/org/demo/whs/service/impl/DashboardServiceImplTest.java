package org.demo.whs.service.impl;

import org.demo.whs.entity.Account;
import org.demo.whs.entity.BackgroundJob;
import org.demo.whs.entity.dto.response.Dashboard.DashboardActivityResponse;
import org.demo.whs.entity.dto.response.Dashboard.DashboardAlertResponse;
import org.demo.whs.entity.dto.response.Dashboard.DashboardOverviewResponse;
import org.demo.whs.entity.dto.response.Dashboard.DashboardResponse;
import org.demo.whs.entity.dto.response.Dashboard.DashboardTrendPointResponse;
import org.demo.whs.entity.dto.response.Dashboard.DashboardWarehouseCapacityResponse;
import org.demo.whs.entity.enums.BackgroundJobStatus;
import org.demo.whs.entity.enums.BackgroundJobType;
import org.demo.whs.repository.BackgroundJobRepository;
import org.demo.whs.repository.custom.DashboardRepositoryCustom;
import org.demo.whs.security.CustomUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DashboardServiceImpl Unit Tests")
class DashboardServiceImplTest {

    @Mock
    private DashboardRepositoryCustom dashboardRepository;

    @Mock
    private BackgroundJobRepository backgroundJobRepository;

    @InjectMocks
    private DashboardServiceImpl dashboardService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateUser(String accountId) {
        Account account = Account.builder().build();
        account.setId(accountId);
        CustomUserDetails userDetails = new CustomUserDetails(account, List.of("ADMIN"), Set.of());
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private DashboardOverviewResponse createOverview(int lowStock, int expiringBatch, int nearCapacity, int failedJobs) {
        return DashboardOverviewResponse.builder()
                .totalOnHandQuantity(BigDecimal.valueOf(1000))
                .totalReservedQuantity(BigDecimal.valueOf(100))
                .totalAvailableQuantity(BigDecimal.valueOf(900))
                .lowStockSkuCount(lowStock)
                .expiringBatchCount(expiringBatch)
                .activeWarehouseCount(2)
                .nearCapacityWarehouseCount(nearCapacity)
                .pendingInboundReceipts(3)
                .pendingOutboundShipments(4)
                .openPurchaseOrders(5)
                .openSalesOrders(6)
                .runningJobs(1)
                .failedJobsToday(failedJobs)
                .build();
    }

    private BackgroundJob createJob(String id, BackgroundJobStatus status) {
        BackgroundJob job = BackgroundJob.builder()
                .jobCode("JOB-" + id)
                .jobType(BackgroundJobType.REPORT_EXPORT)
                .businessType("CURRENT_STOCK")
                .status(status)
                .currentStep("PROCESSING")
                .progressPercent(50)
                .processedRows(50L)
                .totalRows(100L)
                .createdAt(LocalDateTime.now())
                .build();
        job.setId(id);
        return job;
    }

    @Nested
    @DisplayName("getDashboard tests")
    class GetDashboardTests {

        @Test
        @DisplayName("should_ReturnCompleteDashboard_When_UserAuthenticated")
        void should_ReturnCompleteDashboard_When_UserAuthenticated() {
            authenticateUser("acc-001");

            DashboardOverviewResponse overview = createOverview(5, 2, 1, 3);
            String todayIso = LocalDate.now().toString();
            DashboardTrendPointResponse rawPoint = DashboardTrendPointResponse.builder()
                    .label(todayIso)
                    .inboundCount(10L)
                    .outboundCount(5L)
                    .build();

            List<DashboardWarehouseCapacityResponse> capacities = List.of(
                    DashboardWarehouseCapacityResponse.builder().warehouseName("WH-A").build()
            );
            List<DashboardActivityResponse> activities = List.of(
                    DashboardActivityResponse.builder().movementType("INBOUND").build()
            );
            BackgroundJob jobPending = createJob("job-1", BackgroundJobStatus.PENDING);
            BackgroundJob jobFailed = createJob("job-2", BackgroundJobStatus.FAILED);

            when(dashboardRepository.getOverview("wh-1")).thenReturn(overview);
            when(dashboardRepository.getMovementTrend(eq("wh-1"), eq(7))).thenReturn(List.of(rawPoint));
            when(dashboardRepository.getWarehouseCapacities("wh-1", 10)).thenReturn(capacities);
            when(dashboardRepository.getRecentActivities("wh-1", 10)).thenReturn(activities);
            when(backgroundJobRepository.findByRequestedByOrderByCreatedAtDesc(eq("acc-001"), any(PageRequest.class)))
                    .thenReturn(new PageImpl<>(List.of(jobPending, jobFailed)));

            // Act
            DashboardResponse result = dashboardService.getDashboard("wh-1", 7, 10, 5);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getOverview()).isEqualTo(overview);
            assertThat(result.getWarehouseCapacities()).hasSize(1);
            assertThat(result.getRecentActivities()).hasSize(1);
            assertThat(result.getRecentJobs()).hasSize(2);

            // Check recent job mapping
            assertThat(result.getRecentJobs().get(0).getId()).isEqualTo("job-1");
            assertThat(result.getRecentJobs().get(0).getCancellable()).isTrue();
            assertThat(result.getRecentJobs().get(0).getRetryable()).isFalse();

            assertThat(result.getRecentJobs().get(1).getId()).isEqualTo("job-2");
            assertThat(result.getRecentJobs().get(1).getCancellable()).isFalse();
            assertThat(result.getRecentJobs().get(1).getRetryable()).isTrue();

            // Check alerts
            assertThat(result.getAlerts()).hasSize(4);
            DashboardAlertResponse lowStockAlert = result.getAlerts().stream()
                    .filter(a -> "LOW_STOCK".equals(a.getCode()))
                    .findFirst().orElseThrow();
            assertThat(lowStockAlert.getSeverity()).isEqualTo("HIGH");
            assertThat(lowStockAlert.getValue()).isEqualTo(5);

            // Check trend points filled for 7 days
            assertThat(result.getTrend()).hasSize(7);
        }

        @Test
        @DisplayName("should_ReturnInfoAlerts_When_AllAlertCountsAreZero")
        void should_ReturnInfoAlerts_When_AllAlertCountsAreZero() {
            // Unauthenticated: SecurityContext is empty
            DashboardOverviewResponse overview = createOverview(0, 0, 0, 0);

            when(dashboardRepository.getOverview(isNull())).thenReturn(overview);
            when(dashboardRepository.getMovementTrend(isNull(), eq(7))).thenReturn(List.of());
            when(dashboardRepository.getWarehouseCapacities(isNull(), eq(10))).thenReturn(List.of());
            when(dashboardRepository.getRecentActivities(isNull(), eq(10))).thenReturn(List.of());
            when(backgroundJobRepository.findByRequestedByOrderByCreatedAtDesc(isNull(), any(PageRequest.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            // Act
            DashboardResponse result = dashboardService.getDashboard(null, 7, 10, 5);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getAlerts()).allMatch(a -> "INFO".equals(a.getSeverity()));
        }

        @Test
        @DisplayName("should_NormalizeNegativeOrZeroLimitsToOne")
        void should_NormalizeNegativeOrZeroLimitsToOne() {
            authenticateUser("acc-1");

            DashboardOverviewResponse overview = createOverview(1, 0, 0, 0);

            when(dashboardRepository.getOverview("wh-2")).thenReturn(overview);
            when(dashboardRepository.getMovementTrend("wh-2", 1)).thenReturn(List.of());
            when(dashboardRepository.getWarehouseCapacities("wh-2", 10)).thenReturn(List.of());
            when(dashboardRepository.getRecentActivities("wh-2", 1)).thenReturn(List.of());
            when(backgroundJobRepository.findByRequestedByOrderByCreatedAtDesc(eq("acc-1"), any(PageRequest.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            // Passing negative values: days = -5, activityLimit = 0, jobLimit = -2
            DashboardResponse result = dashboardService.getDashboard("wh-2", -5, 0, -2);

            assertThat(result).isNotNull();
            assertThat(result.getTrend()).hasSize(1);
            verify(dashboardRepository).getMovementTrend("wh-2", 1);
            verify(dashboardRepository).getRecentActivities("wh-2", 1);
            verify(backgroundJobRepository).findByRequestedByOrderByCreatedAtDesc(eq("acc-1"), eq(PageRequest.of(0, 1)));
        }
    }
}
