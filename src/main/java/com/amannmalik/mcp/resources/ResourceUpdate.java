package com.amannmalik.mcp.resources;

import com.amannmalik.mcp.util.ValidationUtil;

public record ResourceUpdate(String uri, String title) {
    public ResourceUpdate {
        uri = ValidationUtil.requireAbsoluteUri(uri);
        title = ValidationUtil.cleanNullable(title);
    }
}
