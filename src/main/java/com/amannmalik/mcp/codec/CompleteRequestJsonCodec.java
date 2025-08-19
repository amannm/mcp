package com.amannmalik.mcp.codec;

import com.amannmalik.mcp.spi.*;
import jakarta.json.*;

public class CompleteRequestJsonCodec implements JsonCodec<CompleteRequest> {
    private static final ArgumentJsonCodec ARGUMENT_CODEC = new ArgumentJsonCodec();
    private static final RefJsonCodec REF_CODEC = new RefJsonCodec();
    private static final ContextJsonCodec CONTEXT_CODEC = new ContextJsonCodec();

    @Override
    public JsonObject toJson(CompleteRequest req) {
        JsonObjectBuilder b = Json.createObjectBuilder()
                .add("ref", REF_CODEC.toJson(req.ref()))
                .add("argument", ARGUMENT_CODEC.toJson(req.argument()));
        if (req.context() != null && !req.context().arguments().isEmpty()) {
            b.add("context", CONTEXT_CODEC.toJson(req.context()));
        }
        return AbstractEntityCodec.addMeta(b, req._meta()).build();
    }

    @Override
    public CompleteRequest fromJson(JsonObject obj) {
        JsonObject refObj = obj.getJsonObject("ref");
        JsonObject argObj = obj.getJsonObject("argument");
        if (refObj == null || argObj == null) {
            throw new IllegalArgumentException("ref and argument required");
        }
        Ref ref = REF_CODEC.fromJson(refObj);
        Argument arg = ARGUMENT_CODEC.fromJson(argObj);
        Context ctx = obj.containsKey("context") ? CONTEXT_CODEC.fromJson(obj.getJsonObject("context")) : null;
        return new CompleteRequest(ref, arg, ctx, AbstractEntityCodec.meta(obj));
    }
}
