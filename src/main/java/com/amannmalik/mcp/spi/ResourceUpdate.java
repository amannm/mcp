package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.util.ValidationUtil;

import java.net.URI;

public record ResourceUpdate(URI uri, String title) {
    public ResourceUpdate {
        uri = ValidationUtil.requireAbsoluteUri(uri);
        title = ValidationUtil.cleanNullable(title);
    }
}
