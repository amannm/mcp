package com.amannmalik.mcp.codec;

import com.amannmalik.mcp.api.model.ServerInfo;
import jakarta.json.*;

public final class ServerInfoAbstractEntityCodec extends AbstractEntityCodec<ServerInfo> {
    @Override
    public JsonObject toJson(ServerInfo info) {
        JsonObjectBuilder b = Json.createObjectBuilder()
                .add("name", info.name())
                .add("version", info.version());
        if (info.title() != null) b.add("title", info.title());
        return b.build();
    }

    @Override
    public ServerInfo fromJson(JsonObject obj) {
        if (obj == null) throw new IllegalArgumentException("object required");
        String name = requireString(obj, "name");
        String version = requireString(obj, "version");
        String title = obj.getString("title", null);
        return new ServerInfo(name, title, version);
    }
}
