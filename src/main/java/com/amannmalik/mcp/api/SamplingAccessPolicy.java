package com.amannmalik.mcp.api;

import com.amannmalik.mcp.api.model.Principal;

@FunctionalInterface
public interface SamplingAccessPolicy {
    SamplingAccessPolicy PERMISSIVE = p -> {
    };

    void requireAllowed(Principal principal);
}

