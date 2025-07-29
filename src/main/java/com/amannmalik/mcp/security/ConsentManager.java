package com.amannmalik.mcp.security;

import com.amannmalik.mcp.auth.Principal;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


public final class ConsentManager {
    private final Map<String, Set<String>> consents = new ConcurrentHashMap<>();

    public void grant(String principalId, String scope) {
        consents.computeIfAbsent(principalId, k -> ConcurrentHashMap.newKeySet())
                .add(scope);
    }

    public void revoke(String principalId, String scope) {
        var set = consents.get(principalId);
        if (set != null) set.remove(scope);
    }

    public void requireConsent(Principal principal, String scope) {
        if (principal == null) throw new IllegalArgumentException("principal required");
        if (scope == null || scope.isBlank()) throw new IllegalArgumentException("scope required");
        var set = consents.get(principal.id());
        if (set == null || !set.contains(scope)) {
            throw new SecurityException("User consent required: " + scope);
        }
    }
}
