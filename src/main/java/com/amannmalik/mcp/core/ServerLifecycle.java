package com.amannmalik.mcp.core;

import com.amannmalik.mcp.api.ClientCapability;
import com.amannmalik.mcp.api.RequestId;
import com.amannmalik.mcp.api.ServerCapability;
import com.amannmalik.mcp.api.ServerFeature;
import com.amannmalik.mcp.api.ServerInfo;
import com.amannmalik.mcp.jsonrpc.JsonRpcError;
import com.amannmalik.mcp.util.InitializeRequest;
import com.amannmalik.mcp.util.InitializeResponse;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Coordinates lifecycle transitions and negotiation details for {@link com.amannmalik.mcp.api.McpServer}.
 */
public final class ServerLifecycle {
    private static final int NOT_INITIALIZED_ERROR = -32002;

    private final List<String> supportedVersions;
    private final Set<ServerCapability> declaredCapabilities;
    private final ServerInfo serverInfo;
    private final String instructions;

    private LifecycleState state = LifecycleState.INIT;
    private Set<ClientCapability> clientCapabilities = EnumSet.noneOf(ClientCapability.class);
    private ClientFeatures clientFeatures = ClientFeatures.EMPTY;
    private String protocolVersion;

    public ServerLifecycle(Collection<String> supportedVersions,
                           Set<ServerCapability> declaredCapabilities,
                           ServerInfo serverInfo,
                           String instructions) {
        this.supportedVersions = normaliseSupportedVersions(supportedVersions);
        this.declaredCapabilities = normaliseServerCapabilities(declaredCapabilities);
        this.serverInfo = Objects.requireNonNull(serverInfo, "serverInfo");
        this.instructions = instructions;
        this.protocolVersion = this.supportedVersions.get(0);
    }

    public InitializeResponse initialize(InitializeRequest request, Set<ServerFeature> features) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(features, "features");
        requireState(LifecycleState.INIT);

        clientCapabilities = normaliseClientCapabilities(request.capabilities());
        clientFeatures = Objects.requireNonNullElse(request.features(), ClientFeatures.EMPTY);
        protocolVersion = negotiateProtocolVersion(request.protocolVersion());

        var negotiatedCapabilities = new Capabilities(
                clientCapabilities,
                declaredCapabilities,
                Map.of(),
                Map.of());

        return new InitializeResponse(
                protocolVersion,
                negotiatedCapabilities,
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
            throw new IllegalStateException("Invalid lifecycle state: " + state);
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

    private static List<String> normaliseSupportedVersions(Collection<String> versions) {
        if (versions == null || versions.isEmpty()) {
            throw new IllegalArgumentException("supportedVersions required");
        }
        var sorted = new ArrayList<>(versions);
        sorted.sort(Comparator.reverseOrder());
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

    private String negotiateProtocolVersion(String requested) {
        if (requested != null && supportedVersions.contains(requested)) {
            return requested;
        }
        return supportedVersions.get(0);
    }
}

