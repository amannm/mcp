package com.amannmalik.mcp.lifecycle;

import com.amannmalik.mcp.core.AbstractEntityCodec;
import com.amannmalik.mcp.core.JsonCodec;
import com.amannmalik.mcp.validation.InputSanitizer;
import jakarta.json.*;

public record ClientInfo(String name, String title, String version) {
    public static final JsonCodec<ClientInfo> CODEC = new AbstractEntityCodec<>() {
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
        name = InputSanitizer.requireClean(name);
        version = InputSanitizer.requireClean(version);
        title = InputSanitizer.cleanNullable(title);
    }
}
