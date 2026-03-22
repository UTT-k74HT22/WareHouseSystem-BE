package org.demo.whs.utils.strategy.importing;

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
public class ImportParseResult {

    @Builder.Default
    private List<String> headers = List.of();

    @Builder.Default
    private List<Map<String, String>> rows = List.of();
}
