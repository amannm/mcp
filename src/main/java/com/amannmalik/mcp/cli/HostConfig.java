package com.amannmalik.mcp.cli;

import java.util.Map;

public record HostConfig(Map<String, String> clients) implements CliConfig {
    public HostConfig {
        if (clients == null || clients.isEmpty()) {
            throw new IllegalArgumentException("clients required");
        }
        clients = Map.copyOf(clients);
    }
}
