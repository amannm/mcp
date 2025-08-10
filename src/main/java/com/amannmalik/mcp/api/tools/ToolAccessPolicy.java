package com.amannmalik.mcp.api.tools;

import com.amannmalik.mcp.api.Principal;

@FunctionalInterface
public interface ToolAccessPolicy {
    void requireAllowed(Principal principal, String tool);

    ToolAccessPolicy PERMISSIVE = (p, t) -> {
    };
}
