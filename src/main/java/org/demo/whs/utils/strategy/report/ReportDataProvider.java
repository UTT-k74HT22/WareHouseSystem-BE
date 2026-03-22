package org.demo.whs.utils.strategy.report;

import org.demo.whs.entity.BackgroundJob;
import org.demo.whs.entity.dto.ReportExportJobPayload;
import org.demo.whs.entity.enums.ReportType;

public interface ReportDataProvider {

    ReportType getReportType();

    default boolean supports(ReportType reportType) {
        return getReportType() == reportType;
    }

    ReportDataSet loadDataSet(BackgroundJob job, ReportExportJobPayload payload);
}
