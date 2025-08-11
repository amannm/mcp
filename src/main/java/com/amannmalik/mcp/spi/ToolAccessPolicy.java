package com.amannmalik.mcp.spi;

@FunctionalInterface
public interface ToolAccessPolicy {
    ToolAccessPolicy PERMISSIVE = (p, t) -> {
    };

    void requireAllowed(Principal principal, Tool tool);
}
