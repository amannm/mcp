package com.amannmalik.mcp.auth;

import com.amannmalik.mcp.spi.Principal;

import java.util.List;

/// - [Authorization](specification/2025-06-18/basic/authorization.mdx)
/// - [Security Best Practices](specification/2025-06-18/basic/security_best_practices.mdx)
public final class AuthorizationManager {
    private final List<AuthorizationStrategy> strategies;

    public AuthorizationManager(List<AuthorizationStrategy> strategies) {
        if (strategies == null || strategies.isEmpty()) {
            throw new IllegalArgumentException("strategies required");
        }
        this.strategies = List.copyOf(strategies);
    }

    public Principal authorize(String authorizationHeader) throws AuthorizationException {
        for (var strategy : strategies) {
            var result = strategy.authorize(authorizationHeader);
            if (result.isPresent()) {
                return result.get();
            }
        }
        throw new AuthorizationException("No valid authorization strategy");
    }
}
