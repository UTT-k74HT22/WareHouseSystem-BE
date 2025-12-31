package org.demo.whs.entity.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String expireAccessToken;
    private String expireRefreshToken;
    private String ip;
}
