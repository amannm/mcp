package com.amannmalik.mcp.schema.core;

import java.util.Objects;

/** Common validation helpers for core value objects. */
final class FieldValidations {
    private FieldValidations() {}

    static String requireName(String value) {
        return requireNotBlank("name", value);
    }

    static String requireNotBlank(String field, String value) {
        Objects.requireNonNull(value, field + " cannot be null");
        if (value.isBlank()) throw new IllegalArgumentException(field + " cannot be blank");
        return value;
    }
}
