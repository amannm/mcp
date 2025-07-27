package com.amannmalik.mcp.lifecycle;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;

import java.util.EnumSet;
import java.util.Set;

/** Utility for serializing lifecycle objects to JSON. */
public final class LifecycleCodec {
    private LifecycleCodec() {}

    public static JsonObject toJson(InitializeRequest request) {
        return Json.createObjectBuilder()
                .add("protocolVersion", request.protocolVersion())
                .add("capabilities", toJson(request.capabilities()))
                .add("clientInfo", toJson(request.clientInfo()))
                .build();
    }

    public static InitializeResponse toInitializeResponse(JsonObject obj) {
        String version = obj.getString("protocolVersion");
        JsonObject capsObj = obj.getJsonObject("capabilities");
        Set<ClientCapability> clientCaps = fromJsonClientCaps(capsObj.getJsonObject("client"));
        Set<ServerCapability> serverCaps = fromJsonServerCaps(capsObj.getJsonObject("server"));
        Capabilities caps = new Capabilities(clientCaps, serverCaps);
        JsonObject info = obj.getJsonObject("serverInfo");
        ServerInfo serverInfo = new ServerInfo(info.getString("name"), info.getString("title"), info.getString("version"));
        String instructions = obj.containsKey("instructions") ? obj.getString("instructions") : null;
        return new InitializeResponse(version, caps, serverInfo, instructions);
    }

    public static JsonObject toJson(InitializeResponse response) {
        JsonObjectBuilder builder = Json.createObjectBuilder()
                .add("protocolVersion", response.protocolVersion())
                .add("capabilities", toJson(response.capabilities()))
                .add("serverInfo", toJson(response.serverInfo()));
        if (response.instructions() != null) builder.add("instructions", response.instructions());
        return builder.build();
    }

    private static JsonObject toJson(Capabilities caps) {
        return Json.createObjectBuilder()
                .add("client", toJson(caps.client()))
                .add("server", toJson(caps.server()))
                .build();
    }

    private static JsonObject toJson(ClientInfo info) {
        return Json.createObjectBuilder()
                .add("name", info.name())
                .add("title", info.title())
                .add("version", info.version())
                .build();
    }

    private static JsonObject toJson(ServerInfo info) {
        return Json.createObjectBuilder()
                .add("name", info.name())
                .add("title", info.title())
                .add("version", info.version())
                .build();
    }

    private static JsonObject toJson(Set<? extends Enum<?>> caps) {
        JsonObjectBuilder b = Json.createObjectBuilder();
        for (Enum<?> c : caps) b.add(c.name().toLowerCase(), Json.createObjectBuilder().build());
        return b.build();
    }

    private static Set<ClientCapability> fromJsonClientCaps(JsonObject obj) {
        EnumSet<ClientCapability> set = EnumSet.noneOf(ClientCapability.class);
        if (obj != null) for (String key : obj.keySet()) set.add(ClientCapability.valueOf(key.toUpperCase()));
        return set;
    }

    private static Set<ServerCapability> fromJsonServerCaps(JsonObject obj) {
        EnumSet<ServerCapability> set = EnumSet.noneOf(ServerCapability.class);
        if (obj != null) for (String key : obj.keySet()) set.add(ServerCapability.valueOf(key.toUpperCase()));
        return set;
    }
}
