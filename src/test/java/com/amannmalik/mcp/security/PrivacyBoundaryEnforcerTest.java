package com.amannmalik.mcp.security;

import com.amannmalik.mcp.auth.Principal;
import com.amannmalik.mcp.server.resources.Audience;
import com.amannmalik.mcp.server.resources.ResourceAnnotations;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PrivacyBoundaryEnforcerTest {
    @Test
    void enforcesAudience() {
        PrivacyBoundaryEnforcer e = new PrivacyBoundaryEnforcer();
        e.allow("u", Audience.USER);
        Principal p = new Principal("u", Set.of());
        ResourceAnnotations userAnn = new ResourceAnnotations(EnumSet.of(Audience.USER), null, Instant.now());
        ResourceAnnotations assistAnn = new ResourceAnnotations(EnumSet.of(Audience.ASSISTANT), null, null);
        assertDoesNotThrow(() -> e.requireAllowed(p, userAnn));
        assertThrows(SecurityException.class, () -> e.requireAllowed(p, assistAnn));
    }
}
