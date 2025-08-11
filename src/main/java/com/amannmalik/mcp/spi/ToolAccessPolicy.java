package com.amannmalik.mcp.spi;

@FunctionalInterface
public interface ToolAccessPolicy {
    ToolAccessPolicy PERMISSIVE = (p, t) -> {
    };

    void requireAllowed(Principal principal, String tool);
}
