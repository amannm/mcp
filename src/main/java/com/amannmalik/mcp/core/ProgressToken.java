package com.amannmalik.mcp.core;

import com.amannmalik.mcp.util.ValidationUtil;

public sealed interface ProgressToken permits ProgressToken.StringToken, ProgressToken.NumericToken {
    String asString();

    record StringToken(String value) implements ProgressToken {
        public StringToken {
            value = ValidationUtil.requireClean(value);
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
