package com.amannmalik.mcp.validation;

import java.net.URI;


public final class UriValidator {
    private UriValidator() {
    }

    public static String requireAbsolute(String uri) {
        if (uri == null) throw new IllegalArgumentException("uri is required");
        URI parsed;
        try {
            parsed = URI.create(uri).normalize();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid URI: " + uri, e);
        }
        if (!parsed.isAbsolute()) {
            throw new IllegalArgumentException("URI must be absolute: " + uri);
        }
        if (parsed.getFragment() != null) {
            throw new IllegalArgumentException("URI must not contain fragment: " + uri);
        }
        return parsed.toString();
    }

    public static String requireFileUri(String uri) {
        String normalized = requireAbsolute(uri);
        if (!normalized.startsWith("file:")) {
            throw new IllegalArgumentException("URI must start with file:");
        }
        return normalized;
    }
}
