package org.demo.whs.repository;

import org.demo.whs.entity.AccountHasRole;
import org.demo.whs.entity.AccountRoleId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountHasRoleRepository extends JpaRepository<AccountHasRole, AccountRoleId> {
}
