package org.demo.whs.repository;

import org.demo.whs.entity.Role;
import org.demo.whs.entity.enums.RoleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, String>,
        JpaSpecificationExecutor<Role> {

    /**
     * Find all role names for a given username.
     *
     * @param username the account username
     * @return list of role name strings (e.g. ["ADMIN", "USER"])
     */
    @Query(value = """
    SELECT r.name
    FROM roles r
    INNER JOIN account_roles ar ON r.id = ar.role_id
    INNER JOIN accounts a ON ar.account_id = a.id
    WHERE a.username = :username
    """, nativeQuery = true)
    List<String> findRoleNamesByUsername(@Param("username") String username);

    /**
     * Find all role names for a given account ID (UUID).
     * <p>
     * Returns {@code List<String>} because:
     * 1) Native queries return raw strings, not enums
     * 2) A user may have multiple roles (e.g. ADMIN + MANAGER)
     * </p>
     *
     * @param accountId the account UUID
     * @return list of role name strings (e.g. ["ADMIN", "USER"])
     */
    @Query(value = """
    SELECT r.name
    FROM roles r
    INNER JOIN account_roles ar ON r.id = ar.role_id
    WHERE ar.account_id = :accountId
    """, nativeQuery = true)
    List<String> findRoleNamesByAccountId(@Param("accountId") String accountId);

    Optional<Role> findByName(RoleType name);

    boolean existsByName(RoleType name);

    @Query("""
    SELECT rp.id.roleId, COUNT(rp.id.permissionId)
    FROM RoleHasPermission rp
    WHERE rp.id.roleId IN :roleIds
    GROUP BY rp.id.roleId
""")
    List<Object[]> countPermissionsByRoleIds(List<String> roleIds);

    @Query("""
    SELECT ar.id.roleId, COUNT(ar.id.accountId)
    FROM AccountHasRole ar
    WHERE ar.id.roleId IN :roleIds
    GROUP BY ar.id.roleId
""")
    List<Object[]> countUsersByRoleIds(List<String> roleIds);
}
