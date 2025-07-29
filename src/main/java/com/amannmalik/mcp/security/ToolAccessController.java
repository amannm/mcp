package com.amannmalik.mcp.security;

import com.amannmalik.mcp.auth.Principal;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ToolAccessController implements ToolAccessPolicy {
    private final Map<String, Set<String>> permissions = new ConcurrentHashMap<>();

    public void allow(String principalId, String tool) {
        permissions.computeIfAbsent(principalId, k -> ConcurrentHashMap.newKeySet())
                .add(tool);
    }

    public void revoke(String principalId, String tool) {
        var set = permissions.get(principalId);
        if (set != null) set.remove(tool);
    }

    @Override
    public void requireAllowed(Principal principal, String tool) {
        if (principal == null) throw new IllegalArgumentException("principal required");
        if (tool == null || tool.isBlank()) throw new IllegalArgumentException("tool required");
        var set = permissions.get(principal.id());
        if (set == null || !set.contains(tool)) {
            throw new SecurityException("Tool not authorized: " + tool);
        }
    }
}
