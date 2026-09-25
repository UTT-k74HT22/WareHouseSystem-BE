package org.demo.whs.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PublicErrorMessageResolverTest {

    private final PublicErrorMessageResolver resolver = new PublicErrorMessageResolver();

    @Test
    void should_PreserveSpecificMeaning_When_KnownEnglishMessageIsProvided() {
        assertThat(resolver.resolve("COM_001", "Source location not found"))
                .isEqualTo("Không tìm thấy vị trí nguồn");
    }

    @Test
    void should_UseErrorCodeMessage_When_EnglishMessageIsUnknown() {
        assertThat(resolver.resolve("PROD_001", "Unknown internal product detail"))
                .isEqualTo("Không tìm thấy sản phẩm");
    }

    @Test
    void should_PreserveVietnameseMessage_When_MessageIsAlreadyLocalized() {
        assertThat(resolver.resolve("COM_001", "Số lượng phải lớn hơn 0"))
                .isEqualTo("Số lượng phải lớn hơn 0");
    }

    @Test
    void should_HideUnknownInternalMessage_When_ErrorCodeIsUnknown() {
        assertThat(resolver.resolve("UNKNOWN", "Database connection refused"))
                .isEqualTo("Hệ thống gặp sự cố, vui lòng thử lại sau");
    }
}
