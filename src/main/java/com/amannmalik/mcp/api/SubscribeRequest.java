package com.amannmalik.mcp.api;

import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.JsonObject;

import java.net.URI;

public record SubscribeRequest(URI uri, JsonObject _meta) {
    public SubscribeRequest {
        uri = ValidationUtil.requireAbsoluteUri(uri);
        ValidationUtil.requireMeta(_meta);
    }
}
