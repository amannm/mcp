package com.amannmalik.mcp.security;

import java.util.Set;


public final class OriginValidator {
    private final Set<String> allowedOrigins;

    public OriginValidator(Set<String> allowedOrigins) {
        if (allowedOrigins == null || allowedOrigins.isEmpty()) {
            throw new IllegalArgumentException("allowedOrigins required");
        }
        this.allowedOrigins = Set.copyOf(allowedOrigins);
    }

    public boolean isValid(String origin) {
        if (origin == null || origin.isBlank()) {
            return false;
        }
        return allowedOrigins.contains(origin);
    }

    public void requireValid(String origin) {
        if (!isValid(origin)) {
            throw new SecurityException("Invalid origin: " + origin);
        }
    }
}
