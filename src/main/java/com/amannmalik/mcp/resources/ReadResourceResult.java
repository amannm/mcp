package com.amannmalik.mcp.resources;

import com.amannmalik.mcp.api.ResourceBlock;
import com.amannmalik.mcp.codec.ResourceBlockJsonCodec;
import com.amannmalik.mcp.jsonrpc.JsonCodec;
import com.amannmalik.mcp.util.*;
import jakarta.json.*;

import java.util.ArrayList;
import java.util.List;

public record ReadResourceResult(List<ResourceBlock> contents, JsonObject _meta) {

    private static final ResourceBlockJsonCodec RESOURCE_BLOCK_CODEC = new ResourceBlockJsonCodec();

    public static final JsonCodec<ReadResourceResult> CODEC = new JsonCodec<>() {
        @Override
        public JsonObject toJson(ReadResourceResult r) {
            JsonArrayBuilder arr = Json.createArrayBuilder();
            r.contents().forEach(c -> arr.add(RESOURCE_BLOCK_CODEC.toJson(c)));
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
                contents.add(RESOURCE_BLOCK_CODEC.fromJson(v.asJsonObject()));
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
