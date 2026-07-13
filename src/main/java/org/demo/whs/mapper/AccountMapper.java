package org.demo.whs.mapper;

import org.demo.whs.entity.Account;
import org.demo.whs.entity.UserProfile;
import org.demo.whs.entity.dto.response.User.AccountResponse;
import org.demo.whs.entity.enums.AccountStatus;
import org.springframework.stereotype.Component;

/**
 * Mapper for Account entity.
 * <p>
 * Responsible for converting between Account entities and their corresponding DTOs.
 * </p>
 */
@Component
public class AccountMapper {

    /**
     * Create an Account entity with the given username and raw password.
     * The password is hashed using the provided PasswordEncoder.
     *
     * @param username    the username for the account
     * @param password the raw password to be hashed and stored
     * @return a new Account entity with ACTIVE status
     */
    public static Account getAccount(String username, String password) {
        Account account = Account.builder()
                .username(username)
                .password(password)
                .status(AccountStatus.ACTIVE)
                .build();
        return account;
    }

    /**
     * Map Account entity → AccountResponse
     */
    public AccountResponse toAccountResponse(Account account) {
        if (account == null) return null;

        return new AccountResponse(
                account.getId(),
                account.getUsername(),
                account.getStatus() != null ? account.getStatus().name() : null,
                null,
                null,
                null
        );
    }

    /**
     * Map Account entity + UserProfile → AccountResponse with full name
     */
    public AccountResponse toAccountResponseWithProfile(Account account, UserProfile profile) {
        if (account == null) return null;

        String fullName = null;
        if (profile != null) {
            String firstName = profile.getFirstName();
            String lastName = profile.getLastName();
            if (firstName != null && !firstName.isEmpty()) {
                fullName = lastName != null && !lastName.isEmpty()
                        ? firstName + " " + lastName
                        : firstName;
            } else if (lastName != null && !lastName.isEmpty()) {
                fullName = lastName;
            }
        }

        return new AccountResponse(
                account.getId(),
                account.getUsername(),
                account.getStatus() != null ? account.getStatus().name() : null,
                profile != null ? profile.getEmail() : null,
                profile != null ? profile.getFirstName() : null,
                profile != null ? profile.getLastName() : null,
                fullName,
                account.getCreatedAt() != null ? account.getCreatedAt().toString() : null,
                account.getUpdatedAt() != null ? account.getUpdatedAt().toString() : null
        );
    }
}
