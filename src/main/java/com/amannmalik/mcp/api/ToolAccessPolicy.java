package com.amannmalik.mcp.api;

@FunctionalInterface
public interface ToolAccessPolicy {
    void requireAllowed(Principal principal, String tool);

    ToolAccessPolicy PERMISSIVE = (p, t) -> {
    };
}
