package com.amannmalik.mcp.json;

import java.util.Base64;

public final class Base64Util {
    private Base64Util() {}

    public static String encode(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    public static byte[] decode(String value) {
        return Base64.getDecoder().decode(value);
    }
}
