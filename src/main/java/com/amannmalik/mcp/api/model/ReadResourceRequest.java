package com.amannmalik.mcp.api.model;

import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.JsonObject;

public record ReadResourceRequest(String uri, JsonObject _meta) {

    public ReadResourceRequest {
        uri = ValidationUtil.requireAbsoluteUri(uri);
        ValidationUtil.requireMeta(_meta);
    }

}
