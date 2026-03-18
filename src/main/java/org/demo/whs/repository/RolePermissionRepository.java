package org.demo.whs.repository;

import io.lettuce.core.dynamic.annotation.Param;
import org.demo.whs.entity.RoleHasPermission;
import org.demo.whs.entity.RolePermissionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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

}
