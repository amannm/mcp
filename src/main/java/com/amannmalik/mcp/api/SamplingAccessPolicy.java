package com.amannmalik.mcp.api;

@FunctionalInterface
public interface SamplingAccessPolicy {
    SamplingAccessPolicy PERMISSIVE = p -> {
    };

    void requireAllowed(Principal principal);
}

