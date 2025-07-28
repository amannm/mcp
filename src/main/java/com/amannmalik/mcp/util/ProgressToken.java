package com.amannmalik.mcp.util;

/** Identifier for progress tracking. */
public sealed interface ProgressToken permits ProgressToken.StringToken, ProgressToken.NumericToken {
    record StringToken(String value) implements ProgressToken {}
    record NumericToken(long value) implements ProgressToken {}
}
