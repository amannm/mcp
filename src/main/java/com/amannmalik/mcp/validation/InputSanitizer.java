package com.amannmalik.mcp.validation;

import java.util.HashMap;
import java.util.Map;

public final class InputSanitizer {
    private InputSanitizer() {
    }

    public static boolean isVisibleAscii(String value) {
        if (value == null) return false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c < 0x21 || c > 0x7E) return false;
        }
        return true;
    }

    public static String requireClean(String value) {
        if (value == null) throw new IllegalArgumentException("value is required");
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
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
}
