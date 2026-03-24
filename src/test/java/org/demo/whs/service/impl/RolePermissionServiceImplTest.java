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

    private AssignPermissionsRequest request;

    @BeforeEach
    void setUp() {
        request = new AssignPermissionsRequest();
        request.setPermissionIds(List.of("p1", "p2"));
    }

    // =========================
    // ✅ SUCCESS CASE
    // =========================
    @Test
    void assignPermissions_success() {

        String roleId = "r1";

        Role role = new Role();
        Permission p1 = new Permission();
        p1.setId("p1");

        Permission p2 = new Permission();
        p2.setId("p2");

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
        when(permissionRepository.findAllById(List.of("p1", "p2")))
                .thenReturn(List.of(p1, p2));
        when(rolePermissionRepository.findPermissionIdsByRoleId(roleId))
                .thenReturn(List.of());

        RoleHasPermission e1 = new RoleHasPermission();
        RoleHasPermission e2 = new RoleHasPermission();

        when(rolePermissionMapper.createEntity(roleId, "p1")).thenReturn(e1);
        when(rolePermissionMapper.createEntity(roleId, "p2")).thenReturn(e2);

        PermissionResponse r1 = new PermissionResponse();
        PermissionResponse r2 = new PermissionResponse();

        when(rolePermissionMapper.toResponse(p1)).thenReturn(r1);
        when(rolePermissionMapper.toResponse(p2)).thenReturn(r2);

        List<PermissionResponse> result =
                service.assignPermissions(roleId, request);

        assertEquals(2, result.size());
        verify(rolePermissionRepository).saveAll(List.of(e1, e2));
    }

    // =========================
    // ❌ BAD REQUEST
    // =========================
    @Test
    void assignPermissions_nullRequest_shouldThrow() {
        assertThrows(BadRequestException.class,
                () -> service.assignPermissions("r1", null));
    }

    @Test
    void assignPermissions_emptyPermissionIds_shouldThrow() {
        request.setPermissionIds(List.of());

        assertThrows(BadRequestException.class,
                () -> service.assignPermissions("r1", request));
    }

    // =========================
    // ❌ ROLE NOT FOUND
    // =========================
    @Test
    void assignPermissions_roleNotFound_shouldThrow() {

        when(roleRepository.findById("r1"))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> service.assignPermissions("r1", request));
    }

    // =========================
    // ❌ PERMISSION NOT FOUND
    // =========================
    @Test
    void assignPermissions_permissionNotFound_shouldThrow() {

        when(roleRepository.findById("r1"))
                .thenReturn(Optional.of(new Role()));

        // trả về thiếu permission
        when(permissionRepository.findAllById(List.of("p1", "p2")))
                .thenReturn(List.of(new Permission()));

        assertThrows(NotFoundException.class,
                () -> service.assignPermissions("r1", request));
    }

    // =========================
    // ⚠️ DUPLICATE (ALREADY EXISTS)
    // =========================
    @Test
    void assignPermissions_existingPermission_shouldSkip() {

        String roleId = "r1";

        Permission p1 = new Permission();
        p1.setId("p1");

        Permission p2 = new Permission();
        p2.setId("p2"); // ✅ FIX

        when(roleRepository.findById(roleId))
                .thenReturn(Optional.of(new Role()));

        when(permissionRepository.findAllById(List.of("p1", "p2")))
                .thenReturn(List.of(p1, p2));

        // p1 đã tồn tại
        when(rolePermissionRepository.findPermissionIdsByRoleId(roleId))
                .thenReturn(List.of("p1"));

        RoleHasPermission entity = new RoleHasPermission();
        when(rolePermissionMapper.createEntity(eq(roleId), eq("p2")))
                .thenReturn(entity);

        when(rolePermissionMapper.toResponse(any()))
                .thenReturn(new PermissionResponse());

        List<PermissionResponse> result =
                service.assignPermissions(roleId, request);

        // ✅ chỉ insert 1 cái (p2)
        verify(rolePermissionRepository)
                .saveAll(argThat(iterable -> ((List<?>) iterable).size() == 1));

        assertEquals(2, result.size());
    }

    // =========================
// REMOVE PERMISSION TEST
// =========================

    // ✅ SUCCESS CASE
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

    // ❌ BAD REQUEST
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

    // ❌ ROLE NOT FOUND
    @Test
    void removePermission_roleNotFound_shouldThrow() {

        when(roleRepository.findById("r1"))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> service.removePermission("r1", "p1"));
    }

    // ❌ PERMISSION NOT ASSIGNED
    @Test
    void removePermission_permissionNotAssigned_shouldThrow() {

        String roleId = "r1";
        String permissionId = "p1";

        when(roleRepository.findById(roleId))
                .thenReturn(Optional.of(new Role()));

        when(rolePermissionRepository
                .deleteByIdRoleIdAndIdPermissionId(roleId, permissionId))
                .thenReturn(0);

        assertThrows(NotFoundException.class,
                () -> service.removePermission(roleId, permissionId));
    }
}