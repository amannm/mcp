package com.amannmalik.mcp.schema.core;

import java.util.Objects;

/** Common validation helpers for core value objects. */
public final class FieldValidations {
    private FieldValidations() {}

    public static String requireName(String value) {
        return requireNotBlank("name", value);
    }

    public static String requireNotBlank(String field, String value) {
        Objects.requireNonNull(value, field + " cannot be null");
        if (value.isBlank()) throw new IllegalArgumentException(field + " cannot be blank");
        return value;
    }

    public static String requireMimeType(String value) {
        requireNotBlank("mimeType", value);
        if (!value.contains("/")) throw new IllegalArgumentException("invalid mimeType");
        return value;
    }
}
