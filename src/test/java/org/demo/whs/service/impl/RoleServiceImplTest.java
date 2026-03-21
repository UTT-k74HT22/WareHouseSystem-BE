package org.demo.whs.service.impl;

import org.demo.whs.entity.Permission;
import org.demo.whs.entity.Role;
import org.demo.whs.entity.dto.request.Role.CreateRoleRequest;
import org.demo.whs.entity.dto.response.Permission.PermissionResponse;
import org.demo.whs.entity.dto.response.Role.RoleResponse;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.NotFoundException;
import org.demo.whs.mapper.RoleMapper;
import org.demo.whs.repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RoleServiceImplTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private RoleMapper roleMapper;

    @InjectMocks
    private RoleServiceImpl roleService;

    private CreateRoleRequest request;
    private Role role;
    private Permission permission;

    @BeforeEach
    void setUp() {
        // Immutable request
        request = CreateRoleRequest.builder()
                .name("Admin Manager")
                .description("Test role")
                .isDefault(true)
                .build();

        // Role instance
        role = Role.builder()
                .code("ROLE_ADMIN")
                .name(request.getName())
                .description("Administrator role")
                .isDefault(true)
                .build();
        // Set id thủ công do builder không build được id kế thừa
        role.setId("role-123");

        // Permission dummy
        permission = Permission.builder()
                .code("PERM_READ_USER")
                .name("Read User")
                .build();
        permission.setId("perm-001");

        // Lenient stubs cho mapper
        lenient().when(roleMapper.createEntity(any(CreateRoleRequest.class))).thenReturn(role);
        lenient().when(roleMapper.toResponse(any(Role.class))).thenReturn(RoleResponse.builder()
                .id(role.getId())
                .build());
        lenient().when(roleMapper.toResponseWithPermissions(any(Role.class), anyList())).thenAnswer(invocation -> {
            Role r = invocation.getArgument(0);
            List<Permission> perms = invocation.getArgument(1);
            return RoleResponse.builder()
                    .id(r.getId())
                    .permissions(perms.stream().map(p -> PermissionResponse.builder().id(p.getId()).build()).toList())
                    .build();
        });
    }

    // ===================== CREATE ROLE TESTS =====================
    @Test
    void createRole_success_defaultRoleUpdate() {
        when(roleRepository.existsByNameIgnoreCase(role.getName())).thenReturn(false);
        when(roleRepository.existsByCode("role_admin_manager")).thenReturn(false);
        when(roleRepository.existsByIsDefaultTrue()).thenReturn(true);
        when(roleRepository.save(any(Role.class))).thenReturn(role);

        RoleResponse response = roleService.createRole(request);

        assertNotNull(response);
        assertEquals("role-123", response.getId());
        verify(roleRepository).updateAllIsDefaultToFalse();
        verify(roleRepository).save(any(Role.class));
    }

    @Test
    void createRole_nameNull_throwsBadRequest() {
        CreateRoleRequest invalidRequest = CreateRoleRequest.builder()
                .name(null)
                .description("Test role")
                .isDefault(true)
                .build();

        assertThrows(BadRequestException.class, () -> roleService.createRole(invalidRequest));
        verify(roleRepository, never()).save(any());
    }

    @Test
    void createRole_duplicateName_throwsBadRequest() {
        when(roleRepository.existsByNameIgnoreCase(role.getName())).thenReturn(true);

        assertThrows(BadRequestException.class, () -> roleService.createRole(request));
        verify(roleRepository, never()).save(any());
    }

    @Test
    void createRole_duplicateCode_throwsBadRequest() {
        when(roleRepository.existsByNameIgnoreCase(role.getName())).thenReturn(false);
        Role mockRole = new Role();
        when(roleMapper.createEntity(any(CreateRoleRequest.class))).thenReturn(mockRole);
        when(roleRepository.save(mockRole)).thenThrow(new org.springframework.dao.DataIntegrityViolationException("duplicate"));

        assertThrows(BadRequestException.class, () -> roleService.createRole(request));
        verify(roleRepository).save(mockRole);
    }

    @Test
    void createRole_firstRole_shouldBeDefault() {
        when(roleRepository.existsByNameIgnoreCase(role.getName())).thenReturn(false);
        when(roleRepository.existsByCode("role_admin_manager")).thenReturn(false);
        when(roleRepository.existsByIsDefaultTrue()).thenReturn(false); // chưa có default
        when(roleRepository.save(any(Role.class))).thenReturn(role);

        RoleResponse response = roleService.createRole(request);

        assertNotNull(response);
        assertEquals("role-123", response.getId());
        // guard → không gọi updateAllIsDefaultToFalse
        verify(roleRepository, never()).updateAllIsDefaultToFalse();
    }

    // ===================== GET ROLE BY ID TESTS =====================
    @Test
    void getRoleById_success_withPermissions() {
        when(roleRepository.findById("role-123")).thenReturn(Optional.of(role));
        when(roleRepository.findPermissionsByRoleId("role-123")).thenReturn(List.of(permission));

        RoleResponse response = roleService.getRoleById("role-123");

        assertNotNull(response);
        assertEquals("role-123", response.getId());
        assertEquals(1, response.getPermissions().size());

        verify(roleRepository).findById("role-123");
        verify(roleRepository).findPermissionsByRoleId("role-123");
        verify(roleMapper).toResponseWithPermissions(role, List.of(permission));
    }

    @Test
    void getRoleById_success_noPermissions() {
        when(roleRepository.findById("role-123")).thenReturn(Optional.of(role));
        when(roleRepository.findPermissionsByRoleId("role-123")).thenReturn(List.of());

        RoleResponse response = roleService.getRoleById("role-123");

        assertNotNull(response);
        assertEquals("role-123", response.getId());
        assertTrue(response.getPermissions().isEmpty());
    }

    @Test
    void getRoleById_notFound_throwsNotFound() {
        when(roleRepository.findById("role-404")).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class, () -> roleService.getRoleById("role-404"));
        assertEquals("Role not found", ex.getMessage());
    }
}