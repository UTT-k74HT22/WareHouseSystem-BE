package org.demo.whs.mapper;

import org.demo.whs.entity.Account;
import org.demo.whs.entity.AccountHasRole;
import org.demo.whs.entity.AccountRoleId;
import org.demo.whs.entity.Role;
import org.springframework.stereotype.Component;

/**
 * Mapper for AccountHasRole entity.
 * <p>
 * Responsible for converting between AccountHasRole entities and their corresponding DTOs.
 * </p>
 */
@Component
public class AccountHasRoleMapper {

    /**
     * Create an AccountHasRole entity linking the given account and role.
     *
     * @param account      the account to link
     * @param resolvedRole the role to link
     * @return a new AccountHasRole entity with the appropriate composite key
     */
    public static AccountHasRole getAccountHasRole(Account account, Role resolvedRole) {
        return new AccountHasRole(
                AccountRoleId.builder()
                        .accountId(account.getId())
                        .roleId(resolvedRole.getId())
                        .build()
        );
    }

}
