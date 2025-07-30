package com.amannmalik.mcp.ping;

import com.amannmalik.mcp.client.McpClient;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Periodically sends ping requests and invokes a callback when a configured
 * number of consecutive pings fail.
 */
public final class PingScheduler implements AutoCloseable {
    private final McpClient client;
    private final long interval;
    private final long timeout;
    private final Runnable onFailure;
    private final int maxFailures;
    private int failureCount;
    private ScheduledExecutorService exec;

    public PingScheduler(McpClient client,
                         long intervalMillis,
                         long timeoutMillis,
                         Runnable onFailure) {
        this(client, intervalMillis, timeoutMillis, onFailure, 1);
    }

    public PingScheduler(McpClient client,
                         long intervalMillis,
                         long timeoutMillis,
                         Runnable onFailure,
                         int maxFailures) {
        if (client == null || onFailure == null) throw new IllegalArgumentException("client and onFailure required");
        if (intervalMillis <= 0 || timeoutMillis <= 0) throw new IllegalArgumentException("invalid timing");
        if (maxFailures <= 0) throw new IllegalArgumentException("maxFailures must be > 0");
        this.client = client;
        this.interval = intervalMillis;
        this.timeout = timeoutMillis;
        this.onFailure = onFailure;
        this.maxFailures = maxFailures;
        this.failureCount = 0;
    }

    public synchronized void start() {
        if (exec != null) throw new IllegalStateException("already started");
        exec = Executors.newSingleThreadScheduledExecutor();
        exec.scheduleAtFixedRate(this::check, interval, interval, TimeUnit.MILLISECONDS);
    }

    private void check() {
        if (PingMonitor.isAlive(client, timeout)) {
            failureCount = 0;
            return;
        }

        failureCount++;
        if (failureCount >= maxFailures) {
            if (System.err != null) System.err.println("Ping failed");
            try {
                onFailure.run();
            } catch (Exception ignore) {
            } finally {
                failureCount = 0;
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
