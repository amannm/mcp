package com.amannmalik.mcp.validation;

public final class InputSanitizer {
    private InputSanitizer() {
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

    /**
     * Sanitizes optional input. Returns {@code null} if {@code value} is null,
     * otherwise delegates to {@link #requireClean(String)}.
     */
    public static String cleanNullable(String value) {
        return value == null ? null : requireClean(value);
    }
}
