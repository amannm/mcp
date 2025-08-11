package com.amannmalik.mcp.api;

import com.amannmalik.mcp.util.ValidationUtil;

public record ClientInfo(String name, String title, String version) {
    public ClientInfo {
        if (name == null || version == null) {
            throw new IllegalArgumentException("name and version required");
        }
        name = ValidationUtil.requireClean(name);
        version = ValidationUtil.requireClean(version);
        title = ValidationUtil.cleanNullable(title);
    }
}
