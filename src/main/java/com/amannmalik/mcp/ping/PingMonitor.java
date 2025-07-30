package com.amannmalik.mcp.ping;

import com.amannmalik.mcp.client.McpClient;

import java.io.IOException;

public final class PingMonitor {
    private PingMonitor() {
    }

    public static boolean isAlive(McpClient client, long timeoutMillis) {
        try {
            client.ping(timeoutMillis);
            return true;
        } catch (IOException | RuntimeException ignore) {
            return false;
        }
    }
}
