package com.amannmalik.mcp.spi;

@FunctionalInterface
public interface SamplingAccessPolicy {
    SamplingAccessPolicy PERMISSIVE = p -> {
    };

    void requireAllowed(Principal principal);
}

