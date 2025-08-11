package com.amannmalik.mcp.api;

import com.amannmalik.mcp.api.model.Annotations;
import com.amannmalik.mcp.api.model.Principal;

@FunctionalInterface
public interface ResourceAccessPolicy {
    void requireAllowed(Principal principal, Annotations annotations);
}
