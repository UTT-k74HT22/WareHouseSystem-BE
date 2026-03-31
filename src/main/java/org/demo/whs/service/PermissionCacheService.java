package org.demo.whs.service;

import java.util.Set;

/**
 * Service xử lý cache permission (Redis).
 */
public interface PermissionCacheService {

    /**
     * Lấy permission của user (ưu tiên Redis).
     */
    Set<String> getPermissions(String userId);

    /**
     * Xóa cache khi role/permission thay đổi.
     */
    void evictPermissions(String userId);

    void evictAllUsers();
}