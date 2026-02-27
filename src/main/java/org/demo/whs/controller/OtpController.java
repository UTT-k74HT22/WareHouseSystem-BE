package org.demo.whs.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.dto.request.Otp.SendOtpRequest;
import org.demo.whs.entity.dto.request.Otp.VerifyOtpRequest;
import org.demo.whs.entity.dto.response.BaseResponse;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.service.OtpService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/otp")
@RequiredArgsConstructor
@Slf4j
@Validated
public class OtpController {

    private final OtpService otpService;

    /**
     * Send/Resend OTP to email
     * PUBLIC - No authentication required
     * POST /api/v1/otp/send
     */
    @PostMapping("/send")
    public ResponseEntity<BaseResponse<String>> sendOtp(
            @RequestBody @Valid SendOtpRequest request
    ) {

        log.info("[OTP] Sending OTP | email={} | type={}",
                request.getEmail(),
                request.getType());

        otpService.sendOtp(request.getEmail(), request.getType());

        return ResponseEntity.ok(
                BaseResponse.success("OTP sent successfully")
        );
    }

    /**
     * Verify OTP
     * PUBLIC - No authentication required
     * POST /api/v1/otp/verify
     */
    @PostMapping("/verify")
    public ResponseEntity<BaseResponse<String>> verifyOtp(
            @RequestBody @Valid VerifyOtpRequest request
    ) {

        log.info("[OTP] Verifying OTP | email={} | type={}",
                request.getEmail(),
                request.getType());

        boolean isValid = otpService.verifyOtp(
                request.getEmail(),
                request.getOtp(),
                request.getType()
        );

        if (!isValid) {
            return ResponseEntity.badRequest()
                    .body(BaseResponse.error(
                            ErrorCode.OTP_006.getCode(),
                            "Invalid or expired OTP",
                            null
                    ));
        }

        return ResponseEntity.ok(
                BaseResponse.success("OTP verified successfully")
        );
    }
}