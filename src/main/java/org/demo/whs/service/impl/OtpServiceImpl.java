package org.demo.whs.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.Account;
import org.demo.whs.entity.UserProfile;
import org.demo.whs.entity.enums.EmailType;
import org.demo.whs.entity.enums.OtpType;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.mapper.OtpMapper;
import org.demo.whs.repository.AccountRepository;
import org.demo.whs.repository.UserProfileRepository;
import org.demo.whs.service.EmailService;
import org.demo.whs.service.OtpService;
import org.demo.whs.service.RedisService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpServiceImpl implements OtpService {

    private final RedisService redisService;
    private final AccountRepository accountRepository;
    private final EmailService mailService;
    private final UserProfileRepository userRepository;
    private final OtpMapper OtpMapper;

    private final SecureRandom random = new SecureRandom();

    @Value("${app.otp.ttl-minutes}")
    private long ttlMinutes;

    @Value("${app.otp.resend-limit-seconds}")
    private long resendLimitSeconds;

    @Value("${app.otp.count-ttl-hours}")
    private long countTtlHours;

    @Value("${app.otp.max-send-per-day}")
    private int maxSendPerDay;

    // =========================================================
    // PUBLIC METHODS
    // =========================================================

    @Override
    public void sendOtp(String email, OtpType type) {

        validateOtpType(type);
        
        try {
            validateBusiness(email, type);
        } catch (BadRequestException e) {
            if (type == OtpType.FORGOT_PASSWORD) {
                log.warn("[SECURITY] Forgot password OTP requested for non-existent email: {}", email);
                return;
            }
            throw e;
        }
        
        validateRateLimit(email, type);

        // Luôn tạo mã OTP mới để đảm bảo Reset TTL (thời gian hết hạn)
        // Điều này giúp tránh lỗi "mã vừa gửi đã hết hạn"
        String otp = generateOtpCode(email, type);

        sendOtpEmail(email, otp, type);

        increaseSendCount(email);
        markLastSend(email, type);

        log.info("OTP sent successfully | email={} | type={}", email, type);
    }

    @Override
    @Transactional
    public boolean verifyOtp(String email, String otpCode, OtpType type) {

        String key = OtpMapper.buildOtpKey(email, type);
        Object stored = redisService.get(key);

        if (stored == null) {
            return false;
        }

        boolean isValid = stored.toString().equals(otpCode);

        if (isValid) {
            // Nếu là xác thực đăng ký, kích hoạt tài khoản luôn
            if (type == OtpType.REGISTER) {
                activateAccount(email);
            }
            redisService.delete(key); // one-time use
        }

        return isValid;
    }

    private void activateAccount(String email) {
        UserProfile profile = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException(
                        "User profile not found for email: " + email,
                        ErrorCode.OTP_002
                ));

        Account account = accountRepository.findById(profile.getAccountId())
                .orElseThrow(() -> new BadRequestException(
                        "Account not found for id: " + profile.getAccountId(),
                        ErrorCode.OTP_002
                ));

        if (account.getStatus() == org.demo.whs.entity.enums.AccountStatus.INACTIVE) {
            account.setStatus(org.demo.whs.entity.enums.AccountStatus.ACTIVE);
            accountRepository.save(account);
            log.info("Account activated successfully | email={}", email);
        }
    }

    @Override
    public void deleteOtp(String email, OtpType type) {
        redisService.delete(OtpMapper.buildOtpKey(email, type));
    }

    @Override
    public long countSendOtp(String email) {
        Object countVal = redisService.get(OtpMapper.buildCountKey(email));
        return countVal == null ? 0 : Long.parseLong(countVal.toString());
    }

    public String generateOtpCode(String email, OtpType type) {

        String otp = String.format("%06d", random.nextInt(1_000_000));

        redisService.set(
                OtpMapper.buildOtpKey(email, type),
                otp,
                ttlMinutes,
                TimeUnit.MINUTES
        );

        return otp;
    }

    private void validateOtpType(OtpType type) {
        if (type == null) {
            throw new BadRequestException(ErrorCode.OTP_001);
        }
    }

    private void validateBusiness(String email, OtpType type) {
        UserProfile profile = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new BadRequestException(
                                "Account not found for email: " + email,
                                ErrorCode.OTP_002
                        )
                );

        Account account = accountRepository.findById(profile.getAccountId())
                .orElseThrow(() ->
                        new BadRequestException(
                                "Account not found for profile: " + profile.getId(),
                                ErrorCode.OTP_002
                        )
                );

        if (type == OtpType.REGISTER) {
            // Chỉ cho phép gửi lại OTP nếu tài khoản chưa kích hoạt
            if (account.getStatus() != org.demo.whs.entity.enums.AccountStatus.INACTIVE) {
                throw new BadRequestException("Account is already active or unavailable for registration", ErrorCode.AUTH_003);
            }
        }

        if (type == OtpType.FORGOT_PASSWORD) {
            // Chỉ cho phép gửi OTP quên mật khẩu cho tài khoản đang hoạt động
            if (account.getStatus() != org.demo.whs.entity.enums.AccountStatus.ACTIVE) {
                throw new BadRequestException("Account is not active", ErrorCode.AUTH_007);
            }
        }
    }

    private void validateRateLimit(String email, OtpType type) {

        String countKey = OtpMapper.buildCountKey(email);
        String lastSendKey = OtpMapper.buildLastSendKey(email, type);

        long count = Optional.ofNullable(redisService.get(countKey))
                .map(Object::toString)
                .map(Long::parseLong)
                .orElse(0L);

        if (count >= maxSendPerDay) {
            throw new BadRequestException(ErrorCode.OTP_004);
        }

        if (redisService.exists(lastSendKey)) {
            throw new BadRequestException(ErrorCode.OTP_005);
        }
    }

    private void increaseSendCount(String email) {

        String countKey = OtpMapper.buildCountKey(email);

        long count = Optional.ofNullable(redisService.get(countKey))
                .map(Object::toString)
                .map(Long::parseLong)
                .orElse(0L);

        redisService.set(
                countKey,
                count + 1,
                countTtlHours,
                TimeUnit.HOURS
        );
    }

    private void markLastSend(String email, OtpType type) {
        redisService.set(
                OtpMapper.buildLastSendKey(email, type),
                "sent",
                resendLimitSeconds,
                TimeUnit.SECONDS
        );
    }

    private void sendOtpEmail(String email, String otp, OtpType type) {

        String subject = switch (type) {
            case REGISTER -> "Verify Your Email - Warehouse Management System";
            case FORGOT_PASSWORD -> "Reset Your Password - Warehouse Management System";
        };

        Map<String, Object> variables = new HashMap<>();
        variables.put("title", subject);
        variables.put("otp", otp);
        variables.put("ttlMinutes", ttlMinutes);

        mailService.sendTemplateEmail(
                email,
                subject,
                "email/otp-email",
                variables,
                EmailType.OTP_VERIFICATION
        );
    }
}