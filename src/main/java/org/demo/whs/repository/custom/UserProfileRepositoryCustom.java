package org.demo.whs.repository.custom;

import org.demo.whs.entity.dto.response.User.AccountResponse;
import java.util.List;

/**
 * Custom Repository Interface for UserProfile.
 */
public interface UserProfileRepositoryCustom {

    /**
     * Custom query to fetch accounts by role name.
     *
     * @param roleName the name of the role
     * @return the list of AccountResponse DTOs
     */
    List<AccountResponse> getAccountsByRole(String roleName);

    /**
     * Custom query to fetch accounts by a list of IDs.
     *
     * @param ids the list of account IDs
     * @return the list of AccountResponse DTOs
     */
    List<AccountResponse> getAccountsByIds(List<String> ids);

    /**
     * Custom query to fetch account by ID.
     *
     * @param id the account ID
     * @return the AccountResponse DTO
     */
    AccountResponse getAccountById(String id);
}
