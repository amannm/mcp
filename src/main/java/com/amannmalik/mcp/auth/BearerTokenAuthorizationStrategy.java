package com.amannmalik.mcp.auth;

import com.amannmalik.mcp.spi.Principal;

import java.util.Optional;

public final class BearerTokenAuthorizationStrategy implements AuthorizationStrategy {
    private final TokenValidator validator;

    public BearerTokenAuthorizationStrategy(TokenValidator validator) {
        this.validator = validator;
    }

    @Override
    public Optional<Principal> authorize(String authorizationHeader) throws AuthorizationException {
        if (authorizationHeader == null) return Optional.empty();
        String[] parts = authorizationHeader.split("\\s+", 2);
        if (!"bearer".equalsIgnoreCase(parts[0])) return Optional.empty();
        if (parts.length != 2 || parts[1].trim().isEmpty()) {
            throw new AuthorizationException("Invalid bearer token", 400);
        }
        return Optional.of(validator.validate(parts[1].trim()));
    }
}
