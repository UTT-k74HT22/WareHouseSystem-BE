package org.demo.whs.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.*;
import org.demo.whs.entity.dto.request.Auth.RegisterRequest;
import org.demo.whs.entity.dto.request.LoginRequest;
import org.demo.whs.entity.dto.request.RefreshTokenRequest;
import org.demo.whs.entity.dto.response.AuthResponse;
import org.demo.whs.entity.dto.response.RefreshTokenResponse;
import org.demo.whs.entity.enums.AccountStatus;
import org.demo.whs.entity.enums.OtpType;
import org.demo.whs.entity.enums.RoleType;
import org.demo.whs.exception.AuthenticationFailedException;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.UnauthorizedException;
import org.demo.whs.mapper.AuthMapper;
import org.demo.whs.mapper.UserProfileMapper;
import org.demo.whs.repository.AccountHasRoleRepository;
import org.demo.whs.repository.AccountRepository;
import org.demo.whs.repository.RoleRepository;
import org.demo.whs.repository.UserProfileRepository;
import org.demo.whs.security.JwtProvider;
import org.demo.whs.service.AuthService;
import org.demo.whs.service.OtpService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
    /**
     * Sau khi hợp lệ sẽ tạo access token và refresh token.
     *
     * @param request thông tin đăng nhập
     * @return thông tin token và thời hạn
     * @throws AuthenticationFailedException nếu thông tin không hợp lệ
     */
    @Override
    public AuthResponse authenticate(LoginRequest request) {
        log.debug("Authentication attempt: {}", request.getUsername());

        Account account = getAccountByUsername(request.getUsername());
        validatePassword(request.getPassword(), account.getPassword());
        validateAccountStatus(account);

        List<String> roles = roleRepository.findRoleNamesByUsername(account.getUsername());

        String accessToken = jwtProvider.buildAccessToken(account, roles);
        String refreshToken = jwtProvider.buildRefreshToken(account);

        log.info("User authenticated: {}", account.getUsername());

        return authMapper.toResponse(
                accessToken,
                refreshToken,
                jwtProvider.getExpirationAccessToken(accessToken),
                jwtProvider.getExpirationRefreshToken(refreshToken),
                null
        );
    }
    /**
     *  @param request chứa refresh token
     *      * @return access token mới
     *      * @throws UnauthorizedException nếu token không hợp lệ
     */
    @Override
    public RefreshTokenResponse refreshToken(RefreshTokenRequest request) {

        String refreshToken = request.getRefreshToken();
        validateRefreshToken(refreshToken);

        String username = jwtProvider.getUsernameFromToken(refreshToken);
        Account account = getAccountByUsername(username);
        validateAccountStatus(account);

        List<String> roles = roleRepository.findRoleNamesByUsername(username);
        String newAccessToken = jwtProvider.buildAccessToken(account, roles);

        log.info("Access token refreshed for user: {}", username);

        return authMapper.toRefreshResponse(
                newAccessToken,
                jwtProvider.getExpirationAccessToken(newAccessToken)
        );
    }
    /**
     * @param request chứa thông tin đăng ký
     * @throws BadRequestException nếu thông tin không hợp lệ
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

        // Sau khi commit thành công mới gửi OTP
        otpService.sendOtp(request.getEmail(), OtpType.REGISTER);

        log.info("User registered successfully: {}", savedAccount.getUsername());
    }
    /**
     * Kiểm tra username và email đã tồn tại hay chưa.
     *
     * @param request thông tin đăng ký
     * @throws BadRequestException nếu thông tin không hợp lệ
     */
    private void validateDuplicateUser(RegisterRequest request) {

        if (accountRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException(AUTH_002);
        }

        if (userProfileRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException(AUTH_003);
        }
    }
    /**
     * Gán vai trò cho tài khoản.
     *
     * @param account tài khoản
     */
    private void assignUserRole(Account account) {

        Role userRole = roleRepository.findByName(RoleType.USER)
                .orElseThrow(() -> new BadRequestException(ROLE_001));

        AccountRoleId accountRoleId = AccountRoleId.builder()
                .accountId(account.getId())
                .roleId(userRole.getId())
                .build();

        accountHasRoleRepository.save(new AccountHasRole(accountRoleId));
    }
    /**
     * Tạo thông tin người dùng.
     *
     * @param request thông tin đăng ký
     * @param account tài khoản
     */
    private void createUserProfile(RegisterRequest request, Account account) {
        UserProfile profile = userProfileMapper.toUserProfile(request, account);
        userProfileRepository.save(profile);
    }

    private void validatePassword(String rawPassword, String encodedPassword) {
        if (!passwordEncoder.matches(rawPassword, encodedPassword)) {
            log.warn("Invalid password attempt");
            throw new AuthenticationFailedException(AUTH_001);
        }
    }
    /**
     * Kiểm tra trạng thái tài khoản.
     *
     * @param account tài khoản
     * @throws AuthenticationFailedException nếu tài khoản không hợp lệ
     */
    private void validateAccountStatus(Account account) {

        switch (account.getStatus()) {
            case INACTIVE -> throw new AuthenticationFailedException(AUTH_004);
            case SUSPENDED -> throw new AuthenticationFailedException(AUTH_007);

            case ACTIVE -> {
                // OK
            }
            default -> throw new AuthenticationFailedException(AUTH_001);
        }
    }
    /**
     Lấy tài khoản theo username.
     * @param username tên đăng nhập
     * @return Account tương ứng
     * @throws AuthenticationFailedException nếu không tồn tại
     * */
    private Account getAccountByUsername(String username) {
        return accountRepository.findByUsername(username)
                .orElseThrow(() -> new AuthenticationFailedException(AUTH_001));
    }
    /**
     * Kiểm tra refresh token hợp lệ và đúng loại.
     *
     * @param token refresh token
     * @throws UnauthorizedException nếu token không hợp lệ
     */
    private void validateRefreshToken(String token) {

        if (!jwtProvider.validateToken(token)) {
            throw new UnauthorizedException(AUTH_006);
        }

        if (!jwtProvider.isRefreshToken(token)) {
            throw new UnauthorizedException(AUTH_006);
        }
    }
}