package com.amannmalik.mcp.lifecycle;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;


public class ProtocolLifecycle {

    private final Set<ServerCapability> serverCapabilities;
    private final ServerInfo serverInfo;
    private final String instructions;
    private String protocolVersion = Protocol.LATEST_VERSION;
    private LifecycleState state = LifecycleState.INIT;
    private Set<ClientCapability> clientCapabilities = Set.of();
    private ClientFeatures clientFeatures = ClientFeatures.EMPTY;

    public ProtocolLifecycle(Set<ServerCapability> serverCapabilities, ServerInfo serverInfo, String instructions) {
        this.serverCapabilities = EnumSet.copyOf(serverCapabilities);
        this.serverInfo = serverInfo;
        this.instructions = instructions;
    }

    public InitializeResponse initialize(InitializeRequest request) {
        ensureState(LifecycleState.INIT);
        Set<ClientCapability> requested = request.capabilities().client();
        clientCapabilities = requested.isEmpty()
                ? EnumSet.noneOf(ClientCapability.class)
                : EnumSet.copyOf(requested);
        clientFeatures = request.features() == null ? ClientFeatures.EMPTY : request.features();

        if (request.protocolVersion() != null && request.protocolVersion().equals(Protocol.LATEST_VERSION)) {
            protocolVersion = request.protocolVersion();
        } else {
            protocolVersion = Protocol.LATEST_VERSION;
        }

        return new InitializeResponse(
                protocolVersion,
                new Capabilities(clientCapabilities, serverCapabilities, Map.of(), Map.of()),
                serverInfo,
                instructions,
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

    public ClientFeatures clientFeatures() {
        return clientFeatures;
    }

    public Set<ServerCapability> serverCapabilities() {
        return serverCapabilities;
    }

    public String protocolVersion() {
        return protocolVersion;
    }

    private void ensureState(LifecycleState expected) {
        if (state != expected) {
            throw new IllegalStateException("Invalid lifecycle state: " + state);
        }
    }
}
