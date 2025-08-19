package com.amannmalik.mcp.util;

import com.amannmalik.mcp.spi.Cursor;

public final class CursorUtil {
    private CursorUtil() {
    }

    public static Cursor sanitize(String cursor) {
        if (cursor == null) return Cursor.Start.INSTANCE;
        var clean = ValidationUtil.cleanNullable(cursor);
        return new Cursor.Token(clean);
    }
}
