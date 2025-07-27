package com.amannmalik.mcp.lifecycle;

import java.util.EnumSet;
import java.util.Set;

public class ProtocolLifecycle {
    public static final String SUPPORTED_VERSION = "2025-06-18";

    private final Set<ServerCapability> serverCapabilities;
    private LifecycleState state = LifecycleState.INIT;
    private Set<ClientCapability> clientCapabilities = Set.of();

    public ProtocolLifecycle(Set<ServerCapability> serverCapabilities) {
        this.serverCapabilities = EnumSet.copyOf(serverCapabilities);
    }

    public InitializeResponse initialize(InitializeRequest request) {
        ensureState(LifecycleState.INIT);

        if (!SUPPORTED_VERSION.equals(request.protocolVersion())) {
            throw new UnsupportedProtocolVersionException(request.protocolVersion(), SUPPORTED_VERSION);
        }

        Set<ClientCapability> requested = request.capabilities().client();
        clientCapabilities = requested.isEmpty() ? EnumSet.noneOf(ClientCapability.class) : EnumSet.copyOf(requested);

        return new InitializeResponse(
                SUPPORTED_VERSION,
                new Capabilities(clientCapabilities, serverCapabilities),
                new ServerInfo("mcp-java", "MCP Java Reference", "0.1.0"),
                null
        );
    }

    public void initialized() {
        ensureState(LifecycleState.INIT);
        state = LifecycleState.OPERATION;
    }

    public void shutdown() {
        state = LifecycleState.SHUTDOWN;
    }

    public LifecycleState state() {
        return state;
    }

    public Set<ClientCapability> negotiatedClientCapabilities() {
        return clientCapabilities;
    }

    public Set<ServerCapability> serverCapabilities() {
        return serverCapabilities;
    }

    private void ensureState(LifecycleState expected) {
        if (state != expected) {
            throw new IllegalStateException("Invalid lifecycle state: " + state);
        }
    }
}
