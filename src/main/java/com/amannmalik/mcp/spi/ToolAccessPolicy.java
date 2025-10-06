package com.amannmalik.mcp.spi;

@FunctionalInterface
public interface ToolAccessPolicy {
    void requireAllowed(Principal principal, Tool tool);
}
