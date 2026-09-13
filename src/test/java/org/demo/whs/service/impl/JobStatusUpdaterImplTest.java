package org.demo.whs.service.impl;

import org.demo.whs.entity.BackgroundJob;
import org.demo.whs.entity.BackgroundJobStepLog;
import org.demo.whs.entity.dto.response.FileUploadResponse;
import org.demo.whs.entity.enums.BackgroundJobStatus;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.exception.NotFoundException;
import org.demo.whs.repository.BackgroundJobRepository;
import org.demo.whs.repository.BackgroundJobStepLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JobStatusUpdaterImpl Unit Tests")
class JobStatusUpdaterImplTest {

    @Mock
    private BackgroundJobRepository backgroundJobRepository;

    @Mock
    private BackgroundJobStepLogRepository backgroundJobStepLogRepository;

    @InjectMocks
    private JobStatusUpdaterImpl jobStatusUpdater;

    private BackgroundJob createJob(String id, BackgroundJobStatus status) {
        BackgroundJob job = BackgroundJob.builder()
                .status(status)
                .currentStep(status.name())
                .progressPercent(0)
                .build();
        job.setId(id);
        return job;
    }

    @Nested
    @DisplayName("startValidation tests")
    class StartValidationTests {

        @Test
        @DisplayName("should_TransitionToValidating_When_JobIsPending")
        void should_TransitionToValidating_When_JobIsPending() {
            BackgroundJob job = createJob("job-1", BackgroundJobStatus.PENDING);
            when(backgroundJobRepository.findById("job-1")).thenReturn(Optional.of(job));
            when(backgroundJobRepository.save(any(BackgroundJob.class))).thenAnswer(inv -> inv.getArgument(0));

            BackgroundJob result = jobStatusUpdater.startValidation("job-1", "VALIDATE_HEADERS", "Validating file");

            assertThat(result.getStatus()).isEqualTo(BackgroundJobStatus.VALIDATING);
            assertThat(result.getCurrentStep()).isEqualTo("VALIDATE_HEADERS");
            assertThat(result.getProgressPercent()).isEqualTo(5);
            assertThat(result.getStartedAt()).isNotNull();

            verify(backgroundJobStepLogRepository).save(any(BackgroundJobStepLog.class));
            verify(backgroundJobRepository).save(job);
        }

        @Test
        @DisplayName("should_ThrowNotFoundException_When_JobNotFound")
        void should_ThrowNotFoundException_When_JobNotFound() {
            when(backgroundJobRepository.findById("job-x")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> jobStatusUpdater.startValidation("job-x", "STEP", "msg"))
                    .isInstanceOf(NotFoundException.class)
                    .matches(e -> ((NotFoundException) e).getErrorCode().equals(ErrorCode.JOB_001.getCode()));
        }

        @Test
        @DisplayName("should_ThrowBadRequestException_When_TransitionIsInvalid")
        void should_ThrowBadRequestException_When_TransitionIsInvalid() {
            // COMPLETED cannot transition to VALIDATING
            BackgroundJob job = createJob("job-1", BackgroundJobStatus.COMPLETED);
            when(backgroundJobRepository.findById("job-1")).thenReturn(Optional.of(job));

            assertThatThrownBy(() -> jobStatusUpdater.startValidation("job-1", "STEP", "msg"))
                    .isInstanceOf(BadRequestException.class)
                    .matches(e -> ((BadRequestException) e).getErrorCode().equals(ErrorCode.JOB_002.getCode()));
        }
    }

    @Nested
    @DisplayName("startProcessing tests")
    class StartProcessingTests {

        @Test
        @DisplayName("should_TransitionToProcessing_When_JobIsValidating")
        void should_TransitionToProcessing_When_JobIsValidating() {
            BackgroundJob job = createJob("job-2", BackgroundJobStatus.VALIDATING);
            when(backgroundJobRepository.findById("job-2")).thenReturn(Optional.of(job));
            when(backgroundJobRepository.save(any(BackgroundJob.class))).thenAnswer(inv -> inv.getArgument(0));

            BackgroundJob result = jobStatusUpdater.startProcessing("job-2", "PROCESS_DATA", 500L, "Started processing");

            assertThat(result.getStatus()).isEqualTo(BackgroundJobStatus.PROCESSING);
            assertThat(result.getCurrentStep()).isEqualTo("PROCESS_DATA");
            assertThat(result.getProgressPercent()).isEqualTo(15);
            assertThat(result.getProcessedRows()).isEqualTo(0L);
            assertThat(result.getTotalRows()).isEqualTo(500L);

            verify(backgroundJobStepLogRepository).save(any(BackgroundJobStepLog.class));
        }
    }

    @Nested
    @DisplayName("updateProgress tests")
    class UpdateProgressTests {

        @Test
        @DisplayName("should_UpdateProgressSuccessfully")
        void should_UpdateProgressSuccessfully() {
            BackgroundJob job = createJob("job-3", BackgroundJobStatus.PROCESSING);
            job.setProgressPercent(15);
            when(backgroundJobRepository.findById("job-3")).thenReturn(Optional.of(job));
            when(backgroundJobRepository.save(any(BackgroundJob.class))).thenAnswer(inv -> inv.getArgument(0));

            BackgroundJob result = jobStatusUpdater.updateProgress(
                    "job-3", BackgroundJobStatus.PROCESSING, "PROCESSING_CHUNK_1", 50, 250L, 500L, "Processed 250 rows");

            assertThat(result.getStatus()).isEqualTo(BackgroundJobStatus.PROCESSING);
            assertThat(result.getProgressPercent()).isEqualTo(50);
            assertThat(result.getProcessedRows()).isEqualTo(250L);
            assertThat(result.getTotalRows()).isEqualTo(500L);
        }
    }

    @Nested
    @DisplayName("markGeneratingFile and markUploading tests")
    class FileGenerationAndUploadTests {

        @Test
        @DisplayName("should_TransitionToGeneratingFile_When_JobIsProcessing")
        void should_TransitionToGeneratingFile_When_JobIsProcessing() {
            BackgroundJob job = createJob("job-4", BackgroundJobStatus.PROCESSING);
            when(backgroundJobRepository.findById("job-4")).thenReturn(Optional.of(job));
            when(backgroundJobRepository.save(any(BackgroundJob.class))).thenAnswer(inv -> inv.getArgument(0));

            BackgroundJob result = jobStatusUpdater.markGeneratingFile("job-4", "GEN_EXCEL", "Generating file");

            assertThat(result.getStatus()).isEqualTo(BackgroundJobStatus.GENERATING_FILE);
            assertThat(result.getProgressPercent()).isEqualTo(75);
        }

        @Test
        @DisplayName("should_TransitionToUploading_When_JobIsGeneratingFile")
        void should_TransitionToUploading_When_JobIsGeneratingFile() {
            BackgroundJob job = createJob("job-5", BackgroundJobStatus.GENERATING_FILE);
            when(backgroundJobRepository.findById("job-5")).thenReturn(Optional.of(job));
            when(backgroundJobRepository.save(any(BackgroundJob.class))).thenAnswer(inv -> inv.getArgument(0));

            BackgroundJob result = jobStatusUpdater.markUploading("job-5", "UPLOAD_MINIO", "Uploading file");

            assertThat(result.getStatus()).isEqualTo(BackgroundJobStatus.UPLOADING);
            assertThat(result.getProgressPercent()).isEqualTo(90);
        }
    }

    @Nested
    @DisplayName("markCompleted tests")
    class MarkCompletedTests {

        @Test
        @DisplayName("should_MarkJobCompleted_With_UploadedFileInfo")
        void should_MarkJobCompleted_With_UploadedFileInfo() {
            BackgroundJob job = createJob("job-6", BackgroundJobStatus.UPLOADING);
            FileUploadResponse fileInfo = FileUploadResponse.builder()
                    .originalFileName("stock_report.xlsx")
                    .objectName("jobs/2026/stock_report.xlsx")
                    .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    .size(1024L)
                    .build();

            when(backgroundJobRepository.findById("job-6")).thenReturn(Optional.of(job));
            when(backgroundJobRepository.save(any(BackgroundJob.class))).thenAnswer(inv -> inv.getArgument(0));

            BackgroundJob result = jobStatusUpdater.markCompleted("job-6", fileInfo, "Job completed successfully");

            assertThat(result.getStatus()).isEqualTo(BackgroundJobStatus.COMPLETED);
            assertThat(result.getCurrentStep()).isEqualTo("COMPLETED");
            assertThat(result.getProgressPercent()).isEqualTo(100);
            assertThat(result.getFinishedAt()).isNotNull();
            assertThat(result.getResultFileName()).isEqualTo("stock_report.xlsx");
            assertThat(result.getStorageObjectKey()).isEqualTo("jobs/2026/stock_report.xlsx");
            assertThat(result.getResultMimeType()).isEqualTo("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            assertThat(result.getResultFileSize()).isEqualTo(1024L);

            verify(backgroundJobStepLogRepository).save(any(BackgroundJobStepLog.class));
        }

        @Test
        @DisplayName("should_MarkJobCompleted_Without_UploadedFile")
        void should_MarkJobCompleted_Without_UploadedFile() {
            BackgroundJob job = createJob("job-7", BackgroundJobStatus.UPLOADING);
            when(backgroundJobRepository.findById("job-7")).thenReturn(Optional.of(job));
            when(backgroundJobRepository.save(any(BackgroundJob.class))).thenAnswer(inv -> inv.getArgument(0));

            BackgroundJob result = jobStatusUpdater.markCompleted("job-7", null, "Completed");

            assertThat(result.getStatus()).isEqualTo(BackgroundJobStatus.COMPLETED);
            assertThat(result.getResultFileName()).isNull();
        }

        @Test
        @DisplayName("should_ThrowBadRequestException_When_CompletingJobInInvalidStatus")
        void should_ThrowBadRequestException_When_CompletingJobInInvalidStatus() {
            // Cannot jump directly from PENDING to COMPLETED
            BackgroundJob job = createJob("job-8", BackgroundJobStatus.PENDING);
            when(backgroundJobRepository.findById("job-8")).thenReturn(Optional.of(job));

            assertThatThrownBy(() -> jobStatusUpdater.markCompleted("job-8", null, "Completed"))
                    .isInstanceOf(BadRequestException.class)
                    .matches(e -> ((BadRequestException) e).getErrorCode().equals(ErrorCode.JOB_002.getCode()));
        }
    }

    @Nested
    @DisplayName("markFailed tests")
    class MarkFailedTests {

        @Test
        @DisplayName("should_MarkJobFailed_When_StatusAllowsFailure")
        void should_MarkJobFailed_When_StatusAllowsFailure() {
            BackgroundJob job = createJob("job-9", BackgroundJobStatus.PROCESSING);
            when(backgroundJobRepository.findById("job-9")).thenReturn(Optional.of(job));
            when(backgroundJobRepository.save(any(BackgroundJob.class))).thenAnswer(inv -> inv.getArgument(0));

            BackgroundJob result = jobStatusUpdater.markFailed("job-9", "ERR_PARSE", "Syntax error in row 10", "PARSE_CSV");

            assertThat(result.getStatus()).isEqualTo(BackgroundJobStatus.FAILED);
            assertThat(result.getCurrentStep()).isEqualTo("PARSE_CSV");
            assertThat(result.getErrorCode()).isEqualTo("ERR_PARSE");
            assertThat(result.getErrorMessage()).isEqualTo("Syntax error in row 10");
            assertThat(result.getFinishedAt()).isNotNull();

            verify(backgroundJobStepLogRepository).save(any(BackgroundJobStepLog.class));
        }

        @Test
        @DisplayName("should_ThrowBadRequestException_When_FailingCompletedJob")
        void should_ThrowBadRequestException_When_FailingCompletedJob() {
            // COMPLETED job cannot transition to FAILED
            BackgroundJob job = createJob("job-10", BackgroundJobStatus.COMPLETED);
            when(backgroundJobRepository.findById("job-10")).thenReturn(Optional.of(job));

            assertThatThrownBy(() -> jobStatusUpdater.markFailed("job-10", "ERR", "msg", "STEP"))
                    .isInstanceOf(BadRequestException.class)
                    .matches(e -> ((BadRequestException) e).getErrorCode().equals(ErrorCode.JOB_002.getCode()));
        }
    }

    @Nested
    @DisplayName("markCancelled tests")
    class MarkCancelledTests {

        @Test
        @DisplayName("should_MarkJobCancelled_When_JobIsCancellable")
        void should_MarkJobCancelled_When_JobIsCancellable() {
            BackgroundJob job = createJob("job-11", BackgroundJobStatus.PENDING);
            when(backgroundJobRepository.findById("job-11")).thenReturn(Optional.of(job));
            when(backgroundJobRepository.save(any(BackgroundJob.class))).thenAnswer(inv -> inv.getArgument(0));

            BackgroundJob result = jobStatusUpdater.markCancelled("job-11", "User requested cancel");

            assertThat(result.getStatus()).isEqualTo(BackgroundJobStatus.CANCELLED);
            assertThat(result.getCurrentStep()).isEqualTo("CANCELLED");
            assertThat(result.getErrorMessage()).isEqualTo("User requested cancel");
            assertThat(result.getFinishedAt()).isNotNull();
        }

        @Test
        @DisplayName("should_ThrowBadRequestException_When_JobIsNotCancellable")
        void should_ThrowBadRequestException_When_JobIsNotCancellable() {
            // COMPLETED job is not cancellable
            BackgroundJob job = createJob("job-12", BackgroundJobStatus.COMPLETED);
            when(backgroundJobRepository.findById("job-12")).thenReturn(Optional.of(job));

            assertThatThrownBy(() -> jobStatusUpdater.markCancelled("job-12", "Cancel"))
                    .isInstanceOf(BadRequestException.class)
                    .matches(e -> ((BadRequestException) e).getErrorCode().equals(ErrorCode.JOB_004.getCode()));
        }
    }

    @Nested
    @DisplayName("resetForRetry tests")
    class ResetForRetryTests {

        @Test
        @DisplayName("should_ResetJobToPending_When_JobIsFailed")
        void should_ResetJobToPending_When_JobIsFailed() {
            BackgroundJob job = createJob("job-13", BackgroundJobStatus.FAILED);
            job.setStartedAt(LocalDateTime.now().minusHours(1));
            job.setFinishedAt(LocalDateTime.now());
            job.setProgressPercent(60);
            job.setProcessedRows(100L);
            job.setTotalRows(200L);
            job.setErrorCode("ERR_001");
            job.setErrorMessage("Network timeout");

            when(backgroundJobRepository.findById("job-13")).thenReturn(Optional.of(job));
            when(backgroundJobRepository.save(any(BackgroundJob.class))).thenAnswer(inv -> inv.getArgument(0));

            BackgroundJob result = jobStatusUpdater.resetForRetry("job-13", "Admin retrying");

            assertThat(result.getStatus()).isEqualTo(BackgroundJobStatus.PENDING);
            assertThat(result.getCurrentStep()).isEqualTo("PENDING");
            assertThat(result.getProgressPercent()).isEqualTo(0);
            assertThat(result.getProcessedRows()).isEqualTo(0L);
            assertThat(result.getTotalRows()).isEqualTo(0L);
            assertThat(result.getStartedAt()).isNull();
            assertThat(result.getFinishedAt()).isNull();
            assertThat(result.getErrorCode()).isNull();
            assertThat(result.getErrorMessage()).isNull();

            verify(backgroundJobStepLogRepository).save(any(BackgroundJobStepLog.class));
        }

        @Test
        @DisplayName("should_ResetJobToPending_When_JobIsCancelled")
        void should_ResetJobToPending_When_JobIsCancelled() {
            BackgroundJob job = createJob("job-14", BackgroundJobStatus.CANCELLED);
            when(backgroundJobRepository.findById("job-14")).thenReturn(Optional.of(job));
            when(backgroundJobRepository.save(any(BackgroundJob.class))).thenAnswer(inv -> inv.getArgument(0));

            BackgroundJob result = jobStatusUpdater.resetForRetry("job-14", "Retry cancelled job");

            assertThat(result.getStatus()).isEqualTo(BackgroundJobStatus.PENDING);
        }

        @Test
        @DisplayName("should_ThrowBadRequestException_When_JobIsNotRetryable")
        void should_ThrowBadRequestException_When_JobIsNotRetryable() {
            // PROCESSING job cannot be reset for retry
            BackgroundJob job = createJob("job-15", BackgroundJobStatus.PROCESSING);
            when(backgroundJobRepository.findById("job-15")).thenReturn(Optional.of(job));

            assertThatThrownBy(() -> jobStatusUpdater.resetForRetry("job-15", "Retry"))
                    .isInstanceOf(BadRequestException.class)
                    .matches(e -> ((BadRequestException) e).getErrorCode().equals(ErrorCode.JOB_003.getCode()));
        }
    }
}
