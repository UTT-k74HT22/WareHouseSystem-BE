package org.demo.whs.service.impl;

import org.demo.whs.entity.AccountHasRole;
import org.demo.whs.entity.Account;
import org.demo.whs.entity.dto.response.User.AccountResponse;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.AccountRoleId;
import org.demo.whs.entity.Role;
import org.demo.whs.entity.dto.request.UserRole.AssignRolesRequest;
import org.demo.whs.entity.dto.response.Role.RoleResponse;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.NotFoundException;
import org.demo.whs.mapper.AccountMapper;
import org.demo.whs.mapper.RoleMapper;
import org.demo.whs.mapper.UserRoleMapper;
import org.demo.whs.repository.AccountHasRoleRepository;
import org.demo.whs.repository.AccountRepository;
import org.demo.whs.repository.RoleRepository;
import org.demo.whs.service.PermissionCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserRoleServiceImplTest {
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private AccountHasRoleRepository accountHasRoleRepository;
    @Mock
    private UserRoleMapper userRoleMapper;
    @Mock
    private RoleMapper roleMapper;
    @Mock
    private AccountMapper accountMapper;
    @Mock
    private PermissionCacheService permissionCacheService;

    @InjectMocks
    private UserRoleServiceImpl service;

    private String userId;
    private String roleId1;
    private String roleId2;

    @BeforeEach
    void setUp() {
        userId = "user-1";
        roleId1 = "role-1";
        roleId2 = "role-2";
    }

    // ================= ASSIGN ROLES =================

    @Test
    void assignRolesToUser_success() {

        AssignRolesRequest request = new AssignRolesRequest();
        request.setRoleIds(List.of(roleId1, roleId2));

        Role role1 = new Role();
        role1.setId(roleId1);
        Role role2 = new Role();
        role2.setId(roleId2);

        AccountHasRole entity1 = new AccountHasRole();
        entity1.setId(new AccountRoleId(userId, roleId1));
        AccountHasRole entity2 = new AccountHasRole();
        entity2.setId(new AccountRoleId(userId, roleId2));

        RoleResponse res1 = mock(RoleResponse.class);
        RoleResponse res2 = mock(RoleResponse.class);

        when(accountRepository.existsById(userId)).thenReturn(true);
        when(roleRepository.findAllById(any())).thenReturn(List.of(role1, role2));
        when(accountHasRoleRepository.findByIdAccountId(userId)).thenReturn(new ArrayList<>());

        when(userRoleMapper.createEntity(userId, roleId1)).thenReturn(entity1);
        when(userRoleMapper.createEntity(userId, roleId2)).thenReturn(entity2);

        when(roleMapper.toResponse(role1)).thenReturn(res1);
        when(roleMapper.toResponse(role2)).thenReturn(res2);

        List<RoleResponse> result = service.assignRolesToUser(userId, request);

        assertEquals(2, result.size());
        verify(accountHasRoleRepository).saveAll(any());
        verify(permissionCacheService).evictPermissions(userId);
    }

    @Test
    void assignRolesToUser_requestNull_throwException() {
        assertThrows(BadRequestException.class,
                () -> service.assignRolesToUser(userId, null));
    }

    @Test
    void assignRolesToUser_emptyRoleIds_throwException() {

        AssignRolesRequest request = new AssignRolesRequest();
        request.setRoleIds(Collections.emptyList());

        assertThrows(BadRequestException.class,
                () -> service.assignRolesToUser(userId, request));
    }

    @Test
    void assignRolesToUser_userNotFound_throwException() {

        AssignRolesRequest request = new AssignRolesRequest();
        request.setRoleIds(List.of(roleId1));

        when(accountRepository.existsById(userId)).thenReturn(false);

        assertThrows(NotFoundException.class,
                () -> service.assignRolesToUser(userId, request));
    }

    @Test
    void assignRolesToUser_roleNotFound_throwException() {

        AssignRolesRequest request = new AssignRolesRequest();
        request.setRoleIds(List.of(roleId1));

        when(accountRepository.existsById(userId)).thenReturn(true);
        when(roleRepository.findAllById(any())).thenReturn(Collections.emptyList());

        assertThrows(NotFoundException.class,
                () -> service.assignRolesToUser(userId, request));
    }

    @Test
    void assignRolesToUser_duplicateRole_skipInsert() {

        AssignRolesRequest request = new AssignRolesRequest();
        request.setRoleIds(List.of(roleId1));

        Role role1 = new Role();
        role1.setId(roleId1);

        AccountHasRole existing = new AccountHasRole();
        existing.setId(new AccountRoleId(userId, roleId1));

        when(accountRepository.existsById(userId)).thenReturn(true);
        when(roleRepository.findAllById(any())).thenReturn(List.of(role1));
        when(accountHasRoleRepository.findByIdAccountId(userId))
                .thenReturn(new ArrayList<>(List.of(existing)));

        service.assignRolesToUser(userId, request);

        verify(accountHasRoleRepository, never()).saveAll(any());
        verify(permissionCacheService, never()).evictPermissions(any());
    }

    // ================= REMOVE ROLE =================

    @Test
    void removeRoleFromUser_success() {

        when(accountHasRoleRepository.existsByIdAccountIdAndIdRoleId(userId, roleId1))
                .thenReturn(true);

        when(roleRepository.findById(roleId1))
                .thenReturn(Optional.of(new Role()));

        when(accountHasRoleRepository.countByIdAccountId(userId))
                .thenReturn(2L)   // trước delete
                .thenReturn(1L);  // sau delete

        assertDoesNotThrow(() -> service.removeRoleFromUser(userId, roleId1));

        verify(accountHasRoleRepository)
                .deleteByIdAccountIdAndIdRoleId(userId, roleId1);
        verify(permissionCacheService).evictPermissions(userId);
    }

    @Test
    void removeRoleFromUser_mappingNotExists_throwException() {

        when(accountHasRoleRepository.existsByIdAccountIdAndIdRoleId(userId, roleId1))
                .thenReturn(false);

        assertThrows(BadRequestException.class,
                () -> service.removeRoleFromUser(userId, roleId1));

        verify(accountHasRoleRepository, never()).deleteByIdAccountIdAndIdRoleId(any(), any());
    }

    @Test
    void removeRoleFromUser_roleNotFound_throwException() {

        when(accountHasRoleRepository.existsByIdAccountIdAndIdRoleId(userId, roleId1))
                .thenReturn(true);

        when(roleRepository.findById(roleId1))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> service.removeRoleFromUser(userId, roleId1));

        verify(accountHasRoleRepository, never()).deleteByIdAccountIdAndIdRoleId(any(), any());
    }

    @Test
    void removeRoleFromUser_lastRole_throwException() {

        when(accountHasRoleRepository.existsByIdAccountIdAndIdRoleId(userId, roleId1))
                .thenReturn(true);

        when(roleRepository.findById(roleId1))
                .thenReturn(Optional.of(new Role()));

        when(accountHasRoleRepository.countByIdAccountId(userId))
                .thenReturn(1L);

        assertThrows(BadRequestException.class,
                () -> service.removeRoleFromUser(userId, roleId1));

        verify(accountHasRoleRepository, never()).deleteByIdAccountIdAndIdRoleId(any(), any());
    }

    @Test
    void removeRoleFromUser_raceCondition_throwIllegalState() {

        when(accountHasRoleRepository.existsByIdAccountIdAndIdRoleId(userId, roleId1))
                .thenReturn(true);

        when(roleRepository.findById(roleId1))
                .thenReturn(Optional.of(new Role()));

        when(accountHasRoleRepository.countByIdAccountId(userId))
                .thenReturn(2L)   // trước delete
                .thenReturn(0L);  // sau delete (race condition)

        assertThrows(IllegalStateException.class,
                () -> service.removeRoleFromUser(userId, roleId1));
    }

    // ================= GET USER ROLES =================

    @Test
    void getUserRoles_success() {

        Role role1 = new Role();
        role1.setId(roleId1);
        Role role2 = new Role();
        role2.setId(roleId2);

        Page<Role> page = new PageImpl<>(List.of(role1, role2), PageRequest.of(0, 10), 2);

        RoleResponse res1 = mock(RoleResponse.class);
        RoleResponse res2 = mock(RoleResponse.class);

        when(accountHasRoleRepository.countByIdAccountId(userId)).thenReturn(2L);
        when(roleRepository.findRolesByUserId(eq(userId), any(Pageable.class))).thenReturn(page);
        when(roleMapper.toResponse(role1)).thenReturn(res1);
        when(roleMapper.toResponse(role2)).thenReturn(res2);

        PageResponse<RoleResponse> response = service.getUserRoles(userId, PageRequest.of(0, 10));

        assertNotNull(response);
        assertEquals(2, response.getContent().size());
        assertTrue(response.getContent().contains(res1));
        assertTrue(response.getContent().contains(res2));

        verify(accountHasRoleRepository).countByIdAccountId(userId);
        verify(roleRepository).findRolesByUserId(eq(userId), any(Pageable.class));
    }

    @Test
    void getUserRoles_userNotFound_throwException() {

        when(accountHasRoleRepository.countByIdAccountId(userId)).thenReturn(0L);

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> service.getUserRoles(userId, PageRequest.of(0, 10)));

        assertEquals("USER_ROLE_001", ex.getErrorCode());
        verify(accountHasRoleRepository).countByIdAccountId(userId);
        verifyNoInteractions(roleRepository);
    }

    @Test
    void getUserRoles_noRolesInPage_throwException() {

        when(accountHasRoleRepository.countByIdAccountId(userId)).thenReturn(1L);

        Page<Role> emptyPage = Page.empty();
        when(roleRepository.findRolesByUserId(eq(userId), any(Pageable.class))).thenReturn(emptyPage);

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> service.getUserRoles(userId, PageRequest.of(0, 10)));

        assertEquals("USER_ROLE_002", ex.getErrorCode());
        verify(accountHasRoleRepository).countByIdAccountId(userId);
        verify(roleRepository).findRolesByUserId(eq(userId), any(Pageable.class));
    }

    @Test
    void getRoleUsers_success() {

        Pageable pageable = PageRequest.of(0, 10);

        Account account = new Account();
        account.setId("user-1");
        account.setUsername("admin");

        Page<Account> page = new PageImpl<>(List.of(account), pageable, 1);

        AccountResponse response = mock(AccountResponse.class);

        when(roleRepository.existsById(roleId1)).thenReturn(true);
        when(accountRepository.findUsersByRoleId(roleId1, pageable)).thenReturn(page);
        when(accountMapper.toAccountResponse(account)).thenReturn(response);

        PageResponse<AccountResponse> result = service.getRoleUsers(roleId1, pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());

        verify(accountRepository).findUsersByRoleId(roleId1, pageable);
    }

    @Test
    void getRoleUsers_roleNotFound_throwException() {

        Pageable pageable = PageRequest.of(0, 10);

        when(roleRepository.existsById(roleId1)).thenReturn(false);

        assertThrows(NotFoundException.class,
                () -> service.getRoleUsers(roleId1, pageable));

        verify(accountRepository, never()).findUsersByRoleId(any(), any());
    }

    @Test
    void getRoleUsers_emptyResult() {

        Pageable pageable = PageRequest.of(0, 10);

        Page<Account> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

        when(roleRepository.existsById(roleId1)).thenReturn(true);
        when(accountRepository.findUsersByRoleId(roleId1, pageable)).thenReturn(emptyPage);

        PageResponse<AccountResponse> result = service.getRoleUsers(roleId1, pageable);

        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());

        verify(accountRepository).findUsersByRoleId(roleId1, pageable);
    }
}
