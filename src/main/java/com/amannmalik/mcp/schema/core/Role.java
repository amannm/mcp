package com.amannmalik.mcp.schema.core;

/** Sender or recipient role in a conversation. */
public enum Role {
    USER("user"),
    ASSISTANT("assistant");

    private final String value;

    Role(String value) { this.value = value; }

    @Override
    public String toString() { return value; }
}
