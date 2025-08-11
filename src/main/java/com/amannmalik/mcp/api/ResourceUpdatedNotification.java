package com.amannmalik.mcp.api;

import com.amannmalik.mcp.util.ValidationUtil;

public record ResourceUpdatedNotification(String uri, String title) {
    public ResourceUpdatedNotification {
        uri = ValidationUtil.requireAbsoluteUri(uri);
        title = ValidationUtil.cleanNullable(title);
    }
}
