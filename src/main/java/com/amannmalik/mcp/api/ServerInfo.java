package com.amannmalik.mcp.api;

import com.amannmalik.mcp.core.AbstractEntityCodec;
import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.*;

public record ServerInfo(String name, String title, String version) {
     static final JsonCodec<ServerInfo> CODEC = new AbstractEntityCodec<>() {
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
    };

    public ServerInfo {
        if (name == null || version == null) {
            throw new IllegalArgumentException("name and version required");
        }
        name = ValidationUtil.requireClean(name);
        version = ValidationUtil.requireClean(version);
        title = ValidationUtil.cleanNullable(title);
    }
}
