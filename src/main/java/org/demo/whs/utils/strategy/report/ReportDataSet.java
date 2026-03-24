package org.demo.whs.utils.strategy.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportDataSet {

    private String reportName;
    private String sheetName;

    @Builder.Default
    private List<String> headers = List.of();

    @Builder.Default
    private List<Map<String, Object>> rows = List.of();

    @Builder.Default
    private Map<String, Object> parameters = Map.of();
}
