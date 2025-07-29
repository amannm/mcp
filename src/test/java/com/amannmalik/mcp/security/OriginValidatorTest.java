package com.amannmalik.mcp.security;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;

class OriginValidatorTest {
    @Test
    void testValidatesOriginHeaderPresence() {
        OriginValidator validator = new OriginValidator(Set.of("http://localhost"));
        assertFalse(validator.isValid(null));
        assertFalse(validator.isValid(""));
        assertTrue(validator.isValid("http://localhost"));
        assertFalse(validator.isValid("http://evil.com"));
    }

    @Test
    void testRequireValidThrows() {
        OriginValidator validator = new OriginValidator(Set.of("http://localhost"));
        assertThrows(SecurityException.class, () -> validator.requireValid("http://evil.com"));
    }
}
