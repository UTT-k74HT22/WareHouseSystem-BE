package org.demo.whs.service.impl;

import org.demo.whs.entity.Account;
import org.demo.whs.entity.UserProfile;
import org.demo.whs.entity.enums.AccountStatus;
import org.demo.whs.entity.enums.OtpType;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.mapper.OtpMapper;
import org.demo.whs.repository.AccountRepository;
import org.demo.whs.repository.UserProfileRepository;
import org.demo.whs.service.EmailService;
import org.demo.whs.service.RedisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OtpServiceImpl Unit Tests")
class OtpServiceImplTest {

    @Mock
    private RedisService redisService;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private EmailService mailService;

    @Mock
    private UserProfileRepository userRepository;

    @Spy
    private OtpMapper otpMapper = new OtpMapper();

    @InjectMocks
    private OtpServiceImpl otpService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(otpService, "ttlMinutes", 5L);
        ReflectionTestUtils.setField(otpService, "resendLimitSeconds", 60L);
        ReflectionTestUtils.setField(otpService, "countTtlHours", 24L);
        ReflectionTestUtils.setField(otpService, "maxSendPerDay", 5);
    }

    private UserProfile buildProfile(String accountId, String email) {
        return UserProfile.builder()
                .email(email)
                .accountId(accountId)
                .build();
    }

    private Account buildAccount(String id, AccountStatus status) {
        Account a = Account.builder().status(status).build();
        a.setId(id);
        return a;
    }

    @Nested
    @DisplayName("sendOtp tests")
    class SendOtpTests {

        @Test
        @DisplayName("should_ThrowBadRequestException_When_OtpTypeIsNull")
        void should_ThrowBadRequestException_When_OtpTypeIsNull() {
            assertThatThrownBy(() -> otpService.sendOtp("user@test.com", null))
                    .isInstanceOf(BadRequestException.class)
                    .matches(e -> ((BadRequestException) e).getErrorCode().equals(ErrorCode.OTP_001.getCode()));
        }

        @Test
        @DisplayName("should_ThrowBadRequestException_When_ExceedMaxSendPerDay")
        void should_ThrowBadRequestException_When_ExceedMaxSendPerDay() {
            // Arrange: email exists, account active, but count >= maxSendPerDay
            UserProfile profile = buildProfile("acc-1", "user@test.com");
            Account account = buildAccount("acc-1", AccountStatus.ACTIVE);

            when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(profile));
            when(accountRepository.findById("acc-1")).thenReturn(Optional.of(account));
            // count = 5 >= maxSendPerDay = 5
            when(redisService.get(contains("otp_count"))).thenReturn("5");

            // Act & Assert
            assertThatThrownBy(() -> otpService.sendOtp("user@test.com", OtpType.FORGOT_PASSWORD))
                    .isInstanceOf(BadRequestException.class)
                    .matches(e -> ((BadRequestException) e).getErrorCode().equals(ErrorCode.OTP_004.getCode()));

            verify(mailService, never()).sendTemplateEmail(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("should_ThrowBadRequestException_When_ResendTooSoon")
        void should_ThrowBadRequestException_When_ResendTooSoon() {
            // Arrange
            UserProfile profile = buildProfile("acc-1", "user@test.com");
            Account account = buildAccount("acc-1", AccountStatus.ACTIVE);

            when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(profile));
            when(accountRepository.findById("acc-1")).thenReturn(Optional.of(account));
            // count < max, but lastSend key exists
            when(redisService.get(contains("otp_count"))).thenReturn("2");
            when(redisService.exists(contains("otp_last_send"))).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> otpService.sendOtp("user@test.com", OtpType.FORGOT_PASSWORD))
                    .isInstanceOf(BadRequestException.class)
                    .matches(e -> ((BadRequestException) e).getErrorCode().equals(ErrorCode.OTP_005.getCode()));
        }

        @Test
        @DisplayName("should_SendOtpSuccessfully_When_RegisterAndAccountInactive")
        void should_SendOtpSuccessfully_When_RegisterAndAccountInactive() {
            // Arrange
            UserProfile profile = buildProfile("acc-1", "reg@test.com");
            Account account = buildAccount("acc-1", AccountStatus.INACTIVE);

            when(userRepository.findByEmail("reg@test.com")).thenReturn(Optional.of(profile));
            when(accountRepository.findById("acc-1")).thenReturn(Optional.of(account));
            when(redisService.get(contains("otp_count"))).thenReturn(null);
            when(redisService.exists(contains("otp_last_send"))).thenReturn(false);
            doNothing().when(redisService).set(anyString(), any(), anyLong(), any());
            doNothing().when(mailService).sendTemplateEmail(any(), any(), any(), any(), any());

            // Act
            otpService.sendOtp("reg@test.com", OtpType.REGISTER);

            // Assert
            verify(mailService).sendTemplateEmail(
                    eq("reg@test.com"), any(), eq("email/otp-email"), any(), any());
        }

        @Test
        @DisplayName("should_SilentlyReturn_When_ForgotPasswordAndEmailNotFound")
        void should_SilentlyReturn_When_ForgotPasswordAndEmailNotFound() {
            // For FORGOT_PASSWORD, OTP_002 is swallowed to prevent email enumeration
            when(userRepository.findByEmail("ghost@test.com")).thenReturn(Optional.empty());

            // Act - should NOT throw
            otpService.sendOtp("ghost@test.com", OtpType.FORGOT_PASSWORD);

            // Assert
            verify(mailService, never()).sendTemplateEmail(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("should_ThrowBadRequestException_When_RegisterAndAccountAlreadyActive")
        void should_ThrowBadRequestException_When_RegisterAndAccountAlreadyActive() {
            // If account is ACTIVE, REGISTER OTP must be rejected
            UserProfile profile = buildProfile("acc-2", "active@test.com");
            Account account = buildAccount("acc-2", AccountStatus.ACTIVE);

            when(userRepository.findByEmail("active@test.com")).thenReturn(Optional.of(profile));
            when(accountRepository.findById("acc-2")).thenReturn(Optional.of(account));

            assertThatThrownBy(() -> otpService.sendOtp("active@test.com", OtpType.REGISTER))
                    .isInstanceOf(BadRequestException.class)
                    .matches(e -> ((BadRequestException) e).getErrorCode().equals(ErrorCode.AUTH_003.getCode()));
        }
    }

    @Nested
    @DisplayName("verifyOtp tests")
    class VerifyOtpTests {

        @Test
        @DisplayName("should_ReturnFalse_When_OtpNotInRedis")
        void should_ReturnFalse_When_OtpNotInRedis() {
            when(redisService.get(anyString())).thenReturn(null);

            boolean result = otpService.verifyOtp("user@test.com", "123456", OtpType.FORGOT_PASSWORD);

            assertThat(result).isFalse();
            verify(redisService, never()).delete(anyString());
        }

        @Test
        @DisplayName("should_ReturnFalse_When_OtpCodeDoesNotMatch")
        void should_ReturnFalse_When_OtpCodeDoesNotMatch() {
            when(redisService.get(anyString())).thenReturn("999999");

            boolean result = otpService.verifyOtp("user@test.com", "123456", OtpType.FORGOT_PASSWORD);

            assertThat(result).isFalse();
            verify(redisService, never()).delete(anyString());
        }

        @Test
        @DisplayName("should_ReturnTrue_And_DeleteKey_When_OtpMatchesForForgotPassword")
        void should_ReturnTrue_And_DeleteKey_When_OtpMatchesForForgotPassword() {
            when(redisService.get(anyString())).thenReturn("123456");

            boolean result = otpService.verifyOtp("user@test.com", "123456", OtpType.FORGOT_PASSWORD);

            assertThat(result).isTrue();
            verify(redisService).delete(anyString());
        }

        @Test
        @DisplayName("should_ReturnTrue_And_ActivateAccount_When_OtpMatchesForRegister")
        void should_ReturnTrue_And_ActivateAccount_When_OtpMatchesForRegister() {
            // Arrange
            UserProfile profile = buildProfile("acc-1", "reg@test.com");
            Account account = buildAccount("acc-1", AccountStatus.INACTIVE);

            when(redisService.get(anyString())).thenReturn("654321");
            when(userRepository.findByEmail("reg@test.com")).thenReturn(Optional.of(profile));
            when(accountRepository.findById("acc-1")).thenReturn(Optional.of(account));
            when(accountRepository.save(any())).thenReturn(account);

            // Act
            boolean result = otpService.verifyOtp("reg@test.com", "654321", OtpType.REGISTER);

            // Assert
            assertThat(result).isTrue();
            verify(accountRepository).save(argThat(a -> a.getStatus() == AccountStatus.ACTIVE));
            verify(redisService).delete(anyString());
        }
    }

    @Nested
    @DisplayName("countSendOtp tests")
    class CountSendOtpTests {

        @Test
        @DisplayName("should_ReturnZero_When_NoCountKeyInRedis")
        void should_ReturnZero_When_NoCountKeyInRedis() {
            when(redisService.get(anyString())).thenReturn(null);

            long count = otpService.countSendOtp("user@test.com");

            assertThat(count).isZero();
        }

        @Test
        @DisplayName("should_ReturnCount_When_CountKeyExistsInRedis")
        void should_ReturnCount_When_CountKeyExistsInRedis() {
            when(redisService.get(anyString())).thenReturn("3");

            long count = otpService.countSendOtp("user@test.com");

            assertThat(count).isEqualTo(3L);
        }
    }

    @Nested
    @DisplayName("deleteOtp tests")
    class DeleteOtpTests {

        @Test
        @DisplayName("should_DeleteKey_When_Called")
        void should_DeleteKey_When_Called() {
            otpService.deleteOtp("user@test.com", OtpType.FORGOT_PASSWORD);
            verify(redisService).delete(anyString());
        }
    }
}
