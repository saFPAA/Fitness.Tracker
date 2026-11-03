package com.Author.Fitness.Security;

import org.springframework.security.access.AccessDeniedException;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static boolean isAdmin(CustomUserDetails currentUser) {
        return currentUser.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }

    public static void checkOwnershipOrAdmin(Long ownerId, CustomUserDetails currentUser) {
        if (!isAdmin(currentUser) && !ownerId.equals(currentUser.getId())) {
            throw new AccessDeniedException("You do not have permission to access this resource");
        }
    }
}