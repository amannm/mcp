package com.amannmalik.mcp.codec;

import com.amannmalik.mcp.api.model.SubscribeRequest;
import jakarta.json.*;

import java.util.Set;

public final class SubscribeRequestAbstractEntityCodec extends AbstractEntityCodec<SubscribeRequest> {
    @Override
    public JsonObject toJson(SubscribeRequest req) {
        JsonObjectBuilder b = Json.createObjectBuilder().add("uri", req.uri());
        if (req._meta() != null) b.add("_meta", req._meta());
        return b.build();
    }

    @Override
    public SubscribeRequest fromJson(JsonObject obj) {
        if (obj == null) throw new IllegalArgumentException("object required");
        requireOnlyKeys(obj, Set.of("uri", "_meta"));
        String uri = requireString(obj, "uri");
        JsonObject meta = obj.getJsonObject("_meta");
        return new SubscribeRequest(uri, meta);
    }
}
