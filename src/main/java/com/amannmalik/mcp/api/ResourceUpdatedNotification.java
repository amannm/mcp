package com.amannmalik.mcp.api;

import com.amannmalik.mcp.util.ValidationUtil;

import java.net.URI;

public record ResourceUpdatedNotification(URI uri, String title) {
    public ResourceUpdatedNotification {
        uri = ValidationUtil.requireAbsoluteUri(uri);
        title = ValidationUtil.cleanNullable(title);
    }
}
