package com.amannmalik.mcp.security;

import com.amannmalik.mcp.auth.Principal;
import com.amannmalik.mcp.annotations.Annotations;

@FunctionalInterface
public interface ResourceAccessController {
    void requireAllowed(Principal principal, Annotations annotations);

    ResourceAccessController ALLOW_ALL = (p, a) -> {
    };
}
