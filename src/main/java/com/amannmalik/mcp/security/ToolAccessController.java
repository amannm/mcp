package com.amannmalik.mcp.security;

import com.amannmalik.mcp.auth.Principal;

public final class ToolAccessController implements ToolAccessPolicy {
    private final PrincipalPermissions<String> permissions = new PrincipalPermissions<>();

    public void allow(String principalId, String tool) {
        permissions.grant(principalId, tool);
    }

    public void revoke(String principalId, String tool) {
        permissions.revoke(principalId, tool);
    }

    @Override
    public void requireAllowed(Principal principal, String tool) {
        if (principal == null) throw new IllegalArgumentException("principal required");
        if (tool == null || tool.isBlank()) throw new IllegalArgumentException("tool required");
        permissions.requirePermission(principal.id(), tool, "Tool not authorized: " + tool);
    }
}
