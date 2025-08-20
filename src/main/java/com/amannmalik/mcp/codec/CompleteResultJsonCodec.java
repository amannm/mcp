package com.amannmalik.mcp.codec;

import com.amannmalik.mcp.spi.CompleteResult;
import com.amannmalik.mcp.spi.Completion;
import jakarta.json.Json;
import jakarta.json.JsonObject;

public class CompleteResultJsonCodec implements JsonCodec<CompleteResult> {

    private static final JsonCodec<Completion> COMPLETION_JSON_CODEC = new CompletionJsonCodec();

    @Override
    public JsonObject toJson(CompleteResult res) {
        var b = Json.createObjectBuilder()
                .add("completion", COMPLETION_JSON_CODEC.toJson(res.completion()));
        if (res._meta() != null) {
            b.add("_meta", res._meta());
        }
        return b.build();
    }

    @Override
    public CompleteResult fromJson(JsonObject obj) {
        var compObj = obj.getJsonObject("completion");
        if (compObj == null) {
            throw new IllegalArgumentException("completion required");
        }
        var comp = COMPLETION_JSON_CODEC.fromJson(compObj);
        var meta = obj.getJsonObject("_meta");
        return new CompleteResult(comp, meta);
    }
}
