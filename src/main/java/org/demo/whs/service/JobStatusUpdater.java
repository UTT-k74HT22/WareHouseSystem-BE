package org.demo.whs.service;

import org.demo.whs.entity.BackgroundJob;
import org.demo.whs.entity.enums.BackgroundJobStatus;
import org.demo.whs.entity.dto.response.FileUploadResponse;

/**
 * Service interface for updating the status of background jobs.
 * This interface defines methods for various stages of a background job's lifecycle,
 * including validation, processing, file generation, uploading, completion, failure, cancellation, and retry.
 */
public interface JobStatusUpdater {

    /**
     * Starts the validation phase of a background job.
     *
     * @param jobId the unique identifier of the background job
     * @param stepName the name of the current step in the job process
     * @param message an optional message providing additional context about the validation step
     * @return the updated BackgroundJob entity reflecting the new status
     */
    BackgroundJob startValidation(String jobId, String stepName, String message);

    /**
     * Starts the processing phase of a background job.
     *
     * @param jobId the unique identifier of the background job
     * @param stepName the name of the current step in the job process
     * @param totalRows the total number of rows to be processed, used for progress tracking
     * @param message an optional message providing additional context about the processing step
     * @return the updated BackgroundJob entity reflecting the new status
     */
    BackgroundJob startProcessing(String jobId, String stepName, Long totalRows, String message);

    /**
     * Updates the progress of a background job during processing.
     *
     * @param jobId the unique identifier of the background job
     * @param status the current status of the background job (e.g., PROCESSING, VALIDATING)
     * @param stepName the name of the current step in the job process
     * @param progressPercent the percentage of completion for the current step
     * @param processedRows the number of rows that have been processed so far
     * @param totalRows the total number of rows to be processed
     * @param message an optional message providing additional context about the progress update
     * @return the updated BackgroundJob entity reflecting the new progress and status
     */
    BackgroundJob updateProgress(String jobId, BackgroundJobStatus status, String stepName, Integer progressPercent, Long processedRows, Long totalRows, String message);

    /**
     * Marks the file generation phase of a background job.
     *
     * @param jobId the unique identifier of the background job
     * @param stepName the name of the current step in the job process
     * @param message an optional message providing additional context about the file generation step
     * @return the updated BackgroundJob entity reflecting the new status
     */
    BackgroundJob markGeneratingFile(String jobId, String stepName, String message);

    /**
     * Marks the uploading phase of a background job.
     *
     * @param jobId the unique identifier of the background job
     * @param stepName the name of the current step in the job process
     * @param message an optional message providing additional context about the uploading step
     * @return the updated BackgroundJob entity reflecting the new status
     */
    BackgroundJob markUploading(String jobId, String stepName, String message);

    /**
     * Marks the completion of a background job, including details about the uploaded file.
     *
     * @param jobId the unique identifier of the background job
     * @param uploadedFile an object containing details about the uploaded file, such as name, size, and storage key
     * @param message an optional message providing additional context about the completion step
     * @return the updated BackgroundJob entity reflecting the completed status and file details
     */
    BackgroundJob markCompleted(String jobId, FileUploadResponse uploadedFile, String message);

    /**
     * Marks a background job as failed, including error details.
     *
     * @param jobId the unique identifier of the background job
     * @param errorCode a code representing the type of error that occurred
     * @param errorMessage a detailed message describing the error
     * @param stepName the name of the current step in the job process where the failure occurred
     * @return the updated BackgroundJob entity reflecting the failed status and error details
     */
    BackgroundJob markFailed(String jobId, String errorCode, String errorMessage, String stepName);

    /**
     * Marks a background job as cancelled, including the reason for cancellation.
     *
     * @param jobId the unique identifier of the background job
     * @param reason a message providing the reason for cancellation
     * @return the updated BackgroundJob entity reflecting the cancelled status and reason
     */
    BackgroundJob markCancelled(String jobId, String reason);

    /**
     * Resets a background job for retry, including the reason for the reset.
     *
     * @param jobId the unique identifier of the background job
     * @param reason a message providing the reason for resetting the job for retry
     * @return the updated BackgroundJob entity reflecting the reset status and reason
     */
    BackgroundJob resetForRetry(String jobId, String reason);
}
