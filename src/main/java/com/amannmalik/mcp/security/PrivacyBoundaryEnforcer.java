package com.amannmalik.mcp.security;

import com.amannmalik.mcp.annotations.Annotations;
import com.amannmalik.mcp.auth.Principal;
import com.amannmalik.mcp.prompts.Role;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class PrivacyBoundaryEnforcer implements ResourceAccessController {
    private final Map<String, Set<Role>> permissions = new ConcurrentHashMap<>();

    public void allow(String principalId, Role audience) {
        permissions.computeIfAbsent(principalId, k -> EnumSet.noneOf(Role.class))
                .add(audience);
    }

    public void revoke(String principalId, Role audience) {
        var set = permissions.get(principalId);
        if (set != null) set.remove(audience);
    }

    @Override
    public void requireAllowed(Principal principal, Annotations ann) {
        if (principal == null) throw new IllegalArgumentException("principal required");
        if (ann == null || ann.audience().isEmpty()) return;

        var set = permissions.get(principal.id());
        if (set == null || !set.containsAll(ann.audience())) {
            throw new SecurityException("Audience not permitted: " + ann.audience());
        }
    }
}
