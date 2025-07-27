package com.mcp.protocol.primitives;

import java.util.Objects;

public record Cursor(String value) {
    public Cursor {
        Objects.requireNonNull(value);
    }
}
