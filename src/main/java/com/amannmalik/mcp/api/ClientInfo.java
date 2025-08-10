package com.amannmalik.mcp.api;

import com.amannmalik.mcp.core.AbstractEntityCodec;
import com.amannmalik.mcp.jsonrpc.JsonCodec;
import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.*;

public record ClientInfo(String name, String title, String version) {
    static final JsonCodec<ClientInfo> CODEC = new AbstractEntityCodec<>() {
        @Override
        public JsonObject toJson(ClientInfo info) {
            JsonObjectBuilder b = Json.createObjectBuilder()
                    .add("name", info.name())
                    .add("version", info.version());
            if (info.title() != null) b.add("title", info.title());
            return b.build();
        }

        @Override
        public ClientInfo fromJson(JsonObject obj) {
            if (obj == null) throw new IllegalArgumentException("object required");
            String name = requireString(obj, "name");
            String version = requireString(obj, "version");
            String title = obj.getString("title", null);
            return new ClientInfo(name, title, version);
        }
    };

    public ClientInfo {
        if (name == null || version == null) {
            throw new IllegalArgumentException("name and version required");
        }
        name = ValidationUtil.requireClean(name);
        version = ValidationUtil.requireClean(version);
        title = ValidationUtil.cleanNullable(title);
    }
}
