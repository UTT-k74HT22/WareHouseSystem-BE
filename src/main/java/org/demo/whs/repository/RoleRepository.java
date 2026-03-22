package org.demo.whs.repository;

import org.demo.whs.entity.Permission;
import org.demo.whs.entity.Role;
import org.demo.whs.entity.enums.RoleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing Role entities.
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, String>,
        JpaSpecificationExecutor<Role> {

    /**
     * Retrieves the list of role names assigned to a given username.
     *
     * @param username the username of the account
     * @return a list of role names associated with the user
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
     * Retrieves the list of role names assigned to a given account ID.
     *
     * @param accountId the ID of the account
     * @return a list of role names associated with the account
     */
    @Query(value = """
            SELECT r.name
            FROM roles r
            INNER JOIN account_roles ar ON r.id = ar.role_id
            WHERE ar.account_id = :accountId
            """, nativeQuery = true)
    List<String> findRoleNamesByAccountId(@Param("accountId") String accountId);

    /**
     * Finds a Role by its RoleType enum name.
     *
     * @param name the RoleType of the role
     * @return an Optional containing the Role if found, empty otherwise
     */
    Optional<Role> findByName(RoleType name);

    /**
     * Checks if a role exists with the given RoleType name.
     *
     * @param name the RoleType to check
     * @return true if a role with the given name exists, false otherwise
     */
    boolean existsByName(String name);

    /**
     * Retrieves the list of Permissions assigned to a role by its ID.
     *
     * @param roleId the ID of the role
     * @return a list of Permissions associated with the role
     */
    @Query(value = """
            SELECT p.*
            FROM permissions p
            JOIN role_permissions rhp ON rhp.permission_id = p.id
            WHERE rhp.role_id = :roleId
            """, nativeQuery = true)
    List<Permission> findPermissionsByRoleId(@Param("roleId") String roleId);

    /**
     * Checks if a role exists with the given name (case-insensitive).
     *
     * @param name the name of the role
     * @return true if a role with the given name exists, false otherwise
     */
    boolean existsByNameIgnoreCase(String name);

    /**
     * Checks if any role is marked as default.
     *
     * @return true if a default role exists, false otherwise
     */
    boolean existsByIsDefaultTrue();

    /**
     * Checks if a role exists with the given code.
     *
     * @param code the code of the role
     * @return true if a role with the code exists, false otherwise
     */
    boolean existsByCode(String code);

    /**
     * Updates all roles in the database to set isDefault = false.
     * Typically used before assigning a new default role.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Role r SET r.isDefault = false WHERE r.isDefault = true")
    void updateAllIsDefaultToFalse();

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