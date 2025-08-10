package com.amannmalik.mcp.api.sampling;

import com.amannmalik.mcp.api.Principal;

@FunctionalInterface
public interface SamplingAccessPolicy {
    void requireAllowed(Principal principal);

    SamplingAccessPolicy PERMISSIVE = p -> {
    };
}

