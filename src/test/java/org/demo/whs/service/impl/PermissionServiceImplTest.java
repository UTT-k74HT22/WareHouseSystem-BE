package org.demo.whs.service.impl;

import org.demo.whs.entity.Permission;
import org.demo.whs.entity.dto.request.Permission.CreatePermissionRequest;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.dto.response.Permission.PermissionResponse;
import org.demo.whs.entity.enums.ActionType;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.ConflictException;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.mapper.PermissionMapper;
import org.demo.whs.repository.PermissionRepository;
import org.demo.whs.repository.RolePermissionRepository;
import org.demo.whs.service.PermissionCacheService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PermissionServiceImpl Unit Tests")
class PermissionServiceImplTest {

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private RolePermissionRepository rolePermissionRepository;

    @Mock
    private PermissionMapper permissionMapper;

    @Mock
    private PermissionCacheService permissionCacheService;

    @InjectMocks
    private PermissionServiceImpl permissionService;

    // ================= CREATE =================

    @Test
    @DisplayName("createPermission_shouldSucceed_WhenValid")
    void createPermission_shouldSucceed_WhenValid() {

        CreatePermissionRequest request = new CreatePermissionRequest();
        request.setName("Inventory Read");
        request.setResource("INV");
        request.setAction(ActionType.READ);
        request.setDescription("Read inventory");

        Permission entity = new Permission();

        Permission saved = Permission.builder()
                .code("PERM_INV_READ")
                .name(request.getName())
                .resource(request.getResource())
                .action(request.getAction())
                .description(request.getDescription())
                .build();

        PermissionResponse response = PermissionResponse.builder()
                .code("PERM_INV_READ")
                .name("Inventory Read")
                .resource("INV")
                .action(ActionType.READ)
                .description("Read inventory")
                .build();

        when(permissionRepository.existsByResourceAndAction("INV", ActionType.READ))
                .thenReturn(false);
        when(permissionMapper.createEntity(request)).thenReturn(entity);
        when(permissionRepository.save(any())).thenReturn(saved);
        when(permissionMapper.toResponse(saved)).thenReturn(response);

        PermissionResponse result = permissionService.createPermission(request);

        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo("PERM_INV_READ");

        verify(permissionRepository).save(any());
    }

    @Test
    @DisplayName("createPermission_shouldThrow_WhenDuplicate")
    void createPermission_shouldThrow_WhenDuplicate() {

        CreatePermissionRequest request = new CreatePermissionRequest();
        request.setResource("INV");
        request.setAction(ActionType.READ);

        when(permissionRepository.existsByResourceAndAction("INV", ActionType.READ))
                .thenReturn(true);

        assertThatThrownBy(() -> permissionService.createPermission(request))
                .isInstanceOf(BadRequestException.class);

        verify(permissionRepository, never()).save(any());
    }

    @Test
    @DisplayName("createPermission_shouldGenerateCorrectCode")
    void createPermission_shouldGenerateCorrectCode() {

        CreatePermissionRequest request = new CreatePermissionRequest();
        request.setName("Order Write");
        request.setResource("ORDER");
        request.setAction(ActionType.WRITE);

        Permission entity = new Permission();

        when(permissionRepository.existsByResourceAndAction("ORDER", ActionType.WRITE))
                .thenReturn(false);
        when(permissionMapper.createEntity(request)).thenReturn(entity);
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

        PermissionResponse result = permissionService.createPermission(request);

        assertThat(result.getCode()).isEqualTo("PERM_ORDER_WRITE");
    }

    @Test
    @DisplayName("createPermission_shouldNormalizeResourceBeforePersisting")
    void createPermission_shouldNormalizeResourceBeforePersisting() {

        CreatePermissionRequest request = new CreatePermissionRequest();
        request.setName("Order Create");
        request.setResource(" order ");
        request.setAction(ActionType.CREATE);

        Permission entity = new Permission();

        when(permissionRepository.existsByResourceAndAction("ORDER", ActionType.CREATE))
                .thenReturn(false);
        when(permissionMapper.createEntity(request)).thenReturn(entity);
        when(permissionRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(permissionMapper.toResponse(any()))
                .thenAnswer(invocation -> {
                    Permission permission = invocation.getArgument(0);
                    return PermissionResponse.builder()
                            .code(permission.getCode())
                            .resource(permission.getResource())
                            .action(permission.getAction())
                            .build();
                });

        PermissionResponse result = permissionService.createPermission(request);

        assertThat(result.getCode()).isEqualTo("PERM_ORDER_CREATE");
        assertThat(result.getResource()).isEqualTo("ORDER");
        verify(permissionRepository).existsByResourceAndAction("ORDER", ActionType.CREATE);
    }

    // ================= GET =================

    @Test
    @DisplayName("getPermissions_shouldReturnAll")
    void getPermissions_shouldReturnAll() {

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

        PageResponse<PermissionResponse> result =
                permissionService.getPermissions(null, null, null, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("getPermissions_shouldFilterByResource")
    void getPermissions_shouldFilterByResource() {

        Pageable pageable = PageRequest.of(0, 10);

        Permission permission = new Permission();
        PermissionResponse response = new PermissionResponse();

        Page<Permission> page = new PageImpl<>(List.of(permission));

        when(permissionRepository.findAll(
                Mockito.<org.springframework.data.jpa.domain.Specification<Permission>>any(),
                eq(pageable)
        )).thenReturn(page);
        when(permissionMapper.toResponse(permission)).thenReturn(response);

        PageResponse<PermissionResponse> result =
                permissionService.getPermissions("INV", null, null, pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("getPermissions_shouldFilterByAction")
    void getPermissions_shouldFilterByAction() {

        Pageable pageable = PageRequest.of(0, 10);

        Permission permission = new Permission();
        PermissionResponse response = new PermissionResponse();

        Page<Permission> page = new PageImpl<>(List.of(permission));

        when(permissionRepository.findAll(
                Mockito.<org.springframework.data.jpa.domain.Specification<Permission>>any(),
                eq(pageable)
        )).thenReturn(page);
        when(permissionMapper.toResponse(permission)).thenReturn(response);

        PageResponse<PermissionResponse> result =
                permissionService.getPermissions(null, ActionType.READ, null, pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("getPermissions_shouldSearchByKeyword")
    void getPermissions_shouldSearchByKeyword() {

        Pageable pageable = PageRequest.of(0, 10);

        Permission permission = new Permission();
        PermissionResponse response = new PermissionResponse();

        Page<Permission> page = new PageImpl<>(List.of(permission));

        when(permissionRepository.findAll(
                Mockito.<org.springframework.data.jpa.domain.Specification<Permission>>any(),
                eq(pageable)
        )).thenReturn(page);
        when(permissionMapper.toResponse(permission)).thenReturn(response);

        PageResponse<PermissionResponse> result =
                permissionService.getPermissions(null, null, "inventory", pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("getPermissionById_shouldReturnPermission_WhenFound")
    void getPermissionById_shouldReturnPermission_WhenFound() {

        String id = "perm-id-1";

        Permission permission = new Permission();
        permission.setId(id);
        permission.setName("Inventory Read");

        PermissionResponse response = new PermissionResponse();
        response.setId(id);
        response.setName("Inventory Read");

        when(permissionRepository.findById(id))
                .thenReturn(java.util.Optional.of(permission));

        when(permissionMapper.toResponse(permission))
                .thenReturn(response);

        PermissionResponse result = permissionService.getPermissionById(id);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getName()).isEqualTo("Inventory Read");

        verify(permissionRepository).findById(id);
        verify(permissionMapper).toResponse(permission);
    }

    @Test
    @DisplayName("getPermissionById_shouldThrowNotFound_WhenNotExist")
    void getPermissionById_shouldThrowNotFound_WhenNotExist() {

        String id = "not-found-id";

        when(permissionRepository.findById(id))
                .thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> permissionService.getPermissionById(id))
                .isInstanceOf(org.demo.whs.exception.NotFoundException.class);

        verify(permissionRepository).findById(id);
        verify(permissionMapper, never()).toResponse(any());
    }

    @Test
    @DisplayName("deletePermission_shouldSucceed_WhenNotInUse")
    void deletePermission_shouldSucceed_WhenNotInUse() {

        String id = "perm-id-1";

        Permission permission = new Permission();
        permission.setId(id);

        when(permissionRepository.findById(id))
                .thenReturn(java.util.Optional.of(permission));

        when(rolePermissionRepository.findRoleIdsByPermissionId(id))
                .thenReturn(List.of());

        doNothing().when(permissionRepository).delete(permission);

        permissionService.deletePermission(id);

        verify(permissionRepository).findById(id);
        verify(rolePermissionRepository).findRoleIdsByPermissionId(id);
        verify(permissionRepository).delete(permission);
    }

    @Test
    @DisplayName("deletePermission_shouldThrowNotFound_WhenNotExist")
    void deletePermission_shouldThrowNotFound_WhenNotExist() {

        String id = "not-found-id";

        when(permissionRepository.findById(id))
                .thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> permissionService.deletePermission(id))
                .isInstanceOf(org.demo.whs.exception.NotFoundException.class);

        verify(permissionRepository).findById(id);
        verify(rolePermissionRepository, never()).findRoleIdsByPermissionId(any());
        verify(permissionRepository, never()).delete(any(Permission.class));
    }

    @Test
    @DisplayName("deletePermission_shouldThrowConflict_WhenInUse")
    void deletePermission_shouldThrowConflict_WhenInUse() {

        String id = "perm-id-1";

        Permission permission = new Permission();
        permission.setId(id);

        when(permissionRepository.findById(id))
                .thenReturn(java.util.Optional.of(permission));

        when(rolePermissionRepository.findRoleIdsByPermissionId(id))
                .thenReturn(List.of("ROLE_ADMIN", "ROLE_USER"));

        assertThatThrownBy(() -> permissionService.deletePermission(id))
                .isInstanceOf(org.demo.whs.exception.ConflictException.class);

        verify(permissionRepository).findById(id);
        verify(rolePermissionRepository).findRoleIdsByPermissionId(id);
        verify(permissionRepository, never()).delete(any(Permission.class));
    }

    private void validatePermissionNotInUse(String permissionId) {
        List<String> roleIds = rolePermissionRepository.findRoleIdsByPermissionId(permissionId);

        if (!roleIds.isEmpty()) {
            throw new ConflictException(ErrorCode.PERM_007);
        }
    }
}
