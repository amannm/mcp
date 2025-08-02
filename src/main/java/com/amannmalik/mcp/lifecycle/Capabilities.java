package com.amannmalik.mcp.lifecycle;

import jakarta.json.JsonObject;

import java.util.*;

public record Capabilities(Set<ClientCapability> client,
                           Set<ServerCapability> server,
                           Map<String, JsonObject> clientExperimental,
                           Map<String, JsonObject> serverExperimental) {
    public Capabilities {
        client = client == null ? Set.of() : client.isEmpty() ? Set.of() : EnumSet.copyOf(client);
        server = server == null ? Set.of() : server.isEmpty() ? Set.of() : EnumSet.copyOf(server);
        clientExperimental = clientExperimental == null || clientExperimental.isEmpty()
                ? Map.of() : Map.copyOf(clientExperimental);
        serverExperimental = serverExperimental == null || serverExperimental.isEmpty()
                ? Map.of() : Map.copyOf(serverExperimental);
    }

    public Set<ClientCapability> client() {
        return Collections.unmodifiableSet(client);
    }

    public Set<ServerCapability> server() {
        return Collections.unmodifiableSet(server);
    }

    public Map<String, JsonObject> clientExperimental() {
        return Collections.unmodifiableMap(clientExperimental);
    }

    public Map<String, JsonObject> serverExperimental() {
        return Collections.unmodifiableMap(serverExperimental);
    }
}
