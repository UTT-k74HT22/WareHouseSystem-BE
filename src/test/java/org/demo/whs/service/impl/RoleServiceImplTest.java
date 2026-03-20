package org.demo.whs.service.impl;

import org.demo.whs.entity.Role;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.dto.response.Role.RoleResponse;
import org.demo.whs.mapper.RoleMapper;
import org.demo.whs.repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleServiceImplTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private RoleMapper roleMapper;

    @InjectMocks
    private RoleServiceImpl roleService;

    private Role role1;
    private Role role2;

    @BeforeEach
    void setUp() {
        role1 = new Role();
        role1.setId("1");
        role1.setName(null);

        role2 = new Role();
        role2.setId("2");
        role2.setName(null);
    }

    // ===================== GET ROLES SUCCESS =====================
    @Test
    void getRoles_success() {

        Pageable pageable = PageRequest.of(0, 2);

        Page<Role> rolePage = new PageImpl<>(List.of(role1, role2), pageable, 2);

        // FIX ambiguous findAll
        when(roleRepository.findAll(
                ArgumentMatchers.<org.springframework.data.jpa.domain.Specification<Role>>any(),
                eq(pageable)
        )).thenReturn(rolePage);

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
        assertEquals(2, response.getContent().size());
    }

    // ===================== EMPTY =====================
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

        assertNotNull(response);
        assertTrue(response.getContent().isEmpty());

        verify(roleRepository, never()).countPermissionsByRoleIds(any());
        verify(roleRepository, never()).countUsersByRoleIds(any());
    }

    // ===================== FILTER =====================
    @Test
    void getRoles_withFilter() {

        Pageable pageable = PageRequest.of(0, 2);

        Page<Role> rolePage = new PageImpl<>(List.of(role1), pageable, 1);

        when(roleRepository.findAll(
                ArgumentMatchers.<org.springframework.data.jpa.domain.Specification<Role>>any(),
                eq(pageable)
        )).thenReturn(rolePage);

        when(roleMapper.toResponse(role1))
                .thenReturn(RoleResponse.builder().id("1").build());

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
    void getRoles_withSearch() {

        Pageable pageable = PageRequest.of(0, 2);

        Page<Role> rolePage = new PageImpl<>(List.of(role2), pageable, 1);

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
    void getRoles_missingCount_shouldDefaultZero() {

        Pageable pageable = PageRequest.of(0, 2);

        Page<Role> rolePage = new PageImpl<>(List.of(role1), pageable, 1);

        when(roleRepository.findAll(
                ArgumentMatchers.<org.springframework.data.jpa.domain.Specification<Role>>any(),
                eq(pageable)
        )).thenReturn(rolePage);

        RoleResponse res = RoleResponse.builder().id("1").build();

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

    // ===================== VERIFY NO N+1 =====================
    @Test
    void getRoles_shouldCallBatchCountOnlyOnce() {

        Pageable pageable = PageRequest.of(0, 2);

        Page<Role> rolePage = new PageImpl<>(List.of(role1, role2), pageable, 2);

        when(roleRepository.findAll(
                ArgumentMatchers.<org.springframework.data.jpa.domain.Specification<Role>>any(),
                eq(pageable)
        )).thenReturn(rolePage);

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