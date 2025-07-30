package com.amannmalik.mcp.security;

import com.amannmalik.mcp.auth.Principal;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class SamplingAccessController implements SamplingAccessPolicy {
    private final Set<String> allowed = ConcurrentHashMap.newKeySet();

    public void allow(String principalId) {
        if (principalId == null || principalId.isBlank()) throw new IllegalArgumentException("principalId required");
        allowed.add(principalId);
    }

    public void revoke(String principalId) {
        if (principalId == null || principalId.isBlank()) throw new IllegalArgumentException("principalId required");
        allowed.remove(principalId);
    }

    @Override
    public void requireAllowed(Principal principal) {
        if (principal == null) throw new IllegalArgumentException("principal required");
        if (!allowed.contains(principal.id())) {
            throw new SecurityException("Sampling not authorized");
        }
    }
}

