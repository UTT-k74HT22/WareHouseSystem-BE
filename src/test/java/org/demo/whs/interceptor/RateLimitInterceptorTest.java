package org.demo.whs.interceptor;

import org.demo.whs.annotation.RateLimit;
import org.demo.whs.entity.enums.RateLimitType;
import org.demo.whs.exception.RateLimitExceededException;
import org.demo.whs.entity.dto.RateLimitDTO;
import org.demo.whs.service.RateLimitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.method.HandlerMethod;

import java.lang.annotation.Annotation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RateLimitInterceptor
 * Test scenarios:
 * - Request allowed when under limit
 * - Request blocked when limit exceeded
 * - Headers added correctly
 * - Different rate limit types (IP, USER, API, GLOBAL)
 * - IP extraction from various headers
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitInterceptor Unit Tests")
class RateLimitInterceptorTest {

    @Mock
    private RateLimitService rateLimitService;

    @Mock
    private HandlerMethod handlerMethod;

    @InjectMocks
    private RateLimitInterceptor rateLimitInterceptor;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private RateLimit mockRateLimitAnnotation;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();

        // Create mock annotation
        mockRateLimitAnnotation = new RateLimit() {
            @Override
            public String key() {
                return "test-api";
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

        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should allow request when under rate limit")
    void testPreHandle_AllowedRequest() throws Exception {
        // Given
        request.setRemoteAddr("192.168.1.1");
        request.setRequestURI("/api/v1/test");

        RateLimitDTO allowedInfo = new RateLimitDTO(true, 5, 10, System.currentTimeMillis() / 1000 + 45, null);

        when(handlerMethod.getMethodAnnotation(RateLimit.class)).thenReturn(mockRateLimitAnnotation);
        when(rateLimitService.checkRateLimit(eq(mockRateLimitAnnotation), anyString(), anyString())).thenReturn(allowedInfo);

        // When
        boolean result = rateLimitInterceptor.preHandle(request, response, handlerMethod);

        // Then
        assertThat(result).isTrue();
        assertThat(response.getHeader("X-RateLimit-Limit")).isEqualTo("10");
        assertThat(response.getHeader("X-RateLimit-Remaining")).isEqualTo("5");
        assertThat(response.getHeader("X-RateLimit-Reset")).isNotNull();
        assertThat(response.getHeader("Retry-After")).isNull();

        verify(rateLimitService).checkRateLimit(eq(mockRateLimitAnnotation), eq("192.168.1.1"), anyString());
    }

    @Test
    @DisplayName("Should block request when rate limit exceeded")
    void testPreHandle_ExceededLimit() {
        // Given
        request.setRemoteAddr("192.168.1.1");
        request.setRequestURI("/api/v1/test");

        RateLimitDTO exceededInfo = new RateLimitDTO(false, 0, 10, System.currentTimeMillis() / 1000 + 30, 30L);

        when(handlerMethod.getMethodAnnotation(RateLimit.class)).thenReturn(mockRateLimitAnnotation);
        when(rateLimitService.checkRateLimit(eq(mockRateLimitAnnotation), anyString(), anyString())).thenReturn(exceededInfo);

        // When & Then
        assertThatThrownBy(() -> rateLimitInterceptor.preHandle(request, response, handlerMethod))
                .isInstanceOf(RateLimitExceededException.class)
                .hasMessageContaining("Rate limit exceeded");

        // Verify headers are still set
        assertThat(response.getHeader("X-RateLimit-Limit")).isEqualTo("10");
        assertThat(response.getHeader("X-RateLimit-Remaining")).isEqualTo("0");
        assertThat(response.getHeader("Retry-After")).isEqualTo("30");
    }

    @Test
    @DisplayName("Should skip rate limit check when no annotation present")
    void testPreHandle_NoAnnotation() throws Exception {
        // Given
        when(handlerMethod.getMethodAnnotation(RateLimit.class)).thenReturn(null);
        when(handlerMethod.getBeanType()).thenReturn((Class) Object.class);

        // When
        boolean result = rateLimitInterceptor.preHandle(request, response, handlerMethod);

        // Then
        assertThat(result).isTrue();
        verify(rateLimitService, never()).checkRateLimit(any(), any(), any());
    }

    @Test
    @DisplayName("Should extract IP from X-Forwarded-For header")
    void testGetClientIpAddress_XForwardedFor() throws Exception {
        // Given
        request.addHeader("X-Forwarded-For", "203.0.113.1, 198.51.100.1");
        request.setRequestURI("/api/v1/test");

        RateLimitDTO allowedInfo = new RateLimitDTO(true, 5, 10, System.currentTimeMillis() / 1000 + 45, null);

        when(handlerMethod.getMethodAnnotation(RateLimit.class)).thenReturn(mockRateLimitAnnotation);
        when(rateLimitService.checkRateLimit(eq(mockRateLimitAnnotation), eq("203.0.113.1"), anyString())).thenReturn(allowedInfo);

        // When
        boolean result = rateLimitInterceptor.preHandle(request, response, handlerMethod);

        // Then
        assertThat(result).isTrue();
        verify(rateLimitService).checkRateLimit(eq(mockRateLimitAnnotation), eq("203.0.113.1"), anyString());
    }

    @Test
    @DisplayName("Should extract IP from X-Real-IP header")
    void testGetClientIpAddress_XRealIP() throws Exception {
        // Given
        request.addHeader("X-Real-IP", "203.0.113.5");
        request.setRequestURI("/api/v1/test");

        RateLimitDTO allowedInfo = new RateLimitDTO(true, 5, 10, System.currentTimeMillis() / 1000 + 45, null);

        when(handlerMethod.getMethodAnnotation(RateLimit.class)).thenReturn(mockRateLimitAnnotation);
        when(rateLimitService.checkRateLimit(eq(mockRateLimitAnnotation), eq("203.0.113.5"), anyString())).thenReturn(allowedInfo);

        // When
        rateLimitInterceptor.preHandle(request, response, handlerMethod);

        // Then
        verify(rateLimitService).checkRateLimit(eq(mockRateLimitAnnotation), eq("203.0.113.5"), anyString());
    }

    @Test
    @DisplayName("Should use USER type rate limit for authenticated users")
    void testPreHandle_UserType() throws Exception {
        // Given
        RateLimit userAnnotation = createAnnotation("user-api", RateLimitType.USER);

        // Set authenticated user
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("john.doe", null, null);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        request.setRequestURI("/api/v1/user");
        RateLimitDTO allowedInfo = new RateLimitDTO(true, 5, 10, System.currentTimeMillis() / 1000 + 45, null);

        when(handlerMethod.getMethodAnnotation(RateLimit.class)).thenReturn(userAnnotation);
        when(rateLimitService.checkRateLimit(eq(userAnnotation), eq("john.doe"), anyString())).thenReturn(allowedInfo);

        // When
        boolean result = rateLimitInterceptor.preHandle(request, response, handlerMethod);

        // Then
        assertThat(result).isTrue();
        verify(rateLimitService).checkRateLimit(eq(userAnnotation), eq("john.doe"), anyString());
    }

    @Test
    @DisplayName("Should use 'anonymous' for USER type when not authenticated")
    void testPreHandle_UserType_NotAuthenticated() throws Exception {
        // Given
        RateLimit userAnnotation = createAnnotation("user-api", RateLimitType.USER);

        request.setRequestURI("/api/v1/user");
        RateLimitDTO allowedInfo = new RateLimitDTO(true, 5, 10, System.currentTimeMillis() / 1000 + 45, null);

        when(handlerMethod.getMethodAnnotation(RateLimit.class)).thenReturn(userAnnotation);
        when(rateLimitService.checkRateLimit(eq(userAnnotation), eq("anonymous"), anyString())).thenReturn(allowedInfo);

        // When
        rateLimitInterceptor.preHandle(request, response, handlerMethod);

        // Then
        verify(rateLimitService).checkRateLimit(eq(userAnnotation), eq("anonymous"), anyString());
    }

    @Test
    @DisplayName("Should use API type rate limit with request URI")
    void testPreHandle_ApiType() throws Exception {
        // Given
        RateLimit apiAnnotation = createAnnotation("api-endpoint", RateLimitType.API);

        request.setRequestURI("/api/v1/products");
        RateLimitDTO allowedInfo = new RateLimitDTO(true, 5, 10, System.currentTimeMillis() / 1000 + 45, null);

        when(handlerMethod.getMethodAnnotation(RateLimit.class)).thenReturn(apiAnnotation);
        when(rateLimitService.checkRateLimit(eq(apiAnnotation), eq("/api/v1/products"), anyString())).thenReturn(allowedInfo);

        // When
        rateLimitInterceptor.preHandle(request, response, handlerMethod);

        // Then
        verify(rateLimitService).checkRateLimit(eq(apiAnnotation), eq("/api/v1/products"), anyString());
    }

    @Test
    @DisplayName("Should use GLOBAL type rate limit with 'global' identifier")
    void testPreHandle_GlobalType() throws Exception {
        // Given
        RateLimit globalAnnotation = createAnnotation("global-limit", RateLimitType.GLOBAL);

        request.setRequestURI("/api/v1/test");
        RateLimitDTO allowedInfo = new RateLimitDTO(true, 5, 10, System.currentTimeMillis() / 1000 + 45, null);

        when(handlerMethod.getMethodAnnotation(RateLimit.class)).thenReturn(globalAnnotation);
        when(rateLimitService.checkRateLimit(eq(globalAnnotation), eq("global"), anyString())).thenReturn(allowedInfo);

        // When
        rateLimitInterceptor.preHandle(request, response, handlerMethod);

        // Then
        verify(rateLimitService).checkRateLimit(eq(globalAnnotation), eq("global"), anyString());
    }

    @Test
    @DisplayName("Should check class-level annotation when method annotation not present")
    void testPreHandle_ClassLevelAnnotation() throws Exception {
        // Given
        request.setRemoteAddr("192.168.1.1");
        RateLimitDTO allowedInfo = new RateLimitDTO(true, 5, 10, System.currentTimeMillis() / 1000 + 45, null);

        when(handlerMethod.getMethodAnnotation(RateLimit.class)).thenReturn(null);
        when(handlerMethod.getBeanType()).thenReturn((Class) TestController.class);
        when(rateLimitService.checkRateLimit(any(), anyString(), anyString())).thenReturn(allowedInfo);

        // When
        boolean result = rateLimitInterceptor.preHandle(request, response, handlerMethod);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should allow non-HandlerMethod objects")
    void testPreHandle_NotHandlerMethod() throws Exception {
        // Given
        Object nonHandlerMethod = new Object();

        // When
        boolean result = rateLimitInterceptor.preHandle(request, response, nonHandlerMethod);

        // Then
        assertThat(result).isTrue();
        verify(rateLimitService, never()).checkRateLimit(any(), any(), any());
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

    @RateLimit(key = "test-class", limit = 50, duration = 60, type = RateLimitType.IP)
    static class TestController {
        public void testMethod() {
        }
    }
}


