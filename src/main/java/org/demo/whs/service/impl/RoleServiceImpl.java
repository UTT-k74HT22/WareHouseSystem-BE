package org.demo.whs.service.impl;

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
import org.demo.whs.service.RoleService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementation of RoleService that handles CRUD operations for Role entities,
 * including role creation, retrieval, update, and deletion.
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
     * Retrieves a paginated list of roles.
     *
     * @param pageable pagination information
     * @return a page of RoleResponse objects
     */
    @Override
    public PageResponse<RoleResponse> getRoles(Pageable pageable) {
        return null; // implement later
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
     * Updates an existing role with the provided request data.
     *
     * @param id      the ID of the role to update
     * @param request the update request containing new role data
     * @return the updated RoleResponse
     */
    @Override
    public RoleResponse updateRole(String id, UpdateRoleRequest request) {
        return null; // implement later
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
}