package org.demo.whs.service.impl;

import org.demo.whs.entity.Permission;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.dto.response.Permission.PermissionResponse;
import org.demo.whs.entity.enums.ActionType;
import org.demo.whs.mapper.PermissionMapper;
import org.demo.whs.repository.PermissionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermissionServiceImplTest {

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private PermissionMapper permissionMapper;

    @InjectMocks
    private PermissionServiceImpl permissionService;

    @Test
    void getPermissions_shouldReturnAllPermissions_whenNoFilter() {

        // given
        Pageable pageable = PageRequest.of(0, 10);

        Permission permission = new Permission();
        permission.setName("inventory.read");

        PermissionResponse response = new PermissionResponse();
        response.setName("inventory.read");

        Page<Permission> page = new PageImpl<>(List.of(permission));

        when(permissionRepository.findAll(
                Mockito.<org.springframework.data.jpa.domain.Specification<Permission>>any(),
                eq(pageable)
        )).thenReturn(page);

        when(permissionMapper.toResponse(permission)).thenReturn(response);

        // when
        PageResponse<PermissionResponse> result =
                permissionService.getPermissions(null, null, null, pageable);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("inventory.read");

        verify(permissionRepository).findAll(
                ArgumentMatchers.<Specification<Permission>>any(),
                eq(pageable)
        );
        verify(permissionMapper).toResponse(permission);
    }

    @Test
    void getPermissions_shouldFilterByResource() {

        // given
        Pageable pageable = PageRequest.of(0, 10);

        Permission permission = new Permission();
        PermissionResponse response = new PermissionResponse();

        Page<Permission> page = new PageImpl<>(List.of(permission));

        when(permissionRepository.findAll(
                Mockito.<org.springframework.data.jpa.domain.Specification<Permission>>any(),
                eq(pageable)
        )).thenReturn(page);

        when(permissionMapper.toResponse(permission)).thenReturn(response);

        // when
        PageResponse<PermissionResponse> result =
                permissionService.getPermissions("INVENTORY", null, null, pageable);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);

        verify(permissionRepository).findAll(
                ArgumentMatchers.<Specification<Permission>>any(),
                eq(pageable)
        );
    }

    @Test
    void getPermissions_shouldFilterByAction() {

        // given
        Pageable pageable = PageRequest.of(0, 10);

        Permission permission = new Permission();
        PermissionResponse response = new PermissionResponse();

        Page<Permission> page = new PageImpl<>(List.of(permission));

        when(permissionRepository.findAll(
                Mockito.<org.springframework.data.jpa.domain.Specification<Permission>>any(),
                eq(pageable)
        )).thenReturn(page);

        when(permissionMapper.toResponse(permission)).thenReturn(response);

        // when
        PageResponse<PermissionResponse> result =
                permissionService.getPermissions(null, ActionType.READ, null, pageable);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);

        verify(permissionRepository).findAll(
                ArgumentMatchers.<Specification<Permission>>any(),
                eq(pageable)
        );
    }

    @Test
    void getPermissions_shouldSearchByKeyword() {

        // given
        Pageable pageable = PageRequest.of(0, 10);

        Permission permission = new Permission();
        PermissionResponse response = new PermissionResponse();

        Page<Permission> page = new PageImpl<>(List.of(permission));

        when(permissionRepository.findAll(
                Mockito.<org.springframework.data.jpa.domain.Specification<Permission>>any(),
                eq(pageable)
        )).thenReturn(page);

        when(permissionMapper.toResponse(permission)).thenReturn(response);

        // when
        PageResponse<PermissionResponse> result =
                permissionService.getPermissions(null, null, "inventory", pageable);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);

        verify(permissionRepository).findAll(
                ArgumentMatchers.<Specification<Permission>>any(),
                eq(pageable)
        );
    }
}