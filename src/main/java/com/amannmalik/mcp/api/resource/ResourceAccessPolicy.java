package com.amannmalik.mcp.api.resource;

import com.amannmalik.mcp.api.Annotations;
import com.amannmalik.mcp.api.Principal;

@FunctionalInterface
public interface ResourceAccessPolicy {
    void requireAllowed(Principal principal, Annotations annotations);
}
