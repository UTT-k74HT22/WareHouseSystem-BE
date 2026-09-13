package org.demo.whs.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import org.demo.whs.exception.StorageException;
import org.demo.whs.repository.PermissionRepository;
import org.demo.whs.service.RedisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PermissionCacheServiceImpl Unit Tests")
class PermissionCacheServiceImplTest {

    @Mock
    private RedisService redisService;

    @Mock
    private PermissionRepository permissionRepository;

    private PermissionCacheServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PermissionCacheServiceImpl(redisService, permissionRepository);
    }

    @Test
    @DisplayName("Should return cached permissions when cache hit")
    void should_ReturnCachedPermissions_WhenCacheHit() {
        Set<String> cachedPermissions = Set.of("PERM_USER_READ");
        when(redisService.getOptional(eq("user:permissions:user-1"), any(TypeReference.class)))
                .thenReturn(Optional.of(cachedPermissions));

        Set<String> result = service.getPermissions("user-1");

        assertThat(result).containsExactlyInAnyOrder("PERM_USER_READ");
        verify(permissionRepository, never()).getPermissionCodesByUserId("user-1");
    }

    @Test
    @DisplayName("Should fallback to database when cache read fails")
    void should_FallbackToDatabase_WhenCacheReadFails() {
        Set<String> dbPermissions = Set.of("PERM_PRODUCT_READ");
        when(redisService.getOptional(eq("user:permissions:user-1"), any(TypeReference.class)))
                .thenThrow(new RuntimeException("Redis unavailable"));
        when(permissionRepository.getPermissionCodesByUserId("user-1"))
                .thenReturn(dbPermissions);

        Set<String> result = service.getPermissions("user-1");

        assertThat(result).containsExactlyInAnyOrder("PERM_PRODUCT_READ");
        verify(redisService).saveWithTTL(
                eq("user:permissions:user-1"),
                eq(dbPermissions),
                eq(15L),
                eq(TimeUnit.MINUTES)
        );
    }

    @Test
    @DisplayName("Should throw service unavailable when database query fails")
    void should_ThrowServiceUnavailable_WhenDatabaseQueryFails() {
        when(redisService.getOptional(eq("user:permissions:user-1"), any(TypeReference.class)))
                .thenReturn(Optional.empty());
        when(permissionRepository.getPermissionCodesByUserId("user-1"))
                .thenThrow(new RuntimeException("DB unavailable"));

        assertThatThrownBy(() -> service.getPermissions("user-1"))
                .isInstanceOf(StorageException.class)
                .hasFieldOrPropertyWithValue("errorCode", "PERM_013");
    }
}
