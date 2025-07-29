package com.amannmalik.mcp.lifecycle;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

import java.util.EnumSet;
import java.util.Set;

/** JSON utilities for lifecycle messages. */
public final class LifecycleCodec {
    private LifecycleCodec() {}

    public static JsonObject toJsonObject(InitializeRequest req) {
        var caps = Json.createObjectBuilder();
        req.capabilities().client()
                .forEach(c -> caps.add(c.name().toLowerCase(), JsonValue.EMPTY_JSON_OBJECT));
        return Json.createObjectBuilder()
                .add("protocolVersion", req.protocolVersion())
                .add("capabilities", caps.build())
                .add("clientInfo", Json.createObjectBuilder()
                        .add("name", req.clientInfo().name())
                        .add("title", req.clientInfo().title())
                        .add("version", req.clientInfo().version())
                        .build())
                .build();
    }

    public static InitializeRequest toInitializeRequest(JsonObject obj) {
        String version = obj.getString("protocolVersion");
        JsonObject capsObj = obj.getJsonObject("capabilities");
        Set<ClientCapability> client = EnumSet.noneOf(ClientCapability.class);
        if (capsObj != null) {
            capsObj.forEach((k, v) -> {
                try {
                    client.add(ClientCapability.valueOf(k.toUpperCase()));
                } catch (IllegalArgumentException ignore) {}
            });
        }
        Set<ServerCapability> server = EnumSet.noneOf(ServerCapability.class);
        Capabilities caps = new Capabilities(
                client.isEmpty() ? Set.of() : EnumSet.copyOf(client),
                server.isEmpty() ? Set.of() : EnumSet.copyOf(server)
        );
        JsonObject ci = obj.getJsonObject("clientInfo");
        ClientInfo info = new ClientInfo(ci.getString("name"), ci.getString("title"), ci.getString("version"));
        return new InitializeRequest(version, caps, info);
    }

    public static JsonObject toJsonObject(InitializeResponse resp) {
        var server = Json.createObjectBuilder();
        for (var c : resp.capabilities().server()) {
            var b = Json.createObjectBuilder();
            switch (c) {
                case PROMPTS -> b.add("listChanged", true);
                case RESOURCES -> b.add("subscribe", true).add("listChanged", true);
                case TOOLS -> b.add("listChanged", true);
                default -> {}
            }
            server.add(c.name().toLowerCase(), b.build());
        }
        var builder = Json.createObjectBuilder()
                .add("protocolVersion", resp.protocolVersion())
                .add("capabilities", server.build())
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

    public static InitializeResponse toInitializeResponse(JsonObject obj) {
        String version = obj.getString("protocolVersion");
        JsonObject capsObj = obj.getJsonObject("capabilities");
        Set<ClientCapability> client = EnumSet.noneOf(ClientCapability.class);
        Set<ServerCapability> server = EnumSet.noneOf(ServerCapability.class);
        if (capsObj != null) {
            capsObj.forEach((k, v) -> {
                try {
                    server.add(ServerCapability.valueOf(k.toUpperCase()));
                } catch (IllegalArgumentException ignore) {}
            });
        }
        Capabilities caps = new Capabilities(
                client.isEmpty() ? Set.of() : EnumSet.copyOf(client),
                server.isEmpty() ? Set.of() : EnumSet.copyOf(server)
        );
        JsonObject si = obj.getJsonObject("serverInfo");
        ServerInfo info = new ServerInfo(si.getString("name"), si.getString("title"), si.getString("version"));
        String instructions = obj.containsKey("instructions") ? obj.getString("instructions") : null;
        return new InitializeResponse(version, caps, info, instructions);
    }
}
