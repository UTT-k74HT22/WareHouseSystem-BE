package org.demo.whs.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.Account;
import org.demo.whs.entity.AccountHasRole;
import org.demo.whs.entity.AccountRoleId;
import org.demo.whs.entity.UserProfile;
import org.demo.whs.entity.dto.request.LoginRequest;
import org.demo.whs.entity.dto.request.RefreshTokenRequest;
import org.demo.whs.entity.dto.request.RegisterRequest;
import org.demo.whs.entity.dto.response.AuthResponse;
import org.demo.whs.entity.dto.response.RefreshTokenResponse;
import org.demo.whs.entity.enums.AccountStatus;
import org.demo.whs.entity.enums.RoleType;
import org.demo.whs.exception.AuthenticationFailedException;
import org.demo.whs.exception.BadRequest;
import org.demo.whs.exception.UnauthorizedException;
import org.demo.whs.mapper.AuthMapper;
import org.demo.whs.repository.AccountHasRoleRepository;
import org.demo.whs.repository.AccountRepository;
import org.demo.whs.repository.RoleRepository;
import org.demo.whs.repository.UserProfileRepository;
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
    private final UserProfileRepository userProfileRepository;
    private final AccountHasRoleRepository accountHasRoleRepository;
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

    /**
     * Registers a new user based on the provided registration request.
     *
     * @param request the registration request containing user details
     */
    @Override
    @Transactional
    public Void register(RegisterRequest request) {
        log.info("Registering user: {}", request.getUsername());

        // Validate registration data
        validRegister(request);

        // Create new account
        Account newAccount = authMapper.getNewAccount(request);
        accountRepository.save(newAccount);

        // Get USER role ID from database
        setRelationship(newAccount);

        //TODO: Send activation email

        log.info("User registered successfully: {}", request.getUsername());
        return null;
    }

    private void setRelationship(Account newAccount) {
        String userRoleId = roleRepository.findIdByName(RoleType.USER.toString());
        if (userRoleId == null) {
            log.error("USER role not found in database");
            throw new BadRequest(COM_002);
        }

        // Create account-role relationship
        AccountHasRole accountHasRole = new AccountHasRole();
        accountHasRole.setId(new AccountRoleId(newAccount.getId(), userRoleId));
        accountHasRoleRepository.save(accountHasRole);

        // Create account - user profile relationship (if applicable)
        UserProfile userProfile = new UserProfile();
        userProfile.setAccountId(newAccount.getId());
        userProfileRepository.save(userProfile);
    }

    private void validRegister(RegisterRequest request) {
        if (request.getUsername() == null) {
            log.warn("Registration failed - username is null");
            throw new BadRequest(AUTH_010);
        }

        if (accountRepository.existsByUsername(request.getUsername())) {
            log.warn("Registration failed - username already exists: {}", request.getUsername());
            throw new BadRequest(AUTH_010);
        }

        if (request.getPassword() == null) {
            log.warn("Registration failed - password is null");
            throw new BadRequest(AUTH_010);
        }

        if (request.getPasswordConfirm() == null) {
            log.warn("Registration failed - password confirmation is null");
            throw new BadRequest(AUTH_011);
        }

        if (!request.getPassword().equals(request.getPasswordConfirm())) {
            log.warn("Registration failed - password and confirmation do not match for username: {}", request.getUsername());
            throw new BadRequest(AUTH_012);
        }
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
