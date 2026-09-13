package org.demo.whs.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import org.demo.whs.repository.PermissionRepository;
import org.demo.whs.service.RedisService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PermissionCacheServiceImpl Unit Tests")
class PermissionCacheServiceImplTest {

    @Mock
    private RedisService redisService;

    @Mock
    private PermissionRepository permissionRepository;

    @InjectMocks
    private PermissionCacheServiceImpl permissionCacheService;

    @Nested
    @DisplayName("getPermissions tests")
    class GetPermissionsTests {

        @Test
        @DisplayName("should_ReturnEmptySet_When_UserIdIsNull")
        void should_ReturnEmptySet_When_UserIdIsNull() {
            Set<String> result = permissionCacheService.getPermissions(null);
            assertThat(result).isEmpty();
            verifyNoInteractions(redisService, permissionRepository);
        }

        @Test
        @DisplayName("should_ReturnEmptySet_When_UserIdIsBlank")
        void should_ReturnEmptySet_When_UserIdIsBlank() {
            Set<String> result = permissionCacheService.getPermissions("   ");
            assertThat(result).isEmpty();
            verifyNoInteractions(redisService, permissionRepository);
        }

        @Test
        @DisplayName("should_ReturnCachedPermissions_When_CacheHit")
        void should_ReturnCachedPermissions_When_CacheHit() {
            // Arrange
            Set<String> cached = Set.of("PERMISSION_A", "PERMISSION_B");
            when(redisService.getOptional(eq("user:permissions:usr-1"), any(TypeReference.class)))
                    .thenReturn(Optional.of(cached));

            // Act
            Set<String> result = permissionCacheService.getPermissions("usr-1");

            // Assert
            assertThat(result).containsExactlyInAnyOrder("PERMISSION_A", "PERMISSION_B");
            verifyNoInteractions(permissionRepository);
        }

        @Test
        @DisplayName("should_QueryDBAndSaveCache_When_CacheMiss")
        void should_QueryDBAndSaveCache_When_CacheMiss() {
            // Arrange
            Set<String> dbPermissions = Set.of("PERM_VIEW", "PERM_EDIT");
            when(redisService.getOptional(eq("user:permissions:usr-2"), any(TypeReference.class)))
                    .thenReturn(Optional.empty());
            when(permissionRepository.getPermissionCodesByUserId("usr-2")).thenReturn(dbPermissions);

            // Act
            Set<String> result = permissionCacheService.getPermissions("usr-2");

            // Assert
            assertThat(result).containsExactlyInAnyOrder("PERM_VIEW", "PERM_EDIT");
            verify(permissionRepository).getPermissionCodesByUserId("usr-2");
            verify(redisService).saveWithTTL(
                    eq("user:permissions:usr-2"),
                    eq(dbPermissions),
                    eq(15L),
                    eq(TimeUnit.MINUTES)
            );
        }

        @Test
        @DisplayName("should_ReturnEmptySet_And_NotThrow_When_DBQueryFails")
        void should_ReturnEmptySet_And_NotThrow_When_DBQueryFails() {
            // Arrange
            when(redisService.getOptional(anyString(), any(TypeReference.class))).thenReturn(Optional.empty());
            when(permissionRepository.getPermissionCodesByUserId("usr-err"))
                    .thenThrow(new RuntimeException("DB is down"));

            // Act
            Set<String> result = permissionCacheService.getPermissions("usr-err");

            // Assert - graceful degradation, never throws
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should_ReturnEmptySet_And_NotThrow_When_CacheSaveFails")
        void should_ReturnEmptySet_And_NotThrow_When_CacheSaveFails() {
            // Arrange
            when(redisService.getOptional(anyString(), any(TypeReference.class))).thenReturn(Optional.empty());
            when(permissionRepository.getPermissionCodesByUserId("usr-3")).thenReturn(Set.of("PERM_A"));
            doThrow(new RuntimeException("Redis timeout"))
                    .when(redisService).saveWithTTL(anyString(), any(), anyLong(), any());

            // Act
            Set<String> result = permissionCacheService.getPermissions("usr-3");

            // Assert - should still return DB data even when cache save fails
            assertThat(result).containsExactly("PERM_A");
        }
    }

    @Nested
    @DisplayName("evictPermissions tests")
    class EvictPermissionsTests {

        @Test
        @DisplayName("should_DeleteCacheKey_When_ValidUserId")
        void should_DeleteCacheKey_When_ValidUserId() {
            permissionCacheService.evictPermissions("usr-1");
            verify(redisService).delete("user:permissions:usr-1");
        }

        @Test
        @DisplayName("should_NotThrow_When_RedisDeleteFails")
        void should_NotThrow_When_RedisDeleteFails() {
            doThrow(new RuntimeException("Redis unavailable"))
                    .when(redisService).delete(anyString());

            // Should not propagate exception
            permissionCacheService.evictPermissions("usr-fail");
        }
    }

    @Nested
    @DisplayName("evictAllUsers tests")
    class EvictAllUsersTests {

        @Test
        @DisplayName("should_DeleteAllMatchingKeys_When_KeysExist")
        void should_DeleteAllMatchingKeys_When_KeysExist() {
            Set<String> keys = Set.of("user:permissions:u1", "user:permissions:u2");
            when(redisService.getAllKeys("user:permissions:*")).thenReturn(keys);

            permissionCacheService.evictAllUsers();

            verify(redisService).delete(keys);
        }

        @Test
        @DisplayName("should_NotCallDelete_When_NoKeysExist")
        void should_NotCallDelete_When_NoKeysExist() {
            when(redisService.getAllKeys("user:permissions:*")).thenReturn(Set.of());

            permissionCacheService.evictAllUsers();

            verify(redisService, never()).delete(any(Set.class));
        }

        @Test
        @DisplayName("should_NotThrow_When_RedisGetAllKeysFails")
        void should_NotThrow_When_RedisGetAllKeysFails() {
            doThrow(new RuntimeException("Redis is down"))
                    .when(redisService).getAllKeys(anyString());

            // Should not propagate exception
            permissionCacheService.evictAllUsers();
        }
    }
}
