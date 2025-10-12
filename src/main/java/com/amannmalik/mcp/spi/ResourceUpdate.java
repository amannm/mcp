package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.core.SpiPreconditions;

import java.net.URI;

public record ResourceUpdate(URI uri, String title) {
    public ResourceUpdate {
        uri = SpiPreconditions.requireAbsoluteUri(uri);
        title = SpiPreconditions.cleanNullable(title);
    }
}
