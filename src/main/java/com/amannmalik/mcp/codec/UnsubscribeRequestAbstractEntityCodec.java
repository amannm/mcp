package com.amannmalik.mcp.codec;

import com.amannmalik.mcp.api.UnsubscribeRequest;
import jakarta.json.Json;
import jakarta.json.JsonObject;

import java.net.URI;
import java.util.Set;

public final class UnsubscribeRequestAbstractEntityCodec extends AbstractEntityCodec<UnsubscribeRequest> {
    @Override
    public JsonObject toJson(UnsubscribeRequest req) {
        var b = Json.createObjectBuilder().add("uri", req.uri().toString());
        if (req._meta() != null) {
            b.add("_meta", req._meta());
        }
        return b.build();
    }

    @Override
    public UnsubscribeRequest fromJson(JsonObject obj) {
        if (obj == null) {
            throw new IllegalArgumentException("object required");
        }
        requireOnlyKeys(obj, Set.of("uri", "_meta"));
        var uriString = requireString(obj, "uri");
        var uri = URI.create(uriString);
        var meta = obj.getJsonObject("_meta");
        return new UnsubscribeRequest(uri, meta);
    }
}
