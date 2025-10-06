package com.amannmalik.mcp.spi;

@FunctionalInterface
public interface SamplingAccessPolicy {
    void requireAllowed(Principal principal);
}

