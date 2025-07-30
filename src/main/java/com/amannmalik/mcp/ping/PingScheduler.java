package com.amannmalik.mcp.ping;

import com.amannmalik.mcp.client.McpClient;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Periodically sends ping requests and invokes a callback on failure.
 */
public final class PingScheduler implements AutoCloseable {
    private final McpClient client;
    private final long interval;
    private final long timeout;
    private final Runnable onFailure;
    private ScheduledExecutorService exec;

    public PingScheduler(McpClient client, long intervalMillis, long timeoutMillis, Runnable onFailure) {
        if (client == null || onFailure == null) throw new IllegalArgumentException("client and onFailure required");
        if (intervalMillis <= 0 || timeoutMillis <= 0) throw new IllegalArgumentException("invalid timing");
        this.client = client;
        this.interval = intervalMillis;
        this.timeout = timeoutMillis;
        this.onFailure = onFailure;
    }

    public synchronized void start() {
        if (exec != null) throw new IllegalStateException("already started");
        exec = Executors.newSingleThreadScheduledExecutor();
        exec.scheduleAtFixedRate(this::check, interval, interval, TimeUnit.MILLISECONDS);
    }

    private void check() {
        if (!PingMonitor.isAlive(client, timeout)) {
            if (System.err != null) System.err.println("Ping failed");
            try {
                onFailure.run();
            } catch (Exception ignore) {
            }
        }
    }

    @Override
    public synchronized void close() {
        if (exec != null) {
            exec.shutdownNow();
            exec = null;
        }
    }
}
