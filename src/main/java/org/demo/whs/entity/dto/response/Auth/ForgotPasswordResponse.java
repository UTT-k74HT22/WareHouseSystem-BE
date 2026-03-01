package org.demo.whs.entity.dto.response.Auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ForgotPasswordResponse {
    /**
     * Reset Token (JWT with type: resetPassword)
     */
    private String resetToken;
}
