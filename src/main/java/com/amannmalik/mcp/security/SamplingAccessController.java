package com.amannmalik.mcp.security;

import com.amannmalik.mcp.api.SamplingAccessPolicy;
import com.amannmalik.mcp.api.Principal;

import java.util.Objects;

public final class SamplingAccessController implements SamplingAccessPolicy {
    private static final Boolean PERMISSION = Boolean.TRUE;
    private final PrincipalPermissions<Boolean> permissions = new PrincipalPermissions<>();

    public void allow(String principalId) {
        permissions.grant(Objects.requireNonNull(principalId, "principalId required"), PERMISSION);
    }

    public void revoke(String principalId) {
        permissions.revoke(Objects.requireNonNull(principalId, "principalId required"), PERMISSION);
    }

    @Override
    public void requireAllowed(Principal principal) {
        if (principal == null) throw new IllegalArgumentException("principal required");
        permissions.requirePermission(principal.id(), PERMISSION, "Sampling not authorized");
    }
}

