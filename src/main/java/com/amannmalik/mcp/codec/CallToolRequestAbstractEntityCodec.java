package com.amannmalik.mcp.codec;

import com.amannmalik.mcp.api.CallToolRequest;
import jakarta.json.*;

import java.util.Set;

public final class CallToolRequestAbstractEntityCodec extends AbstractEntityCodec<CallToolRequest> {
    @Override
    public JsonObject toJson(CallToolRequest req) {
        JsonObjectBuilder b = Json.createObjectBuilder()
                .add("name", req.name())
                .add("arguments", req.arguments());
        if (req._meta() != null) b.add("_meta", req._meta());
        return b.build();
    }

    @Override
    public CallToolRequest fromJson(JsonObject obj) {
        if (obj == null) throw new IllegalArgumentException("object required");
        requireOnlyKeys(obj, Set.of("name", "arguments", "_meta"));
        String name = requireString(obj, "name");
        JsonObject arguments = obj.getJsonObject("arguments");
        JsonObject meta = obj.getJsonObject("_meta");
        return new CallToolRequest(name, arguments, meta);
    }
}
