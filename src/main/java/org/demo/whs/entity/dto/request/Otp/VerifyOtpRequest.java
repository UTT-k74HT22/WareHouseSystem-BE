package org.demo.whs.entity.dto.request.Otp;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.demo.whs.entity.enums.OtpType;

public class VerifyOtpRequest {
    @NotBlank(message = "Email not null")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "OTP not null")
    @Size(min = 6, max = 6, message = "OTP must be 6 digits")
    private String otp;

    private OtpType type;
}
