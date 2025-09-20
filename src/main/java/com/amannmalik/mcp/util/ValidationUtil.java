package com.amannmalik.mcp.util;

import jakarta.json.JsonObject;

import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

public final class ValidationUtil {
    private static final Pattern LABEL =
            Pattern.compile("[A-Za-z](?:[A-Za-z0-9-]*[A-Za-z0-9])?");
    private static final Pattern NAME =
            Pattern.compile("(?:[A-Za-z0-9](?:[A-Za-z0-9._-]*[A-Za-z0-9])?)?");
    private static final Set<String> RESERVED_META_PREFIXES = Set.of("modelcontextprotocol", "mcp");

    private ValidationUtil() {
    }

    public static boolean containsNonVisibleAscii(String value) {
        if (value == null) {
            return true;
        }
        for (var i = 0; i < value.length(); i++) {
            var c = value.charAt(i);
            if (c < 0x21 || c > 0x7E) {
                return true;
            }
        }
        return false;
    }

    public static String requireClean(String value) {
        if (value == null) {
            throw new IllegalArgumentException("value is required");
        }
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
        if (map == null || map.isEmpty()) {
            return Map.of();
        }
        var sanitized = new HashMap<String, String>(map.size());
        map.forEach((k, v) -> sanitized.put(requireClean(k), requireClean(v)));
        return Map.copyOf(sanitized);
    }

    public static void requireMeta(String key) {
        Objects.requireNonNull(key, "key required");
        var slash = key.indexOf('/');
        if (slash == 0) {
            throw new IllegalArgumentException("_meta prefix must not be empty: " + key);
        }
        if (slash >= 0 && key.indexOf('/', slash + 1) >= 0) {
            throw new IllegalArgumentException("_meta key may contain at most one '/' character: " + key);
        }

        var prefix = slash >= 0 ? key.substring(0, slash) : null;
        var name = slash >= 0 ? key.substring(slash + 1) : key;

        if (prefix != null) {
            validateMetaPrefix(prefix, key);
        }
        validateMetaName(name, key);
    }

    public static void requireMeta(JsonObject obj) {
        if (obj == null) {
            return;
        }
        for (var key : obj.keySet()) {
            requireMeta(key);
        }
    }

    public static URI requireAbsoluteUri(URI uri) {
        if (uri == null) {
            throw new IllegalArgumentException("uri is required");
        }
        URI parsed;
        try {
            parsed = uri.normalize();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid URI: " + uri, e);
        }
        if (!parsed.isAbsolute()) {
            throw new IllegalArgumentException("URI must be absolute: " + uri);
        }
        if (parsed.getFragment() != null) {
            throw new IllegalArgumentException("URI must not contain fragment: " + uri);
        }
        return parsed;
    }

    public static URI requireFileUri(URI uri) {
        var normalized = requireAbsoluteUri(uri);
        if (!normalized.getScheme().equals("file")) {
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
        if (origin == null || origin.isBlank()) {
            return false;
        }
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
        if (template == null) {
            throw new IllegalArgumentException("uriTemplate is required");
        }
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
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
            }
            if (depth < 0) {
                throw new IllegalArgumentException("Unmatched braces in URI template: " + template);
            }
        }
        if (depth != 0) {
            throw new IllegalArgumentException("Unmatched braces in URI template: " + template);
        }
    }

    public static Duration requirePositive(Duration value, String field) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(field + " must be positive");
        }
        return value;
    }

    public static int requirePositive(int value, String field) {
        if (value <= 0) {
            throw new IllegalArgumentException(field + " must be > 0");
        }
        return value;
    }

    public static long requirePositive(long value, String field) {
        if (value <= 0) {
            throw new IllegalArgumentException(field + " must be > 0");
        }
        return value;
    }

    public static double requirePositive(double value, String field) {
        if (Double.isNaN(value) || Double.isInfinite(value) || value <= 0.0) {
            throw new IllegalArgumentException(field + " must be > 0 and finite");
        }
        return value;
    }

    public static int requireNonNegative(int value, String field) {
        if (value < 0) {
            throw new IllegalArgumentException(field + " must be >= 0");
        }
        return value;
    }

    public static long requireNonNegative(long value, String field) {
        if (value < 0) {
            throw new IllegalArgumentException(field + " must be >= 0");
        }
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

    private static void validateMetaPrefix(String prefix, String key) {
        var labels = prefix.split("\\.");
        for (var i = 0; i < labels.length; i++) {
            var label = labels[i];
            if (!LABEL.matcher(label).matches()) {
                throw new IllegalArgumentException("Invalid _meta prefix: " + key);
            }
            if (i < labels.length - 1 && RESERVED_META_PREFIXES.contains(label)) {
                throw new IllegalArgumentException("Reserved _meta prefix: " + key);
            }
        }
    }

    private static void validateMetaName(String name, String key) {
        if (!NAME.matcher(name).matches()) {
            throw new IllegalArgumentException("Invalid _meta name: " + key);
        }
    }
}
