package org.demo.whs.mapper;

import org.demo.whs.entity.BackgroundJob;
import org.demo.whs.entity.BackgroundJobStepLog;
import org.demo.whs.entity.dto.response.BackgroundJob.BackgroundJobDetailResponse;
import org.demo.whs.entity.dto.response.BackgroundJob.BackgroundJobFileResponse;
import org.demo.whs.entity.dto.response.BackgroundJob.BackgroundJobStatusResponse;
import org.demo.whs.entity.dto.response.BackgroundJob.BackgroundJobStepLogResponse;
import org.demo.whs.entity.dto.response.BackgroundJob.BackgroundJobSummaryResponse;
import org.demo.whs.entity.enums.BackgroundJobStatus;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * Mapper for converting background job entities to response DTOs.
 */
@Component
public class BackgroundJobMapper {

    public BackgroundJobSummaryResponse toSummaryResponse(BackgroundJob job) {
        if (job == null) {
            return null;
        }

        return BackgroundJobSummaryResponse.builder()
                .id(job.getId())
                .jobCode(job.getJobCode())
                .jobType(job.getJobType())
                .businessType(job.getBusinessType())
                .status(job.getStatus())
                .currentStep(job.getCurrentStep())
                .progressPercent(job.getProgressPercent())
                .processedRows(job.getProcessedRows())
                .totalRows(job.getTotalRows())
                .requestedBy(job.getRequestedBy())
                .resultFileName(job.getResultFileName())
                .errorCode(job.getErrorCode())
                .createdAt(job.getCreatedAt())
                .startedAt(job.getStartedAt())
                .finishedAt(job.getFinishedAt())
                .cancellable(isCancellable(job.getStatus()))
                .retryable(isRetryable(job.getStatus()))
                .build();
    }

    public BackgroundJobStatusResponse toStatusResponse(BackgroundJob job, String downloadUrl, Instant downloadUrlExpiresAt) {
        if (job == null) {
            return null;
        }

        BackgroundJobFileResponse file = toFileResponse(job, downloadUrl, downloadUrlExpiresAt);

        return BackgroundJobStatusResponse.builder()
                .id(job.getId())
                .jobCode(job.getJobCode())
                .status(job.getStatus())
                .currentStep(job.getCurrentStep())
                .progressPercent(job.getProgressPercent())
                .processedRows(job.getProcessedRows())
                .totalRows(job.getTotalRows())
                .errorCode(job.getErrorCode())
                .errorMessage(job.getErrorMessage())
                .startedAt(job.getStartedAt())
                .finishedAt(job.getFinishedAt())
                .cancellable(isCancellable(job.getStatus()))
                .retryable(isRetryable(job.getStatus()))
                .downloadAvailable(file != null)
                .file(file)
                .build();
    }

    public BackgroundJobDetailResponse toDetailResponse(
            BackgroundJob job,
            List<BackgroundJobStepLog> stepLogs,
            String downloadUrl,
            Instant downloadUrlExpiresAt
    ) {
        if (job == null) {
            return null;
        }

        List<BackgroundJobStepLogResponse> mappedStepLogs = stepLogs == null
                ? Collections.emptyList()
                : stepLogs.stream().map(this::toStepLogResponse).toList();

        return BackgroundJobDetailResponse.builder()
                .id(job.getId())
                .jobCode(job.getJobCode())
                .jobType(job.getJobType())
                .businessType(job.getBusinessType())
                .status(job.getStatus())
                .currentStep(job.getCurrentStep())
                .requestedBy(job.getRequestedBy())
                .requestPayload(job.getRequestPayload())
                .requestHash(job.getRequestHash())
                .progressPercent(job.getProgressPercent())
                .processedRows(job.getProcessedRows())
                .totalRows(job.getTotalRows())
                .errorCode(job.getErrorCode())
                .errorMessage(job.getErrorMessage())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .startedAt(job.getStartedAt())
                .finishedAt(job.getFinishedAt())
                .createdBy(job.getCreatedBy())
                .updatedBy(job.getUpdatedBy())
                .cancellable(isCancellable(job.getStatus()))
                .retryable(isRetryable(job.getStatus()))
                .file(toFileResponse(job, downloadUrl, downloadUrlExpiresAt))
                .stepLogs(mappedStepLogs)
                .build();
    }

    public BackgroundJobStepLogResponse toStepLogResponse(BackgroundJobStepLog stepLog) {
        if (stepLog == null) {
            return null;
        }

        return BackgroundJobStepLogResponse.builder()
                .id(stepLog.getId())
                .stepName(stepLog.getStepName())
                .status(stepLog.getStatus())
                .message(stepLog.getMessage())
                .progressPercent(stepLog.getProgressPercent())
                .createdAt(stepLog.getCreatedAt())
                .updatedAt(stepLog.getUpdatedAt())
                .build();
    }

    public BackgroundJobFileResponse toFileResponse(BackgroundJob job, String downloadUrl, Instant downloadUrlExpiresAt) {
        if (job == null || job.getStorageObjectKey() == null || job.getResultFileName() == null) {
            return null;
        }

        return BackgroundJobFileResponse.builder()
                .fileName(job.getResultFileName())
                .storageObjectKey(job.getStorageObjectKey())
                .mimeType(job.getResultMimeType())
                .fileSize(job.getResultFileSize())
                .downloadUrl(downloadUrl)
                .downloadUrlExpiresAt(downloadUrlExpiresAt)
                .build();
    }

    private boolean isCancellable(BackgroundJobStatus status) {
        return status == BackgroundJobStatus.PENDING
                || status == BackgroundJobStatus.VALIDATING
                || status == BackgroundJobStatus.PROCESSING
                || status == BackgroundJobStatus.GENERATING_FILE;
    }

    private boolean isRetryable(BackgroundJobStatus status) {
        return status == BackgroundJobStatus.FAILED
                || status == BackgroundJobStatus.CANCELLED;
    }
}
