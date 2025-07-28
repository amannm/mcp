package com.amannmalik.mcp.security;

import com.amannmalik.mcp.auth.Principal;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ToolAccessControllerTest {
    @Test
    void grantsAndChecksTool() {
        ToolAccessController c = new ToolAccessController();
        Principal p = new Principal("u", Set.of());
        c.allow("u", "build");
        assertDoesNotThrow(() -> c.requireAllowed(p, "build"));
        assertThrows(SecurityException.class, () -> c.requireAllowed(p, "test"));
    }
}
