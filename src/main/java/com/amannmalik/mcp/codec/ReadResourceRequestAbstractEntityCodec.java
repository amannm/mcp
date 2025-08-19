package com.amannmalik.mcp.codec;

import com.amannmalik.mcp.api.ReadResourceRequest;
import jakarta.json.*;

import java.util.Set;

public final class ReadResourceRequestAbstractEntityCodec extends AbstractEntityCodec<ReadResourceRequest> {
    @Override
    public JsonObject toJson(ReadResourceRequest req) {
        return addMeta(Json.createObjectBuilder().add("uri", req.uri()), req._meta()).build();
    }

    @Override
    public ReadResourceRequest fromJson(JsonObject obj) {
        if (obj == null) throw new IllegalArgumentException("object required");
        requireOnlyKeys(obj, Set.of("uri", "_meta"));
        String uri = requireString(obj, "uri");
        return new ReadResourceRequest(uri, meta(obj));
    }
}
