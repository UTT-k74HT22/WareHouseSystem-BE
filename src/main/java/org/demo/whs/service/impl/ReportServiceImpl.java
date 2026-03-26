package org.demo.whs.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.demo.whs.entity.dto.ReportExportJobPayload;
import org.demo.whs.entity.dto.request.Inventory.InventoryFilterRequest;
import org.demo.whs.entity.enums.ReportExportFormat;
import org.demo.whs.entity.enums.ReportType;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.service.ReportService;
import org.demo.whs.utils.strategy.export.ExportPayload;
import org.demo.whs.utils.strategy.export.ExportStrategy;
import org.demo.whs.utils.strategy.export.GeneratedExportFile;
import org.demo.whs.utils.strategy.report.ReportDataProvider;
import org.demo.whs.utils.strategy.report.ReportDataSet;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private static final String CURRENT_STOCK_TEMPLATE = "reports/current-stock/current-stock-report.jrxml";
    private static final DateTimeFormatter FILE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final DateTimeFormatter DISPLAY_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final List<ReportDataProvider> reportDataProviders;
    private final List<ExportStrategy> exportStrategies;
    private final ObjectMapper objectMapper;

    @Override
    public GeneratedExportFile exportCurrentStockPdf(InventoryFilterRequest filter, String requestedBy) {
        ReportExportJobPayload payload = ReportExportJobPayload.builder()
                .reportType(ReportType.CURRENT_STOCK)
                .format(ReportExportFormat.PDF)
                .filters(toFilterMap(filter))
                .parameters(buildParameters(requestedBy))
                .build();

        ReportDataSet dataSet = resolveReportDataProvider(ReportType.CURRENT_STOCK).loadDataSet(null, payload);

        Map<String, Object> parameters = new LinkedHashMap<>();
        if (dataSet.getParameters() != null) {
            parameters.putAll(dataSet.getParameters());
        }
        parameters.putIfAbsent("templatePath", CURRENT_STOCK_TEMPLATE);

        ExportPayload exportPayload = ExportPayload.builder()
                .baseFileName("current_stock_" + LocalDateTime.now().format(FILE_TIME_FORMAT))
                .headers(dataSet.getHeaders())
                .rows(dataSet.getRows())
                .parameters(parameters)
                .build();

        return resolveExportStrategy(ReportExportFormat.PDF).export(exportPayload);
    }

    private Map<String, Object> toFilterMap(InventoryFilterRequest filter) {
        InventoryFilterRequest safeFilter = filter == null ? new InventoryFilterRequest() : filter;
        return objectMapper.convertValue(safeFilter, new TypeReference<>() {});
    }

    private Map<String, Object> buildParameters(String requestedBy) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("generatedAt", LocalDateTime.now().format(DISPLAY_TIME_FORMAT));
        parameters.put("generatedBy", requestedBy == null || requestedBy.isBlank() ? "system" : requestedBy);
        parameters.put("templatePath", CURRENT_STOCK_TEMPLATE);
        return parameters;
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
}
