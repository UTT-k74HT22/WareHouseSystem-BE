package org.demo.whs.service.processor.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.demo.whs.configuration.BackgroundJobProperties;
import org.demo.whs.entity.BackgroundJob;
import org.demo.whs.entity.dto.ReportExportJobPayload;
import org.demo.whs.entity.enums.BackgroundJobType;
import org.demo.whs.entity.enums.ReportExportFormat;
import org.demo.whs.entity.enums.ReportType;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.BaseException;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.service.JobStatusUpdater;
import org.demo.whs.service.StorageService;
import org.demo.whs.utils.strategy.export.ExportPayload;
import org.demo.whs.utils.strategy.export.ExportStrategy;
import org.demo.whs.utils.strategy.export.GeneratedExportFile;
import org.demo.whs.utils.strategy.report.ReportDataProvider;
import org.demo.whs.utils.strategy.report.ReportDataSet;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * ReportExportProcessor is responsible for processing background jobs related to report exports.
 * It validates the job, loads the dataset using the appropriate report data provider, generates the export file using the appropriate export strategy, and handles file storage.
 */
@Component
public class ReportExportProcessor extends AbstractExportProcessor {

    private static final DateTimeFormatter FILE_NAME_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final ObjectMapper objectMapper;
    private final List<ReportDataProvider> reportDataProviders;
    private final List<ExportStrategy> exportStrategies;
    private final BackgroundJobProperties backgroundJobProperties;

    /**
     * Constructor for ReportExportProcessor.
     *
     * @param jobStatusUpdater      the service to update job status
     * @param storageService        the service to handle file storage
     * @param objectMapper          the ObjectMapper for JSON processing
     * @param reportDataProviders   the list of report data providers
     * @param exportStrategies      the list of export strategies
     * @param backgroundJobProperties the background job properties for configuration
     */
    public ReportExportProcessor(JobStatusUpdater jobStatusUpdater, StorageService storageService, ObjectMapper objectMapper, List<ReportDataProvider> reportDataProviders, List<ExportStrategy> exportStrategies, BackgroundJobProperties backgroundJobProperties) {
        super(jobStatusUpdater, storageService);
        this.objectMapper = objectMapper;
        this.reportDataProviders = reportDataProviders;
        this.exportStrategies = exportStrategies;
        this.backgroundJobProperties = backgroundJobProperties;
    }

    /**
     * Determines if this processor supports the given background job.
     *
     * @param job the background job to check
     * @return true if the job type is REPORT_EXPORT or SCHEDULED_REPORT, false otherwise
     */
    @Override
    public boolean supports(BackgroundJob job) {
        return job.getJobType() == BackgroundJobType.REPORT_EXPORT
                || job.getJobType() == BackgroundJobType.SCHEDULED_REPORT;
    }

    /**
     * Validates the background job before processing.
     *
     * @param job the background job to validate
     * @throws BadRequestException if the payload is invalid or required strategies/providers are missing
     */
    @Override
    protected void validateJob(BackgroundJob job) {
        ReportExportJobPayload payload = resolvePayload(job);
        resolveReportDataProvider(payload.getReportType());
        resolveExportStrategy(payload.getFormat());
    }

    /**
     * Loads the dataset for the report export job using the appropriate data provider.
     *
     * @param job the background job for which to load the dataset
     * @return the loaded ReportDataSet
     */
    @Override
    protected ReportDataSet loadDataSet(BackgroundJob job) {
        ReportExportJobPayload payload = resolvePayload(job);
        return resolveReportDataProvider(payload.getReportType()).loadDataSet(job, payload);
    }

    /**
     * Generates the export file using the appropriate export strategy.
     *
     * @param job     the background job for which to generate the file
     * @param dataSet the dataset to be exported
     * @return the generated export file
     */
    @Override
    protected GeneratedExportFile generateFile(BackgroundJob job, ReportDataSet dataSet) {
        ReportExportJobPayload payload = resolvePayload(job);
        ExportStrategy strategy = resolveExportStrategy(payload.getFormat());

        ExportPayload exportPayload = ExportPayload.builder()
                .baseFileName(buildBaseFileName(job, dataSet))
                .headers(dataSet.getHeaders())
                .rows(dataSet.getRows())
                .parameters(dataSet.getParameters())
                .build();

        return strategy.export(exportPayload);
    }

    /**
     * Resolves the export folder path from the background job properties.
     *
     * @param job the background job for which to resolve the export folder
     * @return the export folder path
     */
    @Override
    protected String resolveExportFolder(BackgroundJob job) {
        return backgroundJobProperties.getExportFolder();
    }

    private ReportExportJobPayload resolvePayload(BackgroundJob job) {
        try {
            ReportExportJobPayload payload = job.getRequestPayload() == null || job.getRequestPayload().isBlank()
                    ? ReportExportJobPayload.builder().build()
                    : objectMapper.readValue(job.getRequestPayload(), ReportExportJobPayload.class);

            if (payload.getReportType() == null) {
                payload.setReportType(resolveReportTypeFromBusinessType(job.getBusinessType()));
            }

            if (payload.getFormat() == null) {
                payload.setFormat(ReportExportFormat.CSV);
            }

            return payload;
        } catch (BaseException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BadRequestException("Invalid background job payload", ErrorCode.JOB_008);
        }
    }

    private ReportType resolveReportTypeFromBusinessType(String businessType) {
        try {
            return ReportType.valueOf(businessType);
        } catch (Exception ex) {
            throw new BadRequestException("Unsupported report type: " + businessType, ErrorCode.JOB_007);
        }
    }

    private ReportDataProvider resolveReportDataProvider(ReportType reportType) {
        return reportDataProviders.stream()
                .filter(provider -> provider.supports(reportType))
                .findFirst()
                .orElseThrow(() -> new BadRequestException(
                        "No report provider found for report type " + reportType,
                        ErrorCode.JOB_007
                ));
    }

    private ExportStrategy resolveExportStrategy(ReportExportFormat format) {
        return exportStrategies.stream()
                .filter(strategy -> strategy.supports(format))
                .findFirst()
                .orElseThrow(() -> new BadRequestException(
                        "No export strategy found for format " + format,
                        ErrorCode.JOB_006
                ));
    }

    private String buildBaseFileName(BackgroundJob job, ReportDataSet dataSet) {
        String baseName = dataSet.getReportName() == null || dataSet.getReportName().isBlank()
                ? (job.getBusinessType() == null ? "report" : job.getBusinessType().toLowerCase())
                : dataSet.getReportName().trim().replaceAll("\\s+", "_").toLowerCase();

        return baseName + "_" + LocalDateTime.now().format(FILE_NAME_TIME_FORMAT);
    }
}
