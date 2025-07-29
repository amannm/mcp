package com.amannmalik.mcp.client.roots;

import com.amannmalik.mcp.validation.InputSanitizer;
import com.amannmalik.mcp.validation.MetaValidator;
import com.amannmalik.mcp.validation.UriValidator;
import jakarta.json.JsonObject;


public record Root(String uri, String name, JsonObject _meta) {
    public Root {
        uri = UriValidator.requireFileUri(uri);
        name = name == null ? null : InputSanitizer.requireClean(name);
        MetaValidator.requireValid(_meta);
    }
}
