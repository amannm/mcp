package com.amannmalik.mcp.auth;

import com.amannmalik.mcp.util.Base64Util;
import com.amannmalik.mcp.validation.InputSanitizer;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.InvalidKeyException;

import java.io.StringReader;
import java.util.Set;

public final class JwtTokenValidator implements TokenValidator {
    private final String expectedAudience;
    private final byte[] secret;

    public JwtTokenValidator(String expectedAudience) {
        this(expectedAudience, null);
    }

    public JwtTokenValidator(String expectedAudience, byte[] secret) {
        this.expectedAudience = InputSanitizer.requireNonBlank(expectedAudience);
        this.secret = secret == null || secret.length == 0 ? null : secret.clone();
    }

    @Override
    public Principal validate(String token) throws AuthorizationException {
        JwtParts parts = decode(token);
        verifySignature(parts);
        JsonObject payload = parsePayload(parts.payloadJson());
        String subject = extractSubject(payload);
        validateAudience(payload);
        validateResource(payload);
        validateTimestamps(payload);
        Set<String> scopes = extractScopes(payload);
        return new Principal(subject, scopes);
    }

    private record JwtParts(String headerJson, String payloadJson, String signature) {
    }

    private JwtParts decode(String token) throws AuthorizationException {
        token = InputSanitizer.requireNonBlank(token);
        String[] parts = token.split("\\.");
        if (parts.length != 3) throw new AuthorizationException("invalid token format");
        try {
            String header = new String(Base64Util.decodeUrl(parts[0]));
            String payload = new String(Base64Util.decodeUrl(parts[1]));
            return new JwtParts(header, payload, parts[2]);
        } catch (IllegalArgumentException e) {
            throw new AuthorizationException("invalid token encoding");
        }
    }

    private void verifySignature(JwtParts parts) throws AuthorizationException {
        if (secret == null) return;
        JsonObject header;
        try (JsonReader hr = Json.createReader(new StringReader(parts.headerJson()))) {
            header = hr.readObject();
        } catch (Exception e) {
            throw new AuthorizationException("invalid token header");
        }
        String alg = header.getString("alg", null);
        if (!"HS256".equals(alg)) throw new AuthorizationException("unsupported alg");
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            byte[] expected = mac.doFinal((parts.headerJson() + "." + parts.payloadJson()).getBytes(StandardCharsets.US_ASCII));
            byte[] actual = Base64Util.decodeUrl(parts.signature());
            if (!MessageDigest.isEqual(expected, actual)) throw new AuthorizationException("invalid signature");
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new AuthorizationException("signature verification failed");
        }
    }

    private JsonObject parsePayload(String payloadJson) throws AuthorizationException {
        try (JsonReader reader = Json.createReader(new StringReader(payloadJson))) {
            return reader.readObject();
        } catch (Exception e) {
            throw new AuthorizationException("invalid token payload");
        }
    }

    private String extractSubject(JsonObject payload) throws AuthorizationException {
        String sub = payload.getString("sub", null);
        if (sub == null || sub.isBlank()) throw new AuthorizationException("subject required");
        return sub;
    }

    private void validateAudience(JsonObject payload) throws AuthorizationException {
        if (!matches(payload, "aud")) throw new AuthorizationException("audience mismatch");
    }

    private void validateResource(JsonObject payload) throws AuthorizationException {
        if (payload.containsKey("resource") && !matches(payload, "resource")) {
            throw new AuthorizationException("resource mismatch");
        }
    }

    private boolean matches(JsonObject payload, String key) {
        JsonValue val = payload.get(key);
        if (val == null) return false;
        return switch (val.getValueType()) {
            case STRING -> expectedAudience.equals(((JsonString) val).getString());
            case ARRAY -> {
                for (JsonString js : payload.getJsonArray(key).getValuesAs(JsonString.class)) {
                    if (expectedAudience.equals(js.getString())) yield true;
                }
                yield false;
            }
            default -> false;
        };
    }

    private void validateTimestamps(JsonObject payload) throws AuthorizationException {
        long now = System.currentTimeMillis() / 1000;
        if (payload.containsKey("exp") && payload.get("exp").getValueType() == jakarta.json.JsonValue.ValueType.NUMBER) {
            long exp = payload.getJsonNumber("exp").longValue();
            if (now >= exp) throw new AuthorizationException("token expired");
        }
        if (payload.containsKey("nbf") && payload.get("nbf").getValueType() == jakarta.json.JsonValue.ValueType.NUMBER) {
            long nbf = payload.getJsonNumber("nbf").longValue();
            if (now < nbf) throw new AuthorizationException("token not active");
        }
    }

    private Set<String> extractScopes(JsonObject payload) {
        var scopeStr = payload.getString("scope", null);
        return (scopeStr == null || scopeStr.isBlank()) ? Set.of() : Set.of(scopeStr.split(" "));
    }
}
