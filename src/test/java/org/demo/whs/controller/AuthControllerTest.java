package org.demo.whs.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.demo.whs.entity.dto.request.LoginRequest;
import org.demo.whs.entity.dto.request.RefreshTokenRequest;
import org.demo.whs.entity.dto.response.AuthResponse;
import org.demo.whs.entity.dto.response.RefreshTokenResponse;
import org.demo.whs.exception.AuthenticationFailedException;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.exception.GlobalExceptionHandle;
import org.demo.whs.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Import({GlobalExceptionHandle.class})
@ImportAutoConfiguration(exclude = {DataSourceAutoConfiguration.class, FlywayAutoConfiguration.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private org.demo.whs.service.RateLimitService rateLimitService;

    // ================= LOGIN TESTS =================
    @Nested
    @DisplayName("Login Tests")
    class LoginTests {

        @Test
        @DisplayName("Login success - verify IP passed correctly")
        void login_Success_WithIp() throws Exception {

            AuthResponse response = AuthResponse.builder()
                    .accessToken("access-token-123")
                    .refreshToken("refresh-token-456")
                    .expireAccessToken("3600")
                    .expireRefreshToken("86400")
                    .ip("127.0.0.1")
                    .build();

            when(authService.authenticate(any(LoginRequest.class), any()))
                    .thenReturn(response);

            LoginRequest request = new LoginRequest("testuser", "password123");

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.access_token").value("access-token-123"))
                    .andExpect(jsonPath("$.data.ip").value("127.0.0.1"))
                    .andExpect(jsonPath("$.success").value(true));

            // Verify IP được truyền vào service
            ArgumentCaptor<String> ipCaptor = ArgumentCaptor.forClass(String.class);

            verify(authService).authenticate(any(LoginRequest.class), ipCaptor.capture());

            String capturedIp = ipCaptor.getValue();
            assert capturedIp != null;
        }

        @Test
        @DisplayName("Login fail - invalid credentials")
        void login_Fail_InvalidCredentials() throws Exception {

            when(authService.authenticate(any(LoginRequest.class), any()))
                    .thenThrow(new AuthenticationFailedException(ErrorCode.AUTH_001));

            LoginRequest request = new LoginRequest("wronguser", "wrongpass");

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Login fail - validation error")
        void login_Fail_Validation() throws Exception {

            LoginRequest request = new LoginRequest("", "");

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Login success - X-Forwarded-For header")
        void login_Success_WithForwardedHeader() throws Exception {

            AuthResponse response = AuthResponse.builder()
                    .accessToken("access-token-123")
                    .refreshToken("refresh-token-456")
                    .expireAccessToken("3600")
                    .expireRefreshToken("86400")
                    .ip("192.168.1.100")
                    .build();

            when(authService.authenticate(any(LoginRequest.class), any()))
                    .thenReturn(response);

            LoginRequest request = new LoginRequest("testuser", "password123");

            mockMvc.perform(post("/api/v1/auth/login")
                            .header("X-Forwarded-For", "192.168.1.100")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.ip").value("192.168.1.100"));
        }
    }

    // ================= REFRESH TOKEN TESTS =================
    @Nested
    @DisplayName("Refresh Token Tests")
    class RefreshTokenTests {

        @Test
        @DisplayName("Refresh success")
        void refresh_Success() throws Exception {

            RefreshTokenResponse response =
                    new RefreshTokenResponse("new-access-token", "3600");

            when(authService.refreshToken(any(RefreshTokenRequest.class)))
                    .thenReturn(response);

            RefreshTokenRequest request =
                    new RefreshTokenRequest("valid-refresh-token");

            mockMvc.perform(post("/api/v1/auth/refresh-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.access_token").value("new-access-token"))
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("Refresh fail - invalid token")
        void refresh_Fail_InvalidToken() throws Exception {

            when(authService.refreshToken(any()))
                    .thenThrow(new AuthenticationFailedException(ErrorCode.AUTH_006));

            RefreshTokenRequest request =
                    new RefreshTokenRequest("invalid-token");

            mockMvc.perform(post("/api/v1/auth/refresh-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }
}