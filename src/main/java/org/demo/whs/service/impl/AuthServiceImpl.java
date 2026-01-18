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
import org.demo.whs.mapper.AuthMapper;
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
    private final AuthMapper authMapper;

    @Override
    public AuthResponse authenticate(LoginRequest request) {
        log.debug("Authentication attempt for username: {}", request.getUsername());

        // Find account
        Account account = getAccount(request.getUsername());
        // Verify password
        validAccount(request, account);
        // Load user roles
        List<String> roles = roleRepository.findRoleNamesByUsername(account.getUsername());
        // Generate tokens
        String accessToken = jwtProvider.buildAccessToken(account, roles);
        String refreshToken = jwtProvider.buildRefreshToken(account);
        String expireAccessToken = jwtProvider.getExpirationAccessToken(accessToken);
        String expireRefreshToken = jwtProvider.getExpirationRefreshToken(refreshToken);
        log.info("User authenticated successfully: {}", request.getUsername());
        return authMapper.toResponse(accessToken, expireAccessToken, refreshToken, expireRefreshToken, null);
    }

    private void validAccount(LoginRequest request, Account account) {
        if (!passwordEncoder.matches(request.getPassword(), account.getPassword())) {
            log.warn("Authentication failed - invalid password for username: {}", request.getUsername());
            throw new AuthenticationFailedException(AUTH_001);
        }

        checkStatus(account);
    }

    private static void checkStatus(Account account) {
        if (account.getStatus() == AccountStatus.INACTIVE) {
            log.warn("Authentication failed - account inactive: {}", account.getUsername());
            throw new AuthenticationFailedException(AUTH_004);
        }
    }

    private Account getAccount(String userName) {
        return accountRepository.findByUsername(userName)
                .orElseThrow(() -> {
                    log.warn("Authentication failed - username not found: {}", userName);
                    return new AuthenticationFailedException(AUTH_001);
                });
    }

    @Override
    public RefreshTokenResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        log.debug("Attempting to refresh token");
        // Validate refresh token
        validRefreshToken(refreshToken);
        String username = jwtProvider.getUsernameFromToken(refreshToken);
        // Get account and generate new access token
        Account account = getAccount(username);
        // Check account status
        checkStatus(account);
        // Load roles
        List<String> roles = roleRepository.findRoleNamesByUsername(username);
        // Generate new access token
        String newAccessToken = jwtProvider.buildAccessToken(account, roles);
        String expireAccessToken = jwtProvider.getExpirationAccessToken(newAccessToken);

        log.info("Access token refreshed successfully for user: {}", username);
        return authMapper.toRefreshResponse(newAccessToken, expireAccessToken);
    }

    private void validRefreshToken(String refreshToken) {
        if (jwtProvider.validateToken(refreshToken)) {
            log.warn("Invalid refresh token provided");
            throw new UnauthorizedException(AUTH_006);
        }

        if (!jwtProvider.isRefreshToken(refreshToken)) {
            log.warn("Provided token is not a refresh token");
            throw new UnauthorizedException(AUTH_006);
        }
    }
}
