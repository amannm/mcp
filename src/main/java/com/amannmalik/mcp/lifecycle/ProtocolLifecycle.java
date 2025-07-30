package com.amannmalik.mcp.lifecycle;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;


public class ProtocolLifecycle {
    public static final String SUPPORTED_VERSION = "2025-06-18";
    /**
     * The most recent prior revision that implementations should fall back to
     * when no protocol version is negotiated.
     */
    public static final String PREVIOUS_VERSION = "2025-03-26";

    private final Set<ServerCapability> serverCapabilities;
    private final ServerInfo serverInfo;
    private final String instructions;
    private String protocolVersion = SUPPORTED_VERSION;
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

        var versionRequested = request.protocolVersion();
        if (SUPPORTED_VERSION.equals(versionRequested) || PREVIOUS_VERSION.equals(versionRequested)) {
            protocolVersion = versionRequested;
        } else {
            protocolVersion = SUPPORTED_VERSION;
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
