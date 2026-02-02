package org.demo.whs.service;

import org.demo.whs.entity.dto.request.Auth.RegisterRequest;
import org.demo.whs.entity.dto.request.LoginRequest;
import org.demo.whs.entity.dto.request.RefreshTokenRequest;
import org.demo.whs.entity.dto.response.AuthResponse;
import org.demo.whs.entity.dto.response.RefreshTokenResponse;

/**
 * Service Interface for managing authentication and authorization.
 */
public interface AuthService {

    /**
     * Authenticates a user based on the provided login request.
     *
     * @param request the login request containing user credentials
     * @return an authentication response containing tokens and related information
     */
    AuthResponse authenticate(LoginRequest request);

    /**
     * Refreshes the access token using a valid refresh token.
     *
     * @param request the refresh token request
     * @return a new access token and its expiration
     */
    RefreshTokenResponse refreshToken(RefreshTokenRequest request);
    /**
     * Registers a new user based on the provided registration request.
     *
     * @param request the registration request containing user details
     */
    void register(RegisterRequest request);
}
