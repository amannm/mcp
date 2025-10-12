package com.amannmalik.mcp.spi.transport;

import com.amannmalik.mcp.spi.Principal;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

public final class BearerTokenAuthorizationStrategy implements AuthorizationStrategy {
    private static final Pattern BEARER_PREFIX = Pattern.compile("^bearer\\b", Pattern.CASE_INSENSITIVE);
    private final TokenValidator validator;

    public BearerTokenAuthorizationStrategy(TokenValidator validator) {
        this.validator = Objects.requireNonNull(validator, "validator");
    }

    @Override
    public Optional<Principal> authorize(String authorizationHeader) throws AuthorizationException {
        if (authorizationHeader == null) {
            return Optional.empty();
        }
        var trimmed = authorizationHeader.trim();
        if (trimmed.isEmpty() || !BEARER_PREFIX.matcher(trimmed).find()) {
            return Optional.empty();
        }
        var parts = trimmed.split("\\s+", 2);
        if (parts.length != 2 || parts[1].trim().isEmpty()) {
            throw new AuthorizationException("Invalid bearer token", 400);
        }
        return Optional.of(validator.validate(parts[1].trim()));
    }
}
