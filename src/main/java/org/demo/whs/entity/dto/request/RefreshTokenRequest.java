package org.demo.whs.entity.dto.request;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class RefreshTokenRequest {
    @NotBlank(message = "Refresh token must not be blank")
    private String refreshToken;
}

