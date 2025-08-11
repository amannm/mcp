package com.amannmalik.mcp.codec;

import com.amannmalik.mcp.api.model.Root;
import jakarta.json.*;

import java.util.Set;

public final class RootAbstractEntityCodec extends AbstractEntityCodec<Root> {
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
}
