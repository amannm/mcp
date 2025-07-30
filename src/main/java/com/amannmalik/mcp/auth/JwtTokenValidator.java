package com.amannmalik.mcp.auth;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Base64;
import java.util.Set;

/**
 * Minimal JWT validator that checks the audience, expiration and subject claims
 * without relying on external libraries.
 */
public final class JwtTokenValidator implements TokenValidator {
    private final String expectedAudience;
    private final Clock clock;

    public JwtTokenValidator(String expectedAudience) {
        this(expectedAudience, Clock.systemUTC());
    }

    JwtTokenValidator(String expectedAudience, Clock clock) {
        if (expectedAudience == null || expectedAudience.isBlank()) {
            throw new IllegalArgumentException("expectedAudience required");
        }
        this.expectedAudience = expectedAudience;
        this.clock = clock;
    }

    @Override
    public Principal validate(String token) throws AuthorizationException {
        if (token == null || token.isBlank()) {
            throw new AuthorizationException("token required");
        }
        String[] parts = token.split("\\.");
        if (parts.length < 2) {
            throw new AuthorizationException("malformed token");
        }
        String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        JsonObject payload;
        try (JsonReader reader = Json.createReader(new StringReader(payloadJson))) {
            payload = reader.readObject();
        } catch (Exception e) {
            throw new AuthorizationException("invalid token payload");
        }
        if (!expectedAudience.equals(payload.getString("aud", null))) {
            throw new AuthorizationException("invalid audience");
        }
        if (payload.containsKey("exp")) {
            long exp = payload.getJsonNumber("exp").longValue();
            if (exp * 1000L <= clock.millis()) {
                throw new AuthorizationException("token expired");
            }
        }
        String sub = payload.getString("sub", null);
        if (sub == null || sub.isBlank()) {
            throw new AuthorizationException("subject required");
        }
        Set<String> scopes = Set.of();
        String scope = payload.getString("scope", null);
        if (scope != null && !scope.isBlank()) {
            scopes = Set.of(scope.split("\\s+"));
        }
        return new Principal(sub, scopes);
    }
}
