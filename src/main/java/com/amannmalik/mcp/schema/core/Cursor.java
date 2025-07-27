package com.amannmalik.mcp.schema.core;

/** Opaque pagination token. */
public record Cursor(String value) {
    public Cursor {
        FieldValidations.requireNotBlank("cursor", value);
    }

    @Override
    public String toString() { return value; }
}
