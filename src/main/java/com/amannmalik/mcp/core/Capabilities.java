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
}
