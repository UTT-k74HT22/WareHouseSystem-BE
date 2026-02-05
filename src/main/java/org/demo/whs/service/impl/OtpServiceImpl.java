package org.demo.whs.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.Account;
import org.demo.whs.entity.UserProfile;
import org.demo.whs.entity.enums.EmailType;
import org.demo.whs.entity.enums.OtpType;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.repository.AccountRepository;
import org.demo.whs.repository.UserProfileRepository;
import org.demo.whs.service.EmailService;
import org.demo.whs.service.OtpService;
import org.demo.whs.service.RedisService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
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

    private final SecureRandom random = new SecureRandom();

    @Value("${app.otp.ttl-minutes}")
    private long ttlMinutes;

    @Value("${app.otp.resend-limit-seconds}")
    private long resendLimitSeconds;

    @Value("${app.otp.count-ttl-hours}")
    private long countTtlHours;

    @Value("${app.otp.max-send-per-day}")
    private int maxSendPerDay;

    @Override
    public String generateOtpCode(String email, OtpType type) {
        String otp = String.format("%06d", random.nextInt(1_000_000));
        redisService.set(buildOtpKey(email, type), otp, ttlMinutes, TimeUnit.MINUTES);
        return otp;
    }

    @Override
    public void sendOtp(String email, OtpType type) {

        if (type == null) {
            throw new BadRequestException(ErrorCode.OTP_001);
        }

        log.info("Sending OTP | email={} | type={}", email, type);

        if (type == OtpType.REGISTER) {
            UserProfile userProfile = userRepository.findByEmail(email)
                    .orElseThrow(() ->
                            new BadRequestException(
                                    "Account not found for email: " + email,
                                    ErrorCode.OTP_002)
                    );

            String accountId = userProfile.getAccountId();

            Account account = accountRepository.findById(accountId)
                    .orElseThrow(() -> new BadRequestException(
                            "Account not found for profile: " + userProfile.getId(),
                            ErrorCode.OTP_002
                    ));


        }

        String countKey = buildCountKey(email);
        String lastSendKey = buildLastSendKey(email, type);

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

        String otp = generateOtpCode(email, type);

        // Send OTP via email
        String subject = switch (type) {
            case REGISTER -> "Verify Your Email - Warehouse Management System";
            case FORGOT_PASSWORD -> "Reset Your Password - Warehouse Management System";
        };

        String emailContent = String.format(
                "Your OTP code is: %s\n\nThis code will expire in %d minutes.\n\nIf you didn't request this, please ignore this email.",
                otp, ttlMinutes
        );

        mailService.sendSimpleEmail(email, subject, emailContent, EmailType.OTP_VERIFICATION);

        redisService.set(countKey, count + 1, countTtlHours, TimeUnit.HOURS);
        redisService.set(lastSendKey, "sent", resendLimitSeconds, TimeUnit.SECONDS);
    }

    @Override
    public long countSendOtp(String email) {
        Object countVal = redisService.get(buildCountKey(email));
        return countVal == null ? 0 : Long.parseLong(countVal.toString());
    }

    @Override
    public boolean verifyOtp(String email, String otpCode, OtpType type) {
        Object stored = redisService.get(buildOtpKey(email, type));
        return stored != null && stored.toString().equals(otpCode);
    }

    @Override
    public void deleteOtp(String email, OtpType type) {
        redisService.delete(buildOtpKey(email, type));
    }

    // ========= PRIVATE METHODS =========

    private String buildOtpKey(String email, OtpType type) {
        return "otp:%s:%s".formatted(type.name().toLowerCase(), email);
    }

    private String buildCountKey(String email) {
        return "otp_count:%s".formatted(email);
    }

    private String buildLastSendKey(String email, OtpType type) {
        return "otp_last_send:%s:%s".formatted(type.name().toLowerCase(), email);
    }
}
