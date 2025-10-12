package com.amannmalik.mcp.codec;

import com.amannmalik.mcp.api.*;
import com.amannmalik.mcp.core.Capabilities;
import com.amannmalik.mcp.spi.InitializeResponse;
import com.amannmalik.mcp.util.Immutable;
import jakarta.json.Json;
import jakarta.json.JsonObject;

import java.util.*;

public final class InitializeResponseAbstractEntityCodec extends AbstractEntityCodec<InitializeResponse> {

    private static final JsonCodec<ServerInfo> SERVER_INFO_CODEC = new ServerInfoAbstractEntityCodec();

    @Override
    public JsonObject toJson(InitializeResponse resp) {
        var server = Json.createObjectBuilder();
        for (var c : resp.capabilities().server()) {
            var b = Json.createObjectBuilder();
            var f = resp.features();
            switch (c) {
                case PROMPTS -> {
                    if (f.contains(ServerFeature.PROMPTS_LIST_CHANGED)) {
                        b.add("listChanged", true);
                    }
                }
                case RESOURCES -> {
                    if (f.contains(ServerFeature.RESOURCES_SUBSCRIBE)) {
                        b.add("subscribe", true);
                    }
                    if (f.contains(ServerFeature.RESOURCES_LIST_CHANGED)) {
                        b.add("listChanged", true);
                    }
                }
                case TOOLS -> {
                    if (f.contains(ServerFeature.TOOLS_LIST_CHANGED)) {
                        b.add("listChanged", true);
                    }
                }
                default -> {
                }
            }
            server.add(c.code(), b.build());
        }
        resp.capabilities().serverExperimental().forEach(server::add);
        var b = Json.createObjectBuilder()
                .add("protocolVersion", resp.protocolVersion())
                .add("capabilities", server.build())
                .add("serverInfo", SERVER_INFO_CODEC.toJson(resp.serverInfo()));
        if (resp.instructions() != null) {
            b.add("instructions", resp.instructions());
        }
        return b.build();
    }

    @Override
    public InitializeResponse fromJson(JsonObject obj) {
        var version = requireString(obj, "protocolVersion");
        var capsObj = obj.getJsonObject("capabilities");
        var server = EnumSet.noneOf(ServerCapability.class);
        var experimental = new HashMap<String, JsonObject>();
        if (capsObj != null) {
            capsObj.forEach((k, v) -> ServerCapability.from(k)
                    .ifPresentOrElse(server::add, () -> experimental.put(k, v.asJsonObject())));
        }
        var features = EnumSet.noneOf(ServerFeature.class);
        if (capsObj != null) {
            var res = capsObj.getJsonObject(ServerCapability.RESOURCES.code());
            if (res != null) {
                if (getBoolean(res, "subscribe", false)) {
                    features.add(ServerFeature.RESOURCES_SUBSCRIBE);
                }
                if (getBoolean(res, "listChanged", false)) {
                    features.add(ServerFeature.RESOURCES_LIST_CHANGED);
                }
            }
            var tools = capsObj.getJsonObject(ServerCapability.TOOLS.code());
            if (tools != null && getBoolean(tools, "listChanged", false)) {
                features.add(ServerFeature.TOOLS_LIST_CHANGED);
            }
            var prompts = capsObj.getJsonObject(ServerCapability.PROMPTS.code());
            if (prompts != null && getBoolean(prompts, "listChanged", false)) {
                features.add(ServerFeature.PROMPTS_LIST_CHANGED);
            }
        }
        var caps = new Capabilities(
                Set.of(),
                Immutable.enumSet(server),
                Map.of(),
                Immutable.map(experimental)
        );
        var info = SERVER_INFO_CODEC.fromJson(getObject(obj, "serverInfo"));
        var instructions = obj.getString("instructions", null);
        return new InitializeResponse(version, caps, info, instructions, Immutable.enumSet(features));
    }
}
