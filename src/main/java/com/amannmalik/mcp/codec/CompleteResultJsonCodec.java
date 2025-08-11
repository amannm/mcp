package com.amannmalik.mcp.codec;

import com.amannmalik.mcp.api.CompleteResult;
import com.amannmalik.mcp.api.Completion;
import jakarta.json.*;

public class CompleteResultJsonCodec implements JsonCodec<CompleteResult> {

    private static final JsonCodec<Completion> COMPLETION_JSON_CODEC = new CompletionJsonCodec();

    @Override
    public JsonObject toJson(CompleteResult res) {
        JsonObjectBuilder b = Json.createObjectBuilder()
                .add("completion", COMPLETION_JSON_CODEC.toJson(res.completion()));
        if (res._meta() != null) b.add("_meta", res._meta());
        return b.build();
    }

    @Override
    public CompleteResult fromJson(JsonObject obj) {
        JsonObject compObj = obj.getJsonObject("completion");
        if (compObj == null) throw new IllegalArgumentException("completion required");
        Completion comp = COMPLETION_JSON_CODEC.fromJson(compObj);
        JsonObject meta = obj.getJsonObject("_meta");
        return new CompleteResult(comp, meta);
    }
}
