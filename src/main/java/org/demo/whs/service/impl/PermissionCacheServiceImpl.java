package org.demo.whs.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.repository.PermissionRepository;
import org.demo.whs.service.PermissionCacheService;
import org.demo.whs.service.RedisService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class PermissionCacheServiceImpl implements PermissionCacheService {

    private final RedisService redisService;
    private final PermissionRepository permissionRepository;

    private static final String KEY_PREFIX = "user:permissions:";

    private static final long PERMISSION_CACHE_TTL_MINUTES = 15; // 🔥 TTL fix
    @Override
    public Set<String> getPermissions(String userId) {
        String key = KEY_PREFIX + userId;

        var cached = redisService.getOptional(key, new TypeReference<Set<String>>() {});

        if (cached.isPresent()) {
            log.debug("[CACHE HIT] userId={} -> {} permissions", userId, cached.get().size());
            return cached.get();
        }

        // MISS
        log.debug("[CACHE MISS] userId={}", userId);

        Set<String> permissions;
        try {
            permissions = permissionRepository.getPermissionCodesByUserId(userId);
            if (permissions == null) permissions = Collections.emptySet();
            log.debug("[DB QUERY] Loaded {} permissions for userId={}", permissions.size(), userId);
        } catch (Exception e) {
            log.error("[DB QUERY ERROR] userId={}: {}", userId, e.getMessage(), e);
            permissions = Collections.emptySet();
        }

        try {
            redisService.saveWithTTL(key, permissions, PERMISSION_CACHE_TTL_MINUTES, TimeUnit.MINUTES);
            log.debug("[CACHE SAVE] userId={} saved {} permissions with TTL {} minutes",
                    userId, permissions.size(), PERMISSION_CACHE_TTL_MINUTES);
        } catch (Exception e) {
            log.error("[CACHE SAVE ERROR] userId={}: {}", userId, e.getMessage(), e);
        }

        return permissions;
    }

    @Override
    public void evictPermissions(String userId) {
        try {
            redisService.delete(KEY_PREFIX + userId);
            log.debug("[CACHE EVICT] Evicted permissions cache for userId={}", userId);
        } catch (Exception e) {
            log.error("[CACHE EVICT ERROR] userId={}: {}", userId, e.getMessage(), e);
        }
    }

    // 🔥 Thêm method evictAllUsers
    @Override
    public void evictAllUsers() {
        try {
            Set<String> keys = redisService.getAllKeys(KEY_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisService.delete(keys);
                log.info("[CACHE EVICT ALL] Evicted permissions cache for all users, count={}", keys.size());
            } else {
                log.debug("[CACHE EVICT ALL] No keys to evict");
            }
        } catch (Exception e) {
            log.error("[CACHE EVICT ALL ERROR]: {}", e.getMessage(), e);
        }
    }
}