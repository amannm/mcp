package com.amannmalik.mcp.security;

import com.amannmalik.mcp.auth.AuthorizationException;
import com.amannmalik.mcp.auth.Principal;
import com.amannmalik.mcp.auth.TokenValidator;
import com.amannmalik.mcp.security.SecurityViolationLogger.Level;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public final class MockTokenValidator implements TokenValidator {
    private final String expectedAudience;
    private final SecurityViolationLogger logger;

    public MockTokenValidator(String expectedAudience) {
        this(expectedAudience, null);
    }

    public MockTokenValidator(String expectedAudience, SecurityViolationLogger logger) {
        this.expectedAudience = expectedAudience;
        this.logger = logger;
    }

    @Override
    public Principal validate(String token) throws AuthorizationException {
        var claims = Arrays.stream(token.split(";"))
                .map(p -> p.split("=", 2))
                .collect(Collectors.toMap(p -> p[0], p -> p[1]));
        String audience = claims.get("aud");
        if (audience == null || !audience.equals(expectedAudience)) {
            log(Level.WARNING, "invalid_audience");
            throw new AuthorizationException("invalid_audience");
        }
        if (!"valid".equals(claims.get("exp"))) {
            log(Level.WARNING, "expired");
            throw new AuthorizationException("expired");
        }
        if (!"true".equals(claims.get("sig"))) {
            log(Level.WARNING, "invalid_signature");
            throw new AuthorizationException("invalid_signature");
        }
        return new Principal("user", Set.of("read"));
    }

    private void log(Level level, String message) {
        if (logger != null) {
            logger.log(level, message);
        }
    }
}
