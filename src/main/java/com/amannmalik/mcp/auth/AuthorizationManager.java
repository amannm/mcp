package com.amannmalik.mcp.auth;

import java.util.List;

public final class AuthorizationManager {
    private final List<AuthorizationStrategy> strategies;

    public AuthorizationManager(List<AuthorizationStrategy> strategies) {
        if (strategies == null || strategies.isEmpty()) {
            throw new IllegalArgumentException("strategies required");
        }
        this.strategies = List.copyOf(strategies);
    }

    public Principal authorize(String authorizationHeader) throws AuthorizationException {
        for (AuthorizationStrategy strategy : strategies) {
            var result = strategy.authorize(authorizationHeader);
            if (result.isPresent()) return result.get();
        }
        throw new AuthorizationException("No valid authorization strategy");
    }
}
