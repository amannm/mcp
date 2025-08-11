package com.amannmalik.mcp.api;

import com.amannmalik.mcp.api.model.Principal;

@FunctionalInterface
public interface ToolAccessPolicy {
    ToolAccessPolicy PERMISSIVE = (p, t) -> {
    };

    void requireAllowed(Principal principal, String tool);
}
