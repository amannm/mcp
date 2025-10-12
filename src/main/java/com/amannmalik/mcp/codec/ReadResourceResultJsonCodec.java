package com.amannmalik.mcp.codec;

import com.amannmalik.mcp.core.ReadResourceResult;
import com.amannmalik.mcp.spi.ResourceBlock;
import jakarta.json.Json;
import jakarta.json.JsonObject;

import java.util.ArrayList;

public class ReadResourceResultJsonCodec implements JsonCodec<ReadResourceResult> {
    private static final ResourceBlockJsonCodec RESOURCE_BLOCK_CODEC = new ResourceBlockJsonCodec();

    public ReadResourceResultJsonCodec() {
    }

    @Override
    public JsonObject toJson(ReadResourceResult r) {
        var arr = Json.createArrayBuilder();
        r.contents().forEach(c -> arr.add(RESOURCE_BLOCK_CODEC.toJson(c)));
        var b = Json.createObjectBuilder().add("contents", arr.build());
        if (r._meta() != null) {
            b.add("_meta", r._meta());
        }
        return b.build();
    }

    @Override
    public ReadResourceResult fromJson(JsonObject obj) {
        if (obj == null) {
            throw new IllegalArgumentException("object required");
        }
        var arr = obj.getJsonArray("contents");
        if (arr == null) {
            throw new IllegalArgumentException("contents required");
        }
        var contents = new ArrayList<ResourceBlock>();
        for (var v : arr) {
            if (!(v instanceof JsonObject)) {
                throw new IllegalArgumentException("content must be object");
            }
            contents.add(RESOURCE_BLOCK_CODEC.fromJson(v.asJsonObject()));
        }
        var meta = obj.getJsonObject("_meta");
        return new ReadResourceResult(contents, meta);
    }
}
