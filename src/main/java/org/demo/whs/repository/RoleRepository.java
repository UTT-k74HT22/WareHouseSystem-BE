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

/**
 * Repository interface for managing {@link Role} entities.
 *
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, String>,
        JpaSpecificationExecutor<Role> {

    /**
     * Retrieve all role names assigned to a user by username.
     *
     * @param username the username of the account
     * @return list of role names (e.g. ["ADMIN", "USER"])
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
     * Retrieve all role names assigned to a user by account ID.
     *
     * @param accountId the account ID (UUID)
     * @return list of role names (e.g. ["ADMIN", "MANAGER"])
     */
    @Query(value = """
        SELECT r.name
        FROM roles r
        INNER JOIN account_roles ar ON r.id = ar.role_id
        WHERE ar.account_id = :accountId
        """, nativeQuery = true)
    List<String> findRoleNamesByAccountId(@Param("accountId") String accountId);

    /**
     * Find a role by its enum name.
     *
     * @param name role type enum
     * @return optional role entity
     */
    Optional<Role> findByName(RoleType name);

    /**
     * Check whether a role exists by its name.
     *
     * @param name role type enum
     * @return {@code true} if exists, otherwise {@code false}
     */
    boolean existsByName(RoleType name);

    /**
     * Count number of permissions assigned to each role.
     * This is a batch query to avoid N+1 problem.
     *
     * @param roleIds list of role IDs
     * @return list of Object arrays:
     */
    @Query("""
        SELECT rp.id.roleId, COUNT(rp.id.permissionId)
        FROM RoleHasPermission rp
        WHERE rp.id.roleId IN :roleIds
        GROUP BY rp.id.roleId
    """)
    List<Object[]> countPermissionsByRoleIds(List<String> roleIds);

    /**
     * Count number of users assigned to each role.
     * This is a batch query to improve performance and prevent N+1 queries.
     *
     * @param roleIds list of role IDs
     * @return list of Object arrays:
     */
    @Query("""
        SELECT ar.id.roleId, COUNT(ar.id.accountId)
        FROM AccountHasRole ar
        WHERE ar.id.roleId IN :roleIds
        GROUP BY ar.id.roleId
    """)
    List<Object[]> countUsersByRoleIds(List<String> roleIds);
}