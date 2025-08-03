package com.amannmalik.mcp.tools;

import com.amannmalik.mcp.auth.Principal;

@FunctionalInterface
public interface ToolAccessPolicy {
    void requireAllowed(Principal principal, String tool);

    ToolAccessPolicy PERMISSIVE = (p, t) -> {
    };
}
