package org.demo.whs.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.Account;
import org.demo.whs.entity.AccountHasRole;
import org.demo.whs.entity.Role;
import org.demo.whs.entity.dto.request.UserRole.AssignRolesRequest;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.dto.response.Role.RoleResponse;
import org.demo.whs.entity.dto.response.User.AccountResponse;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.exception.NotFoundException;
import org.demo.whs.mapper.AccountMapper;
import org.demo.whs.mapper.RoleMapper;
import org.demo.whs.mapper.UserRoleMapper;
import org.demo.whs.repository.AccountHasRoleRepository;
import org.demo.whs.repository.AccountRepository;
import org.demo.whs.repository.RoleRepository;
import org.demo.whs.service.PermissionCacheService;
import org.demo.whs.service.UserRoleService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserRoleServiceImpl implements UserRoleService {

    private final AccountRepository accountRepository;
    private final RoleRepository roleRepository;
    private final AccountHasRoleRepository accountHasRoleRepository;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final AccountMapper accountMapper;
    private final PermissionCacheService permissionCacheService;

    @Override
    @Transactional
    public List<RoleResponse> assignRolesToUser(String userId, AssignRolesRequest request) {

        log.info("Assign roles to user {}", userId);

        if (request == null || request.getRoleIds() == null || request.getRoleIds().isEmpty()) {
            throw new BadRequestException(ErrorCode.USER_ROLE_005);
        }

        if (!accountRepository.existsById(userId)) {
            throw new NotFoundException(ErrorCode.USER_ROLE_001);
        }

        Set<String> requestedRoleIds = new HashSet<>(request.getRoleIds());

        List<Role> roles = roleRepository.findAllById(requestedRoleIds);

        if (roles.size() != requestedRoleIds.size()) {
            throw new NotFoundException(ErrorCode.USER_ROLE_002);
        }

        List<AccountHasRole> existing = accountHasRoleRepository.findByIdAccountId(userId);

        Set<String> existingRoleIds = existing.stream()
                .map(r -> r.getId().getRoleId())
                .collect(Collectors.toSet());

        // Find roles to remove (exist but not in request)
        Set<String> rolesToRemove = new HashSet<>(existingRoleIds);
        rolesToRemove.removeAll(requestedRoleIds);

        // Find roles to add (in request but not exist yet)
        Set<String> rolesToAdd = new HashSet<>(requestedRoleIds);
        rolesToAdd.removeAll(existingRoleIds);

        // Remove old roles
        if (!rolesToRemove.isEmpty()) {
            for (String roleId : rolesToRemove) {
                accountHasRoleRepository.deleteByIdAccountIdAndIdRoleId(userId, roleId);
                log.info("Removed role {} from user {}", roleId, userId);
            }
        }

        // Add new roles
        List<AccountHasRole> toSave = new ArrayList<>();
        for (Role role : roles) {
            if (rolesToAdd.contains(role.getId())) {
                toSave.add(userRoleMapper.createEntity(userId, role.getId()));
            }
        }

        if (!toSave.isEmpty()) {
            accountHasRoleRepository.saveAll(toSave);
            log.info("Added {} new roles to user {}", toSave.size(), userId);
        }

        // Evict cache if any changes
        if (!rolesToRemove.isEmpty() || !toSave.isEmpty()) {
            permissionCacheService.evictPermissions(userId);
        }

        // Return updated roles
        List<AccountHasRole> updated = accountHasRoleRepository.findByIdAccountId(userId);
        List<String> roleIds = updated.stream()
                .map(r -> r.getId().getRoleId())
                .toList();

        List<Role> result = roleRepository.findAllById(roleIds);

        return result.stream()
                .map(roleMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void removeRoleFromUser(String userId, String roleId) {

        log.info("Revoking role {} from user {}", roleId, userId);

        boolean exists = accountHasRoleRepository
                .existsByIdAccountIdAndIdRoleId(userId, roleId);

        if (!exists) {
            throw new BadRequestException(ErrorCode.USER_ROLE_004);
        }

        roleRepository.findById(roleId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.ROLE_001));

        long count = accountHasRoleRepository.countByIdAccountId(userId);

        if (count == 1) {
            log.warn("Cannot remove last role of user {}", userId);
            throw new BadRequestException(ErrorCode.USER_ROLE_008);
        }

        accountHasRoleRepository
                .deleteByIdAccountIdAndIdRoleId(userId, roleId);

        permissionCacheService.evictPermissions(userId);

        log.info("Deleted role {} from user {}", roleId, userId);

        long actualCount = accountHasRoleRepository.countByIdAccountId(userId);
        if (actualCount == 0) {
            throw new IllegalStateException("User must have at least one role");
        }
    }

    @Override
    public PageResponse<RoleResponse> getUserRoles(String userId, Pageable pageable) {

        log.info("Get roles for user {} with page={}", userId, pageable);

        if (!accountRepository.existsById(userId)) {
            log.warn("User {} not found", userId);
            throw new NotFoundException(ErrorCode.USER_ROLE_001);
        }

        long roleCount = accountHasRoleRepository.countByIdAccountId(userId);
        if (roleCount == 0) {
            log.warn("User {} has no roles", userId);
            return PageResponse.of(pageable.getPageNumber(), pageable.getPageSize(), List.of());
        }

        Page<Role> page = roleRepository.findRolesByUserId(userId, pageable);
        
        List<RoleResponse> roleResponses = page.stream()
                .map(roleMapper::toResponse)
                .toList();

        return PageResponse.from(page, roleResponses);
    }

    @Override
    public PageResponse<AccountResponse> getRoleUsers(String roleId, Pageable pageable) {
        log.info("Fetching users for roleId={} with pageable: page={}, size={}",
                roleId, pageable.getPageNumber(), pageable.getPageSize());

        if (!roleRepository.existsById(roleId)) {
            log.warn("Role not found: {}", roleId);
            throw new NotFoundException(ErrorCode.ROLE_001);
        }

        Page<Account> page = accountRepository.findUsersByRoleId(roleId, pageable);
        log.info("Found {} users for roleId={}", page.getTotalElements(), roleId);

        List<AccountResponse> content = page.getContent().stream()
                .map(accountMapper::toAccountResponse)
                .peek(dto -> log.debug("Mapped Account {} -> AccountResponse {}", dto.getAccountId(), dto.getUsername()))
                .collect(Collectors.toList());

        log.info("Returning page {} of size {} with totalPages={} for roleId={}",
                page.getNumber(), page.getSize(), page.getTotalPages(), roleId);

        return PageResponse.from(page, content);
    }
}
