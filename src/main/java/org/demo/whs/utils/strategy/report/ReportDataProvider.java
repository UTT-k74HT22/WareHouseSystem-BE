package org.demo.whs.utils.strategy.report;

import org.demo.whs.entity.BackgroundJob;
import org.demo.whs.entity.dto.ReportExportJobPayload;
import org.demo.whs.entity.enums.ReportType;

/**
 * ReportDataProvider: Interface for providing report data based on report type
 * NOTE: Lấy dữ liệu để export báo cáo, mỗi loại báo cáo sẽ có một implementation khác nhau trả về dataset phù hợp với cấu trúc của báo cáo đó
 * Không generate file ở đây, chỉ trả về dataset, phần export file sẽ do một component khác xử lý chung cho tất cả các loại báo cáo
 */
public interface ReportDataProvider {

    ReportType getReportType();

    default boolean supports(ReportType reportType) {
        return getReportType() == reportType;
    }

    ReportDataSet loadDataSet(BackgroundJob job, ReportExportJobPayload payload);
}
