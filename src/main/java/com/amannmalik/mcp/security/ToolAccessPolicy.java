package com.amannmalik.mcp.security;

import com.amannmalik.mcp.auth.Principal;

@FunctionalInterface
public interface ToolAccessPolicy {
    void requireAllowed(Principal principal, String tool);

    ToolAccessPolicy PERMISSIVE = (p, t) -> {};
}
