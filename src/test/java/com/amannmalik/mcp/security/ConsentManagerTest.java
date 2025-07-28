package com.amannmalik.mcp.security;

import com.amannmalik.mcp.auth.Principal;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ConsentManagerTest {
    @Test
    void grantsAndChecksConsent() {
        ConsentManager m = new ConsentManager();
        Principal p = new Principal("u", Set.of());
        m.grant("u", "file1");
        assertDoesNotThrow(() -> m.requireConsent(p, "file1"));
        assertThrows(SecurityException.class, () -> m.requireConsent(p, "file2"));
    }
}
