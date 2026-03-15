package org.demo.whs.service;

import org.demo.whs.entity.dto.request.UserRole.AssignRolesRequest;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.dto.response.Role.RoleResponse;
import org.demo.whs.entity.dto.response.User.AccountResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UserRoleService {

    List<RoleResponse> assignRolesToUser(String userId, AssignRolesRequest request);

    void removeRoleFromUser(String userId, String roleId);

    PageResponse<RoleResponse> getUserRoles(String userId, Pageable pageable);

    PageResponse<AccountResponse> getRoleUsers(String roleId, Pageable pageable);
}
