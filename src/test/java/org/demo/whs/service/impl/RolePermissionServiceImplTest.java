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
import org.demo.whs.repository.AccountHasRoleRepository;
import org.demo.whs.repository.PermissionRepository;
import org.demo.whs.repository.RolePermissionRepository;
import org.demo.whs.repository.RoleRepository;
import org.demo.whs.service.PermissionCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import org.springframework.data.domain.*;

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

    @Mock
    private AccountHasRoleRepository accountHasRoleRepository;

    @Mock
    private PermissionCacheService permissionCacheService;

    @InjectMocks
    private RolePermissionServiceImpl service;

    private AssignPermissionsRequest request;
    private String roleId;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        request = new AssignPermissionsRequest();
        request.setPermissionIds(List.of("p1", "p2"));

        roleId = "role-1";
        pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
    }

    // =========================
    // ✅ ASSIGN PERMISSIONS
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
        when(accountHasRoleRepository.findAccountIdsByRoleId(roleId))
                .thenReturn(List.of("u1", "u2"));

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
        verify(permissionCacheService).evictPermissions("u1");
        verify(permissionCacheService).evictPermissions("u2");
    }

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

    @Test
    void assignPermissions_roleNotFound_shouldThrow() {

        when(roleRepository.findById("r1"))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> service.assignPermissions("r1", request));
    }

    @Test
    void assignPermissions_permissionNotFound_shouldThrow() {

        when(roleRepository.findById("r1"))
                .thenReturn(Optional.of(new Role()));

        when(permissionRepository.findAllById(List.of("p1", "p2")))
                .thenReturn(List.of(new Permission()));

        assertThrows(NotFoundException.class,
                () -> service.assignPermissions("r1", request));
    }

    @Test
    void assignPermissions_existingPermission_shouldSkip() {

        String roleId = "r1";

        Permission p1 = new Permission();
        p1.setId("p1");

        Permission p2 = new Permission();
        p2.setId("p2");

        when(roleRepository.findById(roleId))
                .thenReturn(Optional.of(new Role()));

        when(permissionRepository.findAllById(List.of("p1", "p2")))
                .thenReturn(List.of(p1, p2));

        when(rolePermissionRepository.findPermissionIdsByRoleId(roleId))
                .thenReturn(List.of("p1"));
        when(accountHasRoleRepository.findAccountIdsByRoleId(roleId))
                .thenReturn(List.of("u1"));

        RoleHasPermission entity = new RoleHasPermission();
        when(rolePermissionMapper.createEntity(eq(roleId), eq("p2")))
                .thenReturn(entity);

        when(rolePermissionMapper.toResponse(any()))
                .thenReturn(new PermissionResponse());

        List<PermissionResponse> result =
                service.assignPermissions(roleId, request);

        verify(rolePermissionRepository)
                .saveAll(argThat(iterable -> ((List<?>) iterable).size() == 1));
        verify(permissionCacheService).evictPermissions("u1");

        assertEquals(2, result.size());
    }

    // =========================
    // REMOVE PERMISSION
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
        when(accountHasRoleRepository.findAccountIdsByRoleId(roleId))
                .thenReturn(List.of("u1"));

        assertDoesNotThrow(() ->
                service.removePermission(roleId, permissionId)
        );

        verify(rolePermissionRepository)
                .deleteByIdRoleIdAndIdPermissionId(roleId, permissionId);
        verify(permissionCacheService).evictPermissions("u1");
    }

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

    @Test
    void removePermission_roleNotFound_shouldThrow() {

        when(roleRepository.findById("r1"))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> service.removePermission("r1", "p1"));
    }

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

    // =========================
    // GET ROLE PERMISSIONS (NEW)
    // =========================

    @Test
    void getRolePermissions_success() {

        Permission permission = new Permission();
        permission.setId("perm-1");

        PermissionResponse response = PermissionResponse.builder()
                .id("perm-1")
                .build();

        Page<Permission> permissionPage =
                new PageImpl<>(List.of(permission), pageable, 1);

        when(roleRepository.findById(roleId))
                .thenReturn(Optional.of(new Role()));

        when(rolePermissionRepository.findPermissionsByRoleId(roleId, null, pageable))
                .thenReturn(permissionPage);

        when(rolePermissionMapper.toResponse(permission))
                .thenReturn(response);

        PageResponse<PermissionResponse> result =
                service.getRolePermissions(roleId, null, pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("perm-1", result.getContent().get(0).getId());
    }

    @Test
    void getRolePermissions_roleIdNull_throwBadRequest() {
        assertThrows(BadRequestException.class, () ->
                service.getRolePermissions(null, null, pageable)
        );
    }

    @Test
    void getRolePermissions_roleIdEmpty_throwBadRequest() {
        assertThrows(BadRequestException.class, () ->
                service.getRolePermissions("", null, pageable)
        );
    }

    @Test
    void getRolePermissions_roleNotFound_throwNotFound() {

        when(roleRepository.findById(roleId))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
                service.getRolePermissions(roleId, null, pageable)
        );
    }

    @Test
    void getRolePermissions_filterByResource_success() {

        String resource = "inventory";

        Permission permission = new Permission();
        permission.setId("perm-2");

        PermissionResponse response = PermissionResponse.builder()
                .id("perm-2")
                .resource(resource)
                .build();

        Page<Permission> permissionPage =
                new PageImpl<>(List.of(permission), pageable, 1);

        when(roleRepository.findById(roleId))
                .thenReturn(Optional.of(new Role()));

        when(rolePermissionRepository.findPermissionsByRoleId(roleId, resource, pageable))
                .thenReturn(permissionPage);

        when(rolePermissionMapper.toResponse(permission))
                .thenReturn(response);

        PageResponse<PermissionResponse> result =
                service.getRolePermissions(roleId, resource, pageable);

        assertEquals(1, result.getContent().size());
        assertEquals(resource, result.getContent().get(0).getResource());
    }
}
