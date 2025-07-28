package com.amannmalik.mcp.security;

import com.amannmalik.mcp.auth.Principal;
import com.amannmalik.mcp.server.resources.ResourceAnnotations;

@FunctionalInterface
public interface ResourceAccessController {
    void requireAllowed(Principal principal, ResourceAnnotations annotations);

    ResourceAccessController ALLOW_ALL = (p, a) -> {};
}
