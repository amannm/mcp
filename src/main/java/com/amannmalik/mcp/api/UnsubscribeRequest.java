package com.amannmalik.mcp.api;

import com.amannmalik.mcp.codec.JsonCodec;
import com.amannmalik.mcp.codec.UnsubscribeRequestAbstractEntityCodec;
import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.JsonObject;

public record UnsubscribeRequest(String uri, JsonObject _meta) {
    static final JsonCodec<UnsubscribeRequest> CODEC = new UnsubscribeRequestAbstractEntityCodec();

    public UnsubscribeRequest {
        uri = ValidationUtil.requireAbsoluteUri(uri);
        ValidationUtil.requireMeta(_meta);
    }

}
