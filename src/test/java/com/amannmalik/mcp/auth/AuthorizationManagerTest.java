package com.amannmalik.mcp.auth;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AuthorizationManagerTest {
    @Test
    void choosesStrategy() throws Exception {
        AuthorizationStrategy skip = header -> Optional.empty();
        AuthorizationStrategy bearer = new BearerTokenAuthorizationStrategy(token -> new Principal(token, Set.of()));
        AuthorizationManager manager = new AuthorizationManager(List.of(skip, bearer));
        Principal p = manager.authorize("Bearer t");
        assertEquals("t", p.id());
    }

    @Test
    void failsWhenNoneMatch() {
        AuthorizationStrategy skip = header -> Optional.empty();
        AuthorizationManager manager = new AuthorizationManager(List.of(skip));
        assertThrows(AuthorizationException.class, () -> manager.authorize("Bearer x"));
    }
}
