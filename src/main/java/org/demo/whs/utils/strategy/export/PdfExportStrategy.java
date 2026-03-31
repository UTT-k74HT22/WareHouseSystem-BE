package org.demo.whs.utils.strategy.export;

import lombok.RequiredArgsConstructor;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import org.demo.whs.entity.enums.ReportExportFormat;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.helpers.report.JasperReportTemplateResolver;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class PdfExportStrategy implements ExportStrategy {

    private final JasperReportTemplateResolver templateResolver;

    @Override
    public ReportExportFormat getFormat() {
        return ReportExportFormat.PDF;
    }

    @Override
    public GeneratedExportFile export(ExportPayload payload) {
        String templatePath = resolveTemplatePath(payload);
        JasperReport jasperReport = templateResolver.resolve(templatePath);

        try {
            JasperPrint jasperPrint = JasperFillManager.fillReport(
                    jasperReport,
                    payload.getParameters() == null ? Map.of() : payload.getParameters(),
                    buildDataSource(payload.getRows())
            );

            byte[] pdfContent = JasperExportManager.exportReportToPdf(jasperPrint);
            return GeneratedExportFile.builder()
                    .fileName(payload.getBaseFileName() + ".pdf")
                    .contentType("application/pdf")
                    .extension("pdf")
                    .content(pdfContent)
                    .build();
        } catch (JRException exception) {
            throw new IllegalStateException("Failed to export PDF report", exception);
        }
    }

    private JRDataSource buildDataSource(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return new JREmptyDataSource(1);
        }

        List<Map<String, ?>> normalizedRows = new ArrayList<>(rows);
        return new JRMapCollectionDataSource(normalizedRows);
    }

    private String resolveTemplatePath(ExportPayload payload) {
        if (payload == null || payload.getParameters() == null) {
            throw new BadRequestException("Missing templatePath for PDF export", ErrorCode.JOB_008);
        }

        Object templatePath = payload.getParameters().get("templatePath");
        if (!(templatePath instanceof String path) || path.isBlank()) {
            throw new BadRequestException("Missing templatePath for PDF export", ErrorCode.JOB_008);
        }

        return path;
    }
}
