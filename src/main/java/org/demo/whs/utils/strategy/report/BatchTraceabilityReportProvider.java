package org.demo.whs.utils.strategy.report;

import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.BackgroundJob;
import org.demo.whs.entity.dto.ReportExportJobPayload;
import org.demo.whs.entity.enums.ReportType;
import org.demo.whs.exception.NotImplementedException;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class BatchTraceabilityReportProvider implements ReportDataProvider {

    @Override
    public ReportType getReportType() {
        return ReportType.BATCH_TRACEABILITY;
    }

    @Override
    public ReportDataSet loadDataSet(BackgroundJob job, ReportExportJobPayload payload) {
        log.info("Batch traceability report provider invoked for jobCode={}", job.getJobCode());
        throw new NotImplementedException("Batch traceability report query is scaffolded but not implemented yet");
    }
}
