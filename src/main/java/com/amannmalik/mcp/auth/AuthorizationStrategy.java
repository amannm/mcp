package com.amannmalik.mcp.auth;

import com.amannmalik.mcp.api.Principal;

import java.util.Optional;

/// - [Authorization](specification/2025-06-18/basic/authorization.mdx)
@FunctionalInterface
public interface AuthorizationStrategy {
    Optional<Principal> authorize(String authorizationHeader) throws AuthorizationException;
}
