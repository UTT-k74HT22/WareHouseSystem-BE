package org.demo.whs.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.BackgroundJob;
import org.demo.whs.entity.BackgroundJobStepLog;
import org.demo.whs.entity.dto.response.FileUploadResponse;
import org.demo.whs.entity.enums.BackgroundJobStatus;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.exception.NotFoundException;
import org.demo.whs.repository.BackgroundJobRepository;
import org.demo.whs.repository.BackgroundJobStepLogRepository;
import org.demo.whs.service.JobStatusUpdater;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Service implementation for updating background job statuses and logging steps.
 * This service ensures valid status transitions and maintains a history of job steps.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JobStatusUpdaterImpl implements JobStatusUpdater {

    private static final Map<BackgroundJobStatus, Set<BackgroundJobStatus>> ALLOWED_TRANSITIONS = buildAllowedTransitions();

    private final BackgroundJobRepository backgroundJobRepository;
    private final BackgroundJobStepLogRepository backgroundJobStepLogRepository;

    @Override
    @Transactional
    public BackgroundJob startValidation(String jobId, String stepName, String message) {
        return transition(jobId, BackgroundJobStatus.VALIDATING, stepName, 5, null, null, null, null, message);
    }

    @Override
    @Transactional
    public BackgroundJob startProcessing(String jobId, String stepName, Long totalRows, String message) {
        return transition(jobId, BackgroundJobStatus.PROCESSING, stepName, 15, 0L, totalRows, null, null, message);
    }

    @Override
    @Transactional
    public BackgroundJob updateProgress(String jobId, BackgroundJobStatus status, String stepName, Integer progressPercent, Long processedRows, Long totalRows, String message) {
        return transition(jobId, status, stepName, progressPercent, processedRows, totalRows, null, null, message);
    }

    @Override
    @Transactional
    public BackgroundJob markGeneratingFile(String jobId, String stepName, String message) {
        return transition(jobId, BackgroundJobStatus.GENERATING_FILE, stepName, 75, null, null, null, null, message);
    }

    @Override
    @Transactional
    public BackgroundJob markUploading(String jobId, String stepName, String message) {
        return transition(jobId, BackgroundJobStatus.UPLOADING, stepName, 90, null, null, null, null, message);
    }

    @Override
    @Transactional
    public BackgroundJob markCompleted(String jobId, FileUploadResponse uploadedFile, String message) {
        BackgroundJob job = getJob(jobId);
        validateTransition(job.getStatus(), BackgroundJobStatus.COMPLETED);

        job.setStatus(BackgroundJobStatus.COMPLETED);
        job.setCurrentStep("COMPLETED");
        job.setProgressPercent(100);
        job.setFinishedAt(LocalDateTime.now());
        job.setErrorCode(null);
        job.setErrorMessage(null);

        if (uploadedFile != null) {
            job.setResultFileName(uploadedFile.getOriginalFileName());
            job.setStorageObjectKey(uploadedFile.getObjectName());
            job.setResultMimeType(uploadedFile.getContentType());
            job.setResultFileSize(uploadedFile.getSize());
        }

        BackgroundJob savedJob = backgroundJobRepository.save(job);
        appendStepLog(savedJob, "COMPLETED", BackgroundJobStatus.COMPLETED, message, savedJob.getProgressPercent());
        log.info("Background job marked completed id={} file={}", savedJob.getId(), savedJob.getStorageObjectKey());
        return savedJob;
    }

    @Override
    @Transactional
    public BackgroundJob markFailed(String jobId, String errorCode, String errorMessage, String stepName) {
        BackgroundJob job = getJob(jobId);
        validateTransition(job.getStatus(), BackgroundJobStatus.FAILED);

        job.setStatus(BackgroundJobStatus.FAILED);
        job.setCurrentStep(stepName);
        job.setFinishedAt(LocalDateTime.now());
        job.setErrorCode(errorCode);
        job.setErrorMessage(errorMessage);

        BackgroundJob savedJob = backgroundJobRepository.save(job);
        appendStepLog(savedJob, stepName, BackgroundJobStatus.FAILED, errorMessage, savedJob.getProgressPercent());
        log.warn("Background job marked failed id={} errorCode={}", savedJob.getId(), errorCode);
        return savedJob;
    }

    @Override
    @Transactional
    public BackgroundJob markCancelled(String jobId, String reason) {
        BackgroundJob job = getJob(jobId);
        if (!isCancellable(job.getStatus())) {
            throw new BadRequestException("Background job is not cancellable", ErrorCode.JOB_004);
        }

        job.setStatus(BackgroundJobStatus.CANCELLED);
        job.setCurrentStep("CANCELLED");
        job.setFinishedAt(LocalDateTime.now());
        job.setErrorCode(null);
        job.setErrorMessage(reason);

        BackgroundJob savedJob = backgroundJobRepository.save(job);
        appendStepLog(savedJob, "CANCELLED", BackgroundJobStatus.CANCELLED, reason, savedJob.getProgressPercent());
        log.info("Background job marked cancelled id={}", savedJob.getId());
        return savedJob;
    }

    @Override
    @Transactional
    public BackgroundJob resetForRetry(String jobId, String reason) {
        BackgroundJob job = getJob(jobId);
        if (job.getStatus() != BackgroundJobStatus.FAILED && job.getStatus() != BackgroundJobStatus.CANCELLED) {
            throw new BadRequestException("Background job is not retryable", ErrorCode.JOB_003);
        }

        job.setStatus(BackgroundJobStatus.PENDING);
        job.setCurrentStep("PENDING");
        job.setProgressPercent(0);
        job.setProcessedRows(0L);
        job.setTotalRows(0L);
        job.setStartedAt(null);
        job.setFinishedAt(null);
        job.setResultFileName(null);
        job.setStorageObjectKey(null);
        job.setResultMimeType(null);
        job.setResultFileSize(null);
        job.setErrorCode(null);
        job.setErrorMessage(null);

        BackgroundJob savedJob = backgroundJobRepository.save(job);
        appendStepLog(savedJob, "PENDING", BackgroundJobStatus.PENDING, reason, savedJob.getProgressPercent());
        log.info("Background job reset for retry id={}", savedJob.getId());
        return savedJob;
    }

    private BackgroundJob transition(String jobId, BackgroundJobStatus targetStatus, String stepName, Integer progressPercent, Long processedRows, Long totalRows, String errorCode, String errorMessage, String message) {
        BackgroundJob job = getJob(jobId);
        validateTransition(job.getStatus(), targetStatus);

        job.setStatus(targetStatus);
        job.setCurrentStep(stepName);
        job.setErrorCode(errorCode);
        job.setErrorMessage(errorMessage);

        if (progressPercent != null) {
            job.setProgressPercent(progressPercent);
        }

        if (processedRows != null) {
            job.setProcessedRows(processedRows);
        }

        if (totalRows != null) {
            job.setTotalRows(totalRows);
        }

        if (targetStatus == BackgroundJobStatus.VALIDATING || targetStatus == BackgroundJobStatus.PROCESSING) {
            if (job.getStartedAt() == null) {
                job.setStartedAt(LocalDateTime.now());
            }
        }

        BackgroundJob savedJob = backgroundJobRepository.save(job);
        appendStepLog(savedJob, stepName, targetStatus, message, savedJob.getProgressPercent());
        return savedJob;
    }

    private BackgroundJob getJob(String jobId) {
        return backgroundJobRepository.findById(jobId)
                .orElseThrow(() -> new NotFoundException("Background job not found with ID: " + jobId, ErrorCode.JOB_001));
    }

    private void validateTransition(BackgroundJobStatus currentStatus, BackgroundJobStatus targetStatus) {
        if (currentStatus == targetStatus) {
            return;
        }

        Set<BackgroundJobStatus> allowedTargets = ALLOWED_TRANSITIONS.getOrDefault(currentStatus, EnumSet.noneOf(BackgroundJobStatus.class));
        if (!allowedTargets.contains(targetStatus)) {
            throw new BadRequestException(
                    String.format("Invalid background job status transition from %s to %s", currentStatus, targetStatus),
                    ErrorCode.JOB_002
            );
        }
    }

    private boolean isCancellable(BackgroundJobStatus status) {
        return status == BackgroundJobStatus.PENDING
                || status == BackgroundJobStatus.VALIDATING
                || status == BackgroundJobStatus.PROCESSING
                || status == BackgroundJobStatus.GENERATING_FILE;
    }

    private void appendStepLog(BackgroundJob job, String stepName, BackgroundJobStatus status, String message, Integer progressPercent) {
        if ((stepName == null || stepName.isBlank()) && (message == null || message.isBlank())) {
            return;
        }

        BackgroundJobStepLog stepLog = BackgroundJobStepLog.builder()
                .jobId(job.getId())
                .stepName(stepName == null || stepName.isBlank() ? status.name() : stepName)
                .status(status)
                .message(message)
                .progressPercent(progressPercent == null ? 0 : progressPercent)
                .build();
        backgroundJobStepLogRepository.save(stepLog);
    }

    private static Map<BackgroundJobStatus, Set<BackgroundJobStatus>> buildAllowedTransitions() {
        Map<BackgroundJobStatus, Set<BackgroundJobStatus>> transitions = new EnumMap<>(BackgroundJobStatus.class);
        transitions.put(BackgroundJobStatus.PENDING, EnumSet.of(
                BackgroundJobStatus.VALIDATING,
                BackgroundJobStatus.FAILED,
                BackgroundJobStatus.CANCELLED
        ));
        transitions.put(BackgroundJobStatus.VALIDATING, EnumSet.of(
                BackgroundJobStatus.PROCESSING,
                BackgroundJobStatus.FAILED,
                BackgroundJobStatus.CANCELLED
        ));
        transitions.put(BackgroundJobStatus.PROCESSING, EnumSet.of(
                BackgroundJobStatus.PROCESSING,
                BackgroundJobStatus.GENERATING_FILE,
                BackgroundJobStatus.FAILED,
                BackgroundJobStatus.CANCELLED
        ));
        transitions.put(BackgroundJobStatus.GENERATING_FILE, EnumSet.of(
                BackgroundJobStatus.UPLOADING,
                BackgroundJobStatus.FAILED,
                BackgroundJobStatus.CANCELLED
        ));
        transitions.put(BackgroundJobStatus.UPLOADING, EnumSet.of(
                BackgroundJobStatus.COMPLETED,
                BackgroundJobStatus.FAILED
        ));
        transitions.put(BackgroundJobStatus.FAILED, EnumSet.of(BackgroundJobStatus.PENDING));
        transitions.put(BackgroundJobStatus.CANCELLED, EnumSet.of(BackgroundJobStatus.PENDING));
        transitions.put(BackgroundJobStatus.COMPLETED, EnumSet.noneOf(BackgroundJobStatus.class));
        return transitions;
    }
}
