package com.amannmalik.mcp;

import com.amannmalik.mcp.validation.MetaValidator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MetaValidatorTest {
    @Test
    void reservedPrefixRejected() {
        assertThrows(IllegalArgumentException.class, () -> MetaValidator.requireValid("mcp.dev/foo"));
        assertThrows(IllegalArgumentException.class, () -> MetaValidator.requireValid("api.modelcontextprotocol.org/bar"));
    }

    @Test
    void nonReservedPrefixAccepted() {
        assertDoesNotThrow(() -> MetaValidator.requireValid("mcp/foo"));
        assertDoesNotThrow(() -> MetaValidator.requireValid("example.com/foo"));
    }
}
