package org.demo.whs.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.*;
import org.demo.whs.entity.dto.request.Auth.ChangePassWordRequest;
import org.demo.whs.entity.dto.request.Auth.RegisterRequest;
import org.demo.whs.entity.dto.request.LoginRequest;
import org.demo.whs.entity.dto.request.RefreshTokenRequest;
import org.demo.whs.entity.dto.response.Auth.ForgotPasswordResponse;
import org.demo.whs.entity.dto.response.AuthResponse;
import org.demo.whs.entity.dto.response.Permission.MyPermissionsResponse;
import org.demo.whs.entity.dto.response.RefreshTokenResponse;
import org.demo.whs.entity.enums.AccountStatus;
import org.demo.whs.entity.enums.ActionType;
import org.demo.whs.entity.enums.OtpType;
import org.demo.whs.entity.enums.RoleType;
import org.demo.whs.exception.AuthenticationFailedException;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.exception.UnauthorizedException;
import org.demo.whs.mapper.AuthMapper;
import org.demo.whs.mapper.UserProfileMapper;
import org.demo.whs.repository.AccountHasRoleRepository;
import org.demo.whs.repository.AccountRepository;
import org.demo.whs.repository.RoleRepository;
import org.demo.whs.repository.UserProfileRepository;
import org.demo.whs.security.JwtProvider;
import org.demo.whs.security.SecurityUtils;
import org.demo.whs.service.AuthService;
import org.demo.whs.service.OtpService;
import org.demo.whs.service.PermissionCacheService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static org.demo.whs.exception.ErrorCode.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AccountRepository accountRepository;
    private final UserProfileRepository userProfileRepository;
    private final RoleRepository roleRepository;
    private final AccountHasRoleRepository accountHasRoleRepository;
    private final UserProfileMapper userProfileMapper;
    private final OtpService otpService;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final AuthMapper authMapper;
    private final PermissionCacheService permissionCacheService;

    /**
     * Authenticates a user based on the provided login request.
     */
    @Override
    public AuthResponse authenticate(LoginRequest request, String clientIp) {
        log.debug("Authentication attempt for username: {} from IP: {}",
                request.getUsername(), clientIp);

        Account account = getAccountByUsername(request.getUsername());

        verifyPassword(request.getPassword(), account.getPassword());
        validateAccountStatus(account);

        List<String> roles = roleRepository.findRoleNamesByUsername(account.getUsername());

        String accessToken = jwtProvider.buildAccessToken(account, roles);
        String refreshToken = jwtProvider.buildRefreshToken(account);
        preloadPermissionCache(account.getId());

        log.info("User authenticated successfully: {} from IP: {}",
                account.getUsername(), clientIp);

        return authMapper.toResponse(
                accessToken,
                refreshToken,
                jwtProvider.getExpirationAccessToken(accessToken),
                jwtProvider.getExpirationRefreshToken(refreshToken),
                clientIp
        );
    }
    /**
     * Refreshes the access token using a valid refresh token.
     */
    @Override
    public RefreshTokenResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();
        log.debug("Attempting to refresh token");

        validateRefreshToken(refreshToken);

        String username = jwtProvider.getUsernameFromToken(refreshToken);
        Account account = getAccountByUsername(username);
        validateAccountStatus(account);

        List<String> roles = roleRepository.findRoleNamesByUsername(username);
        String newAccessToken = jwtProvider.buildAccessToken(account, roles);

        log.info("Access token refreshed successfully for user: {}", username);

        return authMapper.toRefreshResponse(
                newAccessToken,
                jwtProvider.getExpirationAccessToken(newAccessToken)
        );
    }

    /**
     * Registers a new user based on the provided registration request.
     */
    @Override
    @Transactional
    public void register(RegisterRequest request) {
        log.info("Registering new user: {}", request.getUsername());

        validateDuplicateUser(request);

        Account account = authMapper.registerAcc(request);
        account.setPassword(passwordEncoder.encode(request.getPassword()));
        account.setStatus(AccountStatus.INACTIVE);

        Account savedAccount = accountRepository.save(account);

        assignUserRole(savedAccount);
        createUserProfile(request, savedAccount);

        otpService.sendOtp(request.getEmail(), OtpType.REGISTER);

        log.info("User registered successfully: {}", savedAccount.getUsername());
    }

    // ================= FORGOT PASSWORD =================
    /**
     * Sends a forgot password OTP to the provided email address.
     */
    @Override
    public void forgotPassword(String email) {
        log.info("Processing forgot password request for email: {}", email);

        boolean userExists = userProfileRepository.existsByEmail(email);
        if (!userExists) {
            log.warn("Forgot password requested for non-existent email: {}", email);
            return;
        }

        otpService.sendOtp(email, OtpType.FORGOT_PASSWORD);
    }
    /**
     * Verifies the forgot password OTP and returns a password reset response.
     */
    @Override
    public ForgotPasswordResponse verifyForgotPasswordOtp(String email, String otp) {
        log.info("Verifying forgot password OTP for email: {}", email);

        boolean isValid = otpService.verifyOtp(email, otp, OtpType.FORGOT_PASSWORD);
        if (!isValid) {
            throw new BadRequestException(OTP_006);
        }

        UserProfile profile = userProfileRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException(OTP_006));

        Account account = accountRepository.findById(profile.getAccountId())
                .orElseThrow(() -> new BadRequestException(OTP_006));

        // Tạo Reset Token chuyên biệt (không chứa roles, chỉ có type=resetPassword)
        String resetToken = jwtProvider.buildResetToken(account);

        log.info("OTP verified. Returning dedicated resetToken for user: {}", account.getUsername());

        return ForgotPasswordResponse.builder()
                .resetToken(resetToken)
                .build();
    }
    /**
     * Resets the user's password using the current authenticated session and its token.
     */
    @Override
    @Transactional
    public void resetPassword(String authHeader, String newPassword) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException(AUTH_005);
        }

        String token = authHeader.substring(7);
        String tokenType = jwtProvider.getTypeFromToken(token);

        if (!"resetPassword".equals(tokenType)) {
            log.warn("[SECURITY] Attempted password reset with invalid token type: {}", tokenType);
            throw new UnauthorizedException(AUTH_005);
        }

        String username = jwtProvider.getUsernameFromToken(token);
        log.info("Processing password reset for user: {}", username);

        if (username == null || username.equalsIgnoreCase("anonymousUser")) {
            throw new UnauthorizedException(AUTH_005);
        }

        Account account = getAccountByUsername(username);

        if (newPassword == null || newPassword.length() < 6) {
            throw new BadRequestException(RESET_003);
        }
        if (passwordEncoder.matches(newPassword, account.getPassword())) {
            throw new BadRequestException(RESET_004);
        }

        account.setPassword(passwordEncoder.encode(newPassword));
        accountRepository.save(account);


        log.info("Password reset successful for user: {}", username);
    }
    /**
     * Changes the user's password using the current authenticated session and its token.
     */
    @Override
    @Transactional
    public void changePassword(ChangePassWordRequest request) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName().equals("anonymousUser")) {
            throw new UnauthorizedException(AUTH_005);
        }

        String username = authentication.getName();
        Account account = getAccountByUsername(username);

        if (request.getNewPassword() == null || request.getNewPassword().length() < 6) {
            throw new BadRequestException(RESET_003);
        }
        if (passwordEncoder.matches(request.getNewPassword(), account.getPassword())) {
            throw new BadRequestException(RESET_004);
        }

        if (!passwordEncoder.matches(request.getOldPassword(), account.getPassword())) {
            throw new BadRequestException(RESET_005);
        }

        account.setPassword(passwordEncoder.encode(request.getNewPassword()));
        accountRepository.save(account);

        log.info("Password changed successfully for user: {}", username);
    }

    @Override
    public boolean checkPermission(String resource, ActionType action) {

        // 🔥 1. Lấy accountId từ SecurityContext
        String accountId = SecurityUtils.getCurrentAccountId();

        if (accountId == null) {
            log.warn("Unauthenticated access attempt");
            throw new UnauthorizedException(ErrorCode.AUTH_005);
        }

        // 🔥 2. Build permission code theo chuẩn hệ thống
        String permissionCode = generatePermissionCode(resource, action);

        // 🔥 3. Lấy permission từ cache
        Set<String> permissions = permissionCacheService.getPermissions(accountId);

        if (permissions == null || permissions.isEmpty()) {
            log.warn("No permissions found for accountId={}", accountId);
            return false;
        }

        boolean allowed = permissions.contains(permissionCode);

        log.info("Check permission: accountId={}, permission={}, allowed={}",
                accountId, permissionCode, allowed);

        return allowed;
    }

    @Override
    public MyPermissionsResponse getMyPermissions() {
        String accountId = SecurityUtils.getCurrentAccountId();

        if (accountId == null) {
            log.warn("Unauthenticated access attempt when loading my permissions");
            throw new UnauthorizedException(ErrorCode.AUTH_005);
        }

        Set<String> permissions = permissionCacheService.getPermissions(accountId);
        List<String> permissionCodes = permissions == null
                ? List.of()
                : permissions.stream()
                        .sorted(Comparator.naturalOrder())
                        .toList();

        log.info("Loaded {} permissions for accountId={}", permissionCodes.size(), accountId);

        return MyPermissionsResponse.builder()
                .permissions(permissionCodes)
                .build();
    }

    // ================= PRIVATE METHODS =================

    private void validateDuplicateUser(RegisterRequest request) {
        if (accountRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException(AUTH_002);
        }

        if (userProfileRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException(AUTH_003);
        }
    }

    private void assignUserRole(Account account) {
        Role userRole = roleRepository.findByName(String.valueOf(RoleType.USER))
                .orElseThrow(() -> new BadRequestException(ROLE_001));

        AccountRoleId accountRoleId = AccountRoleId.builder()
                .accountId(account.getId())
                .roleId(userRole.getId())
                .build();

        accountHasRoleRepository.save(new AccountHasRole(accountRoleId));
    }

    private void createUserProfile(RegisterRequest request, Account account) {
        UserProfile profile = userProfileMapper.toUserProfile(request, account);
        userProfileRepository.save(profile);
    }

    private void verifyPassword(String rawPassword, String encodedPassword) {
        if (!passwordEncoder.matches(rawPassword, encodedPassword)) {
            log.warn("Authentication failed - invalid password");
            throw new AuthenticationFailedException(AUTH_001);
        }
    }

    private void validateAccountStatus(Account account) {
        switch (account.getStatus()) {
            case INACTIVE -> throw new AuthenticationFailedException(AUTH_009);
            case SUSPENDED -> throw new AuthenticationFailedException(AUTH_007);
            case ACTIVE -> { }
            default -> throw new AuthenticationFailedException(AUTH_001);
        }
    }

    private Account getAccountByUsername(String username) {
        return accountRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("Authentication failed - username not found: {}", username);
                    return new AuthenticationFailedException(AUTH_001);
                });
    }

    private void preloadPermissionCache(String accountId) {
        try {
            Set<String> permissions = permissionCacheService.getPermissions(accountId);
            int permissionCount = permissions != null ? permissions.size() : 0;
            log.info("Preloaded {} permissions into cache for accountId={}", permissionCount, accountId);
        } catch (Exception e) {
            log.error("Failed to preload permission cache for accountId={}", accountId, e);
        }
    }

    private void validateRefreshToken(String token) {
        if (!jwtProvider.validateToken(token) || !jwtProvider.isRefreshToken(token)) {
            log.warn("Invalid refresh token");
            throw new UnauthorizedException(AUTH_006);
        }
    }

    /**
     * Generate permission code theo chuẩn hệ thống.
     *
     * Format:
     * PERM_<RESOURCE>_<ACTION>
     *
     * Ví dụ:
     * PERM_USER_CREATE
     */
    private String generatePermissionCode(String resource, ActionType action) {

        if (resource == null || resource.isBlank()) {
            throw new IllegalArgumentException("Resource cannot be null or blank");
        }

        if (action == null) {
            throw new IllegalArgumentException("Action cannot be null");
        }

        return "PERM_" +
                resource.trim().toUpperCase() +
                "_" +
                action.name();
    }
}
