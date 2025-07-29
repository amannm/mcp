package com.amannmalik.mcp.host;

import com.amannmalik.mcp.client.McpClient;
import com.amannmalik.mcp.auth.Principal;
import com.amannmalik.mcp.jsonrpc.JsonRpcMessage;
import com.amannmalik.mcp.security.ConsentManager;
import jakarta.json.JsonObject;

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

    public HostProcess(SecurityPolicy policy, ConsentManager consents, Principal principal) {
        this.policy = policy;
        this.consents = consents;
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

    public JsonRpcMessage request(String id, String method, JsonObject params) throws IOException {
        McpClient client = clients.get(id);
        if (client == null) throw new IllegalArgumentException("Unknown client: " + id);
        if (!client.connected()) throw new IllegalStateException("Client not connected: " + id);
        return client.request(method, params);
    }

    public void notify(String id, String method, JsonObject params) throws IOException {
        McpClient client = clients.get(id);
        if (client == null) throw new IllegalArgumentException("Unknown client: " + id);
        if (!client.connected()) throw new IllegalStateException("Client not connected: " + id);
        client.notify(method, params);
    }

    @Override
    public void close() throws IOException {
        for (String id : Set.copyOf(clients.keySet())) {
            unregister(id);
        }
    }
}
