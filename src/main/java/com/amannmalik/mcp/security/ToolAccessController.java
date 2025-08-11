package com.amannmalik.mcp.security;

import com.amannmalik.mcp.spi.Principal;
import com.amannmalik.mcp.spi.Tool;
import com.amannmalik.mcp.spi.ToolAccessPolicy;

public final class ToolAccessController implements ToolAccessPolicy {
    private final PrincipalPermissions<String> permissions = new PrincipalPermissions<>();

    public void allow(String principalId, String tool) {
        permissions.grant(principalId, tool);
    }

    public void revoke(String principalId, String tool) {
        permissions.revoke(principalId, tool);
    }

    @Override
    public void requireAllowed(Principal principal, Tool tool) {
        if (principal == null) throw new IllegalArgumentException("principal required");
        if (tool == null) throw new IllegalArgumentException("tool required");
        permissions.requirePermission(principal.id(), tool.name(), "Tool not authorized: " + tool.name());
    }
}
