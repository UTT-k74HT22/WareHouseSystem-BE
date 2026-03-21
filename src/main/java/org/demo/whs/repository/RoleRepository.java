package org.demo.whs.repository;

import org.demo.whs.entity.Permission;
import org.demo.whs.entity.Role;
import org.demo.whs.entity.enums.RoleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, String>{

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

    /**
     * Lấy danh sách Permission gán cho role theo role.id
     */
    @Query(value = """
        SELECT p.*
        FROM permissions p
        JOIN role_permissions rhp ON rhp.permission_id = p.id
        WHERE rhp.role_id = :roleId
    """, nativeQuery = true)
    List<Permission> findPermissionsByRoleId(@Param("roleId") String roleId);
}
