package com.amannmalik.mcp.lifecycle;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public record Capabilities(Set<ClientCapability> client, Set<ServerCapability> server) {
    public Capabilities {
        client = client == null ? Set.of() : client.isEmpty() ? Set.of() : EnumSet.copyOf(client);
        server = server == null ? Set.of() : server.isEmpty() ? Set.of() : EnumSet.copyOf(server);
    }

    public Set<ClientCapability> client() {
        return Collections.unmodifiableSet(client);
    }

    public Set<ServerCapability> server() {
        return Collections.unmodifiableSet(server);
    }
}
