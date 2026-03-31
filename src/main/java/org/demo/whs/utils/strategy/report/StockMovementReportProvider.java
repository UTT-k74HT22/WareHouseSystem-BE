package org.demo.whs.utils.strategy.report;

import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.BackgroundJob;
import org.demo.whs.entity.dto.ReportExportJobPayload;
import org.demo.whs.entity.enums.ReportType;
import org.demo.whs.exception.NotImplementedException;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class StockMovementReportProvider implements ReportDataProvider {

    @Override
    public ReportType getReportType() {
        return ReportType.STOCK_MOVEMENT;
    }

    @Override
    public ReportDataSet loadDataSet(BackgroundJob job, ReportExportJobPayload payload) {
        log.info("Stock movement report provider invoked for jobCode={}", job.getJobCode());
        throw new NotImplementedException("Stock movement report query is scaffolded but not implemented yet");
    }
}
