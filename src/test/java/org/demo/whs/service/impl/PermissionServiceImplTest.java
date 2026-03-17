package org.demo.whs.service.impl;

import org.demo.whs.entity.Permission;
import org.demo.whs.entity.dto.request.Permission.CreatePermissionRequest;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.dto.response.Permission.PermissionResponse;
import org.demo.whs.entity.enums.ActionType;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.ConflictException;
import org.demo.whs.mapper.PermissionMapper;
import org.demo.whs.repository.PermissionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.Optional;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PermissionServiceImpl Unit Tests")
class PermissionServiceImplTest {

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private PermissionMapper permissionMapper;

    @InjectMocks
    private PermissionServiceImpl permissionService;

    // --- CREATE PERMISSION TESTS ---

    @Test
    @DisplayName("createPermission_shouldSucceed_WhenPermissionValid")
    void createPermission_shouldSucceed_WhenPermissionValid() {

        // Arrange
        CreatePermissionRequest request = new CreatePermissionRequest();
        request.setName("Inventory Read");
        request.setResource("INV");
        request.setAction(ActionType.READ);
        request.setDescription("Read inventory");
    void getPermissions_shouldReturnAllPermissions_whenNoFilter() {

        Permission permission = Permission.builder()
                .name(request.getName())
                .resource(request.getResource())
                .action(request.getAction())
                .description(request.getDescription())
                .build();
        // given
        Pageable pageable = PageRequest.of(0, 10);

        Permission savedPermission = Permission.builder()
                .code("PERM_INV_READ")
                .name(request.getName())
                .resource(request.getResource())
                .action(request.getAction())
                .description(request.getDescription())
                .build();
        Permission permission = new Permission();
        permission.setName("inventory.read");

        PermissionResponse response = PermissionResponse.builder()
                .id("perm-1")
                .code("PERM_INV_READ")
                .name("Inventory Read")
                .resource("INV")
                .action(ActionType.READ)
                .description("Read inventory")
                .build();
        PermissionResponse response = new PermissionResponse();
        response.setName("inventory.read");

        when(permissionRepository.existsByResourceAndAction("INV", ActionType.READ))
                .thenReturn(false);
        Page<Permission> page = new PageImpl<>(List.of(permission));

        when(permissionMapper.createEntity(request))
                .thenReturn(permission);
        when(permissionRepository.findAll(
                Mockito.<org.springframework.data.jpa.domain.Specification<Permission>>any(),
                eq(pageable)
        )).thenReturn(page);

        when(permissionRepository.save(any(Permission.class)))
                .thenReturn(savedPermission);
        when(permissionMapper.toResponse(permission)).thenReturn(response);

        when(permissionMapper.toResponse(savedPermission))
                .thenReturn(response);
        // when
        PageResponse<PermissionResponse> result =
                permissionService.getPermissions(null, null, null, pageable);

        // Act
        PermissionResponse result = permissionService.createPermission(request);

        // Assert
        // then
        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo("PERM_INV_READ");
        assertThat(result.getName()).isEqualTo("Inventory Read");
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("inventory.read");

        verify(permissionRepository).save(any(Permission.class));
        verify(permissionRepository).findAll(
                ArgumentMatchers.<Specification<Permission>>any(),
                eq(pageable)
        );
        verify(permissionMapper).toResponse(permission);
    }

    @Test
    @DisplayName("createPermission_shouldThrowConflict_WhenPermissionAlreadyExists")
    void createPermission_shouldThrowConflict_WhenPermissionAlreadyExists() {
    void getPermissions_shouldFilterByResource() {

        // given
        Pageable pageable = PageRequest.of(0, 10);

        Permission permission = new Permission();
        PermissionResponse response = new PermissionResponse();

        Page<Permission> page = new PageImpl<>(List.of(permission));

        // Arrange
        CreatePermissionRequest request = new CreatePermissionRequest();
        request.setName("Inventory Read");
        request.setResource("INV");
        request.setAction(ActionType.READ);
        when(permissionRepository.findAll(
                Mockito.<org.springframework.data.jpa.domain.Specification<Permission>>any(),
                eq(pageable)
        )).thenReturn(page);

        when(permissionRepository.existsByResourceAndAction("INV", ActionType.READ))
                .thenReturn(true);
        when(permissionMapper.toResponse(permission)).thenReturn(response);

        // Act & Assert
        assertThatThrownBy(() -> permissionService.createPermission(request))
                .isInstanceOf(BadRequestException.class);
        // when
        PageResponse<PermissionResponse> result =
                permissionService.getPermissions("INVENTORY", null, null, pageable);

        verify(permissionRepository, never()).save(any());
        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);

        verify(permissionRepository).findAll(
                ArgumentMatchers.<Specification<Permission>>any(),
                eq(pageable)
        );
    }

    @Test
    @DisplayName("createPermission_shouldGenerateCorrectCode")
    void createPermission_shouldGenerateCorrectCode() {
    void getPermissions_shouldFilterByAction() {

        // Arrange
        CreatePermissionRequest request = new CreatePermissionRequest();
        request.setName("Order Write");
        request.setResource("ORDER");
        request.setAction(ActionType.WRITE);
        // given
        Pageable pageable = PageRequest.of(0, 10);

        Permission permission = Permission.builder()
                .name(request.getName())
                .resource(request.getResource())
                .action(request.getAction())
                .build();
        Permission permission = new Permission();
        PermissionResponse response = new PermissionResponse();

        when(permissionRepository.existsByResourceAndAction("ORDER", ActionType.WRITE))
                .thenReturn(false);
        Page<Permission> page = new PageImpl<>(List.of(permission));

        when(permissionMapper.createEntity(request))
                .thenReturn(permission);
        when(permissionRepository.findAll(
                Mockito.<org.springframework.data.jpa.domain.Specification<Permission>>any(),
                eq(pageable)
        )).thenReturn(page);

        when(permissionRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(permissionMapper.toResponse(permission)).thenReturn(response);

        when(permissionMapper.toResponse(any()))
                .thenAnswer(invocation -> {
                    Permission p = invocation.getArgument(0);
                    return PermissionResponse.builder()
                            .code(p.getCode())
                            .name(p.getName())
                            .resource(p.getResource())
                            .action(p.getAction())
                            .build();
                });
        // when
        PageResponse<PermissionResponse> result =
                permissionService.getPermissions(null, ActionType.READ, null, pageable);

        // Act
        PermissionResponse response = permissionService.createPermission(request);
        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);

        // Assert
        assertThat(response.getCode()).isEqualTo("PERM_ORDER_WRITE");
        verify(permissionRepository).findAll(
                ArgumentMatchers.<Specification<Permission>>any(),
                eq(pageable)
        );
    }

    @Test
    @DisplayName("createPermission_shouldHandleNullRequest")
    void createPermission_shouldHandleNullRequest() {
    void getPermissions_shouldSearchByKeyword() {

        // given
        Pageable pageable = PageRequest.of(0, 10);

        Permission permission = new Permission();
        PermissionResponse response = new PermissionResponse();

        Page<Permission> page = new PageImpl<>(List.of(permission));

        // Act & Assert
        assertThatThrownBy(() -> permissionService.createPermission(null))
                .isInstanceOf(NullPointerException.class);
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