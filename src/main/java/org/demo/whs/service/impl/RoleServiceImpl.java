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
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of RoleService.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final RoleMapper roleMapper;

    @Override
    public RoleResponse createRole(CreateRoleRequest request) {
        return null;
    }

    /**
     * Get roles with filtering + pagination.
     *
     * @param isDefault filter by default role
     * @param search    keyword search
     * @param pageable  pagination info
     * @return paginated roles
     */
    @Override
    @Transactional(readOnly = true)
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

        Map<String, Long> permissionCountMap = mapToCountMapSafe(
                roleRepository.countPermissionsByRoleIds(roleIds)
        );

        Map<String, Long> userCountMap = mapToCountMapSafe(
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
     * Safe mapping for aggregation results.
     * Prevent:
     * - NullPointerException
     * - ClassCastException
     */
    private Map<String, Long> mapToCountMapSafe(List<Object[]> data) {

        if (data == null || data.isEmpty()) {
            return Collections.emptyMap();
        }

        return data.stream()
                .filter(Objects::nonNull)
                .filter(arr -> arr.length >= 2 && arr[0] != null)
                .collect(Collectors.toMap(
                        arr -> String.valueOf(arr[0]),
                        arr -> {
                            Object count = arr[1];
                            if (count == null) return 0L;
                            if (count instanceof Number num) {
                                return num.longValue();
                            }
                            return 0L;
                        }
                ));
    }
}