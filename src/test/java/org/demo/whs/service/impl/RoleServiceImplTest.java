package org.demo.whs.service.impl;

import org.demo.whs.entity.Permission;
import org.demo.whs.entity.Role;
import org.demo.whs.entity.dto.request.Role.CreateRoleRequest;
import org.demo.whs.entity.dto.response.Permission.PermissionResponse;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.dto.response.Role.RoleResponse;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.NotFoundException;
import org.demo.whs.mapper.RoleMapper;
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
import static org.mockito.ArgumentMatchers.*;
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
    private Role role1;
    private Role role2;

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
        role1 = new Role();
        role1.setId("1");
        role1.setName(null);

        role2 = new Role();
        role2.setId("2");
        role2.setName(null);
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

    // ===================== GET ROLES SUCCESS =====================
    // ===================== CREATE ROLE TESTS =====================
    @Test
    void getRoles_success() {

        Pageable pageable = PageRequest.of(0, 2);

        Page<Role> rolePage = new PageImpl<>(List.of(role1, role2), pageable, 2);

        // FIX ambiguous findAll
        when(roleRepository.findAll(
                ArgumentMatchers.<org.springframework.data.jpa.domain.Specification<Role>>any(),
                eq(pageable)
        )).thenReturn(rolePage);
    void createRole_success_defaultRoleUpdate() {
        when(roleRepository.existsByNameIgnoreCase(role.getName())).thenReturn(false);
        when(roleRepository.existsByCode("role_admin_manager")).thenReturn(false);
        when(roleRepository.existsByIsDefaultTrue()).thenReturn(true);
        when(roleRepository.save(any(Role.class))).thenReturn(role);

        RoleResponse response = roleService.createRole(request);
        when(roleMapper.toResponse(role1))
                .thenReturn(RoleResponse.builder().id("1").build());

        when(roleMapper.toResponse(role2))
                .thenReturn(RoleResponse.builder().id("2").build());

        // FIX đúng kiểu List<Object[]>
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
        assertEquals("role-123", response.getId());
        verify(roleRepository).updateAllIsDefaultToFalse();
        verify(roleRepository).save(any(Role.class));
        assertEquals(2, response.getContent().size());
    }

    // ===================== EMPTY =====================
    @Test
    void createRole_nameNull_throwsBadRequest() {
        CreateRoleRequest invalidRequest = CreateRoleRequest.builder()
                .name(null)
                .description("Test role")
                .isDefault(true)
                .build();
    void getRoles_empty() {

        Pageable pageable = PageRequest.of(0, 2);

        Page<Role> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        assertThrows(BadRequestException.class, () -> roleService.createRole(invalidRequest));
        verify(roleRepository, never()).save(any());
        when(roleRepository.findAll(
                ArgumentMatchers.<org.springframework.data.jpa.domain.Specification<Role>>any(),
                eq(pageable)
        )).thenReturn(emptyPage);

        PageResponse<RoleResponse> response =
                roleService.getRoles(null, null, pageable);

        assertNotNull(response);
        assertTrue(response.getContent().isEmpty());

        verify(roleRepository, never()).countPermissionsByRoleIds(any());
        verify(roleRepository, never()).countUsersByRoleIds(any());
    }

    // ===================== FILTER =====================
    @Test
    void createRole_duplicateName_throwsBadRequest() {
        when(roleRepository.existsByNameIgnoreCase(role.getName())).thenReturn(true);
    void getRoles_withFilter() {

        Pageable pageable = PageRequest.of(0, 2);

        Page<Role> rolePage = new PageImpl<>(List.of(role1), pageable, 1);

        when(roleRepository.findAll(
                ArgumentMatchers.<org.springframework.data.jpa.domain.Specification<Role>>any(),
                eq(pageable)
        )).thenReturn(rolePage);

        when(roleMapper.toResponse(role1))
                .thenReturn(RoleResponse.builder().id("1").build());

        assertThrows(BadRequestException.class, () -> roleService.createRole(request));
        verify(roleRepository, never()).save(any());
        doReturn(java.util.Collections.singletonList(
                new Object[]{"1", 1L}
        )).when(roleRepository).countPermissionsByRoleIds(any());

        doReturn(java.util.Collections.singletonList(
                new Object[]{"1", 2L}
        )).when(roleRepository).countUsersByRoleIds(any());

        PageResponse<RoleResponse> response =
                roleService.getRoles(true, null, pageable);

        assertEquals(1, response.getContent().size());
    }

    // ===================== SEARCH =====================
    @Test
    void createRole_duplicateCode_throwsBadRequest() {
        when(roleRepository.existsByNameIgnoreCase(role.getName())).thenReturn(false);
        Role mockRole = new Role();
        when(roleMapper.createEntity(any(CreateRoleRequest.class))).thenReturn(mockRole);
        when(roleRepository.save(mockRole)).thenThrow(new org.springframework.dao.DataIntegrityViolationException("duplicate"));
    void getRoles_withSearch() {

        Pageable pageable = PageRequest.of(0, 2);

        Page<Role> rolePage = new PageImpl<>(List.of(role2), pageable, 1);

        assertThrows(BadRequestException.class, () -> roleService.createRole(request));
        verify(roleRepository).save(mockRole);
        when(roleRepository.findAll(
                ArgumentMatchers.<org.springframework.data.jpa.domain.Specification<Role>>any(),
                eq(pageable)
        )).thenReturn(rolePage);

        when(roleMapper.toResponse(role2))
                .thenReturn(RoleResponse.builder().id("2").build());

        doReturn(java.util.Collections.singletonList(
                new Object[]{"1", 1L}
        )).when(roleRepository).countPermissionsByRoleIds(any());

        doReturn(java.util.Collections.singletonList(
                new Object[]{"1", 2L}
        )).when(roleRepository).countUsersByRoleIds(any());

        PageResponse<RoleResponse> response =
                roleService.getRoles(null, "user", pageable);

        assertEquals(1, response.getContent().size());
    }

    // ===================== DEFAULT COUNT = 0 =====================
    @Test
    void createRole_firstRole_shouldBeDefault() {
        when(roleRepository.existsByNameIgnoreCase(role.getName())).thenReturn(false);
        when(roleRepository.existsByCode("role_admin_manager")).thenReturn(false);
        when(roleRepository.existsByIsDefaultTrue()).thenReturn(false); // chưa có default
        when(roleRepository.save(any(Role.class))).thenReturn(role);
    void getRoles_missingCount_shouldDefaultZero() {

        Pageable pageable = PageRequest.of(0, 2);

        Page<Role> rolePage = new PageImpl<>(List.of(role1), pageable, 1);

        when(roleRepository.findAll(
                ArgumentMatchers.<org.springframework.data.jpa.domain.Specification<Role>>any(),
                eq(pageable)
        )).thenReturn(rolePage);

        RoleResponse response = roleService.createRole(request);
        RoleResponse res = RoleResponse.builder().id("1").build();

        assertNotNull(response);
        assertEquals("role-123", response.getId());
        // guard → không gọi updateAllIsDefaultToFalse
        verify(roleRepository, never()).updateAllIsDefaultToFalse();
        when(roleMapper.toResponse(role1)).thenReturn(res);

        // empty list → default 0
        when(roleRepository.countPermissionsByRoleIds(any()))
                .thenReturn(List.of());

        when(roleRepository.countUsersByRoleIds(any()))
                .thenReturn(List.of());

        PageResponse<RoleResponse> response =
                roleService.getRoles(null, null, pageable);

        assertEquals(0L, response.getContent().get(0).getPermissionCount());
        assertEquals(0L, response.getContent().get(0).getUserCount());
    }

    // ===================== GET ROLE BY ID TESTS =====================
    // ===================== VERIFY NO N+1 =====================
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
    void getRoles_shouldCallBatchCountOnlyOnce() {

        Pageable pageable = PageRequest.of(0, 2);

        Page<Role> rolePage = new PageImpl<>(List.of(role1, role2), pageable, 2);

        when(roleRepository.findAll(
                ArgumentMatchers.<org.springframework.data.jpa.domain.Specification<Role>>any(),
                eq(pageable)
        )).thenReturn(rolePage);

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
        when(roleMapper.toResponse(any()))
                .thenReturn(RoleResponse.builder().build());

        when(roleRepository.countPermissionsByRoleIds(any()))
                .thenReturn(List.of());

        when(roleRepository.countUsersByRoleIds(any()))
                .thenReturn(List.of());

        roleService.getRoles(null, null, pageable);

        verify(roleRepository, times(1)).countPermissionsByRoleIds(any());
        verify(roleRepository, times(1)).countUsersByRoleIds(any());
    }
}