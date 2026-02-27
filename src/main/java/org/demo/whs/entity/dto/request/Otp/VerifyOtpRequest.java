package org.demo.whs.entity.dto.request.Otp;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import org.demo.whs.entity.enums.OtpType;

@Getter
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class VerifyOtpRequest {
    @NotBlank(message = "Email not null")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "OTP not null")
    @Size(min = 6, max = 6, message = "OTP must be 6 digits")
    private String otp;

    @NotNull(message = "Type not null")
    private OtpType type;
}
