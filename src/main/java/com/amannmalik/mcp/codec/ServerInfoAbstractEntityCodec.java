package com.amannmalik.mcp.codec;

import com.amannmalik.mcp.api.ServerInfo;
import jakarta.json.Json;
import jakarta.json.JsonObject;

public final class ServerInfoAbstractEntityCodec extends AbstractEntityCodec<ServerInfo> {
    public ServerInfoAbstractEntityCodec() {
    }

    @Override
    public JsonObject toJson(ServerInfo info) {
        var b = Json.createObjectBuilder()
                .add("name", info.name())
                .add("version", info.version());
        if (info.title() != null) {
            b.add("title", info.title());
        }
        return b.build();
    }

    @Override
    public ServerInfo fromJson(JsonObject obj) {
        if (obj == null) {
            throw new IllegalArgumentException("object required");
        }
        var name = requireString(obj, "name");
        var version = requireString(obj, "version");
        var title = obj.getString("title", null);
        return new ServerInfo(name, title, version);
    }
}
