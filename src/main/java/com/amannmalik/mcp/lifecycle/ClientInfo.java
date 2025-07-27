package com.amannmalik.mcp.lifecycle;

public record ClientInfo(String name, String title, String version) {
    public ClientInfo {
        if (name == null || title == null || version == null) {
            throw new IllegalArgumentException("ClientInfo fields must not be null");
        }
    }
}
