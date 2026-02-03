package org.demo.whs.repository;

import org.demo.whs.entity.AccountHasRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for AccountHasRole entity.
 */
@Repository
public interface AccountHasRoleRepository extends JpaRepository<AccountHasRole, String> {
}
