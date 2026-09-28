package org.demo.whs.service.impl;

import com.mysql.cj.util.TestUtils;
import org.demo.whs.entity.Permission;
import org.demo.whs.entity.Role;
import org.demo.whs.entity.dto.request.Role.CreateRoleRequest;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.dto.request.Role.UpdateRoleRequest;
import org.demo.whs.entity.dto.response.Permission.PermissionResponse;
import org.demo.whs.entity.dto.response.Role.RoleResponse;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.exception.NotFoundException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.demo.whs.mapper.RoleMapper;
import org.demo.whs.repository.AccountHasRoleRepository;
import org.demo.whs.repository.RolePermissionRepository;
import org.demo.whs.repository.RoleRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import org.springframework.data.domain.*;

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

    @Mock
    private AccountHasRoleRepository accountHasRoleRepository;

    @Mock
    private RolePermissionRepository rolePermissionRepository;

    private Role role1;
    private Role role2;
    private Role role;
    private Permission permission;
    private CreateRoleRequest request;

    @BeforeEach
    void setUp() {

        // ====== ROLE LIST ======
        role1 = new Role();
        role1.setId("1");

        role2 = new Role();
        role2.setId("2");

        // ====== CREATE ROLE ======
        request = CreateRoleRequest.builder()
                .name("Admin Manager")
                .description("Test role")
                .isDefault(true)
                .build();

        role = Role.builder()
                .code("ROLE_MANAGER")
                .name(request.getName())
                .description("Administrator role")
                .isDefault(true)
                .build();
        role.setId("role-123");

        permission = Permission.builder()
                .code("PERM_READ_USER")
                .name("Read User")
                .build();
        permission.setId("perm-001");

        // ====== LENIENT MAPPER ======
        lenient().when(roleMapper.createEntity(any(CreateRoleRequest.class))).thenReturn(role);

        lenient().when(roleMapper.toResponse(any(Role.class)))
                .thenAnswer(invocation -> {
                    Role r = invocation.getArgument(0);
                    return RoleResponse.builder().id(r.getId()).build();
                });

        lenient().when(roleMapper.toResponseWithPermissions(any(Role.class), anyList()))
                .thenAnswer(invocation -> {
                    Role r = invocation.getArgument(0);
                    List<Permission> perms = invocation.getArgument(1);
                    return RoleResponse.builder()
                            .id(r.getId())
                            .permissions(perms.stream()
                                    .map(p -> PermissionResponse.builder().id(p.getId()).build())
                                    .toList())
                            .build();
                });

        lenient().when(roleMapper.toDetailResponse(any(Role.class), anyLong(), anyLong()))
                .thenAnswer(invocation -> {
                    Role r = invocation.getArgument(0);
                    Long permissionCount = invocation.getArgument(1);
                    Long userCount = invocation.getArgument(2);

                    return RoleResponse.builder()
                            .id(r.getId())
                            .permissionCount(permissionCount)
                            .userCount(userCount)
                            .build();
                });
    }

    // =====================================================
    // ===================== GET ROLES ======================
    // =====================================================

    @Test
    void getRoles_success() {

        Pageable pageable = PageRequest.of(0, 2);

        Page<Role> rolePage = new PageImpl<>(List.of(role1, role2), pageable, 2);

        when(roleRepository.findAll(
                ArgumentMatchers.<org.springframework.data.jpa.domain.Specification<Role>>any(),
                eq(pageable)
        )).thenReturn(rolePage);

        when(roleRepository.countPermissionsByRoleIds(any()))
                .thenReturn(List.of(
                        new Object[]{"1", 3L},
                        new Object[]{"2", 5L}
                ));

        when(roleRepository.countUsersByRoleIds(any()))
                .thenReturn(List.of(
                        new Object[]{"1", 10L},
                        new Object[]{"2", 20L}
                ));

        PageResponse<RoleResponse> response =
                roleService.getRoles(null, null, pageable);

        assertNotNull(response);
        assertEquals(2, response.getContent().size());
    }

    @Test
    void getRoles_empty() {

        Pageable pageable = PageRequest.of(0, 2);

        Page<Role> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(roleRepository.findAll(
                ArgumentMatchers.<org.springframework.data.jpa.domain.Specification<Role>>any(),
                eq(pageable)
        )).thenReturn(emptyPage);

        PageResponse<RoleResponse> response =
                roleService.getRoles(null, null, pageable);

        assertTrue(response.getContent().isEmpty());

        verify(roleRepository, never()).countPermissionsByRoleIds(any());
        verify(roleRepository, never()).countUsersByRoleIds(any());
    }

    @Test
    void getRoles_missingCount_shouldDefaultZero() {

        Pageable pageable = PageRequest.of(0, 2);

        Page<Role> rolePage = new PageImpl<>(List.of(role1), pageable, 1);

        when(roleRepository.findAll(
                ArgumentMatchers.<org.springframework.data.jpa.domain.Specification<Role>>any(),
                eq(pageable)
        )).thenReturn(rolePage);

        when(roleRepository.countPermissionsByRoleIds(any())).thenReturn(List.of());
        when(roleRepository.countUsersByRoleIds(any())).thenReturn(List.of());

        PageResponse<RoleResponse> response =
                roleService.getRoles(null, null, pageable);

        assertEquals(0L, response.getContent().get(0).getPermissionCount());
        assertEquals(0L, response.getContent().get(0).getUserCount());
    }

    @Test
    void getRoles_shouldCallBatchCountOnlyOnce() {

        Pageable pageable = PageRequest.of(0, 2);

        Page<Role> rolePage = new PageImpl<>(List.of(role1, role2), pageable, 2);

        when(roleRepository.findAll(
                ArgumentMatchers.<org.springframework.data.jpa.domain.Specification<Role>>any(),
                eq(pageable)
        )).thenReturn(rolePage);
        when(roleRepository.countPermissionsByRoleIds(any())).thenReturn(List.of());
        when(roleRepository.countUsersByRoleIds(any())).thenReturn(List.of());

        roleService.getRoles(null, null, pageable);

        verify(roleRepository, times(1)).countPermissionsByRoleIds(any());
        verify(roleRepository, times(1)).countUsersByRoleIds(any());
    }

    // =====================================================
    // ===================== CREATE ROLE ====================
    // =====================================================

    @Test
    void createRole_success_defaultRoleUpdate() {

        when(roleRepository.existsByNameIgnoreCase(role.getName())).thenReturn(false);
        when(roleRepository.existsByCode(any())).thenReturn(false);
        when(roleRepository.existsByIsDefaultTrue()).thenReturn(true);
        when(roleRepository.save(any(Role.class))).thenReturn(role);

        RoleResponse response = roleService.createRole(request);

        assertNotNull(response);
        assertEquals("role-123", response.getId());

        verify(roleRepository).updateAllIsDefaultToFalse();
    }

    @Test
    void createRole_nameNull_throwsBadRequest() {

        CreateRoleRequest invalidRequest = CreateRoleRequest.builder()
                .name(null)
                .build();

        assertThrows(BadRequestException.class,
                () -> roleService.createRole(invalidRequest));

        verify(roleRepository, never()).save(any());
    }

    @Test
    void createRole_duplicateName_throwsBadRequest() {

        when(roleRepository.existsByNameIgnoreCase(role.getName())).thenReturn(true);

        assertThrows(BadRequestException.class,
                () -> roleService.createRole(request));
    }

    @Test
    void createRole_firstRole_shouldBeDefault() {

        when(roleRepository.existsByNameIgnoreCase(role.getName())).thenReturn(false);
        when(roleRepository.existsByCode(any())).thenReturn(false);
        when(roleRepository.existsByIsDefaultTrue()).thenReturn(false);
        when(roleRepository.save(any())).thenReturn(role);

        roleService.createRole(request);

        verify(roleRepository, never()).updateAllIsDefaultToFalse();
    }

    // =====================================================
    // ===================== GET BY ID ======================
    // =====================================================

    @Test
    void getRoleById_success_withPermissions() {

        when(roleRepository.findById("role-123")).thenReturn(Optional.of(role));
        when(roleRepository.findPermissionsByRoleId("role-123"))
                .thenReturn(List.of(permission));

        RoleResponse response = roleService.getRoleById("role-123");

        assertEquals(1, response.getPermissions().size());
    }

    @Test
    void getRoleById_success_noPermissions() {

        when(roleRepository.findById("role-123")).thenReturn(Optional.of(role));
        when(roleRepository.findPermissionsByRoleId("role-123"))
                .thenReturn(List.of());

        RoleResponse response = roleService.getRoleById("role-123");

        assertTrue(response.getPermissions().isEmpty());
    }

    @Test
    void getRoleById_notFound() {

        when(roleRepository.findById("role-404")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> roleService.getRoleById("role-404"));
    }

    @Test
    void testUpdateRole_Success() {
        // Given
        String roleId = "role-123";

        UpdateRoleRequest request = UpdateRoleRequest.builder()
                .name("Manager")
                .description("Updated role")
                .isDefault(true)
                .build();

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));

        Role updatedRole = Role.builder()
                .code("ROLE_MANAGER")
                .name("Manager")
                .description("Updated role")
                .isDefault(true)
                .build();

        when(roleRepository.save(any(Role.class))).thenReturn(updatedRole);

        RoleResponse mockResponse = RoleResponse.builder()
                .id(roleId)
                .name("Manager")
                .build();

        when(roleMapper.toResponse(updatedRole)).thenReturn(mockResponse);

        // When
        RoleResponse response = roleService.updateRole(roleId, request);

        // Then
        assertNotNull(response);
        assertEquals("Manager", response.getName());

        verify(roleRepository).findById(roleId);
        verify(roleRepository).save(any(Role.class));
        verify(roleMapper).toResponse(updatedRole);
    }

    @Test
    void testUpdateRole_NotFound() {
        when(roleRepository.findById("invalid"))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> roleService.updateRole("invalid",
                        UpdateRoleRequest.builder().build()));
    }

    @Test
    void testUpdateRole_SetDefault() {
        String roleId = "role-123";

        // 🔥 Override lại role cho test này
        role.setIsDefault(false);

        UpdateRoleRequest request = UpdateRoleRequest.builder()
                .isDefault(true)
                .build();

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
        when(roleRepository.save(any(Role.class))).thenAnswer(i -> i.getArgument(0));
        when(roleMapper.toResponse(any())).thenReturn(RoleResponse.builder().build());

        roleService.updateRole(roleId, request);

        verify(roleRepository).updateAllIsDefaultToFalse(); // ✅ pass
        verify(roleRepository).save(any(Role.class));
    }

    // =====================================================
// ===================== DELETE ROLE ==================
// =====================================================

    @Test
    void deleteRole_success() {
        // Given
        role.setIsDefault(false); // không phải role mặc định
        when(roleRepository.findById("role-123")).thenReturn(Optional.of(role));
        when(accountHasRoleRepository.countByIdRoleId("role-123")).thenReturn(0L);

        // When
        assertDoesNotThrow(() -> roleService.deleteRole("role-123"));

        // Then
        verify(roleRepository).delete(role);
    }

    @Test
    void deleteRole_notFound_throwsNotFoundException() {
        // Given
        when(roleRepository.findById("role-404")).thenReturn(Optional.empty());

        // When / Then
        assertThrows(NotFoundException.class, () -> roleService.deleteRole("role-404"));
    }

    @Test
    void deleteRole_systemAdmin_throwsBadRequestException() {
        Role adminRole = Role.builder()
                .code("ROLE_ADMIN")
                .name("ADMIN")
                .isDefault(false)
                .build();
        when(roleRepository.findById("role-admin")).thenReturn(Optional.of(adminRole));

        assertThatThrownBy(() -> roleService.deleteRole("role-admin"))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROLE_007.getCode());

        verify(roleRepository, never()).delete(any(Role.class));
    }

    @Test
    void updateRole_systemAdmin_throwsBadRequestException() {
        Role adminRole = Role.builder()
                .code("ROLE_ADMIN")
                .name("ADMIN")
                .isDefault(false)
                .build();
        when(roleRepository.findById("role-admin")).thenReturn(Optional.of(adminRole));

        UpdateRoleRequest request = UpdateRoleRequest.builder()
                .description("Hacked")
                .build();

        assertThatThrownBy(() -> roleService.updateRole("role-admin", request))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROLE_007.getCode());
    }

    @Test
    void updateRole_unsetDefault_throwsBadRequestException() {
        Role defaultRole = Role.builder()
                .code("ROLE_MANAGER")
                .name("Manager")
                .isDefault(true)
                .build();
        when(roleRepository.findById("role-default")).thenReturn(Optional.of(defaultRole));

        UpdateRoleRequest request = UpdateRoleRequest.builder()
                .isDefault(false)
                .build();

        assertThatThrownBy(() -> roleService.updateRole("role-default", request))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROLE_005.getCode());
    }

    @Test
    void deleteRole_isDefault_throwsBadRequestException() {
        // Given
        role.setIsDefault(true);
        when(roleRepository.findById("role-123")).thenReturn(Optional.of(role));

        // When / Then
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> roleService.deleteRole("role-123"));

        assertEquals("ROLE_005", exception.getErrorCode());
    }

    @Test
    void deleteRole_roleInUse_throwsBadRequestException() {
        // Given
        role.setIsDefault(false);
        when(roleRepository.findById("role-123")).thenReturn(Optional.of(role));
        when(accountHasRoleRepository.countByIdRoleId("role-123")).thenReturn(5L); // role đang dùng

        // When / Then
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> roleService.deleteRole("role-123"));

        assertEquals("ROLE_006", exception.getErrorCode());
    }

    @Test
    void deleteRole_invalidId_throwsBadRequestException() {
        // Given
        String invalidId = ""; // empty id

        // When / Then
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> roleService.deleteRole(invalidId));

        assertEquals("COM_001", exception.getErrorCode());
    }
}