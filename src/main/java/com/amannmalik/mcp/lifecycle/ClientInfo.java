package com.amannmalik.mcp.lifecycle;

import com.amannmalik.mcp.validation.InputSanitizer;

public record ClientInfo(String name, String title, String version) {
    public ClientInfo {
        if (name == null || version == null) {
            throw new IllegalArgumentException("name and version required");
        }
        name = InputSanitizer.requireClean(name);
        version = InputSanitizer.requireClean(version);
        title = InputSanitizer.cleanNullable(title);
    }
}
