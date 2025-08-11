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
                Set.of(),
                server.isEmpty() ? Set.of() : EnumSet.copyOf(server),
                Map.of(),
                experimental.isEmpty() ? Map.of() : Map.copyOf(experimental)
        );
        ServerInfo info = SERVER_INFO_CODEC.fromJson(getObject(obj, "serverInfo"));
        String instructions = obj.getString("instructions", null);
        ServerFeatures features = new ServerFeatures(resSub, resList, toolList, promptList);
        return new InitializeResponse(version, caps, info, instructions, features);
    }
}
