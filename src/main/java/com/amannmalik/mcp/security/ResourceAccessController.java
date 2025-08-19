package com.amannmalik.mcp.security;

import com.amannmalik.mcp.spi.*;

public final class ResourceAccessController implements ResourceAccessPolicy {
    private final PrincipalPermissions<Role> permissions = new PrincipalPermissions<>();

    public void allow(String principalId, Role audience) {
        permissions.grant(principalId, audience);
    }

    public void revoke(String principalId, Role audience) {
        permissions.revoke(principalId, audience);
    }

    @Override
    public void requireAllowed(Principal principal, Annotations ann) {
        if (principal == null) throw new IllegalArgumentException("principal required");
        if (ann == null || ann.audience().isEmpty()) return;

        for (var r : ann.audience()) {
            permissions.requirePermission(principal.id(), r, "Audience not permitted: " + ann.audience());
        }
    }
}
