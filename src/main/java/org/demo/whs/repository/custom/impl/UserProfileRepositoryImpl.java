package org.demo.whs.repository.custom.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.dto.response.User.AccountResponse;
import org.demo.whs.entity.enums.AccountStatus;
import org.demo.whs.repository.custom.UserProfileRepositoryCustom;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
@Slf4j
public class UserProfileRepositoryImpl implements UserProfileRepositoryCustom {

    private final EntityManager entityManager;

    @Override
    public List<AccountResponse> getAccountsByRole(String roleName) {
        log.info("Fetching accounts with role: {}", roleName);

        Query query = entityManager.createNativeQuery("""
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
                """);

        query.setParameter("roleName", roleName);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return results.stream()
                .map(row -> AccountResponse.builder()
                        .accountId((String) row[0])
                        .username((String) row[1])
                        .status(AccountStatus.valueOf((String) row[2]))
                        .email((String) row[3])
                        .firstName((String) row[4])
                        .lastName((String) row[5])
                        .build())
                .collect(Collectors.toList());
    }
}