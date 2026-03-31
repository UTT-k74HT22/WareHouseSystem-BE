package org.demo.whs.service.impl;

import org.demo.whs.entity.dto.response.Permission.MyPermissionsResponse;
import org.demo.whs.exception.UnauthorizedException;
import org.demo.whs.security.SecurityUtils;
import org.demo.whs.service.PermissionCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl My Permissions Unit Tests")
class AuthMyPermissionsServiceImplTest {

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

    @Test
    @DisplayName("Should return sorted permissions for current user")
    void should_ReturnSortedPermissions_ForCurrentUser() {
        try (var mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentAccountId).thenReturn("account-1");
            when(permissionCacheService.getPermissions("account-1")).thenReturn(new LinkedHashSet<>(List.of(
                    "PERM_USER_READ",
                    "PERM_PRODUCT_READ",
                    "PERM_BATCH_CREATE"
            )));

            MyPermissionsResponse response = authService.getMyPermissions();

            assertThat(response.getPermissions()).containsExactly(
                    "PERM_BATCH_CREATE",
                    "PERM_PRODUCT_READ",
                    "PERM_USER_READ"
            );
        }
    }

    @Test
    @DisplayName("Should return empty permissions when cache has no data")
    void should_ReturnEmptyPermissions_When_CacheHasNoData() {
        try (var mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentAccountId).thenReturn("account-1");
            when(permissionCacheService.getPermissions("account-1")).thenReturn(null);

            MyPermissionsResponse response = authService.getMyPermissions();

            assertThat(response.getPermissions()).isEmpty();
        }
    }

    @Test
    @DisplayName("Should throw unauthorized when current user is missing")
    void should_ThrowUnauthorized_When_CurrentUserIsMissing() {
        try (var mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentAccountId).thenReturn(null);

            assertThatThrownBy(() -> authService.getMyPermissions())
                    .isInstanceOf(UnauthorizedException.class)
                    .hasFieldOrPropertyWithValue("errorCode", "AUTH_005");
        }
    }
}
