package org.demo.whs.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.dto.request.Auth.RegisterRequest;
import org.demo.whs.utils.annotation.RateLimit;
import org.demo.whs.entity.dto.request.LoginRequest;
import org.demo.whs.entity.dto.request.RefreshTokenRequest;
import org.demo.whs.entity.dto.response.AuthResponse;
import org.demo.whs.entity.dto.response.BaseResponse;
import org.demo.whs.entity.dto.response.RefreshTokenResponse;
import org.demo.whs.entity.enums.RateLimitType;
import org.demo.whs.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for handling authentication-related endpoints.
 * API Version: v1
 */
@RequestMapping("/api/v1/auth")
@RestController
@RequiredArgsConstructor
@Slf4j
@Validated
public class AuthController {

    private final AuthService authService;

    /**
     * Endpoint for user login.
     * Rate limited: 5 requests per 5 minutes per IP address to prevent brute force attacks
     * FAIL-CLOSED: Block all requests nếu Redis down (security-critical endpoint)
     *
     * @param request the login request containing user credentials
     * @return a response entity containing the authentication response
     */
    @PostMapping("/login")
//    @RateLimit(
//        key = "login",
//        limit = 5,
//        duration = 300, // 5 phút
//        type = RateLimitType.IP,
//        message = "Too many login attempts. Please try again after 5 minutes.",
//        failClosed = true  // CRITICAL: Block requests nếu Redis down
//    )
    public ResponseEntity<BaseResponse<AuthResponse>> login(@RequestBody @Valid LoginRequest request) {
        log.debug("Login attempt for username: {}", request.getUsername());
        AuthResponse authResponse = authService.authenticate(request);
        log.info("User logged in successfully: {}", request.getUsername());
        return ResponseEntity.ok(BaseResponse.success(authResponse));
    }

    /**
     * Endpoint for refreshing access token.
     * Rate limited: 10 requests per minute per user to prevent token abuse
     *
     * @param request the refresh token request
     * @return a response entity containing the new access token
     */
    @PostMapping("/refresh-token")
    @RateLimit(
        key = "refresh-token",
        limit = 10,
        duration = 60,
        type = RateLimitType.USER,
        message = "Too many token refresh requests. Please try again later."
    )
    public ResponseEntity<BaseResponse<RefreshTokenResponse>> refreshToken(@RequestBody @Valid RefreshTokenRequest request) {
        log.debug("Refresh token request received");
        RefreshTokenResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /**
     * Endpoint for user registration.
     * Creates a new user account with the provided registration details.
     *
     * @param request the registration request containing user details
     * @return a response entity containing the registration success message
     */
    @PostMapping("/register")
    public ResponseEntity<BaseResponse<String>> register(@RequestBody @Valid RegisterRequest request) {
        log.debug("Register attempt for username: {}", request.getUsername());
        authService.register(request);
        return ResponseEntity.ok(BaseResponse.success("User registered successfully"));
    }
}
