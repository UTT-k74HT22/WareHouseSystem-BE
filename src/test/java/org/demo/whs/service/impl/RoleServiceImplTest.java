package org.demo.whs.service.impl;

import org.demo.whs.entity.Permission;
import org.demo.whs.entity.Role;
import org.demo.whs.entity.dto.response.Permission.PermissionResponse;
import org.demo.whs.entity.dto.response.Role.RoleResponse;
import org.demo.whs.exception.NotFoundException;
import org.demo.whs.mapper.RoleMapper;
import org.demo.whs.repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RoleServiceImplTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private RoleMapper roleMapper;

    @InjectMocks
    private RoleServiceImpl roleService;

    private Role role;
    private Permission permission;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Dummy Role
        role = Role.builder()
                .code("ROLE_ADMIN")
                .name("Admin")
                .description("Administrator role")
                .isDefault(true)
                .build();
        role.setId("role-123"); // fix lỗi builder không có id

        // Dummy Permission
        permission = Permission.builder()
                .code("PERM_READ_USER")
                .name("Read User")
                .build();
        permission.setId("perm-001");
    }

    @Test
    void testGetRoleById_Success_WithPermissions() {
        // Mock repository
        when(roleRepository.findById("role-123")).thenReturn(Optional.of(role));
        when(roleRepository.findPermissionsByRoleId("role-123")).thenReturn(List.of(permission));

        // Mock mapper
        RoleResponse mockResponse = RoleResponse.builder()
                .id(role.getId())
                .permissions(List.of(PermissionResponse.builder().id(permission.getId()).build()))
                .build();
        when(roleMapper.toResponseWithPermissions(role, List.of(permission))).thenReturn(mockResponse);

        // Call service
        RoleResponse response = roleService.getRoleById("role-123");

        // Assertions
        assertNotNull(response);
        assertEquals("role-123", response.getId());
        assertEquals(1, response.getPermissions().size());

        // Verify interactions
        verify(roleRepository, times(1)).findById("role-123");
        verify(roleRepository, times(1)).findPermissionsByRoleId("role-123");
        verify(roleMapper, times(1)).toResponseWithPermissions(role, List.of(permission));
    }

    @Test
    void testGetRoleById_Success_NoPermissions() {
        when(roleRepository.findById("role-123")).thenReturn(Optional.of(role));
        when(roleRepository.findPermissionsByRoleId("role-123")).thenReturn(List.of());

        RoleResponse mockResponse = RoleResponse.builder()
                .id(role.getId())
                .permissions(List.of())
                .build();
        when(roleMapper.toResponseWithPermissions(role, List.of())).thenReturn(mockResponse);

        RoleResponse response = roleService.getRoleById("role-123");

        assertNotNull(response);
        assertEquals("role-123", response.getId());
        assertTrue(response.getPermissions().isEmpty());

        verify(roleRepository, times(1)).findById("role-123");
        verify(roleRepository, times(1)).findPermissionsByRoleId("role-123");
        verify(roleMapper, times(1)).toResponseWithPermissions(role, List.of());
    }

    @Test
    void testGetRoleById_NotFound() {
        when(roleRepository.findById("role-404")).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> roleService.getRoleById("role-404"));

        // Chỉ check message thôi (không check code)
        assertEquals("Role not found", ex.getMessage());

        verify(roleRepository, times(1)).findById("role-404");
        verify(roleRepository, never()).findPermissionsByRoleId(any());
        verify(roleMapper, never()).toResponseWithPermissions(any(), any());
    }
}