package com.amannmalik.mcp.codec;

import com.amannmalik.mcp.api.CompleteResult;
import com.amannmalik.mcp.api.Completion;
import jakarta.json.*;

public class CompleteResultJsonCodec implements JsonCodec<CompleteResult> {

    static final JsonCodec<Completion> CODEC = new CompletionJsonCodec();

    @Override
    public JsonObject toJson(CompleteResult res) {
        JsonObjectBuilder b = Json.createObjectBuilder()
                .add("completion", CODEC.toJson(res.completion()));
        if (res._meta() != null) b.add("_meta", res._meta());
        return b.build();
    }

    @Override
    public CompleteResult fromJson(JsonObject obj) {
        JsonObject compObj = obj.getJsonObject("completion");
        if (compObj == null) throw new IllegalArgumentException("completion required");
        Completion comp = CODEC.fromJson(compObj);
        JsonObject meta = obj.getJsonObject("_meta");
        return new CompleteResult(comp, meta);
    }
}
