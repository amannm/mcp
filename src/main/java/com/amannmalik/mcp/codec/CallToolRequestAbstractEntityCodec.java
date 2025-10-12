package com.amannmalik.mcp.codec;

import com.amannmalik.mcp.api.Request.CallToolRequest;
import jakarta.json.Json;
import jakarta.json.JsonObject;

import java.util.Set;

public final class CallToolRequestAbstractEntityCodec extends AbstractEntityCodec<CallToolRequest> {
    public CallToolRequestAbstractEntityCodec() {
    }

    @Override
    public JsonObject toJson(CallToolRequest req) {
        var b = Json.createObjectBuilder()
                .add("name", req.name())
                .add("arguments", req.arguments());
        if (req._meta() != null) {
            b.add("_meta", req._meta());
        }
        return b.build();
    }

    @Override
    public CallToolRequest fromJson(JsonObject obj) {
        if (obj == null) {
            throw new IllegalArgumentException("object required");
        }
        requireOnlyKeys(obj, Set.of("name", "arguments", "_meta"));
        var name = requireString(obj, "name");
        var arguments = obj.getJsonObject("arguments");
        var meta = obj.getJsonObject("_meta");
        return new CallToolRequest(name, arguments, meta);
    }
}
