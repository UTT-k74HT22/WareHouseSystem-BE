package org.demo.whs.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.Account;
import org.demo.whs.entity.dto.request.LoginRequest;
import org.demo.whs.entity.dto.response.AuthResponse;
import org.demo.whs.entity.enums.AccountStatus;
import org.demo.whs.exception.BadRequest;
import org.demo.whs.exception.NotFoundException;
import org.demo.whs.repository.AccountRepository;
import org.demo.whs.security.JwtProvider;
import org.demo.whs.service.AuthService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import static org.demo.whs.exception.ErrorCode.*;

/**
 * Service Implementation for managing authentication and authorization.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    @Override
    public AuthResponse authenticate(LoginRequest request) {
        log.info("Authenticating user: {}", request.getUsername());

        Account account = accountRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> {
                    log.error("Authentication failed for user: {}", request.getUsername());
                    return new NotFoundException("Invalid username or password", AUTH_001);
                });

        if (!passwordEncoder.matches(request.getPassword(), account.getPassword())) {
            log.error("Authentication failed for user: {}", request.getUsername());
            throw new NotFoundException("Invalid username or password", AUTH_001);
        }

        if (account.getStatus() != AccountStatus.ACTIVE) {
            log.error("Authentication failed for inactive user: {}", request.getUsername());
            throw new BadRequest("User account is inactive", AUTH_004);
        }

        String accessToken = jwtProvider.buildAccessToken(account);
        String refreshToken = jwtProvider.buildRefreshToken(account.getUsername());
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }
}
