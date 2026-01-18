package org.demo.whs.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.dto.request.LoginRequest;
import org.demo.whs.entity.dto.request.RefreshTokenRequest;
import org.demo.whs.entity.dto.response.AuthResponse;
import org.demo.whs.entity.dto.response.BaseResponse;
import org.demo.whs.entity.dto.response.RefreshTokenResponse;
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
     *
     * @param request the login request containing user credentials
     * @return a response entity containing the authentication response
     */
    @PostMapping("/login")
    public ResponseEntity<BaseResponse<AuthResponse>> login(@RequestBody @Valid LoginRequest request) {
        log.debug("Login attempt for username: {}", request.getUsername());
        AuthResponse authResponse = authService.authenticate(request);
        log.info("User logged in successfully: {}", request.getUsername());
        return ResponseEntity.ok(BaseResponse.success(authResponse));
    }

    /**
     * Endpoint for refreshing access token.
     *
     * @param request the refresh token request
     * @return a response entity containing the new access token
     */
    @PostMapping("/refresh-token")
    public ResponseEntity<BaseResponse<RefreshTokenResponse>> refreshToken(@RequestBody @Valid RefreshTokenRequest request) {
        log.debug("Refresh token request received");
        RefreshTokenResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(BaseResponse.success(response));
    }
}
