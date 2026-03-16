package org.demo.whs.service;

import org.demo.whs.entity.dto.request.Role.CreateRoleRequest;
import org.demo.whs.entity.dto.request.Role.UpdateRoleRequest;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.dto.response.Role.RoleResponse;
import org.springframework.data.domain.Pageable;


public interface RoleService {

    // ROLE CRUD
    RoleResponse createRole(CreateRoleRequest request);

    PageResponse<RoleResponse> getRoles(Pageable pageable);

    RoleResponse getRoleById(String id);

    RoleResponse updateRole(String id, UpdateRoleRequest request);

    void deleteRole(String id);

}