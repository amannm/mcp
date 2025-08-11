package com.amannmalik.mcp.api;

@FunctionalInterface
public interface ToolAccessPolicy {
    ToolAccessPolicy PERMISSIVE = (p, t) -> {
    };

    void requireAllowed(Principal principal, String tool);
}
