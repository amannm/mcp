package com.amannmalik.mcp.ping;

import com.amannmalik.mcp.client.McpClient;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PingMonitor {
    private static final Logger LOG = LoggerFactory.getLogger(PingMonitor.class);
    private PingMonitor() {
    }

    public static boolean isAlive(McpClient client, long timeoutMillis) {
        try {
            client.ping(timeoutMillis);
            return true;
        } catch (IOException | RuntimeException e) {
            LOG.debug("Ping failure: {}", e.getMessage());
            return false;
        }
    }
}
