package org.demo.whs.entity.dto.response.User;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.demo.whs.entity.enums.AccountStatus;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AccountResponse {
    private String accountId;
    private String username;
    private AccountStatus status;
    private String email;
    private String firstName;
    private String lastName;
    private String fullName;
    private String createdAt;
    private String updatedAt;

    public AccountResponse(String accountId, String username, String status, String email, String firstName, String lastName) {
        this.accountId = accountId;
        this.username = username;
        this.status = status != null ? AccountStatus.valueOf(status) : null;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public AccountResponse(String accountId, String username, String status, String email, String firstName, String lastName, String fullName, String createdAt, String updatedAt) {
        this.accountId = accountId;
        this.username = username;
        this.status = status != null ? AccountStatus.valueOf(status) : null;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.fullName = fullName;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}