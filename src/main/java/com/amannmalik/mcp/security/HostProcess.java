package com.amannmalik.mcp.security;

import com.amannmalik.mcp.auth.Principal;
import com.amannmalik.mcp.client.McpClient;
import com.amannmalik.mcp.jsonrpc.JsonRpcError;
import com.amannmalik.mcp.jsonrpc.JsonRpcMessage;
import com.amannmalik.mcp.jsonrpc.JsonRpcResponse;
import com.amannmalik.mcp.lifecycle.ServerCapability;
import com.amannmalik.mcp.prompts.Role;
import com.amannmalik.mcp.server.tools.ListToolsResult;
import com.amannmalik.mcp.server.tools.ToolCodec;
import com.amannmalik.mcp.server.tools.ToolResult;
import com.amannmalik.mcp.RequestMethod;
import com.amannmalik.mcp.util.PaginatedRequest;
import com.amannmalik.mcp.util.PaginationCodec;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class HostProcess implements AutoCloseable {
    private final Map<String, McpClient> clients = new ConcurrentHashMap<>();
    private final SecurityPolicy policy;
    private final ConsentManager consents;
    private final Principal principal;
    private final ToolAccessController toolAccess;
    private final PrivacyBoundaryEnforcer privacyBoundary;
    private final SamplingAccessController samplingAccess;

    private static Optional<ServerCapability> capabilityForMethod(String method) {
        if (method.startsWith("tools/")) return Optional.of(ServerCapability.TOOLS);
        if (method.startsWith("resources/")) return Optional.of(ServerCapability.RESOURCES);
        if (method.startsWith("prompts/")) return Optional.of(ServerCapability.PROMPTS);
        if (method.startsWith("completion/")) return Optional.of(ServerCapability.COMPLETIONS);
        if (method.startsWith("logging/")) return Optional.of(ServerCapability.LOGGING);
        return Optional.empty();
    }

    private static void requireCapability(McpClient client, Optional<ServerCapability> cap) {
        cap.ifPresent(c -> {
            if (!client.serverCapabilities().contains(c)) {
                throw new IllegalStateException("Server capability not supported: " + c);
            }
        });
    }

    public HostProcess(SecurityPolicy policy,
                       ConsentManager consents,
                       ToolAccessController toolAccess,
                       PrivacyBoundaryEnforcer privacyBoundary,
                       SamplingAccessController samplingAccess,
                       Principal principal) {
        this.policy = policy;
        this.consents = consents;
        this.toolAccess = toolAccess;
        this.privacyBoundary = privacyBoundary;
        this.samplingAccess = samplingAccess;
        this.principal = principal;
    }

    public void register(String id, McpClient client) throws IOException {
        if (!policy.allow(client)) {
            throw new SecurityException("Client not authorized: " + client.info().name());
        }
        consents.requireConsent(principal, client.info().name());
        if (clients.putIfAbsent(id, client) != null) {
            throw new IllegalArgumentException("Client already registered: " + id);
        }
        try {
            client.setPrincipal(principal);
            client.setSamplingAccessPolicy(samplingAccess);
            client.configurePing(30000, 5000);
            client.connect();
        } catch (IOException e) {
            clients.remove(id);
            throw e;
        }
    }

    public void unregister(String id) throws IOException {
        McpClient client = clients.remove(id);
        if (client != null) {
            client.disconnect();
        }
    }

    public void grantConsent(String scope) {
        consents.grant(principal.id(), scope);
    }

    public void revokeConsent(String scope) {
        consents.revoke(principal.id(), scope);
    }

    public void allowTool(String tool) {
        toolAccess.allow(principal.id(), tool);
    }

    public void revokeTool(String tool) {
        toolAccess.revoke(principal.id(), tool);
    }

    public void allowSampling() {
        samplingAccess.allow(principal.id());
    }

    public void revokeSampling() {
        samplingAccess.revoke(principal.id());
    }

    public void allowAudience(Role audience) {
        privacyBoundary.allow(principal.id(), audience);
    }

    public void revokeAudience(Role audience) {
        privacyBoundary.revoke(principal.id(), audience);
    }

    public Set<String> clientIds() {
        return Collections.unmodifiableSet(clients.keySet());
    }

    public String aggregateContext() {
        return clients.values().stream()
                .map(McpClient::context)
                .collect(Collectors.joining(System.lineSeparator()));
    }

    public ListToolsResult listTools(String clientId, String cursor) throws IOException {
        McpClient client = clients.get(clientId);
        if (client == null) throw new IllegalArgumentException("Unknown client: " + clientId);
        requireCapability(client, Optional.of(ServerCapability.TOOLS));
        JsonObject params = PaginationCodec.toJsonObject(new PaginatedRequest(cursor));
        JsonRpcMessage resp = client.request(RequestMethod.TOOLS_LIST.method(), params);
        if (resp instanceof JsonRpcResponse r) return ToolCodec.toListToolsResult(r.result());
        if (resp instanceof JsonRpcError err) throw new IOException(err.error().message());
        throw new IOException("Unexpected response");
    }

    public ToolResult callTool(String clientId, String name, JsonObject args) throws IOException {
        McpClient client = clients.get(clientId);
        if (client == null) throw new IllegalArgumentException("Unknown client: " + clientId);
        requireCapability(client, Optional.of(ServerCapability.TOOLS));
        consents.requireConsent(principal, "tool:" + name);
        toolAccess.requireAllowed(principal, name);
        JsonObject params = Json.createObjectBuilder()
                .add("name", name)
                .add("arguments", args == null ? JsonValue.EMPTY_JSON_OBJECT : args)
                .build();
        JsonRpcMessage resp = client.request(RequestMethod.TOOLS_CALL.method(), params);
        if (resp instanceof JsonRpcResponse r) return ToolCodec.toToolResult(r.result());
        if (resp instanceof JsonRpcError err) throw new IOException(err.error().message());
        throw new IOException("Unexpected response");
    }

    public JsonObject createMessage(String clientId, JsonObject params) throws IOException {
        McpClient client = clients.get(clientId);
        if (client == null) throw new IllegalArgumentException("Unknown client: " + clientId);
        if (!client.connected()) throw new IllegalStateException("Client not connected: " + clientId);
        consents.requireConsent(principal, "sampling");
        samplingAccess.requireAllowed(principal);
        JsonRpcMessage resp = client.request(RequestMethod.SAMPLING_CREATE_MESSAGE.method(), params);
        if (resp instanceof JsonRpcResponse r) return r.result();
        if (resp instanceof JsonRpcError err) throw new IOException(err.error().message());
        throw new IOException("Unexpected response");
    }

    public JsonRpcMessage request(String id, String method, JsonObject params) throws IOException {
        McpClient client = clients.get(id);
        if (client == null) throw new IllegalArgumentException("Unknown client: " + id);
        if (!client.connected()) throw new IllegalStateException("Client not connected: " + id);
        requireCapability(client, capabilityForMethod(method));
        return client.request(method, params);
    }

    public void notify(String id, String method, JsonObject params) throws IOException {
        McpClient client = clients.get(id);
        if (client == null) throw new IllegalArgumentException("Unknown client: " + id);
        if (!client.connected()) throw new IllegalStateException("Client not connected: " + id);
        requireCapability(client, capabilityForMethod(method));
        client.notify(method, params);
    }

    @Override
    public void close() throws IOException {
        for (String id : Set.copyOf(clients.keySet())) {
            unregister(id);
        }
    }
}
