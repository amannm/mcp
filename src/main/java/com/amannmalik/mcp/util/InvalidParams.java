package com.amannmalik.mcp.util;

import java.util.function.Supplier;

public final class InvalidParams extends RuntimeException {
    public InvalidParams(String message) { super(message); }

    public static <T> T valid(Supplier<T> s) {
        try {
            return s.get();
        } catch (IllegalArgumentException e) {
            throw new InvalidParams(e.getMessage());
        }
    }
}
