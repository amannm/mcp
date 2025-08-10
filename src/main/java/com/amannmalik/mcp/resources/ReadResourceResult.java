package com.amannmalik.mcp.resources;

import com.amannmalik.mcp.jsonrpc.JsonCodec;
import com.amannmalik.mcp.api.ResourceBlock;
import com.amannmalik.mcp.util.Immutable;
import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.*;

import java.util.ArrayList;
import java.util.List;

public record ReadResourceResult(List<ResourceBlock> contents, JsonObject _meta) {
    public static final JsonCodec<ReadResourceResult> CODEC = new JsonCodec<>() {
        @Override
        public JsonObject toJson(ReadResourceResult r) {
            JsonArrayBuilder arr = Json.createArrayBuilder();
            r.contents().forEach(c -> arr.add(ResourceBlock.CODEC.toJson(c)));
            JsonObjectBuilder b = Json.createObjectBuilder().add("contents", arr.build());
            if (r._meta() != null) b.add("_meta", r._meta());
            return b.build();
        }

        @Override
        public ReadResourceResult fromJson(JsonObject obj) {
            if (obj == null) throw new IllegalArgumentException("object required");
            JsonArray arr = obj.getJsonArray("contents");
            if (arr == null) throw new IllegalArgumentException("contents required");
            List<ResourceBlock> contents = new ArrayList<>();
            for (JsonValue v : arr) {
                if (v.getValueType() != JsonValue.ValueType.OBJECT) {
                    throw new IllegalArgumentException("content must be object");
                }
                contents.add(ResourceBlock.CODEC.fromJson(v.asJsonObject()));
            }
            JsonObject meta = obj.getJsonObject("_meta");
            return new ReadResourceResult(contents, meta);
        }
    };

    public ReadResourceResult {
        contents = Immutable.list(contents);
        ValidationUtil.requireMeta(_meta);
    }
}
