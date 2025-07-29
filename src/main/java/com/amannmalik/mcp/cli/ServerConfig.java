package com.amannmalik.mcp.cli;

public record ServerConfig(TransportType transport, int port) implements CliConfig {
    public ServerConfig {
        if (transport == null) throw new IllegalArgumentException("transport");
        if (transport == TransportType.HTTP && port < 0) {
            throw new IllegalArgumentException("port must be >= 0 for HTTP");
        }
        if (transport == TransportType.HTTP && port > 65535) {
            throw new IllegalArgumentException("invalid port: " + port);
        }
    }
}
