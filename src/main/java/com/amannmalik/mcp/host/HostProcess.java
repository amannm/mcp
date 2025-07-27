package com.amannmalik.mcp.host;

import com.amannmalik.mcp.client.McpClient;

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

    public HostProcess(SecurityPolicy policy) {
        this.policy = policy;
    }

    public void register(String id, McpClient client) throws IOException {
        if (!policy.allow(client)) {
            throw new SecurityException("Client not authorized: " + client.info().name());
        }
        if (clients.putIfAbsent(id, client) != null) {
            throw new IllegalArgumentException("Client already registered: " + id);
        }
        client.connect();
    }

    public void unregister(String id) throws IOException {
        McpClient client = clients.remove(id);
        if (client != null) {
            client.disconnect();
        }
    }

    public Set<String> clientIds() {
        return Collections.unmodifiableSet(clients.keySet());
    }

    public String aggregateContext() {
        return clients.values().stream()
                .map(McpClient::context)
                .collect(Collectors.joining(System.lineSeparator()));
    }

    @Override
    public void close() throws IOException {
        for (String id : Set.copyOf(clients.keySet())) {
            unregister(id);
        }
    }
}
