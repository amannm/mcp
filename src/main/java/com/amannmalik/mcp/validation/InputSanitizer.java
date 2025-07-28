package com.amannmalik.mcp.validation;

/** Basic input sanitization helpers. */
public final class InputSanitizer {
    private InputSanitizer() {}

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
}
