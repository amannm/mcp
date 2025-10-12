package com.amannmalik.mcp.api;

import com.amannmalik.mcp.util.ValidationUtil;

import java.math.BigInteger;

public sealed interface ProgressToken permits
        ProgressToken.StringToken,
        ProgressToken.NumericToken {
    record StringToken(String value) implements ProgressToken {
        public StringToken {
            value = ValidationUtil.requireNonBlank(value);
        }
    }

    record NumericToken(BigInteger value) implements ProgressToken {
    }
}
