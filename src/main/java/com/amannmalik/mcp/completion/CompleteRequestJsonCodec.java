package com.amannmalik.mcp.completion;

import com.amannmalik.mcp.api.*;
import com.amannmalik.mcp.jsonrpc.JsonCodec;
import com.amannmalik.mcp.util.ContextJsonCodec;
import com.amannmalik.mcp.util.RefJsonCodec;
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
        if (req._meta() != null) b.add("_meta", req._meta());
        return b.build();
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
        JsonObject meta = obj.getJsonObject("_meta");
        return new CompleteRequest(ref, arg, ctx, meta);
    }
}
