package com.amannmalik.mcp.lifecycle;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ProtocolLifecycleTest {
    @Test
    void selectsLatestSupportedVersionWhenRequestedUnsupported() {
        ProtocolLifecycle lifecycle = new ProtocolLifecycle(
                EnumSet.noneOf(ServerCapability.class),
                new ServerInfo("S", "Server", "1"),
                "",
                Set.of("2024-11-05", "2025-06-18"));
        InitializeRequest req = new InitializeRequest(
                "2026-01-01",
                new Capabilities(Set.of(), Set.of(), Map.of(), Map.of()),
                new ClientInfo("c", "Client", "1"),
                ClientFeatures.EMPTY
        );
        InitializeResponse res = lifecycle.initialize(req);
        assertEquals("2025-06-18", res.protocolVersion());
    }
}
