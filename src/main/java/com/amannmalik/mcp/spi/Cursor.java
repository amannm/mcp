package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.util.ValidationUtil;

public sealed interface Cursor permits Cursor.End, Cursor.Token {
    static Cursor of(String value) {
        return value == null ? End.INSTANCE : new Token(value);
    }

    record Token(String value) implements Cursor {
        public Token {
            if (value == null) throw new IllegalArgumentException("value is required");
            value = ValidationUtil.requireClean(value);
        }
    }

    enum End implements Cursor {
        INSTANCE
    }
}
