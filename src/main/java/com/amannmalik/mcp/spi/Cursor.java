package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.util.Base64Util;
import com.amannmalik.mcp.util.ValidationUtil;

import java.nio.charset.StandardCharsets;

public sealed interface Cursor permits Cursor.Start, Cursor.End, Cursor.Token {
    static Cursor of(String value) {
        return value == null ? End.INSTANCE : new Token(value);
    }

    static Cursor.Token fromIndex(int index) {
        if (index < 0) {
            throw new IllegalArgumentException("index must be >= 0");
        }
        return new Token(encode(index));
    }

    static int index(Cursor cursor) {
        return switch (cursor) {
            case null -> 0;
            case Start ignored -> 0;
            case Token(var value) -> decode(value);
            case End ignored -> throw new IllegalArgumentException("Invalid cursor");
        };
    }

    static String requireValid(String token) {
        decode(token);
        return token;
    }

    private static String encode(int index) {
        var raw = Integer.toString(index);
        return Base64Util.encodeUrl(raw.getBytes(StandardCharsets.UTF_8));
    }

    private static int decode(String token) {
        if (token == null) {
            return 0;
        }
        try {
            var s = new String(Base64Util.decodeUrl(token), StandardCharsets.UTF_8);
            return Integer.parseInt(s);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid cursor");
        }
    }

    enum Start implements Cursor {
        INSTANCE
    }

    enum End implements Cursor {
        INSTANCE
    }

    record Token(String value) implements Cursor {
        public Token {
            if (value == null) {
                throw new IllegalArgumentException("value is required");
            }
            value = ValidationUtil.requireClean(value);
            Cursor.requireValid(value);
        }
    }
}
