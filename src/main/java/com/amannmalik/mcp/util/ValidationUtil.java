package com.amannmalik.mcp.util;

import jakarta.json.*;

import java.net.URI;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.regex.Pattern;

public final class ValidationUtil {
    private static final Pattern LABEL =
            Pattern.compile("[A-Za-z](?:[A-Za-z0-9-]*[A-Za-z0-9])?");
    private static final Pattern NAME =
            Pattern.compile("(?:[A-Za-z0-9](?:[A-Za-z0-9._-]*[A-Za-z0-9])?)?");

    private ValidationUtil() {
    }

    public static boolean containsNonVisibleAscii(String value) {
        if (value == null) return true;
        for (var i = 0; i < value.length(); i++) {
            var c = value.charAt(i);
            if (c < 0x21 || c > 0x7E) return true;
        }
        return false;
    }

    public static String requireClean(String value) {
        if (value == null) throw new IllegalArgumentException("value is required");
        for (var i = 0; i < value.length(); i++) {
            var c = value.charAt(i);
            if (c < 0x20 && c != '\n' && c != '\r' && c != '\t') {
                throw new IllegalArgumentException("Control characters not allowed");
            }
        }
        return value;
    }

    public static String cleanNullable(String value) {
        return value == null ? null : requireClean(value);
    }

    public static String requireNonBlank(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("value required");
        }
        return requireClean(value);
    }

    public static Map<String, String> requireCleanMap(Map<String, String> map) {
        if (map == null || map.isEmpty()) return Map.of();
        Map<String, String> copy = new HashMap<>();
        map.forEach((k, v) -> copy.put(requireClean(k), requireClean(v)));
        return Map.copyOf(copy);
    }

    public static void requireMeta(String key) {
        if (key == null) throw new IllegalArgumentException("key required");
        var slash = key.indexOf('/');
        var prefix = slash >= 0 ? key.substring(0, slash) : null;
        var name = slash >= 0 ? key.substring(slash + 1) : key;

        if (slash == 0) {
            throw new IllegalArgumentException("_meta prefix must not be empty: " + key);
        }
        if (slash >= 0 && key.indexOf('/', slash + 1) >= 0) {
            throw new IllegalArgumentException("_meta key may contain at most one '/' character: " + key);
        }

        if (prefix != null) {
            var labels = prefix.split("\\.");
            for (var i = 0; i < labels.length; i++) {
                var label = labels[i];
                if (!LABEL.matcher(label).matches()) {
                    throw new IllegalArgumentException("Invalid _meta prefix: " + key);
                }
                if (i < labels.length - 1 && (label.equals("modelcontextprotocol") || label.equals("mcp"))) {
                    throw new IllegalArgumentException("Reserved _meta prefix: " + key);
                }
            }
        }

        if (!NAME.matcher(name).matches()) {
            throw new IllegalArgumentException("Invalid _meta name: " + key);
        }
    }

    public static void requireMeta(JsonObject obj) {
        if (obj == null) return;
        for (var key : obj.keySet()) requireMeta(key);
    }

    public static String requireAbsoluteUri(String uri) {
        if (uri == null) throw new IllegalArgumentException("uri is required");
        URI parsed;
        try {
            parsed = URI.create(uri).normalize();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid URI: " + uri, e);
        }
        if (!parsed.isAbsolute()) {
            throw new IllegalArgumentException("URI must be absolute: " + uri);
        }
        if (parsed.getFragment() != null) {
            throw new IllegalArgumentException("URI must not contain fragment: " + uri);
        }
        return parsed.toString();
    }

    public static String requireFileUri(String uri) {
        var normalized = requireAbsoluteUri(uri);
        if (!normalized.startsWith("file:")) {
            throw new IllegalArgumentException("URI must start with file:");
        }
        return normalized;
    }

    public static Set<String> requireAllowedOrigins(Set<String> origins) {
        if (origins == null || origins.isEmpty()) {
            throw new IllegalArgumentException("allowedOrigins required");
        }
        return Set.copyOf(origins);
    }

    public static boolean isAllowedOrigin(String origin, Set<String> allowedOrigins) {
        if (origin == null || origin.isBlank()) return false;
        URI parsed;
        try {
            parsed = URI.create(origin).normalize();
        } catch (Exception e) {
            return false;
        }
        var norm = parsed.getScheme() + "://" + parsed.getAuthority();
        return allowedOrigins.contains(norm);
    }

    public static void requireAllowedOrigin(String origin, Set<String> allowedOrigins) {
        if (!isAllowedOrigin(origin, allowedOrigins)) {
            throw new SecurityException("Invalid origin: " + origin);
        }
    }

    public static String requireAbsoluteTemplate(String template) {
        if (template == null) throw new IllegalArgumentException("uriTemplate is required");
        checkBraces(template);
        var replaced = template.replaceAll("\\{[^}]*}", "x");
        URI parsed;
        try {
            parsed = URI.create(replaced).normalize();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid URI template: " + template, e);
        }
        if (!parsed.isAbsolute()) {
            throw new IllegalArgumentException("URI template must be absolute: " + template);
        }
        if (parsed.getFragment() != null) {
            throw new IllegalArgumentException("URI template must not contain fragment: " + template);
        }
        return template;
    }

    private static void checkBraces(String template) {
        var depth = 0;
        for (var i = 0; i < template.length(); i++) {
            var c = template.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') depth--;
            if (depth < 0) throw new IllegalArgumentException("Unmatched braces in URI template: " + template);
        }
        if (depth != 0) throw new IllegalArgumentException("Unmatched braces in URI template: " + template);
    }

    public static int requirePositive(int value, String field) {
        if (value <= 0) throw new IllegalArgumentException(field + " must be > 0");
        return value;
    }

    public static long requirePositive(long value, String field) {
        if (value <= 0) throw new IllegalArgumentException(field + " must be > 0");
        return value;
    }

    public static double requirePositive(double value, String field) {
        if (Double.isNaN(value) || Double.isInfinite(value) || value <= 0.0) {
            throw new IllegalArgumentException(field + " must be > 0 and finite");
        }
        return value;
    }

    public static int requireNonNegative(int value, String field) {
        if (value < 0) throw new IllegalArgumentException(field + " must be >= 0");
        return value;
    }

    public static long requireNonNegative(long value, String field) {
        if (value < 0) throw new IllegalArgumentException(field + " must be >= 0");
        return value;
    }

    public static double requireNonNegative(double value, String field) {
        if (Double.isNaN(value) || Double.isInfinite(value) || value < 0.0) {
            throw new IllegalArgumentException(field + " must be >= 0 and finite");
        }
        return value;
    }

    public static double requireFraction(double value, String field) {
        if (Double.isNaN(value) || Double.isInfinite(value) || value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException(field + " must be between 0.0 and 1.0");
        }
        return value;
    }

    public static void validateSchema(JsonObject schema, JsonObject value) {
        if (schema == null) return;
        if (!"object".equals(schema.getString("type", null))) return;
        validateObject(schema, value);
    }

    private static void validateObject(JsonObject schema, JsonObject value) {
        var required = schema.getJsonArray("required");
        if (required != null) {
            for (var r : required.getValuesAs(JsonString.class)) {
                if (!value.containsKey(r.getString())) {
                    throw new IllegalArgumentException("Missing required field: " + r.getString());
                }
            }
        }
        var props = schema.getJsonObject("properties");
        if (props == null) return;
        for (var e : props.entrySet()) {
            var name = e.getKey();
            if (!value.containsKey(name)) continue;
            var prop = e.getValue().asJsonObject();
            var v = value.get(name);
            validateProperty(name, prop, v);
        }
        for (var key : value.keySet()) {
            if (!props.containsKey(key)) {
                throw new IllegalArgumentException("Unexpected field: " + key);
            }
        }
    }

    private static void validateProperty(String name, JsonObject schema, JsonValue value) {
        var t = schema.getString("type", null);
        if (t == null) return;
        switch (t) {
            case "string" -> validateString(name, value, schema);
            case "number" -> validateNumber(name, value, schema);
            case "integer" -> validateInteger(name, value, schema);
            case "boolean" -> requireType(value, JsonValue.ValueType.TRUE, JsonValue.ValueType.FALSE, name);
            case "array" -> requireType(value, JsonValue.ValueType.ARRAY, name);
            case "object" -> {
                requireType(value, JsonValue.ValueType.OBJECT, name);
                validateObject(schema, value.asJsonObject());
            }
            default -> {
            }
        }
    }

    private static void validateString(String field, JsonValue v, JsonObject schema) {
        requireType(v, JsonValue.ValueType.STRING, field);
        var str = ((JsonString) v).getString();
        if (schema.containsKey("minLength") && str.length() < schema.getInt("minLength")) {
            throw new IllegalArgumentException("minLength violated for " + field);
        }
        if (schema.containsKey("maxLength") && str.length() > schema.getInt("maxLength")) {
            throw new IllegalArgumentException("maxLength violated for " + field);
        }
        if (schema.containsKey("enum")) {
            var match = schema.getJsonArray("enum")
                    .getValuesAs(JsonString.class)
                    .stream()
                    .anyMatch(js -> js.getString().equals(str));
            if (!match) {
                throw new IllegalArgumentException("enum violation for " + field);
            }
        }
        if (schema.containsKey("format")) {
            validateFormat(str, schema.getString("format"), field);
        }
    }

    private static void validateNumber(String field, JsonValue v, JsonObject schema) {
        requireType(v, JsonValue.ValueType.NUMBER, field);
        var num = ((JsonNumber) v).doubleValue();
        if (schema.containsKey("minimum") && num < schema.getJsonNumber("minimum").doubleValue()) {
            throw new IllegalArgumentException("minimum violated for " + field);
        }
        if (schema.containsKey("maximum") && num > schema.getJsonNumber("maximum").doubleValue()) {
            throw new IllegalArgumentException("maximum violated for " + field);
        }
    }

    private static void validateInteger(String field, JsonValue v, JsonObject schema) {
        requireInteger(v, field);
        var num = ((JsonNumber) v).longValue();
        if (schema.containsKey("minimum") && num < schema.getJsonNumber("minimum").longValue()) {
            throw new IllegalArgumentException("minimum violated for " + field);
        }
        if (schema.containsKey("maximum") && num > schema.getJsonNumber("maximum").longValue()) {
            throw new IllegalArgumentException("maximum violated for " + field);
        }
    }

    private static void validateFormat(String value, String format, String field) {
        try {
            switch (format) {
                case "email" -> {
                    if (!value.matches("^[^@]+@[^@]+\\.[^@]+$")) {
                        throw new IllegalArgumentException("Invalid email for " + field);
                    }
                }
                case "uri" -> {
                    if (URI.create(value).toString().isEmpty()) {
                        throw new IllegalArgumentException("Invalid uri for " + field);
                    }
                }
                case "date" -> LocalDate.parse(value);
                case "date-time" -> OffsetDateTime.parse(value);
                default -> {
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid " + format + " for " + field);
        }
    }

    private static void requireType(JsonValue value, JsonValue.ValueType expected, String field) {
        if (value.getValueType() != expected) {
            throw new IllegalArgumentException("Expected " + expected + " for " + field);
        }
    }

    private static void requireInteger(JsonValue value, String field) {
        if (value.getValueType() != JsonValue.ValueType.NUMBER
                || !((JsonNumber) value).isIntegral()) {
            throw new IllegalArgumentException("Expected integer for " + field);
        }
    }

    private static void requireType(JsonValue value, JsonValue.ValueType a, JsonValue.ValueType b, String field) {
        var vt = value.getValueType();
        if (vt != a && vt != b) {
            throw new IllegalArgumentException("Expected " + a + " or " + b + " for " + field);
        }
    }
}
