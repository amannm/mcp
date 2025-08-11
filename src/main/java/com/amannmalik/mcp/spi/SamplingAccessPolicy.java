package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.api.model.Principal;

@FunctionalInterface
public interface SamplingAccessPolicy {
    SamplingAccessPolicy PERMISSIVE = p -> {
    };

    void requireAllowed(Principal principal);
}

