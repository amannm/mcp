package com.amannmalik.mcp.cli;

public record ClientConfig(TransportType transport, String command) implements CliConfig {
    public ClientConfig {
        if (transport != TransportType.STDIO) {
            throw new IllegalArgumentException("only stdio supported");
        }
        if (command == null || command.isBlank()) {
            throw new IllegalArgumentException("command required");
        }
    }
}
