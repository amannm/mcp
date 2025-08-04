package com.amannmalik.mcp.elicitation;

import com.amannmalik.mcp.core.JsonCodec;
import com.amannmalik.mcp.core.AbstractEntityCodec;
import com.amannmalik.mcp.validation.*;
import jakarta.json.*;

import java.util.Set;

public record ElicitRequest(String message, JsonObject requestedSchema, JsonObject _meta) {
    public static final JsonCodec<ElicitRequest> CODEC = new JsonCodec<>() {
        @Override
        public JsonObject toJson(ElicitRequest req) {
            JsonObjectBuilder b = Json.createObjectBuilder()
                    .add("message", req.message())
                    .add("requestedSchema", req.requestedSchema());
            if (req._meta() != null) b.add("_meta", req._meta());
            return b.build();
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
            return new ElicitRequest(message, schemaVal.asJsonObject(), obj.getJsonObject("_meta"));
        }
    };

    public ElicitRequest(String message, JsonObject requestedSchema, JsonObject _meta) {
        if (message == null || requestedSchema == null) {
            throw new IllegalArgumentException("message and requestedSchema are required");
        }
        this.message = InputSanitizer.requireClean(message);
        ValidationUtil.requireElicitSchema(requestedSchema);
        this.requestedSchema = requestedSchema;
        ValidationUtil.requireMeta(_meta);
        this._meta = _meta;
    }
}
