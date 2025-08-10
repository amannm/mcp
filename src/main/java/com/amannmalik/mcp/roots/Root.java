package com.amannmalik.mcp.roots;

import com.amannmalik.mcp.core.AbstractEntityCodec;
import com.amannmalik.mcp.jsonrpc.JsonCodec;
import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.*;

import java.util.Set;

public record Root(String uri, String name, JsonObject _meta) {
    public static final JsonCodec<Root> CODEC = new AbstractEntityCodec<>() {
        @Override
        public JsonObject toJson(Root root) {
            JsonObjectBuilder b = Json.createObjectBuilder().add("uri", root.uri());
            if (root.name() != null) b.add("name", root.name());
            if (root._meta() != null) b.add("_meta", root._meta());
            return b.build();
        }

        @Override
        public Root fromJson(JsonObject obj) {
            if (obj == null) throw new IllegalArgumentException("object required");
            requireOnlyKeys(obj, Set.of("uri", "name", "_meta"));
            String uri = requireString(obj, "uri");
            String name = obj.getString("name", null);
            return new Root(uri, name, getObject(obj, "_meta"));
        }
    };

    public Root {
        uri = ValidationUtil.requireFileUri(uri);
        if (name != null) {
            name = ValidationUtil.requireClean(name);
        }
        ValidationUtil.requireMeta(_meta);
    }
}
