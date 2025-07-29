package com.amannmalik.mcp.lifecycle;

import jakarta.json.Json;
import jakarta.json.JsonObject;

import java.util.EnumSet;
import java.util.Set;

public final class LifecycleCodec {
    private LifecycleCodec() {
    }

    public static JsonObject toJsonObject(InitializeRequest req) {
        var caps = Json.createObjectBuilder();
        for (var c : req.capabilities().client()) {
            var b = Json.createObjectBuilder();
            if (c == ClientCapability.ROOTS) {
                b.add("listChanged", true);
            }
            caps.add(c.name().toLowerCase(), b.build());
        }
        var info = Json.createObjectBuilder()
                .add("name", req.clientInfo().name())
                .add("version", req.clientInfo().version());
        if (req.clientInfo().title() != null) info.add("title", req.clientInfo().title());
        return Json.createObjectBuilder()
                .add("protocolVersion", req.protocolVersion())
                .add("capabilities", caps.build())
                .add("clientInfo", info.build())
                .build();
    }

    public static InitializeRequest toInitializeRequest(JsonObject obj) {
        if (!obj.containsKey("protocolVersion")) {
            throw new IllegalArgumentException("protocolVersion required");
        }
        String version = obj.getString("protocolVersion");
        JsonObject capsObj = obj.getJsonObject("capabilities");
        Set<ClientCapability> client = EnumSet.noneOf(ClientCapability.class);
        if (capsObj != null) {
            capsObj.forEach((k, v) -> {
                try {
                    client.add(ClientCapability.valueOf(k.toUpperCase()));
                } catch (IllegalArgumentException ignore) {
                }
            });
        }
        Set<ServerCapability> server = EnumSet.noneOf(ServerCapability.class);
        Capabilities caps = new Capabilities(
                client.isEmpty() ? Set.of() : EnumSet.copyOf(client),
                server.isEmpty() ? Set.of() : EnumSet.copyOf(server)
        );
        JsonObject ci = obj.getJsonObject("clientInfo");
        if (ci == null) {
            throw new IllegalArgumentException("clientInfo required");
        }
        ClientInfo info = new ClientInfo(
                ci.getString("name"),
                ci.containsKey("title") ? ci.getString("title") : null,
                ci.getString("version")
        );
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
                default -> {
                }
            }
            server.add(c.name().toLowerCase(), b.build());
        }
        var info = Json.createObjectBuilder()
                .add("name", resp.serverInfo().name())
                .add("version", resp.serverInfo().version());
        if (resp.serverInfo().title() != null) info.add("title", resp.serverInfo().title());
        var builder = Json.createObjectBuilder()
                .add("protocolVersion", resp.protocolVersion())
                .add("capabilities", server.build())
                .add("serverInfo", info.build());
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
                } catch (IllegalArgumentException ignore) {
                }
            });
        }
        Capabilities caps = new Capabilities(
                client.isEmpty() ? Set.of() : EnumSet.copyOf(client),
                server.isEmpty() ? Set.of() : EnumSet.copyOf(server)
        );
        JsonObject si = obj.getJsonObject("serverInfo");
        if (si == null) {
            throw new IllegalArgumentException("serverInfo required");
        }
        ServerInfo info = new ServerInfo(
                si.getString("name"),
                si.containsKey("title") ? si.getString("title") : null,
                si.getString("version")
        );
        String instructions = obj.containsKey("instructions") ? obj.getString("instructions") : null;
        return new InitializeResponse(version, caps, info, instructions);
    }
}
