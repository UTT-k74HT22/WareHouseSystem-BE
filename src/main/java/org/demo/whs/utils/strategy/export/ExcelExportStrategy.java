package org.demo.whs.utils.strategy.export;

import org.demo.whs.entity.enums.ReportExportFormat;
import org.demo.whs.exception.NotImplementedException;
import org.springframework.stereotype.Component;

@Component
public class ExcelExportStrategy implements ExportStrategy {

    @Override
    public ReportExportFormat getFormat() {
        return ReportExportFormat.XLSX;
    }

    @Override
    public GeneratedExportFile export(ExportPayload payload) {
        throw new NotImplementedException("Excel export strategy is scaffolded but not implemented yet");
    }
}
