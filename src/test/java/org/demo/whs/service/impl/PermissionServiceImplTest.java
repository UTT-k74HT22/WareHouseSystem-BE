package org.demo.whs.service.impl;

import org.demo.whs.entity.Permission;
import org.demo.whs.entity.dto.request.Permission.CreatePermissionRequest;
import org.demo.whs.entity.dto.response.Permission.PermissionResponse;
import org.demo.whs.entity.enums.ActionType;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.ConflictException;
import org.demo.whs.mapper.PermissionMapper;
import org.demo.whs.repository.PermissionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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

        Permission permission = Permission.builder()
                .name(request.getName())
                .resource(request.getResource())
                .action(request.getAction())
                .description(request.getDescription())
                .build();

        Permission savedPermission = Permission.builder()
                .code("PERM_INV_READ")
                .name(request.getName())
                .resource(request.getResource())
                .action(request.getAction())
                .description(request.getDescription())
                .build();

        PermissionResponse response = PermissionResponse.builder()
                .id("perm-1")
                .code("PERM_INV_READ")
                .name("Inventory Read")
                .resource("INV")
                .action(ActionType.READ)
                .description("Read inventory")
                .build();

        when(permissionRepository.existsByResourceAndAction("INV", ActionType.READ))
                .thenReturn(false);

        when(permissionMapper.createEntity(request))
                .thenReturn(permission);

        when(permissionRepository.save(any(Permission.class)))
                .thenReturn(savedPermission);

        when(permissionMapper.toResponse(savedPermission))
                .thenReturn(response);

        // Act
        PermissionResponse result = permissionService.createPermission(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo("PERM_INV_READ");
        assertThat(result.getName()).isEqualTo("Inventory Read");

        verify(permissionRepository).save(any(Permission.class));
    }

    @Test
    @DisplayName("createPermission_shouldThrowConflict_WhenPermissionAlreadyExists")
    void createPermission_shouldThrowConflict_WhenPermissionAlreadyExists() {

        // Arrange
        CreatePermissionRequest request = new CreatePermissionRequest();
        request.setName("Inventory Read");
        request.setResource("INV");
        request.setAction(ActionType.READ);

        when(permissionRepository.existsByResourceAndAction("INV", ActionType.READ))
                .thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> permissionService.createPermission(request))
                .isInstanceOf(BadRequestException.class);

        verify(permissionRepository, never()).save(any());
    }

    @Test
    @DisplayName("createPermission_shouldGenerateCorrectCode")
    void createPermission_shouldGenerateCorrectCode() {

        // Arrange
        CreatePermissionRequest request = new CreatePermissionRequest();
        request.setName("Order Write");
        request.setResource("ORDER");
        request.setAction(ActionType.WRITE);

        Permission permission = Permission.builder()
                .name(request.getName())
                .resource(request.getResource())
                .action(request.getAction())
                .build();

        when(permissionRepository.existsByResourceAndAction("ORDER", ActionType.WRITE))
                .thenReturn(false);

        when(permissionMapper.createEntity(request))
                .thenReturn(permission);

        when(permissionRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

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

        // Act
        PermissionResponse response = permissionService.createPermission(request);

        // Assert
        assertThat(response.getCode()).isEqualTo("PERM_ORDER_WRITE");
    }

    @Test
    @DisplayName("createPermission_shouldHandleNullRequest")
    void createPermission_shouldHandleNullRequest() {

        // Act & Assert
        assertThatThrownBy(() -> permissionService.createPermission(null))
                .isInstanceOf(NullPointerException.class);
    }

}