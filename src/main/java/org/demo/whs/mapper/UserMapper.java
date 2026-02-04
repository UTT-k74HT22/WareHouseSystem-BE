package org.demo.whs.mapper;

import org.demo.whs.entity.Account;
import org.demo.whs.entity.UserProfile;
import org.demo.whs.entity.dto.response.User.AccountResponse;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Mapper for User entity and related DTOs.
 */
@Component
public class UserMapper {

    /**
     * Convert an Account entity and UserProfile entity to an AccountResponse DTO.
     *
     * @param account      the Account entity
     * @param userProfile  the UserProfile entity
     * @return the AccountResponse DTO
     */
    public AccountResponse toAccountResponse(Account account, UserProfile userProfile) {
        return AccountResponse.builder()
                .accountId(account.getId())
                .username(account.getUsername())
                .status(account.getStatus())
                .email(userProfile.getEmail())
                .firstName(userProfile.getFirstName())
                .lastName(userProfile.getLastName())
                .build();
    }

    /**
     * Convert a list of Account entities to a list of AccountResponse DTOs.
     *
     * @param accounts               the list of Account entities
     * @param profileMapByAccountId  a map of UserProfile entities keyed by account ID
     * @return the list of AccountResponse DTOs
     */
    public List<AccountResponse> toAccountResponseList(List<Account> accounts,
                                                       Map<String, UserProfile> profileMapByAccountId) {
        if (accounts == null || accounts.isEmpty()) return Collections.emptyList();

        return accounts.stream()
                .map(acc -> toAccountResponse(acc, profileMapByAccountId.get(acc.getId())))
                .collect(Collectors.toList());
    }
}
