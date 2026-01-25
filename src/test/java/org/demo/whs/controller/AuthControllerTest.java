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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Import({GlobalExceptionHandle.class})
@ImportAutoConfiguration(exclude = {DataSourceAutoConfiguration.class, FlywayAutoConfiguration.class})
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private org.demo.whs.service.RateLimitService rateLimitService;

    @Nested
    @DisplayName("Tests cho Login")
    class LoginTests {

        @Test
        @DisplayName("Case 1: Login thành công")
        void login_Success() throws Exception {
            AuthResponse response = new AuthResponse("access-token-123", "3600", "refresh-token-456", "86400", null);
            LoginRequest validRequest = new LoginRequest("testuser", "password123");
            when(authService.authenticate(any(LoginRequest.class))).thenReturn(response);

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.access_token").value("access-token-123"))
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("Case 2: Login thất bại - Sai thông tin (AUTH_001)")
        void login_Fail_InvalidCredentials() throws Exception {
            LoginRequest validRequest = new LoginRequest("wronguser", "wrongpass");
            when(authService.authenticate(any(LoginRequest.class)))
                    .thenThrow(new AuthenticationFailedException(ErrorCode.AUTH_001));

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Case 3: Login thất bại - Tài khoản bị khóa (AUTH_004)")
        void login_Fail_AccountInactive() throws Exception {
            LoginRequest validRequest = new LoginRequest("inactive_user", "password123");
            when(authService.authenticate(any(LoginRequest.class)))
                    .thenThrow(new AuthenticationFailedException(ErrorCode.AUTH_004));

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Case 4: Login thất bại - Validation (Username trống)")
        void login_Fail_BadRequest() throws Exception {
            LoginRequest invalidRequest = new LoginRequest("", "");

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Tests cho Refresh Token")
    class RefreshTokenTests {

        @Test
        @DisplayName("Case 1: Refresh thành công")
        void refresh_Success() throws Exception {
            RefreshTokenResponse response = new RefreshTokenResponse("new-access-token", "3600");
            RefreshTokenRequest validRequest = new RefreshTokenRequest("valid-refresh-token-12345");
            when(authService.refreshToken(any(RefreshTokenRequest.class))).thenReturn(response);

            mockMvc.perform(post("/api/v1/auth/refresh-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.access_token").value("new-access-token"))
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("Case 2: Refresh thất bại - Token hết hạn hoặc sai")
        void refresh_Fail_InvalidToken() throws Exception {
            RefreshTokenRequest validRequest = new RefreshTokenRequest("invalid-refresh-token");
            when(authService.refreshToken(any(RefreshTokenRequest.class)))
                    .thenThrow(new AuthenticationFailedException(ErrorCode.AUTH_006));

            mockMvc.perform(post("/api/v1/auth/refresh-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }
    }

    //TODO: IMPLEMENT REGISTER TESTS
}