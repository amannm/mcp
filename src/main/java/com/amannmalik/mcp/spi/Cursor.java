package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.util.ValidationUtil;

public sealed interface Cursor permits Cursor.Start, Cursor.End, Cursor.Token {
    static Cursor of(String value) {
        return value == null ? End.INSTANCE : new Token(value);
    }

    enum Start implements Cursor {
        INSTANCE
    }

    enum End implements Cursor {
        INSTANCE
    }

    record Token(String value) implements Cursor {
        public Token {
            if (value == null) throw new IllegalArgumentException("value is required");
            value = ValidationUtil.requireClean(value);
            Pagination.requireValidCursor(value);
        }
    }
}
