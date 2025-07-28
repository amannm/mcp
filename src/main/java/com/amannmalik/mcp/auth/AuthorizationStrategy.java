package com.amannmalik.mcp.auth;

import java.util.Optional;

@FunctionalInterface
public interface AuthorizationStrategy {
    Optional<Principal> authorize(String authorizationHeader) throws AuthorizationException;
}
