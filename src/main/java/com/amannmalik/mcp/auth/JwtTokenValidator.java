package com.amannmalik.mcp.auth;

import com.amannmalik.mcp.spi.Principal;
import com.amannmalik.mcp.util.Base64Util;
import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Set;

public final class JwtTokenValidator implements TokenValidator {
    private final String expectedAudience;
    private final byte[] secret;

    public JwtTokenValidator(String expectedAudience) {
        this(expectedAudience, null);
    }

    public JwtTokenValidator(String expectedAudience, byte[] secret) {
        this.expectedAudience = ValidationUtil.requireNonBlank(expectedAudience);
        this.secret = secret == null || secret.length == 0 ? null : secret.clone();
    }

    @Override
    public Principal validate(String token) throws AuthorizationException {
        var parts = decode(token);
        verifySignature(parts);
        var payload = parsePayload(parts.payloadJson());
        var subject = extractSubject(payload);
        requireAudience(payload, "aud", false, "audience mismatch");
        requireAudience(payload, "resource", true, "resource mismatch");
        validateTimestamps(payload);
        var scopes = extractScopes(payload);
        return new Principal(subject, scopes);
    }

    private JwtParts decode(String token) throws AuthorizationException {
        token = ValidationUtil.requireNonBlank(token);
        var parts = token.split("\\.");
        if (parts.length != 3) {
            throw new AuthorizationException("invalid token format");
        }
        try {
            var header = new String(Base64Util.decodeUrl(parts[0]), StandardCharsets.UTF_8);
            var payload = new String(Base64Util.decodeUrl(parts[1]), StandardCharsets.UTF_8);
            return new JwtParts(header, payload, parts[2]);
        } catch (IllegalArgumentException e) {
            throw new AuthorizationException("invalid token encoding");
        }
    }

    private void verifySignature(JwtParts parts) throws AuthorizationException {
        if (secret == null) {
            return;
        }
        JsonObject header;
        try (var hr = Json.createReader(new StringReader(parts.headerJson()))) {
            header = hr.readObject();
        } catch (Exception e) {
            throw new AuthorizationException("invalid token header");
        }
        var alg = header.getString("alg", null);
        if (!"HS256".equals(alg)) {
            throw new AuthorizationException("unsupported alg");
        }
        try {
            var mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            var expected = mac.doFinal((parts.headerJson() + "." + parts.payloadJson()).getBytes(StandardCharsets.US_ASCII));
            var actual = Base64Util.decodeUrl(parts.signature());
            if (!MessageDigest.isEqual(expected, actual)) {
                throw new AuthorizationException("invalid signature");
            }
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new AuthorizationException("signature verification failed");
        }
    }

    private JsonObject parsePayload(String payloadJson) throws AuthorizationException {
        try (var reader = Json.createReader(new StringReader(payloadJson))) {
            return reader.readObject();
        } catch (Exception e) {
            throw new AuthorizationException("invalid token payload");
        }
    }

    private String extractSubject(JsonObject payload) throws AuthorizationException {
        var sub = payload.getString("sub", null);
        if (sub == null || sub.isBlank()) {
            throw new AuthorizationException("subject required");
        }
        return sub;
    }

    private void requireAudience(JsonObject payload, String key, boolean optional, String err)
            throws AuthorizationException {
        var val = payload.get(key);
        if (val == null) {
            if (!optional) {
                throw new AuthorizationException(err);
            }
            return;
        }
        if (mismatch(val)) {
            throw new AuthorizationException(err);
        }
    }

    private boolean mismatch(JsonValue val) {
        return switch (val.getValueType()) {
            case STRING -> !expectedAudience.equalsIgnoreCase(((JsonString) val).getString());
            case ARRAY -> val.asJsonArray()
                    .getValuesAs(JsonString.class)
                    .stream()
                    .noneMatch(js -> expectedAudience.equalsIgnoreCase(js.getString()));
            default -> true;
        };
    }

    private void validateTimestamps(JsonObject payload) throws AuthorizationException {
        var now = System.currentTimeMillis() / 1000;
        if (payload.containsKey("exp") && payload.get("exp").getValueType() == JsonValue.ValueType.NUMBER) {
            var exp = payload.getJsonNumber("exp").longValue();
            if (now >= exp) {
                throw new AuthorizationException("token expired");
            }
        }
        if (payload.containsKey("nbf") && payload.get("nbf").getValueType() == JsonValue.ValueType.NUMBER) {
            var nbf = payload.getJsonNumber("nbf").longValue();
            if (now < nbf) {
                throw new AuthorizationException("token not active");
            }
        }
    }

    private Set<String> extractScopes(JsonObject payload) {
        var scopeStr = payload.getString("scope", null);
        return (scopeStr == null || scopeStr.isBlank()) ? Set.of() : Set.of(scopeStr.split(" "));
    }

    private record JwtParts(String headerJson, String payloadJson, String signature) {
    }
}
