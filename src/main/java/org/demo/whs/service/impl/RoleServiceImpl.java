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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final RoleMapper roleMapper;

    @Override
    @Transactional
    public RoleResponse createRole(CreateRoleRequest request) {
        validateRoleName(request.getName());

        boolean isDefault = determineIsDefault(request.getIsDefault());

        // Guard để tránh update 0 row
        if (isDefault && roleRepository.existsByIsDefaultTrue()) {
            roleRepository.updateAllIsDefaultToFalse();
        }

        String code = generateRoleCode(request.getName());

        Role role = roleMapper.createEntity(request);
        role.setCode(code);
        role.setIsDefault(isDefault);

        // Wrap save để handle race condition duplicate
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
    public void deleteRole(String id) {}

    // ------------------- PRIVATE METHODS -------------------

    private void validateRoleName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new BadRequestException(ErrorCode.ROLE_002); // "Role name cannot be empty"
        }

        if (roleRepository.existsByNameIgnoreCase(name)) {
            throw new BadRequestException(ErrorCode.ROLE_004); // duplicate
        }
    }

    private boolean determineIsDefault(Boolean requestIsDefault) {
        if (Boolean.TRUE.equals(requestIsDefault)) {
            return true;
        }
        return !roleRepository.existsByIsDefaultTrue();
    }

    private String generateRoleCode(String name) {
        String normalized = name.trim()
                .toLowerCase()
                .replaceAll("[^a-z0-9\\s]", "") // remove special chars
                .replaceAll("\\s+", "_");

        return "role_" + normalized;
    }
}