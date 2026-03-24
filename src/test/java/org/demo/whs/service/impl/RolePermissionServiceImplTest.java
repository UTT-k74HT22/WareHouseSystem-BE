package org.demo.whs.service.impl;

import org.demo.whs.entity.Permission;
import org.demo.whs.entity.Role;
import org.demo.whs.entity.RoleHasPermission;
import org.demo.whs.entity.dto.request.RolePermission.AssignPermissionsRequest;
import org.demo.whs.entity.dto.response.PageResponse;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.mockito.*;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
@ExtendWith(MockitoExtension.class)
class RolePermissionServiceImplTest {

    @Mock
    private RolePermissionRepository rolePermissionRepository;
    private RoleRepository roleRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private RolePermissionRepository rolePermissionRepository;
    private RoleRepository roleRepository;

    @Mock
    private RolePermissionMapper rolePermissionMapper;

    @InjectMocks
    private RolePermissionServiceImpl rolePermissionService;
    private RolePermissionServiceImpl service;

    private String roleId;
    private Pageable pageable;
    private AssignPermissionsRequest request;

    @BeforeEach
    void setUp() {
        request = new AssignPermissionsRequest();
        request.setPermissionIds(List.of("p1", "p2"));
        roleId = "role-1";
        pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
    }

    // =========================
    // SUCCESS
    // ✅ SUCCESS CASE
    // =========================
    @Test
    void assignPermissions_success() {

        String roleId = "r1";
    void getRolePermissions_success() {
        // given
        Permission permission = new Permission();
        permission.setId("perm-1");

        Role role = new Role();
        Permission p1 = new Permission();
        p1.setId("p1");
        PermissionResponse response = PermissionResponse.builder()
                .id("perm-1")
                .build();

        Page<Permission> permissionPage =
                new PageImpl<>(java.util.List.of(permission), pageable, 1);
        Permission p2 = new Permission();
        p2.setId("p2");

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
        when(permissionRepository.findAllById(List.of("p1", "p2")))
                .thenReturn(List.of(p1, p2));
        when(rolePermissionRepository.findPermissionIdsByRoleId(roleId))
                .thenReturn(List.of());
        when(roleRepository.findById(roleId))
                .thenReturn(Optional.of(new Role()));

        when(rolePermissionRepository.findPermissionsByRoleId(roleId, null, pageable))
                .thenReturn(permissionPage);
        RoleHasPermission e1 = new RoleHasPermission();
        RoleHasPermission e2 = new RoleHasPermission();

        when(rolePermissionMapper.createEntity(roleId, "p1")).thenReturn(e1);
        when(rolePermissionMapper.createEntity(roleId, "p2")).thenReturn(e2);
        when(rolePermissionMapper.toResponse(permission))
                .thenReturn(response);

        // when
        PageResponse<PermissionResponse> result =
                rolePermissionService.getRolePermissions(roleId, null, pageable);
        PermissionResponse r1 = new PermissionResponse();
        PermissionResponse r2 = new PermissionResponse();

        // then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("perm-1", result.getContent().get(0).getId());
        when(rolePermissionMapper.toResponse(p1)).thenReturn(r1);
        when(rolePermissionMapper.toResponse(p2)).thenReturn(r2);

        List<PermissionResponse> result =
                service.assignPermissions(roleId, request);

        assertEquals(2, result.size());
        verify(rolePermissionRepository).saveAll(List.of(e1, e2));
        verify(roleRepository).findById(roleId);
        verify(rolePermissionRepository)
                .findPermissionsByRoleId(roleId, null, pageable);
        verify(rolePermissionMapper).toResponse(permission);
    }

    // =========================
    // VALIDATION ERROR
    // ❌ BAD REQUEST
    // =========================
    @Test
    void getRolePermissions_roleIdNull_throwBadRequest() {
        assertThrows(BadRequestException.class, () ->
                rolePermissionService.getRolePermissions(null, null, pageable)
        );

        verifyNoInteractions(roleRepository);
    void assignPermissions_nullRequest_shouldThrow() {
        assertThrows(BadRequestException.class,
                () -> service.assignPermissions("r1", null));
    }

    @Test
    void assignPermissions_emptyPermissionIds_shouldThrow() {
        request.setPermissionIds(List.of());
    void getRolePermissions_roleIdEmpty_throwBadRequest() {
        assertThrows(BadRequestException.class, () ->
                rolePermissionService.getRolePermissions("", null, pageable)
        );

        assertThrows(BadRequestException.class,
                () -> service.assignPermissions("r1", request));
        verifyNoInteractions(roleRepository);
    }

    // =========================
    // ❌ ROLE NOT FOUND
    // ROLE NOT FOUND
    // =========================
    @Test
    void getRolePermissions_roleNotFound_throwNotFound() {
        when(roleRepository.findById(roleId))
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
        assertThrows(NotFoundException.class, () ->
                rolePermissionService.getRolePermissions(roleId, null, pageable)
        );

        verify(roleRepository).findById(roleId);
        verifyNoInteractions(rolePermissionRepository);
        when(roleRepository.findById("r1"))
                .thenReturn(Optional.of(new Role()));

        // trả về thiếu permission
        when(permissionRepository.findAllById(List.of("p1", "p2")))
                .thenReturn(List.of(new Permission()));

        assertThrows(NotFoundException.class,
                () -> service.assignPermissions("r1", request));
    }

    // =========================
    // FILTER RESOURCE
    // ⚠️ DUPLICATE (ALREADY EXISTS)
    // =========================
    @Test
    void getRolePermissions_filterByResource_success() {
        String resource = "inventory";
    void assignPermissions_existingPermission_shouldSkip() {

        Permission permission = new Permission();
        permission.setId("perm-2");
        String roleId = "r1";

        PermissionResponse response = PermissionResponse.builder()
                .id("perm-2")
                .resource(resource)
                .build();
        Permission p1 = new Permission();
        p1.setId("p1");

        Permission p2 = new Permission();
        p2.setId("p2"); // ✅ FIX
        Page<Permission> permissionPage =
                new PageImpl<>(java.util.List.of(permission), pageable, 1);

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
        when(rolePermissionRepository.findPermissionsByRoleId(roleId, resource, pageable))
                .thenReturn(permissionPage);

        when(rolePermissionMapper.toResponse(permission))
                .thenReturn(response);
    // =========================
// REMOVE PERMISSION TEST
// =========================

        PageResponse<PermissionResponse> result =
                rolePermissionService.getRolePermissions(roleId, resource, pageable);
    // ✅ SUCCESS CASE
    @Test
    void removePermission_success() {

        assertEquals(1, result.getContent().size());
        assertEquals(resource, result.getContent().get(0).getResource());
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
                .findPermissionsByRoleId(roleId, resource, pageable);
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