package org.demo.whs.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.dto.request.Auth.*;
import org.demo.whs.entity.dto.request.Permission.CheckPermissionRequest;
import org.demo.whs.entity.dto.response.Auth.ForgotPasswordResponse;
import org.demo.whs.entity.dto.response.Permission.CheckPermissionResponse;
import org.demo.whs.service.PermissionService;
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
import org.springframework.web.bind.annotation.*;
import org.demo.whs.entity.dto.request.Auth.ChangePassWordRequest;

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
    @RateLimit(
        key = "login",
        limit = 5,
        duration = 300, // 5 phút
        type = RateLimitType.IP,
        message = "Too many login attempts. Please try again.",
        failClosed = true  // CRITICAL: Block requests nếu Redis down
    )
    public ResponseEntity<BaseResponse<AuthResponse>> login(@RequestBody @Valid LoginRequest request, HttpServletRequest httpRequest) {
        log.debug("Login attempt for username: {}", request.getUsername());

        // Extract client IP
        String clientIp = getClientIp(httpRequest);
        log.debug("Client IP: {}", clientIp);

        AuthResponse authResponse = authService.authenticate(request, clientIp);
        log.info("User logged in successfully: {} from IP: {}", request.getUsername(), clientIp);
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
        message = "Too many token refresh requests. Please try again later.",
        failClosed = true  // Security-sensitive endpoint: block when Redis is unavailable
    )
    public ResponseEntity<BaseResponse<RefreshTokenResponse>> refreshToken(@RequestBody @Valid RefreshTokenRequest request) {
        log.debug("Refresh token request received");
        RefreshTokenResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /**
     * Extract client IP address from HTTP request.
     * Supports both direct connections and proxied requests.
     *
     * @param request HTTP request
     * @return Client IP address
     */
    private String getClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();

        // Check for X-Forwarded-For header (proxy/load balancer)
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(forwardedFor)) {
            // X-Forwarded-For format: client, proxy1, proxy2
            // Take the first IP (original client)
            return forwardedFor.split(",")[0].trim();
        }

        // Check for X-Real-IP header (nginx)
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isEmpty() && !"unknown".equalsIgnoreCase(realIp)) {
            return realIp;
        }

        // Default to remote address
        return remoteAddr;
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

    /**
     * Endpoint for initiating forgot password process.
     * Rate limited: 3 requests per 15 minutes per IP
     */
    @PostMapping("/forgot-password")
    @RateLimit(
            key = "forgot-password",
            limit = 3,
            duration = 900,
            type = RateLimitType.IP,
            message = "Too many forgot password requests. Please try again after 15 minutes."
    )
    public ResponseEntity<BaseResponse<String>> forgotPassword(@RequestBody @Valid ForgotPasswordRequest request) {
        log.debug("Forgot password request for email: {}", request.getEmail());
        authService.forgotPassword(request.getEmail());
        return ResponseEntity.ok(BaseResponse.success("OTP sent to your email"));
    }

    /**
     * Endpoint for verifying forgot password OTP.
     * Rate limited: 5 attempts per 15 minutes per IP
     */
    @PostMapping("/verify-forgot-password-otp")
    @RateLimit(
            key = "verify-forgot-password-otp",
            limit = 5,
            duration = 900,
            type = RateLimitType.IP,
            message = "Too many verification attempts. Please try again after 15 minutes."
    )
    public ResponseEntity<BaseResponse<ForgotPasswordResponse>> verifyForgotPasswordOtp(@RequestBody @Valid VerifyForgotPasswordRequest request) {
        log.debug("Verifying forgot password OTP for email: {}", request.getEmail());
        ForgotPasswordResponse response = authService.verifyForgotPasswordOtp(request.getEmail(), request.getOtp());
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /**
     * Endpoint for resetting password using the current authenticated session.
     * Rate limited: 3 attempts per 15 minutes per IP
     */
    @PostMapping("/reset-password")
    @RateLimit(
            key = "reset-password",
            limit = 3,
            duration = 900,
            type = RateLimitType.IP,
            message = "Too many password reset attempts. Please try again after 15 minutes."
    )
    public ResponseEntity<BaseResponse<String>> resetPassword(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody @Valid ResetPasswordRequest request) {
        log.debug("Resetting password using token session");
        authService.resetPassword(authHeader, request.getNewPassword());
        return ResponseEntity.ok(BaseResponse.success("Password reset successfully"));
    }
    /**
     * Endpoint for changing password using the current authenticated session.
     */
    @PostMapping("/change-password")
    public ResponseEntity<BaseResponse<String>> changePassWord(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody @Valid ChangePassWordRequest request){
        authService.changePassword(request);
        return ResponseEntity.ok(BaseResponse.success("Password changed successfully"));
    }

    /**
     * Check permission after authentication
     * @param request resource and action
     * @return
     */
    @PostMapping("/check-permission")
    public ResponseEntity<BaseResponse<Boolean>> checkPermission(
            @Valid @RequestBody CheckPermissionRequest request) {
        boolean allowed = authService.checkPermission(
                request.getResource(),
                request.getAction()
        );

        return ResponseEntity.ok(BaseResponse.success(allowed));
    }
}
