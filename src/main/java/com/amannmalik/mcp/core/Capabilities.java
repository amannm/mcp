package com.amannmalik.mcp.core;

import com.amannmalik.mcp.api.ClientCapability;
import com.amannmalik.mcp.api.ServerCapability;
import com.amannmalik.mcp.util.Immutable;
import jakarta.json.JsonObject;

import java.util.Map;
import java.util.Set;

public record Capabilities(Set<ClientCapability> client,
                           Set<ServerCapability> server,
                           Map<String, JsonObject> clientExperimental,
                           Map<String, JsonObject> serverExperimental) {
    public Capabilities {
        client = Immutable.enumSet(client);
        server = Immutable.enumSet(server);
        clientExperimental = Immutable.map(clientExperimental);
        serverExperimental = Immutable.map(serverExperimental);
    }

    /// Return immutable views to avoid exposing internal representation.
    @Override
    public Set<ClientCapability> client() {
        return Set.copyOf(client);
    }

    @Override
    public Set<ServerCapability> server() {
        return Set.copyOf(server);
    }

    @Override
    public Map<String, JsonObject> clientExperimental() {
        return Map.copyOf(clientExperimental);
    }

    @Override
    public Map<String, JsonObject> serverExperimental() {
        return Map.copyOf(serverExperimental);
    }
}
