package org.demo.whs.service.impl;

import jdk.jfr.Timestamp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.Role;
import org.demo.whs.entity.Permission;
import org.demo.whs.entity.dto.request.Role.CreateRoleRequest;
import org.demo.whs.entity.dto.request.Role.UpdateRoleRequest;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.dto.response.Role.RoleResponse;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.mapper.RoleMapper;
import org.demo.whs.exception.NotFoundException;
import org.demo.whs.repository.RoleRepository;
import org.demo.whs.repository.specification.RoleSpecification;
import org.demo.whs.service.RoleService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of RoleService.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final RoleMapper roleMapper;

    /**
     * Creates a new Role based on the given request.
     *
     * @param request the role creation request containing name, description, and optional default flag
     * @return the saved Role mapped to RoleResponse
     * @throws BadRequestException if the role name is null, empty, or already exists,
     *                             or if a duplicate code is detected during save
     */
    @Override
    @Transactional
    public RoleResponse createRole(CreateRoleRequest request) {
        validateRoleName(request.getName());

        boolean isDefault = determineIsDefault(request.getIsDefault());

        if (isDefault && roleRepository.existsByIsDefaultTrue()) {
            roleRepository.updateAllIsDefaultToFalse();
        }

        String code = generateRoleCode(request.getName());

        Role role = roleMapper.createEntity(request);
        role.setCode(code);
        role.setIsDefault(isDefault);

        try {
            Role savedRole = roleRepository.save(role);

            log.info("CREATE ROLE: id={}, code={}, name={}, isDefault={}",
                    savedRole.getId(),
                    savedRole.getCode(),
                    savedRole.getName(),
                    savedRole.getIsDefault());

            return roleMapper.toResponse(savedRole);
        } catch (DataIntegrityViolationException e) {
            log.warn("CREATE ROLE duplicate detected: name={}", request.getName());
            throw new BadRequestException(ErrorCode.ROLE_004);
        }
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

        Page<RoleResponse> responsePage = rolePage.map(role ->
                roleMapper.toDetailResponse(
                        role,
                        permissionCountMap.getOrDefault(role.getId(), 0L),
                        userCountMap.getOrDefault(role.getId(), 0L)
                )
        );

        return PageResponse.from(responsePage);
    }

    /**
     * Retrieves a Role by its ID, including its associated Permissions.
     *
     * @param roleId the ID of the role to retrieve
     * @return the RoleResponse containing role details and permissions
     * @throws NotFoundException if no role exists with the given ID
     */
    @Override
    public RoleResponse getRoleById(String roleId) {

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.ROLE_001));

        List<Permission> permissions = roleRepository.findPermissionsByRoleId(roleId);

        return roleMapper.toResponseWithPermissions(role, permissions);
    }

    /**
     * Update an existing role.
     *
     * @param id      the ID of the role to update
     * @param request the request body containing updated role details
     * @return the updated role details
     */
    @Override
    @Transactional
    public RoleResponse updateRole(String id, UpdateRoleRequest request) {

        log.info("Updating role with id={}", id);

        if (request == null) {
            throw new BadRequestException(ErrorCode.COM_001);
        }

        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorCode.ROLE_001));

        if (request.getName() != null && request.getName().isBlank()) {
            throw new BadRequestException(ErrorCode.ROLE_002);
        }

        if (Boolean.TRUE.equals(request.getIsDefault()) &&
                !Boolean.TRUE.equals(role.getIsDefault())) {
            roleRepository.updateAllIsDefaultToFalse();
        }

        roleMapper.updateEntity(request, role);

        Role updated = roleRepository.save(role);

        log.info("Role updated successfully id={}, name={}", updated.getId(), updated.getName());

        return roleMapper.toResponse(updated);
    }

    /**
     * Deletes a role by its ID.
     *
     * @param id the ID of the role to delete
     */
    @Override
    public void deleteRole(String id) {}


    /**
     * Validates the role name.
     *
     * @param name the role name to validate
     * @throws BadRequestException if the name is null, empty, or already exists in the repository
     */
    private void validateRoleName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new BadRequestException(ErrorCode.ROLE_002);
        }

        if (roleRepository.existsByNameIgnoreCase(name)) {
            throw new BadRequestException(ErrorCode.ROLE_004);
        }
    }

    /**
     * Determines whether the role should be marked as default.
     *
     * @param requestIsDefault the requested default flag
     * @return true if the role should be default, false otherwise
     */
    private boolean determineIsDefault(Boolean requestIsDefault) {
        if (Boolean.TRUE.equals(requestIsDefault)) {
            return true;
        }
        return !roleRepository.existsByIsDefaultTrue();
    }

    /**
     * Generates a role code from the role name.
     *
     * @param name the role name
     * @return the generated role code
     */
    private String generateRoleCode(String name) {
        String normalized = name.trim()
                .toLowerCase()
                .replaceAll("[^a-z0-9\\s]", "")
                .replaceAll("\\s+", "_");

        return "role_" + normalized;
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