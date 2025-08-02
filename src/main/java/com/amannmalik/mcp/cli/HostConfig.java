package com.amannmalik.mcp.cli;

import java.util.Map;

@Deprecated
public record HostConfig(Map<String, String> clients) implements CliConfig {
    public HostConfig {
        if (clients == null || clients.isEmpty()) {
            throw new IllegalArgumentException("clients required");
        }
        clients = Map.copyOf(clients);
    }
}
