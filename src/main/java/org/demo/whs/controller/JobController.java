package org.demo.whs.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.dto.request.BackgroundJob.BackgroundJobFilterRequest;
import org.demo.whs.entity.dto.request.BackgroundJob.CancelBackgroundJobRequest;
import org.demo.whs.entity.dto.request.BackgroundJob.RetryBackgroundJobRequest;
import org.demo.whs.entity.dto.response.BackgroundJob.BackgroundJobDetailResponse;
import org.demo.whs.entity.dto.response.BackgroundJob.BackgroundJobFileResponse;
import org.demo.whs.entity.dto.response.BackgroundJob.BackgroundJobStatusResponse;
import org.demo.whs.entity.dto.response.BackgroundJob.BackgroundJobSummaryResponse;
import org.demo.whs.entity.dto.response.BaseResponse;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.security.SecurityUtils;
import org.demo.whs.service.BackgroundJobService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Core API controller for background job tracking endpoints.
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@RequestMapping("/api/v1/jobs")
@Tag(name = "Background Jobs", description = "Background job tracking endpoints")
public class JobController {

    private final BackgroundJobService backgroundJobService;

    /**
     * List current user's background jobs with pagination and filtering.
     *
     * @param request the background job filter request containing pagination and filter criteria
     * @return a paginated list of background job summaries
     */
    @GetMapping("/my")
    @Operation(summary = "List current user's background jobs")
    public ResponseEntity<BaseResponse<PageResponse<BackgroundJobSummaryResponse>>> getMyJobs(
            @Valid @ModelAttribute BackgroundJobFilterRequest request) {
        String currentAccountId = SecurityUtils.getCurrentAccountId();
        log.info("Get my jobs requested by accountId={}", currentAccountId);

        PageResponse<BackgroundJobSummaryResponse> response = backgroundJobService.getMyJobs(currentAccountId, request);
        return ResponseEntity.ok(BaseResponse.success(response, "Background jobs retrieved successfully"));
    }

    /**
     * Get background job detail by job ID.
     *
     * @param jobId the ID of the background job
     * @return the background job detail
     */
    @GetMapping("/{jobId}")
    @Operation(summary = "Get background job detail")
    public ResponseEntity<BaseResponse<BackgroundJobDetailResponse>> getJobDetail(@PathVariable String jobId) {
        String currentAccountId = SecurityUtils.getCurrentAccountId();
        log.info("Get background job detail id={} requested by accountId={}", jobId, currentAccountId);

        BackgroundJobDetailResponse response = backgroundJobService.getJobDetail(jobId, currentAccountId);
        return ResponseEntity.ok(BaseResponse.success(response, "Background job detail retrieved successfully"));
    }

    /**
     * Get background job status by job ID.
     *
     * @param jobId the ID of the background job
     * @return the background job status
     */
    @GetMapping("/{jobId}/status")
    @Operation(summary = "Get background job status")
    public ResponseEntity<BaseResponse<BackgroundJobStatusResponse>> getJobStatus(@PathVariable String jobId) {
        String currentAccountId = SecurityUtils.getCurrentAccountId();
        log.info("Get background job status id={} requested by accountId={}", jobId, currentAccountId);

        BackgroundJobStatusResponse response = backgroundJobService.getJobStatus(jobId, currentAccountId);
        return ResponseEntity.ok(BaseResponse.success(response, "Background job status retrieved successfully"));
    }

    /**
     * Retry a background job by job ID.
     *
     * @param jobId   the ID of the background job
     * @param request the retry background job request containing any necessary parameters for retrying
     * @return the background job status after retrying
     */
    @PostMapping("/{jobId}/retry")
    @Operation(summary = "Retry a background job")
    public ResponseEntity<BaseResponse<BackgroundJobStatusResponse>> retryJob(
            @PathVariable String jobId,
            @Valid @RequestBody(required = false) RetryBackgroundJobRequest request) {
        String currentAccountId = SecurityUtils.getCurrentAccountId();
        log.info("Retry background job id={} requested by accountId={}", jobId, currentAccountId);

        BackgroundJobStatusResponse response = backgroundJobService.retryJob(jobId, currentAccountId, request);
        return ResponseEntity.ok(BaseResponse.success(response, "Background job retry flow is scaffolded"));
    }

    /**
     * Cancel a background job by job ID.
     *
     * @param jobId   the ID of the background job
     * @param request the cancel background job request containing any necessary parameters for canceling
     * @return the background job status after canceling
     */
    @PostMapping("/{jobId}/cancel")
    @Operation(summary = "Cancel a background job")
    public ResponseEntity<BaseResponse<BackgroundJobStatusResponse>> cancelJob(
            @PathVariable String jobId,
            @Valid @RequestBody(required = false) CancelBackgroundJobRequest request) {
        String currentAccountId = SecurityUtils.getCurrentAccountId();
        log.info("Cancel background job id={} requested by accountId={}", jobId, currentAccountId);

        BackgroundJobStatusResponse response = backgroundJobService.cancelJob(jobId, currentAccountId, request);
        return ResponseEntity.ok(BaseResponse.success(response, "Background job cancel flow is scaffolded"));
    }

    /**
     * Get background job download link by job ID.
     *
     * @param jobId the ID of the background job
     * @return the background job file response containing the download link and file details
     */
    @GetMapping("/{jobId}/download")
    @Operation(summary = "Get background job download link")
    public ResponseEntity<BaseResponse<BackgroundJobFileResponse>> getJobDownload(@PathVariable String jobId) {
        String currentAccountId = SecurityUtils.getCurrentAccountId();
        log.info("Get background job download id={} requested by accountId={}", jobId, currentAccountId);

        BackgroundJobFileResponse response = backgroundJobService.getJobDownload(jobId, currentAccountId);
        return ResponseEntity.ok(BaseResponse.success(response, "Background job download link generated successfully"));
    }
}
