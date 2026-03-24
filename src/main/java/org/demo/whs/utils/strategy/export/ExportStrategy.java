package org.demo.whs.utils.strategy.export;

import org.demo.whs.entity.enums.ReportExportFormat;

/**
 * ExportStrategy: Interface for exporting report data to different formats (e.g., CSV, Excel, PDF)
 * NOTE: Mỗi định dạng file sẽ có một implementation khác nhau để export dataset thành file tương ứng
 * Ví dụ: CSVExportStrategy sẽ export dataset thành file CSV, ExcelExportStrategy sẽ export dataset thành file Excel, v.v.
 * Phần load dataset sẽ do ReportDataProvider xử lý, phần ExportStrategy chỉ tập trung vào việc export dataset thành file
 */
public interface ExportStrategy {

    ReportExportFormat getFormat();

    default boolean supports(ReportExportFormat format) {
        return getFormat() == format;
    }

    GeneratedExportFile export(ExportPayload payload);
}
