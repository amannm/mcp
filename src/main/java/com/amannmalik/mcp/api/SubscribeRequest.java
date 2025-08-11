package com.amannmalik.mcp.api;

import com.amannmalik.mcp.codec.JsonCodec;
import com.amannmalik.mcp.codec.SubscribeRequestAbstractEntityCodec;
import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.JsonObject;

public record SubscribeRequest(String uri, JsonObject _meta) {
    static final JsonCodec<SubscribeRequest> CODEC = new SubscribeRequestAbstractEntityCodec();

    public SubscribeRequest {
        uri = ValidationUtil.requireAbsoluteUri(uri);
        ValidationUtil.requireMeta(_meta);
    }

}
