package com.amannmalik.mcp.api;

import com.amannmalik.mcp.codec.JsonCodec;
import com.amannmalik.mcp.codec.ReadResourceRequestAbstractEntityCodec;
import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.JsonObject;

public record ReadResourceRequest(String uri, JsonObject _meta) {
    public static final JsonCodec<ReadResourceRequest> CODEC = new ReadResourceRequestAbstractEntityCodec();

    public ReadResourceRequest {
        uri = ValidationUtil.requireAbsoluteUri(uri);
        ValidationUtil.requireMeta(_meta);
    }

}
