package com.amannmalik.mcp.lifecycle;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.*;

class ProtocolLifecycleTest {
    @Test
    void successfulInitializationTransitionsToOperationAfterInitialized() {
        ProtocolLifecycle lifecycle = new ProtocolLifecycle(EnumSet.of(ServerCapability.PROMPTS));
        InitializeRequest request = new InitializeRequest(
                ProtocolLifecycle.SUPPORTED_VERSION,
                new Capabilities(EnumSet.of(ClientCapability.ROOTS), EnumSet.noneOf(ServerCapability.class)),
                new ClientInfo("client", "Client", "1")
        );
        InitializeResponse response = lifecycle.initialize(request);
        assertEquals(ProtocolLifecycle.SUPPORTED_VERSION, response.protocolVersion());
        assertEquals(LifecycleState.INIT, lifecycle.state());
        lifecycle.initialized();
        assertEquals(LifecycleState.OPERATION, lifecycle.state());
    }

    @Test
    void unsupportedVersionThrowsException() {
        ProtocolLifecycle lifecycle = new ProtocolLifecycle(EnumSet.noneOf(ServerCapability.class));
        InitializeRequest request = new InitializeRequest(
                "2024-11-05",
                new Capabilities(EnumSet.noneOf(ClientCapability.class), EnumSet.noneOf(ServerCapability.class)),
                new ClientInfo("client", "Client", "1")
        );
        assertThrows(UnsupportedProtocolVersionException.class, () -> lifecycle.initialize(request));
    }

    @Test
    void shutdownAlwaysAllowed() {
        ProtocolLifecycle lifecycle = new ProtocolLifecycle(EnumSet.noneOf(ServerCapability.class));
        lifecycle.shutdown();
        assertEquals(LifecycleState.SHUTDOWN, lifecycle.state());
    }
}
