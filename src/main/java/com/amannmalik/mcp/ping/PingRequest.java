package com.amannmalik.mcp.ping;

import com.amannmalik.mcp.core.AbstractEntityCodec;
import com.amannmalik.mcp.core.JsonCodec;
import com.amannmalik.mcp.validation.ValidationUtil;
import jakarta.json.JsonObject;

public record PingRequest(JsonObject _meta) {
    public static final JsonCodec<PingRequest> CODEC =
            AbstractEntityCodec.metaOnly(PingRequest::_meta, PingRequest::new);

    public PingRequest {
        ValidationUtil.requireMeta(_meta);
    }
}
