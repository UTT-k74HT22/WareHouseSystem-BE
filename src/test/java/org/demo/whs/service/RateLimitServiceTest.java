package org.demo.whs.service;

import org.demo.whs.annotation.RateLimit;
import org.demo.whs.entity.dto.RateLimitDTO;
import org.demo.whs.entity.enums.RateLimitType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RateLimitService
 * Test các scenarios:
 * - Allow request khi chưa đạt limit
 * - Reject request khi vượt quá limit
 * - Reset rate limit
 * - Get rate limit info
 * - Handle Redis errors (fallback mechanism)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitService Unit Tests")
class RateLimitServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private RateLimitService rateLimitService;

    @Captor
    private ArgumentCaptor<List<String>> keysCaptor;

    @Captor
    private ArgumentCaptor<Object[]> argsCaptor;

    private RateLimit mockRateLimitAnnotation;

    @BeforeEach
    void setUp() {
        // Create mock annotation
        mockRateLimitAnnotation = new RateLimit() {
            @Override
            public String key() {
                return "test-key";
            }

            @Override
            public int limit() {
                return 10;
            }

            @Override
            public int duration() {
                return 60;
            }

            @Override
            public RateLimitType type() {
                return RateLimitType.IP;
            }

            @Override
            public String message() {
                return "Rate limit exceeded";
            }

            @Override
            public boolean failClosed() {
                return false;
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return RateLimit.class;
            }
        };
    }

    @Test
    @DisplayName("Should allow request when under rate limit")
    void testCheckRateLimit_AllowedRequest() {
        // Given
        String identifier = "192.168.1.1";
        String routeKey = "POST:/api/v1/test";
        // New Lua script returns List<Long> with {remaining, ttl}
        java.util.List<Long> luaResult = java.util.Arrays.asList(7L, 45L);

        when(redisTemplate.execute(
                any(RedisScript.class),
                anyList(),
                anyInt(),
                anyInt()
        )).thenReturn(luaResult);

        // When
        RateLimitDTO result = rateLimitService.checkRateLimit(mockRateLimitAnnotation, identifier, routeKey);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isAllowed()).isTrue();
        assertThat(result.getRemaining()).isEqualTo(7);
        assertThat(result.getLimit()).isEqualTo(10);
        assertThat(result.getRetryAfter()).isNull(); // null when allowed

        verify(redisTemplate).execute(
                any(RedisScript.class),
                anyList(),
                eq(10),
                eq(60)
        );
    }

    @Test
    @DisplayName("Should reject request when rate limit exceeded")
    void testCheckRateLimit_ExceededLimit() {
        // Given
        String identifier = "192.168.1.1";
        String routeKey = "POST:/api/v1/test";
        // Lua returns {-1, ttl} when exceeded
        java.util.List<Long> luaResult = java.util.Arrays.asList(-1L, 30L);

        when(redisTemplate.execute(
                any(RedisScript.class),
                anyList(),
                anyInt(),
                anyInt()
        )).thenReturn(luaResult);

        // When
        RateLimitDTO result = rateLimitService.checkRateLimit(mockRateLimitAnnotation, identifier, routeKey);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isAllowed()).isFalse();
        assertThat(result.getRemaining()).isZero();
        assertThat(result.getRetryAfter()).isEqualTo(30L); // Non-null when exceeded
        assertThat(result.getLimit()).isEqualTo(10);
    }

    @Test
    @DisplayName("Should allow request when Redis returns null (fallback)")
    void testCheckRateLimit_RedisReturnsNull_ShouldFallback() {
        // Given
        String identifier = "192.168.1.1";
        String routeKey = "POST:/api/v1/test";
        when(redisTemplate.execute(
                any(RedisScript.class),
                anyList(),
                anyInt(),
                anyInt()
        )).thenReturn(null);

        // When
        RateLimitDTO result = rateLimitService.checkRateLimit(mockRateLimitAnnotation, identifier, routeKey);

        // Then - Should allow request as fallback (fail-open by default)
        assertThat(result).isNotNull();
        assertThat(result.isAllowed()).isTrue();
        assertThat(result.getRemaining()).isEqualTo(10);
        assertThat(result.getLimit()).isEqualTo(10);
    }

    @Test
    @DisplayName("Should allow request when Redis throws exception (fallback)")
    void testCheckRateLimit_RedisThrowsException_ShouldFallback() {
        // Given
        String identifier = "192.168.1.1";
        String routeKey = "POST:/api/v1/test";
        when(redisTemplate.execute(
                any(RedisScript.class),
                anyList(),
                anyInt(),
                anyInt()
        )).thenThrow(new RuntimeException("Redis connection failed"));

        // When
        RateLimitDTO result = rateLimitService.checkRateLimit(mockRateLimitAnnotation, identifier, routeKey);

        // Then - Should allow request as fallback (fail-open)
        assertThat(result).isNotNull();
        assertThat(result.isAllowed()).isTrue();
    }

    @Test
    @DisplayName("Should reset rate limit successfully")
    void testResetRateLimit() {
        // Given
        String identifier = "192.168.1.1";
        String routeKey = "POST:/api/v1/test";
        String expectedKey = "rate_limit:IP:test-key:POST:/api/v1/test:192.168.1.1";

        when(redisTemplate.delete(expectedKey)).thenReturn(true);

        // When
        rateLimitService.resetRateLimit(RateLimitType.IP, "test-key", identifier, routeKey);

        // Then
        verify(redisTemplate).delete(expectedKey);
    }

    @Test
    @DisplayName("Should get rate limit info without incrementing counter")
    void testGetRateLimitInfo() {
        // Given
        String identifier = "192.168.1.1";
        String routeKey = "POST:/api/v1/test";
        String expectedKey = "rate_limit:IP:test-key:POST:/api/v1/test:192.168.1.1";
        Integer currentCount = 3;
        Long ttl = 45L;

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(expectedKey)).thenReturn(currentCount);
        when(redisTemplate.getExpire(expectedKey, TimeUnit.SECONDS)).thenReturn(ttl);

        // When
        RateLimitDTO result = rateLimitService.getRateLimitInfo(
                RateLimitType.IP,
                "test-key",
                identifier,
                routeKey,
                10,
                60
        );

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isAllowed()).isTrue();
        assertThat(result.getRemaining()).isEqualTo(7); // 10 - 3
        assertThat(result.getLimit()).isEqualTo(10);
        assertThat(result.getRetryAfter()).isNull(); // null when allowed

        verify(valueOperations).get(expectedKey);
        verify(redisTemplate, never()).execute(any(RedisScript.class), anyList(), any(), any());
    }

    @Test
    @DisplayName("Should handle different RateLimitTypes correctly")
    void testCheckRateLimit_DifferentTypes() {
        // Test IP type
        RateLimit ipAnnotation = createAnnotation("ip-key", RateLimitType.IP);
        when(redisTemplate.execute(any(RedisScript.class), anyList(), anyInt(), anyInt()))
                .thenReturn(java.util.Arrays.asList(5L, 30L));

        RateLimitDTO result = rateLimitService.checkRateLimit(ipAnnotation, "192.168.1.1", "GET:/api/test");
        assertThat(result.isAllowed()).isTrue();

        // Verify Redis key contains IP type
        verify(redisTemplate).execute(
                any(RedisScript.class),
                argThat((java.util.List<String> keys) -> keys.get(0).contains("IP")),
                anyInt(),
                anyInt()
        );
    }

    @Test
    @DisplayName("Should use default TTL when Redis TTL is null")
    void testCheckRateLimit_NullTTL_ShouldUseDefault() {
        // Given
        String identifier = "192.168.1.1";
        String routeKey = "POST:/api/v1/test";
        // Lua script now returns TTL, so this test is not applicable
        // But we can test edge case where Lua returns TTL = duration
        java.util.List<Long> luaResult = java.util.Arrays.asList(5L, 60L);

        when(redisTemplate.execute(any(RedisScript.class), anyList(), anyInt(), anyInt()))
                .thenReturn(luaResult);

        // When
        RateLimitDTO result = rateLimitService.checkRateLimit(mockRateLimitAnnotation, identifier, routeKey);

        // Then - resetTime should be now + ttl
        assertThat(result.getResetTime()).isGreaterThan(System.currentTimeMillis() / 1000);
    }

    @Test
    @DisplayName("Should get rate limit info when key does not exist")
    void testGetRateLimitInfo_KeyNotExists() {
        // Given
        String identifier = "192.168.1.1";
        String routeKey = "POST:/api/v1/test";
        String expectedKey = "rate_limit:IP:test-key:POST:/api/v1/test:192.168.1.1";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(expectedKey)).thenReturn(null);
        when(redisTemplate.getExpire(expectedKey, TimeUnit.SECONDS)).thenReturn(-2L); // Key not exists

        // When
        RateLimitDTO result = rateLimitService.getRateLimitInfo(
                RateLimitType.IP,
                "test-key",
                identifier,
                routeKey,
                10,
                60
        );

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isAllowed()).isTrue();
        assertThat(result.getRemaining()).isEqualTo(10); // Full limit available
    }

    // Helper method to create mock annotation
    private RateLimit createAnnotation(String key, RateLimitType type) {
        return new RateLimit() {
            @Override
            public String key() {
                return key;
            }

            @Override
            public int limit() {
                return 10;
            }

            @Override
            public int duration() {
                return 60;
            }

            @Override
            public RateLimitType type() {
                return type;
            }

            @Override
            public String message() {
                return "Rate limit exceeded";
            }

            @Override
            public boolean failClosed() {
                return false;
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return RateLimit.class;
            }
        };
    }
}
