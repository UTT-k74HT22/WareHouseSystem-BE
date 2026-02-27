package org.demo.whs.service;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.distributed.proxy.RemoteBucketBuilder;
import org.demo.whs.entity.dto.RateLimitDTO;
import org.demo.whs.entity.enums.RateLimitType;
import org.demo.whs.utils.annotation.RateLimit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.annotation.Annotation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitService Unit Tests - Bucket4j")
class RateLimitServiceTest {

    @Mock
    private ProxyManager<String> proxyManager;

    @Mock
    private RemoteBucketBuilder<String> builder;

    @Mock
    private BucketProxy bucket;

    @InjectMocks
    private RateLimitService rateLimitService;

    private RateLimit mockAnnotation;

    @Mock
    private ConsumptionProbe probe;
    @BeforeEach
    void setUp() {

        mockAnnotation = new RateLimit() {
            @Override public String key() { return "test-key"; }
            @Override public int limit() { return 10; }
            @Override public int duration() { return 60; }
            @Override public RateLimitType type() { return RateLimitType.IP; }
            @Override public String message() { return "Rate limit exceeded"; }
            @Override public boolean failClosed() { return false; }
            @Override public Class<? extends Annotation> annotationType() { return RateLimit.class; }
        };
    }

    @Test
    @DisplayName("Should allow request when token available")
    void shouldAllowRequest() {

        when(proxyManager.builder()).thenReturn(builder);

        doReturn(bucket)
                .when(builder)
                .build(anyString(), any(BucketConfiguration.class));

        when(bucket.tryConsumeAndReturnRemaining(1))
                .thenReturn(probe);

        when(probe.isConsumed()).thenReturn(true);
        when(probe.getRemainingTokens()).thenReturn(9L);

        RateLimitDTO result = rateLimitService.checkRateLimit(
                mockAnnotation,
                "192.168.1.1",
                "POST:/api/test"
        );

        assertThat(result).isNotNull();
        assertThat(result.isAllowed()).isTrue();
        assertThat(result.getRemaining()).isEqualTo(9);
        assertThat(result.getLimit()).isEqualTo(10);
        assertThat(result.getRetryAfter()).isNull();

        verify(bucket).tryConsumeAndReturnRemaining(1);
    }

    @Test
    @DisplayName("Should reject request when tokens exhausted")
    void shouldRejectRequest() {

        when(proxyManager.builder()).thenReturn(builder);
        doReturn(bucket)
                .when(builder)
                .build(anyString(), any(BucketConfiguration.class));


        // 30 seconds wait (30e9 nanoseconds)
        when(probe.isConsumed()).thenReturn(false);
        when(probe.getNanosToWaitForRefill()).thenReturn(30_000_000_000L);

        when(bucket.tryConsumeAndReturnRemaining(1))
                .thenReturn(probe);

        RateLimitDTO result = rateLimitService.checkRateLimit(
                mockAnnotation,
                "192.168.1.1",
                "POST:/api/test"
        );

        assertThat(result).isNotNull();
        assertThat(result.isAllowed()).isFalse();
        assertThat(result.getRemaining()).isZero();
        assertThat(result.getRetryAfter()).isEqualTo(30L);
        assertThat(result.getLimit()).isEqualTo(10);
    }

    @Test
    @DisplayName("Should fallback allow when Redis error and failOpen")
    void shouldFallbackAllow() {

        when(proxyManager.builder()).thenReturn(builder);
        when(builder.build(anyString(), any(BucketConfiguration.class)))
                .thenThrow(new RuntimeException("Redis down"));

        RateLimitDTO result = rateLimitService.checkRateLimit(
                mockAnnotation,
                "192.168.1.1",
                "POST:/api/test"
        );

        assertThat(result).isNotNull();
        assertThat(result.isAllowed()).isTrue();
        assertThat(result.getRemaining()).isEqualTo(10);
    }

    @Test
    @DisplayName("Should fallback reject when Redis error and failClosed=true")
    void shouldFallbackRejectWhenFailClosed() {

        RateLimit failClosedAnnotation = new RateLimit() {
            @Override public String key() { return "test-key"; }
            @Override public int limit() { return 10; }
            @Override public int duration() { return 60; }
            @Override public RateLimitType type() { return RateLimitType.IP; }
            @Override public String message() { return "Rate limit exceeded"; }
            @Override public boolean failClosed() { return true; }
            @Override public Class<? extends Annotation> annotationType() { return RateLimit.class; }
        };

        when(proxyManager.builder()).thenReturn(builder);
        when(builder.build(anyString(), any(BucketConfiguration.class)))
                .thenThrow(new RuntimeException("Redis down"));

        RateLimitDTO result = rateLimitService.checkRateLimit(
                failClosedAnnotation,
                "192.168.1.1",
                "POST:/api/test"
        );

        assertThat(result).isNotNull();
        assertThat(result.isAllowed()).isFalse();
        assertThat(result.getRemaining()).isZero();
        assertThat(result.getRetryAfter()).isEqualTo(60L);
    }
}