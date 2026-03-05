package org.demo.whs.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ErrorCode enum tests")
class ErrorCodeTest {

    @Test
    @DisplayName("All error code values should be unique")
    void should_HaveUniqueCodeValues_When_EnumeratingAllErrorCodes() {
        Set<String> uniqueCodes = Arrays.stream(ErrorCode.values())
                .map(ErrorCode::getCode)
                .collect(Collectors.toSet());

        assertThat(uniqueCodes).hasSameSizeAs(ErrorCode.values());
    }

    @Test
    @DisplayName("AUTH_010 should map to AUTH_010 value")
    void should_MapAuth010ToUniqueValue_When_ReadingAuth010() {
        assertThat(ErrorCode.AUTH_010.getCode()).isEqualTo("AUTH_010");
    }
}
