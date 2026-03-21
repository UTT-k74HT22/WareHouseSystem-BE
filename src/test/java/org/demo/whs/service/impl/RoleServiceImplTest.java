package org.demo.whs.service.impl;

import org.demo.whs.entity.Role;
import org.demo.whs.entity.dto.request.Role.CreateRoleRequest;
import org.demo.whs.entity.dto.response.Role.RoleResponse;
import org.demo.whs.exception.BadRequestException;
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

    @BeforeEach
    void setUp() {
        // Builder immutable
        request = CreateRoleRequest.builder()
                .name("Admin Manager")
                .description("Test role")
                .isDefault(true)
                .build();

        // Role không null để tránh NPE
        role = new Role();
        role.setId("1");
        role.setName(request.getName());

        // Stub chung
        lenient().when(roleMapper.createEntity(any(CreateRoleRequest.class))).thenReturn(role);
        lenient().when(roleMapper.toResponse(any(Role.class))).thenReturn(RoleResponse.builder().build());
    }

    // ===================== SUCCESS =====================
    @Test
    void createRole_success() {
        when(roleRepository.existsByNameIgnoreCase("Admin Manager")).thenReturn(false);
        when(roleRepository.existsByCode("role_admin_manager")).thenReturn(false);
        when(roleRepository.existsByIsDefaultTrue()).thenReturn(true);
        when(roleRepository.save(any(Role.class))).thenReturn(role);

        RoleResponse response = roleService.createRole(request);

        assertNotNull(response);
        verify(roleRepository).updateAllIsDefaultToFalse();
        verify(roleRepository).save(any(Role.class));
    }

    // ===================== NAME NULL =====================
    @Test
    void createRole_nameNull_throwException() {
        request = CreateRoleRequest.builder()
                .name(null)
                .description("Test role")
                .isDefault(true)
                .build();

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
    void createRole_firstRole_shouldBeDefault() {
        request = CreateRoleRequest.builder()
                .name("Admin Manager")
                .description("Test role")
                .isDefault(null)
                .build();

        when(roleRepository.existsByNameIgnoreCase("Admin Manager")).thenReturn(false);
        when(roleRepository.existsByCode("role_admin_manager")).thenReturn(false);
        when(roleRepository.save(any(Role.class))).thenReturn(role);

        when(roleRepository.existsByIsDefaultTrue()).thenReturn(false); // chưa có default role

        roleService.createRole(request);

        // guard → không gọi updateAllIsDefaultToFalse
        verify(roleRepository, never()).updateAllIsDefaultToFalse();
    }
}