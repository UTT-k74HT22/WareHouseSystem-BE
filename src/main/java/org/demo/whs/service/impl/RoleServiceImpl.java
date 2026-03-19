package org.demo.whs.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.Role;
import org.demo.whs.entity.dto.request.Role.CreateRoleRequest;
import org.demo.whs.entity.dto.request.Role.UpdateRoleRequest;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.dto.response.Role.RoleResponse;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.mapper.RoleMapper;
import org.demo.whs.repository.RoleRepository;
import org.demo.whs.service.RoleService;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final RoleMapper roleMapper;

    /**
     * Creates a new role in the system.
     *
     * @param request the role creation request
     * @return the created role response
     */
    @Override
    @Transactional
    public RoleResponse createRole(CreateRoleRequest request) {

        validateRoleName(request.getName());

        boolean isDefault = determineIsDefault(request.getIsDefault());

        handleDefaultRole(isDefault);

        String code = generateRoleCode(request.getName());

        if (roleRepository.existsByCode(code)) {
            throw new BadRequestException(ErrorCode.ROLE_004);
        }

        Role role = roleMapper.createEntity(request);
        role.setCode(code);
        role.setIsDefault(isDefault);

        Role savedRole = roleRepository.save(role);

        log.info("CREATE ROLE: id={}, code={}, name={}, isDefault={}",
                savedRole.getId(),
                savedRole.getCode(),
                savedRole.getName(),
                savedRole.getIsDefault());

        return roleMapper.toResponse(savedRole);
    }

    @Override
    public PageResponse<RoleResponse> getRoles(Pageable pageable) {
        return null;
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
     * Validates role name.
     *
     * @param name role name
     */
    private void validateRoleName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new BadRequestException(ErrorCode.ROLE_002);
        }

        if (roleRepository.existsByName(name)) {
            throw new BadRequestException(ErrorCode.ROLE_004);
        }
    }

    /**
     * Determines whether the new role should be set as default.
     *
     * @param requestIsDefault flag from request
     * @return true if role should be default, otherwise false
     */
    private boolean determineIsDefault(Boolean requestIsDefault) {
        if(Boolean.TRUE.equals(requestIsDefault)) {
            return true;
        }

        return !roleRepository.existsByIsDefaultTrue();
    }

    /**
     * Ensures that only one default role exists in the system.
     *
     * @param isDefault whether the new role is default
     */
    private void handleDefaultRole(boolean isDefault) {
        if (isDefault) {
            roleRepository.updateAllIsDefaultToFalse();
        }
    }

    /**
     * Generates role code based on role name.
     *
     * @param name role name
     * @return generated role code
     */
    private String generateRoleCode(String name) {

        if (name == null || name.trim().isEmpty()) {
            throw new BadRequestException(ErrorCode.ROLE_002);
        }

        String normalized = name.trim()
                .toLowerCase()
                .replaceAll("[^a-z0-9\\s]", "")
                .replaceAll("\\s+", "_");

        return "role_" + normalized;
    }
}
