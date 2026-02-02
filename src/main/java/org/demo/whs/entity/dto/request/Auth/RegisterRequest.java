package org.demo.whs.entity.dto.request.Auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class RegisterRequest {
    @NotBlank(message = "Username not blank")
    @Size(min = 3, max = 50)
    private String username;

    @NotBlank(message = "Password not blank")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
            message = "Password must be at least 8 characters and contain uppercase, lowercase, digit, and special character"
    )
    @Size(min = 6, max = 50)
    private String password;

    @NotBlank
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
            message = "Password must be at least 8 characters and contain uppercase, lowercase, digit, and special character"
    )
    private String confirmPassword;
    @NotBlank
    @Size(min = 3, max = 50)
    private String fullName;
    @NotBlank

    @Email
    private String email;

    @NotBlank
    @Size(max = 50)
    private String firstName;

    @NotBlank
    @Size(max = 50)
    private String lastName;

    @Size(max = 15)
    private String phoneNumber;
}
