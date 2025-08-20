package com.amannmalik.mcp.codec;

import com.amannmalik.mcp.spi.CompleteRequest;
import jakarta.json.Json;
import jakarta.json.JsonObject;

public class CompleteRequestJsonCodec implements JsonCodec<CompleteRequest> {
    private static final ArgumentJsonCodec ARGUMENT_CODEC = new ArgumentJsonCodec();
    private static final RefJsonCodec REF_CODEC = new RefJsonCodec();
    private static final ContextJsonCodec CONTEXT_CODEC = new ContextJsonCodec();

    @Override
    public JsonObject toJson(CompleteRequest req) {
        var b = Json.createObjectBuilder()
                .add("ref", REF_CODEC.toJson(req.ref()))
                .add("argument", ARGUMENT_CODEC.toJson(req.argument()));
        if (req.context() != null && !req.context().arguments().isEmpty()) {
            b.add("context", CONTEXT_CODEC.toJson(req.context()));
        }
        if (req._meta() != null) {
            b.add("_meta", req._meta());
        }
        return b.build();
    }

    @Override
    public CompleteRequest fromJson(JsonObject obj) {
        var refObj = obj.getJsonObject("ref");
        var argObj = obj.getJsonObject("argument");
        if (refObj == null || argObj == null) {
            throw new IllegalArgumentException("ref and argument required");
        }
        var ref = REF_CODEC.fromJson(refObj);
        var arg = ARGUMENT_CODEC.fromJson(argObj);
        var ctx = obj.containsKey("context") ? CONTEXT_CODEC.fromJson(obj.getJsonObject("context")) : null;
        var meta = obj.getJsonObject("_meta");
        return new CompleteRequest(ref, arg, ctx, meta);
    }
}
