package com.amannmalik.mcp.security;

import com.amannmalik.mcp.auth.Principal;

/**
 * Access controller for sampling requests. Internally uses {@link PrincipalPermissions}
 * to keep the implementation consistent with other policy classes.
 */
public final class SamplingAccessController implements SamplingAccessPolicy {
    private static final Boolean PERMISSION = Boolean.TRUE;

    private final PrincipalPermissions<Boolean> permissions = new PrincipalPermissions<>();

    public void allow(String principalId) {
        permissions.grant(principalId, PERMISSION);
    }

    public void revoke(String principalId) {
        permissions.revoke(principalId, PERMISSION);
    }

    @Override
    public void requireAllowed(Principal principal) {
        if (principal == null) throw new IllegalArgumentException("principal required");
        if (!permissions.contains(principal.id(), PERMISSION)) {
            throw new SecurityException("Sampling not authorized");
        }
    }
}

