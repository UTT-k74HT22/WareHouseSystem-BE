package org.demo.whs.repository;

import org.demo.whs.entity.Permission;
import org.demo.whs.entity.enums.ActionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, String>,
        JpaSpecificationExecutor<Permission> {

    /**
     * Checks whether a permission already exists by its unique name.
     *
     * @param Name permission name
     * @return true if permission exists, false otherwise
     */
    boolean existsByName(String Name);

    /**
     *
     * @param resource
     * @param action
     * @return
     */
    boolean existsByResourceAndAction(String resource, ActionType action);

    /**
     *
     * @param name
     * @param id
     * @return
     */
    boolean existsByNameAndIdNot(String name, String id);

    /**
     * Lấy toàn bộ permission code của user thông qua role.
     * Flow:
     * user → account_roles → roles → role_permissions → permissions
     *
     * @param userId id user
     * @return tập permission code (PERM_USER_CREATE, ...)
     */
    @Query(value = """
        SELECT DISTINCT p.code
        FROM permissions p
        JOIN role_permissions rp ON p.id = rp.permission_id
        JOIN roles r ON r.id = rp.role_id
        JOIN account_roles ar ON ar.role_id = r.id
        WHERE ar.account_id = :userId
    """, nativeQuery = true)
    Set<String> getPermissionCodesByUserId(String userId);

    @Query("""
        SELECT DISTINCT p.resource
        FROM Permission p
        ORDER BY p.resource
    """)
    List<String> findDistinctResources();
}
