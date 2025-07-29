package com.amannmalik.mcp.validation;

import jakarta.json.Json;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MetaValidatorTest {

    @Test
    void allowsValidKey() {
        MetaValidator.requireValid("foo/bar");
    }

    @Test
    void rejectsReservedPrefix() {
        assertThrows(IllegalArgumentException.class,
                () -> MetaValidator.requireValid("modelcontextprotocol.io/foo"));
        assertThrows(IllegalArgumentException.class,
                () -> MetaValidator.requireValid("api.modelcontextprotocol.org/bar"));
        assertThrows(IllegalArgumentException.class,
                () -> MetaValidator.requireValid("mcp.dev/baz"));
        assertThrows(IllegalArgumentException.class,
                () -> MetaValidator.requireValid("tools.mcp.com/thing"));
    }

    @Test
    void validatesJsonObject() {
        var obj = Json.createObjectBuilder()
                .add("foo/bar", 1)
                .add("baz", 2)
                .build();
        MetaValidator.requireValid(obj);
    }
}
