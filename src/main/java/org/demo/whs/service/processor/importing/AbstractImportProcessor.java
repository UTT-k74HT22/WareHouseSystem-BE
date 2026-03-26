package org.demo.whs.service.processor.importing;

import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.BackgroundJob;
import org.demo.whs.entity.enums.BackgroundJobStatus;
import org.demo.whs.exception.BaseException;
import org.demo.whs.service.JobStatusUpdater;
import org.demo.whs.utils.strategy.importing.ImportParseResult;
import org.demo.whs.utils.strategy.importing.ImportParserStrategy;

import java.io.InputStream;

@Slf4j
public abstract class AbstractImportProcessor {

    private final JobStatusUpdater jobStatusUpdater;

    protected AbstractImportProcessor(JobStatusUpdater jobStatusUpdater) {
        this.jobStatusUpdater = jobStatusUpdater;
    }

    public final void process(BackgroundJob job, InputStream inputStream) {
        try {
            jobStatusUpdater.startValidation(job.getId(), "VALIDATING", "Validating import file");
            validateInput(job, inputStream);

            jobStatusUpdater.startProcessing(job.getId(), "PROCESSING", null, "Parsing import file");
            ImportParserStrategy parserStrategy = resolveParser(job);
            ImportParseResult parseResult = parserStrategy.parse(inputStream);

            long totalRows = parseResult.getRows() == null ? 0L : parseResult.getRows().size();
            jobStatusUpdater.updateProgress(
                    job.getId(),
                    BackgroundJobStatus.PROCESSING,
                    "PROCESSING",
                    60,
                    totalRows,
                    totalRows,
                    "Import file parsed"
            );

            validateRows(job, parseResult);
            persistRows(job, parseResult);
        } catch (Exception ex) {
            String errorCode = ex instanceof BaseException baseException ? baseException.getErrorCode() : "COM_002";
            String errorMessage = ex.getMessage() == null ? "Background job import failed" : ex.getMessage();

            log.error("Background import processor failed for jobId={}", job.getId(), ex);
            jobStatusUpdater.markFailed(job.getId(), errorCode, errorMessage, "PROCESSING");
        }
    }

    protected abstract void validateInput(BackgroundJob job, InputStream inputStream);

    protected abstract ImportParserStrategy resolveParser(BackgroundJob job);

    protected abstract void validateRows(BackgroundJob job, ImportParseResult parseResult);

    protected abstract void persistRows(BackgroundJob job, ImportParseResult parseResult);
}
