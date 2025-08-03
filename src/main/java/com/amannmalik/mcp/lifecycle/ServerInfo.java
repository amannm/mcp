package com.amannmalik.mcp.lifecycle;

import com.amannmalik.mcp.server.roots.validation.InputSanitizer;

public record ServerInfo(String name, String title, String version) {
    public ServerInfo {
        if (name == null || version == null) {
            throw new IllegalArgumentException("name and version required");
        }
        name = InputSanitizer.requireClean(name);
        version = InputSanitizer.requireClean(version);
        title = InputSanitizer.cleanNullable(title);
    }
}
