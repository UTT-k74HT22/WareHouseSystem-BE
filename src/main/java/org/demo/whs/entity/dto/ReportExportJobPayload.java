package org.demo.whs.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.demo.whs.entity.enums.ReportExportFormat;
import org.demo.whs.entity.enums.ReportType;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportExportJobPayload {

    @Builder.Default
    private ReportType reportType = ReportType.CURRENT_STOCK;

    @Builder.Default
    private ReportExportFormat format = ReportExportFormat.CSV;

    @Builder.Default
    private Map<String, Object> filters = Map.of();

    @Builder.Default
    private Map<String, Object> parameters = Map.of();
}
