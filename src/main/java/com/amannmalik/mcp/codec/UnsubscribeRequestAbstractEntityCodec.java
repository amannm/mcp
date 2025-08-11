package com.amannmalik.mcp.codec;

import com.amannmalik.mcp.api.model.UnsubscribeRequest;
import jakarta.json.*;

import java.util.Set;

public final class UnsubscribeRequestAbstractEntityCodec extends AbstractEntityCodec<UnsubscribeRequest> {
    @Override
    public JsonObject toJson(UnsubscribeRequest req) {
        JsonObjectBuilder b = Json.createObjectBuilder().add("uri", req.uri());
        if (req._meta() != null) b.add("_meta", req._meta());
        return b.build();
    }

    @Override
    public UnsubscribeRequest fromJson(JsonObject obj) {
        if (obj == null) throw new IllegalArgumentException("object required");
        requireOnlyKeys(obj, Set.of("uri", "_meta"));
        String uri = requireString(obj, "uri");
        JsonObject meta = obj.getJsonObject("_meta");
        return new UnsubscribeRequest(uri, meta);
    }
}
