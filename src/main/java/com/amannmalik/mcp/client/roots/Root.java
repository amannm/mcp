package com.amannmalik.mcp.client.roots;

import com.amannmalik.mcp.server.roots.validation.*;
import jakarta.json.JsonObject;

public record Root(String uri, String name, JsonObject _meta) {
    public Root {
        uri = UriValidator.requireFileUri(uri);
        if (name != null) {
            name = InputSanitizer.requireClean(name);
        }
        MetaValidator.requireValid(_meta);
    }
}
