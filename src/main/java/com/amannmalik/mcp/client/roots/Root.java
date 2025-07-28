package com.amannmalik.mcp.client.roots;

/** Filesystem boundary made available to a server. */
public record Root(String uri, String name) {
    public Root {
        if (uri == null) throw new IllegalArgumentException("uri is required");
        if (!uri.startsWith("file:")) {
            throw new IllegalArgumentException("uri must start with file:");
        }
    }
}
