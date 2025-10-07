package com.amannmalik.mcp.transport;

import com.amannmalik.mcp.spi.Principal;

import java.util.Optional;

/// - [Authorization](specification/2025-06-18/basic/authorization.mdx)
@FunctionalInterface
public interface AuthorizationStrategy {
    Optional<Principal> authorize(String authorizationHeader) throws AuthorizationException;
}
