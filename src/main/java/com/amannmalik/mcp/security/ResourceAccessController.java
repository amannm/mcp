package com.amannmalik.mcp.security;

import com.amannmalik.mcp.annotations.Annotations;
import com.amannmalik.mcp.auth.Principal;

@FunctionalInterface
public interface ResourceAccessController {
    void requireAllowed(Principal principal, Annotations annotations);
}
