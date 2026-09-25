package org.demo.whs.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ErrorCode enum tests")
class ErrorCodeTest {

    @Test
    @DisplayName("Only warehouse alias entries should share the same raw code value")
    void should_AllowOnlyExpectedWarehouseAliases_When_EnumeratingAllErrorCodes() {
        Map<String, List<String>> namesByCode = Arrays.stream(ErrorCode.values())
                .collect(Collectors.groupingBy(
                        ErrorCode::getCode,
                        Collectors.mapping(ErrorCode::name, Collectors.toList())
                ));

        Map<String, List<String>> duplicateNamesByCode = namesByCode.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        assertThat(duplicateNamesByCode).containsExactlyInAnyOrderEntriesOf(Map.of(
                "WHS_001", List.of("WHS_001", "WH_001"),
                "WHS_002", List.of("WHS_002", "WH_002"),
                "WHS_003", List.of("WHS_003", "WH_003"),
                "WHS_004", List.of("WHS_004", "WH_004")
        ));
    }

    @Test
    @DisplayName("AUTH_010 should map to AUTH_010 value")
    void should_MapAuth010ToUniqueValue_When_ReadingAuth010() {
        assertThat(ErrorCode.AUTH_010.getCode()).isEqualTo("AUTH_010");
    }
}
