package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.core.SpiPreconditions;
import jakarta.json.JsonObject;

import java.net.URI;

public record Root(URI uri, String name, JsonObject _meta) {
    public Root {
        uri = SpiPreconditions.requireFileUri(uri);
        if (name != null) {
            name = SpiPreconditions.requireClean(name);
        }
        SpiPreconditions.requireMeta(_meta);
    }
}
