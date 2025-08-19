package com.amannmalik.mcp.codec;

import com.amannmalik.mcp.spi.CompleteResult;
import com.amannmalik.mcp.spi.Completion;
import jakarta.json.*;

public class CompleteResultJsonCodec implements JsonCodec<CompleteResult> {

    private static final JsonCodec<Completion> COMPLETION_JSON_CODEC = new CompletionJsonCodec();

    @Override
    public JsonObject toJson(CompleteResult res) {
        return AbstractEntityCodec.addMeta(
                Json.createObjectBuilder().add("completion", COMPLETION_JSON_CODEC.toJson(res.completion())),
                res._meta()
        ).build();
    }

    @Override
    public CompleteResult fromJson(JsonObject obj) {
        JsonObject compObj = obj.getJsonObject("completion");
        if (compObj == null) throw new IllegalArgumentException("completion required");
        Completion comp = COMPLETION_JSON_CODEC.fromJson(compObj);
        return new CompleteResult(comp, AbstractEntityCodec.meta(obj));
    }
}
