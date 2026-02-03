package org.demo.whs.entity.dto.request.Otp;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.demo.whs.entity.enums.OtpType;
@Getter
@Setter
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SendOtpRequest {
    @NotBlank(message = "Email not null")
    @Email(message = "Email not format")
    private String email;

    private OtpType type;
}
