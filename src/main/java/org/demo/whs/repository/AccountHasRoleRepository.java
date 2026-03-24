package org.demo.whs.repository;

import org.demo.whs.entity.AccountHasRole;
import org.demo.whs.entity.AccountRoleId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for AccountHasRole entity.
 * PK is a composite key {@link AccountRoleId}.
 */
@Repository
public interface AccountHasRoleRepository extends JpaRepository<AccountHasRole, AccountRoleId> {

    long countByIdRoleId(String roleId);

    boolean existsByIdAccountIdAndIdRoleId(String accountId, String roleId);

    List<AccountHasRole> findByIdAccountId(String accountId);
}
