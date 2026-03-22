package org.demo.whs.utils.strategy.export;

import org.demo.whs.entity.enums.ReportExportFormat;
import org.demo.whs.exception.NotImplementedException;
import org.springframework.stereotype.Component;

@Component
public class PdfExportStrategy implements ExportStrategy {

    @Override
    public ReportExportFormat getFormat() {
        return ReportExportFormat.PDF;
    }

    @Override
    public GeneratedExportFile export(ExportPayload payload) {
        throw new NotImplementedException("PDF export strategy is scaffolded but not implemented yet");
    }
}
