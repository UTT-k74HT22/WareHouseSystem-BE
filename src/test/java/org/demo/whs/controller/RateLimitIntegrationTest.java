package org.demo.whs.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.demo.whs.entity.dto.request.LoginRequest;
import org.demo.whs.entity.dto.response.AuthResponse;
import org.demo.whs.service.AuthService;
import org.demo.whs.entity.dto.RateLimitDTO;
import org.demo.whs.service.RateLimitService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Rate Limiting functionality
 * Tests the complete flow from HTTP request to rate limit check
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Rate Limiting Integration Tests")
class RateLimitIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private RateLimitService rateLimitService;

    @MockBean
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    @DisplayName("Should allow request and return rate limit headers when under limit")
    void testRateLimit_AllowedRequest_ShouldReturnHeaders() throws Exception {
        // Given
        LoginRequest loginRequest = new LoginRequest("testuser", "password123");

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .expireAccessToken("3600")
                .expireRefreshToken("86400")
                .ip("127.0.0.1")
                .build();

        RateLimitDTO allowedInfo = new RateLimitDTO(
                true,
                4, // 4 remaining
                300, // 5 minutes
                System.currentTimeMillis() / 1000 + 300,
                5 // limit
        );

        when(authService.authenticate(any(LoginRequest.class))).thenReturn(authResponse);
        when(rateLimitService.checkRateLimit(any(), anyString())).thenReturn(allowedInfo);

        // When & Then
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-RateLimit-Limit"))
                .andExpect(header().string("X-RateLimit-Limit", "5"))
                .andExpect(header().exists("X-RateLimit-Remaining"))
                .andExpect(header().string("X-RateLimit-Remaining", "4"))
                .andExpect(header().exists("X-RateLimit-Reset"))
                .andExpect(header().doesNotExist("Retry-After"))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"));
    }

    @Test
    @DisplayName("Should return 429 Too Many Requests when rate limit exceeded")
    void testRateLimit_ExceededLimit_ShouldReturn429() throws Exception {
        // Given
        LoginRequest loginRequest = new LoginRequest("testuser", "password123");

        RateLimitDTO exceededInfo = new RateLimitDTO(
                false,
                0, // no remaining
                180, // retry after 3 minutes
                System.currentTimeMillis() / 1000 + 180,
                5 // limit
        );

        when(rateLimitService.checkRateLimit(any(), anyString())).thenReturn(exceededInfo);

        // When & Then
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("X-RateLimit-Limit"))
                .andExpect(header().string("X-RateLimit-Remaining", "0"))
                .andExpect(header().exists("Retry-After"))
                .andExpect(header().string("Retry-After", "180"))
                .andExpect(jsonPath("$.code").value("RATE_001"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.retryAfter").value(180));
    }

    @Test
    @DisplayName("Should apply different rate limits for different endpoints")
    void testRateLimit_DifferentEndpoints_DifferentLimits() throws Exception {
        // Test login endpoint (5 requests per 5 minutes)
        LoginRequest loginRequest = new LoginRequest("testuser", "password123");

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .expireAccessToken("3600")
                .expireRefreshToken("86400")
                .ip("127.0.0.1")
                .build();

        RateLimitDTO loginRateLimit = new RateLimitDTO(true, 4, 300, System.currentTimeMillis() / 1000 + 300, 5);

        when(authService.authenticate(any(LoginRequest.class))).thenReturn(authResponse);
        when(rateLimitService.checkRateLimit(any(), anyString())).thenReturn(loginRateLimit);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(header().string("X-RateLimit-Limit", "5"));
    }

    @Test
    @DisplayName("Should use IP address as identifier for IP-based rate limiting")
    void testRateLimit_IpBased_DifferentIps() throws Exception {
        // Given
        LoginRequest loginRequest = new LoginRequest("testuser", "password123");

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .expireAccessToken("3600")
                .expireRefreshToken("86400")
                .ip("192.168.1.100")
                .build();

        RateLimitDTO allowedInfo = new RateLimitDTO(true, 4, 300, System.currentTimeMillis() / 1000 + 300, 5);

        when(authService.authenticate(any(LoginRequest.class))).thenReturn(authResponse);
        when(rateLimitService.checkRateLimit(any(), anyString())).thenReturn(allowedInfo);

        // Request from different IP
        mockMvc.perform(post("/api/v1/auth/login")
                        .with(request -> {
                            request.setRemoteAddr("192.168.1.100");
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-RateLimit-Limit"));
    }

    @Test
    @DisplayName("Should handle X-Forwarded-For header for IP extraction")
    void testRateLimit_XForwardedFor() throws Exception {
        // Given
        LoginRequest loginRequest = new LoginRequest("testuser", "password123");

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .expireAccessToken("3600")
                .expireRefreshToken("86400")
                .ip("203.0.113.1")
                .build();

        RateLimitDTO allowedInfo = new RateLimitDTO(true, 4, 300, System.currentTimeMillis() / 1000 + 300, 5);

        when(authService.authenticate(any(LoginRequest.class))).thenReturn(authResponse);
        when(rateLimitService.checkRateLimit(any(), anyString())).thenReturn(allowedInfo);

        // Request with X-Forwarded-For header
        mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-Forwarded-For", "203.0.113.1, 198.51.100.1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-RateLimit-Limit"));
    }

    @Test
    @DisplayName("Should include custom message in rate limit error response")
    void testRateLimit_CustomMessage() throws Exception {
        // Given
        LoginRequest loginRequest = new LoginRequest("testuser", "password123");

        RateLimitDTO exceededInfo = new RateLimitDTO(false, 0, 300, System.currentTimeMillis() / 1000 + 300, 5);

        when(rateLimitService.checkRateLimit(any(), anyString())).thenReturn(exceededInfo);

        // When & Then
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value("Too many login attempts. Please try again after 5 minutes."));
    }
}
