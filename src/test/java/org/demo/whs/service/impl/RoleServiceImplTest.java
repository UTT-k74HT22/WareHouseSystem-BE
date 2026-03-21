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
import org.springframework.dao.DataIntegrityViolationException;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
// Cho phép lenient toàn bộ class → tránh UnnecessaryStubbingException
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
        // Builder immutable
        request = CreateRoleRequest.builder()
                .name("Admin Manager")
                .description("Test role")
        MockitoAnnotations.openMocks(this);

        // Dummy Role
        role = Role.builder()
                .code("ROLE_ADMIN")
                .name("Admin")
                .description("Administrator role")
                .isDefault(true)
                .build();

        // Role không null để tránh NPE
        role = new Role();
        role.setId("1");
        role.setName(request.getName());

        // Stub chung
        lenient().when(roleMapper.createEntity(any(CreateRoleRequest.class))).thenReturn(role);
        lenient().when(roleMapper.toResponse(any(Role.class))).thenReturn(RoleResponse.builder().build());
        role.setId("role-123"); // fix lỗi builder không có id

        // Dummy Permission
        permission = Permission.builder()
                .code("PERM_READ_USER")
                .name("Read User")
                .build();
        permission.setId("perm-001");
    }

    // ===================== SUCCESS =====================
    @Test
    void createRole_success() {
        when(roleRepository.existsByNameIgnoreCase("Admin Manager")).thenReturn(false);
        when(roleRepository.existsByCode("role_admin_manager")).thenReturn(false);
        when(roleRepository.existsByIsDefaultTrue()).thenReturn(true);
        when(roleRepository.save(any(Role.class))).thenReturn(role);
    void testGetRoleById_Success_WithPermissions() {
        // Mock repository
        when(roleRepository.findById("role-123")).thenReturn(Optional.of(role));
        when(roleRepository.findPermissionsByRoleId("role-123")).thenReturn(List.of(permission));

        RoleResponse response = roleService.createRole(request);
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
        verify(roleRepository).updateAllIsDefaultToFalse();
        verify(roleRepository).save(any(Role.class));
        assertEquals("role-123", response.getId());
        assertEquals(1, response.getPermissions().size());

        // Verify interactions
        verify(roleRepository, times(1)).findById("role-123");
        verify(roleRepository, times(1)).findPermissionsByRoleId("role-123");
        verify(roleMapper, times(1)).toResponseWithPermissions(role, List.of(permission));
    }

    // ===================== NAME NULL =====================
    @Test
    void createRole_nameNull_throwException() {
        request = CreateRoleRequest.builder()
                .name(null)
                .description("Test role")
                .isDefault(true)
    void testGetRoleById_Success_NoPermissions() {
        when(roleRepository.findById("role-123")).thenReturn(Optional.of(role));
        when(roleRepository.findPermissionsByRoleId("role-123")).thenReturn(List.of());

        RoleResponse mockResponse = RoleResponse.builder()
                .id(role.getId())
                .permissions(List.of())
                .build();
        when(roleMapper.toResponseWithPermissions(role, List.of())).thenReturn(mockResponse);

        assertThrows(BadRequestException.class,
                () -> roleService.createRole(request));

        verify(roleRepository, never()).save(any());
    }

    // ===================== NAME DUPLICATE =====================
    @Test
    void createRole_duplicateName_throwException() {
        when(roleRepository.existsByNameIgnoreCase("Admin Manager")).thenReturn(true);

        assertThrows(BadRequestException.class,
                () -> roleService.createRole(request));

        verify(roleRepository, never()).save(any());
    }
        RoleResponse response = roleService.getRoleById("role-123");

        assertNotNull(response);
        assertEquals("role-123", response.getId());
        assertTrue(response.getPermissions().isEmpty());

        verify(roleRepository, times(1)).findById("role-123");
        verify(roleRepository, times(1)).findPermissionsByRoleId("role-123");
        verify(roleMapper, times(1)).toResponseWithPermissions(role, List.of());
    // ===================== CODE DUPLICATE =====================
    @Test
    void createRole_duplicateCode_throwException() {
        // ensure name check passes
        when(roleRepository.existsByNameIgnoreCase("Admin Manager")).thenReturn(false);

        // roleMapper trả non-null → tránh NullPointerException
        Role mockRole = new Role();
        when(roleMapper.createEntity(any(CreateRoleRequest.class))).thenReturn(mockRole);

        // mock save() throw DataIntegrityViolationException → service sẽ catch và throw BadRequestException
        when(roleRepository.save(mockRole)).thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThrows(BadRequestException.class,
                () -> roleService.createRole(request));

        // verify save() đã được gọi
        verify(roleRepository).save(mockRole);
    }

    // ===================== DEFAULT LOGIC =====================
    @Test
    void testGetRoleById_NotFound() {
        when(roleRepository.findById("role-404")).thenReturn(Optional.empty());
    void createRole_firstRole_shouldBeDefault() {
        request = CreateRoleRequest.builder()
                .name("Admin Manager")
                .description("Test role")
                .isDefault(null)
                .build();

        when(roleRepository.existsByNameIgnoreCase("Admin Manager")).thenReturn(false);
        when(roleRepository.existsByCode("role_admin_manager")).thenReturn(false);
        when(roleRepository.save(any(Role.class))).thenReturn(role);
        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> roleService.getRoleById("role-404"));

        when(roleRepository.existsByIsDefaultTrue()).thenReturn(false); // chưa có default role
        // Chỉ check message thôi (không check code)
        assertEquals("Role not found", ex.getMessage());

        roleService.createRole(request);

        // guard → không gọi updateAllIsDefaultToFalse
        verify(roleRepository, never()).updateAllIsDefaultToFalse();
        verify(roleRepository, times(1)).findById("role-404");
        verify(roleRepository, never()).findPermissionsByRoleId(any());
        verify(roleMapper, never()).toResponseWithPermissions(any(), any());
    }
}