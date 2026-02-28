package org.demo.whs.service;

import org.demo.whs.entity.dto.request.Otp.VerifyOtpRequest;
import org.demo.whs.entity.enums.OtpType;

public interface OtpService {
    /**
     * Sinh ra mã OTP và lưu vào Redis với TTL (VD: 10 phút)
     * @param email email account
     * @param type loại OTP (REGISTER, FORGOT_PASSWORD...)
     * @return mã OTP
     */
    String generateOtpCode(String email, OtpType type);

    /**
     * Gửi mã OTP đến user
     * @param email email
     * @param type tpye
     */
    void sendOtp(String email, OtpType type);

    /**
     * Đếm số lần gửi OTP trong 24h
     * @param email email account
     * @return tổng số lần gửi trong 24h
     */
    long countSendOtp(String email);

    /**
     * Xác thực OTP người dùng nhập
     * @param email email account
     * @param otpCode mã OTP người dùng nhập
     * @param type loại OTP
     * @return true nếu đúng và chưa hết hạn
     */
    boolean verifyOtp(String email, String otpCode, OtpType type);

    /**
     * Xóa OTP sau khi verify thành công
     */
    void deleteOtp(String email, OtpType type);
}
