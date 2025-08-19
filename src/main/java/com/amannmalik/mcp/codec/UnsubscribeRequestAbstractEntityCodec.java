package com.amannmalik.mcp.codec;

import com.amannmalik.mcp.api.UnsubscribeRequest;
import jakarta.json.*;

import java.util.Set;

public final class UnsubscribeRequestAbstractEntityCodec extends AbstractEntityCodec<UnsubscribeRequest> {
    @Override
    public JsonObject toJson(UnsubscribeRequest req) {
        return addMeta(Json.createObjectBuilder().add("uri", req.uri()), req._meta()).build();
    }

    @Override
    public UnsubscribeRequest fromJson(JsonObject obj) {
        if (obj == null) throw new IllegalArgumentException("object required");
        requireOnlyKeys(obj, Set.of("uri", "_meta"));
        String uri = requireString(obj, "uri");
        return new UnsubscribeRequest(uri, meta(obj));
    }
}
