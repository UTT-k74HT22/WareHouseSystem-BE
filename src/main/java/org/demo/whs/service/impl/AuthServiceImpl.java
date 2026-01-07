package org.demo.whs.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.Account;
import org.demo.whs.entity.dto.request.LoginRequest;
import org.demo.whs.entity.dto.request.RefreshTokenRequest;
import org.demo.whs.entity.dto.response.AuthResponse;
import org.demo.whs.entity.dto.response.RefreshTokenResponse;
import org.demo.whs.entity.enums.AccountStatus;
import org.demo.whs.exception.AuthenticationFailedException;
import org.demo.whs.exception.UnauthorizedException;
import org.demo.whs.repository.AccountRepository;
import org.demo.whs.repository.RoleRepository;
import org.demo.whs.security.JwtProvider;
import org.demo.whs.service.AuthService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;
import static org.demo.whs.exception.ErrorCode.*;

/**
 * Service Implementation for managing authentication and authorization.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AccountRepository accountRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    @Override
    public AuthResponse authenticate(LoginRequest request) {
        log.debug("Authentication attempt for username: {}", request.getUsername());

        // Find account
        Account account = accountRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> {
                    log.warn("Authentication failed - username not found: {}", request.getUsername());
                    // Don't reveal whether username exists or not
                    return new AuthenticationFailedException(AUTH_001);
                });

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), account.getPassword())) {
            log.warn("Authentication failed - invalid password for username: {}", request.getUsername());
            // Don't reveal that username exists but password is wrong
            throw new AuthenticationFailedException(AUTH_001);
        }

        // Check account status
        if (account.getStatus() == AccountStatus.INACTIVE) {
            log.warn("Authentication failed - account inactive: {}", request.getUsername());
            throw new AuthenticationFailedException(AUTH_004);
        }

        if (account.getStatus() == AccountStatus.SUSPENDED) {
            log.warn("Authentication failed - account suspended: {}", request.getUsername());
            throw new AuthenticationFailedException(AUTH_007);
        }

        if (account.getStatus() == AccountStatus.DELETED) {
            log.warn("Authentication failed - account deleted: {}", request.getUsername());
            throw new AuthenticationFailedException(AUTH_001);
        }

        // Load user roles
        List<String> roles = roleRepository.findRoleNamesByUsername(account.getUsername());

        // Generate tokens
        String accessToken = jwtProvider.buildAccessToken(account, roles);
        String refreshToken = jwtProvider.buildRefreshToken(account);
        String expireAccessToken = jwtProvider.getExpirationAccessToken(accessToken);
        String expireRefreshToken = jwtProvider.getExpirationRefreshToken(refreshToken);

        log.info("User authenticated successfully: {}", request.getUsername());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expireAccessToken(expireAccessToken)
                .expireRefreshToken(expireRefreshToken)
                .build();
    }

    @Override
    public RefreshTokenResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        log.debug("Attempting to refresh token");

        // Validate refresh token
        if (jwtProvider.validateToken(refreshToken)) {
            log.warn("Invalid refresh token provided");
            throw new UnauthorizedException(AUTH_006);
        }

        // Check if it's actually a refresh token
        if (!jwtProvider.isRefreshToken(refreshToken)) {
            log.warn("Provided token is not a refresh token");
            throw new UnauthorizedException(AUTH_006);
        }
        String username = jwtProvider.getUsernameFromToken(refreshToken);

        // Get account and generate new access token
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedException(AUTH_002));

        // Check account status
        if (account.getStatus() != AccountStatus.ACTIVE) {
            log.warn("Cannot refresh token - account not active: {}", username);
            throw new AuthenticationFailedException(AUTH_004);
        }

        // Load roles
        List<String> roles = roleRepository.findRoleNamesByUsername(username);

        // Generate new access token
        String newAccessToken = jwtProvider.buildAccessToken(account, roles);
        String expireAccessToken = jwtProvider.getExpirationAccessToken(newAccessToken);

        log.info("Access token refreshed successfully for user: {}", username);

        return RefreshTokenResponse.builder()
                .accessToken(newAccessToken)
                .expireAccessToken(expireAccessToken)
                .build();
    }
}
