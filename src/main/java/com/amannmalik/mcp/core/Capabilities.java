package com.amannmalik.mcp.core;

import com.amannmalik.mcp.api.ClientCapability;
import com.amannmalik.mcp.api.ServerCapability;
import jakarta.json.JsonObject;

import java.util.*;

public record Capabilities(Set<ClientCapability> client,
                           Set<ServerCapability> server,
                           Map<String, JsonObject> clientExperimental,
                           Map<String, JsonObject> serverExperimental) {
    public Capabilities {
        client = immutableEnumSet(client);
        server = immutableEnumSet(server);
        clientExperimental = immutableMap(clientExperimental);
        serverExperimental = immutableMap(serverExperimental);
    }

    private static <E extends Enum<E>> Set<E> immutableEnumSet(Set<E> set) {
        return set == null || set.isEmpty() ? Set.of() : EnumSet.copyOf(set);
    }

    private static <K, V> Map<K, V> immutableMap(Map<K, V> map) {
        return map == null || map.isEmpty() ? Map.of() : Map.copyOf(map);
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
