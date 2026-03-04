package org.demo.whs.interceptor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RateLimitInterceptor registration tests")
class RateLimitInterceptorRegistrationTest {

    @Test
    @DisplayName("Deprecated interceptor should not be a Spring component")
    void should_NotBeSpringComponent_When_FilterIsSourceOfTruth() {
        assertThat(RateLimitInterceptor.class.isAnnotationPresent(Component.class)).isFalse();
    }
}
