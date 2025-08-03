package com.amannmalik.mcp.auth;

import com.amannmalik.mcp.validation.InputSanitizer;

import java.util.Set;

public final class PermissiveTokenValidator implements TokenValidator {
    @Override
    public Principal validate(String token) throws AuthorizationException {
        if (token == null || token.isBlank()) throw new AuthorizationException("token required");
        return new Principal(InputSanitizer.requireClean(token), Set.of());
    }
}
