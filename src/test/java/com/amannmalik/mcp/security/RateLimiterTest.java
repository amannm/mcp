package com.amannmalik.mcp.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RateLimiterTest {
    @Test
    void limitsWithinWindow() throws Exception {
        RateLimiter r = new RateLimiter(1, 50);
        assertDoesNotThrow(() -> r.requireAllowance("t"));
        assertThrows(SecurityException.class, () -> r.requireAllowance("t"));
        Thread.sleep(60);
        assertDoesNotThrow(() -> r.requireAllowance("t"));
    }
}
