package org.demo.whs.mapper;

import org.demo.whs.entity.Account;
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
}
