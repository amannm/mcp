package com.amannmalik.mcp.api.resource;

import com.amannmalik.mcp.util.ValidationUtil;

public record ResourceUpdate(String uri, String title) {
    public ResourceUpdate {
        uri = ValidationUtil.requireAbsoluteUri(uri);
        title = ValidationUtil.cleanNullable(title);
    }
}
