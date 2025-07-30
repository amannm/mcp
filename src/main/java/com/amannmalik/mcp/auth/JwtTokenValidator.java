package com.amannmalik.mcp.auth;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;

import java.io.StringReader;
import com.amannmalik.mcp.util.Base64Util;
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
            payloadJson = new String(Base64Util.decodeUrl(parts[1]));
        } catch (IllegalArgumentException e) {
            throw new AuthorizationException("invalid token encoding");
        }
        JsonObject payload;
        try (JsonReader reader = Json.createReader(new StringReader(payloadJson))) {
            payload = reader.readObject();
        } catch (Exception e) {
            throw new AuthorizationException("invalid token payload");
        }
        boolean audOk = false;
        String aud = payload.getString("aud", null);
        if (aud != null) audOk = expectedAudience.equals(aud);
        if (!audOk) {
            JsonArray arr = payload.getJsonArray("aud");
            if (arr != null) {
                for (var js : arr.getValuesAs(jakarta.json.JsonString.class)) {
                    if (expectedAudience.equals(js.getString())) {
                        audOk = true;
                        break;
                    }
                }
            }
        }
        if (!audOk) {
            throw new AuthorizationException("audience mismatch");
        }

        if (payload.containsKey("resource")) {
            boolean resourceOk = false;
            switch (payload.get("resource").getValueType()) {
                case STRING -> resourceOk = expectedAudience.equals(payload.getString("resource"));
                case ARRAY -> {
                    JsonArray arr = payload.getJsonArray("resource");
                    for (var js : arr.getValuesAs(jakarta.json.JsonString.class)) {
                        if (expectedAudience.equals(js.getString())) {
                            resourceOk = true;
                            break;
                        }
                    }
                }
                default -> {
                }
            }
            if (!resourceOk) {
                throw new AuthorizationException("resource mismatch");
            }
        }
        String sub = payload.getString("sub", null);
        if (sub == null || sub.isBlank()) {
            throw new AuthorizationException("subject required");
        }
        long now = System.currentTimeMillis() / 1000;
        if (payload.containsKey("exp") && payload.get("exp").getValueType() == jakarta.json.JsonValue.ValueType.NUMBER) {
            long exp = payload.getJsonNumber("exp").longValue();
            if (now >= exp) {
                throw new AuthorizationException("token expired");
            }
        }
        if (payload.containsKey("nbf") && payload.get("nbf").getValueType() == jakarta.json.JsonValue.ValueType.NUMBER) {
            long nbf = payload.getJsonNumber("nbf").longValue();
            if (now < nbf) {
                throw new AuthorizationException("token not active");
            }
        }
        Set<String> scopes = Set.of();
        var scopeStr = payload.getString("scope", null);
        if (scopeStr != null && !scopeStr.isBlank()) {
            scopes = Set.of(scopeStr.split(" "));
        }
        return new Principal(sub, scopes);
    }
}
