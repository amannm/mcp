package com.amannmalik.mcp.sampling;

import com.amannmalik.mcp.auth.Principal;

@FunctionalInterface
public interface SamplingAccessPolicy {
    void requireAllowed(Principal principal);

    SamplingAccessPolicy PERMISSIVE = p -> {
    };
}

