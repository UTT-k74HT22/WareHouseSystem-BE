package org.demo.whs.service;

import org.demo.whs.entity.dto.request.BackgroundJob.BackgroundJobFilterRequest;
import org.demo.whs.entity.dto.request.BackgroundJob.CancelBackgroundJobRequest;
import org.demo.whs.entity.dto.request.BackgroundJob.RetryBackgroundJobRequest;
import org.demo.whs.entity.dto.response.BackgroundJob.BackgroundJobDetailResponse;
import org.demo.whs.entity.dto.response.BackgroundJob.BackgroundJobFileResponse;
import org.demo.whs.entity.dto.response.BackgroundJob.BackgroundJobStatusResponse;
import org.demo.whs.entity.dto.response.BackgroundJob.BackgroundJobSummaryResponse;
import org.demo.whs.entity.dto.response.PageResponse;

/**
 * Service interface for background job tracking APIs.
 */
public interface BackgroundJobService {

    /**
     * List current user's background jobs with pagination and filtering.
     *
     * @param requestedBy the account ID of the user requesting the jobs
     * @param request     the background job filter request containing pagination and filter criteria
     * @return a paginated list of background job summaries
     */
    PageResponse<BackgroundJobSummaryResponse> getMyJobs(String requestedBy, BackgroundJobFilterRequest request);

    /**
     * Get background job detail by job ID.
     *
     * @param jobId       the ID of the background job
     * @param requestedBy the account ID of the user requesting the job detail
     * @return the background job detail
     */
    BackgroundJobDetailResponse getJobDetail(String jobId, String requestedBy);

    /**
     * Get background job status by job ID.
     *
     * @param jobId       the ID of the background job
     * @param requestedBy the account ID of the user requesting the job status
     * @return the background job status
     */
    BackgroundJobStatusResponse getJobStatus(String jobId, String requestedBy);

    /**
     * Retry a background job by job ID.
     *
     * @param jobId       the ID of the background job
     * @param requestedBy the account ID of the user requesting the retry
     * @param request     the retry background job request containing any necessary parameters for retrying
     * @return the background job status after retrying
     */
    BackgroundJobStatusResponse retryJob(String jobId, String requestedBy, RetryBackgroundJobRequest request);

    /**
     * Cancel a background job by job ID.
     *
     * @param jobId       the ID of the background job
     * @param requestedBy the account ID of the user requesting the cancellation
     * @param request     the cancel background job request containing any necessary parameters for cancellation
     * @return the background job status after cancellation
     */
    BackgroundJobStatusResponse cancelJob(String jobId, String requestedBy, CancelBackgroundJobRequest request);

    /**
     * Get background job result file download link by job ID.
     *
     * @param jobId       the ID of the background job
     * @param requestedBy the account ID of the user requesting the download
     * @return the background job file response containing the download link and file information
     */
    BackgroundJobFileResponse getJobDownload(String jobId, String requestedBy);
}
