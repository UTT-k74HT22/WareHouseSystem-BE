package org.demo.whs.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

public class SecurityUtils {

    private SecurityUtils() {
        // Utility class
    }

    public static String getCurrentUsername() {
        Authentication authentication = getCurrentAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }

        if (principal instanceof String principalName) {
            return principalName;
        }

        return authentication.getName(); // fallback
    }

    /**
     * Retrieves the ID (UUID) of the currently authenticated user.
     *
     * @return the user ID or null if unauthenticated or not using CustomUserDetails
     */
    public static String getCurrentAccountId() {
        Authentication authentication = getCurrentAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails customUserDetails) {
            return customUserDetails.getAccount().getId();
        }

        return null;
    }

    public static boolean hasAuthority(String authority) {
        Authentication authentication = getCurrentAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || authority == null || authority.isBlank()) {
            return false;
        }

        return authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> authority.equals(grantedAuthority.getAuthority()));
    }

    private static Authentication getCurrentAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }
}
