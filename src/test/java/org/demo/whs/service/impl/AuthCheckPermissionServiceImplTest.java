package org.demo.whs.service.impl;

import org.demo.whs.entity.Account;
import org.demo.whs.entity.enums.AccountStatus;
import org.demo.whs.entity.enums.ActionType;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.UnauthorizedException;
import org.demo.whs.security.CustomUserDetails;
import org.demo.whs.service.PermissionCacheService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl Check Permission Unit Tests")
class AuthCheckPermissionServiceImplTest {

    @Mock
    private PermissionCacheService permissionCacheService;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                permissionCacheService
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(String accountId) {
        Account account = Account.builder()
                .username("test.user")
                .password("hashed")
                .status(AccountStatus.ACTIVE)
                .build();
        account.setId(accountId);
        CustomUserDetails principal = new CustomUserDetails(account, List.of("USER"), Set.of());
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    @DisplayName("Should return true when current user has requested permission")
    void should_ReturnTrue_When_CurrentUserHasRequestedPermission() {
        authenticateAs("account-1");
        when(permissionCacheService.getPermissions("account-1"))
                .thenReturn(Set.of("PERM_USER_READ", "PERM_USER_CREATE"));

        boolean allowed = authService.checkPermission("USER", ActionType.READ);

        assertThat(allowed).isTrue();
        verify(permissionCacheService).getPermissions("account-1");
    }

    @Test
    @DisplayName("Should reject non canonical resource format")
    void should_RejectNonCanonicalResourceFormat() {
        authenticateAs("account-1");

        assertThatThrownBy(() -> authService.checkPermission("stock-adjustment", ActionType.READ))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("errorCode", "PERM_009");
    }

    @Test
    @DisplayName("Should return false when permission is missing")
    void should_ReturnFalse_When_PermissionIsMissing() {
        authenticateAs("account-1");
        when(permissionCacheService.getPermissions("account-1")).thenReturn(Set.of("PERM_USER_CREATE"));

        boolean allowed = authService.checkPermission("USER", ActionType.READ);

        assertThat(allowed).isFalse();
    }

    @Test
    @DisplayName("Should throw unauthorized when current user is missing")
    void should_ThrowUnauthorized_When_CurrentUserIsMissing() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> authService.checkPermission("USER", ActionType.READ))
                .isInstanceOf(UnauthorizedException.class)
                .hasFieldOrPropertyWithValue("errorCode", "AUTH_005");
    }
}
