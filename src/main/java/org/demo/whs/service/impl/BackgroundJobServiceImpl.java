package org.demo.whs.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.BackgroundJob;
import org.demo.whs.entity.BackgroundJobStepLog;
import org.demo.whs.entity.dto.request.BackgroundJob.BackgroundJobFilterRequest;
import org.demo.whs.entity.dto.request.BackgroundJob.CancelBackgroundJobRequest;
import org.demo.whs.entity.dto.request.BackgroundJob.RetryBackgroundJobRequest;
import org.demo.whs.entity.dto.response.BackgroundJob.BackgroundJobDetailResponse;
import org.demo.whs.entity.dto.response.BackgroundJob.BackgroundJobFileResponse;
import org.demo.whs.entity.dto.response.BackgroundJob.BackgroundJobStatusResponse;
import org.demo.whs.entity.dto.response.BackgroundJob.BackgroundJobSummaryResponse;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.exception.NotFoundException;
import org.demo.whs.helpers.producer.BackgroundJobProducerService;
import org.demo.whs.mapper.BackgroundJobMapper;
import org.demo.whs.repository.BackgroundJobRepository;
import org.demo.whs.repository.BackgroundJobStepLogRepository;
import org.demo.whs.service.BackgroundJobService;
import org.demo.whs.service.JobStatusUpdater;
import org.demo.whs.service.StorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Core service scaffold for background job APIs.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BackgroundJobServiceImpl implements BackgroundJobService {

    private final BackgroundJobRepository backgroundJobRepository;
    private final BackgroundJobStepLogRepository backgroundJobStepLogRepository;
    private final BackgroundJobMapper backgroundJobMapper;
    private final StorageService storageService;
    private final JobStatusUpdater jobStatusUpdater;
    private final Optional<BackgroundJobProducerService> backgroundJobProducerService;

    @Value("${app.minio.presigned-url-expiry:3600}")
    private long presignedUrlExpirySeconds;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BackgroundJobSummaryResponse> getMyJobs(String requestedBy, BackgroundJobFilterRequest request) {
        Pageable pageable = PageRequest.of(resolvePage(request), resolveSize(request), Sort.by(Sort.Direction.DESC, "createdAt"));

        if (requestedBy == null || requestedBy.isBlank()) {
            return PageResponse.from(Page.empty(pageable));
        }

        Page<BackgroundJob> page = backgroundJobRepository.findByRequestedByOrderByCreatedAtDesc(requestedBy, pageable);
        List<BackgroundJobSummaryResponse> content = page.getContent()
                .stream()
                .map(backgroundJobMapper::toSummaryResponse)
                .toList();

        return PageResponse.from(page, content);
    }

    @Override
    @Transactional(readOnly = true)
    public BackgroundJobDetailResponse getJobDetail(String jobId, String requestedBy) {
        BackgroundJob job = getOwnedJob(jobId, requestedBy);
        List<BackgroundJobStepLog> stepLogs = backgroundJobStepLogRepository.findByJobIdOrderByCreatedAtAsc(jobId);
        return backgroundJobMapper.toDetailResponse(job, stepLogs, null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public BackgroundJobStatusResponse getJobStatus(String jobId, String requestedBy) {
        BackgroundJob job = getOwnedJob(jobId, requestedBy);
        return backgroundJobMapper.toStatusResponse(job, null, null);
    }

    @Override
    @Transactional
    public BackgroundJobStatusResponse retryJob(String jobId, String requestedBy, RetryBackgroundJobRequest request) {
        BackgroundJob job = getOwnedJob(jobId, requestedBy);
        String reason = request == null ? null : request.getReason();

        BackgroundJob retriedJob = jobStatusUpdater.resetForRetry(job.getId(), reason);
        dispatchAfterCommit(retriedJob);

        log.info("Retry requested for background job id={} by requestedBy={} reason={}", jobId, requestedBy, reason);
        return backgroundJobMapper.toStatusResponse(retriedJob, null, null);
    }

    @Override
    @Transactional
    public BackgroundJobStatusResponse cancelJob(String jobId, String requestedBy, CancelBackgroundJobRequest request) {
        BackgroundJob job = getOwnedJob(jobId, requestedBy);
        String reason = request == null ? null : request.getReason();

        BackgroundJob cancelledJob = jobStatusUpdater.markCancelled(job.getId(), reason);
        log.info("Cancel requested for background job id={} by requestedBy={} reason={}", jobId, requestedBy, reason);
        return backgroundJobMapper.toStatusResponse(cancelledJob, null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public BackgroundJobFileResponse getJobDownload(String jobId, String requestedBy) {
        BackgroundJob job = getOwnedJob(jobId, requestedBy);

        if (job.getStorageObjectKey() == null || job.getResultFileName() == null) {
            throw new NotFoundException("Background job result file not found", ErrorCode.STORAGE_002);
        }

        String downloadUrl = storageService.getPresignedUrl(job.getStorageObjectKey());
        Instant expiresAt = Instant.now().plusSeconds(presignedUrlExpirySeconds);

        return backgroundJobMapper.toFileResponse(job, downloadUrl, expiresAt);
    }

    private BackgroundJob getOwnedJob(String jobId, String requestedBy) {
        if (requestedBy == null || requestedBy.isBlank()) {
            throw new NotFoundException("Background job not found with ID: " + jobId, ErrorCode.JOB_001);
        }

        return backgroundJobRepository.findByIdAndRequestedBy(jobId, requestedBy)
                .orElseThrow(() -> new NotFoundException("Background job not found with ID: " + jobId, ErrorCode.JOB_001));
    }

    private int resolvePage(BackgroundJobFilterRequest request) {
        return request == null || request.getPage() == null ? 0 : request.getPage();
    }

    private int resolveSize(BackgroundJobFilterRequest request) {
        return request == null || request.getSize() == null ? 20 : request.getSize();
    }

    private void dispatchAfterCommit(BackgroundJob job) {
        if (backgroundJobProducerService.isEmpty()) {
            log.warn("BackgroundJobProducerService not available, skip queue dispatch for job id={}", job.getId());
            return;
        }

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            backgroundJobProducerService.get().sendJobToQueue(job);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                backgroundJobProducerService.get().sendJobToQueue(job);
            }
        });
    }
}
