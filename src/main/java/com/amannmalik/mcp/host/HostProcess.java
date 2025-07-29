package com.amannmalik.mcp.host;

import com.amannmalik.mcp.client.McpClient;
import com.amannmalik.mcp.auth.Principal;
import com.amannmalik.mcp.security.ConsentManager;
import com.amannmalik.mcp.security.ToolAccessPolicy;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import com.amannmalik.mcp.jsonrpc.*;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manages a collection of MCP clients within a single host application.
 */
public final class HostProcess implements AutoCloseable {
    private final Map<String, McpClient> clients = new ConcurrentHashMap<>();
    private final SecurityPolicy policy;
    private final ConsentManager consents;
    private final Principal principal;
    private final ToolAccessPolicy toolAccess;

    public HostProcess(SecurityPolicy policy,
                       ConsentManager consents,
                       ToolAccessPolicy toolAccess,
                       Principal principal) {
        this.policy = policy;
        this.consents = consents;
        this.toolAccess = toolAccess;
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

    public Set<String> clientIds() {
        return Collections.unmodifiableSet(clients.keySet());
    }

    public String aggregateContext() {
        return clients.values().stream()
                .map(McpClient::context)
                .collect(Collectors.joining(System.lineSeparator()));
    }

    public JsonObject listTools(String clientId, String cursor) throws IOException {
        McpClient client = clients.get(clientId);
        if (client == null) throw new IllegalArgumentException("Unknown client: " + clientId);
        JsonObject params = cursor == null
                ? Json.createObjectBuilder().build()
                : Json.createObjectBuilder().add("cursor", cursor).build();
        JsonRpcMessage resp = client.request("tools/list", params);
        if (resp instanceof JsonRpcResponse r) return r.result();
        if (resp instanceof JsonRpcError err) throw new IOException(err.error().message());
        throw new IOException("Unexpected response");
    }

    public JsonObject callTool(String clientId, String name, JsonObject args) throws IOException {
        McpClient client = clients.get(clientId);
        if (client == null) throw new IllegalArgumentException("Unknown client: " + clientId);
        toolAccess.requireAllowed(principal, name);
        JsonObject params = Json.createObjectBuilder()
                .add("name", name)
                .add("arguments", args == null ? Json.createObjectBuilder().build() : args)
                .build();
        JsonRpcMessage resp = client.request("tools/call", params);
        if (resp instanceof JsonRpcResponse r) return r.result();
        if (resp instanceof JsonRpcError err) throw new IOException(err.error().message());
        throw new IOException("Unexpected response");
    }

    @Override
    public void close() throws IOException {
        for (String id : Set.copyOf(clients.keySet())) {
            unregister(id);
        }
    }
}
