package com.amannmalik.mcp.lifecycle;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

import java.util.EnumSet;
import java.util.Set;

/** JSON utilities for lifecycle messages. */
public final class LifecycleCodec {
    private LifecycleCodec() {}

    public static InitializeRequest toInitializeRequest(JsonObject obj) {
        String version = obj.getString("protocolVersion");
        JsonObject capsObj = obj.getJsonObject("capabilities");
        Set<ClientCapability> client = EnumSet.noneOf(ClientCapability.class);
        Set<ServerCapability> server = EnumSet.noneOf(ServerCapability.class);
        if (capsObj != null) {
            var clientObj = capsObj.getJsonObject("client");
            if (clientObj != null) {
                clientObj.forEach((k, v) -> client.add(ClientCapability.valueOf(k.toUpperCase())));
            }
            var serverObj = capsObj.getJsonObject("server");
            if (serverObj != null) {
                serverObj.forEach((k, v) -> server.add(ServerCapability.valueOf(k.toUpperCase())));
            }
        }
        Capabilities caps = new Capabilities(
                client.isEmpty() ? Set.of() : EnumSet.copyOf(client),
                server.isEmpty() ? Set.of() : EnumSet.copyOf(server)
        );
        JsonObject ci = obj.getJsonObject("clientInfo");
        ClientInfo info = new ClientInfo(ci.getString("name"), ci.getString("title"), ci.getString("version"));
        return new InitializeRequest(version, caps, info);
    }

    public static JsonObject toJsonObject(InitializeResponse resp) {
        var client = Json.createObjectBuilder();
        resp.capabilities().client().forEach(c -> client.add(c.name().toLowerCase(), JsonValue.EMPTY_JSON_OBJECT));
        var server = Json.createObjectBuilder();
        resp.capabilities().server().forEach(c -> server.add(c.name().toLowerCase(), JsonValue.EMPTY_JSON_OBJECT));
        var builder = Json.createObjectBuilder()
                .add("protocolVersion", resp.protocolVersion())
                .add("capabilities", Json.createObjectBuilder()
                        .add("client", client)
                        .add("server", server)
                        .build())
                .add("serverInfo", Json.createObjectBuilder()
                        .add("name", resp.serverInfo().name())
                        .add("title", resp.serverInfo().title())
                        .add("version", resp.serverInfo().version())
                        .build());
        if (resp.instructions() != null) {
            builder.add("instructions", resp.instructions());
        }
        return builder.build();
    }
}
