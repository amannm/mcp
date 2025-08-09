package com.amannmalik.mcp.lifecycle;

import java.util.*;

/// - [Lifecycle](specification/2025-06-18/basic/lifecycle.mdx)
/// - [Conformance Suite](src/test/resources/com/amannmalik/mcp/lifecycle.feature)
public class ProtocolLifecycle {

    private final Set<ServerCapability> serverCapabilities;
    private final ServerInfo serverInfo;
    private final String instructions;
    private final List<String> supportedVersions;
    private String protocolVersion;
    private LifecycleState state = LifecycleState.INIT;
    private Set<ClientCapability> clientCapabilities = Set.of();
    private ClientFeatures clientFeatures = ClientFeatures.EMPTY;

    public ProtocolLifecycle(Set<ServerCapability> serverCapabilities,
                             ServerInfo serverInfo,
                             String instructions) {
        this(serverCapabilities, serverInfo, instructions,
                Set.of(Protocol.LATEST_VERSION, Protocol.PREVIOUS_VERSION));
    }

    public ProtocolLifecycle(Set<ServerCapability> serverCapabilities,
                             ServerInfo serverInfo,
                             String instructions,
                             Set<String> supportedVersions) {
        this.serverCapabilities = EnumSet.copyOf(serverCapabilities);
        this.serverInfo = serverInfo;
        this.instructions = instructions;
        List<String> versions = new ArrayList<>(supportedVersions);
        versions.sort(Comparator.reverseOrder());
        this.supportedVersions = List.copyOf(versions);
        this.protocolVersion = this.supportedVersions.getFirst();
    }

    public InitializeResponse initialize(InitializeRequest request) {
        ensureState(LifecycleState.INIT);
        Set<ClientCapability> requested = request.capabilities().client();
        clientCapabilities = requested.isEmpty()
                ? EnumSet.noneOf(ClientCapability.class)
                : EnumSet.copyOf(requested);
        clientFeatures = request.features() == null ? ClientFeatures.EMPTY : request.features();

        if (request.protocolVersion() != null && supportedVersions.contains(request.protocolVersion())) {
            protocolVersion = request.protocolVersion();
        } else {
            protocolVersion = supportedVersions.getFirst();
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
