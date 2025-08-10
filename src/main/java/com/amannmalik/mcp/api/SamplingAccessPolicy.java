package com.amannmalik.mcp.api;

@FunctionalInterface
public interface SamplingAccessPolicy {
    void requireAllowed(Principal principal);

    SamplingAccessPolicy PERMISSIVE = p -> {
    };
}

