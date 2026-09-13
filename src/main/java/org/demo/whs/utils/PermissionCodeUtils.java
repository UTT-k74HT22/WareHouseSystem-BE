package org.demo.whs.utils;

import org.demo.whs.entity.enums.ActionType;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.ErrorCode;

/**
 * Central helper for building and normalizing permission codes.
 * Single source of truth for {@code PERM_<RESOURCE>_<ACTION>} format.
 */
public final class PermissionCodeUtils {

    private PermissionCodeUtils() {
    }

    public static String generateCode(String resource, ActionType action) {
        if (action == null) {
            throw new BadRequestException(ErrorCode.PERM_010);
        }
        return "PERM_" + normalizeResource(resource) + "_" + action.name();
    }

    public static String normalizeResource(String resource) {
        if (resource == null || resource.isBlank()) {
            throw new BadRequestException(ErrorCode.PERM_009);
        }
        String normalized = resource.trim().toUpperCase();
        if (!normalized.matches("^[A-Z][A-Z0-9]*(?:_[A-Z0-9]+)*$")) {
            throw new BadRequestException(ErrorCode.PERM_009);
        }
        return normalized;
    }
}
