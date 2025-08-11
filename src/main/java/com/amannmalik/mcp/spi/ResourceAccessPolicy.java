package com.amannmalik.mcp.spi;

@FunctionalInterface
public interface ResourceAccessPolicy {
    void requireAllowed(Principal principal, Annotations annotations);
}
