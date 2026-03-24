package org.demo.whs.utils.strategy.export;

import org.demo.whs.entity.enums.ReportExportFormat;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class CsvExportStrategy implements ExportStrategy {

    @Override
    public ReportExportFormat getFormat() {
        return ReportExportFormat.CSV;
    }

    @Override
    public GeneratedExportFile export(ExportPayload payload) {
        List<String> headers = resolveHeaders(payload);
        List<Map<String, Object>> rows = payload.getRows() == null ? List.of() : payload.getRows();
        StringBuilder builder = new StringBuilder();

        if (!headers.isEmpty()) {
            builder.append(String.join(",", headers.stream().map(this::escape).toList())).append(System.lineSeparator());
        }

        for (Map<String, Object> row : rows) {
            List<String> values = new ArrayList<>();
            for (String header : headers) {
                Object value = row == null ? null : row.get(header);
                values.add(escape(value == null ? "" : String.valueOf(value)));
            }
            builder.append(String.join(",", values)).append(System.lineSeparator());
        }

        return GeneratedExportFile.builder()
                .fileName(payload.getBaseFileName() + ".csv")
                .contentType("text/csv")
                .extension("csv")
                .content(builder.toString().getBytes(StandardCharsets.UTF_8))
                .build();
    }

    private List<String> resolveHeaders(ExportPayload payload) {
        if (payload.getHeaders() != null && !payload.getHeaders().isEmpty()) {
            return payload.getHeaders();
        }

        if (payload.getRows() == null || payload.getRows().isEmpty() || payload.getRows().get(0) == null) {
            return List.of();
        }

        return new ArrayList<>(payload.getRows().get(0).keySet());
    }

    private String escape(String value) {
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }
}
