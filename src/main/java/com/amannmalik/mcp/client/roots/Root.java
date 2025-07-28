package com.amannmalik.mcp.client.roots;

import com.amannmalik.mcp.validation.UriValidator;
import com.amannmalik.mcp.validation.InputSanitizer;

/** Filesystem boundary made available to a server. */
public record Root(String uri, String name) {
    public Root {
        uri = UriValidator.requireFileUri(uri);
        name = name == null ? null : InputSanitizer.requireClean(name);
    }
}
