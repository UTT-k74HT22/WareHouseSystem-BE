package org.demo.whs.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.demo.whs.entity.dto.RateLimitDTO;
import org.demo.whs.entity.enums.RateLimitType;
import org.demo.whs.service.RateLimitService;
import org.demo.whs.utils.annotation.RateLimit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitFilter Unit Tests")
class RateLimitFilterTest {

    @Mock
    private RateLimitService rateLimitService;

    @Mock
    private RequestMappingHandlerMapping handlerMapping;

    private RateLimitFilter rateLimitFilter;
    private HandlerExecutionChain refreshTokenExecutionChain;

    @BeforeEach
    void setUp() throws NoSuchMethodException {
        rateLimitFilter = new RateLimitFilter(rateLimitService, handlerMapping, new ObjectMapper());
        HandlerMethod refreshHandlerMethod = new HandlerMethod(
                new TestAuthController(),
                TestAuthController.class.getMethod("refreshToken")
        );
        refreshTokenExecutionChain = new HandlerExecutionChain(refreshHandlerMethod);
    }

    @Test
    @DisplayName("should_UseDifferentBucketKeys_When_RefreshTokensAreDifferent")
    void should_UseDifferentBucketKeys_When_RefreshTokensAreDifferent() throws Exception {
        when(handlerMapping.getHandler(any())).thenReturn(refreshTokenExecutionChain);
        when(rateLimitService.checkRateLimit(any(), anyString(), anyString()))
                .thenReturn(new RateLimitDTO(true, 9, 10, System.currentTimeMillis() / 1000 + 60, null));

        MockHttpServletRequest request1 = buildRefreshTokenRequest("203.0.113.10", "refresh-token-user-1");
        MockHttpServletRequest request2 = buildRefreshTokenRequest("203.0.113.10", "refresh-token-user-2");

        rateLimitFilter.doFilter(request1, new MockHttpServletResponse(), new MockFilterChain());
        rateLimitFilter.doFilter(request2, new MockHttpServletResponse(), new MockFilterChain());

        ArgumentCaptor<String> identifierCaptor = ArgumentCaptor.forClass(String.class);
        verify(rateLimitService, times(2)).checkRateLimit(any(), identifierCaptor.capture(), anyString());

        List<String> identifiers = identifierCaptor.getAllValues();
        assertThat(identifiers).hasSize(2);
        assertThat(identifiers.get(0)).isNotEqualTo(identifiers.get(1));
        assertThat(identifiers.get(0)).startsWith("refresh-ip:203.0.113.10:token:");
        assertThat(identifiers.get(1)).startsWith("refresh-ip:203.0.113.10:token:");
    }

    @Test
    @DisplayName("should_UseIpFallbackKey_When_RefreshTokenIsMissing")
    void should_UseIpFallbackKey_When_RefreshTokenIsMissing() throws Exception {
        when(handlerMapping.getHandler(any())).thenReturn(refreshTokenExecutionChain);
        when(rateLimitService.checkRateLimit(any(), anyString(), anyString()))
                .thenReturn(new RateLimitDTO(true, 9, 10, System.currentTimeMillis() / 1000 + 60, null));

        MockHttpServletRequest request = buildRefreshTokenRequest("203.0.113.20", "");
        request.setContent("{}".getBytes(StandardCharsets.UTF_8));

        rateLimitFilter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        ArgumentCaptor<String> identifierCaptor = ArgumentCaptor.forClass(String.class);
        verify(rateLimitService).checkRateLimit(any(), identifierCaptor.capture(), anyString());
        assertThat(identifierCaptor.getValue()).isEqualTo("refresh-ip:203.0.113.20");
    }

    private MockHttpServletRequest buildRefreshTokenRequest(String remoteIp, String refreshToken) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/refresh-token");
        request.setRemoteAddr(remoteIp);
        request.setContentType("application/json");
        String body = "{\"refresh_token\":\"" + refreshToken + "\"}";
        request.setContent(body.getBytes(StandardCharsets.UTF_8));
        return request;
    }

    private static class TestAuthController {
        @RateLimit(
                key = "refresh-token",
                limit = 10,
                duration = 60,
                type = RateLimitType.USER
        )
        public void refreshToken() {
        }
    }
}
