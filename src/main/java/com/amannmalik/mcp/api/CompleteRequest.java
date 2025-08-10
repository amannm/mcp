package com.amannmalik.mcp.api;

import com.amannmalik.mcp.completion.CompleteRequestJsonCodec;
import com.amannmalik.mcp.jsonrpc.JsonCodec;
import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.*;

public record CompleteRequest(
        Ref ref,
        Argument argument,
        Context context,
        JsonObject _meta
) {
    static final JsonCodec<CompleteRequest> CODEC = new CompleteRequestJsonCodec();

    public CompleteRequest {
        if (ref == null || argument == null) {
            throw new IllegalArgumentException("ref and argument are required");
        }
        ValidationUtil.requireMeta(_meta);
    }

}
