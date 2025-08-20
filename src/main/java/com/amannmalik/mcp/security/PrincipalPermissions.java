package com.amannmalik.mcp.security;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

final class PrincipalPermissions<T> {
    private final Map<String, Set<T>> map = new ConcurrentHashMap<>();

    private static void requirePrincipal(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("principalId required");
        }
    }

    public void grant(String principalId, T permission) {
        requirePrincipal(principalId);
        if (permission == null) {
            throw new IllegalArgumentException("permission required");
        }
        map.computeIfAbsent(principalId, k -> ConcurrentHashMap.newKeySet())
                .add(permission);
    }

    public void revoke(String principalId, T permission) {
        requirePrincipal(principalId);
        if (permission == null) {
            throw new IllegalArgumentException("permission required");
        }
        var set = map.get(principalId);
        if (set != null) {
            set.remove(permission);
        }
    }

    public boolean contains(String principalId, T permission) {
        requirePrincipal(principalId);
        if (permission == null) {
            throw new IllegalArgumentException("permission required");
        }
        var set = map.get(principalId);
        return set != null && set.contains(permission);
    }

    public void requirePermission(String principalId, T permission, String message) {
        if (!contains(principalId, permission)) {
            throw new SecurityException(message);
        }
    }
}

