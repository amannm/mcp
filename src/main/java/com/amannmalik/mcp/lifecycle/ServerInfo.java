package com.amannmalik.mcp.lifecycle;

import com.amannmalik.mcp.validation.InputSanitizer;

public record ServerInfo(String name, String title, String version) {
    public ServerInfo {
        if (name == null || version == null) {
            throw new IllegalArgumentException("name and version required");
        }
        name = InputSanitizer.requireClean(name);
        version = InputSanitizer.requireClean(version);
        title = title == null ? null : InputSanitizer.requireClean(title);
    }
}
