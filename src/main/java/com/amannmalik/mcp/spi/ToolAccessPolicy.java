package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.api.model.Principal;

@FunctionalInterface
public interface ToolAccessPolicy {
    ToolAccessPolicy PERMISSIVE = (p, t) -> {
    };

    void requireAllowed(Principal principal, String tool);
}
