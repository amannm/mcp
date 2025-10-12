package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.spi.internal.CursorCodec;

public sealed interface Cursor permits Cursor.Start, Cursor.End, Cursor.Token {
    static Cursor of(String value) {
        return value == null ? End.INSTANCE : new Token(value);
    }

    static Cursor.Token fromIndex(int index) {
        return CursorCodec.fromIndex(index);
    }

    static int index(Cursor cursor) {
        return CursorCodec.index(cursor);
    }

    static String requireValid(String token) {
        return CursorCodec.requireValid(token);
    }

    enum Start implements Cursor {
        INSTANCE
    }

    enum End implements Cursor {
        INSTANCE
    }

    record Token(String value) implements Cursor {
        public Token {
            value = CursorCodec.sanitizeToken(value);
        }
    }
}
