package com.amannmalik.mcp.lifecycle;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProtocolLifecycleTest {
    @Test
    void initializationNegotiatesSupportedVersion() {
        ProtocolLifecycle lifecycle = new ProtocolLifecycle(Set.of(ServerCapability.RESOURCES));
        InitializeRequest req = new InitializeRequest(
                "2024-01-01",
                new Capabilities(Set.of(), Set.of()),
                new ClientInfo("test", "Test", "1.0")
        );
        InitializeResponse resp = lifecycle.initialize(req);
        assertEquals(ProtocolLifecycle.SUPPORTED_VERSION, resp.protocolVersion());
    }
}
