package org.demo.whs.utils.strategy.export;

import org.demo.whs.entity.enums.ReportExportFormat;

public interface ExportStrategy {

    ReportExportFormat getFormat();

    default boolean supports(ReportExportFormat format) {
        return getFormat() == format;
    }

    GeneratedExportFile export(ExportPayload payload);
}
