package org.demo.whs.service.processor.export;

import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.BackgroundJob;
import org.demo.whs.entity.dto.response.FileUploadResponse;
import org.demo.whs.entity.enums.BackgroundJobStatus;
import org.demo.whs.exception.BaseException;
import org.demo.whs.service.JobStatusUpdater;
import org.demo.whs.service.StorageService;
import org.demo.whs.service.processor.BackgroundJobProcessor;
import org.demo.whs.utils.strategy.export.GeneratedExportFile;
import org.demo.whs.utils.strategy.report.ReportDataSet;

/**
 * AbstractExportProcessor provides a template for processing background export jobs. It defines the common workflow
 * for validating the job, loading the necessary data, generating the export file, uploading it to storage,
 * and updating the job status throughout the process. Subclasses are responsible for implementing the specific
 * details of each step, allowing for flexibility in handling different types of export jobs while maintaining
 * a consistent processing structure.
 */
@Slf4j
public abstract class AbstractExportProcessor implements BackgroundJobProcessor {

    private final JobStatusUpdater jobStatusUpdater;
    private final StorageService storageService;

    protected AbstractExportProcessor(JobStatusUpdater jobStatusUpdater, StorageService storageService) {
        this.jobStatusUpdater = jobStatusUpdater;
        this.storageService = storageService;
    }

    /**
     * Template method that defines the steps to process a background export job.
     * It handles the common workflow of validating the job, loading data, generating the file,
     * uploading it, and updating the job status accordingly. Subclasses are responsible for
     * implementing the specific details of each step.
     *
     * @param job The background job to be processed
     */
    @Override
    public final void process(BackgroundJob job) {
        try {
            jobStatusUpdater.startValidation(job.getId(), "VALIDATING", "Validating export job payload");
            validateJob(job);

            jobStatusUpdater.startProcessing(job.getId(), "PROCESSING", null, "Loading report data");
            ReportDataSet dataSet = loadDataSet(job);
            long totalRows = dataSet.getRows() == null ? 0L : dataSet.getRows().size();

            jobStatusUpdater.updateProgress(
                    job.getId(),
                    BackgroundJobStatus.PROCESSING,
                    "PROCESSING",
                    60,
                    totalRows,
                    totalRows,
                    "Report data prepared"
            );

            jobStatusUpdater.markGeneratingFile(job.getId(), "GENERATING_FILE", "Generating export file");
            GeneratedExportFile generatedFile = generateFile(job, dataSet);

            jobStatusUpdater.markUploading(job.getId(), "UPLOADING", "Uploading generated file");
            FileUploadResponse uploadedFile = storageService.uploadFile(
                    generatedFile.getContent(),
                    generatedFile.getFileName(),
                    generatedFile.getContentType(),
                    resolveExportFolder(job)
            );

            jobStatusUpdater.markCompleted(job.getId(), uploadedFile, "Background job completed successfully");
            log.info("Background export processor completed jobId={} file={}", job.getId(), uploadedFile.getObjectName());
        } catch (Exception ex) {
            handleFailure(job, ex);
        }
    }

    /**
     * Validates the background job payload and any necessary preconditions before processing.
     * Subclasses should implement this method to perform specific validation logic relevant to the type of export job.
     *
     * @param job The background job to be validated
     * @throws Exception if validation fails, which will be caught and handled in the process method
     */
    protected abstract void validateJob(BackgroundJob job);

    /**
     * Loads the data set required for generating the export file. Subclasses should implement this method to
     * retrieve and prepare the specific data needed for the export based on the job's parameters.
     *
     * @param job The background job for which the data set is being loaded
     * @return A ReportDataSet containing the data to be used for file generation
     * @throws Exception if there is an error loading the data, which will be caught and handled in the process method
     */
    protected abstract ReportDataSet loadDataSet(BackgroundJob job);

    /**
     * Generates the export file based on the provided data set. Subclasses should implement this method to
     * create the file content, name, and content type according to the specific requirements of the export job.
     *
     * @param job The background job for which the file is being generated
     * @param dataSet The data set that will be used to generate the file content
     * @return A GeneratedExportFile containing the file content, name, and content type
     * @throws Exception if there is an error during file generation, which will be caught and handled in the process method
     */
    protected abstract GeneratedExportFile generateFile(BackgroundJob job, ReportDataSet dataSet);

    /**
     * Resolves the logical folder or path prefix where the generated export file should be uploaded in the storage system.
     * Subclasses should implement this method to determine the appropriate folder based on the job's parameters or type.
     *
     * @param job The background job for which the export folder is being resolved
     * @return A string representing the logical folder or path prefix for file upload
     */
    protected abstract String resolveExportFolder(BackgroundJob job);

    // Getters for dependencies to be used by subclasses if needed
    protected JobStatusUpdater getJobStatusUpdater() {
        return jobStatusUpdater;
    }

    private void handleFailure(BackgroundJob job, Exception ex) {
        String errorCode = ex instanceof BaseException baseException ? baseException.getErrorCode() : "COM_002";
        String errorMessage = ex.getMessage() == null ? "Background job export failed" : ex.getMessage();

        log.error("Background export processor failed for jobId={}", job.getId(), ex);
        jobStatusUpdater.markFailed(job.getId(), errorCode, errorMessage, "PROCESSING");
    }
}
