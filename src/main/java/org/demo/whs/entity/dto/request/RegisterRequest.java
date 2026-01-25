package org.demo.whs.entity.dto.request;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import org.demo.whs.utils.annotation.ValidPassword;

@Getter
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class RegisterRequest {

    @NotBlank(message = "Username must not be blank")
    private String username;

    @NotBlank(message = "Password must not be blank")
    @ValidPassword
    private String password;

    @NotBlank(message = "Password confirmation must not be blank")
    private String passwordConfirm;
}
