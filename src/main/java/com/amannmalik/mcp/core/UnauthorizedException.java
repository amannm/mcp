package com.amannmalik.mcp.core;

import java.io.IOException;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class UnauthorizedException extends IOException {
    private static final Pattern RESOURCE_METADATA =
            Pattern.compile("resource_metadata=\"([^\"]+)\"");

    private final String wwwAuthenticate;

    public UnauthorizedException(String wwwAuthenticate) {
        super("HTTP 401 Unauthorized");
        this.wwwAuthenticate = wwwAuthenticate;
    }

    public String wwwAuthenticate() {
        return wwwAuthenticate;
    }

    public Optional<String> resourceMetadata() {
        if (wwwAuthenticate == null) return Optional.empty();
        var m = RESOURCE_METADATA.matcher(wwwAuthenticate);
        if (m.find()) return Optional.of(m.group(1));
        return Optional.empty();
    }
}

