package com.amannmalik.mcp.auth;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;

import java.io.StringReader;
import java.util.Base64;
import java.util.Set;

public final class JwtTokenValidator implements TokenValidator {
    private final String expectedAudience;

    public JwtTokenValidator(String expectedAudience) {
        if (expectedAudience == null || expectedAudience.isBlank()) {
            throw new IllegalArgumentException("expectedAudience required");
        }
        this.expectedAudience = expectedAudience;
    }

    @Override
    public Principal validate(String token) throws AuthorizationException {
        if (token == null || token.isBlank()) {
            throw new AuthorizationException("token required");
        }
        String[] parts = token.split("\\.");
        if (parts.length < 2) {
            throw new AuthorizationException("invalid token format");
        }
        String payloadJson;
        try {
            payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]));
        } catch (IllegalArgumentException e) {
            throw new AuthorizationException("invalid token encoding");
        }
        JsonObject payload;
        try (JsonReader reader = Json.createReader(new StringReader(payloadJson))) {
            payload = reader.readObject();
        } catch (Exception e) {
            throw new AuthorizationException("invalid token payload");
        }
        String aud = payload.getString("aud", null);
        if (!expectedAudience.equals(aud)) {
            throw new AuthorizationException("audience mismatch");
        }
        String sub = payload.getString("sub", null);
        if (sub == null || sub.isBlank()) {
            throw new AuthorizationException("subject required");
        }
        Set<String> scopes = Set.of();
        var scopeStr = payload.getString("scope", null);
        if (scopeStr != null && !scopeStr.isBlank()) {
            scopes = Set.of(scopeStr.split(" "));
        }
        return new Principal(sub, scopes);
    }
}
