package com.amannmalik.mcp.lifecycle;

public record ServerInfo(String name, String title, String version) {
    public ServerInfo {
        if (name == null || title == null || version == null) {
            throw new IllegalArgumentException("ServerInfo fields must not be null");
        }
    }
}
