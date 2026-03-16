package org.demo.whs.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.dto.request.UserRole.AssignRolesRequest;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.dto.response.Role.RoleResponse;
import org.demo.whs.entity.dto.response.User.AccountResponse;
import org.demo.whs.repository.RoleRepository;
import org.demo.whs.service.UserRoleService;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserRoleServiceImpl implements UserRoleService {

    private final RoleRepository roleRepository;

    @Override
    public List<RoleResponse> assignRolesToUser(String userId, AssignRolesRequest request) {
        return List.of();
    }

    @Override
    public void removeRoleFromUser(String userId, String roleId) {

    }

    @Override
    public PageResponse<RoleResponse> getUserRoles(String userId, Pageable pageable) {
        return null;
    }

    @Override
    public PageResponse<AccountResponse> getRoleUsers(String roleId, Pageable pageable) {
        return null;
    }
}
