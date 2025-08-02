package com.amannmalik.mcp.lifecycle;

import jakarta.json.Json;
import jakarta.json.JsonObject;

import java.util.*;

public final class LifecycleCodec {
    private LifecycleCodec() {
    }

    public static JsonObject toJsonObject(InitializeRequest req) {
        var caps = Json.createObjectBuilder();
        for (var c : req.capabilities().client()) {
            var b = Json.createObjectBuilder();
            if (c == ClientCapability.ROOTS && req.features().rootsListChanged()) {
                b.add("listChanged", true);
            }
            caps.add(c.name().toLowerCase(), b.build());
        }
        req.capabilities().clientExperimental()
                .forEach(caps::add);
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
        Map<String, JsonObject> experimental = new HashMap<>();
        boolean rootsList = false;
        if (capsObj != null) {
            for (var entry : capsObj.entrySet()) {
                String k = entry.getKey();
                JsonObject v = entry.getValue().asJsonObject();
                switch (k) {
                    case "roots" -> {
                        client.add(ClientCapability.ROOTS);
                        rootsList = v.getBoolean("listChanged", false);
                    }
                    case "sampling" -> client.add(ClientCapability.SAMPLING);
                    case "elicitation" -> client.add(ClientCapability.ELICITATION);
                    default -> experimental.put(k, v);
                }
            }
        }
        Set<ServerCapability> server = EnumSet.noneOf(ServerCapability.class);
        Capabilities caps = new Capabilities(
                client.isEmpty() ? Set.of() : EnumSet.copyOf(client),
                server.isEmpty() ? Set.of() : EnumSet.copyOf(server),
                experimental.isEmpty() ? Map.of() : Map.copyOf(experimental),
                Map.of()
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
        ClientFeatures features = new ClientFeatures(rootsList);
        return new InitializeRequest(version, caps, info, features);
    }

    public static JsonObject toJsonObject(InitializeResponse resp) {
        var server = Json.createObjectBuilder();
        for (var c : resp.capabilities().server()) {
            var b = Json.createObjectBuilder();
            ServerFeatures f = resp.features();
            switch (c) {
                case PROMPTS -> {
                    if (f != null && f.promptsListChanged()) b.add("listChanged", true);
                }
                case RESOURCES -> {
                    if (f != null && f.resourcesSubscribe()) b.add("subscribe", true);
                    if (f != null && f.resourcesListChanged()) b.add("listChanged", true);
                }
                case TOOLS -> {
                    if (f != null && f.toolsListChanged()) b.add("listChanged", true);
                }
                default -> {
                }
            }
            server.add(c.name().toLowerCase(), b.build());
        }
        resp.capabilities().serverExperimental()
                .forEach(server::add);
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
        Map<String, JsonObject> experimental = new HashMap<>();
        if (capsObj != null) {
            capsObj.forEach((k, v) -> {
                try {
                    server.add(ServerCapability.valueOf(k.toUpperCase()));
                } catch (IllegalArgumentException ignore) {
                    experimental.put(k, v.asJsonObject());
                }
            });
        }
        boolean resSub = false;
        boolean resList = false;
        boolean toolList = false;
        boolean promptList = false;
        if (capsObj != null) {
            JsonObject res = capsObj.getJsonObject("resources");
            if (res != null) {
                resSub = res.getBoolean("subscribe", false);
                resList = res.getBoolean("listChanged", false);
            }
            JsonObject tools = capsObj.getJsonObject("tools");
            if (tools != null) {
                toolList = tools.getBoolean("listChanged", false);
            }
            JsonObject prompts = capsObj.getJsonObject("prompts");
            if (prompts != null) {
                promptList = prompts.getBoolean("listChanged", false);
            }
        }
        Capabilities caps = new Capabilities(
                client.isEmpty() ? Set.of() : EnumSet.copyOf(client),
                server.isEmpty() ? Set.of() : EnumSet.copyOf(server),
                Map.of(),
                experimental.isEmpty() ? Map.of() : Map.copyOf(experimental)
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
        ServerFeatures features = new ServerFeatures(resSub, resList, toolList, promptList);
        return new InitializeResponse(version, caps, info, instructions, features);
    }
}
