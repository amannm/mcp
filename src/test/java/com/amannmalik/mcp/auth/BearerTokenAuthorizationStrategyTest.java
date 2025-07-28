package com.amannmalik.mcp.auth;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BearerTokenAuthorizationStrategyTest {
    @Test
    void parsesBearerToken() throws Exception {
        var strategy = new BearerTokenAuthorizationStrategy(token -> new Principal(token, Set.of()));
        var result = strategy.authorize("Bearer abc").orElseThrow();
        assertEquals("abc", result.id());
    }

    @Test
    void rejectsMalformedHeader() {
        var strategy = new BearerTokenAuthorizationStrategy(token -> new Principal(token, Set.of()));
        assertThrows(AuthorizationException.class, () -> strategy.authorize("Bearer"));
        assertThrows(AuthorizationException.class, () -> strategy.authorize("Bearer "));
    }

    @Test
    void ignoresUnsupportedHeader() throws Exception {
        var strategy = new BearerTokenAuthorizationStrategy(token -> { throw new AuthorizationException("should not" ); });
        assertTrue(strategy.authorize("Basic aaa").isEmpty());
        assertTrue(strategy.authorize(null).isEmpty());
    }
}
