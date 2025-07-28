package com.amannmalik.mcp.security;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class OriginValidatorTest {
    @Test
    void validatesOrigins() {
        OriginValidator v = new OriginValidator(Set.of("http://localhost"));
        assertTrue(v.isValid(null));
        assertTrue(v.isValid("http://localhost"));
        assertFalse(v.isValid("http://evil.com"));
    }
}
