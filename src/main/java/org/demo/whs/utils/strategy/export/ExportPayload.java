package org.demo.whs.utils.strategy.export;

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
public class ExportPayload {

    private String baseFileName;

    @Builder.Default
    private List<String> headers = List.of();

    @Builder.Default
    private List<Map<String, Object>> rows = List.of();

    @Builder.Default
    private Map<String, Object> parameters = Map.of();
}
