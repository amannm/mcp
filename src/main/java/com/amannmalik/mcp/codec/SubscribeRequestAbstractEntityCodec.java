package com.amannmalik.mcp.codec;

import com.amannmalik.mcp.api.SubscribeRequest;
import jakarta.json.Json;
import jakarta.json.JsonObject;

import java.net.URI;
import java.util.Set;

public final class SubscribeRequestAbstractEntityCodec extends AbstractEntityCodec<SubscribeRequest> {
    @Override
    public JsonObject toJson(SubscribeRequest req) {
        return addMeta(
                Json.createObjectBuilder().add("uri", req.uri().toString()),
                req._meta())
                .build();
    }

    @Override
    public SubscribeRequest fromJson(JsonObject obj) {
        if (obj == null) throw new IllegalArgumentException("object required");
        requireOnlyKeys(obj, Set.of("uri", "_meta"));
        var uriString = requireString(obj, "uri");
        var uri = URI.create(uriString);
        return new SubscribeRequest(uri, meta(obj));
    }
}
