package com.amannmalik.mcp.util;

import com.amannmalik.mcp.validation.InputSanitizer;

public sealed interface ProgressToken permits ProgressToken.StringToken, ProgressToken.NumericToken {
    String asString();

    record StringToken(String value) implements ProgressToken {
        public StringToken {
            value = InputSanitizer.requireClean(value);
        }

        @Override
        public String asString() {
            return value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    record NumericToken(long value) implements ProgressToken {
        @Override
        public String asString() {
            return Long.toString(value);
        }

        @Override
        public String toString() {
            return Long.toString(value);
        }
    }
}
