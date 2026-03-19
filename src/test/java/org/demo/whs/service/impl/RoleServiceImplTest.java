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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
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
        request = new CreateRoleRequest();
        request.setName("Admin Manager");
        request.setDescription("Test role");
        request.setIsDefault(true);

        role = new Role();
        role.setId("1");
        role.setName("Admin Manager");
    }

    // ===================== SUCCESS =====================
    @Test
    void createRole_success() {

        // chỉ mock những gì flow dùng
        when(roleRepository.existsByName("Admin Manager")).thenReturn(false);


        when(roleMapper.createEntity(request)).thenReturn(role);
        when(roleRepository.save(any(Role.class))).thenReturn(role);
        when(roleMapper.toResponse(any(Role.class)))
                .thenReturn(RoleResponse.builder().build());

        RoleResponse response = roleService.createRole(request);

        assertNotNull(response);

        verify(roleRepository).updateAllIsDefaultToFalse();
        verify(roleRepository).save(any(Role.class));
    }

    // ===================== NAME NULL =====================
    @Test
    void createRole_nameNull_throwException() {

        request.setName(null);

        assertThrows(BadRequestException.class,
                () -> roleService.createRole(request));
    }

    // ===================== NAME DUPLICATE =====================
    @Test
    void createRole_duplicateName_throwException() {

        when(roleRepository.existsByName("Admin Manager")).thenReturn(true);

        assertThrows(BadRequestException.class,
                () -> roleService.createRole(request));

        verify(roleRepository, never()).save(any());
    }

    // ===================== CODE DUPLICATE =====================
    @Test
    void createRole_duplicateCode_throwException() {

        // đảm bảo không fail ở validateName
        when(roleRepository.existsByName("Admin Manager")).thenReturn(false);

        // KHÔNG cần mock existsByIsDefaultTrue nếu không dùng
        // hoặc:
        // when(roleRepository.existsByIsDefaultTrue()).thenReturn(true);

        // mock đúng đoạn bị fail
        when(roleRepository.existsByCode("role_admin_manager")).thenReturn(true);

        assertThrows(BadRequestException.class,
                () -> roleService.createRole(request));

        verify(roleRepository, never()).save(any());
    }

    // ===================== DEFAULT LOGIC =====================
    @Test
    void createRole_firstRole_shouldBeDefault() {

        request.setIsDefault(null);

        when(roleRepository.existsByName("Admin Manager")).thenReturn(false);
        when(roleRepository.existsByIsDefaultTrue()).thenReturn(false);
        when(roleRepository.existsByCode("role_admin_manager")).thenReturn(false);

        when(roleMapper.createEntity(request)).thenReturn(role);
        when(roleRepository.save(any(Role.class))).thenReturn(role);
        when(roleMapper.toResponse(any(Role.class)))
                .thenReturn(RoleResponse.builder().build());

        roleService.createRole(request);

        verify(roleRepository).updateAllIsDefaultToFalse();
    }
}