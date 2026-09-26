package org.demo.whs.service.impl;

import org.demo.whs.entity.BackgroundJob;
import org.demo.whs.entity.BackgroundJobStepLog;
import org.demo.whs.entity.dto.request.BackgroundJob.BackgroundJobActionRequest;
import org.demo.whs.entity.dto.request.BackgroundJob.BackgroundJobFilterRequest;
import org.demo.whs.entity.dto.response.BackgroundJob.BackgroundJobDetailResponse;
import org.demo.whs.entity.dto.response.BackgroundJob.BackgroundJobFileResponse;
import org.demo.whs.entity.dto.response.BackgroundJob.BackgroundJobStatusResponse;
import org.demo.whs.entity.dto.response.BackgroundJob.BackgroundJobSummaryResponse;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.exception.NotFoundException;
import org.demo.whs.mapper.BackgroundJobMapper;
import org.demo.whs.repository.BackgroundJobRepository;
import org.demo.whs.repository.BackgroundJobStepLogRepository;
import org.demo.whs.service.JobStatusUpdater;
import org.demo.whs.service.StorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BackgroundJobServiceImpl Unit Tests")
class BackgroundJobServiceImplTest {

    @Mock
    private BackgroundJobRepository backgroundJobRepository;

    @Mock
    private BackgroundJobStepLogRepository backgroundJobStepLogRepository;

    @Mock
    private BackgroundJobMapper backgroundJobMapper;

    @Mock
    private StorageService storageService;

    @Mock
    private JobStatusUpdater jobStatusUpdater;

    @InjectMocks
    private BackgroundJobServiceImpl backgroundJobService;

    @Nested
    @DisplayName("getMyJobs tests")
    class GetMyJobsTests {

        @Test
        @DisplayName("should_ReturnEmptyPage_When_RequestedByIsNull")
        void should_ReturnEmptyPage_When_RequestedByIsNull() {
            PageResponse<BackgroundJobSummaryResponse> result =
                    backgroundJobService.getMyJobs(null, new BackgroundJobFilterRequest());

            assertThat(result.getContent()).isEmpty();
            verifyNoInteractions(backgroundJobRepository);
        }

        @Test
        @DisplayName("should_ReturnEmptyPage_When_RequestedByIsBlank")
        void should_ReturnEmptyPage_When_RequestedByIsBlank() {
            PageResponse<BackgroundJobSummaryResponse> result =
                    backgroundJobService.getMyJobs("  ", new BackgroundJobFilterRequest());

            assertThat(result.getContent()).isEmpty();
        }

        @Test
        @DisplayName("should_ReturnPageResponse_When_ValidRequestedBy")
        void should_ReturnPageResponse_When_ValidRequestedBy() {
            BackgroundJob job = new BackgroundJob();
            job.setId("job-1");
            Page<BackgroundJob> page = new PageImpl<>(List.of(job));
            BackgroundJobSummaryResponse summary = BackgroundJobSummaryResponse.builder().id("job-1").build();

            when(backgroundJobRepository.searchMyJobs(eq("acc-1"), isNull(), isNull(), isNull(),
                    isNull(), isNull(), isNull(), any(Pageable.class)))
                    .thenReturn(page);
            when(backgroundJobMapper.toSummaryResponse(job)).thenReturn(summary);

            PageResponse<BackgroundJobSummaryResponse> result =
                    backgroundJobService.getMyJobs("acc-1", null);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getId()).isEqualTo("job-1");
        }
    }

    @Nested
    @DisplayName("getJobDetail tests")
    class GetJobDetailTests {

        @Test
        @DisplayName("should_ThrowNotFoundException_When_JobIdNotOwnedByUser")
        void should_ThrowNotFoundException_When_JobIdNotOwnedByUser() {
            when(backgroundJobRepository.findByIdAndRequestedBy("job-x", "acc-1"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> backgroundJobService.getJobDetail("job-x", "acc-1"))
                    .isInstanceOf(NotFoundException.class)
                    .matches(e -> ((NotFoundException) e).getErrorCode().equals(ErrorCode.JOB_001.getCode()));
        }

        @Test
        @DisplayName("should_ReturnDetailResponse_When_JobBelongsToUser")
        void should_ReturnDetailResponse_When_JobBelongsToUser() {
            BackgroundJob job = new BackgroundJob();
            job.setId("job-1");
            List<BackgroundJobStepLog> stepLogs = List.of();
            BackgroundJobDetailResponse detail = BackgroundJobDetailResponse.builder().id("job-1").build();

            when(backgroundJobRepository.findByIdAndRequestedBy("job-1", "acc-1")).thenReturn(Optional.of(job));
            when(backgroundJobStepLogRepository.findByJobIdOrderByCreatedAtAsc("job-1")).thenReturn(stepLogs);
            when(backgroundJobMapper.toDetailResponse(job, stepLogs, null, null)).thenReturn(detail);

            BackgroundJobDetailResponse result = backgroundJobService.getJobDetail("job-1", "acc-1");

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo("job-1");
        }
    }

    @Nested
    @DisplayName("getJobStatus tests")
    class GetJobStatusTests {

        @Test
        @DisplayName("should_ThrowNotFoundException_When_RequestedByIsNull")
        void should_ThrowNotFoundException_When_RequestedByIsNull() {
            assertThatThrownBy(() -> backgroundJobService.getJobStatus("job-1", null))
                    .isInstanceOf(NotFoundException.class)
                    .matches(e -> ((NotFoundException) e).getErrorCode().equals(ErrorCode.JOB_001.getCode()));
        }

        @Test
        @DisplayName("should_ReturnStatusResponse_When_JobFound")
        void should_ReturnStatusResponse_When_JobFound() {
            BackgroundJob job = new BackgroundJob();
            job.setId("job-1");
            BackgroundJobStatusResponse statusResponse = BackgroundJobStatusResponse.builder().id("job-1").build();

            when(backgroundJobRepository.findByIdAndRequestedBy("job-1", "acc-1")).thenReturn(Optional.of(job));
            when(backgroundJobMapper.toStatusResponse(job, null, null)).thenReturn(statusResponse);

            BackgroundJobStatusResponse result = backgroundJobService.getJobStatus("job-1", "acc-1");

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo("job-1");
        }
    }

    @Nested
    @DisplayName("cancelJob tests")
    class CancelJobTests {

        @Test
        @DisplayName("should_CancelJob_When_JobExists")
        void should_CancelJob_When_JobExists() {
            BackgroundJob job = new BackgroundJob();
            job.setId("job-1");
            BackgroundJob cancelled = new BackgroundJob();
            cancelled.setId("job-1");
            BackgroundJobStatusResponse statusResponse = BackgroundJobStatusResponse.builder().id("job-1").build();

            when(backgroundJobRepository.findByIdAndRequestedBy("job-1", "acc-1")).thenReturn(Optional.of(job));
            when(jobStatusUpdater.markCancelled(eq("job-1"), any())).thenReturn(cancelled);
            when(backgroundJobMapper.toStatusResponse(cancelled, null, null)).thenReturn(statusResponse);

            BackgroundJobActionRequest request = new BackgroundJobActionRequest();
            request.setReason("User requested");
            BackgroundJobStatusResponse result = backgroundJobService.cancelJob("job-1", "acc-1", request);

            assertThat(result).isNotNull();
            verify(jobStatusUpdater).markCancelled("job-1", "User requested");
        }
    }

    @Nested
    @DisplayName("getJobDownload tests")
    class GetJobDownloadTests {

        @Test
        @DisplayName("should_ThrowNotFoundException_When_StorageKeyIsNull")
        void should_ThrowNotFoundException_When_StorageKeyIsNull() {
            BackgroundJob job = new BackgroundJob();
            job.setId("job-1");
            job.setStorageObjectKey(null);
            job.setResultFileName(null);

            when(backgroundJobRepository.findByIdAndRequestedBy("job-1", "acc-1")).thenReturn(Optional.of(job));

            assertThatThrownBy(() -> backgroundJobService.getJobDownload("job-1", "acc-1"))
                    .isInstanceOf(NotFoundException.class)
                    .matches(e -> ((NotFoundException) e).getErrorCode().equals(ErrorCode.STORAGE_002.getCode()));
        }

        @Test
        @DisplayName("should_ReturnFileResponse_When_StorageKeyExists")
        void should_ReturnFileResponse_When_StorageKeyExists() {
            BackgroundJob job = new BackgroundJob();
            job.setId("job-1");
            job.setStorageObjectKey("bucket/path/file.pdf");
            job.setResultFileName("file.pdf");

            BackgroundJobFileResponse fileResponse = BackgroundJobFileResponse.builder()
                    .downloadUrl("https://storage/file.pdf")
                    .build();

            when(backgroundJobRepository.findByIdAndRequestedBy("job-1", "acc-1")).thenReturn(Optional.of(job));
            when(storageService.getPresignedUrl("bucket/path/file.pdf")).thenReturn("https://storage/file.pdf");
            when(backgroundJobMapper.toFileResponse(eq(job), anyString(), any())).thenReturn(fileResponse);

            ReflectionTestUtils.setField(backgroundJobService, "presignedUrlExpirySeconds", 3600L);

            BackgroundJobFileResponse result = backgroundJobService.getJobDownload("job-1", "acc-1");

            assertThat(result).isNotNull();
            assertThat(result.getDownloadUrl()).isEqualTo("https://storage/file.pdf");
        }
    }
}
