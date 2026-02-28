package org.demo.whs.mapper;

import org.demo.whs.entity.enums.OtpType;
import org.springframework.stereotype.Component;

@Component
public class OtpMapper {
    private static final String OTP_PREFIX = "otp";
    private static final String OTP_COUNT_PREFIX = "otp_count";
    private static final String OTP_LAST_SEND_PREFIX = "otp_last_send";

    public String buildOtpKey(String email, OtpType type) {
        return "%s:%s:%s".formatted(
                OTP_PREFIX,
                type.name().toLowerCase(),
                email
        );
    }

    public String buildCountKey(String email) {
        return "%s:%s".formatted(OTP_COUNT_PREFIX, email);
    }

    public String buildLastSendKey(String email, OtpType type) {
        return "%s:%s:%s".formatted(
                OTP_LAST_SEND_PREFIX,
                type.name().toLowerCase(),
                email
        );
    }
}

