package org.demo.whs.repository;


import org.springframework.data.repository.query.Param;
import org.demo.whs.entity.Permission;
import org.demo.whs.entity.RoleHasPermission;
import org.demo.whs.entity.RolePermissionId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Repository phục vụ kiểm tra Permission trước khi xóa.
 */
public interface RolePermissionRepository extends JpaRepository<RoleHasPermission, RolePermissionId> {

    /**
     * Lấy danh sách roleId đang sử dụng permission.
     *
     * @param permissionId ID của permission
     * @return danh sách roleId
     */
    @Query("""
        SELECT rp.id.roleId
        FROM RoleHasPermission rp
        WHERE rp.id.permissionId = :permissionId
    """)
    List<String> findRoleIdsByPermissionId(@Param("permissionId") String permissionId);

    @Query("""
    SELECT rp.id.permissionId
    FROM RoleHasPermission rp
    WHERE rp.id.roleId = :roleId
""")
    List<String> findPermissionIdsByRoleId(@Param("roleId") String roleId);

    int deleteByIdRoleIdAndIdPermissionId(String roleId, String permissionId);

    boolean existsByIdRoleIdAndIdPermissionId(String roleId, String permissionId);

    @Query("""
    SELECT p FROM Permission p
    JOIN RoleHasPermission rp ON rp.id.permissionId = p.id
    WHERE rp.id.roleId = :roleId
    AND (:resource IS NULL OR LOWER(p.resource) = LOWER(:resource))
""")
    Page<Permission> findPermissionsByRoleId(
            @Param("roleId") String roleId,
            @Param("resource") String resource,
            Pageable pageable
    );
}
