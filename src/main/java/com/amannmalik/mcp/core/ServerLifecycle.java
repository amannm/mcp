package com.amannmalik.mcp.core;

import com.amannmalik.mcp.api.*;
import com.amannmalik.mcp.jsonrpc.JsonRpcError;
import com.amannmalik.mcp.util.InitializeRequest;
import com.amannmalik.mcp.util.InitializeResponse;

import java.util.*;

public final class ServerLifecycle {
    private static final int NOT_INITIALIZED_ERROR = -32002;

    private final List<String> supportedVersions;
    private final Set<ServerCapability> declaredCapabilities;
    private final ServerInfo serverInfo;
    private final String instructions;
    private final EnumSet<ClientCapability> clientCapabilities = EnumSet.noneOf(ClientCapability.class);

    private LifecycleState state = LifecycleState.INIT;
    private ClientFeatures clientFeatures = ClientFeatures.EMPTY;
    private String protocolVersion;

    public ServerLifecycle(Collection<String> supportedVersions,
                           Set<ServerCapability> declaredCapabilities,
                           ServerInfo serverInfo,
                           String instructions) {
        this.supportedVersions = normaliseSupportedVersions(supportedVersions);
        this.declaredCapabilities = normaliseServerCapabilities(declaredCapabilities);
        this.serverInfo = Objects.requireNonNull(serverInfo, "serverInfo");
        this.instructions = instructions == null ? "" : instructions;
        this.protocolVersion = this.supportedVersions.get(0);
    }

    private static List<String> normaliseSupportedVersions(Collection<String> versions) {
        if (versions == null) {
            throw new IllegalArgumentException("supportedVersions required");
        }
        var sorted = new TreeSet<String>(Comparator.reverseOrder());
        sorted.addAll(versions);
        if (sorted.isEmpty()) {
            throw new IllegalArgumentException("supportedVersions required");
        }
        return List.copyOf(sorted);
    }

    private static Set<ServerCapability> normaliseServerCapabilities(Set<ServerCapability> capabilities) {
        if (capabilities == null || capabilities.isEmpty()) {
            return EnumSet.noneOf(ServerCapability.class);
        }
        return EnumSet.copyOf(capabilities);
    }

    private static Set<ClientCapability> normaliseClientCapabilities(Capabilities requested) {
        if (requested == null) {
            return EnumSet.noneOf(ClientCapability.class);
        }
        var caps = requested.client();
        if (caps.isEmpty()) {
            return EnumSet.noneOf(ClientCapability.class);
        }
        return EnumSet.copyOf(caps);
    }

    public InitializeResponse initialize(InitializeRequest request, Set<ServerFeature> features) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(features, "features");
        requireState(LifecycleState.INIT);

        clientCapabilities.clear();
        clientCapabilities.addAll(normaliseClientCapabilities(request.capabilities()));
        clientFeatures = Objects.requireNonNullElse(request.features(), ClientFeatures.EMPTY);
        protocolVersion = negotiateProtocolVersion(request.protocolVersion());

        return new InitializeResponse(
                protocolVersion,
                negotiatedCapabilities(),
                serverInfo,
                instructions,
                features);
    }

    public void confirmInitialized() {
        requireState(LifecycleState.INIT);
        state = LifecycleState.OPERATION;
    }

    public void shutdown() {
        state = LifecycleState.SHUTDOWN;
    }

    public LifecycleState state() {
        return state;
    }

    public Set<ClientCapability> clientCapabilities() {
        return Set.copyOf(clientCapabilities);
    }

    public ClientFeatures clientFeatures() {
        return clientFeatures;
    }

    public String protocolVersion() {
        return protocolVersion;
    }

    public void requireState(LifecycleState expected) {
        if (state != expected) {
            throw new IllegalStateException("Expected lifecycle state " + expected + " but was " + state);
        }
    }

    public void requireClientCapability(ClientCapability capability) {
        Objects.requireNonNull(capability, "capability");
        if (!clientCapabilities.contains(capability)) {
            throw new IllegalStateException("Missing client capability: " + capability);
        }
    }

    public Optional<JsonRpcError> ensureInitialized(RequestId id, String message) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(message, "message");
        if (state != LifecycleState.OPERATION) {
            return Optional.of(JsonRpcError.of(id, NOT_INITIALIZED_ERROR, message));
        }
        return Optional.empty();
    }

    private Capabilities negotiatedCapabilities() {
        return new Capabilities(
                clientCapabilities,
                declaredCapabilities,
                Map.of(),
                Map.of());
    }

    private String negotiateProtocolVersion(String requested) {
        if (requested != null && supportedVersions.contains(requested)) {
            return requested;
        }
        return supportedVersions.get(0);
    }
}

