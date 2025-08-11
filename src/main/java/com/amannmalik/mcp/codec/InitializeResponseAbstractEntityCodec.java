package com.amannmalik.mcp.codec;

import com.amannmalik.mcp.api.*;
import com.amannmalik.mcp.core.Capabilities;
import com.amannmalik.mcp.util.InitializeResponse;
import jakarta.json.*;

import java.util.*;

public final class InitializeResponseAbstractEntityCodec extends AbstractEntityCodec<InitializeResponse> {

    private static final JsonCodec<ServerInfo> SERVER_INFO_CODEC = new ServerInfoAbstractEntityCodec();

    @Override
    public JsonObject toJson(InitializeResponse resp) {
        JsonObjectBuilder server = Json.createObjectBuilder();
        for (var c : resp.capabilities().server()) {
            JsonObjectBuilder b = Json.createObjectBuilder();
            Set<ServerFeature> f = resp.features();
            switch (c) {
                case PROMPTS -> {
                    if (f != null && f.contains(ServerFeature.PROMPTS_LIST_CHANGED)) b.add("listChanged", true);
                }
                case RESOURCES -> {
                    if (f != null && f.contains(ServerFeature.RESOURCES_SUBSCRIBE)) b.add("subscribe", true);
                    if (f != null && f.contains(ServerFeature.RESOURCES_LIST_CHANGED)) b.add("listChanged", true);
                }
                case TOOLS -> {
                    if (f != null && f.contains(ServerFeature.TOOLS_LIST_CHANGED)) b.add("listChanged", true);
                }
                default -> {
                }
            }
            server.add(c.code(), b.build());
        }
        resp.capabilities().serverExperimental().forEach(server::add);
        JsonObjectBuilder b = Json.createObjectBuilder()
                .add("protocolVersion", resp.protocolVersion())
                .add("capabilities", server.build())
                .add("serverInfo", SERVER_INFO_CODEC.toJson(resp.serverInfo()));
        if (resp.instructions() != null) b.add("instructions", resp.instructions());
        return b.build();
    }

    @Override
    public InitializeResponse fromJson(JsonObject obj) {
        String version = requireString(obj, "protocolVersion");
        JsonObject capsObj = obj.getJsonObject("capabilities");
        Set<ServerCapability> server = EnumSet.noneOf(ServerCapability.class);
        Map<String, JsonObject> experimental = new HashMap<>();
        if (capsObj != null) {
            capsObj.forEach((k, v) -> ServerCapability.from(k)
                    .ifPresentOrElse(server::add, () -> experimental.put(k, v.asJsonObject())));
        }
        EnumSet<ServerFeature> features = EnumSet.noneOf(ServerFeature.class);
        if (capsObj != null) {
            JsonObject res = capsObj.getJsonObject(ServerCapability.RESOURCES.code());
            if (res != null) {
                if (res.getBoolean("subscribe", false)) features.add(ServerFeature.RESOURCES_SUBSCRIBE);
                if (res.getBoolean("listChanged", false)) features.add(ServerFeature.RESOURCES_LIST_CHANGED);
            }
            JsonObject tools = capsObj.getJsonObject(ServerCapability.TOOLS.code());
            if (tools != null && tools.getBoolean("listChanged", false)) {
                features.add(ServerFeature.TOOLS_LIST_CHANGED);
            }
            JsonObject prompts = capsObj.getJsonObject(ServerCapability.PROMPTS.code());
            if (prompts != null && prompts.getBoolean("listChanged", false)) {
                features.add(ServerFeature.PROMPTS_LIST_CHANGED);
            }
        }
        Capabilities caps = new Capabilities(
                Set.of(),
                server.isEmpty() ? Set.of() : EnumSet.copyOf(server),
                Map.of(),
                experimental.isEmpty() ? Map.of() : Map.copyOf(experimental)
        );
        ServerInfo info = SERVER_INFO_CODEC.fromJson(getObject(obj, "serverInfo"));
        String instructions = obj.getString("instructions", null);
        Set<ServerFeature> f = features.isEmpty() ? Set.of() : EnumSet.copyOf(features);
        return new InitializeResponse(version, caps, info, instructions, f);
    }
}
