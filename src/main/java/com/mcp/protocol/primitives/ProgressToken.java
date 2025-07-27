package com.mcp.protocol.primitives;

import java.util.Objects;

public sealed interface ProgressToken permits ProgressToken.StringToken, ProgressToken.IntegerToken {
    record StringToken(String value) implements ProgressToken {
        public StringToken {
            Objects.requireNonNull(value);
        }
    }
    record IntegerToken(long value) implements ProgressToken {}
}
