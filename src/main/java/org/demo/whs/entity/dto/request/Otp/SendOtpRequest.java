package org.demo.whs.entity.dto.request.Otp;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.demo.whs.entity.enums.OtpType;
@Getter
@Setter
public class SendOtpRequest {
    @NotBlank(message = "Email not null")
    @Email(message = "Email not format")
    private String email;

    private OtpType type;
}
