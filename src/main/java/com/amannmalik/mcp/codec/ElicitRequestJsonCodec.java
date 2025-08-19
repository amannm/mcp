package com.amannmalik.mcp.codec;

import com.amannmalik.mcp.spi.ElicitRequest;
import jakarta.json.*;

import java.util.Set;

public class ElicitRequestJsonCodec implements JsonCodec<ElicitRequest> {
    @Override
    public JsonObject toJson(ElicitRequest req) {
        JsonObjectBuilder b = Json.createObjectBuilder()
                .add("message", req.message())
                .add("requestedSchema", req.requestedSchema());
        return AbstractEntityCodec.addMeta(b, req._meta()).build();
    }

    @Override
    public ElicitRequest fromJson(JsonObject obj) {
        if (obj == null) throw new IllegalArgumentException("object required");
        AbstractEntityCodec.requireOnlyKeys(obj, Set.of("message", "requestedSchema", "_meta"));
        String message = obj.getString("message", null);
        if (message == null) throw new IllegalArgumentException("message required");
        JsonValue schemaVal = obj.get("requestedSchema");
        if (schemaVal == null || schemaVal.getValueType() != JsonValue.ValueType.OBJECT) {
            throw new IllegalArgumentException("requestedSchema must be object");
        }
        return new ElicitRequest(message, schemaVal.asJsonObject(), AbstractEntityCodec.meta(obj));
    }
}
