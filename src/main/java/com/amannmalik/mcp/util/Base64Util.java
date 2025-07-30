package com.amannmalik.mcp.util;

import java.util.Base64;

public final class Base64Util {
    private Base64Util() {
    }

    public static byte[] decode(String value) {
        try {
            return Base64.getDecoder().decode(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid base64 data", e);
        }
    }

    public static String encode(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }
}
