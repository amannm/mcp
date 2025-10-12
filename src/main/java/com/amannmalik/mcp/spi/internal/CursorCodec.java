package com.amannmalik.mcp.spi.internal;

import com.amannmalik.mcp.spi.Cursor;
import com.amannmalik.mcp.util.Base64Util;
import com.amannmalik.mcp.util.ValidationUtil;

import java.nio.charset.StandardCharsets;

public final class CursorCodec {
    private CursorCodec() {
    }

    public static Cursor.Token fromIndex(int index) {
        if (index < 0) {
            throw new IllegalArgumentException("index must be >= 0");
        }
        return new Cursor.Token(encode(index));
    }

    public static int index(Cursor cursor) {
        return switch (cursor) {
            case null -> 0;
            case Cursor.Start ignored -> 0;
            case Cursor.Token(var value) -> decode(value);
            case Cursor.End ignored -> throw new IllegalArgumentException("Invalid cursor");
        };
    }

    public static String requireValid(String token) {
        decode(token);
        return token;
    }

    public static Cursor requireCursor(Cursor cursor) {
        return cursor == null ? Cursor.End.INSTANCE : cursor;
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
            throw new IllegalArgumentException("Invalid cursor", e);
        }
    }

    public static String sanitizeToken(String value) {
        ValidationUtil.requireClean(value);
        return requireValid(value);
    }
}
