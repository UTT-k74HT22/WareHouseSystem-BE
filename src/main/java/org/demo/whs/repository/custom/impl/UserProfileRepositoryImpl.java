package org.demo.whs.repository.custom.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.dto.response.User.AccountResponse;
import org.demo.whs.repository.custom.UserProfileRepositoryCustom;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
@Slf4j
public class UserProfileRepositoryImpl implements UserProfileRepositoryCustom {

    private final EntityManager entityManager;

    /**
     * Custom query to fetch accounts by role name.
     *
     * @param roleName the name of the role
     * @return the list of AccountResponse DTOs
     */
    @Override
    public List<AccountResponse> getAccountsByRole(String roleName) {
        log.info("Fetching accounts with role: {}", roleName);

        Query query = entityManager.createNativeQuery(
                """
                        SELECT 
                            a.id AS accountId,
                            a.username AS username,
                            a.status AS status,
                            up.email AS email,
                            up.first_name AS firstName,
                            up.last_name AS lastName
                        FROM user_profiles up
                        LEFT JOIN accounts a ON up.account_id = a.id
                        LEFT JOIN account_roles ar ON a.id = ar.account_id
                        LEFT JOIN roles r ON ar.role_id = r.id
                        WHERE r.name = :roleName
                        """,
                "AccountResponseMapping"
        );
        query.setParameter("roleName", roleName);

        return query.getResultList();
    }

    /**
     * Custom query to fetch accounts by a list of IDs.
     *
     * @param ids the list of account IDs
     * @return the list of AccountResponse DTOs
     */
    @Override
    public List<AccountResponse> getAccountsByIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        log.info("Fetching accounts with ids: {}", ids);

        Query query = entityManager.createNativeQuery(
                """
                        SELECT 
                            a.id AS accountId,
                            a.username AS username,
                            a.status AS status,
                            up.email AS email,
                            up.first_name AS firstName,
                            up.last_name AS lastName
                        FROM user_profiles up
                        LEFT JOIN accounts a ON up.account_id = a.id
                        WHERE a.id IN (:ids)
                        """,
                "AccountResponseMapping"
        );
        query.setParameter("ids", ids);

        return query.getResultList();
    }

    /**
     * Custom query to fetch account by ID.
     *
     * @param id the account ID
     * @return the AccountResponse DTO
     */
    @Override
    public AccountResponse getAccountById(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }

        log.info("Fetching account with id: {}", id);

        Query query = entityManager.createNativeQuery(
                """
                        SELECT 
                            a.id AS accountId,
                            a.username AS username,
                            a.status AS status,
                            up.email AS email,
                            up.first_name AS firstName,
                            up.last_name AS lastName
                        FROM user_profiles up
                        LEFT JOIN accounts a ON up.account_id = a.id
                        WHERE a.id = :id
                        """,
                "AccountResponseMapping"
        );
        query.setParameter("id", id);

        List<AccountResponse> results = query.getResultList();
        return results.isEmpty() ? null : results.get(0);
    }
}