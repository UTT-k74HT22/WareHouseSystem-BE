package org.demo.whs.service.impl;


import org.demo.whs.entity.Permission;
import org.demo.whs.entity.Role;
import org.demo.whs.entity.RoleHasPermission;
import org.demo.whs.entity.dto.request.RolePermission.AssignPermissionsRequest;
import org.demo.whs.entity.dto.response.Permission.PermissionResponse;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.NotFoundException;
import org.demo.whs.mapper.RolePermissionMapper;
import org.demo.whs.repository.PermissionRepository;
import org.demo.whs.repository.RolePermissionRepository;
import org.demo.whs.repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class RolePermissionServiceImplTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private RolePermissionRepository rolePermissionRepository;

    @Mock
    private RolePermissionMapper rolePermissionMapper;

    @InjectMocks
    private RolePermissionServiceImpl service;

    // =========================
    // ✅ SUCCESS CASE
    // =========================
    @Test
    void removePermission_success() {

        String roleId = "r1";
        String permissionId = "p1";

        Role role = new Role();

        when(roleRepository.findById(roleId))
                .thenReturn(Optional.of(role));

        when(rolePermissionRepository
                .deleteByIdRoleIdAndIdPermissionId(roleId, permissionId))
                .thenReturn(1);

        assertDoesNotThrow(() ->
                service.removePermission(roleId, permissionId)
        );

        verify(rolePermissionRepository)
                .deleteByIdRoleIdAndIdPermissionId(roleId, permissionId);
    }

    // =========================
    // ❌ BAD REQUEST
    // =========================
    @Test
    void removePermission_nullRoleId_shouldThrow() {

        assertThrows(BadRequestException.class,
                () -> service.removePermission(null, "p1"));
    }

    @Test
    void removePermission_blankPermissionId_shouldThrow() {

        assertThrows(BadRequestException.class,
                () -> service.removePermission("r1", " "));
    }

    // =========================
    // ❌ ROLE NOT FOUND
    // =========================
    @Test
    void removePermission_roleNotFound_shouldThrow() {

        when(roleRepository.findById("r1"))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> service.removePermission("r1", "p1"));
    }

    // =========================
    // ❌ PERMISSION NOT ASSIGNED
    // =========================
    @Test
    void removePermission_permissionNotAssigned_shouldThrow() {

        String roleId = "r1";
        String permissionId = "p1";

        when(roleRepository.findById(roleId))
                .thenReturn(Optional.of(new Role()));

        // delete trả 0 => không tồn tại
        when(rolePermissionRepository
                .deleteByIdRoleIdAndIdPermissionId(roleId, permissionId))
                .thenReturn(0);

        assertThrows(NotFoundException.class,
                () -> service.removePermission(roleId, permissionId));
    }
}
