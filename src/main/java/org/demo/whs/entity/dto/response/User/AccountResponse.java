package org.demo.whs.entity.dto.response.User;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Getter;
import org.demo.whs.entity.enums.AccountStatus;

@Getter
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AccountResponse {
    private String accountId;
    private String username;
    private AccountStatus status;
    private String email;
    private String firstName;
    private String lastName;

    public AccountResponse(String accountId, String username, AccountStatus status, String email, String firstName, String lastName) {
        this.accountId = accountId;
        this.username = username;
        this.status = AccountStatus.valueOf(String.valueOf(status));
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
    }
}