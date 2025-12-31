package org.demo.whs.service;

import org.demo.whs.entity.dto.request.LoginRequest;
import org.demo.whs.entity.dto.response.AuthResponse;

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

}
