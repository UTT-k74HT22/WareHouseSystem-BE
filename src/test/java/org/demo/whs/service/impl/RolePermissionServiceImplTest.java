package org.demo.whs.service.impl;

import org.demo.whs.entity.Permission;
import org.demo.whs.entity.Role;
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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RolePermissionServiceImplTest {

    @Mock
    private RolePermissionRepository rolePermissionRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private RolePermissionMapper rolePermissionMapper;

    @InjectMocks
    private RolePermissionServiceImpl rolePermissionService;

    private String roleId;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        roleId = "role-1";
        pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
    }

    // =========================
    // SUCCESS
    // =========================
    @Test
    void getRolePermissions_success() {
        // given
        Permission permission = new Permission();
        permission.setId("perm-1");

        PermissionResponse response = PermissionResponse.builder()
                .id("perm-1")
                .build();

        Page<Permission> permissionPage =
                new PageImpl<>(java.util.List.of(permission), pageable, 1);

        when(roleRepository.findById(roleId))
                .thenReturn(Optional.of(new Role()));

        when(rolePermissionRepository.findPermissionsByRoleId(roleId, null, pageable))
                .thenReturn(permissionPage);

        when(rolePermissionMapper.toResponse(permission))
                .thenReturn(response);

        // when
        PageResponse<PermissionResponse> result =
                rolePermissionService.getRolePermissions(roleId, null, pageable);

        // then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("perm-1", result.getContent().get(0).getId());

        verify(roleRepository).findById(roleId);
        verify(rolePermissionRepository)
                .findPermissionsByRoleId(roleId, null, pageable);
        verify(rolePermissionMapper).toResponse(permission);
    }

    // =========================
    // VALIDATION ERROR
    // =========================
    @Test
    void getRolePermissions_roleIdNull_throwBadRequest() {
        assertThrows(BadRequestException.class, () ->
                rolePermissionService.getRolePermissions(null, null, pageable)
        );

        verifyNoInteractions(roleRepository);
    }

    @Test
    void getRolePermissions_roleIdEmpty_throwBadRequest() {
        assertThrows(BadRequestException.class, () ->
                rolePermissionService.getRolePermissions("", null, pageable)
        );

        verifyNoInteractions(roleRepository);
    }

    // =========================
    // ROLE NOT FOUND
    // =========================
    @Test
    void getRolePermissions_roleNotFound_throwNotFound() {
        when(roleRepository.findById(roleId))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
                rolePermissionService.getRolePermissions(roleId, null, pageable)
        );

        verify(roleRepository).findById(roleId);
        verifyNoInteractions(rolePermissionRepository);
    }

    // =========================
    // FILTER RESOURCE
    // =========================
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
                new PageImpl<>(java.util.List.of(permission), pageable, 1);

        when(roleRepository.findById(roleId))
                .thenReturn(Optional.of(new Role()));

        when(rolePermissionRepository.findPermissionsByRoleId(roleId, resource, pageable))
                .thenReturn(permissionPage);

        when(rolePermissionMapper.toResponse(permission))
                .thenReturn(response);

        PageResponse<PermissionResponse> result =
                rolePermissionService.getRolePermissions(roleId, resource, pageable);

        assertEquals(1, result.getContent().size());
        assertEquals(resource, result.getContent().get(0).getResource());

        verify(rolePermissionRepository)
                .findPermissionsByRoleId(roleId, resource, pageable);
    }
}