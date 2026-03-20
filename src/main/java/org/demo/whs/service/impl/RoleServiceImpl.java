package org.demo.whs.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.Role;
import org.demo.whs.entity.dto.request.Role.CreateRoleRequest;
import org.demo.whs.entity.dto.request.Role.UpdateRoleRequest;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.dto.response.Role.RoleResponse;
import org.demo.whs.mapper.RoleMapper;
import org.demo.whs.repository.RoleRepository;
import org.demo.whs.repository.specification.RoleSpecification;
import org.demo.whs.service.RoleService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementation of {@link RoleService}.
 * This service handles business logic related to {@link Role}:
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final RoleMapper roleMapper;

    /**
     * Create a new role.
     *
     * @param request request payload containing role data
     * @return created role response
     */
    @Override
    public RoleResponse createRole(CreateRoleRequest request) {
        return null;
    }

    /**
     * Get roles with pagination.
     *
     * @param pageable pagination information
     * @return paginated list of roles
     */
    @Override
    public PageResponse<RoleResponse> getRoles(Boolean isDefault, String search, Pageable pageable) {

        Page<Role> rolePage = roleRepository.findAll(
                RoleSpecification.filter(isDefault, search),
                pageable
        );

        List<Role> roles = rolePage.getContent();

        if (roles.isEmpty()) {
            return PageResponse.from(rolePage.map(roleMapper::toResponse));
        }

        List<String> roleIds = roles.stream()
                .map(Role::getId)
                .toList();

        var permissionCountMap = mapToCountMap(
                roleRepository.countPermissionsByRoleIds(roleIds)
        );

        var userCountMap = mapToCountMap(
                roleRepository.countUsersByRoleIds(roleIds)
        );

        Page<RoleResponse> responsePage = rolePage.map(role -> {
            RoleResponse res = roleMapper.toResponse(role);

            res.setPermissionCount(
                    permissionCountMap.getOrDefault(role.getId(), 0L)
            );

            res.setUserCount(
                    userCountMap.getOrDefault(role.getId(), 0L)
            );

            return res;
        });

        return PageResponse.from(responsePage);
    }

    @Override
    public RoleResponse getRoleById(String id) {
        return null;
    }

    @Override
    public RoleResponse updateRole(String id, UpdateRoleRequest request) {
        return null;
    }

    @Override
    public void deleteRole(String id) {

    }

    /**
     * Convert raw aggregation query result into a Map.
     *
     * @param data list of Object arrays returned from repository
     * @return map of roleId to count
     */
    private Map<String, Long> mapToCountMap(List<Object[]> data) {
        return data.stream()
                .collect(Collectors.toMap(
                        obj -> (String) obj[0],
                        obj -> ((Number) obj[1]).longValue()
                ));
    }
}