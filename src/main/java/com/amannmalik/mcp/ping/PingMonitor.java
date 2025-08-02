package com.amannmalik.mcp.ping;

import com.amannmalik.mcp.client.McpClient;

import java.io.IOException;
import java.util.Objects;

public final class PingMonitor {
    private PingMonitor() {
    }

    public static boolean isAlive(McpClient client, long timeoutMillis) {
        Objects.requireNonNull(client);
        try {
            client.ping(timeoutMillis);
            return true;
        } catch (IOException | RuntimeException e) {
            System.err.println("Ping failure: " + e.getMessage());
            return false;
        }
    }
}
