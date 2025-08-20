package com.amannmalik.mcp.codec;

import com.amannmalik.mcp.spi.Root;
import jakarta.json.Json;
import jakarta.json.JsonObject;

import java.net.URI;
import java.util.Set;

public final class RootAbstractEntityCodec extends AbstractEntityCodec<Root> {
    @Override
    public JsonObject toJson(Root root) {
        var b = Json.createObjectBuilder().add("uri", root.uri().toString());
        if (root.name() != null) b.add("name", root.name());
        if (root._meta() != null) b.add("_meta", root._meta());
        return b.build();
    }

    @Override
    public Root fromJson(JsonObject obj) {
        if (obj == null) throw new IllegalArgumentException("object required");
        requireOnlyKeys(obj, Set.of("uri", "name", "_meta"));
        var uriString = requireString(obj, "uri");
        var uri = URI.create(uriString);
        var name = obj.getString("name", null);
        return new Root(uri, name, getObject(obj, "_meta"));
    }
}
