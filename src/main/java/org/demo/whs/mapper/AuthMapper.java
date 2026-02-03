package org.demo.whs.mapper;

import org.demo.whs.entity.Account;
import org.demo.whs.entity.dto.request.Auth.RegisterRequest;
import org.demo.whs.entity.dto.response.AuthResponse;
import org.demo.whs.entity.dto.response.RefreshTokenResponse;
import org.demo.whs.entity.enums.AccountStatus;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting authentication data to response DTOs.
 */
@Component
public class AuthMapper {

    /**
     * Convert access and refresh tokens to an AuthResponse DTO.
     *
     * @param accessToken  the access token
     * @param refreshToken the refresh token
     * @return the AuthResponse DTO containing the tokens
     */
    public AuthResponse toResponse(String accessToken, String refreshToken, String expireAccessToken, String expireRefreshToken, String ip) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expireAccessToken(expireAccessToken)
                .expireRefreshToken(expireRefreshToken)
                .ip(ip)
                .build();
    }

    public RefreshTokenResponse toRefreshResponse(String accessToken, String expireAccessToken) {
        return RefreshTokenResponse.builder()
                .accessToken(accessToken)
                .expireAccessToken(expireAccessToken)
                .build();
    }

    /**
     *
     * @param request
     * @return
     */
    public Account registerAcc(RegisterRequest request) {
        return Account.builder()
                .username(request.getUsername())
                .status(AccountStatus.INACTIVE)
                .build();
    }
}
